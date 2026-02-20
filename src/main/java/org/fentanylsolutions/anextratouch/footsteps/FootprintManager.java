package org.fentanylsolutions.anextratouch.footsteps;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
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
public class FootprintManager {

    public static final FootprintManager INSTANCE = new FootprintManager();

    private static final ResourceLocation FOOTPRINT_TEXLOC = new ResourceLocation(
        AnExtraTouch.MODID,
        "textures/particles/footprint.png");

    private static final float TEXEL_WIDTH = 0.125f; // 1/8 of texture for each style
    private static final float TEXEL_HALF = 0.0625f; // Half width for left/right
    private static final float WIDTH = 0.125f;
    private static final float LENGTH = 0.25f;

    private static float zFighter = 0.0f;

    private static class Footprint {

        World world;
        double x;
        double y;
        double z;
        float texU1;
        float texU2;
        float[] cornerX = new float[4];
        float[] cornerZ = new float[4];
        int age;
        int maxAge;
        float opacity;
    }

    private final List<Footprint> footprints = new ArrayList<>();

    public void addFootprint(World world, double x, double y, double z, float rotationYaw, FootprintStyle style,
        boolean isRightFoot, float footScale, int lifespan, float opacity) {
        if (world == null) {
            return;
        }
        int cap = Config.footprintParticleCap;
        if (cap > 0 && footprints.size() >= cap) {
            while (!footprints.isEmpty() && footprints.size() >= cap) {
                footprints.remove(0);
            }
        }

        Footprint fp = new Footprint();
        fp.world = world;

        zFighter += 1.0f;
        if (zFighter > 20.0f) {
            zFighter = 1.0f;
        }
        fp.x = x;
        fp.y = y + zFighter * 0.001;
        fp.z = z;

        float styleOffset = style.ordinal() * TEXEL_WIDTH + (float) 1 / 256; // small offset to avoid bleeding
        fp.texU1 = isRightFoot ? styleOffset + TEXEL_HALF : styleOffset;
        fp.texU2 = fp.texU1 + TEXEL_HALF;

        float radians = (float) Math.toRadians(rotationYaw + 180.0f);
        float cos = MathHelper.cos(radians);
        float sin = MathHelper.sin(radians);

        float scaledWidth = WIDTH * footScale;
        float scaledLength = LENGTH * footScale;
        float[][] baseCorners = { { -scaledWidth, scaledLength }, { scaledWidth, scaledLength },
            { scaledWidth, -scaledLength }, { -scaledWidth, -scaledLength } };

        for (int i = 0; i < 4; i++) {
            fp.cornerX[i] = baseCorners[i][0] * cos - baseCorners[i][1] * sin;
            fp.cornerZ[i] = baseCorners[i][0] * sin + baseCorners[i][1] * cos;
        }

        fp.maxAge = Math.max(1, lifespan);
        fp.opacity = opacity;

        // Apply weather multiplier once at spawn
        int bx = MathHelper.floor_double(fp.x);
        int bz = MathHelper.floor_double(fp.z);
        if (world.isRaining() && world.canBlockSeeTheSky(bx, MathHelper.floor_double(fp.y) + 1, bz)) {
            boolean isSnow = world.getBiomeGenForCoords(bx, bz)
                .getEnableSnow();
            float multiplier = isSnow ? Config.snowLifespanMultiplier : Config.rainLifespanMultiplier;
            fp.maxAge = Math.max(1, (int) (fp.maxAge * multiplier));
        }

        footprints.add(fp);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!Config.footprintsEnabled) {
            footprints.clear();
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            footprints.clear();
            return;
        }

        Iterator<Footprint> it = footprints.iterator();
        while (it.hasNext()) {
            Footprint fp = it.next();
            if (fp.world != mc.theWorld) {
                it.remove();
                continue;
            }

            if (fp.world.isAirBlock(
                MathHelper.floor_double(fp.x),
                MathHelper.floor_double(fp.y - 0.05),
                MathHelper.floor_double(fp.z))) {
                it.remove();
                continue;
            }

            fp.age++;
            if (fp.age >= fp.maxAge) {
                it.remove();
            }
        }
    }

    public void renderInWorldPass(float partialTicks) {
        if (!Config.footprintsEnabled || footprints.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.renderViewEntity;
        if (viewer == null || mc.theWorld == null) {
            return;
        }

        double camX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * (double) partialTicks;
        double camY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * (double) partialTicks;
        double camZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * (double) partialTicks;

        mc.getTextureManager()
            .bindTexture(FOOTPRINT_TEXLOC);

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-5F, -5F);

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_CURRENT_BIT);
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);

            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();

            for (Footprint fp : footprints) {
                if (fp.world != mc.theWorld) {
                    continue;
                }

                float ageFraction = (fp.age + partialTicks) / (float) fp.maxAge;
                float alpha = Math.max(0, 1.0f - ageFraction * ageFraction) * fp.opacity;
                if (alpha <= 0.0f) {
                    continue;
                }

                int bx = MathHelper.floor_double(fp.x);
                int by = MathHelper.floor_double(fp.y);
                int bz = MathHelper.floor_double(fp.z);
                int brightness = fp.world.getLightBrightnessForSkyBlocks(bx, by, bz, 0);

                float renderX = (float) (fp.x - camX);
                float renderY = (float) (fp.y - camY);
                float renderZ = (float) (fp.z - camZ);

                tess.setColorRGBA_F(1.0f, 1.0f, 1.0f, alpha);
                tess.setBrightness(brightness);

                tess.addVertexWithUV(renderX + fp.cornerX[0], renderY, renderZ + fp.cornerZ[0], fp.texU1, 1.0);
                tess.addVertexWithUV(renderX + fp.cornerX[1], renderY, renderZ + fp.cornerZ[1], fp.texU2, 1.0);
                tess.addVertexWithUV(renderX + fp.cornerX[2], renderY, renderZ + fp.cornerZ[2], fp.texU2, 0.0);
                tess.addVertexWithUV(renderX + fp.cornerX[3], renderY, renderZ + fp.cornerZ[3], fp.texU1, 0.0);
            }

            tess.draw();
        } finally {
            GL11.glPopAttrib();
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            GL11.glPolygonOffset(0.0f, 0.0f);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
    }
}
