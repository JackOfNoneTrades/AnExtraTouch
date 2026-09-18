package org.fentanylsolutions.anextratouch.handlers.client.camera;

import java.util.concurrent.ThreadLocalRandom;

import org.fentanylsolutions.anextratouch.Config;
import org.joml.SimplexNoise;
import org.joml.Vector3d;

public final class ScreenShakeManager {

    public static final class Slot {

        public float trauma;
        public float radius;
        public float frequency;
        public float lengthInSeconds;
        public final Vector3d position = new Vector3d(Double.POSITIVE_INFINITY);
        double startTime;
        short version = 1;
        private float noiseOffsetX;
        private float noiseOffsetY;
        private float noiseOffsetZ;

        public boolean hasPosition() {
            return position.isFinite();
        }

        public void setDefaults(double time) {
            trauma = 0.5f;
            radius = 10.0f;
            frequency = 1.0f;
            lengthInSeconds = 2.0f;
            position.set(Double.POSITIVE_INFINITY);
            startTime = time;
            // Pick independent noise paths once per event, keeping motion smooth between frames.
            ThreadLocalRandom random = ThreadLocalRandom.current();
            noiseOffsetX = random.nextFloat() * 256f;
            noiseOffsetY = random.nextFloat() * 256f;
            noiseOffsetZ = random.nextFloat() * 256f;
        }
    }

    private static final Slot DUMMY = new Slot();
    private static final Slot[] instances = new Slot[64];
    private static long instanceMask;
    private static double lastTime;

    // calculated offset for the current frame
    private static double offsetX;
    private static double offsetY;
    private static double offsetZ;

    private ScreenShakeManager() {}

    private static int extractIndex(long handle) {
        return (int) handle;
    }

    private static int extractVersion(long handle) {
        return (int) (handle >> 32);
    }

    private static long constructHandle(int index, int version) {
        return ((long) index) | ((long) version << 32);
    }

    private static boolean isHandleValid(long handle) {
        return handle != 0L && instances[extractIndex(handle)] != null
            && instances[extractIndex(handle)].version == extractVersion(handle);
    }

    public static Slot get(long handle) {
        return isHandleValid(handle) ? instances[extractIndex(handle)] : DUMMY;
    }

    public static Slot createDirect() {
        return get(create());
    }

    public static long create() {
        if (instanceMask == Long.MAX_VALUE) return 0L;

        int index = Long.numberOfTrailingZeros(~instanceMask);
        if (instances[index] == null) instances[index] = new Slot();
        int version = instances[index].version;

        instanceMask |= 1L << index;
        instances[index].setDefaults(lastTime);

        return constructHandle(index, version);
    }

    public static long recreate(long handle) {
        if (!isHandleValid(handle)) return create();
        get(handle).setDefaults(lastTime);
        return handle;
    }

    public static void onCameraUpdate(double time, double deltaTime, Vector3d cameraPos) {
        lastTime = time;

        float maxIntensity = Config.cameraShakeMaxIntensity;
        float maxFrequency = Config.cameraShakeMaxFrequency;

        long mask = instanceMask;
        float total = 0f;
        double noiseX = 0, noiseY = 0, noiseZ = 0;

        while (mask != 0L) {
            int index = Long.numberOfTrailingZeros(mask);
            mask &= ~(1L << index);

            Slot ss = instances[index];
            float progress = ss.lengthInSeconds > 0f
                ? (float) Math.min(Math.max((time - ss.startTime) / ss.lengthInSeconds, 0.0), 1.0)
                : 1f;

            if (progress >= 1f) {
                // free stale entry
                instanceMask &= ~(1L << index);
                ss.version++;
                continue;
            }

            float decay = 1f - progress;
            float intensity = Math.min(Math.max(ss.trauma, 0f), 1f) * (decay * decay);

            if (ss.hasPosition()) {
                float distance = (float) cameraPos.distance(ss.position);
                float distanceFactor = 1f - Math.min(1f, distance / ss.radius);
                intensity *= (distanceFactor * distanceFactor);
            }

            if (intensity <= 0f || !Float.isFinite(intensity)) {
                continue;
            }

            float sampleStep = (float) ((time - ss.startTime) * maxFrequency * ss.frequency);
            noiseX += SimplexNoise.noise(sampleStep, ss.noiseOffsetX) * intensity;
            noiseY += SimplexNoise.noise(sampleStep, ss.noiseOffsetY) * intensity;
            noiseZ += SimplexNoise.noise(sampleStep, ss.noiseOffsetZ) * intensity;
            total += intensity;
        }

        // normalize
        if (total > 1.0f) {
            noiseX /= total;
            noiseY /= total;
            noiseZ /= total;
        }

        offsetX = noiseX * maxIntensity;
        offsetY = noiseY * maxIntensity;
        offsetZ = noiseZ * maxIntensity;
    }

    public static double getOffsetX() {
        return offsetX;
    }

    public static double getOffsetY() {
        return offsetY;
    }

    public static double getOffsetZ() {
        return offsetZ;
    }
}
