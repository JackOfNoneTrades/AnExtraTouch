package org.fentanylsolutions.anextratouch.compat;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import JinRyuu.JRMCore.i.ExtendedPlayer;
import JinRyuu.JRMCore.JRMCoreKeyHandler;
import JinRyuu.JRMCore.p.DBC.DBCPacketHandlerClient;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Dragon Block C / JRMCore aiming compat.
 * Returns true when the player is in DBC combat states that should recouple camera aim:
 * - guard/block mode
 * - charging/releasing ki attacks
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
     * True when DBC/JRMCore runtime indicates the player is in a guard or ki attack state.
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
            int mode = getBlockMode();
            boolean usePressed = isUsePressed();
            boolean fnPressed = isFnPressed();
            boolean guardByInput = mode != 0 && (usePressed || fnPressed || mode == 2);
            boolean kiCharging = JinRyuu.DragonBC.common.DBCClientTickHandler.KAchrgOn
                || JinRyuu.DragonBC.common.DBCClientTickHandler.charge
                || JinRyuu.DragonBC.common.DBCKiTech.releasing;

            return blocking > 0 || guardByInput || kiCharging;
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

        private static int getBlockMode() {
            try {
                return DBCPacketHandlerClient.getDBCPlayerBlockMode();
            } catch (Throwable ignored) {
                return -1;
            }
        }

        private static boolean isUsePressed() {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                return mc != null && mc.gameSettings != null && mc.gameSettings.keyBindUseItem != null
                    && mc.gameSettings.keyBindUseItem.getIsKeyPressed();
            } catch (Throwable ignored) {
                return false;
            }
        }

        private static boolean isFnPressed() {
            try {
                return JRMCoreKeyHandler.Fn != null && JRMCoreKeyHandler.Fn.getIsKeyPressed();
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}
