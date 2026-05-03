package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.fentlib.util.sound.SoundUtil;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

@SideOnly(Side.CLIENT)
public class WaterCascadeManager {

    public static final WaterCascadeManager INSTANCE = new WaterCascadeManager();
    static final int CASCADE_FRAME_COUNT = 12;
    private static final int MAX_ACTIVE_WATERFALL_SOUNDS = 6;
    private static final int SHADER_PACK_SOUND_CHECK_INTERVAL = 20;
    private static final ResourceLocation[] WATERFALL_SOUND_VARIANTS = new ResourceLocation[11];

    private static final IIcon[] CASCADE_ICONS = new IIcon[CASCADE_FRAME_COUNT];
    private static IIcon sprayIcon;

    private final Long2FloatOpenHashMap cascades = new Long2FloatOpenHashMap();
    private final Long2ObjectOpenHashMap<WaterfallLoopSound> waterfallSounds = new Long2ObjectOpenHashMap<WaterfallLoopSound>();
    private final List<SoundTarget> soundCandidates = new ArrayList<SoundTarget>();
    private final LongOpenHashSet soundKeysToKeep = new LongOpenHashSet();
    private World currentWorld;
    private boolean needsNearbyRescan = true;
    private boolean wasCascadeEnabled;
    private boolean wasGamePaused;
    private boolean waterfallSoundsNeedRefresh;
    private int shaderPackSoundCheckTicks;
    private Boolean lastAngelicaShaderPackInUse;

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
    public void onSoundLoad(SoundLoadEvent event) {
        onSoundSystemStopped();
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
            wasGamePaused = false;
            waterfallSoundsNeedRefresh = false;
            shaderPackSoundCheckTicks = 0;
            lastAngelicaShaderPackInUse = null;
            return;
        }

        if (currentWorld != world) {
            cascades.clear();
            stopAllWaterfallSounds();
            currentWorld = world;
            needsNearbyRescan = true;
            wasGamePaused = false;
            waterfallSoundsNeedRefresh = false;
            shaderPackSoundCheckTicks = 0;
            lastAngelicaShaderPackInUse = null;
        }

        if (!Config.waterCascadeEnabled) {
            cascades.clear();
            stopAllWaterfallSounds();
            needsNearbyRescan = true;
            wasCascadeEnabled = false;
            wasGamePaused = false;
            waterfallSoundsNeedRefresh = false;
            shaderPackSoundCheckTicks = 0;
            lastAngelicaShaderPackInUse = null;
            return;
        }

        if (!wasCascadeEnabled) {
            needsNearbyRescan = true;
        }
        wasCascadeEnabled = true;

        if (needsNearbyRescan) {
            needsNearbyRescan = !rescanNearbyChunks(mc);
        }

        ObjectIterator<Long2FloatMap.Entry> iterator = cascades.long2FloatEntrySet()
            .fastIterator();
        while (iterator.hasNext()) {
            Long2FloatMap.Entry entry = iterator.next();
            long posKey = entry.getLongKey();
            int x = posX(posKey);
            int y = posY(posKey);
            int z = posZ(posKey);
            float strength = getCascadeStrength(world, x, y, z);
            if (strength <= 0.0F) {
                iterator.remove();
                removeWaterfallSound(posKey);
                continue;
            }
            if (Float.compare(strength, entry.getFloatValue()) != 0) {
                entry.setValue(strength);
            }
            spawnCascade(world, posKey, strength);
        }

        if (hasAngelicaShaderPackStateChanged()) {
            requestWaterfallSoundRefresh();
        }

        if (mc.isGamePaused()) {
            wasGamePaused = true;
            return;
        }
        if (wasGamePaused) {
            wasGamePaused = false;
            requestWaterfallSoundRefresh();
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

    public void onConfigReload() {
        needsNearbyRescan = true;
        requestWaterfallSoundRefresh();
    }

    public void onSoundSystemResumed() {
        requestWaterfallSoundRefresh();
    }

    public void onSoundSystemStopped() {
        waterfallSounds.clear();
        requestWaterfallSoundRefresh();
    }

    public void onRenderAudioContextChanged() {
        requestWaterfallSoundRefresh();
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
                        int x = (chunk.xPosition << 4) + localX;
                        int y = baseY + localY;
                        int z = (chunk.zPosition << 4) + localZ;
                        if (WetnessFluidHelper.getCascadeFluid(chunk.worldObj, x, y, z) == null) {
                            continue;
                        }

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
        LongIterator iterator = cascades.keySet()
            .iterator();
        while (iterator.hasNext()) {
            long posKey = iterator.nextLong();
            if ((posX(posKey) >> 4) == chunkX && (posZ(posKey) >> 4) == chunkZ) {
                iterator.remove();
                removeWaterfallSound(posKey);
            }
        }
    }

    private void updateCascade(World world, int x, int y, int z) {
        long posKey = positionKey(x, y, z);
        float strength = getCascadeStrength(world, x, y, z);
        if (strength > 0.0F) {
            cascades.put(posKey, strength);
        } else {
            cascades.remove(posKey);
            removeWaterfallSound(posKey);
        }
    }

    public boolean isWaterfallImpact(World world, int x, int y, int z) {
        return getCascadeStrength(world, x, y, z) > 0.0F;
    }

    private float getCascadeStrength(World world, int x, int y, int z) {
        if (y <= 1 || y >= 255) {
            return 0.0F;
        }

        Fluid fallingFluid = WetnessFluidHelper.getCascadeFluid(world, x, y, z);
        if (fallingFluid == null) {
            return 0.0F;
        }

        if (!isFallingFluid(world, x, y, z, fallingFluid)) {
            return 0.0F;
        }

        Fluid impactFluid = WetnessFluidHelper.getCascadeFluid(world, x, y - 1, z);
        if (impactFluid == null) {
            return 0.0F;
        }
        if (!WetnessFluidHelper.isSameCascadeFluid(world, x, y - 2, z, impactFluid)) {
            return 0.0F;
        }

        int strength = 0;
        if (isStillFluidSurface(world, x, y - 1, z, impactFluid) && hasOpenCardinalSide(world, x, y, z)) {
            strength = countAdjacentCascadeFluids(world, x, y, z, impactFluid);
        }

        if (strength <= 0) {
            strength = countExposedStillImpactSurfaces(world, x, y, z, impactFluid);
        }

        if (strength <= 0) {
            return 0.0F;
        }

        return (float) strength;
    }

    private boolean hasOpenCardinalSide(World world, int x, int y, int z) {
        return world.isAirBlock(x, y, z - 1) || world.isAirBlock(x + 1, y, z)
            || world.isAirBlock(x, y, z + 1)
            || world.isAirBlock(x - 1, y, z);
    }

    private int countAdjacentCascadeFluids(World world, int x, int y, int z, Fluid impactFluid) {
        int strength = 0;
        if (WetnessFluidHelper.isSameCascadeFluid(world, x, y - 1, z - 1, impactFluid)) strength++;
        if (WetnessFluidHelper.isSameCascadeFluid(world, x + 1, y - 1, z, impactFluid)) strength++;
        if (WetnessFluidHelper.isSameCascadeFluid(world, x, y - 1, z + 1, impactFluid)) strength++;
        if (WetnessFluidHelper.isSameCascadeFluid(world, x - 1, y - 1, z, impactFluid)) strength++;
        return strength;
    }

    private int countExposedStillImpactSurfaces(World world, int x, int y, int z, Fluid impactFluid) {
        int strength = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx == 0 && dz == 0) || !world.isAirBlock(x + dx, y, z + dz)) {
                    continue;
                }
                if (!isStillImpactSurface(world, x + dx, y - 1, z + dz, impactFluid)) {
                    continue;
                }

                strength++;
                if (strength >= 4) {
                    return strength;
                }
            }
        }
        return strength;
    }

    private boolean isFallingFluid(World world, int x, int y, int z, Fluid fluid) {
        Block block = world.getBlock(x, y, z);
        if (fluid == FluidRegistry.WATER && block instanceof BlockLiquid) {
            return block == net.minecraft.init.Blocks.flowing_water || world.getBlockMetadata(x, y, z) >= 8;
        }

        if (block instanceof BlockFluidClassic) {
            BlockFluidClassic fluidBlock = (BlockFluidClassic) block;
            return !fluidBlock.isSourceBlock(world, x, y, z) && (fluidBlock.isFlowingVertically(world, x, y, z)
                || WetnessFluidHelper.getCascadeFluid(world, x, y - 1, z) != null);
        }

        if (block instanceof BlockFluidBase || block instanceof IFluidBlock) {
            return WetnessFluidHelper.getCascadeFluid(world, x, y - 1, z) != null
                && WetnessFluidHelper.getWettableFluidHeight(world, x, y, z) < 0.999F;
        }

        return false;
    }

    private boolean isStillFluidSurface(World world, int x, int y, int z, Fluid fluid) {
        if (!WetnessFluidHelper.isSameCascadeFluid(world, x, y, z, fluid)) {
            return false;
        }

        Block block = world.getBlock(x, y, z);
        if (fluid == FluidRegistry.WATER && block instanceof BlockLiquid) {
            int meta = world.getBlockMetadata(x, y, z);
            return meta < 8 && WetnessFluidHelper.getFluidFlowDirection(world, x, y, z) <= -999.0D;
        }

        return WetnessFluidHelper.getWettableFluidHeight(world, x, y, z) >= 0.999F
            && WetnessFluidHelper.getFluidFlowDirection(world, x, y, z) <= -999.0D;
    }

    private boolean isStillImpactSurface(World world, int x, int y, int z, Fluid fluid) {
        if (!WetnessFluidHelper.isSameCascadeFluid(world, x, y, z, fluid)) {
            return false;
        }

        Block block = world.getBlock(x, y, z);
        if (fluid == FluidRegistry.WATER && block instanceof BlockLiquid) {
            return world.getBlockMetadata(x, y, z) == 0;
        }

        return isStillFluidSurface(world, x, y, z, fluid);
    }

    private void spawnCascade(World world, long posKey, float strength) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.effectRenderer == null || CASCADE_ICONS[0] == null) {
            return;
        }

        int xPos = posX(posKey);
        int yPos = posY(posKey);
        int zPos = posZ(posKey);

        // Particular spawns one foam particle per cascade per tick along a random edge, with Y
        // sampled inside the falling water column.
        double x = xPos;
        double z = zPos;
        if (world.rand.nextBoolean()) {
            x += world.rand.nextDouble();
            z += world.rand.nextInt(2);
        } else {
            x += world.rand.nextInt(2);
            z += world.rand.nextDouble();
        }

        float columnHeight = getFluidHeight(world, xPos, yPos, zPos);
        double y = yPos + world.rand.nextDouble() * columnHeight;

        CascadeFX cascade = new CascadeFX(world, x, y, z);
        float size = strength / 4.0F * columnHeight;
        cascade.multipleParticleScaleBy(1.0F - (1.0F - size) / 2.0F);
        mc.effectRenderer.addEffect(cascade);

        maybeSpawnForgeWaterfallSpray(world, xPos, yPos, zPos);
    }

    private void maybeSpawnForgeWaterfallSpray(World world, int xPos, int yPos, int zPos) {
        if (!Config.waterfallSprayEnabled || sprayIcon == null) {
            return;
        }

        Block block = world.getBlock(xPos, yPos, zPos);
        if (block instanceof BlockLiquid || !(block instanceof IFluidBlock) || world.rand.nextInt(3) != 0) {
            return;
        }

        double px = xPos;
        double pz = zPos;
        if (world.rand.nextBoolean()) {
            px += world.rand.nextDouble();
            pz += world.rand.nextInt(2);
        } else {
            px += world.rand.nextInt(2);
            pz += world.rand.nextDouble();
        }

        double py = yPos + 0.05D + world.rand.nextDouble() * 0.25D;
        Vec3 flow = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
        try {
            block.velocityToAddToEntity(world, xPos, yPos, zPos, null, flow);
        } catch (Throwable ignored) {}
        float[] rgb = WetnessFluidHelper.getWettableFluidColor(world, xPos, yPos, zPos);
        Minecraft.getMinecraft().effectRenderer
            .addEffect(new WaterfallSprayFX(world, px, py, pz, flow.xCoord * 0.075D, flow.zCoord * 0.075D, rgb));
    }

    private void updateWaterfallSounds(Minecraft mc, World world) {
        if (!Config.waterfallSoundEnabled || Config.waterfallSoundVolume <= 0.0F) {
            waterfallSoundsNeedRefresh = false;
            stopAllWaterfallSounds();
            return;
        }

        if (mc.renderViewEntity == null || mc.getSoundHandler() == null) {
            waterfallSoundsNeedRefresh = false;
            stopAllWaterfallSounds();
            return;
        }

        if (waterfallSoundsNeedRefresh) {
            stopAllWaterfallSounds();
            waterfallSoundsNeedRefresh = false;
        }

        final double maxDistance = getWaterfallSoundRange();
        final double maxDistanceSq = maxDistance * maxDistance;
        final double viewerX = mc.renderViewEntity.posX;
        final double viewerY = mc.renderViewEntity.posY;
        final double viewerZ = mc.renderViewEntity.posZ;

        soundCandidates.clear();
        ObjectIterator<Long2FloatMap.Entry> cascadeIterator = cascades.long2FloatEntrySet()
            .fastIterator();
        while (cascadeIterator.hasNext()) {
            Long2FloatMap.Entry entry = cascadeIterator.next();
            float strength = entry.getFloatValue();
            if (strength < Config.waterfallSoundCutoff) {
                continue;
            }

            long posKey = entry.getLongKey();
            double soundX = posX(posKey) + 0.5D;
            double soundY = posY(posKey) + 0.35D;
            double soundZ = posZ(posKey) + 0.5D;
            double dx = viewerX - soundX;
            double dy = viewerY - soundY;
            double dz = viewerZ - soundZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq <= maxDistanceSq) {
                soundCandidates.add(new SoundTarget(posKey, strength, soundX, soundY, soundZ, distanceSq));
            }
        }

        Collections.sort(soundCandidates, SoundTarget.BY_DISTANCE);
        soundKeysToKeep.clear();
        int limit = Math.min(soundCandidates.size(), MAX_ACTIVE_WATERFALL_SOUNDS);

        for (int i = 0; i < limit; i++) {
            SoundTarget target = soundCandidates.get(i);
            soundKeysToKeep.add(target.posKey);

            WaterfallLoopSound sound = waterfallSounds.get(target.posKey);
            boolean newSound = sound == null;
            if (newSound) {
                ResourceLocation soundId = getWaterfallSoundId(target.strength);
                sound = new WaterfallLoopSound(soundId, randomizePitch(world));
                waterfallSounds.put(target.posKey, sound);
            }

            sound.setState(target.x, target.y, target.z, getWaterfallSoundVolume(target.strength));
            if (newSound) {
                mc.getSoundHandler()
                    .playSound(sound);
            }
        }

        ObjectIterator<Long2ObjectMap.Entry<WaterfallLoopSound>> iterator = waterfallSounds.long2ObjectEntrySet()
            .fastIterator();
        while (iterator.hasNext()) {
            Long2ObjectMap.Entry<WaterfallLoopSound> entry = iterator.next();
            if (!soundKeysToKeep.contains(entry.getLongKey())) {
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

    private void removeWaterfallSound(long posKey) {
        WaterfallLoopSound sound = waterfallSounds.remove(posKey);
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

    private void requestWaterfallSoundRefresh() {
        waterfallSoundsNeedRefresh = true;
    }

    private boolean hasAngelicaShaderPackStateChanged() {
        shaderPackSoundCheckTicks++;
        if (shaderPackSoundCheckTicks < SHADER_PACK_SOUND_CHECK_INTERVAL) {
            return false;
        }
        shaderPackSoundCheckTicks = 0;

        boolean shaderPackInUse = AngelicaShaderHelper.isShaderPackInUse();
        if (lastAngelicaShaderPackInUse == null) {
            lastAngelicaShaderPackInUse = Boolean.valueOf(shaderPackInUse);
            return shaderPackInUse;
        }

        if (lastAngelicaShaderPackInUse.booleanValue() == shaderPackInUse) {
            return false;
        }

        lastAngelicaShaderPackInUse = Boolean.valueOf(shaderPackInUse);
        return true;
    }

    private float getFluidHeight(World world, int x, int y, int z) {
        float height = WetnessFluidHelper.getWettableFluidHeight(world, x, y, z);
        if (height < 0.0F) {
            return 1.0F;
        }

        return height;
    }

    private ResourceLocation getWaterfallSoundId(float strength) {
        int index = MathHelper.clamp_int(Math.round(strength * 4.0F), 0, WATERFALL_SOUND_VARIANTS.length - 1);
        return WATERFALL_SOUND_VARIANTS[index];
    }

    private float getWaterfallSoundVolume(float strength) {
        float normalizedStrength = MathHelper.clamp_float(strength, 0.0F, 2.0F);
        return Config.waterfallSoundVolume * (0.45F + normalizedStrength * 0.45F);
    }

    private float getWaterfallSoundRange() {
        if (Config.waterfallSoundRange > 0.0F) {
            return Config.waterfallSoundRange;
        }

        return SoundUtil.getVanillaMaxDistance(Config.waterfallSoundVolume * 1.35F);
    }

    private float randomizePitch(World world) {
        return 1.0F + 0.2F * (world.rand.nextFloat() - world.rand.nextFloat());
    }

    private static long positionKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) y & 0xFFFL;
    }

    private static int posX(long key) {
        return (int) (key >> 38);
    }

    private static int posY(long key) {
        return (int) (key & 0xFFFL);
    }

    private static int posZ(long key) {
        return (int) (key << 26 >> 38);
    }

    private static class SoundTarget {

        private static final Comparator<SoundTarget> BY_DISTANCE = new Comparator<SoundTarget>() {

            @Override
            public int compare(SoundTarget left, SoundTarget right) {
                return Double.compare(left.distanceSq, right.distanceSq);
            }
        };

        final long posKey;
        final float strength;
        final double x;
        final double y;
        final double z;
        final double distanceSq;

        SoundTarget(long posKey, float strength, double x, double y, double z, double distanceSq) {
            this.posKey = posKey;
            this.strength = strength;
            this.x = x;
            this.y = y;
            this.z = z;
            this.distanceSq = distanceSq;
        }
    }
}
