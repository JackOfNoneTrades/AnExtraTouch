package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.TextureStitchEvent;
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
        boolean hasWetColor;
        float wetRed;
        float wetGreen;
        float wetBlue;

        void setWetColor(float[] rgb) {
            this.wetRed = rgb[0];
            this.wetGreen = rgb[1];
            this.wetBlue = rgb[2];
            this.hasWetColor = true;
        }

        float[] getWetColor() {
            return new float[] { this.wetRed, this.wetGreen, this.wetBlue };
        }

        void clearWetColor() {
            this.hasWetColor = false;
        }
    }

    private final WeakHashMap<EntityLivingBase, WetnessTracker> trackers = new WeakHashMap<>();

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Post event) {
        FallingWaterFX.invalidateWaterColor();
    }

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

        EntityLivingBase entity = (EntityLivingBase) event.entity;
        WetnessFluidHelper.FluidSample fluidSample = WetnessFluidHelper.findWettableFluid(entity);
        boolean inFluid = fluidSample != null;
        boolean wetFromRain = Config.wetnessRainEnabled && WetnessFluidHelper.isRainingOn(entity);

        // Update wetness every WETNESS_TICK_INTERVAL ticks
        tracker.tickCounter++;
        if (tracker.tickCounter >= WETNESS_TICK_INTERVAL) {
            tracker.tickCounter = 0;

            int wetnessLimit = (int) (WETNESS_LIMIT * Config.wetnessDuration);
            if (inFluid) {
                tracker.wetness = Math.min(wetnessLimit, tracker.wetness + WETNESS_FLUID_INCREASE);
                tracker.setWetColor(fluidSample.rgb);
            } else if (wetFromRain) {
                tracker.wetness = Math.min(wetnessLimit, tracker.wetness + WETNESS_RAIN_INCREASE);
                updateRainColor(tracker, entity);
            } else if (event.entity.isBurning()) {
                tracker.wetness = Math.max(0, tracker.wetness - WETNESS_FIRE_DECREASE);
            } else {
                tracker.wetness = Math.max(0, tracker.wetness - WETNESS_DECREASE);
            }

            if (tracker.wetness <= 0) {
                tracker.clearWetColor();
            }
        }

        if (tracker.wetness > 0 && !inFluid) {
            int wetnessLimit = (int) (WETNESS_LIMIT * Config.wetnessDuration);
            float wetRatio = Math.min((float) tracker.wetness / wetnessLimit, 1.0f);
            int spawnRate = Math.round((1.0f - wetRatio) * 10f / Config.wetnessParticleDensity);

            if (spawnRate <= 0 || event.entity.worldObj.getTotalWorldTime() % spawnRate == 0) {
                if (!tracker.hasWetColor) {
                    updateRainColor(tracker, entity);
                }

                double x = event.entity.boundingBox.minX + event.entity.worldObj.rand.nextFloat()
                    * (event.entity.boundingBox.maxX - event.entity.boundingBox.minX);
                double y = event.entity.boundingBox.minY + event.entity.worldObj.rand.nextFloat()
                    * (event.entity.boundingBox.maxY - event.entity.boundingBox.minY);
                double z = event.entity.boundingBox.minZ + event.entity.worldObj.rand.nextFloat()
                    * (event.entity.boundingBox.maxZ - event.entity.boundingBox.minZ);

                Minecraft.getMinecraft().effectRenderer
                    .addEffect(new FallingWaterFX(event.entity.worldObj, x, y, z, tracker.getWetColor()));
            }
        }
    }

    private static void updateRainColor(WetnessTracker tracker, EntityLivingBase entity) {
        tracker.setWetColor(FallingWaterFX.getWaterColor(entity.worldObj, entity.posX, entity.posY, entity.posZ));
    }
}
