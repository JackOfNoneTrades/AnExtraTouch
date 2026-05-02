package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EntityDropParticleFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.handlers.client.effects.FallingWaterFX;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterRippleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityDropParticleFX.class)
public class MixinEntityDropParticleFX {

    @Shadow
    private Material materialType;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void anextratouch$tintWaterDrop(World world, double x, double y, double z, Material material,
        CallbackInfo ci) {
        if (material == Material.water) {
            anextratouch$applyWaterTint();
        }
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void anextratouch$tintWaterDropAfterVanillaColor(CallbackInfo ci) {
        if (this.materialType == Material.water) {
            anextratouch$applyWaterTint();
        }
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void anextratouch$spawnRippleOnWater(CallbackInfo ci) {
        EntityFX particle = (EntityFX) (Object) this;
        if (particle.isDead && this.materialType == Material.water) {
            WaterRippleManager.INSTANCE
                .trySpawnDripRipple(particle.worldObj, particle.posX, particle.posY, particle.posZ);
        }
    }

    private void anextratouch$applyWaterTint() {
        EntityFX particle = (EntityFX) (Object) this;
        float[] rgb = FallingWaterFX.getWaterColor(particle.worldObj, particle.posX, particle.posY, particle.posZ);
        particle.setRBGColorF(rgb[0], rgb[1], rgb[2]);
    }
}
