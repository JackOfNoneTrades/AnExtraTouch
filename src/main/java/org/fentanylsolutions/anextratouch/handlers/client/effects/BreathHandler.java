package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.event.entity.living.LivingEvent;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BreathHandler {

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
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

        if (!AnExtraTouch.vic.breathUpOffsets.containsKey(event.entity.getClass())) {
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
        String dimMode = AnExtraTouch.vic.getBreathDimensionMode(dimId);
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
            if (!AnExtraTouch.vic.isColdBiome(biome.biomeName)) {
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

        double upOffset = baby ? AnExtraTouch.vic.babyBreathUpOffsets.getFloat(clazz)
            : AnExtraTouch.vic.breathUpOffsets.getFloat(clazz);
        double forwardDist = baby ? AnExtraTouch.vic.babyBreathForwardDists.getFloat(clazz)
            : AnExtraTouch.vic.breathForwardDists.getFloat(clazz);

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
        mc.effectRenderer.addEffect(particle);
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
