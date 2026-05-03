package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.compat.EtFuturumBoatCompat;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

@SideOnly(Side.CLIENT)
public final class WakeTrailManager {

    public static final WakeTrailManager INSTANCE = new WakeTrailManager();

    private static final int NODE_RES = 16;
    private static final int NODE_POWER = 4;
    private static final int NODE_MASK = NODE_RES - 1;
    private static final int NODE_GRID = NODE_RES + 2;
    private static final int NODE_CELLS = NODE_GRID * NODE_GRID;
    private static final int MAX_AGE = 30;
    private static final int MAX_NODES = 512;
    private static final int FLOOD_FILL_DISTANCE = 2;
    private static final int FLOOD_FILL_TICK_DELAY = 2;
    private static final float SURFACE_OFFSET = 0.014F;
    private static final float INITIAL_STRENGTH = 20.0F;
    private static final float WAVE_PROPAGATION_FACTOR = 0.95F;
    private static final float WAVE_DECAY_FACTOR = 0.5F;
    private static final double TELEPORT_DISTANCE_SQ = 64.0D;
    private static final double MIN_TRAIL_DISTANCE = 0.025D;
    private static final int FULL_BRIGHT = 15728880;
    private static final int MAX_WAKE_RENDER_BATCH_QUADS = 1800;
    private static final float PADDLE_STRENGTH = INITIAL_STRENGTH * 5.0F;
    private static final float[] COLOR_INTERVALS = new float[] { 0.05F, 0.15F, 0.2F, 0.35F, 0.52F, 0.6F, 0.7F, 0.9F };
    private static final int[][] WAKE_COLORS = new int[][] { { 0, 0, 0, 0 }, { 0x93, 0x99, 0xA6, 0x28 },
        { 0x9E, 0xA5, 0xB0, 0x64 }, { 0xC4, 0xCA, 0xD1, 0xB4 }, { 0, 0, 0, 0 }, { 0xC4, 0xCA, 0xD1, 0xB4 },
        { 0xFF, 0xFF, 0xFF, 0xFF }, { 0xC4, 0xCA, 0xD1, 0xB4 }, { 0x9E, 0xA5, 0xB0, 0x64 } };

    private final Long2ObjectOpenHashMap<WakeNode> nodes = new Long2ObjectOpenHashMap<WakeNode>();
    private final WeakHashMap<Entity, Tracker> trackers = new WeakHashMap<Entity, Tracker>();
    private final List<WakeNode> floodNodes = new ArrayList<WakeNode>();
    private final ArrayDeque<WakeNode> nodePool = new ArrayDeque<WakeNode>(MAX_NODES);
    private final EtFuturumBoatCompat.RowingTrailConsumer rowingTrailConsumer = new EtFuturumBoatCompat.RowingTrailConsumer() {

        @Override
        public void accept(double fromX, double fromZ, double toX, double toZ) {
            nodeTrail(rowingTrailWorld, fromX, fromZ, toX, toZ, rowingTrailY, PADDLE_STRENGTH, rowingTrailVelocity);
        }
    };
    private int stampId;
    private int inputStamp;
    private World rowingTrailWorld;
    private int rowingTrailY;
    private double rowingTrailVelocity;

    private WakeTrailManager() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || !Config.waterWakesEnabled) {
            clearNodes();
            trackers.clear();
            return;
        }

        tickNodes(mc.theWorld);
        inputStamp = nextStampId();
        spawnEntityWakes(mc);
        trimNodeCap();
    }

    public void renderInWorldPass(float partialTicks) {
        if (!Config.waterWakesEnabled || nodes.isEmpty()) {
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
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            WakeRenderBatch batch = new WakeRenderBatch(Tessellator.instance);
            try {
                ObjectIterator<Long2ObjectMap.Entry<WakeNode>> iterator = nodes.long2ObjectEntrySet()
                    .fastIterator();
                while (iterator.hasNext()) {
                    WakeNode node = iterator.next()
                        .getValue();
                    if (node.world == mc.theWorld) {
                        renderNode(batch, node, camX, camY, camZ, partialTicks);
                    }
                }
            } finally {
                batch.finish();
            }

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.entityRenderer.disableLightmap((double) partialTicks);
        } finally {
            GL11.glDepthMask(true);
            GL11.glPopAttrib();
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
        }
    }

    private void tickNodes(World world) {
        floodNodes.clear();
        ObjectIterator<Long2ObjectMap.Entry<WakeNode>> iterator = nodes.long2ObjectEntrySet()
            .fastIterator();
        while (iterator.hasNext()) {
            WakeNode node = iterator.next()
                .getValue();
            if (node.world != world || !isValidNodePos(world, node.x, node.y, node.z)) {
                iterator.remove();
                releaseNode(node);
                continue;
            }

            node.prevAge = node.age;
            node.age++;
            if (node.age >= MAX_AGE) {
                iterator.remove();
                releaseNode(node);
                continue;
            }

            node.tick(
                nodes.get(nodeKey(node.x, node.y, node.z - 1)),
                nodes.get(nodeKey(node.x, node.y, node.z + 1)),
                nodes.get(nodeKey(node.x + 1, node.y, node.z)),
                nodes.get(nodeKey(node.x - 1, node.y, node.z)));

            if (node.floodLevel > 0 && node.age > FLOOD_FILL_TICK_DELAY) {
                floodNodes.add(node);
            }
        }

        for (int i = 0; i < floodNodes.size(); i++) {
            floodFill(floodNodes.get(i));
        }
    }

    private void spawnEntityWakes(Minecraft mc) {
        for (Object entry : mc.theWorld.loadedEntityList) {
            if (!(entry instanceof Entity)) {
                continue;
            }

            Entity entity = (Entity) entry;
            if (!shouldTrack(entity)) {
                continue;
            }

            updateEntityWake(mc.theWorld, entity);
        }
    }

    private void updateEntityWake(World world, Entity entity) {
        double surfaceY = getSurfaceY(world, entity);
        Tracker tracker = trackers.get(entity);
        if (tracker == null) {
            tracker = new Tracker();
            trackers.put(entity, tracker);
        }

        if (surfaceY < 0.0D || !isEntityOnSurface(entity, surfaceY)) {
            tracker.onSurface = false;
            tracker.prevX = entity.posX;
            tracker.prevZ = entity.posZ;
            return;
        }

        double dx = entity.posX - tracker.prevX;
        double dz = entity.posZ - tracker.prevZ;
        double distanceSq = dx * dx + dz * dz;
        if (!tracker.onSurface || distanceSq > TELEPORT_DISTANCE_SQ) {
            tracker.onSurface = true;
            tracker.prevX = entity.posX;
            tracker.prevZ = entity.posZ;
            return;
        }

        double distance = Math.sqrt(distanceSq);
        if (EtFuturumBoatCompat.isBoat(entity)) {
            spawnRowingTrails(world, entity, surfaceY, distance);
        }

        if (distance >= MIN_TRAIL_DISTANCE) {
            spawnTrail(world, entity, tracker.prevX, tracker.prevZ, entity.posX, entity.posZ, surfaceY);
        }

        tracker.onSurface = true;
        tracker.prevX = entity.posX;
        tracker.prevZ = entity.posZ;
    }

    private static boolean shouldTrack(Entity entity) {
        if (entity.isDead || entity.worldObj == null || !entity.worldObj.isRemote) {
            return false;
        }

        return entity instanceof EntityBoat || entity instanceof EntityLivingBase
            || entity instanceof EntityItem
            || EtFuturumBoatCompat.isBoat(entity);
    }

    private void spawnRowingTrails(World world, Entity entity, double surfaceY, double velocity) {
        rowingTrailWorld = world;
        rowingTrailY = MathHelper.floor_double(surfaceY - 1.0E-4D);
        rowingTrailVelocity = velocity;
        EtFuturumBoatCompat.forEachRowingTrail(entity, velocity, rowingTrailConsumer);
        rowingTrailWorld = null;
    }

    private void spawnTrail(World world, Entity entity, double fromX, double fromZ, double toX, double toZ,
        double surfaceY) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        float density = Math.max(0.0F, Config.waterWakeDensity);
        if (distance < MIN_TRAIL_DISTANCE || density <= 0.0F) {
            return;
        }

        int y = MathHelper.floor_double(surfaceY - 1.0E-4D);
        float strength = INITIAL_STRENGTH * density;
        thickNodeTrail(world, fromX, fromZ, toX, toZ, y, strength, distance, entity.width);
    }

    private void nodeTrail(World world, double fromX, double fromZ, double toX, double toZ, int y, float waveStrength,
        double velocity) {
        int x1 = MathHelper.floor_double(fromX * NODE_RES);
        int z1 = MathHelper.floor_double(fromZ * NODE_RES);
        int x2 = MathHelper.floor_double(toX * NODE_RES);
        int z2 = MathHelper.floor_double(toZ * NODE_RES);

        stampLine(world, x1, z1, x2, z2, y, waveStrength, velocity, inputStamp);
    }

    private void thickNodeTrail(World world, double fromX, double fromZ, double toX, double toZ, int y,
        float waveStrength, double velocity, float width) {
        int x1 = MathHelper.floor_double(fromX * NODE_RES);
        int z1 = MathHelper.floor_double(fromZ * NODE_RES);
        int x2 = MathHelper.floor_double(toX * NODE_RES);
        int z2 = MathHelper.floor_double(toZ * NODE_RES);
        int w = Math.max(1, (int) (0.8F * width * NODE_RES / 2.0F));

        float len = (float) Math.sqrt((double) ((z1 - z2) * (z1 - z2) + (x2 - x1) * (x2 - x1)));
        if (len <= 0.0F) {
            return;
        }

        float nx = (z1 - z2) / len;
        float nz = (x2 - x1) / len;
        for (int i = -w; i < w; i++) {
            stampLine(
                world,
                MathHelper.floor_float(x1 + nx * i),
                MathHelper.floor_float(z1 + nz * i),
                MathHelper.floor_float(x2 + nx * i),
                MathHelper.floor_float(z2 + nz * i),
                y,
                waveStrength,
                velocity,
                inputStamp);
        }
    }

    private void floodFill(WakeNode node) {
        insertFloodNode(node, node.x, node.y, node.z - 1);
        insertFloodNode(node, node.x + 1, node.y, node.z);
        insertFloodNode(node, node.x, node.y, node.z + 1);
        insertFloodNode(node, node.x - 1, node.y, node.z);
        node.floodLevel = 0;
    }

    private void insertFloodNode(WakeNode source, int x, int y, int z) {
        int nextLevel = source.floodLevel - 1;
        if (nextLevel < 0) {
            return;
        }

        WakeNode node = getOrCreateNode(source.world, x, y, z, nextLevel);
        if (node != null) {
            node.age = 0;
            node.floodLevel = Math.max(node.floodLevel, nextLevel);
        }
    }

    private void trimNodeCap() {
        while (nodes.size() > MAX_NODES) {
            removeOldestNode();
        }
    }

    private void removeOldestNode() {
        long oldestKey = 0L;
        int oldestAge = -1;
        boolean found = false;
        WakeNode oldestNode = null;
        ObjectIterator<Long2ObjectMap.Entry<WakeNode>> iterator = nodes.long2ObjectEntrySet()
            .fastIterator();
        while (iterator.hasNext()) {
            Long2ObjectMap.Entry<WakeNode> entry = iterator.next();
            WakeNode node = entry.getValue();
            if (node.age > oldestAge) {
                oldestAge = node.age;
                oldestKey = entry.getLongKey();
                oldestNode = node;
                found = true;
            }
        }

        if (found) {
            nodes.remove(oldestKey);
            releaseNode(oldestNode);
        }
    }

    private static double getSurfaceY(World world, Entity entity) {
        AxisAlignedBB box = entity.boundingBox;
        int minX = MathHelper.floor_double(box.minX - 0.001D);
        int maxX = MathHelper.floor_double(box.maxX + 0.001D);
        int minY = MathHelper.floor_double(box.minY - 0.75D);
        int maxY = MathHelper.floor_double(box.maxY + 0.35D);
        int minZ = MathHelper.floor_double(box.minZ - 0.001D);
        int maxZ = MathHelper.floor_double(box.maxZ + 0.001D);
        double best = -1.0D;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!isWakeSurface(world, x, y, z)) {
                        continue;
                    }

                    double surface = getWakeSurfaceY(world, x, y, z);
                    if (surface > best) {
                        best = surface;
                    }
                }
            }
        }

        return best;
    }

    private static boolean isEntityOnSurface(Entity entity, double surfaceY) {
        AxisAlignedBB box = entity.boundingBox;
        return box.maxY > surfaceY + 0.01D && box.minY < surfaceY + 0.45D;
    }

    private static boolean isWakeSurface(World world, int x, int y, int z) {
        if (getWakeSurfaceY(world, x, y, z) < 0.0D) {
            return false;
        }

        return WetnessFluidHelper.getInteractableFluid(world, x, y + 1, z) == null;
    }

    private static boolean isValidNodePos(World world, int x, int y, int z) {
        if (!isWakeSurface(world, x, y, z)) {
            return false;
        }

        Fluid fluid = WetnessFluidHelper.getInteractableFluid(world, x, y, z);
        if (fluid == FluidRegistry.WATER && world.getBlock(x, y, z) instanceof BlockLiquid) {
            return world.getBlockMetadata(x, y, z) == 0;
        }

        return WetnessFluidHelper.getWettableFluidHeight(world, x, y, z) >= 0.999F;
    }

    private static double getWakeSurfaceY(World world, int x, int y, int z) {
        return WetnessFluidHelper.getWettableFluidSurfaceY(world, x, y, z);
    }

    private static float[] sampleWakeFluidColor(World world, int x, int y, int z) {
        return WetnessFluidHelper.getWettableFluidColor(world, x, y, z);
    }

    private static void renderNode(WakeRenderBatch batch, WakeNode node, double camX, double camY, double camZ,
        float partialTicks) {
        float ageDelta = node.prevAge + (node.age - node.prevAge) * partialTicks;
        float progress = clamp(ageDelta / (float) MAX_AGE, 0.0F, 1.0F);
        float ageFade = 1.0F - progress * progress;
        if (ageFade <= 0.003F) {
            return;
        }

        double pixelSize = 1.0D / NODE_RES;
        double y = node.surfaceY + SURFACE_OFFSET - camY;

        for (int pixelZ = 0; pixelZ < NODE_RES; pixelZ++) {
            double z0 = node.z + pixelZ * pixelSize - camZ;
            double z1 = z0 + pixelSize;
            for (int pixelX = 0; pixelX < NODE_RES; pixelX++) {
                int color = sampleColor(node.getWave(pixelX, pixelZ), node.tintR, node.tintG, node.tintB, ageFade);
                int alpha = color >>> 24;
                if (alpha <= 2) {
                    continue;
                }

                double x0 = node.x + pixelX * pixelSize - camX;
                double x1 = x0 + pixelSize;
                batch.addQuad(x0, x1, y, z0, z1, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha);
            }
        }
    }

    private static int sampleColor(float wave, float tintR, float tintG, float tintB, float ageFade) {
        double clampedRange = 1.0D / (1.0D + Math.exp(-0.1D * wave));
        int index = COLOR_INTERVALS.length;
        for (int i = 0; i < COLOR_INTERVALS.length; i++) {
            if (clampedRange < COLOR_INTERVALS[i]) {
                index = i;
                break;
            }
        }

        int[] color = WAKE_COLORS[index];
        if (color[3] == 0) {
            return 0;
        }

        double srcA = Math.pow(color[3] / 255.0D, 5.0D);
        int r = (int) (color[0] * srcA + tintR * 255.0F * (1.0D - srcA));
        int g = (int) (color[1] * srcA + tintG * 255.0F * (1.0D - srcA));
        int b = (int) (color[2] * srcA + tintB * 255.0F * (1.0D - srcA));
        float opacity = color[3] == 0xFF ? ageFade : ageFade * Config.waterWakeAlpha;
        int a = (int) (color[3] * opacity);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private void stampLine(World world, int x1, int z1, int x2, int z2, int y, float waveStrength, double velocity,
        int stamp) {
        int dz = z2 - z1;
        int dx = x2 - x1;
        if (dx == 0) {
            if (z2 < z1) {
                int temp = z1;
                z1 = z2;
                z2 = temp;
            }
            for (int z = z1; z < z2 + 1; z++) {
                stampPixel(world, x1, z, y, waveStrength, velocity, stamp);
            }
        } else {
            float k = (float) dz / dx;
            int adjust = k >= 0 ? 1 : -1;
            int offset = 0;
            if (k <= 1 && k >= -1) {
                int delta = Math.abs(dz) * 2;
                int threshold = Math.abs(dx);
                int thresholdInc = Math.abs(dx) * 2;
                int z = z1;
                if (x2 < x1) {
                    int temp = x1;
                    x1 = x2;
                    x2 = temp;
                    z = z2;
                }
                for (int x = x1; x < x2 + 1; x++) {
                    stampPixel(world, x, z, y, waveStrength, velocity, stamp);
                    offset += delta;
                    if (offset >= threshold) {
                        z += adjust;
                        threshold += thresholdInc;
                    }
                }
            } else {
                int delta = Math.abs(dx) * 2;
                int threshold = Math.abs(dz);
                int thresholdInc = Math.abs(dz) * 2;
                int x = x1;
                if (z2 < z1) {
                    int temp = z1;
                    z1 = z2;
                    z2 = temp;
                }
                for (int z = z1; z < z2 + 1; z++) {
                    stampPixel(world, x, z, y, waveStrength, velocity, stamp);
                    offset += delta;
                    if (offset >= threshold) {
                        x += adjust;
                        threshold += thresholdInc;
                    }
                }
            }
        }
    }

    private void stampPixel(World world, int pixelX, int pixelZ, int y, float waveStrength, double velocity,
        int stamp) {
        WakeNode node = getOrCreateNode(world, pixelX >> NODE_POWER, y, pixelZ >> NODE_POWER, FLOOD_FILL_DISTANCE);
        if (node == null) {
            return;
        }

        node.beginInputStamp(stamp);
        node.setInitialValue(pixelX & NODE_MASK, pixelZ & NODE_MASK, waveStrength, velocity);
    }

    private WakeNode getOrCreateNode(World world, int x, int y, int z, int floodLevel) {
        long key = nodeKey(x, y, z);
        WakeNode node = nodes.get(key);
        if (node != null) {
            node.floodLevel = Math.max(node.floodLevel, floodLevel);
            return node;
        }

        if (!isValidNodePos(world, x, y, z)) {
            return null;
        }

        if (nodes.size() >= MAX_NODES) {
            removeOldestNode();
        }

        node = acquireNode(world, x, y, z, floodLevel);
        nodes.put(key, node);
        return node;
    }

    private WakeNode acquireNode(World world, int x, int y, int z, int floodLevel) {
        WakeNode node = nodePool.pollFirst();
        if (node == null) {
            node = new WakeNode();
        }
        node.reset(world, x, y, z, floodLevel);
        return node;
    }

    private void releaseNode(WakeNode node) {
        if (node == null || nodePool.size() >= MAX_NODES) {
            return;
        }

        node.release();
        nodePool.addLast(node);
    }

    private void clearNodes() {
        ObjectIterator<Long2ObjectMap.Entry<WakeNode>> iterator = nodes.long2ObjectEntrySet()
            .fastIterator();
        while (iterator.hasNext()) {
            releaseNode(
                iterator.next()
                    .getValue());
        }
        nodes.clear();
        floodNodes.clear();
    }

    private int nextStampId() {
        stampId++;
        if (stampId == 0) {
            ObjectIterator<Long2ObjectMap.Entry<WakeNode>> iterator = nodes.long2ObjectEntrySet()
                .fastIterator();
            while (iterator.hasNext()) {
                iterator.next()
                    .getValue().inputStamp = 0;
            }
            stampId = 1;
        }
        return stampId;
    }

    private static long nodeKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) y & 0xFFFL;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static class Tracker {

        double prevX;
        double prevZ;
        boolean onSurface;
    }

    private static class WakeRenderBatch {

        private final Tessellator tessellator;
        private int quads;

        WakeRenderBatch(Tessellator tessellator) {
            this.tessellator = tessellator;
            begin();
        }

        void addQuad(double x0, double x1, double y, double z0, double z1, int red, int green, int blue, int alpha) {
            if (quads >= MAX_WAKE_RENDER_BATCH_QUADS) {
                flush();
            }

            tessellator.setColorRGBA(red, green, blue, alpha);
            tessellator.addVertex(x0, y, z0);
            tessellator.addVertex(x0, y, z1);
            tessellator.addVertex(x1, y, z1);
            tessellator.addVertex(x1, y, z0);
            quads++;
        }

        void finish() {
            tessellator.draw();
        }

        private void flush() {
            tessellator.draw();
            quads = 0;
            begin();
        }

        private void begin() {
            tessellator.startDrawingQuads();
            tessellator.setBrightness(FULL_BRIGHT);
        }
    }

    private static class WakeNode {

        final float[][] u = new float[][] { new float[NODE_CELLS], new float[NODE_CELLS], new float[NODE_CELLS] };
        final float[] initialValues = new float[NODE_CELLS];
        World world;
        int x;
        int y;
        int z;
        long key;
        double surfaceY;
        float tintR;
        float tintG;
        float tintB;
        int floodLevel;
        int age;
        int prevAge;
        int inputStamp;

        void reset(World world, int x, int y, int z, int floodLevel) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.key = nodeKey(x, y, z);
            this.surfaceY = getWakeSurfaceY(world, x, y, z);
            float[] tint = sampleWakeFluidColor(world, x, y, z);
            this.tintR = tint[0];
            this.tintG = tint[1];
            this.tintB = tint[2];
            this.floodLevel = floodLevel;
            this.age = 0;
            this.prevAge = 0;
            this.inputStamp = 0;
            clearAll();
        }

        void release() {
            world = null;
            inputStamp = 0;
        }

        void beginInputStamp(int stamp) {
            if (inputStamp == stamp) {
                return;
            }

            inputStamp = stamp;
            age = 0;
            floodLevel = FLOOD_FILL_DISTANCE;
            clearInitialValues();
        }

        void setInitialValue(int x, int z, float waveStrength, double velocity) {
            float value = (float) (waveStrength * velocity) * (NODE_RES / 16.0F);
            for (int dz = -1; dz < 2; dz++) {
                for (int dx = -1; dx < 2; dx++) {
                    int px = x + dx + 1;
                    int pz = z + dz + 1;
                    int index = index(px, pz);
                    if (Math.abs(value) > Math.abs(initialValues[index])) {
                        initialValues[index] = value;
                    }
                }
            }
        }

        void clearAll() {
            for (int i = 0; i < u.length; i++) {
                Arrays.fill(u[i], 0.0F);
            }
            clearInitialValues();
        }

        void clearInitialValues() {
            Arrays.fill(initialValues, 0.0F);
        }

        void tick(WakeNode north, WakeNode south, WakeNode east, WakeNode west) {
            float alpha = (float) Math.pow(WAVE_PROPAGATION_FACTOR * 16.0F / 20.0F, 2.0D);
            float beta = (float) (Math.log(10.0D * WAVE_DECAY_FACTOR + 10.0D) / Math.log(20.0D));

            for (int i = 2; i >= 1; i--) {
                float[] state = u[i];
                if (north != null) {
                    System.arraycopy(north.u[i], index(0, NODE_RES), state, index(0, 0), NODE_GRID);
                }
                if (south != null) {
                    System.arraycopy(south.u[i], index(0, 1), state, index(0, NODE_RES + 1), NODE_GRID);
                }
                for (int z = 0; z < NODE_RES + 2; z++) {
                    if (east == null && west == null) {
                        break;
                    }
                    if (east != null) {
                        state[index(NODE_RES + 1, z)] = east.u[i][index(1, z)];
                    }
                    if (west != null) {
                        state[index(0, z)] = west.u[i][index(NODE_RES, z)];
                    }
                }
            }

            float[] current = u[0];
            float[] previous = u[1];
            float[] older = u[2];
            for (int z = 1; z < NODE_RES + 1; z++) {
                for (int x = 1; x < NODE_RES + 1; x++) {
                    int index = index(x, z);
                    current[index] += initialValues[index];
                    initialValues[index] = 0.0F;
                    older[index] = previous[index];
                    previous[index] = current[index];
                }
            }

            for (int z = 1; z < NODE_RES + 1; z++) {
                for (int x = 1; x < NODE_RES + 1; x++) {
                    int index = index(x, z);
                    current[index] = (float) (alpha
                        * (0.5F * previous[index(x, z - 1)] + 0.25F * previous[index(x + 1, z - 1)]
                            + 0.5F * previous[index(x + 1, z)]
                            + 0.25F * previous[index(x + 1, z + 1)]
                            + 0.5F * previous[index(x, z + 1)]
                            + 0.25F * previous[index(x - 1, z + 1)]
                            + 0.5F * previous[index(x - 1, z)]
                            + 0.25F * previous[index(x - 1, z - 1)]
                            - 3.0F * previous[index])
                        + 2.0F * previous[index]
                        - older[index]);
                    current[index] *= beta;
                }
            }
        }

        float getWave(int x, int z) {
            int index = index(x + 1, z + 1);
            return (u[0][index] + u[1][index] + u[2][index]) / 3.0F;
        }

        private static int index(int x, int z) {
            return z * NODE_GRID + x;
        }
    }
}
