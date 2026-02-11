package org.fentanylsolutions.anextratouch.mixins.late.ThermalFoundation;

import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.anextratouch.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cofh.thermalfoundation.entity.monster.EntityBlizz", remap = false)
public abstract class MixinBlizzSnowTrail {

    @Inject(method = "func_70636_d", at = @At("TAIL"))
    private void anextratouch$leaveSnowTrail(CallbackInfo ci) {
        if (!Config.blizzSnowTrailEnabled) {
            return;
        }

        net.minecraft.entity.Entity entity = (net.minecraft.entity.Entity) (Object) this;
        if (entity.worldObj.isRemote) {
            return;
        }

        for (int l = 0; l < 4; ++l) {
            int x = MathHelper.floor_double(entity.posX + (double) ((float) (l % 2 * 2 - 1) * 0.25F));
            int y = MathHelper.floor_double(entity.posY);
            int z = MathHelper.floor_double(entity.posZ + (double) ((float) (l / 2 % 2 * 2 - 1) * 0.25F));

            if (entity.worldObj.getBlock(x, y, z)
                .getMaterial() == Material.air
                && entity.worldObj.getBiomeGenForCoords(x, z)
                    .getFloatTemperature(x, y, z) < 0.8F
                && Blocks.snow_layer.canPlaceBlockAt(entity.worldObj, x, y, z)) {
                entity.worldObj.setBlock(x, y, z, Blocks.snow_layer);
            }
        }
    }
}
