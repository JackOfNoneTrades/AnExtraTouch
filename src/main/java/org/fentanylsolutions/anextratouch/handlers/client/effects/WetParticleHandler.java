package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingEvent;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WetParticleHandler {

    private static final int WETNESS_LIMIT = 400;
    private static final int WETNESS_TICK_INTERVAL = 10;
    private static final int WETNESS_FLUID_INCREASE = 10;
    private static final int WETNESS_RAIN_INCREASE = 7;
    private static final int WETNESS_DECREASE = 3;
    private static final int WETNESS_FIRE_DECREASE = 10;

    private static class WetnessTracker {

        int wetness;
        int tickCounter;
    }

    private final WeakHashMap<EntityLivingBase, WetnessTracker> trackers = new WeakHashMap<>();

    // Gives entities DRIP
    // Entities also dry quicker when burning
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!event.entity.worldObj.isRemote) {
            return;
        }
        if (!Config.wetParticlesEnabled) {
            return;
        }
        if (!(event.entity instanceof EntityLivingBase)) {
            return;
        }

        if (!AnExtraTouch.vic.wetnessEntities.contains(event.entity.getClass())) {
            return;
        }

        WetnessTracker tracker = trackers.get(event.entity);
        if (tracker == null) {
            tracker = new WetnessTracker();
            trackers.put((EntityLivingBase) event.entity, tracker);
        }

        // Update wetness every WETNESS_TICK_INTERVAL ticks
        tracker.tickCounter++;
        if (tracker.tickCounter >= WETNESS_TICK_INTERVAL) {
            tracker.tickCounter = 0;

            int wetnessLimit = (int) (WETNESS_LIMIT * Config.wetnessDuration);
            if (event.entity.isInWater()) {
                tracker.wetness = Math.min(wetnessLimit, tracker.wetness + WETNESS_FLUID_INCREASE);
            } else if (Config.wetnessRainEnabled && event.entity.isWet()) {
                tracker.wetness = Math.min(wetnessLimit, tracker.wetness + WETNESS_RAIN_INCREASE);
            } else if (event.entity.isBurning()) {
                tracker.wetness = Math.max(0, tracker.wetness - WETNESS_FIRE_DECREASE);
            } else {
                tracker.wetness = Math.max(0, tracker.wetness - WETNESS_DECREASE);
            }
        }

        if (tracker.wetness > 0 && !event.entity.isInWater()) {
            int wetnessLimit = (int) (WETNESS_LIMIT * Config.wetnessDuration);
            float wetRatio = Math.min((float) tracker.wetness / wetnessLimit, 1.0f);
            int spawnRate = Math.round((1.0f - wetRatio) * 10f / Config.wetnessParticleDensity);

            if (spawnRate <= 0 || event.entity.worldObj.getTotalWorldTime() % spawnRate == 0) {
                double x = event.entity.boundingBox.minX + event.entity.worldObj.rand.nextFloat()
                    * (event.entity.boundingBox.maxX - event.entity.boundingBox.minX);
                double y = event.entity.boundingBox.minY + event.entity.worldObj.rand.nextFloat()
                    * (event.entity.boundingBox.maxY - event.entity.boundingBox.minY);
                double z = event.entity.boundingBox.minZ + event.entity.worldObj.rand.nextFloat()
                    * (event.entity.boundingBox.maxZ - event.entity.boundingBox.minZ);

                Minecraft.getMinecraft().effectRenderer.addEffect(new FallingWaterFX(event.entity.worldObj, x, y, z));
            }
        }
    }
}
