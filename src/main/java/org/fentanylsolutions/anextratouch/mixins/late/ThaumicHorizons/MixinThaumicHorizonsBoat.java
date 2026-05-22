package org.fentanylsolutions.anextratouch.mixins.late.ThaumicHorizons;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;

import org.fentanylsolutions.anextratouch.util.BoatControlState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kentington.thaumichorizons.common.entities.EntityBoatGreatwood;
import com.kentington.thaumichorizons.common.entities.EntityBoatThaumium;

@Mixin(value = { EntityBoatGreatwood.class, EntityBoatThaumium.class }, remap = false)
public class MixinThaumicHorizonsBoat {

    @Unique
    private final BoatControlState anextratouch$boatControls = new BoatControlState();

    @Inject(method = "onUpdate", at = @At("HEAD"), remap = true)
    private void anextratouch$resetBoatInputs(CallbackInfo ci) {
        anextratouch$boatControls.resetIfNotControlled((EntityBoat) (Object) this);
    }

    @Redirect(
        method = "onUpdate",
        at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;moveForward:F"),
        remap = true)
    private float anextratouch$skipVanillaPassengerThrottle(EntityLivingBase passenger) {
        return anextratouch$boatControls.redirectPassengerThrottle(passenger);
    }

    @Inject(
        method = "onUpdate",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;sqrt(D)D", ordinal = 1),
        remap = true)
    private void anextratouch$controlBoatLikeEtFuturum(CallbackInfo ci) {
        anextratouch$boatControls.control((EntityBoat) (Object) this);
    }

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Ljava/lang/Math;atan2(DD)D"), remap = true)
    private double anextratouch$keepControlledBoatYaw(double y, double x) {
        return anextratouch$boatControls.redirectControlledYaw((EntityBoat) (Object) this, y, x);
    }
}
