package dev.radworks.radiation.effects;

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
        return new EffectStrategyResult(
                MODE,
                SELECTED_EFFECT_ID,
                selectedRegistered,
                EXTERNAL_EFFECT_ID,
                externalPresent,
                THRESHOLD,
                List.of("Phase 6D preview only; radworks:radiation is registered but not auto-applied"));
    }

    public static EffectPreviewResult preview(double exposure, ArmorProtectionResult armorProtection) {
        String armorStatus = armorProtection.status();
        if ("full".equals(armorStatus)) {
            return new EffectPreviewResult(
                    false,
                    "blocked_by_full_armor",
                    PREVIEW_DURATION_TICKS,
                    PREVIEW_AMPLIFIER,
                    true,
                    false,
                    THRESHOLD,
                    exposure,
                    armorStatus);
        }

        if (exposure >= THRESHOLD) {
            return new EffectPreviewResult(
                    true,
                    "exposure_at_or_above_threshold",
                    PREVIEW_DURATION_TICKS,
                    PREVIEW_AMPLIFIER,
                    false,
                    false,
                    THRESHOLD,
                    exposure,
                    armorStatus);
        }

        return new EffectPreviewResult(
                false,
                "below_threshold",
                PREVIEW_DURATION_TICKS,
                PREVIEW_AMPLIFIER,
                false,
                false,
                THRESHOLD,
                exposure,
                armorStatus);
    }
}
