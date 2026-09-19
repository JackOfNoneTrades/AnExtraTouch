package org.fentanylsolutions.anextratouch.handlers.client;

/** Only the first inventory snapshot after player recreation establishes a silent baseline. */
final class InitialInventorySync {

    private Object player;

    void begin(Object player) {
        this.player = player;
    }

    boolean pending(Object player) {
        return player != null && this.player == player;
    }

    boolean received(Object player) {
        if (!pending(player)) return false;
        this.player = null;
        return true;
    }
}
