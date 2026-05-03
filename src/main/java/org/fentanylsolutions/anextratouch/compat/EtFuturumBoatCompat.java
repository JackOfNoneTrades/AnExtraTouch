package org.fentanylsolutions.anextratouch.compat;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.entities.EntityNewBoat;

@SideOnly(Side.CLIENT)
public final class EtFuturumBoatCompat {

    private static final String MOD_ID = "etfuturum";
    private static final RowingTrail[] NO_TRAILS = new RowingTrail[0];

    private static Boolean available;

    private EtFuturumBoatCompat() {}

    public static boolean isBoat(Entity entity) {
        return isAvailable() && Bridge.isBoat(entity);
    }

    public static RowingTrail[] getRowingTrails(Entity entity, double velocity) {
        if (!isAvailable() || velocity <= 0.0D) {
            return NO_TRAILS;
        }

        return Bridge.getRowingTrails(entity, velocity);
    }

    private static boolean isAvailable() {
        if (available == null) {
            available = Loader.isModLoaded(MOD_ID);
        }
        return available;
    }

    public static final class RowingTrail {

        public final double fromX;
        public final double fromZ;
        public final double toX;
        public final double toZ;

        private RowingTrail(double fromX, double fromZ, double toX, double toZ) {
            this.fromX = fromX;
            this.fromZ = fromZ;
            this.toX = toX;
            this.toZ = toZ;
        }
    }

    private static final class Bridge {

        private static final double TWO_PI = Math.PI * 2.0D;
        private static final double PADDLE_SPEED = Math.PI / 8.0D;
        private static final double PADDLE_SOUND_TIME = Math.PI / 4.0D;
        private static final double PADDLE_WAKE_START = PADDLE_SPEED / 2.0D;
        private static final double PADDLE_WAKE_END = PADDLE_SOUND_TIME + PADDLE_SPEED;

        private Bridge() {}

        private static boolean isBoat(Entity entity) {
            return entity instanceof EntityNewBoat;
        }

        private static RowingTrail[] getRowingTrails(Entity entity, double velocity) {
            if (!(entity instanceof EntityNewBoat)) {
                return NO_TRAILS;
            }

            EntityNewBoat boat = (EntityNewBoat) entity;
            RowingTrail[] trails = new RowingTrail[2];
            int count = 0;

            float yaw = boat.rotationYaw * 0.017453292F;
            double forwardX = MathHelper.sin(-yaw);
            double forwardZ = MathHelper.cos(yaw);
            double trailLength = velocity * 2.0D;

            for (int side = 0; side < 2; side++) {
                if (!boat.getPaddleState(side)) {
                    continue;
                }

                double phase = positiveModulo(boat.getRowingTime(side, 1.0F), TWO_PI);
                if (phase < PADDLE_WAKE_START || phase > PADDLE_WAKE_END) {
                    continue;
                }

                double x = boat.posX + (side == 1 ? -forwardZ : forwardZ);
                double z = boat.posZ + (side == 1 ? forwardX : -forwardX);
                trails[count++] = new RowingTrail(x, z, x + forwardX * trailLength, z + forwardZ * trailLength);
            }

            if (count == 0) {
                return NO_TRAILS;
            }
            if (count == trails.length) {
                return trails;
            }

            return new RowingTrail[] { trails[0] };
        }

        private static double positiveModulo(double value, double modulo) {
            double result = value % modulo;
            return result < 0.0D ? result + modulo : result;
        }
    }
}
