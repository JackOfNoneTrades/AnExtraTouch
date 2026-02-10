package org.fentanylsolutions.anextratouch.mixins.late.dsurround;

import net.minecraft.entity.player.EntityPlayer;

import org.blockartistry.mod.DynSurround.client.footsteps.engine.interfaces.EventType;
import org.fentanylsolutions.anextratouch.handlers.client.effects.PlayerEffectHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

// Dynamic Surroundings compat

@SideOnly(Side.CLIENT)
@Mixin(targets = "org.blockartistry.mod.DynSurround.client.footsteps.game.system.PFReaderH", remap = false)
public abstract class MixinDSFootsteps {

    @Inject(
        method = "produceStep(Lnet/minecraft/entity/player/EntityPlayer;Lorg/blockartistry/mod/DynSurround/client/footsteps/engine/interfaces/EventType;D)V",
        at = @At("HEAD"))
    private void onProduceStep(EntityPlayer ply, EventType event, double verticalOffsetAsMinus, CallbackInfo ci) {
        System.out.println("TEST");
        PlayerEffectHandler.onEntityStep(ply);
    }
}
