package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WaterfallLoopSound extends MovingSound {

    private final ResourceLocation soundId;
    private boolean stopped;
    private float nextX;
    private float nextY;
    private float nextZ;
    private float nextVolume;

    public WaterfallLoopSound(ResourceLocation soundId, float pitch) {
        super(soundId);
        this.soundId = soundId;
        this.repeat = true;
        this.field_147665_h = 0;
        this.field_147663_c = pitch;
        this.field_147666_i = AttenuationType.LINEAR;
    }

    public ResourceLocation getSoundId() {
        return this.soundId;
    }

    public void setState(double x, double y, double z, float volume) {
        this.nextX = (float) x;
        this.nextY = (float) y;
        this.nextZ = (float) z;
        this.nextVolume = volume;
        this.xPosF = this.nextX;
        this.yPosF = this.nextY;
        this.zPosF = this.nextZ;
        this.volume = this.nextVolume;
        this.stopped = false;
    }

    public void stop() {
        this.stopped = true;
    }

    @Override
    public void update() {
        if (this.stopped) {
            this.donePlaying = true;
            this.volume = 0.0F;
            return;
        }

        this.xPosF = this.nextX;
        this.yPosF = this.nextY;
        this.zPosF = this.nextZ;
        this.volume = this.nextVolume;
    }
}
