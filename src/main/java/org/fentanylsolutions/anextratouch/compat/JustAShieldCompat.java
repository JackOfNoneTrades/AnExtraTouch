package org.fentanylsolutions.anextratouch.compat;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.anextratouch.AnExtraTouch;

import cpw.mods.fml.common.Loader;

/** Uses Just a Shield's own rules for offhand, sword-blocking and crouch activation. */
public final class JustAShieldCompat {

    private static boolean initialized;
    private static Method getShieldInUse;

    private JustAShieldCompat() {}

    public static ItemStack getBlockingShield(EntityPlayer player) {
        if (!initialized) {
            initialized = true;
            if (Loader.isModLoaded("targaseule")) {
                try {
                    getShieldInUse = Class.forName("invalid.myask.undertow.util.ShieldUtil")
                        .getMethod("getShieldInUse", EntityPlayer.class);
                } catch (ReflectiveOperationException | LinkageError e) {
                    AnExtraTouch.LOG.warn("Could not initialize Just a Shield sound compatibility", e);
                }
            }
        }
        if (getShieldInUse != null) {
            try {
                return (ItemStack) getShieldInUse.invoke(null, player);
            } catch (ReflectiveOperationException | LinkageError e) {
                AnExtraTouch.LOG.warn("Disabling Just a Shield sound compatibility after an error", e);
                getShieldInUse = null;
            }
        }
        return null;
    }
}
