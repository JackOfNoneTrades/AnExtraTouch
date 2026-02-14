package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.footsteps.FootprintUtil;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class PlayerEffectHandler {

    /**
     * Per-player tracking data for footprints.
     * The local player's footprints are handled by MixinEntity.
     * EntityOtherPlayerMP doesn't call moveEntity (positions come from server packets),
     * so we track other players here.
     */
    private static class PlayerTracker {

        double lastX, lastZ;
        boolean wasOnGround = true;
        float footprintDistance;
        boolean isRightFoot = true;
        boolean initialized;
    }

    private final WeakHashMap<EntityPlayer, PlayerTracker> trackers = new WeakHashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!event.player.worldObj.isRemote) {
            return;
        }

        EntityPlayer player = event.player;
        PlayerTracker tracker = trackers.get(player);

        // Init tracker on first tick
        if (tracker == null) {
            tracker = new PlayerTracker();
            tracker.lastX = player.posX;
            tracker.lastZ = player.posZ;
            tracker.initialized = true;
            trackers.put(player, tracker);
            return;
        }

        // detect landing for footprints (local player only)
        if (player == Minecraft.getMinecraft().thePlayer) {
            boolean onGround = player.onGround;
            if (!tracker.wasOnGround && onGround) {
                if (AnExtraTouch.vic.footprints.entityStrides.containsKey(player.getClass())
                    && player.ridingEntity == null) {
                    FootprintUtil.spawnFootprint(player, player.isChild(), true);
                    FootprintUtil.spawnFootprint(player, player.isChild(), false);
                }
            }
            tracker.wasOnGround = onGround;
        }

        // footprints for other players
        // Local player is handled by MixinEntity (moveEntity injection).
        // EntityOtherPlayerMP doesn't call moveEntity, so we track them here.
        if (player != Minecraft.getMinecraft().thePlayer) {
            trackOtherPlayer(player, tracker);
        }
    }

    private void trackOtherPlayer(EntityPlayer player, PlayerTracker tracker) {
        // player.onGround doesn't work for EntityOtherPlayerMP
        int bx = MathHelper.floor_double(player.posX);
        int by = MathHelper.floor_double(player.boundingBox.minY - 0.2);
        int bz = MathHelper.floor_double(player.posZ);
        boolean onSolidGround = player.worldObj.getBlock(bx, by, bz)
            .getMaterial()
            .isSolid();

        // landing detection for footprints
        if (!tracker.wasOnGround && onSolidGround) {
            if (AnExtraTouch.vic.footprints.entityStrides.containsKey(player.getClass())
                && player.ridingEntity == null) {
                FootprintUtil.spawnFootprint(player, player.isChild(), true);
                FootprintUtil.spawnFootprint(player, player.isChild(), false);
            }
        }
        tracker.wasOnGround = onSolidGround;

        if (!onSolidGround) {
            tracker.lastX = player.posX;
            tracker.lastZ = player.posZ;
            return;
        }

        double dx = player.posX - tracker.lastX;
        double dz = player.posZ - tracker.lastZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        tracker.lastX = player.posX;
        tracker.lastZ = player.posZ;

        if (dist < 0.001f) {
            return;
        }

        // footprints, trigger when entity moved by stride distance
        if (AnExtraTouch.vic.footprints.entityStrides.containsKey(player.getClass()) && player.ridingEntity == null) {
            tracker.footprintDistance += dist;
            boolean isBaby = player.isChild();
            float stride = isBaby ? AnExtraTouch.vic.footprints.babyEntityStrides.getFloat(player.getClass())
                : AnExtraTouch.vic.footprints.entityStrides.getFloat(player.getClass());
            if (tracker.footprintDistance >= stride) {
                tracker.footprintDistance %= stride;
                tracker.isRightFoot = !tracker.isRightFoot;
                FootprintUtil.spawnFootprint(player, isBaby, tracker.isRightFoot);
            }
        }
    }
}
