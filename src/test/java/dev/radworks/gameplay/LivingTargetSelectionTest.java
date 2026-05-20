package dev.radworks.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LivingTargetSelectionTest {
    @Test
    void skipsArmorStandByDefault() {
        assertEquals(
                "target_skipped",
                LivingTargetSelectionPolicy.skipReason(false, true, true, false));
    }

    @Test
    void skipsAllMobsWhenMobToggleDisabled() {
        assertEquals(
                "target_skipped",
                LivingTargetSelectionPolicy.skipReason(false, false, false, false));
    }

    @Test
    void allowsRegularLivingTargetWhenEnabled() {
        assertNull(LivingTargetSelectionPolicy.skipReason(false, false, true, false));
    }

    @Test
    void enforcesLivingTargetCap() {
        assertTrue(LivingTargetSelectionPolicy.isCapped(32, 32));
    }
}
