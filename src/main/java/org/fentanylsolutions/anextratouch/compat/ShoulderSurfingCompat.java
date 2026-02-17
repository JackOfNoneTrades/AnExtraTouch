package org.fentanylsolutions.anextratouch.compat;

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

    /**
     * Checks SS's CrosshairVisibility config for the current perspective.
     * Returns false if SS is not installed.
     */
    public static boolean shouldRenderCrosshair() {
        return isAvailable() && ShoulderSurfingBridge.shouldRenderCrosshair();
    }

    // Inner class only loaded by the JVM when first referenced,
    // which only happens after isAvailable() confirms SS is on the classpath.
    private static class ShoulderSurfingBridge {

        static boolean isActive() {
            return com.teamderpy.shouldersurfing.client.ShoulderInstance.getInstance()
                .doShoulderSurfing();
        }

        static boolean shouldRenderCrosshair() {
            com.teamderpy.shouldersurfing.client.ShoulderInstance instance =
                com.teamderpy.shouldersurfing.client.ShoulderInstance.getInstance();
            if (!instance.doShoulderSurfing()) return false;
            com.teamderpy.shouldersurfing.config.Perspective perspective =
                com.teamderpy.shouldersurfing.config.Perspective.current();
            return com.teamderpy.shouldersurfing.config.Config.CLIENT
                .getCrosshairVisibility(perspective)
                .doRender(net.minecraft.client.Minecraft.getMinecraft().objectMouseOver,
                    instance.isAiming());
        }
    }
}
