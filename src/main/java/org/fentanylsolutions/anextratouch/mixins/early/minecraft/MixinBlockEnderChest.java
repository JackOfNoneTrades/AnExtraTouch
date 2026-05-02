package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.block.BlockEnderChest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.effects.ChestBubbleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEnderChest.class)
public class MixinBlockEnderChest {

    @Inject(method = "onBlockActivated", at = @At("HEAD"))
    private void anextratouch$releaseBubbles(World world, int x, int y, int z, EntityPlayer player, int side,
        float subX, float subY, float subZ, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.chestBubblesEnabled || !world.isRemote) {
            return;
        }

        if (ChestBubbleManager.canSpawnEnderOpenBubbles(world, x, y, z)) {
            ChestBubbleManager.spawnEnderOpenBubbles(world, x, y, z);
        }
    }
}
