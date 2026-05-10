package dev.radworks.radiation.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.radiation.armor.ArmorProtectionResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class EffectStrategyServiceTest {
    @Test
    void previewBelowThresholdIsBlocked() {
        EffectPreviewResult preview = EffectStrategyService.preview(1.0D, armor("none"));
        assertFalse(preview.wouldApply());
        assertEquals("below_threshold", preview.reason());
        assertFalse(preview.blockedByArmor());
        assertFalse(preview.applied());
    }

    @Test
    void previewAtThresholdWithNoArmorWouldApply() {
        EffectPreviewResult preview = EffectStrategyService.preview(10.0D, armor("none"));
        assertTrue(preview.wouldApply());
        assertEquals("exposure_at_or_above_threshold", preview.reason());
        assertFalse(preview.blockedByArmor());
        assertFalse(preview.applied());
    }

    @Test
    void previewAtThresholdWithPartialArmorWouldApply() {
        EffectPreviewResult preview = EffectStrategyService.preview(10.0D, armor("partial"));
        assertTrue(preview.wouldApply());
        assertEquals("exposure_at_or_above_threshold", preview.reason());
        assertFalse(preview.blockedByArmor());
        assertFalse(preview.applied());
    }

    @Test
    void previewAtThresholdWithFullArmorIsBlocked() {
        EffectPreviewResult preview = EffectStrategyService.preview(10.0D, armor("full"));
        assertFalse(preview.wouldApply());
        assertEquals("blocked_by_full_armor", preview.reason());
        assertTrue(preview.blockedByArmor());
        assertFalse(preview.applied());
    }

    private static ArmorProtectionResult armor(String status) {
        return new ArmorProtectionResult(
                status,
                List.of("head", "chest", "legs", "feet"),
                List.of(),
                List.of(),
                "test",
                false,
                false,
                false,
                0.0D);
    }
}
