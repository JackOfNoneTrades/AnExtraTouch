package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ChestBubbleManager {

    private static final float ENDER_RED = 0.55F;
    private static final float ENDER_GREEN = 0.25F;
    private static final float ENDER_BLUE = 1.0F;

    private ChestBubbleManager() {}

    public static boolean canSpawnOpenBubbles(World world, int x, int y, int z, BlockChest chest) {
        ChestArea area = getChestArea(world, x, y, z);
        return area != null && hasWaterAbove(world, area) && canOpen(world, x, y, z, chest);
    }

    public static boolean canSpawnEnderOpenBubbles(World world, int x, int y, int z) {
        return world.getBlock(x, y, z) == Blocks.ender_chest
            && world.getTileEntity(x, y, z) instanceof TileEntityEnderChest
            && isWater(world, x, y + 1, z);
    }

    public static boolean canRunSoulSandBubbles(TileEntityChest chest) {
        if (chest == null || !chest.hasWorldObj()) {
            return false;
        }

        World world = chest.getWorldObj();
        ChestArea area = getChestArea(world, chest.xCoord, chest.yCoord, chest.zCoord);
        if (area == null || !area.isPrimary(chest.xCoord, chest.zCoord)) {
            return false;
        }

        Block block = world.getBlock(chest.xCoord, chest.yCoord, chest.zCoord);
        return block instanceof BlockChest && hasWaterAbove(world, area)
            && hasSoulSandBelow(world, area)
            && canOpen(world, chest.xCoord, chest.yCoord, chest.zCoord, (BlockChest) block);
    }

    public static boolean canRunSoulSandBubbles(TileEntityEnderChest chest) {
        if (chest == null || !chest.hasWorldObj()) {
            return false;
        }

        World world = chest.getWorldObj();
        return world.getBlock(chest.xCoord, chest.yCoord, chest.zCoord) == Blocks.ender_chest
            && isWater(world, chest.xCoord, chest.yCoord + 1, chest.zCoord)
            && world.getBlock(chest.xCoord, chest.yCoord - 1, chest.zCoord) == Blocks.soul_sand;
    }

    public static void spawnOpenBubbles(World world, int x, int y, int z) {
        ChestArea area = getChestArea(world, x, y, z);
        if (area == null) {
            return;
        }

        spawnBubbles(world, area, area.isDouble() ? 20 : 10);
    }

    public static void spawnEnderOpenBubbles(World world, int x, int y, int z) {
        spawnEnderBubbles(world, x, y, z, 10);
    }

    public static void spawnVentBubbles(TileEntityChest chest) {
        if (chest == null || !chest.hasWorldObj()) {
            return;
        }

        ChestArea area = getChestArea(chest.getWorldObj(), chest.xCoord, chest.yCoord, chest.zCoord);
        if (area != null) {
            spawnBubbles(chest.getWorldObj(), area, area.isDouble() ? 2 : 1);
        }
    }

    public static void spawnEnderVentBubbles(TileEntityEnderChest chest) {
        if (chest != null && chest.hasWorldObj()) {
            spawnEnderBubbles(chest.getWorldObj(), chest.xCoord, chest.yCoord, chest.zCoord, 1);
        }
    }

    public static void playVentOpenSound(TileEntityChest chest) {
        if (chest == null || !chest.hasWorldObj()) {
            return;
        }

        playVentOpenSound(chest.getWorldObj(), chest.xCoord, chest.yCoord, chest.zCoord);
    }

    public static void playVentOpenSound(TileEntityEnderChest chest) {
        if (chest != null && chest.hasWorldObj()) {
            playVentOpenSound(chest.getWorldObj(), chest.xCoord, chest.yCoord, chest.zCoord);
        }
    }

    private static boolean canOpen(World world, int x, int y, int z, BlockChest chest) {
        IInventory inventory = chest.func_149951_m(world, x, y, z);
        return inventory != null;
    }

    private static boolean hasWaterAbove(World world, ChestArea area) {
        for (int x = area.minX; x <= area.maxX; x++) {
            for (int z = area.minZ; z <= area.maxZ; z++) {
                if (isChestBlock(world, x, area.y, z, area.chestBlock) && !isWater(world, x, area.y + 1, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasSoulSandBelow(World world, ChestArea area) {
        for (int x = area.minX; x <= area.maxX; x++) {
            for (int z = area.minZ; z <= area.maxZ; z++) {
                if (isChestBlock(world, x, area.y, z, area.chestBlock)
                    && world.getBlock(x, area.y - 1, z) == Blocks.soul_sand) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isWater(World world, int x, int y, int z) {
        return world.getBlock(x, y, z)
            .getMaterial() == Material.water;
    }

    private static void spawnBubbles(World world, ChestArea area, int count) {
        spawnBubbles(world, area, count, null, 1.0F, 1.0F, 1.0F);
    }

    private static void spawnBubbles(World world, ChestArea area, int count, Block sourceBlock, float red, float green,
        float blue) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.effectRenderer == null) {
            return;
        }

        double xSpan = (double) (area.maxX - area.minX) + 0.5D;
        double zSpan = (double) (area.maxZ - area.minZ) + 0.5D;
        for (int i = 0; i < count; i++) {
            double x = (double) area.minX + 0.25D + world.rand.nextDouble() * xSpan;
            double y = (double) area.y + 0.25D + world.rand.nextDouble() * 0.5D;
            double z = (double) area.minZ + 0.25D + world.rand.nextDouble() * zSpan;
            mc.effectRenderer.addEffect(new ChestBubbleFX(world, x, y, z, sourceBlock, red, green, blue));
        }
    }

    private static void spawnEnderBubbles(World world, int x, int y, int z, int count) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.effectRenderer == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            double particleX = (double) x + 0.25D + world.rand.nextDouble() * 0.5D;
            double particleY = (double) y + 0.25D + world.rand.nextDouble() * 0.5D;
            double particleZ = (double) z + 0.25D + world.rand.nextDouble() * 0.5D;
            mc.effectRenderer.addEffect(
                new ChestBubbleFX(
                    world,
                    particleX,
                    particleY,
                    particleZ,
                    Blocks.ender_chest,
                    ENDER_RED,
                    ENDER_GREEN,
                    ENDER_BLUE));
        }
    }

    private static void playVentOpenSound(World world, int x, int y, int z) {
        world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "liquid.water", 1.0F, 1.0F);
    }

    private static ChestArea getChestArea(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (!(block instanceof BlockChest)) {
            return null;
        }

        ChestArea area = new ChestArea(block, x, y, z);
        area.includeIfChest(world, x - 1, y, z);
        area.includeIfChest(world, x + 1, y, z);
        area.includeIfChest(world, x, y, z - 1);
        area.includeIfChest(world, x, y, z + 1);
        return area;
    }

    private static boolean isChestBlock(World world, int x, int y, int z, Block chestBlock) {
        return world.getBlock(x, y, z) == chestBlock;
    }

    private static class ChestArea {

        final Block chestBlock;
        final int y;
        int minX;
        int maxX;
        int minZ;
        int maxZ;

        ChestArea(Block chestBlock, int x, int y, int z) {
            this.chestBlock = chestBlock;
            this.y = y;
            this.minX = x;
            this.maxX = x;
            this.minZ = z;
            this.maxZ = z;
        }

        void includeIfChest(World world, int x, int y, int z) {
            if (!isChestBlock(world, x, y, z, this.chestBlock)) {
                return;
            }

            if (x < this.minX) this.minX = x;
            if (x > this.maxX) this.maxX = x;
            if (z < this.minZ) this.minZ = z;
            if (z > this.maxZ) this.maxZ = z;
        }

        boolean isDouble() {
            return this.minX != this.maxX || this.minZ != this.maxZ;
        }

        boolean isPrimary(int x, int z) {
            return x == this.minX && z == this.minZ;
        }
    }
}
