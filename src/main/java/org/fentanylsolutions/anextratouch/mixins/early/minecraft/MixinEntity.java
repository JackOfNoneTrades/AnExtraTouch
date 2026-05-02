package org.fentanylsolutions.anextratouch.mixins.early.minecraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.footsteps.FootprintUtil;
import org.fentanylsolutions.anextratouch.handlers.client.StepSoundHandler;
import org.fentanylsolutions.anextratouch.handlers.client.effects.WaterSplashManager;
import org.fentanylsolutions.anextratouch.handlers.server.ServerArmorHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

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

    // End-of-previous-tick position. Used to interpolate the actual surface crossing point
    // for fast entities (arrows) where posX at trigger time is already a full tick past entry.
    @Unique
    private double anextratouch$prevTickX = Double.NaN;
    @Unique
    private double anextratouch$prevTickY = Double.NaN;
    @Unique
    private double anextratouch$prevTickZ = Double.NaN;

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
        if (anextratouch$splashCooldown > 0) return;
        if (anextratouch$isSplashBlacklisted(self)) return;

        double surfaceLevel = anextratouch$findWaterEntrySurface(self);
        if (Double.isNaN(surfaceLevel)) return;

        double maxAbsVy = 0.0;
        int n = Math.min(anextratouch$velYCount, 4);
        for (int i = 0; i < n; i++) {
            if (anextratouch$velY[i] > maxAbsVy) maxAbsVy = anextratouch$velY[i];
        }

        WaterSplashManager.INSTANCE
            .spawnEmitter(self.worldObj, self.posX, surfaceLevel, self.posZ, self.width, (float) maxAbsVy);
        anextratouch$splashCooldown = 10;
    }

    @Unique
    private static double anextratouch$findWaterEntrySurface(Entity entity) {
        AxisAlignedBB box = entity.boundingBox.expand(0.0D, -0.4000000059604645D, 0.0D)
            .contract(0.001D, 0.001D, 0.001D);

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
                    Block block = entity.worldObj.getBlock(x, y, z);
                    if (block.getMaterial() != Material.water) {
                        continue;
                    }
                    if (!WaterSplashManager.isSplashFluidAllowed(entity.worldObj, x, y, z)) {
                        continue;
                    }

                    double surface = y + 1.0D;
                    if (block instanceof BlockLiquid) {
                        surface -= BlockLiquid.getLiquidHeightPercent(entity.worldObj.getBlockMetadata(x, y, z));
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

    // True if any water block intersects the entity's bbox. Doesn't require the bbox to
    // straddle the surface (so items fully submerged after a single physics step still register).
    @Unique
    private static boolean anextratouch$isInWater(Entity e) {
        AxisAlignedBB box = e.boundingBox.expand(0.0D, -0.4D, 0.0D)
            .contract(0.001D, 0.001D, 0.001D);
        int minX = MathHelper.floor_double(box.minX);
        int maxX = MathHelper.floor_double(box.maxX + 1.0D);
        int minY = MathHelper.floor_double(box.minY);
        int maxY = MathHelper.floor_double(box.maxY + 1.0D);
        int minZ = MathHelper.floor_double(box.minZ);
        int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    if (e.worldObj.getBlock(x, y, z)
                        .getMaterial() == Material.water
                        && WaterSplashManager.isSplashFluidAllowed(e.worldObj, x, y, z)) return true;
                }
            }
        }
        return false;
    }

    // Find the water surface Y at or just above the entity's column. Used to position the
    // splash visual when the entity is already underwater and findWaterEntrySurface returns NaN.
    @Unique
    private static double anextratouch$findWaterSurfaceAbove(Entity e) {
        int x = MathHelper.floor_double(e.posX);
        int z = MathHelper.floor_double(e.posZ);
        int startY = MathHelper.floor_double(e.boundingBox.minY) - 2;
        for (int i = 0; i < 16; i++) {
            int y = startY + i;
            Block here = e.worldObj.getBlock(x, y, z);
            Block above = e.worldObj.getBlock(x, y + 1, z);
            if (here.getMaterial() == Material.water && WaterSplashManager.isSplashFluidAllowed(e.worldObj, x, y, z)
                && above.getMaterial() != Material.water) {
                int meta = e.worldObj.getBlockMetadata(x, y, z);
                if (here instanceof BlockLiquid) return (y + 1) - BlockLiquid.getLiquidHeightPercent(meta);
                return y + 1;
            }
        }
        return Double.NaN;
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

        boolean inWaterNow = anextratouch$isInWater(self);

        if (inWaterNow && !anextratouch$wasInWater
            && anextratouch$splashCooldown == 0
            && !anextratouch$isSplashBlacklisted(self)) {
            double surface = anextratouch$findWaterSurfaceAbove(self);
            if (!Double.isNaN(surface)) {
                // Interpolate the surface crossing point along the entity's last-tick path so
                // fast-moving arrows don't drop their splash a tick behind their visible position.
                double splashX = self.posX;
                double splashZ = self.posZ;
                if (!Double.isNaN(anextratouch$prevTickY)) {
                    double dy = self.posY - anextratouch$prevTickY;
                    if (Math.abs(dy) > 1.0e-4) {
                        double t = (surface - anextratouch$prevTickY) / dy;
                        if (t >= 0.0 && t <= 1.0) {
                            splashX = anextratouch$prevTickX + (self.posX - anextratouch$prevTickX) * t;
                            splashZ = anextratouch$prevTickZ + (self.posZ - anextratouch$prevTickZ) * t;
                        }
                    }
                }
                double maxAbsVy = 0.0;
                int n = Math.min(anextratouch$velYCount, 4);
                for (int i = 0; i < n; i++) {
                    if (anextratouch$velY[i] > maxAbsVy) maxAbsVy = anextratouch$velY[i];
                }
                WaterSplashManager.INSTANCE
                    .spawnEmitter(self.worldObj, splashX, surface, splashZ, self.width, (float) maxAbsVy);
                // Play vanilla splash sound. Items/XP orbs/etc skip this in their overridden
                // handleWaterMovement, so we add it here for parity with living-entity splashes.
                float volume = MathHelper.sqrt_double(
                    self.motionX * self.motionX * 0.2 + self.motionY * self.motionY + self.motionZ * self.motionZ * 0.2)
                    * 0.2F;
                // Floor so low-motion entries (items dropped from a chest) are still audible.
                volume = Math.max(0.2F, Math.min(1.0F, volume));
                float pitch = 1.0F + (self.worldObj.rand.nextFloat() - self.worldObj.rand.nextFloat()) * 0.4F;
                // World.playSoundAtEntity routes through worldAccesses.playSound which is a no-op on
                // RenderGlobal. Vanilla client sounds normally arrive via S29PacketSoundEffect ->
                // WorldClient.playSound(...). We're already on the client, so call that directly.
                self.worldObj.playSound(splashX, surface, splashZ, "game.neutral.swim.splash", volume, pitch, false);
                anextratouch$splashCooldown = 10;
            }
        }

        anextratouch$wasInWater = inWaterNow;
        anextratouch$prevTickX = self.posX;
        anextratouch$prevTickY = self.posY;
        anextratouch$prevTickZ = self.posZ;
    }
}
