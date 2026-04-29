package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CascadeFX extends EntityFX {

    public CascadeFX(World world, double x, double y, double z, double velX, double velY, double velZ) {
        super(world, x, y, z, velX, velY, velZ);
        this.motionX = velX;
        this.motionY = velY;
        this.motionZ = velZ;
        this.particleMaxAge = 10;
        this.particleScale = 4.0F;
        updateSpriteForAge();
        removeIfInsideSolidBlock();
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setDead();
        }

        if (this.isDead) {
            return;
        }

        int nextX = MathHelper.floor_double(this.posX);
        int nextY = MathHelper.floor_double(this.posY + this.motionY);
        int nextZ = MathHelper.floor_double(this.posZ);
        if (this.onGround || (this.particleAge > 10 && this.worldObj.getBlock(nextX, nextY, nextZ)
            .getMaterial() == Material.water)) {
            this.motionX *= 0.5D;
            this.motionY *= 0.5D;
            this.motionZ *= 0.5D;
        }

        if (this.worldObj.getBlock(nextX, nextY, nextZ)
            .getMaterial() == Material.water
            && this.worldObj.isAirBlock(
                MathHelper.floor_double(this.posX),
                MathHelper.floor_double(this.posY),
                MathHelper.floor_double(this.posZ))) {
            this.motionX *= 0.9D;
            this.motionY *= 0.9D;
            this.motionZ *= 0.9D;
        }

        this.motionX *= 0.95D;
        this.motionY -= 0.02D;
        this.motionZ *= 0.95D;
        this.moveEntity(this.motionX, this.motionY, this.motionZ);

        removeIfInsideSolidBlock();
        updateSpriteForAge();
    }

    private void updateSpriteForAge() {
        int frame = this.particleAge * WaterCascadeManager.CASCADE_FRAME_COUNT / this.particleMaxAge;
        if (frame < 0) frame = 0;
        if (frame >= WaterCascadeManager.CASCADE_FRAME_COUNT) frame = WaterCascadeManager.CASCADE_FRAME_COUNT - 1;

        IIcon icon = WaterCascadeManager.getCascadeIcon(frame);
        if (icon != null) {
            this.setParticleIcon(icon);
        }
    }

    private void removeIfInsideSolidBlock() {
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.posY);
        int z = MathHelper.floor_double(this.posZ);
        Block block = this.worldObj.getBlock(x, y, z);
        if (block.getMaterial()
            .blocksMovement()) {
            this.particleAlpha = 0.0F;
            this.setDead();
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }
}
