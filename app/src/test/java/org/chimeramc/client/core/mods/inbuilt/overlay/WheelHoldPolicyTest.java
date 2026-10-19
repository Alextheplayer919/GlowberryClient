package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The radial wheel's hold-to-open decision table. Pure rules, no mocks, no device.
 */
public class WheelHoldPolicyTest {

    @Test
    public void aHoldPressOnAClosedWheelOpensItAndRemembersTheGesture() {
        assertEquals(WheelHoldPolicy.OnPress.OPEN_HOLD,
                WheelHoldPolicy.onPress(true, false, false));
    }

    @Test
    public void aHoldPressOnAnAlreadyOpenWheelClosesIt() {
        // The wheel is open from an earlier gesture (or a launcher tap), so the bind must still be
        // able to dismiss it — otherwise it could only ever be closed by tapping outside.
        assertEquals(WheelHoldPolicy.OnPress.CLOSE,
                WheelHoldPolicy.onPress(true, true, true));
        assertEquals(WheelHoldPolicy.OnPress.CLOSE,
                WheelHoldPolicy.onPress(true, true, false));
    }

    @Test
    public void toggleModeStillOpensAndClosesOnSuccessivePresses() {
        assertEquals(WheelHoldPolicy.OnPress.OPEN_TOGGLE,
                WheelHoldPolicy.onPress(false, false, false));
        assertEquals(WheelHoldPolicy.OnPress.CLOSE,
                WheelHoldPolicy.onPress(false, true, false));
    }

    @Test
    public void aHoldReleaseWithASelectionConfirmsIt() {
        assertEquals(WheelHoldPolicy.OnRelease.CONFIRM,
                WheelHoldPolicy.onRelease(true, true, true, true));
    }

    @Test
    public void aHoldReleaseWithNothingHighlightedDismissesInsteadOfActivating() {
        // A release with a stationary cursor must not activate whatever happens to sit under it.
        assertEquals(WheelHoldPolicy.OnRelease.DISMISS,
                WheelHoldPolicy.onRelease(true, true, true, false));
    }

    @Test
    public void aReleaseThatDidNotOpenTheWheelIsIgnored() {
        // A wheel opened by a launcher tap must not be confirmed by an unrelated bind release.
        assertEquals(WheelHoldPolicy.OnRelease.IGNORE,
                WheelHoldPolicy.onRelease(true, true, false, true));
    }

    @Test
    public void aReleaseOnAClosedWheelIsIgnored() {
        assertEquals(WheelHoldPolicy.OnRelease.IGNORE,
                WheelHoldPolicy.onRelease(true, false, false, true));
    }

    @Test
    public void toggleModeIgnoresTheRelease() {
        // Toggle mode commits on the press of the next bind, not on the release of this one.
        assertEquals(WheelHoldPolicy.OnRelease.IGNORE,
                WheelHoldPolicy.onRelease(false, true, true, true));
    }
}
