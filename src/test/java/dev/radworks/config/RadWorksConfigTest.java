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
        assertEquals(1.0D, RadWorksConfig.exposureThreshold(), 1.0e-9);
        assertEquals(120, RadWorksConfig.effectDurationTicks());
        assertEquals(40, RadWorksConfig.scanIntervalTicks());
        assertTrue(RadWorksConfig.applyEffectToPlayers());
        assertTrue(RadWorksConfig.applyEffectToLivingEntities());
        assertTrue(RadWorksConfig.applyEffectToMobs());
        assertFalse(RadWorksConfig.applyEffectToArmorStands());
        assertEquals(32, RadWorksConfig.maxLivingTargetsPerScan());
        assertEquals(8, RadWorksConfig.livingTargetScanRadius());
        assertTrue(RadWorksConfig.applyShieldingToLivingEntities());
        assertFalse(RadWorksConfig.damageEnabled());
        assertFalse(RadWorksConfig.alwaysShowRadiusVisualization());
        assertEquals(EffectMode.EXTERNAL_IF_PRESENT, RadWorksConfig.effectMode());
        assertFalse(RadWorksConfig.enableDevRules());
        assertTrue(RadWorksConfig.dynamicRadiusEnabled());
        assertEquals(0.5D, RadWorksConfig.dynamicRadiusScale(), 1.0e-9);
        assertEquals(8.0D, RadWorksConfig.dynamicRadiusMaxCap(), 1.0e-9);
        assertEquals("log2_scaled", RadWorksConfig.dynamicRadiusFormulaLabel());
        assertTrue(RadWorksConfig.nestedContainersEnabled());
        assertEquals(2, RadWorksConfig.nestedContainerMaxDepth());
        assertEquals(128, RadWorksConfig.nestedContainerMaxItemsPerSource());
        assertEquals(20, RadWorksConfig.nestedContainerDiagnosticSampleCap());
        assertTrue(RadWorksConfig.createTransientCarriersEnabled());
        assertTrue(RadWorksConfig.createTransientCarrierNbtScanEnabled());
        assertEquals(8, RadWorksConfig.createTransientCarrierMaxScanRadius());
        assertEquals(20, RadWorksConfig.createTransientCarrierDiagnosticSampleCap());
        assertEquals(5, RadWorksConfig.createTransientCarrierPathSampleCap());
        assertTrue(RadWorksConfig.entityCarriersEnabled());
        assertTrue(RadWorksConfig.entityDroppedItemsEnabled());
        assertTrue(RadWorksConfig.entityItemFramesEnabled());
        assertTrue(RadWorksConfig.entityPlayerAuraEnabled());
        assertEquals(8, RadWorksConfig.entityCarrierMaxScanRadius());
        assertEquals(20, RadWorksConfig.entityCarrierDiagnosticSampleCap());
        assertTrue(RadWorksConfig.entityChestBoatsEnabled());
        assertTrue(RadWorksConfig.entityPackAnimalsEnabled());
        assertTrue(RadWorksConfig.entityGenericInventoryCapabilityEnabled());
        assertEquals(20, RadWorksConfig.entityInventoryDiagnosticSampleCap());
    }
}
