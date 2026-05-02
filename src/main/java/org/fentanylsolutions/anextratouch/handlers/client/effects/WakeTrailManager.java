package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WakeTrailManager {

    public static final WakeTrailManager INSTANCE = new WakeTrailManager();

    private static final int NODE_RES = 16;
    private static final int NODE_POWER = 4;
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
    private static final float[] COLOR_INTERVALS = new float[] { 0.05F, 0.15F, 0.2F, 0.35F, 0.52F, 0.6F, 0.7F, 0.9F };
    private static final int[][] WAKE_COLORS = new int[][] { { 0, 0, 0, 0 }, { 0x93, 0x99, 0xA6, 0x28 },
        { 0x9E, 0xA5, 0xB0, 0x64 }, { 0xC4, 0xCA, 0xD1, 0xB4 }, { 0, 0, 0, 0 }, { 0xC4, 0xCA, 0xD1, 0xB4 },
        { 0xFF, 0xFF, 0xFF, 0xFF }, { 0xC4, 0xCA, 0xD1, 0xB4 }, { 0x9E, 0xA5, 0xB0, 0x64 } };

    private final Map<NodeKey, WakeNode> nodes = new HashMap<NodeKey, WakeNode>();
    private final WeakHashMap<Entity, Tracker> trackers = new WeakHashMap<Entity, Tracker>();

    private WakeTrailManager() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || !Config.waterWakesEnabled) {
            nodes.clear();
            trackers.clear();
            return;
        }

        tickNodes(mc.theWorld);
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

            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            for (WakeNode node : nodes.values()) {
                if (node.world == mc.theWorld) {
                    renderNode(tessellator, node, camX, camY, camZ, partialTicks);
                }
            }
            tessellator.draw();

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
        List<WakeNode> floodNodes = new ArrayList<WakeNode>();
        Iterator<Map.Entry<NodeKey, WakeNode>> iterator = nodes.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            WakeNode node = iterator.next()
                .getValue();
            if (node.world != world || !isValidNodePos(world, node.x, node.y, node.z)) {
                iterator.remove();
                continue;
            }

            node.prevAge = node.age;
            node.age++;
            if (node.age >= MAX_AGE) {
                iterator.remove();
                continue;
            }

            node.t = node.age / (float) MAX_AGE;
            node.tick(
                nodes.get(new NodeKey(node.x, node.y, node.z - 1)),
                nodes.get(new NodeKey(node.x, node.y, node.z + 1)),
                nodes.get(new NodeKey(node.x + 1, node.y, node.z)),
                nodes.get(new NodeKey(node.x - 1, node.y, node.z)));

            if (node.floodLevel > 0 && node.age > FLOOD_FILL_TICK_DELAY) {
                floodNodes.add(node);
            }
        }

        for (WakeNode node : floodNodes) {
            floodFill(node);
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

        if (distanceSq >= MIN_TRAIL_DISTANCE * MIN_TRAIL_DISTANCE) {
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

        return entity instanceof EntityBoat || entity instanceof EntityLivingBase || entity instanceof EntityItem;
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
        ArrayList<Long> pixelsAffected = new ArrayList<Long>();
        for (int i = -w; i < w; i++) {
            bresenhamLine(
                MathHelper.floor_float(x1 + nx * i),
                MathHelper.floor_float(z1 + nz * i),
                MathHelper.floor_float(x2 + nx * i),
                MathHelper.floor_float(z2 + nz * i),
                pixelsAffected);
        }

        pixelsToNodes(world, pixelsAffected, y, waveStrength, velocity);
    }

    private void pixelsToNodes(World world, ArrayList<Long> pixelsAffected, int y, float waveStrength,
        double velocity) {
        HashMap<NodeKey, WakeNode> affected = new HashMap<NodeKey, WakeNode>();
        for (Long pixel : pixelsAffected) {
            int pixelX = pixelX(pixel.longValue());
            int pixelZ = pixelZ(pixel.longValue());
            NodeKey key = new NodeKey(pixelX >> NODE_POWER, y, pixelZ >> NODE_POWER);
            WakeNode node = affected.get(key);
            if (node == null) {
                node = new WakeNode(world, key.x, key.y, key.z, FLOOD_FILL_DISTANCE);
                affected.put(key, node);
            }

            node.setInitialValue(
                Math.floorMod(pixelX, NODE_RES),
                Math.floorMod(pixelZ, NODE_RES),
                waveStrength,
                velocity);
        }

        for (WakeNode node : affected.values()) {
            insertNode(node);
        }
    }

    private void insertNode(WakeNode incoming) {
        if (!isValidNodePos(incoming.world, incoming.x, incoming.y, incoming.z)) {
            return;
        }

        NodeKey key = incoming.key();
        WakeNode existing = nodes.get(key);
        if (existing != null) {
            existing.revive(incoming);
            return;
        }

        if (nodes.size() >= MAX_NODES) {
            removeOldestNode();
        }
        nodes.put(key, incoming);
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

        NodeKey key = new NodeKey(x, y, z);
        WakeNode existing = nodes.get(key);
        if (existing != null) {
            existing.age = 0;
            existing.floodLevel = Math.max(existing.floodLevel, nextLevel);
            return;
        }

        insertNode(new WakeNode(source.world, x, y, z, nextLevel));
    }

    private void trimNodeCap() {
        while (nodes.size() > MAX_NODES) {
            removeOldestNode();
        }
    }

    private void removeOldestNode() {
        NodeKey oldestKey = null;
        int oldestAge = -1;
        for (Map.Entry<NodeKey, WakeNode> entry : nodes.entrySet()) {
            if (entry.getValue().age > oldestAge) {
                oldestAge = entry.getValue().age;
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            nodes.remove(oldestKey);
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

    private static void renderNode(Tessellator tessellator, WakeNode node, double camX, double camY, double camZ,
        float partialTicks) {
        float ageDelta = node.prevAge + (node.age - node.prevAge) * partialTicks;
        float progress = clamp(ageDelta / (float) MAX_AGE, 0.0F, 1.0F);
        float ageFade = 1.0F - progress * progress;
        if (ageFade <= 0.003F) {
            return;
        }

        double pixelSize = 1.0D / NODE_RES;
        double y = node.surfaceY + SURFACE_OFFSET - camY;

        tessellator.setBrightness(FULL_BRIGHT);
        for (int pixelZ = 0; pixelZ < NODE_RES; pixelZ++) {
            double z0 = node.z + pixelZ * pixelSize - camZ;
            double z1 = z0 + pixelSize;
            for (int pixelX = 0; pixelX < NODE_RES; pixelX++) {
                int[] color = sampleColor(node.getWave(pixelX, pixelZ), node.tint, ageFade);
                if (color[3] <= 2) {
                    continue;
                }

                double x0 = node.x + pixelX * pixelSize - camX;
                double x1 = x0 + pixelSize;
                tessellator.setColorRGBA(color[0], color[1], color[2], color[3]);
                tessellator.addVertex(x0, y, z0);
                tessellator.addVertex(x0, y, z1);
                tessellator.addVertex(x1, y, z1);
                tessellator.addVertex(x1, y, z0);
            }
        }
    }

    private static int[] sampleColor(float wave, float[] tint, float ageFade) {
        double clampedRange = 1.0D / (1.0D + Math.exp(-0.1D * wave));
        int index = COLOR_INTERVALS.length;
        for (int i = 0; i < COLOR_INTERVALS.length; i++) {
            if (clampedRange < COLOR_INTERVALS[i]) {
                index = i;
                break;
            }
        }

        int[] color = WAKE_COLORS[index];
        double srcA = Math.pow(color[3] / 255.0D, 5.0D);
        int r = (int) (color[0] * srcA + tint[0] * 255.0F * (1.0D - srcA));
        int g = (int) (color[1] * srcA + tint[1] * 255.0F * (1.0D - srcA));
        int b = (int) (color[2] * srcA + tint[2] * 255.0F * (1.0D - srcA));
        float opacity = color[3] == 0xFF ? ageFade : ageFade * Config.waterWakeAlpha;
        int a = (int) (color[3] * opacity);
        return new int[] { r, g, b, a };
    }

    private static void bresenhamLine(int x1, int z1, int x2, int z2, ArrayList<Long> points) {
        int dz = z2 - z1;
        int dx = x2 - x1;
        if (dx == 0) {
            if (z2 < z1) {
                int temp = z1;
                z1 = z2;
                z2 = temp;
            }
            for (int z = z1; z < z2 + 1; z++) {
                points.add(pixelKey(x1, z));
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
                    points.add(pixelKey(x, z));
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
                    points.add(pixelKey(x, z));
                    offset += delta;
                    if (offset >= threshold) {
                        x += adjust;
                        threshold += thresholdInc;
                    }
                }
            }
        }
    }

    private static long pixelKey(int x, int z) {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }

    private static int pixelX(long key) {
        return (int) (key >> 32);
    }

    private static int pixelZ(long key) {
        return (int) key;
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

    private static class NodeKey {

        final int x;
        final int y;
        final int z;

        NodeKey(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof NodeKey)) {
                return false;
            }

            NodeKey key = (NodeKey) other;
            return x == key.x && y == key.y && z == key.z;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }

    private static class WakeNode {

        final World world;
        final int x;
        final int y;
        final int z;
        final double surfaceY;
        final float[] tint;
        final float[][][] u = new float[3][NODE_RES + 2][NODE_RES + 2];
        final float[][] initialValues = new float[NODE_RES + 2][NODE_RES + 2];
        int floodLevel;
        int age;
        int prevAge;
        float t;

        WakeNode(World world, int x, int y, int z, int floodLevel) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.surfaceY = getWakeSurfaceY(world, x, y, z);
            this.tint = sampleWakeFluidColor(world, x, y, z);
            this.floodLevel = floodLevel;
        }

        NodeKey key() {
            return new NodeKey(x, y, z);
        }

        void setInitialValue(int x, int z, float waveStrength, double velocity) {
            float value = (float) (waveStrength * velocity) * (NODE_RES / 16.0F);
            for (int dz = -1; dz < 2; dz++) {
                for (int dx = -1; dx < 2; dx++) {
                    int px = x + dx + 1;
                    int pz = z + dz + 1;
                    if (Math.abs(value) > Math.abs(initialValues[pz][px])) {
                        initialValues[pz][px] = value;
                    }
                }
            }
        }

        void revive(WakeNode incoming) {
            this.age = 0;
            this.floodLevel = FLOOD_FILL_DISTANCE;
            for (int z = 0; z < NODE_RES + 2; z++) {
                System.arraycopy(incoming.initialValues[z], 0, this.initialValues[z], 0, NODE_RES + 2);
            }
        }

        void tick(WakeNode north, WakeNode south, WakeNode east, WakeNode west) {
            float alpha = (float) Math.pow(WAVE_PROPAGATION_FACTOR * 16.0F / 20.0F, 2.0D);
            float beta = (float) (Math.log(10.0D * WAVE_DECAY_FACTOR + 10.0D) / Math.log(20.0D));

            for (int i = 2; i >= 1; i--) {
                if (north != null) {
                    u[i][0] = north.u[i][NODE_RES];
                }
                if (south != null) {
                    u[i][NODE_RES + 1] = south.u[i][1];
                }
                for (int z = 0; z < NODE_RES + 2; z++) {
                    if (east == null && west == null) {
                        break;
                    }
                    if (east != null) {
                        u[i][z][NODE_RES + 1] = east.u[i][z][1];
                    }
                    if (west != null) {
                        u[i][z][0] = west.u[i][z][NODE_RES];
                    }
                }
            }

            for (int z = 1; z < NODE_RES + 1; z++) {
                for (int x = 1; x < NODE_RES + 1; x++) {
                    u[0][z][x] += initialValues[z][x];
                    initialValues[z][x] = 0.0F;
                    u[2][z][x] = u[1][z][x];
                    u[1][z][x] = u[0][z][x];
                }
            }

            for (int z = 1; z < NODE_RES + 1; z++) {
                for (int x = 1; x < NODE_RES + 1; x++) {
                    u[0][z][x] = (float) (alpha
                        * (0.5F * u[1][z - 1][x] + 0.25F * u[1][z - 1][x + 1]
                            + 0.5F * u[1][z][x + 1]
                            + 0.25F * u[1][z + 1][x + 1]
                            + 0.5F * u[1][z + 1][x]
                            + 0.25F * u[1][z + 1][x - 1]
                            + 0.5F * u[1][z][x - 1]
                            + 0.25F * u[1][z - 1][x - 1]
                            - 3.0F * u[1][z][x])
                        + 2.0F * u[1][z][x]
                        - u[2][z][x]);
                    u[0][z][x] *= beta;
                }
            }
        }

        float getWave(int x, int z) {
            return (u[0][z + 1][x + 1] + u[1][z + 1][x + 1] + u[2][z + 1][x + 1]) / 3.0F;
        }
    }
}
