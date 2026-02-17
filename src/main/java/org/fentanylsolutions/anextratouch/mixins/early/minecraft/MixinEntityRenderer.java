package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.camera.CameraHandler;
import org.fentanylsolutions.anextratouch.handlers.client.camera.DecoupledCameraHandler;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    /**
     * Swaps entity rotation to decoupled camera rotation. Call anextratouch$restoreRotation() after.
     */
    @Unique
    private void anextratouch$swapRotation() {
        anextratouch$rotationSwapped = false;
        if (!DecoupledCameraHandler.isActive()) return;

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
        // Extract camera world position from GL matrix before overhaul modifies rotation.
        // This accounts for ShoulderSurfing's shoulder offset so aim raytraces originate
        // from the actual camera position, not the player's eye.
        if (DecoupledCameraHandler.isActive()) {
            DecoupledCameraHandler.updateCameraPosition(partialTicks);
        }
        anextratouch$applyCameraOverhaul(mc.renderViewEntity, partialTicks);
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

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void anextratouch$onRenderHandStart(float partialTicks, int pass, CallbackInfo ci) {
        anextratouch$renderingHand = true;
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void anextratouch$onRenderHandEnd(float partialTicks, int pass, CallbackInfo ci) {
        anextratouch$renderingHand = false;
    }
}
