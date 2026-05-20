package dev.radworks.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadWorksConfigLivingTargetsTest {
    @Test
    void livingTargetDefaultsAreBetaSafeAndEnabled() {
        assertTrue(RadWorksConfig.applyEffectToPlayers());
        assertTrue(RadWorksConfig.applyEffectToLivingEntities());
        assertTrue(RadWorksConfig.applyEffectToMobs());
        assertFalse(RadWorksConfig.applyEffectToArmorStands());
        assertTrue(RadWorksConfig.applyShieldingToLivingEntities());
        assertTrue(RadWorksConfig.maxLivingTargetsPerScan() > 0);
        assertTrue(RadWorksConfig.livingTargetScanRadius() > 0);
    }
}
