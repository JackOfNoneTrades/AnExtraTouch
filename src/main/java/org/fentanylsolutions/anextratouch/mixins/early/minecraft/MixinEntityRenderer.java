package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.camera.CameraHandler;
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

    @Inject(method = "orientCamera", at = @At("RETURN"))
    private void anextratouch$onOrientCamera(float partialTicks, CallbackInfo ci) {
        if (!Config.cameraOverhaulEnabled) {
            return;
        }

        EntityLivingBase entity = mc.renderViewEntity;
        if (entity == null || entity.isPlayerSleeping()) {
            return;
        }
        if (!Config.cameraOverhaulThirdPerson && mc.gameSettings.thirdPersonView > 0) {
            return;
        }
        if (mc.gameSettings.debugCamEnable) {
            return;
        }
        if (Config.cameraKeepFirstPersonHandStable && anextratouch$renderingHand) {
            return;
        }

        CameraHandler.update(entity, partialTicks);
        float pitchOff = CameraHandler.getPitchOffset();
        float yawOff = CameraHandler.getYawOffset();
        float rollOff = CameraHandler.getRollOffset();
        if (pitchOff == 0f && yawOff == 0f && rollOff == 0f) {
            return;
        }

        // recalc interpolated values (matching orientCamera's own math)
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180f;
        float eyeH = entity.yOffset - 1.62f;

        // undo eye height translation, yaw rotation, pitch rotation
        GL11.glTranslatef(0f, -eyeH, 0f);
        GL11.glRotatef(-yaw, 0f, 1f, 0f);
        GL11.glRotatef(-pitch, 1f, 0f, 0f);

        // redo with offsets
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
