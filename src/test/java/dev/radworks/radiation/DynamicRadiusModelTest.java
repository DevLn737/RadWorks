package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DynamicRadiusModelTest {
    @Test
    void oneUnitUsesBaseRadius() {
        double effective = DynamicRadiusModel.effectiveRadius(2.0D, DynamicRadiusModel.aggregateUnitsForItems(1));
        assertEquals(2.0D, effective, 1.0e-9);
    }

    @Test
    void sixtyFourUnitsHasLargerRadiusThanOne() {
        double one = DynamicRadiusModel.effectiveRadius(2.0D, DynamicRadiusModel.aggregateUnitsForItems(1));
        double sixtyFour = DynamicRadiusModel.effectiveRadius(2.0D, DynamicRadiusModel.aggregateUnitsForItems(64));
        assertTrue(sixtyFour > one);
    }

    @Test
    void capIsEnforced() {
        double effective = DynamicRadiusModel.effectiveRadius(7.5D, DynamicRadiusModel.aggregateUnitsForItems(1_000_000));
        assertTrue(effective <= 8.0D);
    }

    @Test
    void fluidUnitsUseBuckets() {
        double oneBucket = DynamicRadiusModel.aggregateUnitsForFluids(1_000);
        double twoAndHalf = DynamicRadiusModel.aggregateUnitsForFluids(2_500);
        assertEquals(1.0D, oneBucket, 1.0e-9);
        assertTrue(twoAndHalf > oneBucket);
    }
}
