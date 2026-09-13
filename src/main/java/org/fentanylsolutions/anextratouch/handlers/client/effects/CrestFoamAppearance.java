package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.fentanylsolutions.anextratouch.AnExtraTouch;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Shared Crest-style detail texture sampling and white-foam appearance. */
@SideOnly(Side.CLIENT)
final class CrestFoamAppearance {

    private static final ResourceLocation CREST_TEXTURE = new ResourceLocation(
        "anextratouch",
        "textures/particle/crest_foam.png");
    private static int[] detailPixels;
    private static int detailWidth;
    private static int detailHeight;

    private CrestFoamAppearance() {}

    static void ensureLoaded(Minecraft mc) {
        if (detailPixels != null) return;
        try (InputStream stream = mc.getResourceManager()
            .getResource(CREST_TEXTURE)
            .getInputStream()) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) throw new IOException("Invalid Crest foam image");
            detailWidth = image.getWidth();
            detailHeight = image.getHeight();
            detailPixels = image.getRGB(0, 0, detailWidth, detailHeight, null, 0, detailWidth);
        } catch (IOException e) {
            AnExtraTouch.LOG.warn("Could not load Crest foam detail; using plain foam", e);
            detailWidth = detailHeight = 1;
            detailPixels = new int[] { 0xAAAAAA };
        }
    }

    static void invalidate() {
        detailPixels = null;
        detailWidth = detailHeight = 0;
    }

    static float sample(double u, double v) {
        float uf = (float) u;
        float vf = (float) v;
        float x = (uf - (float) Math.floor(uf)) * detailWidth;
        float y = (vf - (float) Math.floor(vf)) * detailHeight;
        int ix = (int) x % detailWidth, iy = (int) y % detailHeight;
        int nx = (ix + 1) % detailWidth, ny = (iy + 1) % detailHeight;
        float fx = x - (int) x, fy = y - (int) y;
        float a = detailPixels[iy * detailWidth + ix] >> 16 & 255;
        float b = detailPixels[iy * detailWidth + nx] >> 16 & 255;
        float c = detailPixels[ny * detailWidth + ix] >> 16 & 255;
        float d = detailPixels[ny * detailWidth + nx] >> 16 & 255;
        return ((a + (b - a) * fx) * (1.0F - fy) + (c + (d - c) * fx) * fy) / 255.0F;
    }

    static float alpha(float density, float pattern) {
        return CrestFoamSimulation.whiteFoam(density, pattern) * 0.55F;
    }
}
