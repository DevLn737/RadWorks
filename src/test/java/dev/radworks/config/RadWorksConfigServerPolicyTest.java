package dev.radworks.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadWorksConfigServerPolicyTest {
    @Test
    void serverAuthoritativeDefaultsAreSafeAndBounded() {
        assertFalse(RadWorksConfig.alwaysShowRadiusVisualization());
        assertFalse(RadWorksConfig.damageEnabled());

        assertTrue(RadWorksConfig.scanIntervalTicks() > 0);
        assertTrue(RadWorksConfig.maxLivingTargetsPerScan() > 0);
        assertTrue(RadWorksConfig.livingTargetScanRadius() > 0);
        assertTrue(RadWorksConfig.effectDurationTicks() > 0);
        assertTrue(RadWorksConfig.dynamicRadiusMaxCap() > 0.0D);
        assertTrue(RadWorksConfig.dynamicRadiusScale() >= 0.0D);

        assertTrue(RadWorksConfig.createTransientCarrierMaxScanRadius() > 0);
        assertTrue(RadWorksConfig.createTransientCarrierDiagnosticSampleCap() > 0);
        assertTrue(RadWorksConfig.createTransientCarrierPathSampleCap() > 0);
        assertTrue(RadWorksConfig.entityCarrierMaxScanRadius() > 0);
        assertTrue(RadWorksConfig.entityCarrierDiagnosticSampleCap() > 0);
        assertTrue(RadWorksConfig.entityInventoryDiagnosticSampleCap() > 0);

        assertTrue(RadWorksConfig.worldFluidClusterDiscoveryRadius() >= 1);
        assertTrue(RadWorksConfig.worldFluidClusterDiscoveryRadius() <= 32);
        assertTrue(RadWorksConfig.maxLivingTargetsPerScan() >= 1);
        assertTrue(RadWorksConfig.maxLivingTargetsPerScan() <= 256);
        assertTrue(RadWorksConfig.livingTargetScanRadius() >= 1);
        assertTrue(RadWorksConfig.livingTargetScanRadius() <= 32);
    }
}
