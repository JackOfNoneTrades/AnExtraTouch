package org.fentanylsolutions.anextratouch.handlers.client;

import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

// When we play multiplayer and there is no An Extra Touch on it, we calculate armor step sounds on the client
// This has the disadvantage of being slightly delayed
// But if the server also has the mod, it sends packets when steps happen
public class StepSoundHandler {

    private static final float LAND_DISTANCE_MIN = 0.9f;

    private static class ArmorTracker {

        final ItemStack[] prevArmor = new ItemStack[4];
        double lastX, lastZ;
        float stepDistance;
        boolean wasOnGround = true;
        double peakY = Double.NaN;
        boolean initialized;
    }

    private final WeakHashMap<Entity, ArmorTracker> trackers = new WeakHashMap<>();

    // static mixin callbacks

    // Called from MixinEntity hooked into func_145780_a (inside moveEntity) on the client side
    // Only plays for the local player. Remote entities are handled by tick tracking or server packets
    public static void onEntityStep(Entity entity) {
        if (!entity.worldObj.isRemote) {
            return;
        }
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        if (entity != Minecraft.getMinecraft().thePlayer) {
            return;
        }

        playStepAccents((EntityLivingBase) entity);
    }

    // Called from MixinEntity at fall() hook on the client side.
    // Only plays for the local player. Remote entity landing is handled by tick tracking or server packets
    public static void onEntityLand(Entity entity, float distance) {
        if (!entity.worldObj.isRemote) {
            return;
        }
        if (distance < LAND_DISTANCE_MIN) {
            return;
        }
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        if (entity != Minecraft.getMinecraft().thePlayer) {
            return;
        }

        playStepAccents((EntityLivingBase) entity);
    }

    // packet callbacks

    public static void onServerArmorStep(int entityId) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }

        Entity entity = mc.theWorld.getEntityByID(entityId);
        if (entity == null) {
            return;
        }
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }

        playStepAccents((EntityLivingBase) entity);
    }

    // tick handler for equip sounds, and in case we operate client-side only

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) obj;

            // equip sounds: always, all entities (client can read equipment slots)
            if (Config.armorSoundsEnabled && AnExtraTouch.vic.armorSoundEntities.contains(living.getClass())) {
                ArmorTracker tracker = getOrCreateTracker(living);
                if (tracker.initialized) {
                    checkEquipSound(living, tracker);
                }
            }

            // remote entity step/land tracking: only when server doesn't have the mod
            if (!AnExtraTouch.vic.serverHasAET && living != mc.thePlayer) {
                boolean needsTracking = (Config.armorSoundsEnabled
                    && AnExtraTouch.vic.armorSoundEntities.contains(living.getClass()))
                    || (Config.rainSplashEnabled && AnExtraTouch.vic.rainSplashEntities.contains(living.getClass()));
                if (needsTracking) {
                    ArmorTracker tracker = getOrCreateTracker(living);
                    if (tracker.initialized) {
                        trackRemoteEntity(living, tracker);
                    }
                }
            }
        }
    }

    private ArmorTracker getOrCreateTracker(EntityLivingBase living) {
        ArmorTracker tracker = trackers.get(living);
        if (tracker == null) {
            tracker = new ArmorTracker();
            for (int i = 0; i < 4; i++) {
                ItemStack s = living.getEquipmentInSlot(i + 1);
                tracker.prevArmor[i] = s == null ? null : s.copy();
            }
            tracker.lastX = living.posX;
            tracker.lastZ = living.posZ;
            tracker.initialized = true;
            trackers.put(living, tracker);
        }
        return tracker;
    }

    private void trackRemoteEntity(EntityLivingBase living, ArmorTracker tracker) {
        // entity.onGround doesn't work for EntityOtherPlayerMP, so check the block below
        int bx = MathHelper.floor_double(living.posX);
        int by = MathHelper.floor_double(living.boundingBox.minY - 0.2);
        int bz = MathHelper.floor_double(living.posZ);
        boolean onSolidGround = living.worldObj.getBlock(bx, by, bz)
            .getMaterial()
            .isSolid();

        // Landing detection with peak height tracking
        if (!onSolidGround) {
            if (Double.isNaN(tracker.peakY) || living.posY > tracker.peakY) {
                tracker.peakY = living.posY;
            }
        } else if (!tracker.wasOnGround && !Double.isNaN(tracker.peakY)) {
            float fallDist = (float) (tracker.peakY - living.posY);
            if (fallDist > LAND_DISTANCE_MIN) {
                playStepAccents(living);
            }
            tracker.peakY = Double.NaN;
        }
        tracker.wasOnGround = onSolidGround;

        if (!onSolidGround) {
            tracker.lastX = living.posX;
            tracker.lastZ = living.posZ;
            return;
        }

        double dx = living.posX - tracker.lastX;
        double dz = living.posZ - tracker.lastZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        tracker.lastX = living.posX;
        tracker.lastZ = living.posZ;

        if (dist < 0.001f) return;

        // Accumulate dist * 0.6 (vanilla magic number), trigger every 1.0
        tracker.stepDistance += dist * 0.6f;
        if (tracker.stepDistance >= 1.0f) {
            tracker.stepDistance %= 1.0f;
            playStepAccents(living);
        }
    }

    // equip sound detection (basically check if armor itemstacks changed between previous and current tick)

    private void checkEquipSound(EntityLivingBase living, ArmorTracker tracker) {
        String bestCategory = null;
        int bestPriority = -1;

        for (int i = 0; i < 4; i++) {
            ItemStack curr = living.getEquipmentInSlot(i + 1);

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
            playEquipSound(living, bestCategory);
        }
    }

    private static boolean isSameArmorItem(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
            && ItemStack.areItemStackTagsEqual(a, b);
    }

    // sound playback methods

    private static void playStepAccents(EntityLivingBase living) {
        if (Config.armorSoundsEnabled && AnExtraTouch.vic.armorSoundEntities.contains(living.getClass())) {
            playArmorStepSounds(living);
        }
        if (Config.rainSplashEnabled) {
            playRainSplash(living);
        }
    }

    // plays a water sound at high pitch when walking in rain, from dynamic surroundings
    private static void playRainSplash(EntityLivingBase entity) {
        if (!AnExtraTouch.vic.rainSplashEntities.contains(entity.getClass())) {
            return;
        }
        if (!entity.isWet() || entity.isInWater()) {
            return;
        }

        float rainStrength = entity.worldObj.getRainStrength(0f);
        float volume = Config.rainSplashVolume * rainStrength;
        if (volume <= 0.001f) {
            return;
        }

        Minecraft.getMinecraft().theWorld.playSound(
            entity.posX,
            entity.posY - entity.yOffset,
            entity.posZ,
            AnExtraTouch.MODID + ":rain.splash",
            volume,
            1.75f,
            false);
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
            bodyCat = chestPri >= legsPri ? chestCat : legsCat;
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
        if (soundName == null) return;

        float pitch = 0.9f + entity.worldObj.rand.nextFloat() * 0.2f;
        Minecraft.getMinecraft().theWorld
            .playSound(entity.posX, entity.posY - entity.yOffset, entity.posZ, soundName, volume, pitch, false);
    }

    private static void playEquipSound(EntityLivingBase entity, String category) {
        String soundName = getSoundName(category, false, false);
        if (soundName == null) return;
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
