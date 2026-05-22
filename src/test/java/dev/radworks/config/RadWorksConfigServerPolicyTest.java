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
        assertTrue(RadWorksConfig.nestedContainerMaxDepth() > 0);
        assertTrue(RadWorksConfig.nestedContainerMaxItemsPerSource() > 0);
        assertTrue(RadWorksConfig.nestedContainerDiagnosticSampleCap() > 0);
        assertTrue(RadWorksConfig.sourceOverrideDiagnosticSampleCap() > 0);

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
        assertTrue(RadWorksConfig.applyShieldingToLivingEntities());
        assertTrue(RadWorksConfig.nestedContainerMaxDepth() >= 1);
        assertTrue(RadWorksConfig.nestedContainerMaxDepth() <= 5);
        assertTrue(RadWorksConfig.nestedContainerMaxItemsPerSource() >= 1);
        assertTrue(RadWorksConfig.nestedContainerMaxItemsPerSource() <= 1024);
        assertTrue(RadWorksConfig.nestedContainerDiagnosticSampleCap() >= 1);
        assertTrue(RadWorksConfig.nestedContainerDiagnosticSampleCap() <= 200);
        assertTrue(RadWorksConfig.sourceOverrideDiagnosticSampleCap() >= 1);
        assertTrue(RadWorksConfig.sourceOverrideDiagnosticSampleCap() <= 200);
        assertTrue(RadWorksConfig.sourceOverridesEnabled());
        assertTrue(RadWorksConfig.sourceExclusionsEnabled());
        assertTrue(RadWorksConfig.sourceContainmentEnabled());
        assertTrue(RadWorksConfig.forcedSourcesEnabled());
    }
}
