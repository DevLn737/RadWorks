package dev.radworks.config;

import com.google.gson.JsonObject;
import dev.radworks.radiation.effects.EffectMode;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class RadWorksConfig {
    public static final boolean DEFAULT_GAMEPLAY_ENABLED = true;
    public static final boolean DEFAULT_AUTO_APPLY_EFFECT = true;
    public static final double DEFAULT_EXPOSURE_THRESHOLD = 10.0D;
    public static final int DEFAULT_EFFECT_DURATION_TICKS = 120;
    public static final int DEFAULT_SCAN_INTERVAL_TICKS = 40;
    public static final boolean DEFAULT_DAMAGE_ENABLED = false;
    public static final boolean DEFAULT_ENABLE_DEV_RULES = false;
    public static final EffectMode DEFAULT_EFFECT_MODE = EffectMode.EXTERNAL_IF_PRESENT;
    public static final boolean DEFAULT_DYNAMIC_RADIUS_ENABLED = true;
    public static final double DEFAULT_DYNAMIC_RADIUS_SCALE = 0.5D;
    public static final double DEFAULT_DYNAMIC_RADIUS_MAX_CAP = 8.0D;
    public static final String DEFAULT_DYNAMIC_RADIUS_FORMULA = "log2_scaled";

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue GAMEPLAY_ENABLED;
    private static final ModConfigSpec.BooleanValue AUTO_APPLY_EFFECT;
    private static final ModConfigSpec.DoubleValue EXPOSURE_THRESHOLD;
    private static final ModConfigSpec.IntValue EFFECT_DURATION_TICKS;
    private static final ModConfigSpec.IntValue SCAN_INTERVAL_TICKS;
    private static final ModConfigSpec.BooleanValue DAMAGE_ENABLED;
    private static final ModConfigSpec.EnumValue<EffectMode> EFFECT_MODE;
    private static final ModConfigSpec.BooleanValue ENABLE_DEV_RULES;
    private static final ModConfigSpec.BooleanValue DYNAMIC_RADIUS_ENABLED;
    private static final ModConfigSpec.DoubleValue DYNAMIC_RADIUS_SCALE;
    private static final ModConfigSpec.DoubleValue DYNAMIC_RADIUS_MAX_CAP;
    private static final ModConfigSpec.ConfigValue<String> DYNAMIC_RADIUS_FORMULA;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("gameplay");
        GAMEPLAY_ENABLED = builder
                .comment("Master switch for RadWorks beta gameplay behavior.")
                .define("enabled", DEFAULT_GAMEPLAY_ENABLED);
        AUTO_APPLY_EFFECT = builder
                .comment("When true, server-side exposure checks can apply radworks:radiation.")
                .define("autoApplyEffect", DEFAULT_AUTO_APPLY_EFFECT);
        EXPOSURE_THRESHOLD = builder
                .comment("Minimum diagnostic exposure required for beta effect auto-application.")
                .defineInRange("exposureThreshold", DEFAULT_EXPOSURE_THRESHOLD, 0.0D, 1_000_000.0D);
        EFFECT_DURATION_TICKS = builder
                .comment("Duration for automatically applied radworks:radiation.")
                .defineInRange("effectDurationTicks", DEFAULT_EFFECT_DURATION_TICKS, 1, 20 * 60 * 60);
        SCAN_INTERVAL_TICKS = builder
                .comment("Per-player server tick interval between exposure scans.")
                .defineInRange("scanIntervalTicks", DEFAULT_SCAN_INTERVAL_TICKS, 1, 20 * 60 * 10);
        DAMAGE_ENABLED = builder
                .comment("Reserved for POST_BETA. Damage is not implemented in beta.")
                .define("damageEnabled", DEFAULT_DAMAGE_ENABLED);
        EFFECT_MODE = builder
                .comment("Runtime effect compatibility mode: own | external_if_present | external_only | disabled")
                .defineEnum("effectMode", DEFAULT_EFFECT_MODE);
        builder.pop();
        builder.push("rules");
        ENABLE_DEV_RULES = builder
                .comment("Enable development-only radiation rules (vanilla smoke rules).")
                .define("enableDevRules", DEFAULT_ENABLE_DEV_RULES);
        DYNAMIC_RADIUS_ENABLED = builder
                .comment("Enable dynamic radius growth for aggregated inventory/handler sources.")
                .define("dynamicRadiusEnabled", DEFAULT_DYNAMIC_RADIUS_ENABLED);
        DYNAMIC_RADIUS_SCALE = builder
                .comment("Scale for dynamic radius bonus: base + scale*log2(units).")
                .defineInRange("dynamicRadiusScale", DEFAULT_DYNAMIC_RADIUS_SCALE, 0.0D, 10.0D);
        DYNAMIC_RADIUS_MAX_CAP = builder
                .comment("Maximum effective radius for dynamic aggregate sources.")
                .defineInRange("dynamicRadiusMaxCap", DEFAULT_DYNAMIC_RADIUS_MAX_CAP, 0.0D, 128.0D);
        DYNAMIC_RADIUS_FORMULA = builder
                .comment("Diagnostics-only label for dynamic radius formula.")
                .define("dynamicRadiusFormula", DEFAULT_DYNAMIC_RADIUS_FORMULA);
        builder.pop();
        SPEC = builder.build();
    }

    private RadWorksConfig() {
    }

    public static boolean gameplayEnabled() {
        return getBoolean(GAMEPLAY_ENABLED, DEFAULT_GAMEPLAY_ENABLED);
    }

    public static boolean autoApplyEffect() {
        return getBoolean(AUTO_APPLY_EFFECT, DEFAULT_AUTO_APPLY_EFFECT);
    }

    public static double exposureThreshold() {
        return getDouble(EXPOSURE_THRESHOLD, DEFAULT_EXPOSURE_THRESHOLD);
    }

    public static int effectDurationTicks() {
        return getInt(EFFECT_DURATION_TICKS, DEFAULT_EFFECT_DURATION_TICKS);
    }

    public static int scanIntervalTicks() {
        return getInt(SCAN_INTERVAL_TICKS, DEFAULT_SCAN_INTERVAL_TICKS);
    }

    public static boolean damageEnabled() {
        return getBoolean(DAMAGE_ENABLED, DEFAULT_DAMAGE_ENABLED);
    }

    public static EffectMode effectMode() {
        return getEnum(EFFECT_MODE, DEFAULT_EFFECT_MODE);
    }

    public static boolean enableDevRules() {
        return getBoolean(ENABLE_DEV_RULES, DEFAULT_ENABLE_DEV_RULES);
    }

    public static boolean dynamicRadiusEnabled() {
        return getBoolean(DYNAMIC_RADIUS_ENABLED, DEFAULT_DYNAMIC_RADIUS_ENABLED);
    }

    public static double dynamicRadiusScale() {
        return getDouble(DYNAMIC_RADIUS_SCALE, DEFAULT_DYNAMIC_RADIUS_SCALE);
    }

    public static double dynamicRadiusMaxCap() {
        return getDouble(DYNAMIC_RADIUS_MAX_CAP, DEFAULT_DYNAMIC_RADIUS_MAX_CAP);
    }

    public static String dynamicRadiusFormulaLabel() {
        return getString(DYNAMIC_RADIUS_FORMULA, DEFAULT_DYNAMIC_RADIUS_FORMULA);
    }

    public static JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("gameplayEnabled", gameplayEnabled());
        json.addProperty("autoApplyEffect", autoApplyEffect());
        json.addProperty("exposureThreshold", exposureThreshold());
        json.addProperty("effectDurationTicks", effectDurationTicks());
        json.addProperty("scanIntervalTicks", scanIntervalTicks());
        json.addProperty("damageEnabled", damageEnabled());
        json.addProperty("effectMode", effectMode().id());
        json.addProperty("enableDevRules", enableDevRules());
        json.addProperty("dynamicRadiusEnabled", dynamicRadiusEnabled());
        json.addProperty("dynamicRadiusScale", dynamicRadiusScale());
        json.addProperty("dynamicRadiusMaxCap", dynamicRadiusMaxCap());
        json.addProperty("dynamicRadiusFormula", dynamicRadiusFormulaLabel());
        return json;
    }

    private static boolean getBoolean(ModConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static double getDouble(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static int getInt(ModConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static <T extends Enum<T>> T getEnum(ModConfigSpec.EnumValue<T> value, T fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static String getString(ModConfigSpec.ConfigValue<String> value, String fallback) {
        try {
            String loaded = value.get();
            return (loaded == null || loaded.isBlank()) ? fallback : loaded;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
