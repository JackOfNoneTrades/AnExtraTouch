package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityBoat.class)
public class MixinEntityBoat {

    @Unique
    private static final float anextratouch$VANILLA_BOAT_MODEL_YAW_OFFSET = 90.0F;

    @Unique
    private float anextratouch$deltaRotation;

    @Unique
    private boolean anextratouch$leftInputDown;
    @Unique
    private boolean anextratouch$rightInputDown;
    @Unique
    private boolean anextratouch$forwardInputDown;
    @Unique
    private boolean anextratouch$backInputDown;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void anextratouch$resetBoatInputs(CallbackInfo ci) {
        EntityBoat boat = (EntityBoat) (Object) this;
        if (!(boat.riddenByEntity instanceof EntityLivingBase)) {
            anextratouch$deltaRotation = 0.0F;
            anextratouch$updateInputs(false, false, false, false);
        }
    }

    @Redirect(
        method = "onUpdate",
        at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;moveForward:F"))
    private float anextratouch$skipVanillaPassengerThrottle(EntityLivingBase passenger) {
        return 0.0F;
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Ljava/lang/Math;sqrt(D)D", ordinal = 1))
    private void anextratouch$controlBoatLikeEtFuturum(CallbackInfo ci) {
        EntityBoat boat = (EntityBoat) (Object) this;
        Entity passenger = boat.riddenByEntity;
        if (!(passenger instanceof EntityLivingBase)) {
            return;
        }

        EntityLivingBase living = (EntityLivingBase) passenger;
        anextratouch$updateInputs(
            living.moveStrafing > 0.0F,
            living.moveStrafing < 0.0F,
            living.moveForward > 0.0F,
            living.moveForward < 0.0F);
        anextratouch$updateMotion(boat);
        anextratouch$controlBoat(boat);
    }

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Ljava/lang/Math;atan2(DD)D"))
    private double anextratouch$keepControlledBoatYaw(double y, double x) {
        EntityBoat boat = (EntityBoat) (Object) this;
        if (boat.riddenByEntity instanceof EntityLivingBase) {
            return boat.rotationYaw * 0.017453292F;
        }

        return Math.atan2(y, x);
    }

    @Unique
    private void anextratouch$updateInputs(boolean left, boolean right, boolean forward, boolean back) {
        anextratouch$leftInputDown = left;
        anextratouch$rightInputDown = right;
        anextratouch$forwardInputDown = forward;
        anextratouch$backInputDown = back;
    }

    @Unique
    private void anextratouch$updateMotion(EntityBoat boat) {
        float momentum = 0.9F;
        boat.motionX *= momentum;
        boat.motionZ *= momentum;
        anextratouch$deltaRotation *= momentum;
    }

    @Unique
    private void anextratouch$controlBoat(EntityBoat boat) {
        float f = 0.0F;

        if (anextratouch$leftInputDown) {
            anextratouch$deltaRotation += -1.0F;
        }

        if (anextratouch$rightInputDown) {
            ++anextratouch$deltaRotation;
        }

        if (anextratouch$rightInputDown != anextratouch$leftInputDown && !anextratouch$forwardInputDown
            && !anextratouch$backInputDown) {
            f += 0.005F;
        }

        boat.rotationYaw += anextratouch$deltaRotation;

        if (anextratouch$forwardInputDown) {
            f += 0.04F;
        }

        if (anextratouch$backInputDown) {
            f -= 0.005F;
        }

        float thrustYaw = boat.rotationYaw + anextratouch$VANILLA_BOAT_MODEL_YAW_OFFSET;
        boat.motionX += MathHelper.sin(-thrustYaw * 0.017453292F) * f;
        boat.motionZ += MathHelper.cos(thrustYaw * 0.017453292F) * f;
    }
}
