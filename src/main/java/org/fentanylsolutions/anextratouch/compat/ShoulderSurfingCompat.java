package org.fentanylsolutions.anextratouch.compat;

import java.util.Locale;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ShoulderSurfingCompat {

    private static Boolean available;

    public static boolean isAvailable() {
        if (available == null) {
            available = Loader.isModLoaded("shouldersurfing");
        }
        return available;
    }

    public static boolean isShoulderSurfingActive() {
        return isAvailable() && ShoulderSurfingBridge.isActive();
    }

    public static boolean shouldUseStaticShoulderRay() {
        return isAvailable() && ShoulderSurfingBridge.isActive() && !ShoulderSurfingBridge.isCrosshairDynamic();
    }

    public static boolean limitPlayerReach() {
        return isAvailable() && ShoulderSurfingBridge.limitPlayerReach();
    }

    /**
     * Checks SS's CrosshairVisibility config for the current perspective.
     * Returns false if SS is not installed.
     */
    public static boolean shouldRenderCrosshair() {
        return isAvailable() && ShoulderSurfingBridge.shouldRenderCrosshair();
    }

    /**
     * Programmatically sets SS's shoulder surfing state.
     * Used to re-enable shoulder surfing after aim-to-first-person transition.
     */
    public static void setShoulderSurfing(boolean enabled) {
        if (isAvailable()) {
            ShoulderSurfingBridge.setShoulderSurfing(enabled);
        }
    }

    public static String describeRayDebug(EntityLivingBase entity, float partialTicks, double reach,
        boolean hasVisualCamera, double visualCameraX, double visualCameraY, double visualCameraZ) {
        if (!isAvailable()) return "ss=unavailable";
        return ShoulderSurfingBridge.describeRayDebug(
            entity,
            partialTicks,
            reach,
            hasVisualCamera,
            visualCameraX,
            visualCameraY,
            visualCameraZ);
    }

    private static String hitSummary(MovingObjectPosition hit) {
        if (hit == null) return "null";
        String hitVec = hit.hitVec != null ? vecSummary(hit.hitVec) : "null";
        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return String.format(Locale.ROOT, "BLOCK[%d,%d,%d]@%s", hit.blockX, hit.blockY, hit.blockZ, hitVec);
        }
        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && hit.entityHit != null) {
            return String.format(
                Locale.ROOT,
                "ENTITY[id=%d,%s]@%s",
                hit.entityHit.getEntityId(),
                hit.entityHit.getClass()
                    .getSimpleName(),
                hitVec);
        }
        return String.format(Locale.ROOT, "%s@%s", hit.typeOfHit, hitVec);
    }

    private static String vecSummary(Vec3 vec) {
        return pointSummary(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    private static Vec3 copyVec(Vec3 vec) {
        return Vec3.createVectorHelper(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    private static String pointSummary(double x, double y, double z) {
        return String.format(Locale.ROOT, "(%.4f,%.4f,%.4f)", x, y, z);
    }

    private static double distance(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Inner class only loaded by the JVM when first referenced,
    // which only happens after isAvailable() confirms SS is on the classpath.
    private static class ShoulderSurfingBridge {

        static boolean isActive() {
            return com.teamderpy.shouldersurfing.client.ShoulderInstance.getInstance()
                .doShoulderSurfing();
        }

        static boolean shouldRenderCrosshair() {
            com.teamderpy.shouldersurfing.client.ShoulderInstance instance = com.teamderpy.shouldersurfing.client.ShoulderInstance
                .getInstance();
            if (!instance.doShoulderSurfing()) return false;
            com.teamderpy.shouldersurfing.config.Perspective perspective = com.teamderpy.shouldersurfing.config.Perspective
                .current();
            return com.teamderpy.shouldersurfing.config.Config.CLIENT.getCrosshairVisibility(perspective)
                .doRender(net.minecraft.client.Minecraft.getMinecraft().objectMouseOver, instance.isAiming());
        }

        static boolean isCrosshairDynamic() {
            return com.teamderpy.shouldersurfing.config.Config.CLIENT.getCrosshairType()
                .isDynamic();
        }

        static boolean limitPlayerReach() {
            return com.teamderpy.shouldersurfing.config.Config.CLIENT.limitPlayerReach();
        }

        static void setShoulderSurfing(boolean enabled) {
            com.teamderpy.shouldersurfing.client.ShoulderInstance.getInstance()
                .setShoulderSurfing(enabled);
        }

        static String describeRayDebug(EntityLivingBase entity, float partialTicks, double reach,
            boolean hasVisualCamera, double visualCameraX, double visualCameraY, double visualCameraZ) {
            if (entity == null || entity.worldObj == null) return "ss=missing-entity";
            if (!com.teamderpy.shouldersurfing.client.ShoulderInstance.getInstance()
                .doShoulderSurfing()) {
                return "ss=inactive";
            }

            com.teamderpy.shouldersurfing.client.ShoulderHelper.ShoulderLook look = com.teamderpy.shouldersurfing.client.ShoulderHelper
                .shoulderSurfingLook(entity, partialTicks, reach * reach);
            Vec3 eye = entity.getPosition(partialTicks);
            Vec3 headOffset = look.headOffset();
            Vec3 start = eye.addVector(headOffset.xCoord, headOffset.yCoord, headOffset.zCoord);
            Vec3 camera = look.cameraPos();
            Vec3 end = look.traceEndPos();
            MovingObjectPosition hit = entity.worldObj.func_147447_a(copyVec(start), copyVec(end), false, false, true);

            double visualMinusSsCam = hasVisualCamera
                ? distance(visualCameraX, visualCameraY, visualCameraZ, camera.xCoord, camera.yCoord, camera.zCoord)
                : Double.NaN;
            String visualDelta = hasVisualCamera
                ? pointSummary(
                    visualCameraX - camera.xCoord,
                    visualCameraY - camera.yCoord,
                    visualCameraZ - camera.zCoord)
                : "invalid";

            return String.format(
                Locale.ROOT,
                "ss[dynamic=%s limitReach=%s reach=%.3f cameraDistance=%.4f eye=%s headOffset=%s start=%s camera=%s end=%s segment=%.4f hit=%s visualMinusSsCam=%.4f visualDelta=%s]",
                com.teamderpy.shouldersurfing.config.Config.CLIENT.getCrosshairType()
                    .isDynamic(),
                com.teamderpy.shouldersurfing.config.Config.CLIENT.limitPlayerReach(),
                reach,
                com.teamderpy.shouldersurfing.client.ShoulderRenderer.getInstance()
                    .getCameraDistance(),
                vecSummary(eye),
                vecSummary(headOffset),
                vecSummary(start),
                vecSummary(camera),
                vecSummary(end),
                start.distanceTo(end),
                hitSummary(hit),
                visualMinusSsCam,
                visualDelta);
        }
    }
}
