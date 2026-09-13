package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.ItemSoundHandler;
import org.fentanylsolutions.anextratouch.handlers.client.camera.CameraHandler;
import org.fentanylsolutions.anextratouch.handlers.client.camera.ScreenShakeManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @Unique
    private long anextratouch$shakeHandle;
    @Unique
    private long anextratouch$fallShakeHandle;

    @Inject(
        method = "swingItem",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/EntityLivingBase;swingProgressInt:I",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER))
    private void anextratouch$onAcceptedItemSwing(CallbackInfo ci) {
        ItemSoundHandler.onSwing((EntityLivingBase) (Object) this);
    }

    @Inject(method = "swingItem", at = @At("RETURN"))
    private void anextratouch$onSwingItem(CallbackInfo ci) {
        if (!Config.cameraOverhaulEnabled) {
            return;
        }

        EntityLivingBase self = (EntityLivingBase) (Object) this;
        // only for the local player on the client
        if (!self.worldObj.isRemote) {
            return;
        }
        if (self != Minecraft.getMinecraft().thePlayer) {
            return;
        }

        anextratouch$shakeHandle = ScreenShakeManager.recreate(anextratouch$shakeHandle);
        ScreenShakeManager.Slot shake = ScreenShakeManager.get(anextratouch$shakeHandle);
        shake.trauma = Config.cameraHandSwingTrauma;
        shake.frequency = 0.5f;
        shake.lengthInSeconds = 0.5f;

        CameraHandler.notifyOfPlayerAction();
    }

    @Inject(method = "updateFallState", at = @At("HEAD"))
    private void anextratouch$onUpdateFallState(double distanceFallenThisTick, boolean isOnGround, CallbackInfo ci) {
        if (!Config.cameraOverhaulEnabled || !Config.cameraFallShakeEnabled) {
            return;
        }

        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (!self.worldObj.isRemote) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (self != mc.thePlayer) {
            return;
        }
        if (Config.cameraDisableWhilePaused && mc.isGamePaused()) {
            return;
        }
        if (!isOnGround) {
            return;
        }

        float distance = self.fallDistance;
        if (distance < Config.cameraFallShakeMinDistance) {
            return;
        }

        float denom = Math.max(0.0001f, Config.cameraFallShakeMaxDistance - Config.cameraFallShakeMinDistance);
        float t = (distance - Config.cameraFallShakeMinDistance) / denom;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        anextratouch$fallShakeHandle = ScreenShakeManager.recreate(anextratouch$fallShakeHandle);
        ScreenShakeManager.Slot shake = ScreenShakeManager.get(anextratouch$fallShakeHandle);
        shake.trauma = Config.cameraFallShakeMaxTrauma * (t * t);
        shake.frequency = Config.cameraFallShakeFrequency;
        shake.lengthInSeconds = Config.cameraFallShakeLength;

        CameraHandler.notifyOfPlayerAction();
    }
}
