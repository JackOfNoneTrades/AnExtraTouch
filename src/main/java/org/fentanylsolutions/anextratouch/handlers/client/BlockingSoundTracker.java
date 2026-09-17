package org.fentanylsolutions.anextratouch.handlers.client;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Tracks raises without interpreting a stack synchronization as a new right-click. */
final class BlockingSoundTracker {

    private Item item;
    private int metadata;

    boolean update(ItemStack blocking, boolean useKeyHeld) {
        if (blocking == null) {
            // Inventory synchronization can clear itemInUse before the held use key restarts it.
            if (!useKeyHeld) item = null;
            return false;
        }
        Item nextItem = blocking.getItem();
        int nextMetadata = blocking.isItemStackDamageable() ? 0 : blocking.getItemDamage();
        boolean raised = item != nextItem || metadata != nextMetadata;
        item = nextItem;
        metadata = nextMetadata;
        return raised;
    }
}
