package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.footsteps.FootprintUtil;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class PlayerEffectHandler {

    private static final float LAND_DISTANCE_MIN = 0.9f;

    /**
     * Per-player tracking data for equip detection, step sounds, landing, and footprints.
     * The local player's step sounds and footprints are handled by MixinEntity.
     * EntityOtherPlayerMP doesn't call moveEntity (positions come from server packets),
     * so we track other players here.
     */
    private static class PlayerTracker {

        final ItemStack[] prevArmor = new ItemStack[4];
        double lastX, lastZ;
        float stepDistance;
        boolean wasOnGround = true;
        float lastFallDistance;
        double peakY = Double.NaN;
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
            for (int i = 0; i < 4; i++) {
                ItemStack s = player.getEquipmentInSlot(i + 1);
                tracker.prevArmor[i] = s == null ? null : s.copy();
            }
            tracker.lastX = player.posX;
            tracker.lastZ = player.posZ;
            tracker.initialized = true;
            trackers.put(player, tracker);
            return;
        }

        // detect equip sound
        if (Config.armorSoundsEnabled) {
            checkEquipSound(player, tracker);
        }

        // detect landing after fall
        if (player == Minecraft.getMinecraft().thePlayer) {
            boolean onGround = player.onGround;
            if (!onGround) {
                tracker.lastFallDistance = player.fallDistance;
            }
            if (!tracker.wasOnGround && onGround) {
                if (Config.armorSoundsEnabled && tracker.lastFallDistance > LAND_DISTANCE_MIN
                    && AnExtraTouch.vic.armorSoundEntities.contains(player.getClass())) {
                    playArmorStepSounds(player);
                }
                if (AnExtraTouch.vic.entityStrides.containsKey(player.getClass()) && player.ridingEntity == null) {
                    FootprintUtil.spawnFootprint(player, player.isChild(), true);
                    FootprintUtil.spawnFootprint(player, player.isChild(), false);
                }
                tracker.lastFallDistance = 0;
            }
            tracker.wasOnGround = onGround;
        }

        // step sounds + footprints for other players
        // Local player is handled by MixinEntity (moveEntity injection).
        // EntityOtherPlayerMP doesn't call moveEntity, so we track them here.
        if (player != Minecraft.getMinecraft().thePlayer) {
            trackOtherPlayer(player, tracker);
        }
    }

    // Basically compares previous armor set with the previous. If it's different,
    // it calculates the appropriate sound(s) to play
    private void checkEquipSound(EntityPlayer player, PlayerTracker tracker) {
        String bestCategory = null;
        int bestPriority = -1;

        for (int i = 0; i < 4; i++) {
            ItemStack curr = player.getEquipmentInSlot(i + 1);

            if (!isSameArmorItem(tracker.prevArmor[i], curr)) {
                ItemStack relevant = curr != null ? curr : tracker.prevArmor[i];
                if (relevant != null) {
                    String category = AnExtraTouch.vic.resolveArmorCategory(relevant);
                    int priority = AnExtraTouch.vic.getArmorPriority(category);
                    if (priority > bestPriority) {
                        bestPriority = priority;
                        bestCategory = category;
                    }
                }
                tracker.prevArmor[i] = curr == null ? null : curr.copy();
            }
        }

        if (bestCategory != null) {
            playEquipSound(player, bestCategory);
        }
    }

    private boolean isSameArmorItem(ItemStack a, ItemStack b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.getItem() == b.getItem();
    }

    private void trackOtherPlayer(EntityPlayer player, PlayerTracker tracker) {
        // player.onGround doesn't work for EntityOtherPlayerMP
        int bx = MathHelper.floor_double(player.posX);
        int by = MathHelper.floor_double(player.boundingBox.minY - 0.2);
        int bz = MathHelper.floor_double(player.posZ);
        boolean onSolidGround = player.worldObj.getBlock(bx, by, bz)
            .getMaterial()
            .isSolid();

        // landing detection with peak height tracking
        if (Config.armorSoundsEnabled && AnExtraTouch.vic.armorSoundEntities.contains(player.getClass())) {
            if (!onSolidGround) {
                if (Double.isNaN(tracker.peakY) || player.posY > tracker.peakY) {
                    tracker.peakY = player.posY;
                }
            } else if (!tracker.wasOnGround && !Double.isNaN(tracker.peakY)) {
                // we landed
                float fallDist = (float) (tracker.peakY - player.posY);
                if (fallDist > LAND_DISTANCE_MIN) {
                    playArmorStepSounds(player);
                }
                tracker.peakY = Double.NaN;
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

        // armor step sounds, accumulate dist * 0.6 (vanilla magic number), trigger every 1.0
        if (Config.armorSoundsEnabled && AnExtraTouch.vic.armorSoundEntities.contains(player.getClass())) {
            tracker.stepDistance += dist * 0.6f;
            if (tracker.stepDistance >= 1.0f) {
                tracker.stepDistance %= 1.0f;
                playArmorStepSounds(player);
            }
        }

        // footprints, trigger when entity moved by stride distance
        if (AnExtraTouch.vic.entityStrides.containsKey(player.getClass()) && player.ridingEntity == null) {
            tracker.footprintDistance += dist;
            boolean isBaby = player.isChild();
            float stride = isBaby ? AnExtraTouch.vic.babyEntityStrides.getFloat(player.getClass())
                : AnExtraTouch.vic.entityStrides.getFloat(player.getClass());
            if (tracker.footprintDistance >= stride) {
                tracker.footprintDistance %= stride;
                tracker.isRightFoot = !tracker.isRightFoot;
                FootprintUtil.spawnFootprint(player, isBaby, tracker.isRightFoot);
            }
        }
    }

    // Called from MixinEntity hooked into func_145780_a (inside moveEntity)
    // Handles local player and mobs on the client side
    public static void onEntityStep(Entity entity) {
        if (!Config.armorSoundsEnabled) {
            return;
        }
        if (!entity.worldObj.isRemote) {
            return;
        }
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }

        if (!AnExtraTouch.vic.armorSoundEntities.contains(entity.getClass())) {
            return;
        }

        playArmorStepSounds((EntityLivingBase) entity);
    }

    // Called from MixinEntity at fall() hook
    // Plays armor accent on landing for local player and mobs
    // (EntityOtherPlayerMP landing is handled by trackOtherPlayer())
    public static void onEntityLand(Entity entity, float distance) {
        if (!Config.armorSoundsEnabled) {
            return;
        }
        if (!entity.worldObj.isRemote) {
            return;
        }
        if (distance < LAND_DISTANCE_MIN) {
            return;
        }
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }

        if (!AnExtraTouch.vic.armorSoundEntities.contains(entity.getClass())) return;

        playArmorStepSounds((EntityLivingBase) entity);
    }

    private static void playArmorStepSounds(EntityLivingBase living) {
        ItemStack chest = living.getEquipmentInSlot(3);
        ItemStack legs = living.getEquipmentInSlot(2);
        ItemStack feet = living.getEquipmentInSlot(1);

        String chestCat = AnExtraTouch.vic.resolveArmorCategory(chest);
        String legsCat = AnExtraTouch.vic.resolveArmorCategory(legs);
        String feetCat = AnExtraTouch.vic.resolveArmorCategory(feet);

        boolean sprinting = living.isSprinting();
        float volume = Config.armorSoundVolume;

        if ("mixed".equals(Config.armorSoundMode)) {
            if (chestCat != null) {
                playSound(living, chestCat, sprinting, false, volume);
            }
            if (legsCat != null && !legsCat.equals(chestCat)) {
                playSound(living, legsCat, sprinting, false, volume);
            }
        } else {
            String bodyCat;
            int chestPri = AnExtraTouch.vic.getArmorPriority(chestCat);
            int legsPri = AnExtraTouch.vic.getArmorPriority(legsCat);
            if (chestPri >= legsPri) {
                bodyCat = chestCat;
            } else {
                bodyCat = legsCat;
            }
            if (bodyCat != null) {
                playSound(living, bodyCat, sprinting, false, volume);
            }
        }

        if (feetCat != null) {
            playSound(living, feetCat, sprinting, true, volume);
        }
    }

    private static void playSound(EntityLivingBase entity, String category, boolean sprinting, boolean foot,
        float volume) {
        String soundName = getSoundName(category, sprinting, foot);
        if (soundName == null) {
            return;
        }

        float pitch = 0.9f + entity.worldObj.rand.nextFloat() * 0.2f;
        Minecraft.getMinecraft().theWorld
            .playSound(entity.posX, entity.posY - entity.yOffset, entity.posZ, soundName, volume, pitch, false);
    }

    private static void playEquipSound(EntityLivingBase entity, String category) {
        String soundName = getSoundName(category, false, false);
        if (soundName == null) {
            return;
        }
        Minecraft.getMinecraft().theWorld
            .playSound(entity.posX, entity.posY - entity.yOffset, entity.posZ, soundName, 0.5f, 1.0f, false);
    }

    private static String getSoundName(String category, boolean sprinting, boolean foot) {
        if (foot) {
            switch (category) {
                case "light":
                    return AnExtraTouch.MODID + ":armor.light_walk";
                case "medium":
                    return AnExtraTouch.MODID + ":armor.medium_walk";
                case "heavy":
                    return AnExtraTouch.MODID + ":armor.heavy_foot";
                case "crystal":
                    return AnExtraTouch.MODID + ":armor.crystal_foot";
                default:
                    return null;
            }
        }
        if (sprinting) {
            switch (category) {
                case "light":
                    return AnExtraTouch.MODID + ":armor.light_walk";
                case "medium":
                    return AnExtraTouch.MODID + ":armor.medium_walk";
                case "heavy":
                    return AnExtraTouch.MODID + ":armor.heavy_run";
                case "crystal":
                    return AnExtraTouch.MODID + ":armor.crystal_run";
                default:
                    return null;
            }
        }
        switch (category) {
            case "light":
                return AnExtraTouch.MODID + ":armor.light_walk";
            case "medium":
                return AnExtraTouch.MODID + ":armor.medium_walk";
            case "heavy":
                return AnExtraTouch.MODID + ":armor.heavy_walk";
            case "crystal":
                return AnExtraTouch.MODID + ":armor.crystal_walk";
            default:
                return null;
        }
    }
}
