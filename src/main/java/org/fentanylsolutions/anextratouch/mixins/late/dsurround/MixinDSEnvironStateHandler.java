package org.fentanylsolutions.anextratouch.mixins.late.dsurround;

import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;

import org.blockartistry.mod.DynSurround.client.EnvironStateHandler;
import org.fentanylsolutions.anextratouch.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** AET owns these accents while enabled; otherwise DS keeps its original behavior. */
@Mixin(value = EnvironStateHandler.class, remap = false)
public abstract class MixinDSEnvironStateHandler {

    @Inject(
        method = "onItemUse(Lnet/minecraftforge/event/entity/player/AttackEntityEvent;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void anextratouch$replaceAttackSound(AttackEntityEvent event, CallbackInfo ci) {
        if (Config.itemSwingSoundsEnabled) ci.cancel();
    }

    @Inject(
        method = "onItemUse(Lnet/minecraftforge/event/entity/player/PlayerUseItemEvent$Start;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void anextratouch$replaceBowDrawSound(PlayerUseItemEvent.Start event, CallbackInfo ci) {
        if (Config.itemBowDrawSoundsEnabled) ci.cancel();
    }
}
