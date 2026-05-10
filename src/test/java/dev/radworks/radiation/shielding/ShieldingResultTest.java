package dev.radworks.radiation.shielding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShieldingResultTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void reducedWithSingleHitHalvesContribution() {
        ShieldingResult result = ShieldingResult.reduced(5.0D, 1);
        assertEquals("reduced", result.shielding());
        assertEquals(1, result.shieldingBlocksHit());
        assertEquals(0.5D, result.shieldingMultiplier(), EPSILON);
        assertEquals(2.5D, result.finalContribution(), EPSILON);
        assertEquals(2.5D, result.shieldingReduction(), EPSILON);
    }

    @Test
    void reducedUsesMinimumMultiplierCap() {
        ShieldingResult result = ShieldingResult.reduced(10.0D, 8);
        assertEquals("reduced", result.shielding());
        assertEquals(8, result.shieldingBlocksHit());
        assertEquals(0.1D, result.shieldingMultiplier(), EPSILON);
        assertEquals(1.0D, result.finalContribution(), EPSILON);
        assertEquals(9.0D, result.shieldingReduction(), EPSILON);
    }
}
