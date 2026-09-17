package org.fentanylsolutions.anextratouch.compat;

import org.blockartistry.mod.DynSurround.client.footsteps.Footsteps;

import cpw.mods.fml.common.Loader;

public final class DynamicSurroundingsCompat {

    private static Boolean available;

    private DynamicSurroundingsCompat() {}

    public static boolean hasFootstepHandler() {
        if (available == null) available = Loader.isModLoaded("dsurround");
        return available && Bridge.hasFootstepHandler();
    }

    private static final class Bridge {

        private static boolean hasFootstepHandler() {
            // DS creates this handler only when its Footsteps option is enabled at startup.
            return Footsteps.INSTANCE != null;
        }
    }
}
