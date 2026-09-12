package org.fentanylsolutions.anextratouch.handlers.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class StepSoundHandlerTest {

    @Test
    public void nullSlotsCompareAsExpected() {
        assertTrue(StepSoundHandler.isSameArmorItem(null, null));
        assertFalse(StepSoundHandler.isSameArmorItem(null, stack(new Item())));
        assertFalse(StepSoundHandler.isSameArmorItem(stack(new Item()), null));
    }

    @Test
    public void differentItemsAreDifferentEvenWithMatchingMetadata() {
        assertFalse(StepSoundHandler.isSameArmorItem(stack(new Item()), stack(new Item())));
    }

    @Test
    public void drainingChargeDoesNotRepeatedlyEquipArmor() {
        ItemStack current = new ItemStack(new Item().setMaxDamage(27));
        current.setTagCompound(new NBTTagCompound());
        current.getTagCompound()
            .setDouble("charge", 30000000.0D);
        ItemStack snapshot = current.copy();

        // Keep the saved equipment snapshot while the active engine drains energy each tick.
        for (int tick = 1; tick <= 100; tick++) {
            current.getTagCompound()
                .setDouble("charge", 30000000.0D - tick);
            assertFalse(ItemStack.areItemStackTagsEqual(snapshot, current));
            assertTrue(StepSoundHandler.isSameArmorItem(snapshot, current));
        }
    }

    @Test
    public void durabilityAndChargeBarUpdatesDoNotEquipArmor() {
        ItemStack current = new ItemStack(new Item().setMaxDamage(27));
        ItemStack snapshot = current.copy();
        current.setItemDamage(12);

        assertTrue(StepSoundHandler.isSameArmorItem(snapshot, current));
    }

    @Test
    public void modesAndOtherTagsDoNotEquipArmor() {
        Item item = new Item();
        ItemStack previous = new ItemStack(item, 1, 3);
        NBTTagCompound previousTags = new NBTTagCompound();
        previousTags.setString("mode", "safe");
        previousTags.setString("displayName", "Old name");
        previous.setTagCompound(previousTags);

        // The live equipment stack changes every tick; the tracker stores a copy.
        ItemStack snapshot = previous.copy();
        ItemStack current = new ItemStack(item, 1, 3);
        NBTTagCompound currentTags = new NBTTagCompound();
        currentTags.setString("mode", "active");
        currentTags.setString("displayName", "New name");
        currentTags.setString("arbitrary", "changed");
        current.setTagCompound(currentTags);

        assertTrue(StepSoundHandler.isSameArmorItem(snapshot, current));
    }

    @Test
    public void declaredMetadataSubtypesCompareMetadata() {
        Item item = new Item().setHasSubtypes(true);
        assertTrue(StepSoundHandler.isSameArmorItem(new ItemStack(item, 1, 4), new ItemStack(item, 1, 4)));
        assertFalse(StepSoundHandler.isSameArmorItem(new ItemStack(item, 1, 4), new ItemStack(item, 1, 5)));
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }
}
