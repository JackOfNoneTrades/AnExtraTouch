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

    // Inner class only loaded by the JVM when first referenced,
    // which only happens after isAvailable() confirms SS is on the classpath.
    private static class ShoulderSurfingBridge {

        static boolean isActive() {
            return com.teamderpy.shouldersurfing.client.ShoulderInstance.getInstance()
                .doShoulderSurfing();
        }
    }
}
