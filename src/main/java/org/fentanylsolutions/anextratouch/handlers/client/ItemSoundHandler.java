package org.fentanylsolutions.anextratouch.handlers.client;

import java.util.Iterator;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.fentanylsolutions.anextratouch.Config;
import org.fentanylsolutions.anextratouch.handlers.client.ItemSoundRegistry.Category;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client-side item accents driven by observed equipment and arm/use animations. */
public final class ItemSoundHandler {

    public static final ItemSoundHandler INSTANCE = new ItemSoundHandler();
    private final WeakHashMap<EntityLivingBase, Tracker> trackers = new WeakHashMap<>();
    private World world;
    private long tick;

    private ItemSoundHandler() {}

    /** Called only when Minecraft accepts a new swing, including animation restarts. */
    public static void onSwing(EntityLivingBase entity) {
        if (!Config.itemSwingSoundsEnabled || !entity.worldObj.isRemote || entity.isDead) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.isGamePaused() || entity.worldObj != mc.theWorld) return;
        if (entity != mc.thePlayer
            && (!Config.itemSoundsForOtherEntities || entity.getDistanceSqToEntity(mc.thePlayer) > 1024)) return;
        if (freeSwing(entity, mc)) playSwing(entity, ItemSoundRegistry.resolve(entity.getHeldItem()));
    }

    public void onConfigReload() {
        ItemSoundRegistry.reload(Config.itemSoundOverrides);
        reset();
    }

    private void reset() {
        for (Tracker tracker : trackers.values()) stopBow(tracker);
        trackers.clear();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.theWorld) {
            reset();
            world = mc.theWorld;
        }
        if (world == null || mc.thePlayer == null || mc.isGamePaused()) return;
        if (!Config.itemSwingSoundsEnabled && !Config.itemEquipSoundsEnabled && !Config.itemBowDrawSoundsEnabled)
            return;
        tick++;
        for (Object object : world.loadedEntityList) {
            if (!(object instanceof EntityLivingBase)) continue;
            EntityLivingBase entity = (EntityLivingBase) object;
            if (entity.isDead || (entity != mc.thePlayer
                && (!Config.itemSoundsForOtherEntities || entity.getDistanceSqToEntity(mc.thePlayer) > 1024))) {
                Tracker removed = trackers.remove(entity);
                if (removed != null) stopBow(removed);
                continue;
            }
            track(entity, mc);
        }
        // Unloaded entities may still be alive, and weak-reference collection is not immediate.
        Iterator<Tracker> iterator = trackers.values()
            .iterator();
        while (iterator.hasNext()) {
            Tracker tracker = iterator.next();
            if (tracker.lastTick != tick) {
                stopBow(tracker);
                iterator.remove();
            }
        }
    }

    private void track(EntityLivingBase entity, Minecraft mc) {
        ItemStack held = entity.getHeldItem();
        Item item = held == null ? null : held.getItem();
        int metadata = held == null || held.isItemStackDamageable() ? 0 : held.getItemDamage();
        int slot = entity == mc.thePlayer ? mc.thePlayer.inventory.currentItem : -1;
        ItemStack used = entity instanceof EntityPlayer ? ((EntityPlayer) entity).getItemInUse() : null;
        int useTicks = used == null ? 0 : ((EntityPlayer) entity).getItemInUseDuration();
        Category category = ItemSoundRegistry.resolve(held);
        boolean blocking = used != null && used.getItemUseAction() == EnumAction.block;
        boolean drawingBow = used != null && (ItemSoundRegistry.resolve(used) == Category.BOW
            || ItemSoundRegistry.resolve(used) == Category.CROSSBOW);
        Tracker tracker = trackers.get(entity);
        if (tracker != null) {
            if (entity instanceof EntityPlayer && Config.itemEquipSoundsEnabled
                && (tracker.item != item || tracker.metadata != metadata || tracker.slot != slot)) {
                playEquip(entity, held, category);
            }
            if (Config.itemSwingSoundsEnabled && blocking && !tracker.blocking && freeSwing(entity, mc)) {
                playSwing(entity, category);
            }
            if (!drawingBow || !Config.itemBowDrawSoundsEnabled) stopBow(tracker);
            if (drawingBow && Config.itemBowDrawSoundsEnabled
                && (!tracker.drawingBow || useTicks < tracker.useTicks || tracker.item != item)) {
                stopBow(tracker);
                tracker.bowSound = play(entity, "item.bow.pull", Config.itemBowDrawVolume, 0.9F, 1.1F);
            }
        } else {
            tracker = new Tracker();
            trackers.put(entity, tracker);
        }
        tracker.item = item;
        tracker.metadata = metadata;
        tracker.slot = slot;
        tracker.blocking = blocking;
        tracker.drawingBow = drawingBow;
        tracker.useTicks = useTicks;
        tracker.lastTick = tick;
    }

    private static boolean freeSwing(EntityLivingBase entity, Minecraft mc) {
        if (entity == mc.thePlayer && mc.objectMouseOver != null) {
            return mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK;
        }
        MovingObjectPosition hit = entity.rayTrace(3.0D, 1.0F);
        return hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private static void playSwing(EntityLivingBase entity, Category category) {
        String sound;
        float volume = 0.5F;
        float minPitch = 0.8F, maxPitch = 1.2F;
        switch (category) {
            case SWORD:
                sound = "item.sword.swing";
                break;
            case AXE:
            case CROSSBOW:
                sound = "item.blunt.swing";
                break;
            case TOOL:
            case BOW:
                sound = "item.tool.swing";
                break;
            case SHIELD:
                sound = "item.shield.swing";
                volume = 0.25F;
                minPitch = 0.4F;
                maxPitch = 0.6F;
                break;
            case POTION:
                sound = "item.potion.equip";
                volume = 1.0F;
                break;
            case BOOK:
                sound = "pageflipheavy";
                volume = 1.0F;
                break;
            default:
                return;
        }
        play(entity, sound, volume * Config.itemSwingVolume, minPitch, maxPitch);
    }

    private static void playEquip(EntityLivingBase entity, ItemStack stack, Category category) {
        String sound;
        float volume = 0.5F;
        switch (category) {
            case SWORD:
                sound = "item.sword.equip";
                break;
            case AXE:
                sound = "item.blunt.equip";
                break;
            case TOOL:
                sound = "item.tool.equip";
                break;
            case BOW:
            case CROSSBOW:
                sound = "item.bow.equip";
                break;
            case SHIELD:
                sound = "item.shield.equip";
                volume = 0.25F;
                break;
            case POTION:
                sound = "item.potion.equip";
                volume = 0.8F;
                break;
            case BOOK:
                sound = "pageflip";
                volume = 1.0F;
                break;
            case UTILITY:
                if (stack.getItem() instanceof ItemBlock) {
                    if (Config.itemBlockEquipSoundsEnabled) {
                        Block block = Block.getBlockFromItem(stack.getItem());
                        play(
                            entity,
                            new ResourceLocation(block.stepSound.getStepResourcePath()),
                            0.3F * Config.itemEquipVolume,
                            0.8F,
                            1.2F);
                    }
                    return;
                }
                if (!Config.itemUtilityEquipSoundsEnabled) return;
                sound = "item.utility.equip";
                volume = 0.3F;
                break;
            default:
                return;
        }
        play(entity, sound, volume * Config.itemEquipVolume, 0.8F, 1.2F);
    }

    private static ISound play(EntityLivingBase entity, String sound, float volume, float minPitch, float maxPitch) {
        return play(entity, new ResourceLocation("anextratouch", sound), volume, minPitch, maxPitch);
    }

    private static ISound play(EntityLivingBase entity, ResourceLocation sound, float volume, float minPitch,
        float maxPitch) {
        if (volume <= 0) return null;
        float pitch = minPitch + entity.getRNG()
            .nextFloat() * (maxPitch - minPitch);
        ISound instance = new PositionedSoundRecord(
            sound,
            volume,
            pitch,
            (float) entity.posX,
            (float) (entity.posY + entity.getEyeHeight()),
            (float) entity.posZ);
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(instance);
        return instance;
    }

    private static void stopBow(Tracker tracker) {
        if (tracker.bowSound != null) {
            Minecraft.getMinecraft()
                .getSoundHandler()
                .stopSound(tracker.bowSound);
            tracker.bowSound = null;
        }
    }

    private static final class Tracker {

        long lastTick;

        Item item;
        int metadata;
        int slot;
        boolean blocking;
        boolean drawingBow;
        int useTicks;
        ISound bowSound;
    }
}
