package org.fentanylsolutions.anextratouch.handlers.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class BlockingSoundTrackerTest {

    @Test
    public void synchronizationDuringOneHeldPressDoesNotRaiseAgain() {
        BlockingSoundTracker tracker = new BlockingSoundTracker();
        ItemStack shield = new ItemStack(new Item().setMaxDamage(336));
        assertTrue(tracker.update(shield, true));

        // A server update replaces the stack and briefly interrupts item use.
        ItemStack synchronizedShield = shield.copy();
        synchronizedShield.setItemDamage(1);
        synchronizedShield.setTagCompound(new NBTTagCompound());
        synchronizedShield.getTagCompound()
            .setLong("cooldown_end", 1200L);
        for (int tick = 0; tick < 4; tick++) {
            assertFalse(tracker.update(null, true));
        }
        assertFalse(tracker.update(synchronizedShield, true));
        assertFalse(tracker.update(synchronizedShield, true));

        assertFalse(tracker.update(null, false));
        assertTrue(tracker.update(synchronizedShield, true));
    }

    @Test
    public void changingTheBlockingItemDuringHeldUseIsANewRaise() {
        BlockingSoundTracker tracker = new BlockingSoundTracker();
        ItemStack sword = new ItemStack(new Item());
        ItemStack shield = new ItemStack(new Item());
        assertTrue(tracker.update(sword, true));
        assertTrue(tracker.update(shield, true));
        assertFalse(tracker.update(shield, true));
    }

    @Test
    public void remotePlayersAndCrouchBlockingRearmWhenBlockingStops() {
        BlockingSoundTracker tracker = new BlockingSoundTracker();
        ItemStack shield = new ItemStack(new Item());
        assertTrue(tracker.update(shield, false));
        assertFalse(tracker.update(shield, false));
        assertFalse(tracker.update(null, false));
        assertTrue(tracker.update(shield, false));
    }

    @Test
    public void metadataVariantsAreDistinctRaises() {
        BlockingSoundTracker tracker = new BlockingSoundTracker();
        Item item = new Item().setHasSubtypes(true);
        assertTrue(tracker.update(new ItemStack(item, 1, 0), true));
        assertTrue(tracker.update(new ItemStack(item, 1, 1), true));
        assertFalse(tracker.update(new ItemStack(item, 1, 1), true));
    }
}
