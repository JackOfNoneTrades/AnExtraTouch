package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.block.BlockLiquid;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WaterfallSprayFX extends EntityFX {

    public WaterfallSprayFX(World world, double x, double y, double z, double velX, double velZ) {
        this(world, x, y, z, velX, velZ, FallingWaterFX.getWaterColor(world, x, y, z));
    }

    public WaterfallSprayFX(World world, double x, double y, double z, double velX, double velZ, float[] rgb) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.motionX *= 0.30000001192092896D;
        this.motionY = (double) ((float) Math.random() * 0.2F + 0.1F);
        this.motionZ *= 0.30000001192092896D;

        this.particleRed = rgb[0];
        this.particleGreen = rgb[1];
        this.particleBlue = rgb[2];

        this.setSize(0.01F, 0.01F);
        this.particleGravity = 0.06F;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));

        this.motionX += velX;
        this.motionY *= 0.75D;
        this.motionZ += velZ;

        IIcon icon = WaterCascadeManager.getSprayIcon();
        if (icon != null) {
            this.setParticleIcon(icon);
        }
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY -= (double) this.particleGravity;
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9800000190734863D;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= 0.9800000190734863D;

        if (this.particleMaxAge-- <= 0) {
            this.setDead();
        }

        if (this.onGround) {
            if (Math.random() < 0.5D) {
                this.setDead();
            }

            this.motionX *= 0.699999988079071D;
            this.motionZ *= 0.699999988079071D;
        }

        IIcon icon = WaterCascadeManager.getSprayIcon();
        if (icon != null && this.particleIcon == null) {
            this.setParticleIcon(icon);
        }

        net.minecraft.block.material.Material material = this.worldObj
            .getBlock(
                MathHelper.floor_double(this.posX),
                MathHelper.floor_double(this.posY),
                MathHelper.floor_double(this.posZ))
            .getMaterial();

        if (material.isLiquid() || material.isSolid()) {
            int x = MathHelper.floor_double(this.posX);
            int y = MathHelper.floor_double(this.posY);
            int z = MathHelper.floor_double(this.posZ);
            double fluidSurfaceY = WetnessFluidHelper.getWettableFluidSurfaceY(this.worldObj, x, y, z);
            double liquidY = fluidSurfaceY >= 0.0D ? fluidSurfaceY
                : (double) ((float) (y + 1)
                    - BlockLiquid.getLiquidHeightPercent(this.worldObj.getBlockMetadata(x, y, z)));

            if (this.posY < liquidY) {
                this.setDead();
            }
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }
}
