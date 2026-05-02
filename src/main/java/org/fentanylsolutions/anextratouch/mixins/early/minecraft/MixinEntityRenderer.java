package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.footsteps.FootprintManager;
import org.fentanylsolutions.anextratouch.handlers.client.camera.CameraHandler;
import org.fentanylsolutions.anextratouch.handlers.client.camera.DecoupledCameraHandler;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterRippleManager;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterSplashManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow
    private Minecraft mc;
    @Unique
    private boolean anextratouch$renderingHand;

    // Saved entity rotation for HEAD/RETURN swap
    @Unique
    private float anextratouch$savedYaw;
    @Unique
    private float anextratouch$savedPitch;
    @Unique
    private float anextratouch$savedPrevYaw;
    @Unique
    private float anextratouch$savedPrevPitch;
    @Unique
    private boolean anextratouch$rotationSwapped;

    // Camera distance smoothing for clipping prevention
    @Unique
    private static final FloatBuffer anextratouch$matBuf = BufferUtils.createFloatBuffer(16);
    @Unique
    private float anextratouch$smoothedCamDistPrev = -1f;
    @Unique
    private float anextratouch$smoothedCamDistCurr = -1f;
    @Unique
    private int anextratouch$smoothedCamDistTick = Integer.MIN_VALUE;

    // Smooth camera follow (positional lag)
    @Unique
    private double anextratouch$followPrevX, anextratouch$followPrevY, anextratouch$followPrevZ;
    @Unique
    private double anextratouch$followCurrX, anextratouch$followCurrY, anextratouch$followCurrZ;
    @Unique
    private boolean anextratouch$followInit;
    @Unique
    private int anextratouch$followTick = Integer.MIN_VALUE;

    /**
     * Swaps entity rotation to decoupled camera rotation. Call anextratouch$restoreRotation() after.
     */
    @Unique
    private void anextratouch$swapRotation() {
        anextratouch$rotationSwapped = false;
        if (!DecoupledCameraHandler.isActive()) return;
        if (DecoupledCameraHandler.isAimFirstPerson()) return; // vanilla FP handles rotation

        EntityLivingBase entity = mc.renderViewEntity;
        if (entity == null) return;

        anextratouch$savedYaw = entity.rotationYaw;
        anextratouch$savedPitch = entity.rotationPitch;
        anextratouch$savedPrevYaw = entity.prevRotationYaw;
        anextratouch$savedPrevPitch = entity.prevRotationPitch;

        entity.rotationYaw = DecoupledCameraHandler.getEffectiveYaw();
        entity.rotationPitch = DecoupledCameraHandler.getEffectivePitch();
        entity.prevRotationYaw = DecoupledCameraHandler.getEffectivePrevYaw();
        entity.prevRotationPitch = DecoupledCameraHandler.getEffectivePrevPitch();

        anextratouch$rotationSwapped = true;
    }

    @Unique
    private void anextratouch$restoreRotation() {
        if (!anextratouch$rotationSwapped) return;

        EntityLivingBase entity = mc.renderViewEntity;
        if (entity == null) return;

        entity.rotationYaw = anextratouch$savedYaw;
        entity.rotationPitch = anextratouch$savedPitch;
        entity.prevRotationYaw = anextratouch$savedPrevYaw;
        entity.prevRotationPitch = anextratouch$savedPrevPitch;
        anextratouch$rotationSwapped = false;
    }

    /**
     * Before orientCamera: swap entity rotation to decoupled camera rotation.
     * This makes ShoulderSurfing's ASM-injected offsetCamera() and calcCameraDistance()
     * use the camera direction for collision raycasts and shoulder offset positioning.
     */
    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void anextratouch$beforeOrientCamera(float partialTicks, CallbackInfo ci) {
        anextratouch$swapRotation();
    }

    @Inject(method = "orientCamera", at = @At("RETURN"))
    private void anextratouch$onOrientCamera(float partialTicks, CallbackInfo ci) {
        // Smooth camera distance changes from clipping prevention
        anextratouch$smoothCameraClipping(partialTicks);
        // Smooth camera follow (positional lag behind entity)
        anextratouch$smoothCameraFollow(mc.renderViewEntity, partialTicks);
        // Vertical camera offset (compensates for mods like DBC/JBRA that alter yOffset during rendering)
        anextratouch$applyCameraVerticalOffset();
        // Extract camera world position from GL matrix before overhaul modifies rotation.
        // This accounts for ShoulderSurfing's shoulder offset so aim raytraces originate
        // from the actual camera position, not the player's eye.
        if (DecoupledCameraHandler.isActive()) {
            DecoupledCameraHandler.updateCameraPosition(partialTicks);
        }
        anextratouch$applyCameraOverhaul(mc.renderViewEntity, partialTicks);
        // Aim-to-first-person transition: smoothly move camera from third-person to entity eye
        if (DecoupledCameraHandler.isActive() && mc.renderViewEntity != null) {
            DecoupledCameraHandler.applyAimTransition(partialTicks, mc.renderViewEntity);
        }
        // Compute final camera-to-entity distance for player fade
        anextratouch$updateFadeDistance();
        // Store camera world position + orientation for sound centering (before rotation restore)
        DecoupledCameraHandler.updateSoundListener(partialTicks, mc.renderViewEntity);
        anextratouch$restoreRotation();
    }

    /**
     * Before getMouseOver: swap entity rotation so raycasting uses camera direction.
     * This makes mc.objectMouseOver, the crosshair, and interaction target the camera's look direction.
     */
    @Inject(method = "getMouseOver", at = @At("HEAD"))
    private void anextratouch$beforeGetMouseOver(float partialTicks, CallbackInfo ci) {
        anextratouch$swapRotation();
    }

    @Inject(method = "getMouseOver", at = @At("RETURN"))
    private void anextratouch$afterGetMouseOver(float partialTicks, CallbackInfo ci) {
        anextratouch$restoreRotation();
    }

    /**
     * Smooths camera distance changes caused by clipping prevention.
     * Instead of snapping the camera forward/backward when hitting geometry,
     * the distance is interpolated using the configured smoothing factor.
     */
    @Unique
    private void anextratouch$smoothCameraClipping(float partialTicks) {
        float smoothing = Config.cameraClippingSmoothing;
        if (smoothing <= 0f || mc.gameSettings.thirdPersonView == 0) {
            anextratouch$smoothedCamDistPrev = -1f;
            anextratouch$smoothedCamDistCurr = -1f;
            anextratouch$smoothedCamDistTick = Integer.MIN_VALUE;
            return;
        }

        anextratouch$matBuf.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, anextratouch$matBuf);

        float m12 = anextratouch$matBuf.get(12);
        float m13 = anextratouch$matBuf.get(13);
        float m14 = anextratouch$matBuf.get(14);
        float dist = (float) Math.sqrt(m12 * m12 + m13 * m13 + m14 * m14);

        if (anextratouch$smoothedCamDistCurr < 0f || dist < 0.001f) {
            anextratouch$smoothedCamDistPrev = dist;
            anextratouch$smoothedCamDistCurr = dist;
            EntityLivingBase entity = mc.renderViewEntity;
            anextratouch$smoothedCamDistTick = entity != null ? entity.ticksExisted : Integer.MIN_VALUE;
            return;
        }

        EntityLivingBase entity = mc.renderViewEntity;
        int tick = entity != null ? entity.ticksExisted : Integer.MIN_VALUE;

        if (tick != anextratouch$smoothedCamDistTick) {
            anextratouch$smoothedCamDistTick = tick;
            anextratouch$smoothedCamDistPrev = anextratouch$smoothedCamDistCurr;
            float smoothed = anextratouch$smoothedCamDistCurr * smoothing + dist * (1f - smoothing);
            // Never exceed the clipped distance (would clip into walls).
            // Snap in instantly, smooth out gradually.
            if (smoothed > dist) {
                smoothed = dist;
            }
            anextratouch$smoothedCamDistCurr = smoothed;
        } else if (anextratouch$smoothedCamDistCurr > dist) {
            // Within the same tick/pass chain, still snap in immediately for clipping safety.
            anextratouch$smoothedCamDistPrev = Math.min(anextratouch$smoothedCamDistPrev, dist);
            anextratouch$smoothedCamDistCurr = dist;
        }

        float smoothed = anextratouch$smoothedCamDistPrev
            + (anextratouch$smoothedCamDistCurr - anextratouch$smoothedCamDistPrev) * partialTicks;
        if (smoothed > dist) {
            smoothed = dist;
        }

        if (Math.abs(smoothed - dist) < 0.001f) return;

        float scale = smoothed / dist;
        anextratouch$matBuf.put(12, m12 * scale);
        anextratouch$matBuf.put(13, m13 * scale);
        anextratouch$matBuf.put(14, m14 * scale);
        anextratouch$matBuf.position(0);
        GL11.glLoadMatrix(anextratouch$matBuf);
    }

    /**
     * Smooth camera follow: the camera tracks a smoothed position that lags behind the entity.
     * Creates a cinematic trailing effect in third person.
     */
    @Unique
    private void anextratouch$smoothCameraFollow(EntityLivingBase entity, float partialTicks) {
        float smoothing = Config.cameraFollowSmoothing;
        if (smoothing <= 0f || entity == null || mc.gameSettings.thirdPersonView == 0) {
            anextratouch$followInit = false;
            anextratouch$followTick = Integer.MIN_VALUE;
            return;
        }

        double renderX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double renderY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double renderZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;

        if (!anextratouch$followInit) {
            anextratouch$followPrevX = renderX;
            anextratouch$followPrevY = renderY;
            anextratouch$followPrevZ = renderZ;
            anextratouch$followCurrX = renderX;
            anextratouch$followCurrY = renderY;
            anextratouch$followCurrZ = renderZ;
            anextratouch$followTick = entity.ticksExisted;
            anextratouch$followInit = true;
            return;
        }

        // Update smoothing state once per game tick to avoid pass-to-pass feedback jitter.
        if (entity.ticksExisted != anextratouch$followTick) {
            anextratouch$followTick = entity.ticksExisted;
            anextratouch$followPrevX = anextratouch$followCurrX;
            anextratouch$followPrevY = anextratouch$followCurrY;
            anextratouch$followPrevZ = anextratouch$followCurrZ;

            // Snap on teleport (single-tick movement larger than any natural velocity).
            // Use the entity's per-tick delta as the signal, not the smoothing lag — a high-velocity
            // fall produces large steady-state lag without being a teleport, and snapping on it
            // would rubber-band the camera.
            double tdx = entity.posX - entity.prevPosX;
            double tdy = entity.posY - entity.prevPosY;
            double tdz = entity.posZ - entity.prevPosZ;
            if (tdx * tdx + tdy * tdy + tdz * tdz > 64.0) {
                anextratouch$followPrevX = renderX;
                anextratouch$followPrevY = renderY;
                anextratouch$followPrevZ = renderZ;
                anextratouch$followCurrX = renderX;
                anextratouch$followCurrY = renderY;
                anextratouch$followCurrZ = renderZ;
                return;
            }

            float factor = Math.max(1f - smoothing, 0.01f);
            anextratouch$followCurrX += (entity.posX - anextratouch$followCurrX) * factor;
            anextratouch$followCurrY += (entity.posY - anextratouch$followCurrY) * factor;
            anextratouch$followCurrZ += (entity.posZ - anextratouch$followCurrZ) * factor;
        }

        double followX = anextratouch$followPrevX
            + (anextratouch$followCurrX - anextratouch$followPrevX) * partialTicks;
        double followY = anextratouch$followPrevY
            + (anextratouch$followCurrY - anextratouch$followPrevY) * partialTicks;
        double followZ = anextratouch$followPrevZ
            + (anextratouch$followCurrZ - anextratouch$followPrevZ) * partialTicks;

        double offX = followX - renderX;
        double offY = followY - renderY;
        double offZ = followZ - renderZ;

        if (Math.abs(offX) + Math.abs(offY) + Math.abs(offZ) < 0.0001) return;

        // Convert world-space offset to view-space using modelview rotation
        anextratouch$matBuf.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, anextratouch$matBuf);

        float m0 = anextratouch$matBuf.get(0), m1 = anextratouch$matBuf.get(1), m2 = anextratouch$matBuf.get(2);
        float m4 = anextratouch$matBuf.get(4), m5 = anextratouch$matBuf.get(5), m6 = anextratouch$matBuf.get(6);
        float m8 = anextratouch$matBuf.get(8), m9 = anextratouch$matBuf.get(9), m10 = anextratouch$matBuf.get(10);
        float m12 = anextratouch$matBuf.get(12), m13 = anextratouch$matBuf.get(13), m14 = anextratouch$matBuf.get(14);

        // viewOffset = R * worldOffset
        float vx = (float) (m0 * offX + m4 * offY + m8 * offZ);
        float vy = (float) (m1 * offX + m5 * offY + m9 * offZ);
        float vz = (float) (m2 * offX + m6 * offY + m10 * offZ);

        // Apply as view-space translation (subtract: moving camera toward follow pos
        // means shifting the world in the opposite direction)
        anextratouch$matBuf.put(12, m12 - vx);
        anextratouch$matBuf.put(13, m13 - vy);
        anextratouch$matBuf.put(14, m14 - vz);
        anextratouch$matBuf.position(0);
        GL11.glLoadMatrix(anextratouch$matBuf);
    }

    /**
     * Applies a configurable vertical offset to the camera in world-space Y.
     * Compensates for mods like DBC/JBRA that alter entity yOffset during
     * the render cycle, which lowers the camera and causes ground clipping.
     */
    @Unique
    private void anextratouch$applyCameraVerticalOffset() {
        float offset = Config.cameraVerticalOffset;
        if (offset == 0f || mc.gameSettings.thirdPersonView == 0) {
            return;
        }
        anextratouch$matBuf.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, anextratouch$matBuf);
        // Column 1 of the modelview (m4, m5, m6) is the world-Y axis in view space.
        // Translating the camera up in world-Y means shifting the world down in view space.
        float m4 = anextratouch$matBuf.get(4), m5 = anextratouch$matBuf.get(5), m6 = anextratouch$matBuf.get(6);
        anextratouch$matBuf.put(12, anextratouch$matBuf.get(12) - m4 * offset);
        anextratouch$matBuf.put(13, anextratouch$matBuf.get(13) - m5 * offset);
        anextratouch$matBuf.put(14, anextratouch$matBuf.get(14) - m6 * offset);
        anextratouch$matBuf.position(0);
        GL11.glLoadMatrix(anextratouch$matBuf);
    }

    /**
     * Extracts the camera-to-entity distance from the final GL modelview matrix
     * (after all camera manipulations) and stores it for player fade rendering.
     */
    @Unique
    private void anextratouch$updateFadeDistance() {
        if (mc.gameSettings.thirdPersonView == 0 && !DecoupledCameraHandler.isActive()) {
            DecoupledCameraHandler.updateCameraEntityDistance(Float.MAX_VALUE);
            return;
        }
        anextratouch$matBuf.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, anextratouch$matBuf);
        float m12 = anextratouch$matBuf.get(12);
        float m13 = anextratouch$matBuf.get(13);
        float m14 = anextratouch$matBuf.get(14);
        DecoupledCameraHandler.updateCameraEntityDistance((float) Math.sqrt(m12 * m12 + m13 * m13 + m14 * m14));
    }

    @Unique
    private void anextratouch$applyCameraOverhaul(EntityLivingBase entity, float partialTicks) {
        if (!Config.cameraOverhaulEnabled) return;
        if (entity == null || entity.isPlayerSleeping()) return;
        if (!Config.cameraOverhaulThirdPerson && mc.gameSettings.thirdPersonView > 0) return;
        if (mc.gameSettings.debugCamEnable) return;
        if (Config.cameraKeepFirstPersonHandStable && anextratouch$renderingHand) return;

        CameraHandler.update(entity, partialTicks);
        float pitchOff = CameraHandler.getPitchOffset();
        float yawOff = CameraHandler.getYawOffset();
        float rollOff = CameraHandler.getRollOffset();
        if (pitchOff == 0f && yawOff == 0f && rollOff == 0f) return;

        // Recalc interpolated values (matching orientCamera's own math)
        // When decoupled, entity rotation IS camera rotation due to HEAD swap
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180f;
        float eyeH = entity.yOffset - 1.62f;

        // Undo eye height translation, yaw rotation, pitch rotation
        GL11.glTranslatef(0f, -eyeH, 0f);
        GL11.glRotatef(-yaw, 0f, 1f, 0f);
        GL11.glRotatef(-pitch, 1f, 0f, 0f);

        // Redo with camera overhaul offsets
        GL11.glRotatef(rollOff, 0f, 0f, 1f);
        GL11.glRotatef(pitch + pitchOff, 1f, 0f, 0f);
        GL11.glRotatef(yaw + yawOff, 0f, 1f, 0f);
        GL11.glTranslatef(0f, eyeH, 0f);
    }

    /**
     * Swap entity rotation to camera rotation around ActiveRenderInfo.updateRenderInfo so
     * particle billboard orientation uses the camera direction instead of the player body direction.
     * Without this, particles appear squished or culled when the camera faces a different direction
     * than the player in decoupled third person.
     */
    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;updateRenderInfo(Lnet/minecraft/entity/player/EntityPlayer;Z)V"))
    private void anextratouch$beforeUpdateRenderInfo(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        anextratouch$swapRotation();
    }

    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;updateRenderInfo(Lnet/minecraft/entity/player/EntityPlayer;Z)V",
            shift = Shift.AFTER))
    private void anextratouch$afterUpdateRenderInfo(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        anextratouch$restoreRotation();
    }

    /**
     * Render surface overlays in the main world translucent phase (before water),
     * so water properly overlays submerged footprints and distant ripples.
     *
     * Targets the sortAndRender call for the translucent pass (pass 1), sliced
     * between the "water" and "entities" profiler sections. This is compatible
     * with Psychedelicraft's ASM which inserts fixGLState() between the
     * LDC "water" and the endStartSection() call, breaking INVOKE_STRING.
     *
     * Multiple sortAndRender(pass=1) call sites exist across the fancy/non-fancy
     * and anaglyph branches, but only one executes per frame.
     */
    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;sortAndRender(Lnet/minecraft/entity/EntityLivingBase;ID)I"),
        slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=water")))
    private void anextratouch$renderFootprintsBeforeWater(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        FootprintManager.INSTANCE.renderInWorldPass(partialTicks);
        WaterSplashManager.INSTANCE.renderInWorldPass(partialTicks);
        WaterRippleManager.INSTANCE.renderInWorldPass(partialTicks);
    }

    /**
     * Override FOV when in third person with the configurable FOV value.
     */
    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void anextratouch$modifyFov(float partialTicks, boolean isMainView, CallbackInfoReturnable<Float> cir) {
        if (!isMainView) return;
        if (!Config.cameraFovOverrideEnabled) return;
        if (mc.gameSettings.thirdPersonView == 0) return;
        cir.setReturnValue(Config.cameraFovOverride);
    }

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void anextratouch$onRenderHandStart(float partialTicks, int pass, CallbackInfo ci) {
        anextratouch$renderingHand = true;
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void anextratouch$onRenderHandEnd(float partialTicks, int pass, CallbackInfo ci) {
        anextratouch$renderingHand = false;
    }
}
