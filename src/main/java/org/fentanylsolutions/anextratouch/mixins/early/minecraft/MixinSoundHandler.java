package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.camera.DecoupledCameraHandler;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterCascadeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundHandler.class)
public class MixinSoundHandler {

    @Unique
    private boolean anextratouch$swapped;
    @Unique
    private double anextratouch$px, anextratouch$py, anextratouch$pz;
    @Unique
    private double anextratouch$ppx, anextratouch$ppy, anextratouch$ppz;
    @Unique
    private float anextratouch$yaw, anextratouch$pitch;
    @Unique
    private float anextratouch$pyaw, anextratouch$ppitch;

    /**
     * Swap entity position/rotation to camera values before the sound listener is set.
     * SoundManager.setListener uses entity position for listener position and entity
     * rotation for listener orientation. By temporarily overriding these, the OpenAL
     * listener is placed at the camera instead of the player in third person.
     */
    @Inject(method = "setListener", at = @At("HEAD"))
    private void anextratouch$beforeSetListener(EntityPlayer player, float partialTicks, CallbackInfo ci) {
        anextratouch$swapped = false;
        if (player == null || !Config.cameraSoundCentering) return;
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) return;
        if (!DecoupledCameraHandler.isSoundListenerReady()) return;

        // Save real state
        anextratouch$px = player.posX;
        anextratouch$py = player.posY;
        anextratouch$pz = player.posZ;
        anextratouch$ppx = player.prevPosX;
        anextratouch$ppy = player.prevPosY;
        anextratouch$ppz = player.prevPosZ;
        anextratouch$yaw = player.rotationYaw;
        anextratouch$pitch = player.rotationPitch;
        anextratouch$pyaw = player.prevRotationYaw;
        anextratouch$ppitch = player.prevRotationPitch;
        anextratouch$swapped = true;

        // Override with camera state (set prev = current so interpolation gives exact camera pos)
        float cx = DecoupledCameraHandler.getSoundCamX();
        float cy = DecoupledCameraHandler.getSoundCamY();
        float cz = DecoupledCameraHandler.getSoundCamZ();
        player.posX = cx;
        player.posY = cy;
        player.posZ = cz;
        player.prevPosX = cx;
        player.prevPosY = cy;
        player.prevPosZ = cz;
        float cyaw = DecoupledCameraHandler.getSoundCamYaw();
        float cpitch = DecoupledCameraHandler.getSoundCamPitch();
        player.rotationYaw = cyaw;
        player.rotationPitch = cpitch;
        player.prevRotationYaw = cyaw;
        player.prevRotationPitch = cpitch;
    }

    @Inject(method = "setListener", at = @At("RETURN"))
    private void anextratouch$afterSetListener(EntityPlayer player, float partialTicks, CallbackInfo ci) {
        if (!anextratouch$swapped || player == null) return;
        player.posX = anextratouch$px;
        player.posY = anextratouch$py;
        player.posZ = anextratouch$pz;
        player.prevPosX = anextratouch$ppx;
        player.prevPosY = anextratouch$ppy;
        player.prevPosZ = anextratouch$ppz;
        player.rotationYaw = anextratouch$yaw;
        player.rotationPitch = anextratouch$pitch;
        player.prevRotationYaw = anextratouch$pyaw;
        player.prevRotationPitch = anextratouch$ppitch;
        anextratouch$swapped = false;
    }

    @Inject(method = "resumeSounds", at = @At("TAIL"))
    private void anextratouch$refreshWaterfallsAfterResume(CallbackInfo ci) {
        WaterCascadeManager.INSTANCE.onSoundSystemResumed();
    }

    @Inject(method = "stopSounds", at = @At("TAIL"))
    private void anextratouch$clearWaterfallsAfterStop(CallbackInfo ci) {
        WaterCascadeManager.INSTANCE.onSoundSystemStopped();
    }
}
