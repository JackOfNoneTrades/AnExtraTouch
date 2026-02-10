package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FrostBreathFX extends EntityFX {

    private final float baseScale;

    public FrostBreathFX(World world, double x, double y, double z, double vx, double vy, double vz, boolean baby) {
        super(world, x, y, z, 0, 0, 0);

        this.motionX = vx;
        this.motionY = vy;
        this.motionZ = vz;

        // White with slight random brightness variation (up to 30% darker)
        float brightness = 1.0f - (float) (rand.nextDouble() * 0.3);
        this.particleRed = brightness;
        this.particleGreen = brightness;
        this.particleBlue = brightness;

        // DS uses 0.2 * 1.875 * (baby ? 0.125 : 0.25)
        // 1.7.10 default renderParticle uses 0.1 * particleScale as half-size
        // Scale up to compensate: DS final = ~0.094, here 0.1 * 0.94 = 0.094
        this.baseScale = 1.875f * (baby ? 0.125f : 0.25f) * 1.5f;
        this.particleScale = 0.0f;

        this.particleMaxAge = (int) Math.max(((8.0 / (rand.nextDouble() * 0.8 + 0.3)) * 2.5), 1);

        // Cloud sprite: row 0, random starting frame
        this.particleTextureIndexY = 0;
        this.particleTextureIndexX = rand.nextInt(8);

        this.particleAlpha = 0.2f;
        this.particleGravity = 0.0f;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setDead();
            return;
        }

        // Reach full size at 1/32 of lifetime, then stay like that
        float ageFraction = (float) this.particleAge / (float) this.particleMaxAge;
        float sizeFraction = Math.min(ageFraction * 32.0f, 1.0f);
        this.particleScale = this.baseScale * sizeFraction;

        this.moveEntity(this.motionX, this.motionY, this.motionZ);

        // DECELERATION ZAAAAM
        this.motionX *= 0.96;
        this.motionY *= 0.96;
        this.motionZ *= 0.96;

        if (this.onGround) {
            this.motionX *= 0.7;
            this.motionZ *= 0.7;
        }
    }

    @Override
    public int getFXLayer() {
        return 0;
    }
}
