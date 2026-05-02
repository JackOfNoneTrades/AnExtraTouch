package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.particle.EntityBubbleFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.handlers.client.effects.NeutralParticleTexture;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterParticleTint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityBubbleFX.class, priority = 500)
public abstract class MixinEntityBubbleFX extends EntityFX {

    @Unique
    private float anextratouch$tintRed = 1.0F;

    @Unique
    private float anextratouch$tintGreen = 1.0F;

    @Unique
    private float anextratouch$tintBlue = 1.0F;

    protected MixinEntityBubbleFX(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void anextratouch$tintBubble(World world, double x, double y, double z, double motionX, double motionY,
        double motionZ, CallbackInfo ci) {
        NeutralParticleTexture.ensureApplied();
        float[] rgb = WaterParticleTint.getTintWithWaterFallback(world, x, y, z, true);
        this.anextratouch$tintRed = rgb[0];
        this.anextratouch$tintGreen = rgb[1];
        this.anextratouch$tintBlue = rgb[2];
        anextratouch$applyStoredTint();
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void anextratouch$tintBubbleAfterMove(CallbackInfo ci) {
        EntityFX particle = (EntityFX) (Object) this;
        if (!particle.isDead) {
            anextratouch$applyStoredTint();
        }
    }

    private void anextratouch$applyStoredTint() {
        EntityFX particle = (EntityFX) (Object) this;
        particle.setRBGColorF(this.anextratouch$tintRed, this.anextratouch$tintGreen, this.anextratouch$tintBlue);
    }
}
