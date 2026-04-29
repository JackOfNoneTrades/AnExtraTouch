package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.world.ChunkEvent;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WaterCascadeManager {

    public static final WaterCascadeManager INSTANCE = new WaterCascadeManager();
    static final int CASCADE_FRAME_COUNT = 12;

    private static final IIcon[] CASCADE_ICONS = new IIcon[CASCADE_FRAME_COUNT];
    private static IIcon sprayIcon;

    private final Map<ChunkPosition, Float> cascades = new HashMap<ChunkPosition, Float>();
    private World currentWorld;
    private boolean needsNearbyRescan = true;
    private boolean wasCascadeEnabled;

    static IIcon getCascadeIcon(int frame) {
        if (frame < 0 || frame >= CASCADE_FRAME_COUNT) {
            return null;
        }
        return CASCADE_ICONS[frame];
    }

    public static IIcon getSprayIcon() {
        return sprayIcon;
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) {
            return;
        }

        for (int i = 0; i < CASCADE_FRAME_COUNT; i++) {
            CASCADE_ICONS[i] = event.map.registerIcon(AnExtraTouch.MODID + ":cascade_" + i);
        }
        sprayIcon = event.map.registerIcon(AnExtraTouch.MODID + ":generic_0");
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.world.isRemote) {
            return;
        }

        Chunk chunk = event.getChunk();
        removeChunk(chunk.xPosition, chunk.zPosition);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null) {
            cascades.clear();
            currentWorld = null;
            needsNearbyRescan = true;
            wasCascadeEnabled = false;
            return;
        }

        if (currentWorld != world) {
            cascades.clear();
            currentWorld = world;
            needsNearbyRescan = true;
        }

        if (!Config.waterCascadeEnabled) {
            cascades.clear();
            needsNearbyRescan = true;
            wasCascadeEnabled = false;
            return;
        }

        if (!wasCascadeEnabled) {
            needsNearbyRescan = true;
        }
        wasCascadeEnabled = true;

        if (needsNearbyRescan) {
            needsNearbyRescan = !rescanNearbyChunks(mc);
        }

        Iterator<Map.Entry<ChunkPosition, Float>> iterator = cascades.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChunkPosition, Float> entry = iterator.next();
            ChunkPosition pos = entry.getKey();
            float strength = getCascadeStrength(world, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ);
            if (strength <= 0.0F) {
                iterator.remove();
                continue;
            }
            if (Float.compare(
                strength,
                entry.getValue()
                    .floatValue())
                != 0) {
                entry.setValue(Float.valueOf(strength));
            }
            spawnCascade(world, pos, strength);
        }
    }

    public void onChunkFilled(Chunk chunk) {
        if (chunk == null || chunk.worldObj == null || !chunk.worldObj.isRemote) {
            return;
        }

        if (!Config.waterCascadeEnabled) {
            return;
        }

        scanChunk(chunk);
    }

    public void onBlockChanged(Chunk chunk, int localX, int y, int localZ) {
        if (chunk == null || chunk.worldObj == null || !chunk.worldObj.isRemote) {
            return;
        }

        if (!Config.waterCascadeEnabled) {
            return;
        }

        int x = (chunk.xPosition << 4) + localX;
        int z = (chunk.zPosition << 4) + localZ;
        updateAround(chunk.worldObj, x, y, z);
    }

    private boolean rescanNearbyChunks(Minecraft mc) {
        if (mc.renderViewEntity == null) {
            return false;
        }

        int centerChunkX = MathHelper.floor_double(mc.renderViewEntity.posX) >> 4;
        int centerChunkZ = MathHelper.floor_double(mc.renderViewEntity.posZ) >> 4;
        int radius = mc.gameSettings.renderDistanceChunks + 1;

        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                scanChunk(mc.theWorld.getChunkFromChunkCoords(chunkX, chunkZ));
            }
        }

        return true;
    }

    private void scanChunk(Chunk chunk) {
        removeChunk(chunk.xPosition, chunk.zPosition);

        ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();
        for (ExtendedBlockStorage storage : storages) {
            if (storage == null || storage.isEmpty()) {
                continue;
            }

            int baseY = storage.getYLocation();
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        if (storage.getBlockByExtId(localX, localY, localZ)
                            .getMaterial() != Material.water) {
                            continue;
                        }

                        int x = (chunk.xPosition << 4) + localX;
                        int y = baseY + localY;
                        int z = (chunk.zPosition << 4) + localZ;
                        updateCascade(chunk.worldObj, x, y, z);
                    }
                }
            }
        }
    }

    private void updateAround(World world, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    updateCascade(world, x + dx, y + dy, z + dz);
                }
            }
        }
    }

    private void removeChunk(int chunkX, int chunkZ) {
        Iterator<ChunkPosition> iterator = cascades.keySet()
            .iterator();
        while (iterator.hasNext()) {
            ChunkPosition pos = iterator.next();
            if ((pos.chunkPosX >> 4) == chunkX && (pos.chunkPosZ >> 4) == chunkZ) {
                iterator.remove();
            }
        }
    }

    private void updateCascade(World world, int x, int y, int z) {
        ChunkPosition pos = new ChunkPosition(x, y, z);
        float strength = getCascadeStrength(world, x, y, z);
        if (strength > 0.0F) {
            cascades.put(pos, Float.valueOf(strength));
        } else {
            cascades.remove(pos);
        }
    }

    public boolean isWaterfallImpact(World world, int x, int y, int z) {
        return getCascadeStrength(world, x, y, z) > 0.0F;
    }

    private float getCascadeStrength(World world, int x, int y, int z) {
        if (y <= 1 || y >= 255) {
            return 0.0F;
        }

        if (!isImpactFlowingWater(world, x, y, z)) {
            return 0.0F;
        }
        if (!isWater(world, x, y + 1, z)) {
            return 0.0F;
        }
        if (!isStillWaterSurface(world, x, y - 1, z)) {
            return 0.0F;
        }

        boolean foundAir = false;
        for (int dx = -1; dx <= 1 && !foundAir; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (world.isAirBlock(x + dx, y, z + dz)) {
                    foundAir = true;
                    break;
                }
            }
        }
        if (!foundAir) {
            return 0.0F;
        }

        float landingSize = 0.0F;
        if (isWater(world, x, y - 1, z - 1)) landingSize += 1.0F;
        if (isWater(world, x + 1, y - 1, z)) landingSize += 1.0F;
        if (isWater(world, x, y - 1, z + 1)) landingSize += 1.0F;
        if (isWater(world, x - 1, y - 1, z)) landingSize += 1.0F;

        if (landingSize < 1.0F) {
            return 0.0F;
        }

        return getWaterHeight(world, x, y, z) + (landingSize - 2.0F) / 2.0F;
    }

    private boolean isFlowingWater(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block.getMaterial() != Material.water) {
            return false;
        }

        return block == net.minecraft.init.Blocks.flowing_water || world.getBlockMetadata(x, y, z) >= 8;
    }

    private boolean isImpactFlowingWater(World world, int x, int y, int z) {
        if (!isWater(world, x, y, z)) {
            return false;
        }

        int meta = world.getBlockMetadata(x, y, z);
        return meta >= 8 || getWaterFlowDirection(world, x, y, z) > -999.0D;
    }

    private boolean isStillWaterSurface(World world, int x, int y, int z) {
        if (!isWater(world, x, y, z)) {
            return false;
        }

        int meta = world.getBlockMetadata(x, y, z);
        return meta < 8 && getWaterFlowDirection(world, x, y, z) <= -999.0D;
    }

    private void spawnCascade(World world, ChunkPosition pos, float strength) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.effectRenderer == null || CASCADE_ICONS[0] == null) {
            return;
        }

        int particleCount = strength >= 1.6F ? 3 : 2;
        for (int i = 0; i < particleCount; i++) {
            double offsetX = world.rand.nextGaussian() / 5.0D;
            double offsetZ = world.rand.nextGaussian() / 5.0D;
            double signX = offsetX == 0.0D ? (world.rand.nextBoolean() ? 1.0D : -1.0D) : Math.signum(offsetX);
            double signZ = offsetZ == 0.0D ? (world.rand.nextBoolean() ? 1.0D : -1.0D) : Math.signum(offsetZ);

            CascadeFX cascade = new CascadeFX(
                world,
                pos.chunkPosX + 0.5D + offsetX,
                pos.chunkPosY + 0.1D + world.rand.nextDouble() * 0.35D,
                pos.chunkPosZ + 0.5D + offsetZ,
                world.rand.nextFloat() * strength / 10.0F * signX,
                world.rand.nextFloat() * strength / 10.0F,
                world.rand.nextFloat() * strength / 10.0F * signZ);
            mc.effectRenderer.addEffect(cascade);
        }
    }

    private float getWaterHeight(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (!(block instanceof BlockLiquid)) {
            return 1.0F;
        }

        return 1.0F - BlockLiquid.getLiquidHeightPercent(world.getBlockMetadata(x, y, z));
    }

    private boolean isWater(World world, int x, int y, int z) {
        return world.getBlock(x, y, z)
            .getMaterial() == Material.water;
    }

    private double getWaterFlowDirection(World world, int x, int y, int z) {
        return BlockLiquid.getFlowDirection(world, x, y, z, Material.water);
    }
}
