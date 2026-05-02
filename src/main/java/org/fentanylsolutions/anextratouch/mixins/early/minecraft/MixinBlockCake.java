package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.block.BlockCake;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.effects.CakeEatingEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockCake.class)
public class MixinBlockCake {

    @Inject(method = "func_150036_b", at = @At("HEAD"))
    private void anextratouch$spawnCakeEatingEffects(World world, int x, int y, int z, EntityPlayer player,
        CallbackInfo ci) {
        if (!Config.cakeEatingParticlesEnabled || !world.isRemote || !player.canEat(false)) {
            return;
        }

        CakeEatingEffect.spawn(player);
    }
}
