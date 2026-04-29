package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;

import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterCascadeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public class MixinChunk {

    @Inject(method = "fillChunk", at = @At("TAIL"))
    private void anextratouch$refreshCascadesOnChunkFill(byte[] data, int storageMask, int msbMask, boolean fullChunk,
        CallbackInfo ci) {
        WaterCascadeManager.INSTANCE.onChunkFilled((Chunk) (Object) this);
    }

    @Inject(method = "func_150807_a", at = @At("RETURN"))
    private void anextratouch$refreshCascadesOnBlockChange(int localX, int y, int localZ, Block block, int metadata,
        CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        WaterCascadeManager.INSTANCE.onBlockChanged((Chunk) (Object) this, localX, y, localZ);
    }
}
