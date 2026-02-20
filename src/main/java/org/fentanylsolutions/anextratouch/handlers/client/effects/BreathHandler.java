package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BreathHandler {

    private static final ResourceLocation PARTICLE_TEX = new ResourceLocation("textures/particle/particles.png");
    private final List<FrostBreathFX> breathParticles = new ArrayList<>();

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!Config.breathEnabled) {
            return;
        }
        if (!event.entity.worldObj.isRemote) {
            return;
        }
        if (!(event.entity instanceof EntityLivingBase)) {
            return;
        }

        // Breathing rhythm: active for 3 out of 8 cycles of 10 ticks each
        // Multiplying entity ID by a prime for better phase distribution (like DS)
        int tickCount = event.entity.ticksExisted + event.entity.getEntityId() * 311;
        if ((tickCount / 10) % 8 >= 3) {
            return;
        }

        if (!AnExtraTouch.vic.breath.breathUpOffsets.containsKey(event.entity.getClass())) {
            return;
        }

        if (event.entity.isInWater()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.renderViewEntity == null) {
            return;
        }
        double distSq = event.entity.getDistanceSqToEntity(mc.renderViewEntity);
        if (distSq > (double) Config.breathRenderDistance * Config.breathRenderDistance) {
            return;
        }

        int dimId = event.entity.worldObj.provider.dimensionId;
        String dimMode = AnExtraTouch.vic.breath.getBreathDimensionMode(dimId);
        if ("never".equals(dimMode)) {
            return;
        }

        // Skip if head is lowered (eating animation)
        if (event.entity instanceof EntitySheep && ((EntitySheep) event.entity).func_70890_k(0) != 0.0f) {
            return;
        }
        if (event.entity instanceof EntityHorse && ((EntityHorse) event.entity).isEatingHaystack()) {
            return;
        }

        // Biome/altitude check (skip in "always" mode)
        if (!"always".equals(dimMode)) {
            int bx = MathHelper.floor_double(event.entity.posX);
            int by = MathHelper.floor_double(event.entity.boundingBox.minY);
            int bz = MathHelper.floor_double(event.entity.posZ);
            BiomeGenBase biome = event.entity.worldObj.getBiomeGenForCoords(bx, bz);
            if (!AnExtraTouch.vic.breath.isColdBiome(biome)) {
                float temp = biome.getFloatTemperature(bx, by, bz);
                if (temp >= Config.breathTemperatureThreshold && by < Config.breathAltitudeThreshold) {
                    return;
                }
            }
        }

        // Look vector
        float yawRad = (float) Math.toRadians(((EntityLivingBase) event.entity).rotationYawHead);
        float pitchRad = (float) Math.toRadians(event.entity.rotationPitch);
        double cosYaw = MathHelper.cos(-yawRad - (float) Math.PI);
        double sinYaw = MathHelper.sin(-yawRad - (float) Math.PI);
        double cosPitch = -MathHelper.cos(-pitchRad);
        double sinPitch = MathHelper.sin(-pitchRad);
        Vec3 look = Vec3.createVectorHelper(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
        boolean baby = ((EntityLivingBase) event.entity).isChild();
        Class<?> clazz = event.entity.getClass();

        double upOffset = baby ? AnExtraTouch.vic.breath.babyBreathUpOffsets.getFloat(clazz)
            : AnExtraTouch.vic.breath.breathUpOffsets.getFloat(clazz);
        double forwardDist = baby ? AnExtraTouch.vic.breath.babyBreathForwardDists.getFloat(clazz)
            : AnExtraTouch.vic.breath.breathForwardDists.getFloat(clazz);

        double eyeX = event.entity.posX;
        double eyeY = event.entity.boundingBox.minY + event.entity.height;
        double eyeZ = event.entity.posZ;

        double x = eyeX + look.xCoord * forwardDist;
        double y = eyeY + upOffset + look.yCoord * forwardDist;
        double z = eyeZ + look.zCoord * forwardDist;

        // Trajectory: look direction rotated randomly 0-2 radians on each axis (wide spread like DS)
        Random rand = event.entity.worldObj.rand;
        Vec3 trajectory = rotateVec(look, rand.nextFloat() * 2.0f, rand.nextFloat() * 2.0f);
        double len = Math.sqrt(
            trajectory.xCoord * trajectory.xCoord + trajectory.yCoord * trajectory.yCoord
                + trajectory.zCoord * trajectory.zCoord);
        if (len > 0) {
            trajectory = Vec3
                .createVectorHelper(trajectory.xCoord / len, trajectory.yCoord / len, trajectory.zCoord / len);
        }

        double speed = 0.01;
        double vx = trajectory.xCoord * speed;
        double vy = trajectory.yCoord * speed;
        double vz = trajectory.zCoord * speed;

        FrostBreathFX particle = new FrostBreathFX(event.entity.worldObj, x, y, z, vx, vy, vz, baby);
        breathParticles.add(particle);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!Config.breathEnabled || breathParticles.isEmpty()) {
            if (!Config.breathEnabled) {
                breathParticles.clear();
            }
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            breathParticles.clear();
            return;
        }

        Iterator<FrostBreathFX> it = breathParticles.iterator();
        while (it.hasNext()) {
            FrostBreathFX fx = it.next();
            if (fx == null || fx.isDead || fx.worldObj != mc.theWorld) {
                it.remove();
                continue;
            }
            fx.onUpdate();
            if (fx.isDead) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!Config.breathEnabled || breathParticles.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase viewer = mc.renderViewEntity;
        if (viewer == null || mc.theWorld == null) {
            return;
        }

        float rotX = ActiveRenderInfo.rotationX;
        float rotZ = ActiveRenderInfo.rotationZ;
        float rotYZ = ActiveRenderInfo.rotationYZ;
        float rotXY = ActiveRenderInfo.rotationXY;
        float rotXZ = ActiveRenderInfo.rotationXZ;
        EntityFX.interpPosX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * (double) event.partialTicks;
        EntityFX.interpPosY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * (double) event.partialTicks;
        EntityFX.interpPosZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * (double) event.partialTicks;

        mc.getTextureManager()
            .bindTexture(PARTICLE_TEX);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569f);

            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            for (FrostBreathFX fx : breathParticles) {
                if (fx == null || fx.isDead || fx.worldObj != mc.theWorld) {
                    continue;
                }
                tess.setBrightness(fx.getBrightnessForRender(event.partialTicks));
                fx.renderParticle(tess, event.partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
            }
            tess.draw();
        } finally {
            GL11.glPopAttrib();
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        }
    }

    // Rotate a vector by yRot radians around Y axis, then xRot radians around X axis.
    private static Vec3 rotateVec(Vec3 v, float yRot, float xRot) {
        // Y rotation
        double cosY = MathHelper.cos(yRot);
        double sinY = MathHelper.sin(yRot);
        double rx = v.xCoord * cosY + v.zCoord * sinY;
        double ry = v.yCoord;
        double rz = -v.xCoord * sinY + v.zCoord * cosY;

        // X rotation
        double cosX = MathHelper.cos(xRot);
        double sinX = MathHelper.sin(xRot);
        double fx = rx;
        double fy = ry * cosX - rz * sinX;
        double fz = ry * sinX + rz * cosX;

        return Vec3.createVectorHelper(fx, fy, fz);
    }
}
