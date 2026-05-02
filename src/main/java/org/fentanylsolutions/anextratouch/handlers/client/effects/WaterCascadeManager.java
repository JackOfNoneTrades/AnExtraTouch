package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
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
    private static final int MAX_ACTIVE_WATERFALL_SOUNDS = 6;
    private static final ResourceLocation[] WATERFALL_SOUND_VARIANTS = new ResourceLocation[11];

    private static final IIcon[] CASCADE_ICONS = new IIcon[CASCADE_FRAME_COUNT];
    private static IIcon sprayIcon;

    private final Map<ChunkPosition, Float> cascades = new HashMap<ChunkPosition, Float>();
    private final Map<ChunkPosition, WaterfallLoopSound> waterfallSounds = new HashMap<ChunkPosition, WaterfallLoopSound>();
    private World currentWorld;
    private boolean needsNearbyRescan = true;
    private boolean wasCascadeEnabled;

    static {
        WATERFALL_SOUND_VARIANTS[0] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.0");
        WATERFALL_SOUND_VARIANTS[1] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.0");
        WATERFALL_SOUND_VARIANTS[2] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.1");
        WATERFALL_SOUND_VARIANTS[3] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.1");
        WATERFALL_SOUND_VARIANTS[4] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.2");
        WATERFALL_SOUND_VARIANTS[5] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.3");
        WATERFALL_SOUND_VARIANTS[6] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.3");
        WATERFALL_SOUND_VARIANTS[7] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.4");
        WATERFALL_SOUND_VARIANTS[8] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.4");
        WATERFALL_SOUND_VARIANTS[9] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.5");
        WATERFALL_SOUND_VARIANTS[10] = new ResourceLocation(AnExtraTouch.MODID, "waterfall.5");
    }

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
            stopAllWaterfallSounds();
            currentWorld = null;
            needsNearbyRescan = true;
            wasCascadeEnabled = false;
            return;
        }

        if (currentWorld != world) {
            cascades.clear();
            stopAllWaterfallSounds();
            currentWorld = world;
            needsNearbyRescan = true;
        }

        if (!Config.waterCascadeEnabled) {
            cascades.clear();
            stopAllWaterfallSounds();
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

        updateWaterfallSounds(mc, world);
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
                removeWaterfallSound(pos);
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
            removeWaterfallSound(pos);
        }
    }

    public boolean isWaterfallImpact(World world, int x, int y, int z) {
        return getCascadeStrength(world, x, y, z) > 0.0F;
    }

    private float getCascadeStrength(World world, int x, int y, int z) {
        if (y <= 1 || y >= 255) {
            return 0.0F;
        }

        if (!isFlowingWater(world, x, y, z)) {
            return 0.0F;
        }
        if (!isStillWaterSurface(world, x, y - 1, z)) {
            return 0.0F;
        }
        if (!isWater(world, x, y - 2, z)) {
            return 0.0F;
        }

        if (!world.isAirBlock(x, y, z - 1)
            && !world.isAirBlock(x + 1, y, z)
            && !world.isAirBlock(x, y, z + 1)
            && !world.isAirBlock(x - 1, y, z)) {
            return 0.0F;
        }

        int strength = 0;
        if (isWater(world, x, y - 1, z - 1)) strength++;
        if (isWater(world, x + 1, y - 1, z)) strength++;
        if (isWater(world, x, y - 1, z + 1)) strength++;
        if (isWater(world, x - 1, y - 1, z)) strength++;

        if (strength <= 0) {
            return 0.0F;
        }

        return (float) strength;
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

        // Particular spawns one foam particle per cascade per tick along a random edge, with Y
        // sampled inside the falling water column.
        double x = pos.chunkPosX;
        double z = pos.chunkPosZ;
        if (world.rand.nextBoolean()) {
            x += world.rand.nextDouble();
            z += world.rand.nextInt(2);
        } else {
            x += world.rand.nextInt(2);
            z += world.rand.nextDouble();
        }

        float columnHeight = getWaterHeight(world, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ);
        double y = pos.chunkPosY + world.rand.nextDouble() * columnHeight;

        CascadeFX cascade = new CascadeFX(world, x, y, z);
        float size = strength / 4.0F * columnHeight;
        cascade.multipleParticleScaleBy(1.0F - (1.0F - size) / 2.0F);
        mc.effectRenderer.addEffect(cascade);
    }

    private void updateWaterfallSounds(Minecraft mc, World world) {
        if (!Config.waterfallSoundEnabled || Config.waterfallSoundVolume <= 0.0F) {
            stopAllWaterfallSounds();
            return;
        }

        if (mc.renderViewEntity == null || mc.getSoundHandler() == null) {
            stopAllWaterfallSounds();
            return;
        }

        final double maxDistanceSq = 48.0D * 48.0D;
        final double viewerX = mc.renderViewEntity.posX;
        final double viewerY = mc.renderViewEntity.posY;
        final double viewerZ = mc.renderViewEntity.posZ;

        List<SoundTarget> candidates = new ArrayList<SoundTarget>();
        for (Map.Entry<ChunkPosition, Float> entry : cascades.entrySet()) {
            float strength = entry.getValue()
                .floatValue();
            if (strength < Config.waterfallSoundCutoff) {
                continue;
            }

            ChunkPosition pos = entry.getKey();
            double soundX = pos.chunkPosX + 0.5D;
            double soundY = pos.chunkPosY + 0.35D;
            double soundZ = pos.chunkPosZ + 0.5D;
            double dx = viewerX - soundX;
            double dy = viewerY - soundY;
            double dz = viewerZ - soundZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq <= maxDistanceSq) {
                candidates.add(new SoundTarget(pos, strength, soundX, soundY, soundZ, distanceSq));
            }
        }

        Collections.sort(candidates, SoundTarget.BY_DISTANCE);
        Set<ChunkPosition> keep = new HashSet<ChunkPosition>();
        int limit = Math.min(candidates.size(), MAX_ACTIVE_WATERFALL_SOUNDS);

        for (int i = 0; i < limit; i++) {
            SoundTarget target = candidates.get(i);
            keep.add(target.pos);

            WaterfallLoopSound sound = waterfallSounds.get(target.pos);
            if (sound == null) {
                ResourceLocation soundId = getWaterfallSoundId(target.strength);
                sound = new WaterfallLoopSound(soundId, randomizePitch(world));
                waterfallSounds.put(target.pos, sound);
            }

            sound.setState(target.x, target.y, target.z, getWaterfallSoundVolume(target.strength));
            if (!mc.getSoundHandler()
                .isSoundPlaying(sound)) {
                mc.getSoundHandler()
                    .playSound(sound);
            }
        }

        Iterator<Map.Entry<ChunkPosition, WaterfallLoopSound>> iterator = waterfallSounds.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChunkPosition, WaterfallLoopSound> entry = iterator.next();
            if (!keep.contains(entry.getKey())) {
                stopSound(entry.getValue());
                iterator.remove();
            }
        }
    }

    private void stopAllWaterfallSounds() {
        Iterator<WaterfallLoopSound> iterator = waterfallSounds.values()
            .iterator();
        while (iterator.hasNext()) {
            stopSound(iterator.next());
        }
        waterfallSounds.clear();
    }

    private void removeWaterfallSound(ChunkPosition pos) {
        WaterfallLoopSound sound = waterfallSounds.remove(pos);
        if (sound != null) {
            stopSound(sound);
        }
    }

    private void stopSound(WaterfallLoopSound sound) {
        sound.stop();
        Minecraft mc = Minecraft.getMinecraft();
        SoundHandler handler = mc.getSoundHandler();
        if (handler != null) {
            handler.stopSound(sound);
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

    private ResourceLocation getWaterfallSoundId(float strength) {
        int index = MathHelper.clamp_int(Math.round(strength * 4.0F), 0, WATERFALL_SOUND_VARIANTS.length - 1);
        return WATERFALL_SOUND_VARIANTS[index];
    }

    private float getWaterfallSoundVolume(float strength) {
        float normalizedStrength = MathHelper.clamp_float(strength, 0.0F, 2.0F);
        return Config.waterfallSoundVolume * (0.45F + normalizedStrength * 0.45F);
    }

    private float randomizePitch(World world) {
        return 1.0F + 0.2F * (world.rand.nextFloat() - world.rand.nextFloat());
    }

    private static class SoundTarget {

        private static final Comparator<SoundTarget> BY_DISTANCE = new Comparator<SoundTarget>() {

            @Override
            public int compare(SoundTarget left, SoundTarget right) {
                return Double.compare(left.distanceSq, right.distanceSq);
            }
        };

        final ChunkPosition pos;
        final float strength;
        final double x;
        final double y;
        final double z;
        final double distanceSq;

        SoundTarget(ChunkPosition pos, float strength, double x, double y, double z, double distanceSq) {
            this.pos = pos;
            this.strength = strength;
            this.x = x;
            this.y = y;
            this.z = z;
            this.distanceSq = distanceSq;
        }
    }
}
