package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;

import org.fentanylsolutions.anextratouch.handlers.client.effects.NeutralParticleTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class MixinTextureManager {

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void anextratouch$reloadNeutralWaterParticles(IResourceManager resourceManager, CallbackInfo ci) {
        NeutralParticleTexture.invalidate();
    }
}
