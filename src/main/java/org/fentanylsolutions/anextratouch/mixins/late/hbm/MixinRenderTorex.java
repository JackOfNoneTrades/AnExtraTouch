package org.fentanylsolutions.anextratouch.mixins.late.hbm;

import net.minecraft.entity.Entity;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.camera.ScreenShakeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.hbm.render.entity.effect.RenderTorex", remap = false)
public class MixinRenderTorex {

    @Inject(
        method = "func_76986_a",
        at = @At(value = "INVOKE", target = "Lcom/hbm/main/ServerProxy;me()Lnet/minecraft/entity/player/EntityPlayer;"))
    private void anextratouch$doRender(Entity entity, double x, double y, double z, float f0, float interp,
        CallbackInfo ci) {
        if (!Config.cameraOverhaulEnabled) {
            return;
        }

        ScreenShakeManager.Slot shake = ScreenShakeManager.createDirect();
        shake.position.set(entity.posX, entity.posY, entity.posZ);
        shake.radius = 1000f;
        shake.trauma = Config.cameraExplosionTrauma;
        shake.lengthInSeconds = Config.cameraExplosionLength;
    }

}
