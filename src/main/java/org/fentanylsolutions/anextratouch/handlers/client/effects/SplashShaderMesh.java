package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Clips the mesh to the sprite because water shaders can replace the texture's alpha. */
@SideOnly(Side.CLIENT)
public final class SplashShaderMesh {

    private static final Map<ResourceLocation, SplashShaderMesh> CACHE = new HashMap<>();
    private final List<float[]> spans;

    private SplashShaderMesh(List<float[]> spans) {
        this.spans = spans;
    }

    public static void invalidate() {
        CACHE.clear();
    }

    static SplashShaderMesh get(Minecraft mc, ResourceLocation texture) {
        if (!CACHE.containsKey(texture)) {
            SplashShaderMesh mesh = null;
            try (InputStream stream = mc.getResourceManager()
                .getResource(texture)
                .getInputStream()) {
                BufferedImage image = ImageIO.read(stream);
                // Bound geometry from unusually large resource-pack replacements.
                if (image != null && image.getWidth() <= 256 && image.getHeight() <= 256) {
                    mesh = fromImage(image);
                }
            } catch (IOException ignored) {}
            CACHE.put(texture, mesh);
        }
        return CACHE.get(texture);
    }

    static SplashShaderMesh fromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        List<float[]> spans = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            int x = 0;
            while (x < width) {
                if ((image.getRGB(x, y) >>> 24) <= 2) {
                    x++;
                    continue;
                }
                int start = x++;
                while (x < width && (image.getRGB(x, y) >>> 24) > 2) x++;
                spans.add(
                    new float[] { start / (float) width, y / (float) height, x / (float) width,
                        (y + 1) / (float) height });
            }
        }
        return new SplashShaderMesh(spans);
    }

    void render(Tessellator t, float x, float y, float z, float ux, float uy, float uz, float vx, float vy, float vz,
        boolean doubleSided) {
        float nx = uz * vy - uy * vz;
        float ny = ux * vz - uz * vx;
        float nz = uy * vx - ux * vy;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 0.00001F) return;
        nx /= length;
        ny /= length;
        nz /= length;
        for (float[] span : spans) {
            t.setNormal(nx, ny, nz);
            vertex(t, x, y, z, ux, uy, uz, vx, vy, vz, span[0], span[3]);
            vertex(t, x, y, z, ux, uy, uz, vx, vy, vz, span[2], span[3]);
            vertex(t, x, y, z, ux, uy, uz, vx, vy, vz, span[2], span[1]);
            vertex(t, x, y, z, ux, uy, uz, vx, vy, vz, span[0], span[1]);
            if (doubleSided) {
                t.setNormal(-nx, -ny, -nz);
                vertex(t, x, y, z, ux, uy, uz, vx, vy, vz, span[2], span[3]);
                vertex(t, x, y, z, ux, uy, uz, vx, vy, vz, span[0], span[3]);
                vertex(t, x, y, z, ux, uy, uz, vx, vy, vz, span[0], span[1]);
                vertex(t, x, y, z, ux, uy, uz, vx, vy, vz, span[2], span[1]);
            }
        }
    }

    private static void vertex(Tessellator t, float x, float y, float z, float ux, float uy, float uz, float vx,
        float vy, float vz, float u, float v) {
        t.addVertexWithUV(x + ux * u + vx * v, y + uy * u + vy * v, z + uz * u + vz * v, u, v);
    }
}
