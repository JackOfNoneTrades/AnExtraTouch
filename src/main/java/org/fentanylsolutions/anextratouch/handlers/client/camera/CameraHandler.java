package org.fentanylsolutions.anextratouch.handlers.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.anextratouch.Config;
import org.joml.SimplexNoise;
import org.joml.Vector3d;

public final class CameraHandler {

    // context config selected per-frame based on entity state
    public static final class ContextConfig {

        public final float strafingRoll;
        public final float forwardPitch;
        public final float verticalPitch;
        public final float horizSmoothing;
        public final float vertSmoothing;

        public ContextConfig(float strafingRoll, float forwardPitch, float verticalPitch, float horizSmoothing,
            float vertSmoothing) {
            this.strafingRoll = strafingRoll;
            this.forwardPitch = forwardPitch;
            this.verticalPitch = verticalPitch;
            this.horizSmoothing = horizSmoothing;
            this.vertSmoothing = vertSmoothing;
        }
    }

    // Time tracking
    private static long lastNanoTime;
    private static double accumulatedTime;
    private static double deltaTime;
    private static boolean initialized;
    private static int lastEntityId = Integer.MIN_VALUE;

    // Previous frame state
    private static final Vector3d prevCameraEulerRot = new Vector3d();
    private static final Vector3d prevEntityVelocity = new Vector3d();
    private static double lastActionTime;
    private static int prevThirdPersonView = -1;

    // Smoothed offsets
    private static double prevVerticalPitchOffset;
    private static double prevForwardPitchOffset;
    private static double prevStrafingRollOffset;

    // Turning roll
    private static double turningRollTargetOffset;

    // Camera sway
    private static final double CAMERASWAY_FADING_SMOOTHNESS = 3.0;
    private static double cameraSwayFactor;
    private static double cameraSwayFactorTarget;

    // Output
    private static float pitchOffset;
    private static float yawOffset;
    private static float rollOffset;

    private CameraHandler() {}

    public static void update(EntityLivingBase entity, float partialTicks) {
        if (Config.cameraDisableWhilePaused && Minecraft.getMinecraft()
            .isGamePaused()) {
            lastNanoTime = 0L;
            pitchOffset = 0.0f;
            yawOffset = 0.0f;
            rollOffset = 0.0f;
            return;
        }

        // Delta time
        long now = System.nanoTime();
        if (lastNanoTime == 0) lastNanoTime = now;
        deltaTime = (now - lastNanoTime) / 1_000_000_000.0;
        accumulatedTime += deltaTime;
        lastNanoTime = now;

        if (deltaTime <= 0.0) {
            return;
        }

        // Context selection
        ContextConfig ctx;
        if (entity.ridingEntity != null) {
            if (entity.ridingEntity instanceof EntityLivingBase) {
                ctx = new ContextConfig(
                    Config.cameraWalkStrafingRoll * 2.0f,
                    Config.cameraWalkForwardPitch,
                    Config.cameraWalkVerticalPitch,
                    Config.cameraWalkHorizSmoothing,
                    Config.cameraWalkVertSmoothing);
            } else {
                ctx = new ContextConfig(
                    Config.cameraRideStrafingRoll,
                    Config.cameraRideForwardPitch,
                    Config.cameraRideVerticalPitch,
                    Config.cameraRideHorizSmoothing,
                    Config.cameraRideVertSmoothing);
            }
        } else {
            ctx = new ContextConfig(
                Config.cameraWalkStrafingRoll,
                Config.cameraWalkForwardPitch,
                Config.cameraWalkVerticalPitch,
                Config.cameraWalkHorizSmoothing,
                Config.cameraWalkVertSmoothing);
        }

        // current camera rotation (interpolated)
        // when decoupled, entity rotation is already swapped to camera rotation by MixinEntityRenderer HEAD
        double currentYaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        double currentPitch = entity.prevRotationPitch
            + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

        // velocity (while riding, use controlled entity velocity like og mod)
        EntityLivingBase velocitySource = entity;
        if (entity.ridingEntity instanceof EntityLivingBase) {
            velocitySource = (EntityLivingBase) entity.ridingEntity;
        }
        double motionX = velocitySource.motionX;
        double motionY = velocitySource.motionY;
        double motionZ = velocitySource.motionZ;

        // initialize/reset state when camera entity changes (world load/respawn)
        // to avoid a fake turning roll impulse on first frame.
        int entityId = entity.getEntityId();
        if (!initialized || entityId != lastEntityId) {
            initialized = true;
            lastEntityId = entityId;

            prevCameraEulerRot.set(currentPitch, currentYaw, 0.0);
            prevEntityVelocity.set(motionX, motionY, motionZ);
            turningRollTargetOffset = 0.0;
            prevVerticalPitchOffset = 0.0;
            prevForwardPitchOffset = 0.0;
            prevStrafingRollOffset = 0.0;
            cameraSwayFactor = 0.0;
            cameraSwayFactorTarget = 0.0;
            lastActionTime = accumulatedTime;
            prevThirdPersonView = net.minecraft.client.Minecraft.getMinecraft().gameSettings.thirdPersonView;
            pitchOffset = 0.0f;
            yawOffset = 0.0f;
            rollOffset = 0.0f;
            return;
        }

        // player action (movement or look change)
        if (motionX != prevEntityVelocity.x || motionY != prevEntityVelocity.y
            || motionZ != prevEntityVelocity.z
            || currentYaw != prevCameraEulerRot.y
            || currentPitch != prevCameraEulerRot.x) {
            lastActionTime = accumulatedTime;
        }

        // forward-relative velocity (rotate by -yaw)
        // res: x = strafe (left/right), z = forward/backward
        double rad = Math.toRadians(360.0 - currentYaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double strafeVel = cos * motionX - sin * motionZ;
        double forwardVel = sin * motionX + cos * motionZ;

        // calculate effects
        double pitchX = 0;
        double yawY = 0;
        double rollZ = 0;

        // vertical velocity pitch
        {
            double smoothing = 0.00004 * ctx.vertSmoothing;
            double target = motionY * ctx.verticalPitch;
            prevVerticalPitchOffset = damp(prevVerticalPitchOffset, target, smoothing, deltaTime);
            pitchX += prevVerticalPitchOffset;
        }

        // forward velocity pitch
        {
            double smoothing = 0.008 * ctx.horizSmoothing;
            double target = forwardVel * ctx.forwardPitch;
            prevForwardPitchOffset = damp(prevForwardPitchOffset, target, smoothing, deltaTime);
            pitchX += prevForwardPitchOffset;
        }

        // turning roll
        {
            double decaySmoothing = 0.0825 * Config.cameraTurningRollSmoothing;
            double intensity = 1.25 * Config.cameraTurningRollIntensity;
            double accumulation = 0.0048 * Config.cameraTurningRollAccumulation;
            double yawDelta = prevCameraEulerRot.y - currentYaw;

            // don't spaz out when switching perspectives
            int thirdPerson = net.minecraft.client.Minecraft.getMinecraft().gameSettings.thirdPersonView;
            if (thirdPerson != prevThirdPersonView && prevThirdPersonView != -1) yawDelta = 0;
            prevThirdPersonView = thirdPerson;

            // decay
            turningRollTargetOffset = damp(turningRollTargetOffset, 0, decaySmoothing, deltaTime);
            // accumulation
            turningRollTargetOffset = clamp(turningRollTargetOffset + (yawDelta * accumulation), -1.0, 1.0);
            // apply with easing
            double eased = easeInOutCubic(Math.abs(turningRollTargetOffset));
            rollZ += clamp01(eased) * intensity * Math.signum(turningRollTargetOffset);
        }

        // strafing roll
        {
            double smoothing = 0.008 * ctx.horizSmoothing;
            double target = -strafeVel * ctx.strafingRoll;
            prevStrafingRollOffset = damp(prevStrafingRollOffset, target, smoothing, deltaTime);
            rollZ += prevStrafingRollOffset;
        }

        // idle camera sway
        {
            float noiseX = (float) (accumulatedTime * Config.cameraSwayFrequency);

            if ((accumulatedTime - lastActionTime) < Config.cameraSwayFadeInDelay) {
                cameraSwayFactorTarget = 0;
            } else if (cameraSwayFactor == cameraSwayFactorTarget) {
                cameraSwayFactorTarget = 1;
            }

            double fadeLength = cameraSwayFactorTarget > 0 ? Config.cameraSwayFadeInLength
                : Config.cameraSwayFadeOutLength;
            double fadeStep = fadeLength > 0.0 ? deltaTime / fadeLength : 1.0;
            cameraSwayFactor = stepTowards(cameraSwayFactor, cameraSwayFactorTarget, fadeStep);

            double scaledIntensity = Config.cameraSwayIntensity
                * Math.pow(cameraSwayFactor, CAMERASWAY_FADING_SMOOTHNESS);

            // original target is (intensity, intensity, 0), sway affects pitch and yaw only, not roll
            pitchX += SimplexNoise.noise(noiseX, 420) * scaledIntensity;
            yawY += SimplexNoise.noise(noiseX, 1337) * scaledIntensity;
        }

        // in-air falling shake, noise-based
        boolean isFlyingPlayer = entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.allowFlying
            && ((EntityPlayer) entity).capabilities.isFlying;
        if (Config.cameraFallingShakeEnabled && !isFlyingPlayer
            && !entity.onGround
            && motionY < -0.02D
            && entity.fallDistance >= Config.cameraFallingShakeMinDistance) {
            double denom = Math
                .max(0.0001D, Config.cameraFallingShakeMaxDistance - Config.cameraFallingShakeMinDistance);
            double t = (entity.fallDistance - Config.cameraFallingShakeMinDistance) / denom;
            t = clamp01(t);

            double intensity = Config.cameraFallingShakeIntensity * (t * t);
            float noiseX = (float) (accumulatedTime * Config.cameraFallingShakeFrequency);

            // same style as idle sway, just stronger and tied to fall buildup
            pitchX += SimplexNoise.noise(noiseX, 9001) * intensity;
            yawY += SimplexNoise.noise(noiseX, 13337) * intensity;
        }

        // screen shakes
        {
            Vector3d cameraPos = new Vector3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
            ScreenShakeManager.onCameraUpdate(accumulatedTime, deltaTime, cameraPos);
            pitchX += ScreenShakeManager.getOffsetX();
            yawY += ScreenShakeManager.getOffsetY();
            rollZ += ScreenShakeManager.getOffsetZ();
        }

        // storing previous frame state
        prevEntityVelocity.set(motionX, motionY, motionZ);
        prevCameraEulerRot.set(currentPitch, currentYaw, 0);

        pitchOffset = (float) pitchX;
        yawOffset = (float) yawY;
        rollOffset = (float) rollZ;
    }

    public static float getPitchOffset() {
        return pitchOffset;
    }

    public static float getYawOffset() {
        return yawOffset;
    }

    public static float getRollOffset() {
        return rollOffset;
    }

    public static void notifyOfPlayerAction() {
        lastActionTime = accumulatedTime;
    }

    // mafs

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static double clamp01(double value) {
        return value < 0 ? 0 : (value > 1 ? 1 : value);
    }

    private static double lerp(double a, double b, double t) {
        t = clamp01(t);
        return a + (b - a) * t;
    }

    private static double damp(double source, double destination, double smoothing, double dt) {
        return lerp(source, destination, 1.0 - Math.pow(smoothing * smoothing, dt));
    }

    private static double stepTowards(double current, double target, double step) {
        if (current < target) {
            return Math.min(current + step, target);
        } else if (current > target) {
            return Math.max(current - step, target);
        }
        return current;
    }

    private static double easeInOutCubic(double x) {
        return x < 0.5 ? (4 * x * x * x) : (1 - Math.pow(-2 * x + 2, 3) / 2);
    }
}
