package org.fentanylsolutions.anextratouch.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SplashEntryTrackerTest {

    @Test
    public void loadingAlreadySubmergedDoesNotSplash() {
        SplashEntryTracker tracker = new SplashEntryTracker();
        for (int tick = 0; tick < 100; tick++) assertFalse(tracker.update(true));
    }

    @Test
    public void entrySplashesOnceAndSwimmingDoesNotRetrigger() {
        SplashEntryTracker tracker = armedTracker();
        assertTrue(tracker.update(true));
        for (int tick = 0; tick < 100; tick++) assertFalse(tracker.update(true));
    }

    @Test
    public void briefSurfaceBobbingDoesNotRearm() {
        SplashEntryTracker tracker = armedTracker();
        assertTrue(tracker.update(true));
        for (int bob = 0; bob < 30; bob++) {
            assertFalse(tracker.update(false));
            assertFalse(tracker.update(false));
            assertFalse(tracker.update(true));
        }
    }

    @Test
    public void leavingFluidAllowsANewEntry() {
        SplashEntryTracker tracker = armedTracker();
        assertTrue(tracker.update(true));
        for (int tick = 0; tick < 3; tick++) assertFalse(tracker.update(false));
        assertTrue(tracker.update(true));
        assertFalse(tracker.update(true));
    }

    private static SplashEntryTracker armedTracker() {
        SplashEntryTracker tracker = new SplashEntryTracker();
        for (int tick = 0; tick < 3; tick++) tracker.update(false);
        return tracker;
    }
}
