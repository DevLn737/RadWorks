package dev.radworks.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.radiation.effects.EffectMode;
import org.junit.jupiter.api.Test;

class LivingEntityEffectDecisionTest {
    @Test
    void belowThresholdDoesNotApply() {
        var decision = LivingEntityEffectDecisionPolicy.evaluate(
                EffectMode.EXTERNAL_IF_PRESENT,
                "radworks:radiation",
                true,
                0.9D,
                1.0D);
        assertFalse(decision.shouldAttemptApply());
        assertFalse(decision.wouldApply());
        assertEquals("below_threshold", decision.reason());
    }

    @Test
    void aboveThresholdWouldApply() {
        var decision = LivingEntityEffectDecisionPolicy.evaluate(
                EffectMode.OWN,
                "radworks:radiation",
                true,
                10.0D,
                1.0D);
        assertTrue(decision.shouldAttemptApply());
        assertTrue(decision.wouldApply());
        assertEquals("applied", decision.reason());
    }

    @Test
    void selectedEffectMissingSkips() {
        var decision = LivingEntityEffectDecisionPolicy.evaluate(
                EffectMode.EXTERNAL_ONLY,
                null,
                false,
                100.0D,
                1.0D);
        assertFalse(decision.shouldAttemptApply());
        assertEquals("selected_effect_missing", decision.reason());
    }

    @Test
    void disabledModeSkips() {
        var decision = LivingEntityEffectDecisionPolicy.evaluate(
                EffectMode.DISABLED,
                null,
                false,
                100.0D,
                1.0D);
        assertFalse(decision.shouldAttemptApply());
        assertEquals("effect_mode_disabled", decision.reason());
    }
}
