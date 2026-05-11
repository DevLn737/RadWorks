package dev.radworks.radiation.effects;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.radiation.armor.ArmorProtectionResult;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class EffectStrategyService {
    public static final String MODE = "preview_only";
    public static final String SELECTED_EFFECT_ID = "radworks:radiation";
    public static final String EXTERNAL_EFFECT_ID = "createnuclear:radiation";
    public static final int PREVIEW_DURATION_TICKS = 20;
    public static final int PREVIEW_AMPLIFIER = 0;
    public static final double THRESHOLD = 10.0D;

    private static final ResourceLocation SELECTED_EFFECT = ResourceLocation.parse(SELECTED_EFFECT_ID);
    private static final ResourceLocation EXTERNAL_EFFECT = ResourceLocation.parse(EXTERNAL_EFFECT_ID);

    private EffectStrategyService() {
    }

    public static EffectStrategyResult strategy() {
        boolean selectedRegistered = BuiltInRegistries.MOB_EFFECT.containsKey(SELECTED_EFFECT);
        boolean externalPresent = BuiltInRegistries.MOB_EFFECT.containsKey(EXTERNAL_EFFECT);
        RuntimeEffectSelection runtime = resolveRuntimeSelection();
        return new EffectStrategyResult(
                MODE,
                SELECTED_EFFECT_ID,
                selectedRegistered,
                EXTERNAL_EFFECT_ID,
                externalPresent,
                runtime.effectMode().id(),
                runtime.selectedRuntimeEffectId(),
                runtime.selectedRuntimeEffectRegistered(),
                runtime.fallbackReason(),
                RadWorksConfig.exposureThreshold(),
                List.of("Phase 6 beta: effect preview is hypothetical; real auto-apply diagnostics live under gameplay"));
    }

    public static RuntimeEffectSelection resolveRuntimeSelection() {
        EffectMode mode = RadWorksConfig.effectMode();
        boolean ownPresent = BuiltInRegistries.MOB_EFFECT.containsKey(SELECTED_EFFECT);
        boolean externalPresent = BuiltInRegistries.MOB_EFFECT.containsKey(EXTERNAL_EFFECT);

        return switch (mode) {
            case OWN -> new RuntimeEffectSelection(
                    mode,
                    SELECTED_EFFECT_ID,
                    ownPresent,
                    externalPresent,
                    "own_selected");
            case EXTERNAL_IF_PRESENT -> {
                if (externalPresent) {
                    yield new RuntimeEffectSelection(
                            mode,
                            EXTERNAL_EFFECT_ID,
                            true,
                            true,
                            "external_present");
                }
                yield new RuntimeEffectSelection(
                        mode,
                        SELECTED_EFFECT_ID,
                        ownPresent,
                        false,
                        "external_missing_fallback_to_own");
            }
            case EXTERNAL_ONLY -> {
                if (externalPresent) {
                    yield new RuntimeEffectSelection(
                            mode,
                            EXTERNAL_EFFECT_ID,
                            true,
                            true,
                            "external_present");
                }
                yield new RuntimeEffectSelection(
                        mode,
                        null,
                        false,
                        false,
                        "external_only_missing");
            }
            case DISABLED -> new RuntimeEffectSelection(
                    mode,
                    null,
                    false,
                    externalPresent,
                    "mode_disabled");
        };
    }

    public static EffectPreviewResult preview(double exposure, ArmorProtectionResult armorProtection) {
        double threshold = RadWorksConfig.exposureThreshold();
        String armorStatus = armorProtection.status();
        if ("full".equals(armorStatus)) {
            return new EffectPreviewResult(
                    false,
                    "blocked_by_full_armor",
                    RadWorksConfig.effectDurationTicks(),
                    PREVIEW_AMPLIFIER,
                    true,
                    false,
                    threshold,
                    exposure,
                    armorStatus);
        }

        if (exposure >= threshold) {
            return new EffectPreviewResult(
                    true,
                    "exposure_at_or_above_threshold",
                    RadWorksConfig.effectDurationTicks(),
                    PREVIEW_AMPLIFIER,
                    false,
                    false,
                    threshold,
                    exposure,
                    armorStatus);
        }

        return new EffectPreviewResult(
                false,
                "below_threshold",
                RadWorksConfig.effectDurationTicks(),
                PREVIEW_AMPLIFIER,
                false,
                false,
                threshold,
                exposure,
                armorStatus);
    }

    public record RuntimeEffectSelection(
            EffectMode effectMode,
            String selectedRuntimeEffectId,
            boolean selectedRuntimeEffectRegistered,
            boolean externalEffectPresent,
            String fallbackReason) {
    }
}
