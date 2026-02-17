package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import org.fentanylsolutions.anextratouch.handlers.client.camera.DecoupledCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntitySetAngles {

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void anextratouch$onSetAngles(float yaw, float pitch, CallbackInfo ci) {
        if ((Object) this == Minecraft.getMinecraft().renderViewEntity) {
            if (DecoupledCameraHandler.onSetAngles(yaw, pitch)) {
                ci.cancel();
            }
        }
    }
}
