package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.server.MinecraftServer;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

@Mixin(value = MinecraftServer.class, priority = 1100)
public class MixinMinecraftServer {

    @Inject(
        method = "initialWorldChunkLoad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkProviderServer;loadChunk(II)Lnet/minecraft/world/chunk/Chunk;"),
        require = 0,
        expect = 0)
    private void initialWorldChunkLoad(CallbackInfo ci, @Local(ordinal = 0) int progress) {
        AnExtraTouch.vic.chunkLoadingProgress = (progress * 100 / 625);
    }

    @Inject(method = "clearCurrentTask", at = @At("HEAD"))
    private void clearCurrentTask(CallbackInfo ci) {
        AnExtraTouch.vic.chunkLoadingProgress = -1;
    }

}
