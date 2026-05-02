package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.footsteps.FootprintUtil;
import org.fentanylsolutions.anextratouch.handlers.client.StepSoundHandler;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterSplashManager;
import org.fentanylsolutions.anextratouch.handlers.server.ServerArmorHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

    @Shadow
    protected Random rand;

    // Track position for actual distance calculation
    @Unique
    private double anextratouch$lastPosX = Double.NaN;
    @Unique
    private double anextratouch$lastPosZ = Double.NaN;

    // Track distance for footprints separately from step sounds
    @Unique
    private float anextratouch$footprintDistance = 0.0f;

    @Unique
    private boolean anextratouch$isRightFoot = true;

    // Track last 4 abs(motionY) values, peak is used as splash speed
    @Unique
    private final double[] anextratouch$velY = new double[4];
    @Unique
    private int anextratouch$velYCount;

    // Cooldown to suppress repeat splashes from rapidly bouncing entities (e.g. slimes in shallow water)
    @Unique
    private int anextratouch$splashCooldown;

    // Tracks whether this entity was in water on the previous tick, for the generic
    // transition-detection trigger that catches entities (items, XP orbs, ...) whose
    // handleWaterMovement override skips the vanilla playSound path.
    @Unique
    private boolean anextratouch$wasInWater;

    // Hook into call site of func_145780_a (step sound method) inside moveEntity, because we'd be missing overrides
    // that are noops
    @Inject(
        method = "moveEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;func_145780_a(IIILnet/minecraft/block/Block;)V"))
    private void onPlayStepSound(double dx, double dy, double dz, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.worldObj.isRemote) {
            StepSoundHandler.onEntityStep(self);
        } else {
            ServerArmorHandler.onEntityStep(self);
        }
    }

    // Hook into call site of fall() inside updateFallState (fall() is not called directly in moveEntity)
    @Inject(method = "updateFallState", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;fall(F)V"))
    private void onFall(double distanceFallenThisTick, boolean isOnGround, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.worldObj.isRemote) {
            StepSoundHandler.onEntityLand(self, self.fallDistance);
        } else {
            ServerArmorHandler.onEntityLand(self, self.fallDistance);
        }
    }

    // tracking actual position changes.
    @Inject(method = "moveEntity", at = @At("RETURN"))
    private void onMoveEntityReturn(double dx, double dy, double dz, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        if (!self.worldObj.isRemote) {
            return;
        }
        if (!(self instanceof EntityLivingBase)) {
            return;
        }
        if (!AnExtraTouch.vic.footprints.entityStrides.containsKey(self.getClass())) {
            return;
        }
        if (self.ridingEntity != null) {
            return;
        }

        // init last position on first call
        if (Double.isNaN(anextratouch$lastPosX)) {
            anextratouch$lastPosX = self.posX;
            anextratouch$lastPosZ = self.posZ;
            return;
        }

        if (!self.onGround) {
            anextratouch$lastPosX = self.posX;
            anextratouch$lastPosZ = self.posZ;
            return;
        }

        // Calculate actual horizontal distance moved this tick
        double deltaX = self.posX - anextratouch$lastPosX;
        double deltaZ = self.posZ - anextratouch$lastPosZ;
        float distMoved = (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        anextratouch$lastPosX = self.posX;
        anextratouch$lastPosZ = self.posZ;

        if (distMoved < 0.001f) {
            return;
        }

        anextratouch$footprintDistance += distMoved;

        // Check if we've walked far enough for a footprint
        boolean isBaby = ((EntityLivingBase) self).isChild();
        float stride = isBaby ? AnExtraTouch.vic.footprints.babyEntityStrides.getFloat(self.getClass())
            : AnExtraTouch.vic.footprints.entityStrides.getFloat(self.getClass());

        if (anextratouch$footprintDistance >= stride) {
            anextratouch$footprintDistance %= stride;
            anextratouch$isRightFoot = !anextratouch$isRightFoot;
            FootprintUtil.spawnFootprint(self, isBaby, anextratouch$isRightFoot);
        }
    }

    // Track Y velocity history for splash speed calculation
    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void anextratouch$trackVelocityY(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.worldObj.isRemote) return;
        if (!Config.waterSplashEnabled) return;
        anextratouch$velY[anextratouch$velYCount % 4] = Math.abs(self.motionY);
        anextratouch$velYCount++;
        if (anextratouch$splashCooldown > 0) anextratouch$splashCooldown--;
    }

    // Spawn splash emitter when an entity first enters water. Piggybacks on the
    // exact spot vanilla plays the splash sound so timing matches the vanilla splash.
    @Inject(
        method = "handleWaterMovement",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;playSound(Ljava/lang/String;FF)V"))
    private void anextratouch$onWaterEntry(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.worldObj.isRemote) return;
        if (!Config.waterSplashEnabled) return;
        if (self instanceof EntityArrow) return;
        if (anextratouch$splashCooldown > 0) return;
        if (anextratouch$isSplashBlacklisted(self)) return;

        double surfaceLevel = anextratouch$resolveSplashY(self);
        if (anextratouch$isSplashInsideFluid(self.worldObj, self.posX, surfaceLevel, self.posZ)) {
            anextratouch$splashCooldown = 10;
            return;
        }

        WaterSplashManager.INSTANCE.spawnEmitter(
            self.worldObj,
            self.posX,
            surfaceLevel,
            self.posZ,
            self.width,
            anextratouch$getMaxSplashSpeed());
        anextratouch$splashCooldown = 10;
    }

    @Unique
    private static double anextratouch$findWaterEntrySurface(Entity entity) {
        AxisAlignedBB box = anextratouch$getWaterProbeBox(entity);

        int minX = MathHelper.floor_double(box.minX);
        int maxX = MathHelper.floor_double(box.maxX + 1.0D);
        int minY = MathHelper.floor_double(box.minY);
        int maxY = MathHelper.floor_double(box.maxY + 1.0D);
        int minZ = MathHelper.floor_double(box.minZ);
        int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);

        double bestSurface = Double.NaN;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    if (!WaterSplashManager.intersectsSplashFluid(box, entity.worldObj, x, y, z)) {
                        continue;
                    }

                    double surface = WaterSplashManager.getSplashFluidSurfaceY(entity.worldObj, x, y, z);
                    if (surface < 0.0D) {
                        continue;
                    }

                    if (WaterSplashManager.isSplashFluidAllowed(entity.worldObj, x, y + 1, z)) {
                        continue;
                    }

                    if (box.maxY >= surface && (Double.isNaN(bestSurface) || surface > bestSurface)) {
                        bestSurface = surface;
                    }
                }
            }
        }

        return bestSurface;
    }

    @Unique
    private static boolean anextratouch$isSplashBlacklisted(Entity e) {
        if (AnExtraTouch.vic == null || AnExtraTouch.vic.waterSplashEntityBlacklist == null) return false;
        return AnExtraTouch.vic.waterSplashEntityBlacklist.contains(
            e.getClass()
                .getSimpleName());
    }

    // Mirrors vanilla's handleMaterialAcceleration block scan, but against splash-capable Forge fluids too.
    @Unique
    private static boolean anextratouch$isInWater(Entity e) {
        AxisAlignedBB box = anextratouch$getWaterProbeBox(e);
        int minX = MathHelper.floor_double(box.minX);
        int maxX = MathHelper.floor_double(box.maxX + 1.0D);
        int minY = MathHelper.floor_double(box.minY);
        int maxY = MathHelper.floor_double(box.maxY + 1.0D);
        int minZ = MathHelper.floor_double(box.minZ);
        int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    double surface = WaterSplashManager.getSplashFluidSurfaceY(e.worldObj, x, y, z);
                    if (surface >= 0.0D && (double) maxY >= surface) return true;
                }
            }
        }
        return false;
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
    private static AxisAlignedBB anextratouch$getWaterProbeBox(Entity e) {
        return e.boundingBox.expand(0.0D, -0.4000000059604645D, 0.0D)
            .contract(0.001D, 0.001D, 0.001D);
    }

    @Unique
    private static double anextratouch$getVanillaSplashY(Entity e) {
        return MathHelper.floor_double(e.boundingBox.minY) + 1.0D;
    }

    @Unique
    private static double anextratouch$resolveSplashY(Entity e) {
        double surface = anextratouch$findWaterEntrySurface(e);
        return Double.isNaN(surface) ? anextratouch$getVanillaSplashY(e) : surface;
    }

    @Unique
    private float anextratouch$getMaxSplashSpeed() {
        double maxAbsVy = 0.0D;
        int n = Math.min(anextratouch$velYCount, 4);
        for (int i = 0; i < n; i++) {
            if (anextratouch$velY[i] > maxAbsVy) maxAbsVy = anextratouch$velY[i];
        }
        return (float) maxAbsVy;
    }

    @Unique
    private void anextratouch$spawnVanillaSplashParticles(Entity e, double y) {
        int count = (int) (1.0F + e.width * 20.0F);
        for (int i = 0; i < count; i++) {
            float xOffset = (this.rand.nextFloat() * 2.0F - 1.0F) * e.width;
            float zOffset = (this.rand.nextFloat() * 2.0F - 1.0F) * e.width;
            e.worldObj.spawnParticle(
                "bubble",
                e.posX + xOffset,
                y,
                e.posZ + zOffset,
                e.motionX,
                e.motionY - this.rand.nextFloat() * 0.2F,
                e.motionZ);
        }

        boolean centerInsideFluid = anextratouch$isSplashInsideFluid(e.worldObj, e.posX, y, e.posZ);
        for (int i = 0; i < count; i++) {
            float xOffset = (this.rand.nextFloat() * 2.0F - 1.0F) * e.width;
            float zOffset = (this.rand.nextFloat() * 2.0F - 1.0F) * e.width;
            double x = e.posX + xOffset;
            double z = e.posZ + zOffset;
            if (!centerInsideFluid && !anextratouch$isSplashInsideFluid(e.worldObj, x, y, z)) {
                e.worldObj.spawnParticle("splash", x, y, z, e.motionX, e.motionY, e.motionZ);
            }
        }
    }

    @Unique
    private void anextratouch$playClientSplashSound(Entity e, double x, double y, double z) {
        float volume = MathHelper
            .sqrt_double(e.motionX * e.motionX * 0.2D + e.motionY * e.motionY + e.motionZ * e.motionZ * 0.2D) * 0.2F;
        // Floor so low-motion entries (items dropped from a chest) are still audible.
        volume = Math.max(0.2F, Math.min(1.0F, volume));
        float pitch = 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F;
        // World.playSoundAtEntity routes through worldAccesses.playSound which is a no-op on
        // RenderGlobal. Vanilla client sounds normally arrive via S29PacketSoundEffect ->
        // WorldClient.playSound(...). We're already on the client, so call that directly.
        e.worldObj.playSound(x, y, z, "game.neutral.swim.splash", volume, pitch, false);
    }

    @Redirect(
        method = "handleWaterMovement",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Ljava/lang/String;DDDDDD)V"))
    private void anextratouch$skipSubmergedVanillaSplash(World world, String particleName, double x, double y, double z,
        double motionX, double motionY, double motionZ) {
        if (Config.waterSplashEnabled && "splash".equals(particleName)) {
            Entity self = (Entity) (Object) this;
            if (self instanceof EntityArrow || anextratouch$isSplashInsideFluid(world, self.posX, y, self.posZ)
                || anextratouch$isSplashInsideFluid(world, x, y, z)) {
                return;
            }
        }

        world.spawnParticle(particleName, x, y, z, motionX, motionY, motionZ);
    }

    // Generic water-entry detector for entities whose handleWaterMovement override skips the
    // vanilla playSound path (notably EntityItem, EntityXPOrb). Compares this tick's water-presence
    // against last tick and fires a splash on false->true. The shared cooldown prevents
    // double-fires when the playSound injection above already handled the entry.
    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void anextratouch$detectWaterEntry(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.worldObj.isRemote) return;
        if (!Config.waterSplashEnabled) return;
        if (self instanceof EntityArrow) return;

        boolean inWaterNow = anextratouch$isInWater(self);

        if (inWaterNow && !anextratouch$wasInWater
            && anextratouch$splashCooldown == 0
            && !anextratouch$isSplashBlacklisted(self)) {
            double splashY = anextratouch$resolveSplashY(self);
            boolean centerInsideFluid = anextratouch$isSplashInsideFluid(self.worldObj, self.posX, splashY, self.posZ);
            if (!centerInsideFluid) {
                WaterSplashManager.INSTANCE.spawnEmitter(
                    self.worldObj,
                    self.posX,
                    splashY,
                    self.posZ,
                    self.width,
                    anextratouch$getMaxSplashSpeed());
                anextratouch$playClientSplashSound(self, self.posX, splashY, self.posZ);
            }
            anextratouch$spawnVanillaSplashParticles(self, anextratouch$getVanillaSplashY(self));
            anextratouch$splashCooldown = 10;
        }

        anextratouch$wasInWater = inWaterNow;
    }
}
