package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;

import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WetnessFluidHelper {

    private static final FluidBlacklist fluidInteractionBlacklist = new FluidBlacklist();
    private static final FluidBlacklist splashFluidBlacklist = new FluidBlacklist();
    private static final FluidBlacklist cascadeFluidBlacklist = new FluidBlacklist();

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
                    } else if (block.getMaterial() == Material.water && !isIgnoredFluid(block, FluidRegistry.WATER)
                        && intersectsVanillaLiquid(bb, world, x, y, z)) {
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
            Fluid fluid = getInteractableFluid(world, x, y, z, block, fluidBlock);
            if (!isWettableFluid(world, x, y, z, block, fluid)) {
                return -1.0D;
            }

            return getForgeFluidSurfaceY(world, x, y, z, fluidBlock);
        }

        if (block.getMaterial() == Material.water && !isIgnoredFluid(block, FluidRegistry.WATER)) {
            return getVanillaLiquidSurfaceY(world, x, y, z);
        }

        return -1.0D;
    }

    static float getWettableFluidHeight(World world, int x, int y, int z) {
        double surfaceY = getWettableFluidSurfaceY(world, x, y, z);
        return surfaceY < 0.0D ? -1.0F : (float) (surfaceY - y);
    }

    static boolean isWettableFluidAt(World world, double x, double y, double z) {
        return getWettableFluidSurfaceY(
            world,
            MathHelper.floor_double(x),
            MathHelper.floor_double(y),
            MathHelper.floor_double(z)) >= 0.0D;
    }

    static boolean isFluidInteractionAllowed(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        Fluid fluid = block instanceof IFluidBlock ? ((IFluidBlock) block).getFluid()
            : block.getMaterial() == Material.water ? FluidRegistry.WATER : null;
        return !isIgnoredFluid(block, fluid);
    }

    static Fluid getInteractableFluid(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof IFluidBlock) {
            return getInteractableFluid(world, x, y, z, block, (IFluidBlock) block);
        }

        if (block.getMaterial() == Material.water && !isIgnoredFluid(block, FluidRegistry.WATER)) {
            return FluidRegistry.WATER;
        }

        return null;
    }

    static Fluid getCascadeFluid(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof IFluidBlock) {
            Fluid fluid = ((IFluidBlock) block).getFluid();
            return isCascadeFluid(world, x, y, z, block, fluid) ? fluid : null;
        }

        if (block.getMaterial() == Material.water && !isCascadeIgnoredFluid(block, FluidRegistry.WATER)) {
            return FluidRegistry.WATER;
        }

        return null;
    }

    static Fluid getSplashFluid(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof IFluidBlock) {
            Fluid fluid = ((IFluidBlock) block).getFluid();
            return isSplashFluid(world, x, y, z, block, fluid) ? fluid : null;
        }

        if (block.getMaterial() == Material.water && !isSplashIgnoredFluid(block, FluidRegistry.WATER)) {
            return FluidRegistry.WATER;
        }

        return null;
    }

    static boolean isSameInteractableFluid(World world, int x, int y, int z, Fluid fluid) {
        Fluid other = getInteractableFluid(world, x, y, z);
        return other != null && other == fluid;
    }

    static boolean isSameCascadeFluid(World world, int x, int y, int z, Fluid fluid) {
        Fluid other = getCascadeFluid(world, x, y, z);
        return other != null && other == fluid;
    }

    static double getFluidFlowDirection(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof BlockLiquid && block.getMaterial() == Material.water) {
            return BlockLiquid.getFlowDirection(world, x, y, z, Material.water);
        }
        if (block instanceof BlockFluidBase) {
            return BlockFluidBase.getFlowDirection(world, x, y, z);
        }
        return -1000.0D;
    }

    static float[] getWettableFluidColor(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        Fluid fluid = getInteractableFluid(world, x, y, z);
        return fluid == null ? FallingWaterFX.getWaterColor(world, x + 0.5D, y + 0.5D, z + 0.5D)
            : getFluidColor(world, x, y, z, block, fluid);
    }

    public static float[] getFluidColorAtOrBelow(World world, double x, double y, double z) {
        return getFluidColorAtOrBelow(world, x, y, z, false);
    }

    public static float[] getNonWaterFluidColorAtOrBelow(World world, double x, double y, double z) {
        return getFluidColorAtOrBelow(world, x, y, z, true);
    }

    private static float[] getFluidColorAtOrBelow(World world, double x, double y, double z, boolean nonWaterOnly) {
        int blockX = MathHelper.floor_double(x);
        int blockY = MathHelper.floor_double(y);
        int blockZ = MathHelper.floor_double(z);

        float[] rgb = getInteractableFluidColor(world, blockX, blockY, blockZ, nonWaterOnly);
        if (rgb != null) {
            return rgb;
        }

        rgb = getInteractableFluidColor(world, blockX, blockY - 1, blockZ, nonWaterOnly);
        if (rgb != null) {
            return rgb;
        }

        if (nonWaterOnly) {
            return null;
        }

        return FallingWaterFX.getWaterColor(world, x, y, z);
    }

    public static float[] getFluidColorNearOrBelow(World world, double x, double y, double z) {
        return getFluidColorNearOrBelow(world, x, y, z, false);
    }

    public static float[] getNonWaterFluidColorNearOrBelow(World world, double x, double y, double z) {
        return getFluidColorNearOrBelow(world, x, y, z, true);
    }

    private static float[] getFluidColorNearOrBelow(World world, double x, double y, double z, boolean nonWaterOnly) {
        int blockX = MathHelper.floor_double(x);
        int blockY = MathHelper.floor_double(y);
        int blockZ = MathHelper.floor_double(z);

        float[] rgb = getInteractableFluidColor(world, blockX, blockY, blockZ, nonWaterOnly);
        if (rgb != null) {
            return rgb;
        }

        rgb = getInteractableFluidColor(world, blockX, blockY - 1, blockZ, nonWaterOnly);
        if (rgb != null) {
            return rgb;
        }

        for (int dy = 0; dy >= -2; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }

                    rgb = getInteractableFluidColor(world, blockX + dx, blockY + dy, blockZ + dz, nonWaterOnly);
                    if (rgb != null) {
                        return rgb;
                    }
                }
            }
        }

        if (nonWaterOnly) {
            return null;
        }

        return FallingWaterFX.getWaterColor(world, x, y, z);
    }

    static boolean isInsideInteractableFluid(World world, double x, double y, double z) {
        int blockX = MathHelper.floor_double(x);
        int blockY = MathHelper.floor_double(y);
        int blockZ = MathHelper.floor_double(z);
        return getInteractableFluid(world, blockX, blockY, blockZ) != null;
    }

    private static float[] getInteractableFluidColor(World world, int blockX, int blockY, int blockZ) {
        return getInteractableFluidColor(world, blockX, blockY, blockZ, false);
    }

    private static float[] getInteractableFluidColor(World world, int blockX, int blockY, int blockZ,
        boolean nonWaterOnly) {
        Fluid fluid = getInteractableFluid(world, blockX, blockY, blockZ);
        if (fluid != null && (!nonWaterOnly || fluid != FluidRegistry.WATER)) {
            return getWettableFluidColor(world, blockX, blockY, blockZ);
        }

        return null;
    }

    private static boolean isWettableFluid(World world, int x, int y, int z, Block block, Fluid fluid) {
        if (fluid == null || fluid == FluidRegistry.LAVA
            || block.getMaterial() == Material.lava
            || isIgnoredFluid(block, fluid)) {
            return false;
        }

        return !fluid.isGaseous(world, x, y, z);
    }

    private static boolean isCascadeFluid(World world, int x, int y, int z, Block block, Fluid fluid) {
        return isWettableFluid(world, x, y, z, block, fluid) && !isCascadeIgnoredFluid(block, fluid);
    }

    private static boolean isSplashFluid(World world, int x, int y, int z, Block block, Fluid fluid) {
        return isWettableFluid(world, x, y, z, block, fluid) && !isSplashIgnoredFluid(block, fluid);
    }

    private static Fluid getInteractableFluid(World world, int x, int y, int z, Block block, IFluidBlock fluidBlock) {
        Fluid fluid = fluidBlock.getFluid();
        return isWettableFluid(world, x, y, z, block, fluid) ? fluid : null;
    }

    private static boolean isIgnoredFluid(Block block, Fluid fluid) {
        return fluidInteractionBlacklist.isIgnored(block, fluid, Config.fluidInteractionBlacklist);
    }

    private static boolean isSplashIgnoredFluid(Block block, Fluid fluid) {
        return isIgnoredFluid(block, fluid)
            || splashFluidBlacklist.isIgnored(block, fluid, Config.splashFluidBlacklist);
    }

    private static boolean isCascadeIgnoredFluid(Block block, Fluid fluid) {
        return isIgnoredFluid(block, fluid)
            || cascadeFluidBlacklist.isIgnored(block, fluid, Config.cascadeFluidBlacklist);
    }

    private static String normalizeName(String name) {
        return name == null ? ""
            : name.trim()
                .toLowerCase(Locale.ENGLISH);
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

    private static final class FluidBlacklist {

        private String[] cachedEntries = new String[0];
        private String[] cachedEntriesRef;
        private final Set<String> ignoredExactNames = new HashSet<String>();
        private final Set<String> ignoredBareNames = new HashSet<String>();
        private final Map<Block, Boolean> ignoredBlockCache = new IdentityHashMap<Block, Boolean>();

        boolean isIgnored(Block block, Fluid fluid, String[] entries) {
            refresh(entries);
            if (this.ignoredExactNames.isEmpty()) {
                return false;
            }

            Boolean cached = this.ignoredBlockCache.get(block);
            if (cached != null) {
                return cached.booleanValue();
            }

            String fluidName = fluid == null ? null : FluidRegistry.getFluidName(fluid);
            String fallbackFluidName = fluid == null ? null : fluid.getName();
            String blockName = Block.blockRegistry.getNameForObject(block);

            boolean ignored = isNameIgnored(fluidName) || isNameIgnored(fallbackFluidName) || isNameIgnored(blockName);
            this.ignoredBlockCache.put(block, Boolean.valueOf(ignored));
            return ignored;
        }

        private void refresh(String[] entries) {
            if (entries == null) {
                entries = new String[0];
            }

            if (entries == this.cachedEntriesRef) {
                return;
            }

            if (Arrays.equals(this.cachedEntries, entries)) {
                this.cachedEntriesRef = entries;
                return;
            }

            this.cachedEntriesRef = entries;
            this.cachedEntries = entries.clone();
            this.ignoredExactNames.clear();
            this.ignoredBareNames.clear();
            this.ignoredBlockCache.clear();

            for (String rawEntry : entries) {
                String entry = normalizeName(rawEntry);
                if (entry.length() == 0) {
                    continue;
                }

                this.ignoredExactNames.add(entry);
                if (!entry.contains(":")) {
                    this.ignoredBareNames.add(entry);
                }
            }
        }

        private boolean isNameIgnored(String name) {
            if (name == null) {
                return false;
            }

            String normalizedName = normalizeName(name);
            if (this.ignoredExactNames.contains(normalizedName)) {
                return true;
            }

            int namespaceSeparator = normalizedName.indexOf(':');
            return namespaceSeparator >= 0
                && this.ignoredBareNames.contains(normalizedName.substring(namespaceSeparator + 1));
        }
    }
}
