package org.fentanylsolutions.anextratouch.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SwimBobTrackerTest {

    @Test
    public void holdingStillDoesNotCreatePeriodicWakes() {
        SwimBobTracker tracker = new SwimBobTracker();
        for (int tick = 0; tick < 100; tick++) {
            assertEquals(0.0D, tracker.update(0.0D), 0.0D);
        }
    }

    @Test
    public void upwardStrokeCreatesOneWakePerBob() {
        SwimBobTracker tracker = new SwimBobTracker();

        double firstWake = tracker.update(0.035D);
        assertTrue(firstWake > 0.0D);
        assertEquals(0.0D, tracker.update(0.06D), 0.0D);
        assertEquals(0.0D, tracker.update(0.025D), 0.0D);

        assertEquals(0.0D, tracker.update(-0.03D), 0.0D);
        double secondWake = tracker.update(0.05D);
        assertTrue(secondWake > firstWake);
    }

    @Test
    public void subThresholdJitterDoesNotCreateWakes() {
        SwimBobTracker tracker = new SwimBobTracker();
        double[] jitter = { 0.004D, -0.006D, 0.009D, -0.009D, 0.0D };
        for (int cycle = 0; cycle < 20; cycle++) {
            for (double delta : jitter) {
                assertEquals(0.0D, tracker.update(delta), 0.0D);
            }
        }
    }

    @Test
    public void resetAllowsANewUpwardStroke() {
        SwimBobTracker tracker = new SwimBobTracker();
        assertTrue(tracker.update(0.04D) > 0.0D);
        assertEquals(0.0D, tracker.update(0.04D), 0.0D);
        tracker.reset();
        assertTrue(tracker.update(0.04D) > 0.0D);
    }

    @Test
    public void slowBobsStillProduceOneImpulsePerRise() {
        SwimBobTracker tracker = new SwimBobTracker();
        int impulses = 0;
        for (int cycle = 0; cycle < 3; cycle++) {
            for (int tick = 0; tick < 10; tick++) {
                if (tracker.update(0.006D) > 0.0D) impulses++;
            }
            for (int tick = 0; tick < 10; tick++) {
                assertEquals(0.0D, tracker.update(-0.006D), 0.0D);
            }
        }
        assertEquals(3, impulses);
    }

    @Test
    public void briefPositionReversalDoesNotRetriggerTheSameStroke() {
        SwimBobTracker tracker = new SwimBobTracker();
        assertTrue(tracker.update(0.04D) > 0.0D);
        for (int tick = 0; tick < 20; tick++) {
            assertEquals(0.0D, tracker.update(-0.012D), 0.0D);
            assertEquals(0.0D, tracker.update(0.015D), 0.0D);
        }
        assertEquals(0.0D, tracker.update(-0.03D), 0.0D);
        assertTrue(tracker.update(0.03D) > 0.0D);
    }

    @Test
    public void pausesInTheStrokeDoNotGenerateOrRearmRipples() {
        SwimBobTracker tracker = new SwimBobTracker();
        assertTrue(tracker.update(0.04D) > 0.0D);
        for (int tick = 0; tick < 100; tick++) {
            assertEquals(0.0D, tracker.update(0.0D), 0.0D);
        }
        assertEquals(0.0D, tracker.update(0.04D), 0.0D);
    }

    @Test
    public void teleportAndInvalidMotionDoNotCreateImpulsesOrPoisonTheTracker() {
        SwimBobTracker tracker = new SwimBobTracker();
        assertEquals(0.0D, tracker.update(20.0D), 0.0D);
        assertEquals(0.0D, tracker.update(Double.NaN), 0.0D);
        assertEquals(0.0D, tracker.update(Double.POSITIVE_INFINITY), 0.0D);
        assertTrue(tracker.update(0.04D) > 0.0D);
    }
}
