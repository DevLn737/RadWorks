package dev.radworks.radiation.shielding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShieldingEngineTargetAwarePolicyTest {
    @Test
    void selfCarriedSourceIsDetectedByCarrierAndTargetIds() {
        assertTrue(ShieldingEngine.isSelfCarriedSourceForTarget("target-uuid", "target-uuid"));
        assertFalse(ShieldingEngine.isSelfCarriedSourceForTarget("carrier-uuid", "target-uuid"));
        assertFalse(ShieldingEngine.isSelfCarriedSourceForTarget(null, "target-uuid"));
    }
}
