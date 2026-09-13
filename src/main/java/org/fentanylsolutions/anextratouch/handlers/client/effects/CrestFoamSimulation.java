package org.fentanylsolutions.anextratouch.handlers.client.effects;

/**
 * CPU adaptation of Crest's UpdateFoam.compute and OceanFoam.hlsl (WhiteFoamTexture).
 * Copyright (c) 2019 Wave Harmonic and contributors. MIT license: assets/anextratouch/licenses/crest.txt.
 * Upstream revision: db0658ff0b2e93e4a9e28cc2867509658b0ecc00, https://github.com/wave-harmonic/crest.
 *
 * AET supplies a local breaking-wave displacement and flow instead of Crest's Unity ocean LOD textures.
 */
final class CrestFoamSimulation {

    static final int WIDTH = 257;
    static final int HEIGHT = 65;
    private static final float DT = 0.05F;
    private static final float FADE_RATE = 0.7F;
    private static final float WAVE_STRENGTH = 0.45F;
    private static final float WAVE_COVERAGE = 0.8F;
    private static final float FOAM_FEATHER = 0.18F;
    private static final float DETAIL_SCALE = 2.4F; // Blocks covered by one repeat of Crest's foam texture.
    private static final int AMOUNT = 0, MATERIAL_U = 1, MATERIAL_V = 2;
    private static final float[] COMPRESSION = makeCompressionProfile();

    // Carry material coordinates with the density so fine foam detail survives advection without blur.
    private float[][] current = new float[3][WIDTH * HEIGHT];
    private float[][] previous = new float[3][WIDTH * HEIGHT];
    private float[][] next = new float[3][WIDTH * HEIGHT];
    private final float seed;
    private int simulatedTick = -1;
    private float blend;
    private float cellU;
    private float cellV;

    CrestFoamSimulation(float seed) {
        this.seed = seed;
    }

    void advance(float age, float shoreAge, float width, float depth) {
        int targetTick = Math.max(0, (int) Math.floor(age));
        cellU = width / ((WIDTH - 1) * DETAIL_SCALE);
        cellV = depth / ((HEIGHT - 1) * DETAIL_SCALE);
        if (simulatedTick < 0 || targetTick < simulatedTick || targetTick - simulatedTick > 20) {
            simulatedTick = Math.max(-1, targetTick - 20);
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int at = y * WIDTH + x;
                    current[AMOUNT][at] = 0.0F;
                    current[MATERIAL_U][at] = x * cellU + seed;
                    current[MATERIAL_V][at] = y * cellV - simulatedTick * 2.0F * cellV + seed * 0.37F;
                }
            }
        }
        while (simulatedTick < targetTick) {
            simulatedTick++;
            step(simulatedTick * DT, Math.max(0.0F, shoreAge - (age - simulatedTick)));
        }
        blend = age - (float) Math.floor(age);
    }

    private void step(float time, float shoreAge) {
        float breaking = 1.0F - smooth(shoreAge / 8.0F);
        float wash = smooth(shoreAge / 20.0F);
        for (int x = 0; x < WIDTH; x++) {
            float u = x / (float) (WIDTH - 1);
            float surge = (float) Math.sin(u * 14.0F - time * 2.0F + seed);
            float lip = 14.0F + 2.5F * (float) Math.sin(u * 9.0F + time * 1.2F + seed);
            float sharpness = 1.15F + 0.4F * surge + 0.2F * (float) Math.sin(u * 31.0F + time * 3.0F + seed);
            float flowX = (float) Math.sin(u * 12.0F - time * 0.8F + seed) * (8.0F + wash * 6.0F);
            float flowY = 36.0F + surge * 8.0F + wash * 16.0F;
            for (int y = 0; y < HEIGHT; y++) {
                int at = y * WIDTH + x;
                float sx = x - DT * flowX * (0.5F + y / (float) (HEIGHT - 1));
                float sy = y - DT * flowY;
                // Crest: backtrace the previous foam by flow, then dissipate accumulated foam.
                float amount = sample(current[AMOUNT], sx, sy, 0.0F) * Math.max(0.0F, 1.0F - FADE_RATE * DT);

                // The longitudinal displacement is -sharpness * q * exp(-q*q/2).
                // Its Jacobian determinant drops below one as the breaking lip compresses.
                // Transverse displacement is zero, so the 2D determinant reduces to this derivative.
                float q = Math.abs((y - lip) / 5.0F);
                float determinant = 1.0F - sharpness * compression(q);
                amount += 5.0F * DT * WAVE_STRENGTH * saturate(WAVE_COVERAGE - determinant) * breaking;
                next[AMOUNT][at] = saturate(amount);

                // Coordinates enter at the upstream boundary and follow the same flow as the foam.
                // There is no independent, scrolling detail layer in the display pass.
                float inflowU = sx * cellU + seed;
                float inflowV = (sy - (time / DT - 1.0F) * 2.0F) * cellV + seed * 0.37F;
                next[MATERIAL_U][at] = sample(current[MATERIAL_U], sx, sy, inflowU);
                next[MATERIAL_V][at] = sample(current[MATERIAL_V], sx, sy, inflowV);
            }
        }
        float[][] spare = previous;
        previous = current;
        current = next;
        next = spare;
    }

    float amount(int at) {
        return interpolate(AMOUNT, at);
    }

    float materialU(int at) {
        return interpolate(MATERIAL_U, at);
    }

    float materialV(int at) {
        return interpolate(MATERIAL_V, at);
    }

    private float interpolate(int channel, int at) {
        return previous[channel][at] + (current[channel][at] - previous[channel][at]) * blend;
    }

    // Crest WhiteFoamTexture's black-point fade: shrinking foam reveals holes and thin bubble walls.
    static float whiteFoam(float amount, float texture) {
        float blackPoint = saturate(1.0F - amount);
        return smooth((texture - blackPoint) / FOAM_FEATHER);
    }

    private static float sample(float[] field, float x, float y, float outside) {
        if (x < 0.0F || y < 0.0F || x >= WIDTH - 1 || y >= HEIGHT - 1) return outside;
        int ix = (int) x, iy = (int) y;
        float fx = x - ix, fy = y - iy;
        int at = iy * WIDTH + ix;
        return (field[at] * (1.0F - fx) + field[at + 1] * fx) * (1.0F - fy)
            + (field[at + WIDTH] * (1.0F - fx) + field[at + WIDTH + 1] * fx) * fy;
    }

    private static float saturate(float x) {
        return Math.max(0.0F, Math.min(1.0F, x));
    }

    private static float compression(float q) {
        // The shoulders stretch rather than compress, so they cannot deposit foam.
        if (q >= 1.0F) return 0.0F;
        float at = q * (COMPRESSION.length - 1);
        int i = (int) at;
        return COMPRESSION[i] + (COMPRESSION[i + 1] - COMPRESSION[i]) * (at - i);
    }

    private static float[] makeCompressionProfile() {
        float[] profile = new float[513];
        for (int i = 0; i < profile.length; i++) {
            float q = i / (float) (profile.length - 1);
            profile[i] = (1.0F - q * q) * (float) Math.exp(-0.5F * q * q);
        }
        return profile;
    }

    private static float smooth(float x) {
        x = saturate(x);
        return x * x * (3.0F - 2.0F * x);
    }
}
