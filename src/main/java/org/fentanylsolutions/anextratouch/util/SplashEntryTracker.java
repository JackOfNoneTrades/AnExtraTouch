package org.fentanylsolutions.anextratouch.util;

/** Requires a sustained exit before another entry splash, so surface swimming cannot retrigger it. */
public final class SplashEntryTracker {

    private static final int DRY_TICKS_TO_REARM = 3;
    private int dryTicks;

    public boolean isArmed() {
        return dryTicks >= DRY_TICKS_TO_REARM;
    }

    public boolean update(boolean touchingFluid) {
        if (!touchingFluid) {
            dryTicks = Math.min(DRY_TICKS_TO_REARM, dryTicks + 1);
            return false;
        }
        boolean entering = isArmed();
        dryTicks = 0;
        return entering;
    }
}
