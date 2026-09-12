package org.fentanylsolutions.anextratouch.util;

/** Detects upward swimming strokes from travel since the last trough, independent of per-tick speed. */
public final class SwimBobTracker {

    private static final double MIN_BOB_TRAVEL = 0.025D;
    private static final double MAX_TICK_TRAVEL = 0.5D;

    private boolean rising;
    private double travelFromExtreme;
    private double peakRiseSpeed;

    /**
     * @return upward speed once per bob cycle, or zero when no new upward stroke begins
     */
    public double update(double verticalDelta) {
        if (!Double.isFinite(verticalDelta) || Math.abs(verticalDelta) > MAX_TICK_TRAVEL) {
            reset();
            return 0.0D;
        }

        if (rising) {
            // Rearm only after a real descent from the peak, not a single noisy downward sample.
            travelFromExtreme = Math.min(0.0D, travelFromExtreme + verticalDelta);
            if (travelFromExtreme <= -MIN_BOB_TRAVEL) {
                rising = false;
                travelFromExtreme = 0.0D;
                peakRiseSpeed = 0.0D;
            }
        } else {
            travelFromExtreme = Math.max(0.0D, travelFromExtreme + verticalDelta);
            peakRiseSpeed = travelFromExtreme == 0.0D ? 0.0D : Math.max(peakRiseSpeed, verticalDelta);
            if (travelFromExtreme >= MIN_BOB_TRAVEL) {
                rising = true;
                travelFromExtreme = 0.0D;
                return peakRiseSpeed;
            }
        }

        return 0.0D;
    }

    public void reset() {
        rising = false;
        travelFromExtreme = 0.0D;
        peakRiseSpeed = 0.0D;
    }
}
