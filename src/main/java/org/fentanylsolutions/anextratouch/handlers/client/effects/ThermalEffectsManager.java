package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityLavaFX;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidBlock;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-only steam and lava surface decoration.
 *
 * Steam and jet behavior adapted from Dynamic Surroundings, MIT, Copyright (c) 2018 OreCruncher, Abastro.
 * https://github.com/OreCruncher/DynamicSurroundingsFabric/tree/0d352c7e57bbd39786defdba355156a5bdb850f4
 * See assets/anextratouch/licenses/dynamic-surroundings.txt.
 */
@SideOnly(Side.CLIENT)
public final class ThermalEffectsManager {

    public static final ThermalEffectsManager INSTANCE = new ThermalEffectsManager();

    private static final int SCAN_BUDGET = 384;
    private static final int MAX_ACTIVE_JETS = 8;
    private static final int MAX_PARTICLES_PER_TICK = 24;
    private static final int JET_INTERVAL = 3;

    private final LinkedHashMap<Long, Source> sources = new LinkedHashMap<Long, Source>();
    private final LinkedHashMap<Long, Jet> jets = new LinkedHashMap<Long, Jet>();
    private final List<BlockSpec> steamSources = new ArrayList<BlockSpec>();
    private final List<BlockSpec> heatSources = new ArrayList<BlockSpec>();
    private final List<BlockSpec> lavaSources = new ArrayList<BlockSpec>();
    private final Set<String> warnedSpecs = new HashSet<String>();
    private World currentWorld;
    private long scanCursor;
    private int tick;

    private ThermalEffectsManager() {
        onConfigReload();
    }

    public void onConfigReload() {
        sources.clear();
        jets.clear();
        scanCursor = 0L;
        warnedSpecs.clear();
        compile(Config.steamSourceBlocks, steamSources, "steam source");
        compile(Config.steamHeatBlocks, heatSources, "steam heat");
        compile(Config.lavaJetSourceBlocks, lavaSources, "lava jet source");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null || currentWorld != world) {
            reset(world);
        }
        if (world == null || mc.renderViewEntity == null || mc.isGamePaused()) return;

        if (!Config.steamEnabled && !Config.lavaJetsEnabled) {
            sources.clear();
            jets.clear();
            return;
        }
        if (mc.gameSettings.particleSetting >= 2) return;

        tick++;
        Iterator<Map.Entry<Long, Source>> cached = sources.entrySet()
            .iterator();
        while (cached.hasNext()) {
            Map.Entry<Long, Source> entry = cached.next();
            Source source = entry.getValue();
            if (!inRange(mc.renderViewEntity, source) || !world.blockExists(source.x, source.y, source.z)) {
                jets.remove(entry.getKey());
                cached.remove();
            }
        }
        scanNearby(world, mc.renderViewEntity);
        int particleBudget = mc.gameSettings.particleSetting == 1 ? MAX_PARTICLES_PER_TICK / 2 : MAX_PARTICLES_PER_TICK;
        particleBudget = updateJets(mc, world, mc.renderViewEntity, particleBudget);
        emitSteam(mc, world, mc.renderViewEntity, particleBudget);
    }

    private void reset(World world) {
        currentWorld = world;
        sources.clear();
        jets.clear();
        scanCursor = 0L;
        tick = 0;
    }

    private void scanNearby(World world, Entity viewer) {
        int range = MathHelper.clamp_int(Config.thermalEffectRange, 1, 32);
        int side = range * 2 + 1;
        long volume = (long) side * side * side;
        int baseX = MathHelper.floor_double(viewer.posX) - range;
        int baseY = MathHelper.floor_double(viewer.posY) - range;
        int baseZ = MathHelper.floor_double(viewer.posZ) - range;

        for (int checked = 0; checked < SCAN_BUDGET; checked++) {
            long index = scanCursor++ % volume;
            int x = baseX + (int) (index % side);
            index /= side;
            int z = baseZ + (int) (index % side);
            int y = baseY + (int) (index / side);
            if (distanceSq(viewer, x, y, z) > range * range) continue;
            if (y < 0 || y >= world.getActualHeight() || !world.blockExists(x, y, z)) continue;
            inspect(world, viewer, x, y, z);
        }
    }

    private void inspect(World world, Entity viewer, int x, int y, int z) {
        long key = pack(x, y, z);
        Source source = classify(world, x, y, z);
        if (source == null) {
            sources.remove(key);
            jets.remove(key);
            return;
        }

        source.x = x;
        source.y = y;
        source.z = z;
        Source old = sources.get(key);
        if (old != null) {
            sources.put(key, source);
            return;
        }

        int cap = MathHelper.clamp_int(Config.thermalEffectMaxSources, 0, 4096);
        if (cap == 0) return;
        if (sources.size() < cap) {
            sources.put(key, source);
            return;
        }

        // Give steam a route into a cache initially filled by a broad lava lake.
        if (source.kind == Source.STEAM) {
            Long lavaKey = null;
            for (Map.Entry<Long, Source> entry : sources.entrySet()) {
                if (entry.getValue().kind == Source.LAVA) {
                    lavaKey = entry.getKey();
                    break;
                }
            }
            if (lavaKey != null) {
                sources.remove(lavaKey);
                jets.remove(lavaKey);
                sources.put(key, source);
                return;
            }
        }

        long farthestKey = 0L;
        double farthestDistance = -1.0D;
        for (Map.Entry<Long, Source> entry : sources.entrySet()) {
            Source candidate = entry.getValue();
            double distance = distanceSq(viewer, candidate.x, candidate.y, candidate.z);
            if (distance > farthestDistance) {
                farthestDistance = distance;
                farthestKey = entry.getKey();
            }
        }
        if (distanceSq(viewer, x, y, z) < farthestDistance) {
            sources.remove(farthestKey);
            jets.remove(farthestKey);
            sources.put(key, source);
        }
    }

    private Source classify(World world, int x, int y, int z) {
        if (y < 0 || y >= world.getActualHeight() || !world.blockExists(x, y, z)) return null;
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        if (!isExposed(world, x, y, z)) return null;

        boolean solid = !block.getMaterial()
            .isLiquid() && !(block instanceof IFluidBlock);
        if (Config.steamEnabled && matches(steamSources, block, meta) && hasHeat(world, x, y, z)) {
            return new Source(Source.STEAM, solid, 1, spawnHeight(world, block, meta, x, y, z, solid));
        }
        if (Config.lavaJetsEnabled && matches(lavaSources, block, meta)) {
            int strength = solid ? (block.isSideSolid(world, x, y, z, ForgeDirection.UP) ? 2 : 1)
                : liquidDepth(world, x, y, z);
            return new Source(Source.LAVA, solid, strength, spawnHeight(world, block, meta, x, y, z, solid));
        }
        return null;
    }

    private int emitSteam(Minecraft mc, World world, Entity viewer, int budget) {
        if (!Config.steamEnabled || steamSources.isEmpty() || heatSources.isEmpty()) return budget;
        float density = Math.max(0.0F, Config.steamParticleDensity);
        List<Long> steamKeys = new ArrayList<Long>();
        for (Map.Entry<Long, Source> entry : sources.entrySet()) {
            if (entry.getValue().kind == Source.STEAM) steamKeys.add(entry.getKey());
        }
        int count = steamKeys.size();
        int start = count == 0 ? 0 : tick % count;
        for (int offset = 0; offset < count && budget > 0; offset++) {
            Long key = steamKeys.get((start + offset) % count);
            Source source = sources.get(key);
            if (source == null) continue;
            Source valid = classify(world, source.x, source.y, source.z);
            if (valid == null || valid.kind != Source.STEAM) {
                sources.remove(key);
                continue;
            }
            source.height = valid.height;
            if (!inRange(viewer, source)) continue;
            float expected = density / 3.0F;
            int emissions = (int) expected;
            if (world.rand.nextFloat() < expected - emissions) emissions++;
            while (emissions-- > 0 && budget > 0) {
                // Keep the particle's collision box inside a cauldron's opening at spawn.
                double spread = source.solid ? 0.2D : 0.4D;
                double x = source.x + 0.5D + (world.rand.nextDouble() - world.rand.nextDouble()) * spread;
                double z = source.z + 0.5D + (world.rand.nextDouble() - world.rand.nextDouble()) * spread;
                mc.effectRenderer.addEffect(new SteamCloudFX(world, x, source.y + source.height, z, source.solid));
                budget--;
            }
        }
        return budget;
    }

    private int updateJets(Minecraft mc, World world, Entity viewer, int budget) {
        Iterator<Map.Entry<Long, Jet>> active = jets.entrySet()
            .iterator();
        while (active.hasNext()) {
            Map.Entry<Long, Jet> entry = active.next();
            Source source = sources.get(entry.getKey());
            Jet jet = entry.getValue();
            if (source == null || source.kind != Source.LAVA || --jet.ticksLeft <= 0) {
                active.remove();
                continue;
            }
            Source valid = classify(world, source.x, source.y, source.z);
            if (valid == null || valid.kind != Source.LAVA) {
                active.remove();
                sources.remove(entry.getKey());
                continue;
            }
            source.strength = valid.strength;
            source.height = valid.height;
            if (budget > 0 && tick % JET_INTERVAL == 0 && inRange(viewer, source)) {
                spawnJetParticle(mc, world, source, jet.lava);
                budget--;
            }
        }

        if (!Config.lavaJetsEnabled || lavaSources.isEmpty() || jets.size() >= MAX_ACTIVE_JETS) return budget;
        for (Map.Entry<Long, Source> entry : sources.entrySet()) {
            if (jets.size() >= MAX_ACTIVE_JETS) break;
            Source source = entry.getValue();
            if (source.kind != Source.LAVA || jets.containsKey(entry.getKey()) || !inRange(viewer, source)) continue;
            float perTickChance = Math.max(0.0F, Math.min(1.0F, Config.lavaJetChance));
            if (world.rand.nextFloat() >= perTickChance) continue;
            Source valid = classify(world, source.x, source.y, source.z);
            if (valid == null || valid.kind != Source.LAVA) continue;
            source.strength = valid.strength;
            source.height = valid.height;
            int duration = (world.rand.nextInt(Math.max(1, source.strength)) + 2) * 20;
            jets.put(entry.getKey(), new Jet(duration, !source.solid && world.rand.nextInt(3) == 0));
            if (source.strength > 1 && Config.lavaJetSoundVolume > 0.0F) {
                world.playSound(
                    source.x + 0.5D,
                    source.y + source.height,
                    source.z + 0.5D,
                    "fire.fire",
                    Config.lavaJetSoundVolume,
                    0.8F + world.rand.nextFloat() * 0.4F,
                    false);
            }
        }
        return budget;
    }

    private static void spawnJetParticle(Minecraft mc, World world, Source source, boolean lavaJet) {
        double x = source.x + 0.5D + (world.rand.nextDouble() - 0.5D) * 0.35D;
        double y = source.y + source.height + 0.02D;
        double z = source.z + 0.5D + (world.rand.nextDouble() - 0.5D) * 0.35D;
        double rise = Math.max(0.1D, source.strength / 10.0D);
        EntityFX particle;
        if (lavaJet) {
            EntityLavaFX lava = new EntityLavaFX(world, x, y, z);
            lava.motionY = rise;
            particle = lava;
        } else {
            particle = new ThermalFlameFX(world, x, y, z, rise, Math.max(0.5F, source.strength / 3.0F));
        }
        mc.effectRenderer.addEffect(particle);
    }

    private boolean hasHeat(World world, int x, int y, int z) {
        for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++) for (int oz = -1; oz <= 1; oz++) {
            if (ox == 0 && oy == 0 && oz == 0) continue;
            int px = x + ox;
            int py = y + oy;
            int pz = z + oz;
            if (py >= 0 && py < world.getActualHeight() && world.blockExists(px, py, pz)) {
                Block block = world.getBlock(px, py, pz);
                if (matches(heatSources, block, world.getBlockMetadata(px, py, pz))) return true;
            }
        }
        return false;
    }

    private int liquidDepth(World world, int x, int y, int z) {
        int depth = 1;
        Block first = world.getBlock(x, y, z);
        for (; depth < 10; depth++) {
            int py = y - depth;
            if (py < 0 || !world.blockExists(x, py, z)) break;
            Block below = world.getBlock(x, py, z);
            int meta = world.getBlockMetadata(x, py, z);
            boolean liquid = below.getMaterial()
                .isLiquid() || below instanceof IFluidBlock;
            if (!liquid || (below != first && !matches(lavaSources, below, meta))) break;
        }
        return depth;
    }

    private static boolean isExposed(World world, int x, int y, int z) {
        return y + 1 < world.getActualHeight() && world.blockExists(x, y + 1, z) && world.isAirBlock(x, y + 1, z);
    }

    private static double spawnHeight(World world, Block block, int meta, int x, int y, int z, boolean solid) {
        if (solid) {
            if (block instanceof BlockCauldron) return BlockCauldron.getRenderLiquidLevel(meta) + 0.02D;
            block.setBlockBoundsBasedOnState(world, x, y, z);
            return Math.max(0.0D, Math.min(1.5D, block.getBlockBoundsMaxY())) + 0.02D;
        }
        if (block instanceof BlockLiquid) return 1.0D - BlockLiquid.getLiquidHeightPercent(meta) + 0.1D;
        return 1.0D;
    }

    private static boolean inRange(Entity viewer, Source source) {
        double range = MathHelper.clamp_int(Config.thermalEffectRange, 1, 32);
        return distanceSq(viewer, source.x, source.y, source.z) <= range * range;
    }

    private static double distanceSq(Entity viewer, int x, int y, int z) {
        double dx = viewer.posX - (x + 0.5D);
        double dy = viewer.posY - (y + 0.5D);
        double dz = viewer.posZ - (z + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private void compile(String[] rawSpecs, List<BlockSpec> destination, String label) {
        destination.clear();
        if (rawSpecs == null) return;
        for (String raw : rawSpecs) {
            if (raw == null) continue;
            String value = raw.trim();
            if (value.isEmpty()) continue;
            int meta = -1;
            String name = value;
            int at = value.lastIndexOf('@');
            if (at >= 0) {
                name = value.substring(0, at)
                    .trim();
                try {
                    meta = Integer.parseInt(
                        value.substring(at + 1)
                            .trim());
                } catch (NumberFormatException ignored) {
                    warn(raw, label);
                    continue;
                }
                if (meta < 0 || meta > 15) {
                    warn(raw, label);
                    continue;
                }
            }
            if (name.indexOf(':') <= 0 || name.endsWith(":")) {
                warn(raw, label);
                continue;
            }
            if (!Block.blockRegistry.containsKey(name)) continue;
            Object registered = Block.blockRegistry.getObject(name);
            if (registered instanceof Block) destination.add(new BlockSpec((Block) registered, meta));
        }
    }

    private void warn(String raw, String label) {
        String key = label + '\0' + raw;
        if (warnedSpecs.add(key)) AnExtraTouch.LOG.warn("Ignoring malformed {} block entry '{}'", label, raw);
    }

    private static boolean matches(List<BlockSpec> specs, Block block, int meta) {
        for (BlockSpec spec : specs) if (spec.block == block && (spec.meta < 0 || spec.meta == meta)) return true;
        return false;
    }

    private static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL);
    }

    private static final class BlockSpec {

        final Block block;
        final int meta;

        BlockSpec(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }
    }

    private static final class Source {

        static final byte STEAM = 1;
        static final byte LAVA = 2;
        final byte kind;
        final boolean solid;
        int strength;
        double height;
        int x;
        int y;
        int z;

        Source(byte kind, boolean solid, int strength, double height) {
            this.kind = kind;
            this.solid = solid;
            this.strength = strength;
            this.height = height;
        }
    }

    private static final class Jet {

        int ticksLeft;
        final boolean lava;

        Jet(int ticksLeft, boolean lava) {
            this.ticksLeft = ticksLeft;
            this.lava = lava;
        }
    }
}
