package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterSplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityArrow.class)
public class MixinEntityArrow {

    @Unique
    private int anextratouch$splashCooldown;

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void anextratouch$spawnPreciseWaterEntrySplash(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.worldObj.isRemote) return;
        if (!Config.waterSplashEnabled) return;
        if (anextratouch$isSplashBlacklisted(self)) return;

        if (anextratouch$splashCooldown > 0) {
            anextratouch$splashCooldown--;
            return;
        }

        double[] hit = anextratouch$findCrossedExposedTopSurface(self);
        if (hit == null) {
            return;
        }

        WaterSplashManager.INSTANCE
            .spawnEmitter(self.worldObj, hit[0], hit[1], hit[2], self.width, (float) Math.abs(self.motionY));
        anextratouch$spawnVanillaSplashParticles(self, hit[0], hit[1], hit[2]);
        anextratouch$playClientSplashSound(self, hit[0], hit[1], hit[2]);
        anextratouch$splashCooldown = 10;
    }

    @Unique
    private static boolean anextratouch$isSplashBlacklisted(Entity e) {
        if (AnExtraTouch.vic == null || AnExtraTouch.vic.waterSplashEntityBlacklist == null) return false;
        return AnExtraTouch.vic.waterSplashEntityBlacklist.contains(
            e.getClass()
                .getSimpleName());
    }

    @Unique
    private static double[] anextratouch$findCrossedExposedTopSurface(Entity e) {
        double dy = e.posY - e.prevPosY;
        if (dy >= -1.0e-4D) {
            return null;
        }

        AxisAlignedBB currentBox = anextratouch$getWaterProbeBox(e);
        AxisAlignedBB previousBox = currentBox
            .getOffsetBoundingBox(e.prevPosX - e.posX, e.prevPosY - e.posY, e.prevPosZ - e.posZ);

        int minX = MathHelper.floor_double(Math.min(previousBox.minX, currentBox.minX));
        int maxX = MathHelper.floor_double(Math.max(previousBox.maxX, currentBox.maxX) + 1.0D);
        int minY = MathHelper.floor_double(Math.min(e.prevPosY, e.posY)) - 1;
        int maxY = MathHelper.floor_double(Math.max(e.prevPosY, e.posY)) + 1;
        int minZ = MathHelper.floor_double(Math.min(previousBox.minZ, currentBox.minZ));
        int maxZ = MathHelper.floor_double(Math.max(previousBox.maxZ, currentBox.maxZ) + 1.0D);

        double bestT = Double.POSITIVE_INFINITY;
        double[] bestHit = null;
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    double surface = WaterSplashManager.getSplashFluidSurfaceY(e.worldObj, x, y, z);
                    if (surface < 0.0D || WaterSplashManager.isSplashFluidAllowed(e.worldObj, x, y + 1, z)) {
                        continue;
                    }

                    double t = (surface - e.prevPosY) / dy;
                    if (t < -1.0e-4D || t > 1.0001D) {
                        continue;
                    }

                    t = Math.max(0.0D, Math.min(1.0D, t));
                    double hitX = e.prevPosX + (e.posX - e.prevPosX) * t;
                    double hitZ = e.prevPosZ + (e.posZ - e.prevPosZ) * t;
                    if (hitX < x || hitX >= x + 1.0D || hitZ < z || hitZ >= z + 1.0D) {
                        continue;
                    }
                    if (!anextratouch$wasOverBlockBeforeCrossing(e, x, z, t)) {
                        continue;
                    }

                    if (t < bestT) {
                        bestT = t;
                        bestHit = new double[] { hitX, surface, hitZ };
                    }
                }
            }
        }

        return bestHit;
    }

    @Unique
    private static boolean anextratouch$wasOverBlockBeforeCrossing(Entity e, int blockX, int blockZ, double t) {
        double beforeT = Math.max(0.0D, t - 1.0e-3D);
        double beforeX = e.prevPosX + (e.posX - e.prevPosX) * beforeT;
        double beforeZ = e.prevPosZ + (e.posZ - e.prevPosZ) * beforeT;
        return beforeX >= blockX && beforeX < blockX + 1.0D && beforeZ >= blockZ && beforeZ < blockZ + 1.0D;
    }

    @Unique
    private static AxisAlignedBB anextratouch$getWaterProbeBox(Entity e) {
        return e.boundingBox.expand(0.0D, -0.4000000059604645D, 0.0D)
            .contract(0.001D, 0.001D, 0.001D);
    }

    @Unique
    private static boolean anextratouch$isSplashInsideFluid(World world, double x, double y, double z) {
        int blockX = MathHelper.floor_double(x);
        int blockY = MathHelper.floor_double(y);
        int blockZ = MathHelper.floor_double(z);
        double surface = WaterSplashManager.getSplashFluidSurfaceY(world, blockX, blockY, blockZ);
        if (surface < 0.0D) {
            return false;
        }

        return y >= blockY - 1.0e-4D && y < surface - 1.0e-4D;
    }

    @Unique
    private void anextratouch$spawnVanillaSplashParticles(Entity e, double x, double y, double z) {
        int count = (int) (1.0F + e.width * 20.0F);
        for (int i = 0; i < count; i++) {
            float xOffset = (e.worldObj.rand.nextFloat() * 2.0F - 1.0F) * e.width;
            float zOffset = (e.worldObj.rand.nextFloat() * 2.0F - 1.0F) * e.width;
            e.worldObj.spawnParticle(
                "bubble",
                x + xOffset,
                y,
                z + zOffset,
                e.motionX,
                e.motionY - e.worldObj.rand.nextFloat() * 0.2F,
                e.motionZ);
        }

        for (int i = 0; i < count; i++) {
            float xOffset = (e.worldObj.rand.nextFloat() * 2.0F - 1.0F) * e.width;
            float zOffset = (e.worldObj.rand.nextFloat() * 2.0F - 1.0F) * e.width;
            double particleX = x + xOffset;
            double particleZ = z + zOffset;
            if (!anextratouch$isSplashInsideFluid(e.worldObj, particleX, y, particleZ)) {
                e.worldObj.spawnParticle("splash", particleX, y, particleZ, e.motionX, e.motionY, e.motionZ);
            }
        }
    }

    @Unique
    private void anextratouch$playClientSplashSound(Entity e, double x, double y, double z) {
        float volume = MathHelper
            .sqrt_double(e.motionX * e.motionX * 0.2D + e.motionY * e.motionY + e.motionZ * e.motionZ * 0.2D) * 0.2F;
        volume = Math.max(0.2F, Math.min(1.0F, volume));
        float pitch = 1.0F + (e.worldObj.rand.nextFloat() - e.worldObj.rand.nextFloat()) * 0.4F;
        e.worldObj.playSound(x, y, z, "game.neutral.swim.splash", volume, pitch, false);
    }
}
