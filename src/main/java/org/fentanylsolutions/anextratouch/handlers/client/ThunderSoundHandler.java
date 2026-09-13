package org.fentanylsolutions.anextratouch.handlers.client;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.fentlib.util.sound.ICustomMaxDistanceSound;
import org.fentanylsolutions.fentlib.util.sound.SoundUtil;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class ThunderSoundHandler {

    private static final ResourceLocation VANILLA_THUNDER = new ResourceLocation("minecraft:ambient.weather.thunder");
    private static final ResourceLocation THUNDER = new ResourceLocation("anextratouch:weather.thunder");

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlaySound(PlaySoundEvent17 event) {
        ISound original = event.result;
        if (!Config.thunderSoundsEnabled || original == null
            || !VANILLA_THUNDER.equals(original.getPositionedSoundLocation())) return;

        event.result = Config.thunderSoundVolume > 0f && original.getVolume() > 0f ? new ThunderSound(original) : null;
    }

    private static final class ThunderSound extends MovingSound implements ICustomMaxDistanceSound {

        private final float baseVolume;
        private final float maxDistance;

        private ThunderSound(ISound original) {
            super(THUNDER);
            // Vanilla thunder uses volume 10000 to reach distant players. Separate range from gain so
            // the Weather slider and our volume control remain useful below full volume.
            this.baseVolume = MathHelper.clamp_float(original.getVolume(), 0f, 1f);
            this.maxDistance = SoundUtil.getEffectiveMaxDistance(original, SoundUtil.getVanillaMaxDistance(original));
            this.xPosF = original.getXPosF();
            this.yPosF = original.getYPosF();
            this.zPosF = original.getZPosF();
            this.field_147663_c = 1f;
            this.field_147666_i = original.getAttenuationType();
            this.repeat = original.canRepeat();
            this.field_147665_h = original.getRepeatDelay();
        }

        @Override
        public float getVolume() {
            return this.baseVolume * Config.thunderSoundVolume;
        }

        @Override
        public float getMaxSoundDistance() {
            return this.maxDistance;
        }

        @Override
        public void update() {
            if (!Config.thunderSoundsEnabled) {
                this.donePlaying = true;
            }
        }
    }
}
