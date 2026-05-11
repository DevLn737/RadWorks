package dev.radworks.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.radiation.effects.EffectMode;
import org.junit.jupiter.api.Test;

class RadWorksConfigTest {
    @Test
    void betaDefaultsArePlayableButDamageDisabled() {
        assertTrue(RadWorksConfig.gameplayEnabled());
        assertTrue(RadWorksConfig.autoApplyEffect());
        assertEquals(10.0D, RadWorksConfig.exposureThreshold(), 1.0e-9);
        assertEquals(120, RadWorksConfig.effectDurationTicks());
        assertEquals(40, RadWorksConfig.scanIntervalTicks());
        assertFalse(RadWorksConfig.damageEnabled());
        assertFalse(RadWorksConfig.alwaysShowRadiusVisualization());
        assertEquals(EffectMode.EXTERNAL_IF_PRESENT, RadWorksConfig.effectMode());
        assertFalse(RadWorksConfig.enableDevRules());
        assertTrue(RadWorksConfig.dynamicRadiusEnabled());
        assertEquals(0.5D, RadWorksConfig.dynamicRadiusScale(), 1.0e-9);
        assertEquals(8.0D, RadWorksConfig.dynamicRadiusMaxCap(), 1.0e-9);
        assertEquals("log2_scaled", RadWorksConfig.dynamicRadiusFormulaLabel());
    }
}
