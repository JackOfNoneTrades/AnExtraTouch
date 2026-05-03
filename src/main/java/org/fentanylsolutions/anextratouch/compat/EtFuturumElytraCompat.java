package org.fentanylsolutions.anextratouch.compat;

import net.minecraft.entity.Entity;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.api.elytra.IElytraPlayer;

@SideOnly(Side.CLIENT)
public final class EtFuturumElytraCompat {

    private static final String MOD_ID = "etfuturum";

    private static Boolean available;

    private EtFuturumElytraCompat() {}

    public static boolean isElytraFlying(Entity entity) {
        return isAvailable() && Bridge.isElytraFlying(entity);
    }

    private static boolean isAvailable() {
        if (available == null) {
            available = Loader.isModLoaded(MOD_ID);
        }
        return available;
    }

    private static final class Bridge {

        private Bridge() {}

        private static boolean isElytraFlying(Entity entity) {
            return entity instanceof IElytraPlayer && ((IElytraPlayer) entity).etfu$isElytraFlying();
        }
    }
}
