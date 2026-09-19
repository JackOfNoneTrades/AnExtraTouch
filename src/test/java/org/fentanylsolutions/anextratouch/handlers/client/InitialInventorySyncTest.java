package org.fentanylsolutions.anextratouch.handlers.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class InitialInventorySyncTest {

    @Test
    public void restoredInventoryIsSilentButSubsequentInventoryUpdatesAreNot() {
        Object player = new Object();
        InitialInventorySync sync = new InitialInventorySync();
        sync.begin(player);
        assertTrue(sync.pending(player)); // Empty inventory in the first client tick.
        assertTrue(sync.received(player));
        assertFalse(sync.pending(player)); // Real item switches can now sound normally.
        assertFalse(sync.received(player)); // Ordinary inventory updates must not reset the baseline.
    }

    @Test
    public void anotherEntityOrAnotherRespawnCannotCompleteThisPlayersRestore() {
        Object first = new Object(), second = new Object();
        InitialInventorySync sync = new InitialInventorySync();
        sync.begin(first);
        assertFalse(sync.pending(second));
        assertFalse(sync.received(second));
        assertTrue(sync.received(first));
        sync.begin(second);
        assertFalse(sync.received(first));
        assertTrue(sync.pending(second));
        assertTrue(sync.received(second));
        assertFalse(sync.pending(second));
    }

    @Test
    public void disconnectClearsPendingRestore() {
        Object player = new Object();
        InitialInventorySync sync = new InitialInventorySync();
        sync.begin(player);
        sync.begin(null);
        assertFalse(sync.pending(player));
        assertFalse(sync.pending(null));
        assertFalse(sync.received(player));
    }
}
