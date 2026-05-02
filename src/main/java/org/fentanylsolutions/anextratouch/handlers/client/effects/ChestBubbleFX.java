package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ChestBubbleFX extends EntityFX {

    private final Block sourceBlock;

    public ChestBubbleFX(World world, double x, double y, double z) {
        this(world, x, y, z, null, 1.0F, 1.0F, 1.0F);
    }

    public ChestBubbleFX(World world, double x, double y, double z, Block sourceBlock, float red, float green,
        float blue) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sourceBlock = sourceBlock;
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.noClip = true;
        this.motionX = (double) ((world.rand.nextFloat() * 2.0F - 1.0F) * 0.02F);
        this.motionY = 0.025D + (double) (world.rand.nextFloat() * 0.02F);
        this.motionZ = (double) ((world.rand.nextFloat() * 2.0F - 1.0F) * 0.02F);
        this.particleRed = red;
        this.particleGreen = green;
        this.particleBlue = blue;
        this.setParticleTextureIndex(32);
        this.setSize(0.02F, 0.02F);
        this.particleScale *= this.rand.nextFloat() * 0.6F + 0.2F;
        this.particleMaxAge = 20 + world.rand.nextInt(20);
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY += 0.002D;
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.8500000238418579D;
        this.motionY *= 0.8500000238418579D;
        this.motionZ *= 0.8500000238418579D;

        if (!isInWaterOrChest()) {
            this.setDead();
        }

        if (this.particleMaxAge-- <= 0) {
            this.setDead();
        }
    }

    private boolean isInWaterOrChest() {
        Block block = this.worldObj.getBlock(
            MathHelper.floor_double(this.posX),
            MathHelper.floor_double(this.posY),
            MathHelper.floor_double(this.posZ));
        return block.getMaterial() == Material.water || block == this.sourceBlock
            || block == Blocks.chest
            || block == Blocks.trapped_chest;
    }
}
