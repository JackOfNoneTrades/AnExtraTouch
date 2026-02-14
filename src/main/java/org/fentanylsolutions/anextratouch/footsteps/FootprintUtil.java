package org.fentanylsolutions.anextratouch.footsteps;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

public class FootprintUtil {

    /**
     * Spawns a footprint particle for the given entity.
     *
     * @param entity      the entity to spawn a footprint for
     * @param isBaby      whether the entity is a baby
     * @param isRightFoot whether this footprint is for the right foot
     */
    public static void spawnFootprint(Entity entity, boolean isBaby, boolean isRightFoot) {
        if (!Config.footprintsEnabled) {
            return;
        }
        Class<? extends Entity> clazz = entity.getClass();

        float yawRad = (float) Math.toRadians(entity.rotationYaw);
        float rightX = -MathHelper.cos(yawRad);
        float rightZ = -MathHelper.sin(yawRad);

        float offsetDist = isBaby ? AnExtraTouch.vic.footprints.babyEntityStanceWidths.getFloat(clazz)
            : AnExtraTouch.vic.footprints.entityStanceWidths.getFloat(clazz);

        double footX, footZ;
        if (isRightFoot) {
            footX = entity.posX + rightX * offsetDist;
            footZ = entity.posZ + rightZ * offsetDist;
        } else {
            footX = entity.posX - rightX * offsetDist;
            footZ = entity.posZ - rightZ * offsetDist;
        }

        int bx = MathHelper.floor_double(footX);
        int by = MathHelper.floor_double(entity.boundingBox.minY);
        int bz = MathHelper.floor_double(footZ);
        Block block = entity.worldObj.getBlock(bx, by, bz);

        // Non-solid blocks shorter than half a block (like snow layers) are valid surfaces;
        // taller ones (like grass) are pass-through, so fall to the block below
        if (!block.getMaterial()
            .isSolid()) {
            block.setBlockBoundsBasedOnState(entity.worldObj, bx, by, bz);
            if (block.getBlockBoundsMaxY() - block.getBlockBoundsMinY() > 0.5) {
                by--;
                block = entity.worldObj.getBlock(bx, by, bz);
            }
        }

        if (!block.getMaterial()
            .isSolid()) {
            block.setBlockBoundsBasedOnState(entity.worldObj, bx, by, bz);
            if (block.getBlockBoundsMaxY() - block.getBlockBoundsMinY() > 0.5) {
                return;
            }
        }
        if (!AnExtraTouch.vic.footprints.hasFootprint(block)) {
            return;
        }

        double minX = block.getBlockBoundsMinX();
        double minY = block.getBlockBoundsMinY();
        double minZ = block.getBlockBoundsMinZ();
        double maxX = block.getBlockBoundsMaxX();
        double maxY = block.getBlockBoundsMaxY();
        double maxZ = block.getBlockBoundsMaxZ();

        block.setBlockBoundsBasedOnState(entity.worldObj, bx, by, bz);
        double footY = by + block.getBlockBoundsMaxY();
        block.setBlockBounds((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ);

        float footSize = isBaby ? AnExtraTouch.vic.footprints.babyEntityFootSizes.getFloat(clazz)
            : AnExtraTouch.vic.footprints.entityFootSizes.getFloat(clazz);
        int lifespan = AnExtraTouch.vic.footprints.getLifespan(block);
        float opacity = AnExtraTouch.vic.footprints.getOpacity(block);

        FootprintManager.INSTANCE.addFootprint(
            entity.worldObj,
            footX,
            footY,
            footZ,
            entity.rotationYaw,
            FootprintStyle.LOWRES_SQUARE,
            isRightFoot,
            footSize,
            lifespan,
            opacity);
    }
}
