package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WaterSplashManager {

    public static final WaterSplashManager INSTANCE = new WaterSplashManager();

    private static final int FRAME_COUNT = 9;
    private static final int VISUAL_MAX_AGE = 18;
    private static final int EMITTER_MAX_AGE = 24;
    private static final int SECOND_WAVE_AGE = 8;
    private static final float SPEED_CAP = 2.0f;

    private static final ResourceLocation[] SPLASH_TEX = makeFrames("water_splash");
    private static final ResourceLocation[] FOAM_TEX = makeFrames("water_splash_foam");
    private static final ResourceLocation[] RING_TEX = makeFrames("water_splash_ring");

    private static ResourceLocation[] makeFrames(String prefix) {
        ResourceLocation[] r = new ResourceLocation[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            r[i] = new ResourceLocation(
                AnExtraTouch.MODID,
                "textures/particles/water_splash/" + prefix + "_" + (i + 1) + ".png");
        }
        return r;
    }

    private static class Splash {

        World world;
        double x, y, z;
        float width;
        float height;
        int age;
        int prevAge;
        boolean foam;
        float r, g, b;
    }

    private static class Ring {

        World world;
        double x, y, z;
        float width;
        int age;
        int prevAge;
        float r, g, b;
    }

    private static class Emitter {

        World world;
        double x, y, z;
        float width;
        float height;
        float speed;
        float r, g, b;
        int age;
        boolean spawnedSecondWave;
    }

    private final List<Splash> splashes = new ArrayList<Splash>();
    private final List<Ring> rings = new ArrayList<Ring>();
    private final List<Emitter> emitters = new ArrayList<Emitter>();

    public void spawnEmitter(World world, double x, double y, double z, float width, float speed) {
        if (world == null || !world.isRemote) return;

        speed = Math.min(SPEED_CAP, speed);
        float height = (speed / 2f + width / 3f);
        float[] rgb = sampleSplashFluidColor(world, x, y, z);

        spawnSplash(world, x, y, z, width, height, false, rgb);
        spawnSplash(world, x, y, z, width, height, true);
        spawnRing(world, x, y, z, width);

        if (speed > 0.5f) {
            float dropletSpeed = (1.5f / 8f + speed * 1f / 8f) + (width / 6f);
            spawnDroplets(world, x, y, z, width, dropletSpeed, 0.15f, rgb);
        }

        Emitter e = new Emitter();
        e.world = world;
        e.x = x;
        e.y = y;
        e.z = z;
        e.width = width;
        e.height = height;
        e.speed = speed;
        e.r = rgb[0];
        e.g = rgb[1];
        e.b = rgb[2];
        emitters.add(e);
    }

    private void spawnSplash(World world, double x, double y, double z, float width, float height, boolean foam) {
        spawnSplash(world, x, y, z, width, height, foam, null);
    }

    private void spawnSplash(World world, double x, double y, double z, float width, float height, boolean foam,
        float[] rgb) {
        Splash s = new Splash();
        s.world = world;
        s.x = x;
        s.y = y;
        s.z = z;
        s.width = width;
        s.height = height;
        s.foam = foam;
        if (foam) {
            s.r = s.g = s.b = 1.0f;
        } else {
            if (rgb == null) {
                rgb = sampleSplashFluidColor(world, x, y, z);
            }
            s.r = rgb[0];
            s.g = rgb[1];
            s.b = rgb[2];
        }
        splashes.add(s);
    }

    private void spawnRing(World world, double x, double y, double z, float width) {
        Ring r = new Ring();
        r.world = world;
        r.x = x;
        r.y = y;
        r.z = z;
        r.width = width;
        // ring uses default tinted color (white) like vanilla; particular uses red/green/blue from default sprite
        r.r = r.g = r.b = 1.0f;
        rings.add(r);
    }

    private void spawnDroplets(World world, double x, double y, double z, float width, float speed, float spread,
        float[] rgb) {
        java.util.Random rand = world.rand;
        int count = (int) (width * 20f);
        for (int i = 0; i < count; i++) {
            double xVel = nextTriangular(rand, 0.0, spread);
            double yVel = speed * nextTriangular(rand, 1.0, 0.25);
            double zVel = nextTriangular(rand, 0.0, spread);

            double px = x + xVel / spread * width;
            double py = y + 1.0 / 16.0;
            double pz = z + zVel / spread * width;

            EntityFX drop = new FallingWaterFX(world, px, py, pz, rgb);
            drop.motionX = xVel;
            drop.motionY = yVel;
            drop.motionZ = zVel;
            // make the drops a bit smaller (1/8 of base scale matches Particular)
            drop.multipleParticleScaleBy(0.5f);
            Minecraft.getMinecraft().effectRenderer.addEffect(drop);
        }
    }

    private static double nextTriangular(java.util.Random r, double mode, double deviation) {
        return mode + deviation * (r.nextDouble() - r.nextDouble());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            splashes.clear();
            rings.clear();
            emitters.clear();
            return;
        }

        if (!Config.waterSplashEnabled) {
            splashes.clear();
            rings.clear();
            emitters.clear();
            return;
        }

        Iterator<Emitter> ei = emitters.iterator();
        while (ei.hasNext()) {
            Emitter e = ei.next();
            if (e.world != mc.theWorld) {
                ei.remove();
                continue;
            }
            e.age++;
            if (!e.spawnedSecondWave && e.age == SECOND_WAVE_AGE) {
                e.spawnedSecondWave = true;
                float w2 = e.width * 0.66f;
                float h2 = e.height * 2f;
                float[] rgb = new float[] { e.r, e.g, e.b };
                spawnSplash(e.world, e.x, e.y, e.z, w2, h2, false, rgb);
                spawnSplash(e.world, e.x, e.y, e.z, w2, h2, true);
                spawnRing(e.world, e.x, e.y, e.z, w2);
                if (e.speed > 0.5f) {
                    float dropletSpeed = (3f / 8f + e.speed * 1f / 8f) + (e.width / 6f);
                    spawnDroplets(e.world, e.x, e.y, e.z, w2, dropletSpeed, 0.05f, rgb);
                }
            }
            // emitter dies if no longer in water or aged out
            if (e.age >= EMITTER_MAX_AGE || !isSplashFluid(e.world, e.x, e.y, e.z)) {
                ei.remove();
            }
        }

        Iterator<Splash> si = splashes.iterator();
        while (si.hasNext()) {
            Splash s = si.next();
            if (s.world != mc.theWorld) {
                si.remove();
                continue;
            }
            s.prevAge = s.age;
            s.age++;
            if (s.age >= VISUAL_MAX_AGE || !isSplashFluid(s.world, s.x, s.y, s.z)) {
                si.remove();
            }
        }

        Iterator<Ring> ri = rings.iterator();
        while (ri.hasNext()) {
            Ring r = ri.next();
            if (r.world != mc.theWorld) {
                ri.remove();
                continue;
            }
            r.prevAge = r.age;
            r.age++;
            if (r.age >= VISUAL_MAX_AGE || !isSplashFluid(r.world, r.x, r.y, r.z)) {
                ri.remove();
            }
        }
    }

    private static boolean isSplashFluid(World world, double x, double y, double z) {
        int blockX = MathHelper.floor_double(x);
        int blockY = MathHelper.floor_double(y);
        int blockZ = MathHelper.floor_double(z);
        return isSplashFluid(world, blockX, blockY, blockZ) || isSplashFluid(world, blockX, blockY - 1, blockZ);
    }

    private static boolean isSplashFluid(World world, int x, int y, int z) {
        return isSplashFluidAllowed(world, x, y, z);
    }

    private static float[] sampleSplashFluidColor(World world, double x, double y, double z) {
        int blockX = MathHelper.floor_double(x);
        int blockY = MathHelper.floor_double(y);
        int blockZ = MathHelper.floor_double(z);

        if (isSplashFluid(world, blockX, blockY, blockZ)) {
            return WetnessFluidHelper.getWettableFluidColor(world, blockX, blockY, blockZ);
        }
        if (isSplashFluid(world, blockX, blockY - 1, blockZ)) {
            return WetnessFluidHelper.getWettableFluidColor(world, blockX, blockY - 1, blockZ);
        }

        return FallingWaterFX.getWaterColor(world, x, y, z);
    }

    public static boolean isSplashFluidAllowed(World world, int x, int y, int z) {
        return WetnessFluidHelper.getSplashFluid(world, x, y, z) != null;
    }

    public static double getSplashFluidSurfaceY(World world, int x, int y, int z) {
        return WetnessFluidHelper.getSplashFluidSurfaceY(world, x, y, z);
    }

    public static boolean intersectsSplashFluid(AxisAlignedBB box, World world, int x, int y, int z) {
        return WetnessFluidHelper.intersectsSplashFluid(box, world, x, y, z);
    }

    public void renderInWorldPass(float partialTicks) {
        if (!Config.waterSplashEnabled) return;
        if (splashes.isEmpty() && rings.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.renderViewEntity;
        if (viewer == null || mc.theWorld == null) return;

        double camX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double camY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double camZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_CURRENT_BIT);
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_LIGHTING);
            // Alpha test ON discards fully-transparent texture pixels so they don't write depth
            // and block the water surface from drawing through. Without this, the splash quad's
            // bounding rectangle occludes everything behind it.
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
            GL11.glDepthMask(true);

            mc.entityRenderer.enableLightmap((double) partialTicks);

            for (Splash s : splashes) {
                if (s.world != mc.theWorld) continue;
                renderSplash(mc, s, camX, camY, camZ, partialTicks);
            }
            for (Ring r : rings) {
                if (r.world != mc.theWorld) continue;
                renderRing(mc, r, camX, camY, camZ, partialTicks);
            }

            mc.entityRenderer.disableLightmap((double) partialTicks);
        } finally {
            GL11.glDepthMask(true);
            GL11.glPopAttrib();
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
        }
    }

    private static int frameForAge(int prevAge, int age, float partialTicks) {
        float ageDelta = prevAge + (age - prevAge) * partialTicks;
        if (ageDelta < 0) ageDelta = 0;
        int frame = (int) (ageDelta / (float) VISUAL_MAX_AGE * FRAME_COUNT);
        if (frame < 0) frame = 0;
        if (frame >= FRAME_COUNT) frame = FRAME_COUNT - 1;
        return frame;
    }

    private void renderSplash(Minecraft mc, Splash s, double camX, double camY, double camZ, float partialTicks) {
        int frame = frameForAge(s.prevAge, s.age, partialTicks);
        ResourceLocation tex = s.foam ? FOAM_TEX[frame] : SPLASH_TEX[frame];
        mc.getTextureManager()
            .bindTexture(tex);

        float ageDelta = s.prevAge + (s.age - s.prevAge) * partialTicks;
        float progress = ageDelta / (float) VISUAL_MAX_AGE;
        float scale = s.width * (0.8f + 0.2f * progress);

        float fx = (float) (s.x - camX);
        float fy = (float) (s.y - camY);
        float fz = (float) (s.z - camZ);

        int bx = MathHelper.floor_double(s.x);
        int by = MathHelper.floor_double(s.y);
        int bz = MathHelper.floor_double(s.z);
        int brightness = s.world.getLightBrightnessForSkyBlocks(bx, by, bz, 0);

        // 4 corners of a square in XZ around (fx, fy, fz), scaled by scale
        float[][] c = new float[][] { { -scale, -scale }, { -scale, scale }, { scale, scale }, { scale, -scale } };

        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(s.r, s.g, s.b, 1.0f);
        t.setBrightness(brightness);

        float u0 = 0.0f;
        float u1 = 1.0f;
        float v0 = 0.0f;
        float v1 = 1.0f;

        // 4 sides, each rendered double-sided
        renderSide(t, fx, fy, fz, c[0][0], c[0][1], c[1][0], c[1][1], s.height, u0, u1, v0, v1);
        renderSide(t, fx, fy, fz, c[1][0], c[1][1], c[2][0], c[2][1], s.height, u0, u1, v0, v1);
        renderSide(t, fx, fy, fz, c[2][0], c[2][1], c[3][0], c[3][1], s.height, u0, u1, v0, v1);
        renderSide(t, fx, fy, fz, c[3][0], c[3][1], c[0][0], c[0][1], s.height, u0, u1, v0, v1);

        t.draw();
    }

    private void renderSide(Tessellator t, float fx, float fy, float fz, float ax, float az, float bx, float bz,
        float height, float u0, float u1, float v0, float v1) {
        // Front face
        t.addVertexWithUV(fx + ax, fy, fz + az, u0, v1);
        t.addVertexWithUV(fx + bx, fy, fz + bz, u1, v1);
        t.addVertexWithUV(fx + bx, fy + height, fz + bz, u1, v0);
        t.addVertexWithUV(fx + ax, fy + height, fz + az, u0, v0);
        // Back face
        t.addVertexWithUV(fx + bx, fy, fz + bz, u1, v1);
        t.addVertexWithUV(fx + ax, fy, fz + az, u0, v1);
        t.addVertexWithUV(fx + ax, fy + height, fz + az, u0, v0);
        t.addVertexWithUV(fx + bx, fy + height, fz + bz, u1, v0);
    }

    private void renderRing(Minecraft mc, Ring ring, double camX, double camY, double camZ, float partialTicks) {
        int frame = frameForAge(ring.prevAge, ring.age, partialTicks);
        mc.getTextureManager()
            .bindTexture(RING_TEX[frame]);

        float ageDelta = ring.prevAge + (ring.age - ring.prevAge) * partialTicks;
        float progress = ageDelta / (float) VISUAL_MAX_AGE;
        float scale = ring.width * (0.8f + 0.2f * progress);

        float fx = (float) (ring.x - camX);
        float fy = (float) (ring.y - camY) + 0.005f;
        float fz = (float) (ring.z - camZ);

        int bx = MathHelper.floor_double(ring.x);
        int by = MathHelper.floor_double(ring.y);
        int bz = MathHelper.floor_double(ring.z);
        int brightness = ring.world.getLightBrightnessForSkyBlocks(bx, by, bz, 0);

        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(ring.r, ring.g, ring.b, 1.0f);
        t.setBrightness(brightness);
        t.addVertexWithUV(fx - scale, fy, fz - scale, 1.0f, 1.0f);
        t.addVertexWithUV(fx - scale, fy, fz + scale, 1.0f, 0.0f);
        t.addVertexWithUV(fx + scale, fy, fz + scale, 0.0f, 0.0f);
        t.addVertexWithUV(fx + scale, fy, fz - scale, 0.0f, 1.0f);
        t.draw();
    }
}
