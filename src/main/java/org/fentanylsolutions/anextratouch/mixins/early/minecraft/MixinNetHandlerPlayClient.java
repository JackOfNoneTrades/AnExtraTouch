package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S30PacketWindowItems;

import org.fentanylsolutions.anextratouch.handlers.client.ItemSoundHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "handleRespawn", at = @At("RETURN"))
    private void anextratouch$awaitRestoredInventory(S07PacketRespawn packet, CallbackInfo ci) {
        ItemSoundHandler.INSTANCE.onPlayerRecreated();
    }

    @Inject(method = "handleWindowItems", at = @At("RETURN"))
    private void anextratouch$inventoryRestored(S30PacketWindowItems packet, CallbackInfo ci) {
        if (packet.func_148911_c() == 0) ItemSoundHandler.INSTANCE.onInitialInventoryPacket(true);
    }

    @Inject(method = "handleHeldItemChange", at = @At("RETURN"))
    private void anextratouch$selectionRestored(S09PacketHeldItemChange packet, CallbackInfo ci) {
        if (packet.func_149385_c() >= 0 && packet.func_149385_c() < 9)
            ItemSoundHandler.INSTANCE.onInitialInventoryPacket(false);
    }
}
