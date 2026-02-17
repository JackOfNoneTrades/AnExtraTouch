package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.entity.EntityPlayerSP;

import org.fentanylsolutions.anextratouch.handlers.client.camera.DecoupledCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Inject(method = "updateEntityActionState", at = @At("RETURN"))
    private void anextratouch$afterUpdateEntityActionState(CallbackInfo ci) {
        DecoupledCameraHandler.transformMovement((EntityPlayerSP) (Object) this);
    }
}
