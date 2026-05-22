package org.fentanylsolutions.anextratouch.compat;

import net.minecraft.entity.Entity;

import com.kentington.thaumichorizons.common.entities.EntityBoatGreatwood;
import com.kentington.thaumichorizons.common.entities.EntityBoatThaumium;

import cpw.mods.fml.common.Loader;

public final class ThaumicHorizonsBoatCompat {

    private static final String MOD_ID = "ThaumicHorizons";

    private static Boolean available;

    private ThaumicHorizonsBoatCompat() {}

    public static boolean isBoat(Entity entity) {
        return isAvailable() && Bridge.isBoat(entity);
    }

    private static boolean isAvailable() {
        if (available == null) {
            available = Loader.isModLoaded(MOD_ID);
        }
        return available;
    }

    private static final class Bridge {

        private Bridge() {}

        private static boolean isBoat(Entity entity) {
            return entity instanceof EntityBoatGreatwood || entity instanceof EntityBoatThaumium;
        }
    }
}
