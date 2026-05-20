package dev.radworks.gameplay;

import dev.radworks.radiation.effects.EffectMode;

final class LivingEntityEffectDecisionPolicy {
    private LivingEntityEffectDecisionPolicy() {
    }

    static Decision evaluate(
            EffectMode effectMode,
            String selectedRuntimeEffectId,
            boolean selectedRuntimeEffectRegistered,
            double totalExposure,
            double threshold) {
        if (effectMode == EffectMode.DISABLED) {
            return new Decision(false, false, "effect_mode_disabled");
        }
        if (selectedRuntimeEffectId == null || !selectedRuntimeEffectRegistered) {
            return new Decision(false, false, "selected_effect_missing");
        }
        if (totalExposure < threshold) {
            return new Decision(false, false, "below_threshold");
        }
        return new Decision(true, true, "applied");
    }

    record Decision(boolean wouldApply, boolean shouldAttemptApply, String reason) {
    }
}
