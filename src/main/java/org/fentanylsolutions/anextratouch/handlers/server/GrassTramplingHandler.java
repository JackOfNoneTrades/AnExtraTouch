package org.fentanylsolutions.anextratouch.handlers.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

public class GrassTramplingHandler {

    public static final GrassTramplingHandler INSTANCE = new GrassTramplingHandler();

    private static class TrampleData {

        int count;
        int threshold;
        long lastTrampleTick;
    }

    // dim -> (packed xyz -> data)
    private final Map<Integer, Map<Long, TrampleData>> trackedBlocks = new HashMap<>();
    // allocated once per entity, mutated in place: {x, y, z}
    private final WeakHashMap<Entity, int[]> lastEntityBlockPos = new WeakHashMap<>();
    private final Set<Block> trampleableBlocks = new HashSet<>();
    private final Set<Class<?>> tramplingEntityClasses = new HashSet<>();
    private final Object2BooleanOpenHashMap<Class<?>> tramplingEntityCache = new Object2BooleanOpenHashMap<>();
    private final Random random = new Random();
    private int tickCounter = 0;

    public void initHook() {
        populateFromConfig();
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void clear() {
        trackedBlocks.clear();
        lastEntityBlockPos.clear();
        tramplingEntityCache.clear();
    }

    private void populateFromConfig() {
        trampleableBlocks.clear();
        for (String name : Config.tramplingBlocks) {
            Block block = Block.getBlockFromName(name);
            if (block != null) {
                trampleableBlocks.add(block);
            } else {
                AnExtraTouch.LOG.warn("Trampling: Unknown block '{}', skipping.", name);
            }
        }

        tramplingEntityClasses.clear();
        tramplingEntityCache.clear();
        Set<String> names = new HashSet<>();
        for (String name : Config.tramplingEntityClassList) {
            names.add(name);
        }
        for (Map.Entry<String, Class<? extends Entity>> entry : EntityList.stringToClassMapping.entrySet()) {
            if (names.contains(entry.getKey())) {
                tramplingEntityClasses.add(entry.getValue());
            }
        }
        if (names.contains("Player")) {
            tramplingEntityClasses.add(EntityPlayer.class);
        }
    }

    private boolean isEntityAllowed(EntityLivingBase entity) {
        Class<?> entityClass = entity.getClass();
        if (tramplingEntityCache.containsKey(entityClass)) {
            return tramplingEntityCache.getBoolean(entityClass);
        }

        boolean inList = false;
        for (Class<?> clazz : tramplingEntityClasses) {
            if (clazz.isAssignableFrom(entityClass)) {
                inList = true;
                break;
            }
        }

        boolean allowed = Config.tramplingEntityClassListIsBlacklist ? !inList : inList;
        tramplingEntityCache.put(entityClass, allowed);
        return allowed;
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.entityLiving;
        if (entity.worldObj.isRemote) {
            return;
        }
        if (!Config.tramplingEnabled) {
            return;
        }

        int x = MathHelper.floor_double(entity.posX);
        int y = MathHelper.floor_double(entity.posY);
        int z = MathHelper.floor_double(entity.posZ);

        int[] lastPos = lastEntityBlockPos.get(entity);
        if (lastPos != null && lastPos[0] == x && lastPos[1] == y && lastPos[2] == z) {
            return;
        }
        if (lastPos == null) {
            lastEntityBlockPos.put(entity, new int[] { x, y, z });
        } else {
            lastPos[0] = x;
            lastPos[1] = y;
            lastPos[2] = z;
        }

        Block block = entity.worldObj.getBlock(x, y, z);
        if (!trampleableBlocks.contains(block)) {
            return;
        }
        if (!isEntityAllowed(entity)) {
            return;
        }

        int dimId = entity.worldObj.provider.dimensionId;
        long key = packPos(x, y, z);
        Map<Long, TrampleData> dimMap = trackedBlocks.get(dimId);
        if (dimMap == null) {
            dimMap = new HashMap<>();
            trackedBlocks.put(dimId, dimMap);
        }

        long worldTime = entity.worldObj.getTotalWorldTime();
        long forgetTicks = (long) Config.tramplingForgetTime * 60 * 20;

        TrampleData data = dimMap.get(key);
        if (data != null && worldTime - data.lastTrampleTick > forgetTicks) {
            if (AnExtraTouch.isDebug()) {
                AnExtraTouch.debug(
                    "Trampling: Forgot block at " + x
                        + ", "
                        + y
                        + ", "
                        + z
                        + " in dim "
                        + dimId
                        + " (expired, had "
                        + data.count
                        + "/"
                        + data.threshold
                        + " passes)");
            }
            dimMap.remove(key);
            data = null;
        }

        if (data == null) {
            data = new TrampleData();
            int min = Config.tramplingMinPasses;
            int max = Config.tramplingMaxPasses;
            data.threshold = min + random.nextInt(Math.max(1, max - min + 1));
            dimMap.put(key, data);
        }

        data.count++;
        data.lastTrampleTick = worldTime;

        if (data.count >= data.threshold) {
            if (AnExtraTouch.isDebug()) {
                AnExtraTouch.debug(
                    "Trampling: Broke block at " + x
                        + ", "
                        + y
                        + ", "
                        + z
                        + " in dim "
                        + dimId
                        + " after "
                        + data.count
                        + " passes (threshold: "
                        + data.threshold
                        + ")");
            }
            entity.worldObj.func_147480_a(x, y, z, true);
            dimMap.remove(key);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;
        if (tickCounter % 1200 != 0) {
            return;
        }

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }

        long forgetTicks = (long) Config.tramplingForgetTime * 60 * 20;

        for (WorldServer world : server.worldServers) {
            if (world == null) {
                continue;
            }
            int dimId = world.provider.dimensionId;
            Map<Long, TrampleData> dimMap = trackedBlocks.get(dimId);
            if (dimMap == null) {
                continue;
            }

            long worldTime = world.getTotalWorldTime();
            int swept = 0;
            Iterator<Map.Entry<Long, TrampleData>> it = dimMap.entrySet()
                .iterator();
            while (it.hasNext()) {
                Map.Entry<Long, TrampleData> entry = it.next();
                if (worldTime - entry.getValue().lastTrampleTick > forgetTicks) {
                    it.remove();
                    swept++;
                }
            }
            if (swept > 0 && AnExtraTouch.isDebug()) {
                AnExtraTouch.debug("Trampling: Swept " + swept + " expired entries in dim " + dimId);
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote) {
            clear();
            AnExtraTouch.debug("Trampling: Cleared tracked blocks on world load.");
        }
    }

    private static long packPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF)) << 38 | ((long) (y & 0xFFF)) << 26 | ((long) (z & 0x3FFFFFF));
    }
}
