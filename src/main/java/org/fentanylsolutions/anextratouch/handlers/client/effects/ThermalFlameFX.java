package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class ThermalFlameFX extends EntityFX {

    private final float jetScale;

    ThermalFlameFX(World world, double x, double y, double z, double rise, float scale) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.motionX *= 0.01D;
        this.motionY = rise;
        this.motionZ *= 0.01D;
        this.jetScale = scale;
        this.particleMaxAge = (int) (8.0D / (rand.nextDouble() * 0.8D + 0.2D)) + 4;
        this.noClip = true;
        this.setParticleTextureIndex(48);
    }

    @Override
    public void renderParticle(Tessellator tessellator, float partialTick, float rotationX, float rotationZ,
        float rotationYZ, float rotationXY, float rotationXZ) {
        float age = ((float) this.particleAge + partialTick) / (float) this.particleMaxAge;
        this.particleScale = jetScale * (1.0F - age * age * 0.5F);
        super.renderParticle(tessellator, partialTick, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        float age = Math.min(1.0F, ((float) this.particleAge + partialTick) / (float) this.particleMaxAge);
        int brightness = super.getBrightnessForRender(partialTick);
        int block = Math.min(240, (brightness & 255) + (int) (age * 240.0F));
        return block | (brightness >> 16 & 255) << 16;
    }

    @Override
    public float getBrightness(float partialTick) {
        float age = Math.min(1.0F, ((float) this.particleAge + partialTick) / (float) this.particleMaxAge);
        return super.getBrightness(partialTick) * age + 1.0F - age;
    }
}
