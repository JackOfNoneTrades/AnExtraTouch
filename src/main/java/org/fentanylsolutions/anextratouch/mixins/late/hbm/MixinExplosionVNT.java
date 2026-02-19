package org.fentanylsolutions.anextratouch.mixins.late.hbm;

import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.network.NetworkHandler;
import org.fentanylsolutions.anextratouch.network.message.MessageExplosionShake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.network.NetworkRegistry;

@Pseudo
@Mixin(targets = "com.hbm.explosion.vanillant.ExplosionVNT", remap = false)
public abstract class MixinExplosionVNT {

    @Shadow
    public World world;
    @Shadow
    public double posX;
    @Shadow
    public double posY;
    @Shadow
    public double posZ;
    @Shadow
    public float size;

    @Inject(method = "explode", at = @At("RETURN"))
    private void anextratouch$explode(CallbackInfo ci) {
        if (world.isRemote) return;

        NetworkHandler.channel.sendToAllAround(
            new MessageExplosionShake(posX, posY, posZ, size),
            new NetworkRegistry.TargetPoint(world.provider.dimensionId, posX, posY, posZ, size * 10f));
    }

}
