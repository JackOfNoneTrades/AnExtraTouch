package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterCascadeManager;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterfallSprayFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid {

    @Inject(method = "randomDisplayTick", at = @At("TAIL"))
    private void anextratouch$spawnWaterfallSpray(World worldIn, int x, int y, int z, Random random, CallbackInfo ci) {
        if (!Config.waterfallSprayEnabled) return;
        if (WaterCascadeManager.getSprayIcon() == null) return;

        Block block = (Block) (Object) this;
        if (block.getMaterial() != Material.water) return;
        if (!WaterCascadeManager.INSTANCE.isWaterfallImpact(worldIn, x, y, z)) return;

        int meta = worldIn.getBlockMetadata(x, y, z);
        for (int i = 0; i < 2; ++i) {
            if (meta >= 8) {
                double px = x;
                double py = y + 0.05D + random.nextDouble() * 0.25D;
                double pz = z;

                if (random.nextBoolean()) {
                    px += random.nextDouble();
                    pz += random.nextInt(2);
                } else {
                    px += random.nextInt(2);
                    pz += random.nextDouble();
                }

                Minecraft.getMinecraft().effectRenderer
                    .addEffect(new WaterfallSprayFX(worldIn, px, py, pz, 0.0D, 0.0D));
            } else {
                double px = x + random.nextDouble();
                double py = y + random.nextDouble() * (1.0D - BlockLiquid.getLiquidHeightPercent(meta));
                double pz = z + random.nextDouble();

                Vec3 flow = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
                ((BlockLiquid) (Object) this).velocityToAddToEntity(worldIn, x, y, z, null, flow);

                Minecraft.getMinecraft().effectRenderer
                    .addEffect(new WaterfallSprayFX(worldIn, px, py, pz, flow.xCoord * 0.075D, flow.zCoord * 0.075D));
            }
        }
    }
}
