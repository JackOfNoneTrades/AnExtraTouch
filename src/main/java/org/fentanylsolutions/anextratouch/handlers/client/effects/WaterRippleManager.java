package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WaterRippleManager {

    public static final WaterRippleManager INSTANCE = new WaterRippleManager();

    private static final int FRAME_COUNT = 7;
    private static final int MAX_AGE = 7;
    private static final float HALF_SIZE = 0.25F;
    private static final float SURFACE_OFFSET = 0.012F;
    private static final float CRISTALINE_WATER_INVERSE_ALPHA = 1.0F - 180.0F / 255.0F;
    private static final ResourceLocation[] RIPPLE_TEX = makeFrames();
    private static Boolean cristalineWaterLoaded;

    private final List<Ripple> ripples = new ArrayList<Ripple>();
    private final java.util.Random rainRandom = new java.util.Random();

    private WaterRippleManager() {}

    public boolean trySpawnDripRipple(World world, double x, double y, double z) {
        if (!Config.waterDripRipplesEnabled || world == null || !world.isRemote) {
            return false;
        }

        int blockX = MathHelper.floor_double(x);
        int blockY = MathHelper.floor_double(y);
        int blockZ = MathHelper.floor_double(z);
        double surfaceY = getWaterSurfaceY(world, blockX, blockY, blockZ);
        if (surfaceY < 0.0D || y >= surfaceY) {
            return false;
        }

        spawnRipple(world, x, surfaceY, z);
        return true;
    }

    public void spawnRipple(World world, double x, double y, double z) {
        if (world == null || !world.isRemote) {
            return;
        }

        Ripple ripple = new Ripple();
        ripple.world = world;
        ripple.x = x;
        ripple.y = y + SURFACE_OFFSET;
        ripple.z = z;
        ripples.add(ripple);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || (!Config.rainRipplesEnabled && !Config.waterDripRipplesEnabled)) {
            ripples.clear();
            return;
        }

        spawnRainRipples(mc);

        Iterator<Ripple> iterator = ripples.iterator();
        while (iterator.hasNext()) {
            Ripple ripple = iterator.next();
            if (ripple.world != mc.theWorld || !isWater(ripple.world, ripple.x, ripple.y - SURFACE_OFFSET, ripple.z)) {
                iterator.remove();
                continue;
            }

            ripple.prevAge = ripple.age;
            ripple.age++;
            if (ripple.age >= MAX_AGE) {
                iterator.remove();
            }
        }
    }

    public void renderInWorldPass(float partialTicks) {
        if (ripples.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.renderViewEntity;
        if (viewer == null || mc.theWorld == null) {
            return;
        }

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
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
            GL11.glDepthMask(false);

            mc.entityRenderer.enableLightmap((double) partialTicks);
            for (Ripple ripple : ripples) {
                if (ripple.world == mc.theWorld) {
                    renderRipple(mc, ripple, camX, camY, camZ, partialTicks);
                }
            }
            mc.entityRenderer.disableLightmap((double) partialTicks);
        } finally {
            GL11.glDepthMask(true);
            GL11.glPopAttrib();
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
        }
    }

    private void spawnRainRipples(Minecraft mc) {
        if (!Config.rainRipplesEnabled || mc.theWorld == null || mc.renderViewEntity == null) {
            return;
        }

        float rainStrength = mc.theWorld.getRainStrength(1.0F);
        if (!mc.gameSettings.fancyGraphics) {
            rainStrength /= 2.0F;
        }
        if (rainStrength <= 0.0F) {
            return;
        }

        int attempts = (int) (100.0F * rainStrength * rainStrength * Config.rainRippleDensity);
        if (mc.gameSettings.particleSetting == 1) {
            attempts >>= 1;
        } else if (mc.gameSettings.particleSetting == 2) {
            attempts = 0;
        }

        EntityLivingBase viewer = mc.renderViewEntity;
        World world = mc.theWorld;
        int centerX = MathHelper.floor_double(viewer.posX);
        int centerY = MathHelper.floor_double(viewer.posY);
        int centerZ = MathHelper.floor_double(viewer.posZ);
        byte radius = 10;
        this.rainRandom.setSeed((long) viewer.ticksExisted * 312987231L);

        for (int i = 0; i < attempts; i++) {
            int x = centerX + this.rainRandom.nextInt(radius) - this.rainRandom.nextInt(radius);
            int z = centerZ + this.rainRandom.nextInt(radius) - this.rainRandom.nextInt(radius);
            int y = world.getPrecipitationHeight(x, z);
            if (y > centerY + radius || y < centerY - radius) {
                continue;
            }

            BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
            if (!biome.canSpawnLightningBolt() || biome.getFloatTemperature(x, y, z) < 0.15F) {
                continue;
            }

            double surfaceY = getWaterSurfaceY(world, x, y - 1, z);
            if (surfaceY >= 0.0D) {
                spawnRipple(
                    world,
                    (double) x + this.rainRandom.nextFloat(),
                    surfaceY,
                    (double) z + this.rainRandom.nextFloat());
            }
        }
    }

    private static ResourceLocation[] makeFrames() {
        ResourceLocation[] frames = new ResourceLocation[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = new ResourceLocation(
                AnExtraTouch.MODID,
                "textures/particles/water_ripple/water_ripple_" + (i + 1) + ".png");
        }
        return frames;
    }

    private static double getWaterSurfaceY(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block.getMaterial() != Material.water) {
            return -1.0D;
        }

        return (double) (y + 1) - BlockLiquid.getLiquidHeightPercent(world.getBlockMetadata(x, y, z));
    }

    private static boolean isWater(World world, double x, double y, double z) {
        return world.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z))
            .getMaterial() == Material.water;
    }

    private static int frameForAge(int prevAge, int age, float partialTicks) {
        float ageDelta = prevAge + (age - prevAge) * partialTicks;
        if (ageDelta < 0.0F) {
            ageDelta = 0.0F;
        }

        int frame = (int) (ageDelta / (float) MAX_AGE * FRAME_COUNT);
        if (frame < 0) {
            return 0;
        }
        if (frame >= FRAME_COUNT) {
            return FRAME_COUNT - 1;
        }
        return frame;
    }

    private static void renderRipple(Minecraft mc, Ripple ripple, double camX, double camY, double camZ,
        float partialTicks) {
        int frame = frameForAge(ripple.prevAge, ripple.age, partialTicks);
        mc.getTextureManager()
            .bindTexture(RIPPLE_TEX[frame]);

        float x = (float) (ripple.x - camX);
        float y = (float) (ripple.y - camY);
        float z = (float) (ripple.z - camZ);
        int brightness = ripple.world.getLightBrightnessForSkyBlocks(
            MathHelper.floor_double(ripple.x),
            MathHelper.floor_double(ripple.y),
            MathHelper.floor_double(ripple.z),
            0);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setBrightness(brightness);
        tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, getEffectiveAlpha());
        tessellator.addVertexWithUV(x - HALF_SIZE, y, z - HALF_SIZE, 1.0D, 1.0D);
        tessellator.addVertexWithUV(x - HALF_SIZE, y, z + HALF_SIZE, 1.0D, 0.0D);
        tessellator.addVertexWithUV(x + HALF_SIZE, y, z + HALF_SIZE, 0.0D, 0.0D);
        tessellator.addVertexWithUV(x + HALF_SIZE, y, z - HALF_SIZE, 0.0D, 1.0D);
        tessellator.draw();
    }

    private static float getEffectiveAlpha() {
        float alpha = Config.waterRippleAlpha;
        if (isCristalineWaterLoaded()) {
            alpha *= CRISTALINE_WATER_INVERSE_ALPHA;
        }
        return Math.min(1.0F, Math.max(0.0F, alpha));
    }

    private static boolean isCristalineWaterLoaded() {
        if (cristalineWaterLoaded == null) {
            cristalineWaterLoaded = Loader.isModLoaded("cristalinewater");
        }
        return cristalineWaterLoaded;
    }

    private static class Ripple {

        World world;
        double x;
        double y;
        double z;
        int age;
        int prevAge;
    }
}
