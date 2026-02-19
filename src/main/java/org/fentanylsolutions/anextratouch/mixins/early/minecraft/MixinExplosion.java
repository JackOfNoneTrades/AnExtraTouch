package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.camera.ScreenShakeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class MixinExplosion {

    @Shadow
    private World worldObj;
    @Shadow
    public double explosionX;
    @Shadow
    public double explosionY;
    @Shadow
    public double explosionZ;
    @Shadow
    public float explosionSize;

    @Inject(method = "doExplosionB", at = @At("RETURN"))
    private void anextratouch$onExplosion(boolean spawnParticles, CallbackInfo ci) {
        if (!worldObj.isRemote) {
            return;
        }
        if (!Config.cameraOverhaulEnabled) {
            return;
        }

        ScreenShakeManager.Slot shake = ScreenShakeManager.createDirect();
        shake.position.set(explosionX, explosionY, explosionZ);
        shake.radius = explosionSize * 10f;
        shake.trauma = Config.cameraExplosionTrauma;
        shake.lengthInSeconds = Config.cameraExplosionLength;
    }
}
