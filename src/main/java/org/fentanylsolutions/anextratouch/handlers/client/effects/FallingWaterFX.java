package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntitySplashFX;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

// Copy of vanilla EntityDropParticleFX (dripWater) but without the
// bobTimer (falls immediately with gravity)
@SideOnly(Side.CLIENT)
public class FallingWaterFX extends EntityFX {

    private static float cachedR = -1f, cachedG, cachedB;

    public FallingWaterFX(World world, double x, double y, double z) {
        this(world, x, y, z, getWaterColor(world, x, y, z));
    }

    public FallingWaterFX(World world, double x, double y, double z, float[] rgb) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.motionX = this.motionY = this.motionZ = 0.0D;

        this.particleRed = rgb[0];
        this.particleGreen = rgb[1];
        this.particleBlue = rgb[2];

        this.setParticleTextureIndex(112);
        this.setSize(0.01F, 0.01F);
        this.particleGravity = 0.06F;
        this.particleMaxAge = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
    }

    /**
     * Returns the water color at the given block as {r, g, b} floats in [0,1].
     *
     * The base water sample is averaged from the water_still texture (so resource packs that
     * recolor water are picked up). Vanilla 1.7.10 ships water_still as grayscale though, so
     * if the sample has no color information we fall back to Config.waterSplashFallbackColor
     * scaled by the texture's luminance. The result is then modulated by a 3x3 average of the
     * surrounding biomes' getWaterColorMultiplier() (which fires Forge BiomeEvent.GetWaterColor
     * so mods like BoP also tint correctly).
     */
    public static float[] getWaterColor(World world, double x, double y, double z) {
        if (cachedR < 0f) sampleWaterColor();

        int bx = MathHelper.floor_double(x);
        int bz = MathHelper.floor_double(z);
        int totalR = 0, totalG = 0, totalB = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int color = world.getBiomeGenForCoords(bx + dx, bz + dz)
                    .getWaterColorMultiplier();
                totalR += (color >> 16) & 0xFF;
                totalG += (color >> 8) & 0xFF;
                totalB += color & 0xFF;
            }
        }
        float mr = (totalR / 9) / 255f;
        float mg = (totalG / 9) / 255f;
        float mb = (totalB / 9) / 255f;

        float baseR = cachedR, baseG = cachedG, baseB = cachedB;
        float texSat = Math.max(baseR, Math.max(baseG, baseB)) - Math.min(baseR, Math.min(baseG, baseB));
        float biomeSat = Math.max(mr, Math.max(mg, mb)) - Math.min(mr, Math.min(mg, mb));
        // Fallback only when neither the texture nor the biome carries color information,
        // i.e. vanilla 1.7.10 grayscale water in a biome that doesn't tint water (jungle, plains).
        if (texSat < 0.04f && biomeSat < 0.04f) {
            float lum = (baseR + baseG + baseB) / 3f;
            int fallback = org.fentanylsolutions.anextratouch.Config.waterSplashFallbackColor;
            float fr = ((fallback >> 16) & 0xFF) / 255f;
            float fg = ((fallback >> 8) & 0xFF) / 255f;
            float fb = (fallback & 0xFF) / 255f;
            float fLum = (fr + fg + fb) / 3f;
            if (fLum > 0.001f) {
                float scale = lum / fLum;
                baseR = fr * scale;
                baseG = fg * scale;
                baseB = fb * scale;
            }
        }

        return new float[] { Math.min(1f, baseR * mr), Math.min(1f, baseG * mg), Math.min(1f, baseB * mb) };
    }

    private static void sampleWaterColor() {
        try {
            IIcon icon = BlockLiquid.getLiquidIcon("water_still");
            if (icon instanceof TextureAtlasSprite) {
                TextureAtlasSprite sprite = (TextureAtlasSprite) icon;
                int[][] frameData = sprite.getFrameTextureData(0);
                if (frameData != null && frameData.length > 0 && frameData[0] != null) {
                    int[] pixels = frameData[0];
                    long totalR = 0, totalG = 0, totalB = 0;
                    int count = 0;
                    for (int pixel : pixels) {
                        int a = (pixel >> 24) & 0xFF;
                        if (a == 0) continue;
                        totalR += (pixel >> 16) & 0xFF;
                        totalG += (pixel >> 8) & 0xFF;
                        totalB += pixel & 0xFF;
                        count++;
                    }
                    if (count > 0) {
                        cachedR = (totalR / (float) count) / 255f;
                        cachedG = (totalG / (float) count) / 255f;
                        cachedB = (totalB / (float) count) / 255f;
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}
        cachedR = 0.2f;
        cachedG = 0.3f;
        cachedB = 1.0f;
    }

    public static void invalidateWaterColor() {
        cachedR = -1f;
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
            EntitySplashFX splash = new EntitySplashFX(
                this.worldObj,
                this.posX,
                this.posY,
                this.posZ,
                0.0D,
                0.0D,
                0.0D);
            splash.setRBGColorF(this.particleRed, this.particleGreen, this.particleBlue);
            Minecraft.getMinecraft().effectRenderer.addEffect(splash);
        }

        int bx = MathHelper.floor_double(this.posX);
        int by = MathHelper.floor_double(this.posY);
        int bz = MathHelper.floor_double(this.posZ);

        net.minecraft.block.Block block = this.worldObj.getBlock(bx, by, bz);
        if (block.getMaterial()
            .isLiquid()
            || block.getMaterial()
                .isSolid()) {
            double fluidSurfaceY = WetnessFluidHelper.getWettableFluidSurfaceY(this.worldObj, bx, by, bz);
            double collisionY = fluidSurfaceY >= 0.0D ? fluidSurfaceY
                : (float) (by + 1) - BlockLiquid.getLiquidHeightPercent(this.worldObj.getBlockMetadata(bx, by, bz));

            if (this.posY < collisionY) {
                if (fluidSurfaceY >= 0.0D) {
                    WaterRippleManager.INSTANCE.trySpawnDripRipple(this.worldObj, this.posX, this.posY, this.posZ);
                }
                this.setDead();
            }
        }
    }
}
