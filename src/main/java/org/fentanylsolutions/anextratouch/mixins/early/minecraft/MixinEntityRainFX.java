package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityRainFX;
import net.minecraft.client.particle.EntitySplashFX;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.handlers.client.effects.NeutralParticleTexture;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterParticleTint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRainFX.class, priority = 500)
public class MixinEntityRainFX {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void anextratouch$tintRainDrop(World world, double x, double y, double z, CallbackInfo ci) {
        NeutralParticleTexture.ensureApplied();
        anextratouch$applyFluidTint();
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void anextratouch$tintRainDropAfterMove(CallbackInfo ci) {
        EntityFX particle = (EntityFX) (Object) this;
        if (!particle.isDead) {
            anextratouch$applyFluidTint();
        }
    }

    private void anextratouch$applyFluidTint() {
        EntityFX particle = (EntityFX) (Object) this;
        if ((Object) this instanceof EntitySplashFX) {
            WaterParticleTint.applyTint(particle, true);
        } else {
            WaterParticleTint.applyTintWithWaterFallback(particle, false);
        }
    }
}
