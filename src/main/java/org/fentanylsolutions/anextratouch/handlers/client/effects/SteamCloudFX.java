package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class SteamCloudFX extends EntityFX {

    private final float fullScale;

    SteamCloudFX(World world, double x, double y, double z, boolean solidSource) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.motionX *= 0.1D;
        this.motionY = solidSource ? 0.04D : 0.08D;
        this.motionZ *= 0.1D;
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F - rand.nextFloat() * 0.3F;
        this.fullScale = this.particleScale * 1.875F * (solidSource ? 0.5F : 1.0F);
        this.particleMaxAge = (int) (40.0D / (rand.nextDouble() * 0.8D + 0.3D));
        this.noClip = false;
        this.setParticleTextureIndex(7);
    }

    @Override
    public void renderParticle(Tessellator tessellator, float partialTick, float rotationX, float rotationZ,
        float rotationYZ, float rotationXY, float rotationXZ) {
        this.particleScale = fullScale
            * Math.min(1.0F, ((float) this.particleAge + partialTick) / (float) this.particleMaxAge * 32.0F);
        super.renderParticle(tessellator, partialTick, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
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
        this.setParticleTextureIndex(Math.max(0, 7 - this.particleAge * 8 / this.particleMaxAge));
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.96D;
        this.motionY *= 0.96D;
        this.motionZ *= 0.96D;
        // Steam rises independently of players. Vanilla clouds are pulled toward nearby feet,
        // which drags these particles down and into cauldron walls when viewed up close.
        if (this.onGround) {
            this.motionX *= 0.7D;
            this.motionZ *= 0.7D;
        }
    }
}
