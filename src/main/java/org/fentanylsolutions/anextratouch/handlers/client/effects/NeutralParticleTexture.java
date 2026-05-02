package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.fentanylsolutions.anextratouch.AnExtraTouch;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class NeutralParticleTexture {

    private static final ResourceLocation PARTICLE_TEXTURES = new ResourceLocation("textures/particle/particles.png");
    private static final int[] WATER_PARTICLE_CELLS = { 19, 20, 21, 22, 23, 32 };

    private static boolean applied;

    private NeutralParticleTexture() {}

    public static void ensureApplied() {
        if (applied) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getTextureManager() == null || mc.getResourceManager() == null) {
            return;
        }

        try {
            BufferedImage image;
            InputStream stream = mc.getResourceManager()
                .getResource(PARTICLE_TEXTURES)
                .getInputStream();
            try {
                image = ImageIO.read(stream);
            } finally {
                stream.close();
            }
            if (image == null || image.getWidth() < 16 || image.getHeight() < 16) {
                applied = true;
                return;
            }

            int cellWidth = image.getWidth() / 16;
            int cellHeight = image.getHeight() / 16;
            for (int index : WATER_PARTICLE_CELLS) {
                neutralizeCell(image, index % 16 * cellWidth, index / 16 * cellHeight, cellWidth, cellHeight);
            }

            mc.getTextureManager()
                .loadTexture(PARTICLE_TEXTURES, new DynamicTexture(image));
            applied = true;
        } catch (IOException e) {
            applied = true;
            AnExtraTouch.LOG.warn("Could not neutralize vanilla water particle texture", e);
        } catch (RuntimeException e) {
            applied = true;
            AnExtraTouch.LOG.warn("Could not neutralize vanilla water particle texture", e);
        }
    }

    public static void invalidate() {
        applied = false;
    }

    private static void neutralizeCell(BufferedImage image, int startX, int startY, int width, int height) {
        int maxX = Math.min(image.getWidth(), startX + width);
        int maxY = Math.min(image.getHeight(), startY + height);

        for (int y = startY; y < maxY; y++) {
            for (int x = startX; x < maxX; x++) {
                int pixel = image.getRGB(x, y);
                int alpha = pixel >>> 24;
                if (alpha == 0) {
                    continue;
                }

                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;
                int value = Math.max(red, Math.max(green, blue));
                image.setRGB(x, y, alpha << 24 | value << 16 | value << 8 | value);
            }
        }
    }
}
