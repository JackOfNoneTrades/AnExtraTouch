package org.fentanylsolutions.anextratouch.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.anextratouch.Config;

public final class BoatControlState {

    private static final float VANILLA_BOAT_MODEL_YAW_OFFSET = 90.0F;

    private float deltaRotation;
    private boolean leftInputDown;
    private boolean rightInputDown;
    private boolean forwardInputDown;
    private boolean backInputDown;

    public void resetIfNotControlled(EntityBoat boat) {
        if (!isControlledByLiving(boat)) {
            deltaRotation = 0.0F;
            updateInputs(false, false, false, false);
        }
    }

    public float redirectPassengerThrottle(EntityLivingBase passenger) {
        return Config.boatControlsEnabled ? 0.0F : passenger.moveForward;
    }

    public void control(EntityBoat boat) {
        Entity passenger = boat.riddenByEntity;
        if (!Config.boatControlsEnabled || !(passenger instanceof EntityLivingBase)) {
            return;
        }

        EntityLivingBase living = (EntityLivingBase) passenger;
        updateInputs(
            living.moveStrafing > 0.0F,
            living.moveStrafing < 0.0F,
            living.moveForward > 0.0F,
            living.moveForward < 0.0F);
        updateMotion(boat);
        controlBoat(boat);
    }

    public double redirectControlledYaw(EntityBoat boat, double y, double x) {
        if (isControlledByLiving(boat)) {
            return boat.rotationYaw * 0.017453292F;
        }

        return Math.atan2(y, x);
    }

    private static boolean isControlledByLiving(EntityBoat boat) {
        return Config.boatControlsEnabled && boat.riddenByEntity instanceof EntityLivingBase;
    }

    private void updateInputs(boolean left, boolean right, boolean forward, boolean back) {
        leftInputDown = left;
        rightInputDown = right;
        forwardInputDown = forward;
        backInputDown = back;
    }

    private void updateMotion(EntityBoat boat) {
        float momentum = 0.9F;
        boat.motionX *= momentum;
        boat.motionZ *= momentum;
        deltaRotation *= momentum;
    }

    private void controlBoat(EntityBoat boat) {
        float thrust = 0.0F;

        if (leftInputDown) {
            deltaRotation += -1.0F;
        }

        if (rightInputDown) {
            ++deltaRotation;
        }

        if (rightInputDown != leftInputDown && !forwardInputDown && !backInputDown) {
            thrust += 0.005F;
        }

        boat.rotationYaw += deltaRotation;
        normalizeControlledYaw(boat);

        if (forwardInputDown) {
            thrust += 0.04F;
        }

        if (backInputDown) {
            thrust -= 0.005F;
        }

        float thrustYaw = boat.rotationYaw + VANILLA_BOAT_MODEL_YAW_OFFSET;
        boat.motionX += MathHelper.sin(-thrustYaw * 0.017453292F) * thrust;
        boat.motionZ += MathHelper.cos(thrustYaw * 0.017453292F) * thrust;
    }

    private static void normalizeControlledYaw(EntityBoat boat) {
        boat.rotationYaw = MathHelper.wrapAngleTo180_float(boat.rotationYaw);

        while (boat.rotationYaw - boat.prevRotationYaw < -180.0F) {
            boat.prevRotationYaw -= 360.0F;
        }

        while (boat.rotationYaw - boat.prevRotationYaw >= 180.0F) {
            boat.prevRotationYaw += 360.0F;
        }
    }
}
