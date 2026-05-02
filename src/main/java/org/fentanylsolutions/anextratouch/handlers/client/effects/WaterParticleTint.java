package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WaterParticleTint {

    private static Boolean angelicaLoaded;

    private WaterParticleTint() {}

    public static float[] getTint(World world, double x, double y, double z, boolean searchNearby) {
        if (isAngelicaLoaded()) {
            return searchNearby ? WetnessFluidHelper.getNonWaterFluidColorNearOrBelow(world, x, y, z)
                : WetnessFluidHelper.getNonWaterFluidColorAtOrBelow(world, x, y, z);
        }

        return searchNearby ? WetnessFluidHelper.getFluidColorNearOrBelow(world, x, y, z)
            : WetnessFluidHelper.getFluidColorAtOrBelow(world, x, y, z);
    }

    public static boolean applyTint(EntityFX particle, boolean searchNearby) {
        float[] rgb = getTint(particle.worldObj, particle.posX, particle.posY, particle.posZ, searchNearby);
        if (rgb == null) {
            return false;
        }

        particle.setRBGColorF(rgb[0], rgb[1], rgb[2]);
        return true;
    }

    public static float[] getTintWithWaterFallback(World world, double x, double y, double z, boolean searchNearby) {
        return searchNearby ? WetnessFluidHelper.getFluidColorNearOrBelow(world, x, y, z)
            : WetnessFluidHelper.getFluidColorAtOrBelow(world, x, y, z);
    }

    public static void applyTintWithWaterFallback(EntityFX particle, boolean searchNearby) {
        float[] rgb = getTintWithWaterFallback(
            particle.worldObj,
            particle.posX,
            particle.posY,
            particle.posZ,
            searchNearby);
        particle.setRBGColorF(rgb[0], rgb[1], rgb[2]);
    }

    private static boolean isAngelicaLoaded() {
        if (angelicaLoaded == null) {
            angelicaLoaded = Loader.isModLoaded("angelica");
        }

        return angelicaLoaded.booleanValue();
    }
}
