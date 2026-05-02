package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.block.Block;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CascadeFX extends EntityFX {

    public CascadeFX(World world, double x, double y, double z) {
        super(world, x, y, z);
        // Particular's CascadeParticle: small horizontal random, no vertical velocity.
        this.motionX = world.rand.nextDouble() * 0.25D - 0.125D;
        this.motionY = 0.0D;
        this.motionZ = world.rand.nextDouble() * 0.25D - 0.125D;
        this.particleMaxAge = 9;
        // Modern billboard particle scale is rendered directly; 1.7.10 renders 0.1 * particleScale.
        this.particleScale = 10.0F;
        // EntityFX.onUpdate applies motionY -= 0.04 * particleGravity. Particular's super.tick
        // applies velocityY -= 0.04 * gravityStrength (0.4f), so set particleGravity = 0.4 to match.
        this.particleGravity = 0.4F;
        updateSpriteForAge();
        removeIfInsideSolidBlock();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isDead) {
            return;
        }
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
