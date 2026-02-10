package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.block.BlockLiquid;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

// Copy of vanilla EntityDropParticleFX (dripWater) but without the
// bobTimer (falls immediately with gravity)
@SideOnly(Side.CLIENT)
public class FallingWaterFX extends EntityFX {

    public FallingWaterFX(World world, double x, double y, double z) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.motionX = this.motionY = this.motionZ = 0.0D;

        this.particleRed = 0.2F;
        this.particleGreen = 0.3F;
        this.particleBlue = 1.0F;

        this.setParticleTextureIndex(112);
        this.setSize(0.01F, 0.01F);
        this.particleGravity = 0.06F;
        this.particleMaxAge = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        this.motionY -= this.particleGravity;

        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9800000190734863D;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= 0.9800000190734863D;

        if (this.particleMaxAge-- <= 0) {
            this.setDead();
        }

        if (this.onGround) {
            this.setDead();
            this.worldObj.spawnParticle("splash", this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
        }

        int bx = MathHelper.floor_double(this.posX);
        int by = MathHelper.floor_double(this.posY);
        int bz = MathHelper.floor_double(this.posZ);

        net.minecraft.block.Block block = this.worldObj.getBlock(bx, by, bz);
        if (block.getMaterial()
            .isLiquid()
            || block.getMaterial()
                .isSolid()) {
            double d0 = (float) (by + 1)
                - BlockLiquid.getLiquidHeightPercent(this.worldObj.getBlockMetadata(bx, by, bz));

            if (this.posY < d0) {
                this.setDead();
            }
        }
    }
}
