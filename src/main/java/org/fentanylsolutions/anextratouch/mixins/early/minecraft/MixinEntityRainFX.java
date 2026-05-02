package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityRainFX;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.handlers.client.effects.FallingWaterFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRainFX.class)
public class MixinEntityRainFX {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void anextratouch$tintRainDrop(World world, double x, double y, double z, CallbackInfo ci) {
        float[] rgb = FallingWaterFX.getWaterColor(world, x, y, z);
        ((EntityFX) (Object) this).setRBGColorF(rgb[0], rgb[1], rgb[2]);
    }
}
