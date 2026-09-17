package org.fentanylsolutions.anextratouch.handlers.client;

import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.fentanylsolutions.anextratouch.AnExtraTouch;
import org.fentanylsolutions.anextratouch.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Detects armor equip and unequip changes on the client. */
public class StepSoundHandler {

    private static class ArmorTracker {

        final ItemStack[] prevArmor = new ItemStack[4];
    }

    private final WeakHashMap<EntityLivingBase, ArmorTracker> trackers = new WeakHashMap<>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Config.armorSoundsEnabled) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityLivingBase)) continue;

            EntityLivingBase living = (EntityLivingBase) obj;
            if (!AnExtraTouch.vic.armor.armorSoundEntities.contains(living.getClass())) continue;

            ArmorTracker tracker = trackers.get(living);
            if (tracker == null) {
                tracker = createTracker(living);
                trackers.put(living, tracker);
            } else {
                checkEquipSound(living, tracker);
            }
        }
    }

    private static ArmorTracker createTracker(EntityLivingBase living) {
        ArmorTracker tracker = new ArmorTracker();
        for (int i = 0; i < 4; i++) {
            ItemStack stack = living.getEquipmentInSlot(i + 1);
            tracker.prevArmor[i] = stack == null ? null : stack.copy();
        }
        return tracker;
    }

    private static void checkEquipSound(EntityLivingBase living, ArmorTracker tracker) {
        String bestCategory = null;
        int bestPriority = -1;

        for (int i = 0; i < 4; i++) {
            ItemStack current = living.getEquipmentInSlot(i + 1);
            if (isSameArmorItem(tracker.prevArmor[i], current)) continue;

            ItemStack relevant = current != null ? current : tracker.prevArmor[i];
            if (relevant != null) {
                String category = AnExtraTouch.vic.armor.resolveArmorCategory(relevant);
                int priority = AnExtraTouch.vic.armor.getArmorPriority(category);
                if (priority > bestPriority) {
                    bestPriority = priority;
                    bestCategory = category;
                }
            }
            tracker.prevArmor[i] = current == null ? null : current.copy();
        }

        if (bestCategory != null) {
            playEquipSound(living, bestCategory);
        }
    }

    static boolean isSameArmorItem(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem()) return false;

        // Charge, modes and other NBT can change every tick without re-equipping the item.
        // Damage is only part of the identity when the item declares metadata subtypes.
        return !a.getHasSubtypes() || a.getItemDamage() == b.getItemDamage();
    }

    private static void playEquipSound(EntityLivingBase entity, String category) {
        String soundName = getEquipSoundName(category);
        if (soundName == null) return;

        Minecraft.getMinecraft().theWorld.playSound(
            entity.posX,
            entity.posY - entity.yOffset,
            entity.posZ,
            soundName,
            Config.armorSoundVolume,
            1.0f,
            false);
    }

    private static String getEquipSoundName(String category) {
        switch (category) {
            case "light":
                return AnExtraTouch.MODID + ":armor.light_walk";
            case "medium":
                return AnExtraTouch.MODID + ":armor.medium_walk";
            case "heavy":
                return AnExtraTouch.MODID + ":armor.heavy_walk";
            case "crystal":
                return AnExtraTouch.MODID + ":armor.crystal_walk";
            case "elytra":
                return "minecraft_1.21.10:item.armor.equip_elytra";
            default:
                return null;
        }
    }
}
