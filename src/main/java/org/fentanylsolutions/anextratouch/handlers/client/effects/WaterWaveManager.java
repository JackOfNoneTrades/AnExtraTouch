package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.world.ChunkEvent;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WaterWaveManager {

    public static final WaterWaveManager INSTANCE = new WaterWaveManager();

    private static final int FRAME_COUNT = 5;
    private static final int SIZE_SMALL = 0;
    private static final int SIZE_MEDIUM = 1;
    private static final int SIZE_LARGE = 2;
    private static final int MAX_WAVES = 96;
    private static final int MAX_SPAWNS_PER_TICK = 8;
    private static final int SPAWN_ATTEMPTS_PER_WAVE = 18;
    private static final int CACHE_TTL = 20 * 30;
    private static final float SURFACE_OFFSET = 0.018F;
    private static final int FULL_BRIGHT = 15728880;
    private static final String BREAKING_SOUND = AnExtraTouch.MODID + ":waves.waves_breaking";

    private static final ResourceLocation[][] WAVE_TEXTURES = new ResourceLocation[][] { makeFrames("small"),
        makeFrames("medium"), makeFrames("large") };

    private final List<Wave> waves = new ArrayList<Wave>();
    private final Map<Long, CachedShore> shoreCache = new HashMap<Long, CachedShore>();
    private final BiomeWhitelist biomeWhitelist = new BiomeWhitelist();
    private final java.util.Random random = new java.util.Random();

    private World currentWorld;
    private float spawnBudget;

    private WaterWaveManager() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null || !Config.wavesEnabled) {
            waves.clear();
            shoreCache.clear();
            biomeWhitelist.clearCache();
            currentWorld = null;
            spawnBudget = 0.0F;
            return;
        }

        if (currentWorld != world) {
            waves.clear();
            shoreCache.clear();
            biomeWhitelist.clearCache();
            currentWorld = world;
            spawnBudget = 0.0F;
        }

        tickWaves(mc, world);
        spawnWaves(mc, world);
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
            shoreCache.clear();
        }
    }

    public void onChunkFilled(Chunk chunk) {
        if (chunk != null && chunk.worldObj != null && chunk.worldObj.isRemote) {
            shoreCache.clear();
        }
    }

    public void onBlockChanged(Chunk chunk, int localX, int y, int localZ) {
        if (chunk != null && chunk.worldObj != null && chunk.worldObj.isRemote) {
            shoreCache.clear();
        }
    }

    public void onConfigReload() {
        shoreCache.clear();
        biomeWhitelist.clear();
    }

    private void tickWaves(Minecraft mc, World world) {
        Iterator<Wave> iterator = waves.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            if (wave.world != world) {
                iterator.remove();
                continue;
            }

            wave.tick();
            if (wave.dead) {
                iterator.remove();
            }
        }
    }

    private void spawnWaves(Minecraft mc, World world) {
        if (Config.waveSpawnFrequency <= 0 || Config.waveSpawnAmount <= 0.0F || mc.thePlayer == null) {
            return;
        }
        if (world.getTotalWorldTime() % (long) Config.waveSpawnFrequency != 0L) {
            return;
        }

        spawnBudget += Config.waveSpawnAmount;
        int spawnCount = MathHelper.floor_float(spawnBudget);
        spawnBudget -= spawnCount;
        if (random.nextFloat() < spawnBudget) {
            spawnCount++;
            spawnBudget = 0.0F;
        }

        spawnCount = Math.min(spawnCount, MAX_SPAWNS_PER_TICK);
        int attempts = spawnCount * SPAWN_ATTEMPTS_PER_WAVE;
        int spawned = 0;
        for (int i = 0; i < attempts && spawned < spawnCount; i++) {
            WaterSample sample = randomWaterSample(mc, world);
            if (sample == null || !isWithinSpawnFov(mc.thePlayer, sample.x + 0.5D, sample.z + 0.5D)) {
                continue;
            }

            ShoreInfo shore = findNearestShore(world, sample.x, sample.y, sample.z);
            if (shore == null || shore.distance < Config.waveSpawnDistanceFromShoreMin
                || shore.distance > Config.waveSpawnDistanceFromShoreMax) {
                continue;
            }

            addWave(world, sample, shore);
            spawned++;
        }

        trimWaveCap();
        trimCache();
    }

    private WaterSample randomWaterSample(Minecraft mc, World world) {
        double radius = getSpawnRadius(mc);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = Config.waveSpawnDistance <= 0.0F ? radius : Math.sqrt(random.nextDouble()) * radius;
        int x = MathHelper.floor_double(mc.thePlayer.posX + Math.cos(angle) * distance);
        int z = MathHelper.floor_double(mc.thePlayer.posZ + Math.sin(angle) * distance);

        if (!world.blockExists(x, 64, z)) {
            return null;
        }

        if (!isWaveBiomeAllowed(world, x, z)) {
            return null;
        }

        int top = world.getHeightValue(x, z);
        if (top <= 0) {
            return null;
        }

        int minY = Math.max(1, top - 24);
        for (int y = Math.min(255, top); y >= minY; y--) {
            if (!isSurfaceWaveWater(world, x, y, z)) {
                continue;
            }

            double surfaceY = WetnessFluidHelper.getWettableFluidSurfaceY(world, x, y, z);
            if (surfaceY < 0.0D) {
                continue;
            }

            return new WaterSample(x, y, z, surfaceY);
        }

        return null;
    }

    private double getSpawnRadius(Minecraft mc) {
        double configured = Math.max(8.0D, Config.waveSpawnDistance);
        double render = Math.max(2, mc.gameSettings.renderDistanceChunks) * 16.0D;
        return Math.min(configured, render);
    }

    private boolean isWaveBiomeAllowed(World world, int x, int z) {
        return biomeWhitelist.isAllowed(world.getBiomeGenForCoords(x, z), Config.waveBiomeWhitelist);
    }

    private boolean isWithinSpawnFov(EntityPlayer player, double x, double z) {
        if (Config.waveSpawningFOVLimit >= 360.0F) {
            return true;
        }

        double dx = x - player.posX;
        double dz = z - player.posZ;
        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float delta = MathHelper.wrapAngleTo180_float(targetYaw - player.rotationYaw);
        return Math.abs(delta) <= Config.waveSpawningFOVLimit * 0.5F;
    }

    private ShoreInfo findNearestShore(World world, int x, int y, int z) {
        long key = blockKey(x, y, z);
        long now = world.getTotalWorldTime();
        CachedShore cached = shoreCache.get(key);
        if (cached != null && now - cached.tick <= CACHE_TTL) {
            return cached.info;
        }

        ShoreInfo info = scanNearestShore(world, x, y, z);
        shoreCache.put(key, new CachedShore(info, now));
        return info;
    }

    private ShoreInfo scanNearestShore(World world, int waterX, int waterY, int waterZ) {
        int search = Math.max(1, Config.waveSearchDistance);
        for (int radius = 1; radius <= search; radius++) {
            ShoreInfo best = null;
            double bestDistanceSq = Double.MAX_VALUE;

            for (int dx = -radius; dx <= radius; dx++) {
                best = selectCloserShore(
                    world,
                    waterX,
                    waterY,
                    waterZ,
                    waterX + dx,
                    waterZ - radius,
                    best,
                    bestDistanceSq);
                if (best != null) bestDistanceSq = best.distanceSq;
                best = selectCloserShore(
                    world,
                    waterX,
                    waterY,
                    waterZ,
                    waterX + dx,
                    waterZ + radius,
                    best,
                    bestDistanceSq);
                if (best != null) bestDistanceSq = best.distanceSq;
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                best = selectCloserShore(
                    world,
                    waterX,
                    waterY,
                    waterZ,
                    waterX - radius,
                    waterZ + dz,
                    best,
                    bestDistanceSq);
                if (best != null) bestDistanceSq = best.distanceSq;
                best = selectCloserShore(
                    world,
                    waterX,
                    waterY,
                    waterZ,
                    waterX + radius,
                    waterZ + dz,
                    best,
                    bestDistanceSq);
                if (best != null) bestDistanceSq = best.distanceSq;
            }

            if (best != null) {
                return best;
            }
        }

        return null;
    }

    private ShoreInfo selectCloserShore(World world, int waterX, int waterY, int waterZ, int shoreX, int shoreZ,
        ShoreInfo current, double currentDistanceSq) {
        int shoreY = findShoreY(world, shoreX, waterY, shoreZ);
        if (shoreY < 0) {
            return current;
        }

        double dx = ((double) shoreX + 0.5D) - ((double) waterX + 0.5D);
        double dz = ((double) shoreZ + 0.5D) - ((double) waterZ + 0.5D);
        double distanceSq = dx * dx + dz * dz;
        if (distanceSq >= currentDistanceSq) {
            return current;
        }

        double distance = Math.sqrt(distanceSq);
        if (distance <= 0.0D) {
            return current;
        }

        return new ShoreInfo(shoreX, shoreY, shoreZ, dx / distance, dz / distance, distance, distanceSq);
    }

    private int findShoreY(World world, int x, int waterY, int z) {
        if (!world.blockExists(x, waterY, z)) {
            return -1;
        }

        if (isShoreBlock(world, x, waterY, z) && hasAdjacentWaveWater(world, x, waterY, z, waterY)) {
            return waterY;
        }
        if (isShoreBlock(world, x, waterY + 1, z) && hasAdjacentWaveWater(world, x, waterY + 1, z, waterY)) {
            return waterY + 1;
        }

        return -1;
    }

    private boolean hasAdjacentWaveWater(World world, int x, int y, int z, int waterY) {
        return isWaveWater(world, x - 1, waterY, z) || isWaveWater(world, x + 1, waterY, z)
            || isWaveWater(world, x, waterY, z - 1)
            || isWaveWater(world, x, waterY, z + 1);
    }

    private static boolean isShoreBlock(World world, int x, int y, int z) {
        if (y < 0 || y >= 256 || !world.blockExists(x, y, z)) {
            return false;
        }

        Block block = world.getBlock(x, y, z);
        Material material = block.getMaterial();
        return material != Material.air && !material.isLiquid() && material.blocksMovement();
    }

    private static boolean isSurfaceWaveWater(World world, int x, int y, int z) {
        if (!isWaveWater(world, x, y, z)) {
            return false;
        }

        return !isWaveWater(world, x, y + 1, z) && !world.getBlock(x, y + 1, z)
            .getMaterial()
            .blocksMovement();
    }

    private static boolean isWaveWater(World world, int x, int y, int z) {
        if (y < 0 || y >= 256 || !world.blockExists(x, y, z)) {
            return false;
        }

        Block block = world.getBlock(x, y, z);
        return block instanceof BlockLiquid && block.getMaterial() == Material.water
            && WetnessFluidHelper.isFluidInteractionAllowed(world, x, y, z);
    }

    private void addWave(World world, WaterSample sample, ShoreInfo shore) {
        int surroundingWater = countSurroundingWater(world, sample.x, sample.y, sample.z, 2);
        int size = chooseSize(shore.distance, surroundingWater);

        Wave wave = new Wave();
        wave.world = world;
        wave.x = wave.prevX = sample.x + 0.5D;
        wave.y = wave.prevY = sample.surfaceY + SURFACE_OFFSET;
        wave.z = wave.prevZ = sample.z + 0.5D;
        wave.dirX = shore.dirX;
        wave.dirZ = shore.dirZ;
        wave.shoreX = shore.x + 0.5D;
        wave.shoreZ = shore.z + 0.5D;
        wave.size = size;
        wave.width = getWaveWidth(size) * Config.waveScale;
        wave.depth = getWaveDepth(size) * Config.waveScale;
        wave.speed = getWaveSpeed(size);
        wave.maxAge = Math.max(80, (int) (shore.distance / wave.speed) + 60);
        wave.r = 1.0F;
        wave.g = 1.0F;
        wave.b = 1.0F;
        waves.add(wave);
    }

    private int countSurroundingWater(World world, int x, int y, int z, int radius) {
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (isWaveWater(world, x + dx, y, z + dz)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int chooseSize(double distance, int surroundingWater) {
        if (distance >= 20.0D && surroundingWater >= 18) {
            return SIZE_LARGE;
        }
        if (distance >= 10.0D && surroundingWater >= 12) {
            return SIZE_MEDIUM;
        }
        return SIZE_SMALL;
    }

    private static float getWaveWidth(int size) {
        switch (size) {
            case SIZE_LARGE:
                return 7.5F;
            case SIZE_MEDIUM:
                return 5.25F;
            default:
                return 3.25F;
        }
    }

    private static float getWaveDepth(int size) {
        switch (size) {
            case SIZE_LARGE:
                return 4.25F;
            case SIZE_MEDIUM:
                return 3.25F;
            default:
                return 2.25F;
        }
    }

    private double getWaveSpeed(int size) {
        return 0.085D + size * 0.018D + random.nextDouble() * 0.025D;
    }

    private void trimWaveCap() {
        while (waves.size() > MAX_WAVES) {
            waves.remove(0);
        }
    }

    private void trimCache() {
        if (shoreCache.size() > 4096) {
            shoreCache.clear();
        }
    }

    public void renderInWorldPass(float partialTicks) {
        if (!Config.wavesEnabled || waves.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.renderViewEntity;
        if (viewer == null || mc.theWorld == null) {
            return;
        }

        double camX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double camY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double camZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_CURRENT_BIT);
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
            GL11.glDepthMask(false);

            mc.entityRenderer.enableLightmap((double) partialTicks);
            for (Wave wave : waves) {
                if (wave.world == mc.theWorld) {
                    renderWave(mc, wave, camX, camY, camZ, partialTicks);
                }
            }
            mc.entityRenderer.disableLightmap((double) partialTicks);
        } finally {
            GL11.glDepthMask(true);
            GL11.glPopAttrib();
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
        }
    }

    private void renderWave(Minecraft mc, Wave wave, double camX, double camY, double camZ, float partialTicks) {
        int frame = wave.getFrame(partialTicks);
        mc.getTextureManager()
            .bindTexture(WAVE_TEXTURES[wave.size][frame]);

        double x = wave.prevX + (wave.x - wave.prevX) * partialTicks - camX;
        double y = wave.prevY + (wave.y - wave.prevY) * partialTicks - camY;
        double z = wave.prevZ + (wave.z - wave.prevZ) * partialTicks - camZ;

        double sideX = -wave.dirZ;
        double sideZ = wave.dirX;
        double halfWidth = wave.width * 0.5D;
        double halfDepth = wave.depth * 0.5D;

        double x0 = x - sideX * halfWidth - wave.dirX * halfDepth;
        double z0 = z - sideZ * halfWidth - wave.dirZ * halfDepth;
        double x1 = x - sideX * halfWidth + wave.dirX * halfDepth;
        double z1 = z - sideZ * halfWidth + wave.dirZ * halfDepth;
        double x2 = x + sideX * halfWidth + wave.dirX * halfDepth;
        double z2 = z + sideZ * halfWidth + wave.dirZ * halfDepth;
        double x3 = x + sideX * halfWidth - wave.dirX * halfDepth;
        double z3 = z + sideZ * halfWidth - wave.dirZ * halfDepth;

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setBrightness(wave.getBrightness());
        tessellator.setColorRGBA_F(wave.r, wave.g, wave.b, wave.getAlpha(partialTicks));
        tessellator.addVertexWithUV(x0, y, z0, 0.0D, 1.0D);
        tessellator.addVertexWithUV(x1, y, z1, 0.0D, 0.0D);
        tessellator.addVertexWithUV(x2, y, z2, 1.0D, 0.0D);
        tessellator.addVertexWithUV(x3, y, z3, 1.0D, 1.0D);
        tessellator.draw();
    }

    private static ResourceLocation[] makeFrames(String size) {
        ResourceLocation[] frames = new ResourceLocation[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = new ResourceLocation(AnExtraTouch.MODID, "textures/particles/waves/" + size + "_" + i + ".png");
        }
        return frames;
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) (y & 0xFFFL);
    }

    private static class WaterSample {

        final int x;
        final int y;
        final int z;
        final double surfaceY;

        WaterSample(int x, int y, int z, double surfaceY) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.surfaceY = surfaceY;
        }
    }

    private static class ShoreInfo {

        final int x;
        final int y;
        final int z;
        final double dirX;
        final double dirZ;
        final double distance;
        final double distanceSq;

        ShoreInfo(int x, int y, int z, double dirX, double dirZ, double distance, double distanceSq) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dirX = dirX;
            this.dirZ = dirZ;
            this.distance = distance;
            this.distanceSq = distanceSq;
        }
    }

    private static class CachedShore {

        final ShoreInfo info;
        final long tick;

        CachedShore(ShoreInfo info, long tick) {
            this.info = info;
            this.tick = tick;
        }
    }

    private static final class BiomeWhitelist {

        private String[] cachedEntries = new String[0];
        private String[] cachedEntriesRef;
        private final Set<Integer> biomeIds = new HashSet<Integer>();
        private final Set<String> biomeNames = new HashSet<String>();
        private final Set<BiomeDictionary.Type> biomeTypes = new HashSet<BiomeDictionary.Type>();
        private final Map<Integer, Boolean> biomeCache = new HashMap<Integer, Boolean>();
        private boolean allowAll;

        boolean isAllowed(BiomeGenBase biome, String[] entries) {
            refresh(entries);
            if (allowAll) {
                return true;
            }
            if (biome == null || biomeIds.isEmpty() && biomeNames.isEmpty() && biomeTypes.isEmpty()) {
                return false;
            }

            Boolean cached = biomeCache.get(Integer.valueOf(biome.biomeID));
            if (cached != null) {
                return cached.booleanValue();
            }

            boolean allowed = biomeIds.contains(Integer.valueOf(biome.biomeID)) || isBiomeNameListed(biome.biomeName)
                || isBiomeTypeListed(biome);
            biomeCache.put(Integer.valueOf(biome.biomeID), Boolean.valueOf(allowed));
            return allowed;
        }

        void clear() {
            cachedEntries = new String[0];
            cachedEntriesRef = null;
            biomeIds.clear();
            biomeNames.clear();
            biomeTypes.clear();
            biomeCache.clear();
            allowAll = false;
        }

        void clearCache() {
            biomeCache.clear();
        }

        private void refresh(String[] entries) {
            if (entries == null) {
                entries = new String[0];
            }

            if (entries == cachedEntriesRef) {
                return;
            }

            if (Arrays.equals(cachedEntries, entries)) {
                cachedEntriesRef = entries;
                return;
            }

            cachedEntriesRef = entries;
            cachedEntries = entries.clone();
            biomeIds.clear();
            biomeNames.clear();
            biomeTypes.clear();
            biomeCache.clear();
            allowAll = false;

            for (String rawEntry : entries) {
                if (rawEntry == null) {
                    continue;
                }

                String entry = rawEntry.trim();
                if (entry.length() == 0) {
                    continue;
                }

                String lowerEntry = entry.toLowerCase(Locale.ENGLISH);
                if ("*".equals(entry) || "all".equals(lowerEntry) || "any".equals(lowerEntry)) {
                    allowAll = true;
                    continue;
                }
                if (lowerEntry.startsWith("type:")) {
                    addBiomeType(entry.substring("type:".length()));
                    continue;
                }
                if (lowerEntry.startsWith("dictionary:")) {
                    addBiomeType(entry.substring("dictionary:".length()));
                    continue;
                }

                try {
                    biomeIds.add(Integer.valueOf(Integer.parseInt(entry)));
                } catch (NumberFormatException ignored) {
                    addBiomeName(entry);
                }
            }
        }

        private void addBiomeType(String name) {
            try {
                biomeTypes.add(
                    BiomeDictionary.Type.valueOf(
                        name.trim()
                            .toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException ignored) {}
        }

        private void addBiomeName(String name) {
            String normalized = normalizeBiomeName(name);
            if (normalized.length() > 0) {
                biomeNames.add(normalized);
            }

            int namespaceSeparator = name.indexOf(':');
            if (namespaceSeparator >= 0 && namespaceSeparator < name.length() - 1) {
                String bareName = normalizeBiomeName(name.substring(namespaceSeparator + 1));
                if (bareName.length() > 0) {
                    biomeNames.add(bareName);
                }
            }
        }

        private boolean isBiomeNameListed(String name) {
            if (name == null) {
                return false;
            }

            if (biomeNames.contains(normalizeBiomeName(name))) {
                return true;
            }

            int namespaceSeparator = name.indexOf(':');
            return namespaceSeparator >= 0 && namespaceSeparator < name.length() - 1
                && biomeNames.contains(normalizeBiomeName(name.substring(namespaceSeparator + 1)));
        }

        private boolean isBiomeTypeListed(BiomeGenBase biome) {
            for (BiomeDictionary.Type type : biomeTypes) {
                if (BiomeDictionary.isBiomeOfType(biome, type)) {
                    return true;
                }
            }
            return false;
        }

        private static String normalizeBiomeName(String name) {
            if (name == null) {
                return "";
            }

            String lower = name.trim()
                .toLowerCase(Locale.ENGLISH);
            StringBuilder normalized = new StringBuilder(lower.length());
            for (int i = 0; i < lower.length(); i++) {
                char c = lower.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    normalized.append(c);
                }
            }
            return normalized.toString();
        }
    }

    private final class Wave {

        World world;
        double x;
        double y;
        double z;
        double prevX;
        double prevY;
        double prevZ;
        double dirX;
        double dirZ;
        double shoreX;
        double shoreZ;
        double speed;
        float width;
        float depth;
        float r;
        float g;
        float b;
        int size;
        int age;
        int prevAge;
        int shoreAge;
        int prevShoreAge;
        int maxAge;
        boolean reachedShore;
        boolean playedSound;
        boolean dead;

        void tick() {
            prevAge = age;
            prevShoreAge = shoreAge;
            prevX = x;
            prevY = y;
            prevZ = z;
            age++;

            if (reachedShore) {
                shoreAge++;
                speed *= 0.84D;
                x += dirX * speed;
                z += dirZ * speed;
                y += 0.001D;
                if (shoreAge > 28) {
                    dead = true;
                }
                return;
            }

            x += dirX * speed;
            z += dirZ * speed;

            int blockX = MathHelper.floor_double(x);
            int blockY = MathHelper.floor_double(y - SURFACE_OFFSET);
            int blockZ = MathHelper.floor_double(z);
            if (age >= maxAge) {
                dead = true;
                return;
            }

            double distanceToShore = distanceToShore();
            if (distanceToShore < 4.0D) {
                playBreakingSound();
            }

            if (distanceToShore <= 1.2D || !isWaveWater(world, blockX, blockY, blockZ)) {
                reachedShore = true;
                shoreAge = 0;
            }
        }

        double distanceToShore() {
            double dx = shoreX - x;
            double dz = shoreZ - z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        void playBreakingSound() {
            if (playedSound || Config.waveVolume <= 0.0F) {
                return;
            }

            int chance = Math.max(0, Config.waveBreakingSoundChance);
            int bound = chance == Integer.MAX_VALUE ? Integer.MAX_VALUE : chance + 1;
            if (world.rand.nextInt(bound) != 0) {
                return;
            }

            float skyOvercast = Math.min(
                1.0F,
                Math.max(0.0F, (world.getRainStrength(0.0F) + world.getWeightedThunderStrength(0.0F)) / 3.0F));
            float weatherBoost = 1.0F + skyOvercast * 0.5F;
            float volume = Config.waveVolume * weatherBoost
                * (2.5F + size * 0.5F)
                * (0.75F + world.rand.nextFloat() * 0.5F);
            float pitch = 0.7F + world.rand.nextFloat() * 0.8F;
            world.playSound(x, y, z, BREAKING_SOUND, volume, pitch, false);
            playedSound = true;
        }

        int getFrame(float partialTicks) {
            if (reachedShore) {
                float ageDelta = prevShoreAge + (shoreAge - prevShoreAge) * partialTicks;
                int frame = (int) (ageDelta / 5.0F);
                return MathHelper.clamp_int(frame, 0, FRAME_COUNT - 1);
            }

            float ageDelta = prevAge + (age - prevAge) * partialTicks;
            int frame = (int) (ageDelta / 7.0F) % FRAME_COUNT;
            return MathHelper.clamp_int(frame, 0, FRAME_COUNT - 1);
        }

        float getAlpha(float partialTicks) {
            if (reachedShore) {
                float ageDelta = prevShoreAge + (shoreAge - prevShoreAge) * partialTicks;
                if (ageDelta <= 10.0F) {
                    return 1.0F;
                }
                return Math.max(0.0F, 1.0F - (ageDelta - 10.0F) / 18.0F);
            }

            float ageDelta = prevAge + (age - prevAge) * partialTicks;
            return Math.min(1.0F, ageDelta / 10.0F);
        }

        int getBrightness() {
            if (!world.isDaytime() && world.getMoonPhase() == 0) {
                return FULL_BRIGHT;
            }

            return world.getLightBrightnessForSkyBlocks(
                MathHelper.floor_double(x),
                MathHelper.floor_double(y),
                MathHelper.floor_double(z),
                0);
        }
    }
}
