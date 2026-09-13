package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

import org.fentanylsolutions.anextratouch.Config;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Crest foam appearance over the stock wake's white bands, with continuous samples between tiles. */
@SideOnly(Side.CLIENT)
final class WakeFoamTexture {

    private static final int SIZE = 2048;
    private static final int CONTENT = 65;
    private static final int TILE = CONTENT + 2;
    private static final int COLUMNS = SIZE / TILE;
    private static final int MAX_SLOTS = 512;
    private static final double DETAIL_SCALE = 2.4D;
    private static final ResourceLocation LOCATION = new ResourceLocation("anextratouch", "dynamic/wake_foam");

    private static DynamicTexture texture;
    private static int[] pixels;
    private static int used;

    private WakeFoamTexture() {}

    static void beginFrame(Minecraft mc) {
        if (mc == null || mc.getTextureManager() == null) return;
        ensureTexture(mc);
        CrestFoamAppearance.ensureLoaded(mc);
        used = 0;
    }

    static int addTile(int blockX, int blockZ, float[] vertices, int grid, int stride) {
        if (vertices == null || grid < 2 || stride < 4 || vertices.length < grid * grid * stride || used >= MAX_SLOTS)
            return -1;
        boolean visible = false;
        int samples = grid;
        int intervals = grid - 1;
        for (int y = 0; y < grid && !visible; y++) {
            for (int x = 0; x < grid; x++) {
                int index = (y * samples + x) * stride + 3;
                if (index >= 0 && index < vertices.length && vertices[index] > 0.0001f) {
                    visible = true;
                    break;
                }
            }
        }
        if (!visible) return -1;

        int slot = used++;
        int originX = (slot % COLUMNS) * TILE;
        int originY = (slot / COLUMNS) * TILE;
        for (int py = 0; py < CONTENT; py++) {
            for (int px = 0; px < CONTENT; px++) {
                float gx = px * intervals / (float) (CONTENT - 1);
                float gy = py * intervals / (float) (CONTENT - 1);
                int x0 = Math.min(intervals - 1, (int) gx);
                int y0 = Math.min(intervals - 1, (int) gy);
                float tx = gx - x0;
                float ty = gy - y0;
                float a00 = density(vertices, (y0 * samples + x0) * stride + 3);
                float a10 = density(vertices, (y0 * samples + x0 + 1) * stride + 3);
                float a01 = density(vertices, ((y0 + 1) * samples + x0) * stride + 3);
                float a11 = density(vertices, ((y0 + 1) * samples + x0 + 1) * stride + 3);
                float amount = lerp(lerp(a00, a10, tx), lerp(a01, a11, tx), ty);
                if (amount <= 0.0001F) {
                    pixels[(originY + 1 + py) * SIZE + originX + 1 + px] = white(0.0F);
                    continue;
                }
                // Foam detail stays on the water as the wake passes. Wrap in double precision
                // before sampling so adjacent tiles agree even far from the world origin.
                double u = (blockX + px / (double) (CONTENT - 1)) / DETAIL_SCALE;
                double v = (blockZ + py / (double) (CONTENT - 1)) / DETAIL_SCALE;
                float pattern = CrestFoamAppearance.sample(u - Math.floor(u), v - Math.floor(v));
                float alpha = CrestFoamAppearance.alpha(amount, pattern) * Config.waterWakeAlpha;
                pixels[(originY + 1 + py) * SIZE + originX + 1 + px] = white(alpha);
            }
        }
        for (int i = 0; i < CONTENT; i++) {
            pixels[originY * SIZE + originX + 1 + i] = pixels[(originY + 1) * SIZE + originX + 1 + i];
            pixels[(originY + TILE - 1) * SIZE + originX + 1 + i] = pixels[(originY + CONTENT) * SIZE + originX
                + 1
                + i];
            pixels[(originY + 1 + i) * SIZE + originX] = pixels[(originY + 1 + i) * SIZE + originX + 1];
            pixels[(originY + 1 + i) * SIZE + originX + TILE - 1] = pixels[(originY + 1 + i) * SIZE + originX
                + CONTENT];
        }
        pixels[originY * SIZE + originX] = pixels[originY * SIZE + originX + 1];
        pixels[originY * SIZE + originX + TILE - 1] = pixels[originY * SIZE + originX + TILE - 2];
        pixels[(originY + TILE - 1) * SIZE + originX] = pixels[(originY + TILE - 1) * SIZE + originX + 1];
        pixels[(originY + TILE - 1) * SIZE + originX + TILE - 1] = pixels[(originY + TILE - 1) * SIZE + originX
            + TILE
            - 2];
        return slot;
    }

    static void uploadAndBind(Minecraft mc) {
        if (mc == null || mc.getTextureManager() == null || texture == null || used == 0) return;
        int rows = ((used + COLUMNS - 1) / COLUMNS) * TILE;
        TextureUtil.uploadTexture(texture.getGlTextureId(), pixels, SIZE, rows);
        mc.getTextureManager()
            .bindTexture(LOCATION);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    static double u0(int slot) {
        return ((slot % COLUMNS) * TILE + 1.5) / SIZE;
    }

    static double u1(int slot) {
        return ((slot % COLUMNS) * TILE + CONTENT + 0.5) / SIZE;
    }

    static double v0(int slot) {
        return ((slot / COLUMNS) * TILE + 1.5) / SIZE;
    }

    static double v1(int slot) {
        return ((slot / COLUMNS) * TILE + CONTENT + 0.5) / SIZE;
    }

    private static void ensureTexture(Minecraft mc) {
        if (texture != null) return;
        texture = new DynamicTexture(SIZE, SIZE);
        pixels = texture.getTextureData();
        mc.getTextureManager()
            .loadTexture(LOCATION, texture);
    }

    private static int white(float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
        return (a << 24) | 0x00FCFEFF;
    }

    private static float density(float[] vertices, int index) {
        return index >= 0 && index < vertices.length ? Math.max(0.0f, Math.min(1.0f, vertices[index])) : 0.0f;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

}
