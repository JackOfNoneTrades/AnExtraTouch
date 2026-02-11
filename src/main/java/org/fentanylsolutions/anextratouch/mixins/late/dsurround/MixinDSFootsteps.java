package org.fentanylsolutions.anextratouch.mixins.late.dsurround;

import net.minecraft.entity.player.EntityPlayer;

import org.blockartistry.mod.DynSurround.client.footsteps.engine.interfaces.EventType;
import org.blockartistry.mod.DynSurround.client.footsteps.game.system.PFReaderH;
import org.fentanylsolutions.anextratouch.handlers.client.StepSoundHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Dynamic Surroundings compat

@Mixin(value = PFReaderH.class, remap = false)
public abstract class MixinDSFootsteps {

    @Inject(
        method = "produceStep(Lnet/minecraft/entity/player/EntityPlayer;Lorg/blockartistry/mod/DynSurround/client/footsteps/engine/interfaces/EventType;D)V",
        at = @At("HEAD"))
    private void onProduceStep(EntityPlayer player, EventType event, double verticalOffsetAsMinus, CallbackInfo ci) {
        StepSoundHandler.onEntityStep(player);
    }

    @Inject(
        method = "playMultifoot(Lnet/minecraft/entity/player/EntityPlayer;DLorg/blockartistry/mod/DynSurround/client/footsteps/engine/interfaces/EventType;)V",
        at = @At("HEAD"))
    private void onPlayMultifoot(EntityPlayer player, double verticalOffsetAsMinus, EventType eventType,
        CallbackInfo ci) {
        if (eventType == EventType.LAND) {
            StepSoundHandler.onEntityLand(player, 1.0f);
        }
    }
}
