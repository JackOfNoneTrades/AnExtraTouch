package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class WetnessFluidHelper {

    private WetnessFluidHelper() {}

    static final class FluidSample {

        final float[] rgb;

        FluidSample(float[] rgb) {
            this.rgb = rgb;
        }
    }

    static FluidSample findWettableFluid(EntityLivingBase entity) {
        World world = entity.worldObj;
        AxisAlignedBB bb = entity.boundingBox.expand(0.0D, -0.4000000059604645D, 0.0D)
            .contract(0.001D, 0.001D, 0.001D);

        int minX = MathHelper.floor_double(bb.minX);
        int maxX = MathHelper.floor_double(bb.maxX + 1.0D);
        int minY = MathHelper.floor_double(bb.minY);
        int maxY = MathHelper.floor_double(bb.maxY + 1.0D);
        int minZ = MathHelper.floor_double(bb.minZ);
        int maxZ = MathHelper.floor_double(bb.maxZ + 1.0D);

        if (!world.checkChunksExist(minX, minY, minZ, maxX, maxY, maxZ)) {
            return null;
        }

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    Block block = world.getBlock(x, y, z);
                    if (block instanceof IFluidBlock) {
                        IFluidBlock fluidBlock = (IFluidBlock) block;
                        Fluid fluid = fluidBlock.getFluid();
                        if (isWettableFluid(world, x, y, z, block, fluid)
                            && intersectsForgeFluid(bb, world, x, y, z, fluidBlock)) {
                            return new FluidSample(getFluidColor(world, x, y, z, block, fluid));
                        }
                    } else if (block.getMaterial() == Material.water && intersectsVanillaLiquid(bb, world, x, y, z)) {
                        return new FluidSample(FallingWaterFX.getWaterColor(world, x + 0.5D, y + 0.5D, z + 0.5D));
                    }
                }
            }
        }

        return null;
    }

    static boolean isRainingOn(EntityLivingBase entity) {
        World world = entity.worldObj;
        int x = MathHelper.floor_double(entity.posX);
        int z = MathHelper.floor_double(entity.posZ);
        return world.canLightningStrikeAt(x, MathHelper.floor_double(entity.posY), z)
            || world.canLightningStrikeAt(x, MathHelper.floor_double(entity.posY + entity.height), z);
    }

    static double getWettableFluidSurfaceY(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof IFluidBlock) {
            IFluidBlock fluidBlock = (IFluidBlock) block;
            Fluid fluid = fluidBlock.getFluid();
            if (!isWettableFluid(world, x, y, z, block, fluid)) {
                return -1.0D;
            }

            return getForgeFluidSurfaceY(world, x, y, z, fluidBlock);
        }

        if (block.getMaterial() == Material.water) {
            return getVanillaLiquidSurfaceY(world, x, y, z);
        }

        return -1.0D;
    }

    static boolean isWettableFluidAt(World world, double x, double y, double z) {
        return getWettableFluidSurfaceY(
            world,
            MathHelper.floor_double(x),
            MathHelper.floor_double(y),
            MathHelper.floor_double(z)) >= 0.0D;
    }

    private static boolean isWettableFluid(World world, int x, int y, int z, Block block, Fluid fluid) {
        if (fluid == null || fluid == FluidRegistry.LAVA || block.getMaterial() == Material.lava) {
            return false;
        }

        return !fluid.isGaseous(world, x, y, z);
    }

    private static boolean intersectsVanillaLiquid(AxisAlignedBB bb, World world, int x, int y, int z) {
        double surfaceY = getVanillaLiquidSurfaceY(world, x, y, z);
        return bb.maxY >= surfaceY && bb.minY < (double) (y + 1);
    }

    private static boolean intersectsForgeFluid(AxisAlignedBB bb, World world, int x, int y, int z,
        IFluidBlock fluidBlock) {
        float filled = getFilledPercentage(world, x, y, z, fluidBlock);

        if (filled == 0.0F) {
            return false;
        }

        float amount = Math.min(Math.abs(filled), 1.0F);
        double fluidMinY = filled > 0.0F ? y : y + 1.0D - amount;
        double fluidMaxY = filled > 0.0F ? y + amount : y + 1.0D;
        return bb.maxY >= fluidMinY && bb.minY < fluidMaxY;
    }

    private static double getVanillaLiquidSurfaceY(World world, int x, int y, int z) {
        return (double) ((float) (y + 1) - BlockLiquid.getLiquidHeightPercent(world.getBlockMetadata(x, y, z)));
    }

    private static double getForgeFluidSurfaceY(World world, int x, int y, int z, IFluidBlock fluidBlock) {
        float filled = getFilledPercentage(world, x, y, z, fluidBlock);
        if (filled == 0.0F) {
            return -1.0D;
        }

        float amount = Math.min(Math.abs(filled), 1.0F);
        return filled > 0.0F ? y + amount : y + 1.0D;
    }

    private static float getFilledPercentage(World world, int x, int y, int z, IFluidBlock fluidBlock) {
        try {
            return fluidBlock.getFilledPercentage(world, x, y, z);
        } catch (Exception ignored) {
            return 1.0F;
        }
    }

    private static float[] getFluidColor(World world, int x, int y, int z, Block block, Fluid fluid) {
        if (fluid == FluidRegistry.WATER) {
            return FallingWaterFX.getWaterColor(world, x + 0.5D, y + 0.5D, z + 0.5D);
        }

        float[] tint = colorToRgb(fluid.getColor(world, x, y, z));
        float[] icon = sampleIconColor(getFluidIcon(world, x, y, z, block, fluid));
        if (icon == null) {
            return tint;
        }

        return new float[] { clamp(icon[0] * tint[0]), clamp(icon[1] * tint[1]), clamp(icon[2] * tint[2]) };
    }

    private static IIcon getFluidIcon(World world, int x, int y, int z, Block block, Fluid fluid) {
        IIcon icon = null;
        try {
            icon = fluid.getIcon(world, x, y, z);
        } catch (Exception ignored) {}

        if (icon == null) {
            try {
                icon = block.getIcon(1, world.getBlockMetadata(x, y, z));
            } catch (Exception ignored) {}
        }

        return icon;
    }

    private static float[] sampleIconColor(IIcon icon) {
        if (!(icon instanceof TextureAtlasSprite)) {
            return null;
        }

        TextureAtlasSprite sprite = (TextureAtlasSprite) icon;
        int[][] frameData = sprite.getFrameTextureData(0);
        if (frameData == null || frameData.length == 0 || frameData[0] == null) {
            return null;
        }

        int[] pixels = frameData[0];
        long totalR = 0L;
        long totalG = 0L;
        long totalB = 0L;
        int count = 0;
        for (int pixel : pixels) {
            int alpha = (pixel >> 24) & 0xFF;
            if (alpha == 0) {
                continue;
            }

            totalR += (pixel >> 16) & 0xFF;
            totalG += (pixel >> 8) & 0xFF;
            totalB += pixel & 0xFF;
            count++;
        }

        if (count == 0) {
            return null;
        }

        return new float[] { totalR / (count * 255.0F), totalG / (count * 255.0F), totalB / (count * 255.0F) };
    }

    private static float[] colorToRgb(int color) {
        return new float[] { ((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F };
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
