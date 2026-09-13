package org.fentanylsolutions.anextratouch.util;

/** Small, bounded land-mask classifier used to reject isolated water-edge pillars. */
public final class CoastalLandmass {

    private static final int CENTER_BIT = 1 << 12;
    private static final int THREE_BY_THREE = 7 | (7 << 5) | (7 << 10);

    private CoastalLandmass() {}

    /**
     * Returns whether the centered land column belongs to a substantial connected landmass.
     * Bits 0 through 24 represent rows of a 5 by 5 mask, with bit 12 at the center.
     */
    public static boolean isCoast(int landMask) {
        int mask = landMask & ((1 << 25) - 1);
        if ((mask & CENTER_BIT) == 0) return false;

        // Every 3 by 3 window in a 5 by 5 mask contains bit 12, so a full window is
        // cardinally connected to the centered column without a separate flood fill.
        return (mask & THREE_BY_THREE) == THREE_BY_THREE || (mask & (THREE_BY_THREE << 1)) == (THREE_BY_THREE << 1)
            || (mask & (THREE_BY_THREE << 2)) == (THREE_BY_THREE << 2)
            || (mask & (THREE_BY_THREE << 5)) == (THREE_BY_THREE << 5)
            || (mask & (THREE_BY_THREE << 6)) == (THREE_BY_THREE << 6)
            || (mask & (THREE_BY_THREE << 7)) == (THREE_BY_THREE << 7)
            || (mask & (THREE_BY_THREE << 10)) == (THREE_BY_THREE << 10)
            || (mask & (THREE_BY_THREE << 11)) == (THREE_BY_THREE << 11)
            || (mask & (THREE_BY_THREE << 12)) == (THREE_BY_THREE << 12);
    }
}
