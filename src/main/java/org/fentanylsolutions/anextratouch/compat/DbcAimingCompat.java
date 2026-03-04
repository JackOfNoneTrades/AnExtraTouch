package org.fentanylsolutions.anextratouch.compat;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;

import JinRyuu.JRMCore.JRMCoreH;
import JinRyuu.JRMCore.i.ExtendedPlayer;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Dragon Block C / JRMCore aiming compat.
 * Returns true when the player is in DBC combat states that should recouple camera aim:
 * - guard/block mode
 * - ki attack release
 */
@SideOnly(Side.CLIENT)
public class DbcAimingCompat {

    private static Boolean available;

    public static boolean isAvailable() {
        if (available == null) {
            available = isClassPresent("JinRyuu.DragonBC.common.DBCClientTickHandler");
        }
        return available;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * True when DBC/JRMCore runtime indicates the player is in a guard or actively firing a ki attack.
     */
    public static boolean shouldRecouple(EntityPlayerSP player) {
        if (!isAvailable()) {
            return false;
        }
        return DbcBridge.shouldRecouple(player);
    }

    // Lazy-loaded bridge to avoid classloading DBC/JRMCore types when mods are absent.
    private static class DbcBridge {

        static boolean shouldRecouple(EntityPlayerSP player) {
            if (player == null) {
                return false;
            }

            int blocking = getBlocking(player);
            boolean kiShooting = JRMCoreH.isShtng;

            return blocking > 0 || kiShooting;
        }

        private static int getBlocking(EntityPlayerSP player) {
            if (!Loader.isModLoaded("jinryuujrmcore")) {
                return -1;
            }
            try {
                ExtendedPlayer ep = ExtendedPlayer.get((EntityPlayer) player);
                if (ep == null) {
                    return -1;
                }
                return ep.getBlocking();
            } catch (Throwable ignored) {
                return -1;
            }
        }

    }
}
