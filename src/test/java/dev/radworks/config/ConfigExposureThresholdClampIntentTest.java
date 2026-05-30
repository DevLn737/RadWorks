package dev.radworks.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

class ConfigExposureThresholdClampIntentTest {
    @Test
    void exposureThreshold_whenConfiguredAboveDefault_shouldClampToDefault_characterization() throws Exception {
        Field field = RadWorksConfig.class.getDeclaredField("EXPOSURE_THRESHOLD");
        field.setAccessible(true);
        ModConfigSpec.DoubleValue value = (ModConfigSpec.DoubleValue) field.get(null);
        double previous = value.get();
        try {
            value.set(5.0D);
            value.clearCache();
            assertEquals(1.0D, RadWorksConfig.exposureThreshold(), 1.0e-9);
        } finally {
            value.set(previous);
            value.clearCache();
        }
    }

    @Test
    void exposureThreshold_specIntentNeedsDecision_shouldAllowConfiguredValueWithinDeclaredRange() {
        // SPEC_CODE_MISMATCH_CANDIDATE:
        // config declares defineInRange(..., 0..1_000_000), but runtime getter clamps to DEFAULT_EXPOSURE_THRESHOLD.
        // This test records current intent gap without changing runtime behavior.
        assertTrue(RadWorksConfig.DEFAULT_EXPOSURE_THRESHOLD <= 1_000_000.0D);
        assertTrue(RadWorksConfig.exposureThreshold() <= RadWorksConfig.DEFAULT_EXPOSURE_THRESHOLD);
    }
}
