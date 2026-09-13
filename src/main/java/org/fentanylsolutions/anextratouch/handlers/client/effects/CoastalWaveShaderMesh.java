package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Procedural breaking foam over the existing sea surface, without a second water material. */
@SideOnly(Side.CLIENT)
public final class CoastalWaveShaderMesh {

    private static final int SEGMENTS = 64;
    private static final int DEPTH_SEGMENTS = 8;
    private static final int ATLAS_WIDTH = 2048;
    private static final int ATLAS_HEIGHT = 1024;
    private static final int TEXTURE_WIDTH = CrestFoamSimulation.WIDTH;
    private static final int TEXTURE_HEIGHT = CrestFoamSimulation.HEIGHT;
    private static final int TILE_WIDTH = TEXTURE_WIDTH + 2;
    private static final int TILE_HEIGHT = TEXTURE_HEIGHT + 2;
    private static final int COLUMNS = ATLAS_WIDTH / TILE_WIDTH;
    private static final int MAX_TILES = 96;
    private static final ResourceLocation FOAM_LOCATION = new ResourceLocation("anextratouch", "dynamic/coastal_foam");
    private static final float[] NOISE = makeNoise();
    private static DynamicTexture foamTexture;
    private static int[] pixels;
    private static int used;

    private final float seed;
    private final float[] center = new float[SEGMENTS + 1];
    private final float[] radius = new float[SEGMENTS + 1];
    private final CrestFoamSimulation simulation;
    private float width;
    private float depth;
    private float fade;
    private float shoreSpread;
    private int tile;

    CoastalWaveShaderMesh(float seed) {
        this.seed = seed;
        simulation = new CrestFoamSimulation(seed);
    }

    public static void invalidate() {
        if (foamTexture != null) foamTexture.deleteGlTexture();
        foamTexture = null;
        pixels = null;
        CrestFoamAppearance.invalidate();
    }

    static void beginFrame(Minecraft mc) {
        if (foamTexture == null) {
            foamTexture = new DynamicTexture(ATLAS_WIDTH, ATLAS_HEIGHT);
            pixels = foamTexture.getTextureData();
            mc.getTextureManager()
                .loadTexture(FOAM_LOCATION, foamTexture);
        }
        CrestFoamAppearance.ensureLoaded(mc);
        used = 0;
    }

    void prepare(float age, float shoreAge, float alpha, float waveWidth, float waveDepth) {
        width = waveWidth * 0.88F;
        depth = waveDepth;
        fade = smooth(alpha);
        shoreSpread = smooth(shoreAge / 24.0F);
        float time = age * 0.05F;
        for (int i = 0; i <= SEGMENTS; i++) {
            float u = i / (float) SEGMENTS;
            float side = u * 2.0F - 1.0F;
            float arch = (float) Math.sqrt(Math.max(0.0F, 1.0F - side * side));
            // A bowed front and swept-back, narrowing tips give the swash a crescent silhouette.
            center[i] = 0.36F + (1.0F - arch) * 0.36F + (noise(u * 5.0F + seed, time * 0.65F) - 0.5F) * 0.035F * arch;
            radius[i] = (0.18F + 0.025F * noise(u * 7.0F + seed, time * 0.35F + 17.0F)) * arch
                * (1.0F + shoreSpread * 0.6F);
        }
        simulation.advance(age, shoreAge, width, depth * 0.36F);
        tile = used < MAX_TILES ? used++ : -1;
        if (tile >= 0) writeFoam();
    }

    private void writeFoam() {
        int ox = tile % COLUMNS * TILE_WIDTH;
        int oy = tile / COLUMNS * TILE_HEIGHT;
        for (int y = 0; y < TEXTURE_HEIGHT; y++) {
            float across = y / 32.0F - 1.0F;
            for (int x = 0; x < TEXTURE_WIDTH; x++) {
                float u = x / 256.0F;
                int at = y * TEXTURE_WIDTH + x;
                float amount = simulation.amount(at);
                float pattern = CrestFoamAppearance.sample(simulation.materialU(at), simulation.materialV(at));
                float trailing = smooth((1.0F - across) * 2.0F);
                float ends = smooth(Math.min(u, 1.0F - u) * 9.0F);
                float alpha = CrestFoamAppearance.alpha(amount, pattern) * trailing * ends * fade;
                // Alpha carries every spatial fade, including for packs with flat particle colors.
                if (x == 0 || x == TEXTURE_WIDTH - 1 || y == 0 || y == TEXTURE_HEIGHT - 1) alpha = 0.0F;
                pixels[(oy + y + 1) * ATLAS_WIDTH + ox + x + 1] = ((int) (Math.min(1.0F, alpha) * 255.0F) << 24)
                    | 0xFCFEFF;
            }
        }
        for (int x = 0; x < TILE_WIDTH; x++) {
            pixels[oy * ATLAS_WIDTH + ox + x] = 0xFCFEFF;
            pixels[(oy + TILE_HEIGHT - 1) * ATLAS_WIDTH + ox + x] = 0xFCFEFF;
        }
        for (int y = 1; y < TILE_HEIGHT - 1; y++) {
            pixels[(oy + y) * ATLAS_WIDTH + ox] = 0xFCFEFF;
            pixels[(oy + y) * ATLAS_WIDTH + ox + TILE_WIDTH - 1] = 0xFCFEFF;
        }
    }

    static void uploadFoam(Minecraft mc) {
        if (used == 0) return;
        TextureUtil.uploadTexture(
            foamTexture.getGlTextureId(),
            pixels,
            ATLAS_WIDTH,
            ((used + COLUMNS - 1) / COLUMNS) * TILE_HEIGHT);
        mc.getTextureManager()
            .bindTexture(FOAM_LOCATION);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    void render(Tessellator t, double x, double y, double z, double dirX, double dirZ) {
        if (fade <= 0.001F || width <= 0.0F || depth <= 0.0F || tile < 0) return;
        t.setNormal(0.0F, 1.0F, 0.0F);
        for (int i = 0; i < SEGMENTS; i++) {
            for (int j = 0; j < DEPTH_SEGMENTS; j++) {
                float a = j * 2.0F / DEPTH_SEGMENTS - 1.0F;
                float b = (j + 1) * 2.0F / DEPTH_SEGMENTS - 1.0F;
                vertex(t, i, a, x, y, z, dirX, dirZ);
                vertex(t, i, b, x, y, z, dirX, dirZ);
                vertex(t, i + 1, b, x, y, z, dirX, dirZ);
                vertex(t, i + 1, a, x, y, z, dirX, dirZ);
            }
        }
    }

    private void vertex(Tessellator t, int i, float across, double x, double y, double z, double dirX, double dirZ) {
        double u = i / (double) SEGMENTS;
        double v = center[i] + across * radius[i];
        double tu = (tile % COLUMNS * TILE_WIDTH + 1.5D + u * (TEXTURE_WIDTH - 1)) / ATLAS_WIDTH;
        double tv = (tile / COLUMNS * TILE_HEIGHT + 1.5D + (across + 1.0D) * (TEXTURE_HEIGHT - 1) * 0.5D)
            / ATLAS_HEIGHT;
        t.addVertexWithUV(
            x - dirZ * (u - 0.5D) * width + dirX * (0.5D - v) * depth,
            y + 0.004D,
            z + dirX * (u - 0.5D) * width + dirZ * (0.5D - v) * depth,
            tu,
            tv);
    }

    private static float smooth(float x) {
        x = Math.max(0.0F, Math.min(1.0F, x));
        return x * x * (3.0F - 2.0F * x);
    }

    private static float[] makeNoise() {
        float[] values = new float[64 * 64];
        java.util.Random random = new java.util.Random(0xC0A57L);
        for (int i = 0; i < values.length; i++) values[i] = random.nextFloat();
        return values;
    }

    private static float noise(float x, float y) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        float fx = smooth(x - ix), fy = smooth(y - iy);
        float a = NOISE[(iy & 63) * 64 + (ix & 63)];
        float b = NOISE[(iy & 63) * 64 + ((ix + 1) & 63)];
        float c = NOISE[((iy + 1) & 63) * 64 + (ix & 63)];
        float d = NOISE[((iy + 1) & 63) * 64 + ((ix + 1) & 63)];
        return (a + (b - a) * fx) * (1.0F - fy) + (c + (d - c) * fx) * fy;
    }
}
