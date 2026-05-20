package dev.radworks.config;

import com.google.gson.JsonObject;
import dev.radworks.radiation.effects.EffectMode;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class RadWorksConfig {
    public static final boolean DEFAULT_GAMEPLAY_ENABLED = true;
    public static final boolean DEFAULT_AUTO_APPLY_EFFECT = true;
    public static final double DEFAULT_EXPOSURE_THRESHOLD = 1.0D;
    public static final int DEFAULT_EFFECT_DURATION_TICKS = 120;
    public static final int DEFAULT_SCAN_INTERVAL_TICKS = 40;
    public static final boolean DEFAULT_APPLY_EFFECT_TO_PLAYERS = true;
    public static final boolean DEFAULT_APPLY_EFFECT_TO_LIVING_ENTITIES = true;
    public static final boolean DEFAULT_APPLY_EFFECT_TO_MOBS = true;
    public static final boolean DEFAULT_APPLY_EFFECT_TO_ARMOR_STANDS = false;
    public static final int DEFAULT_MAX_LIVING_TARGETS_PER_SCAN = 32;
    public static final int DEFAULT_LIVING_TARGET_SCAN_RADIUS = 8;
    public static final boolean DEFAULT_APPLY_SHIELDING_TO_LIVING_ENTITIES = true;
    public static final boolean DEFAULT_DAMAGE_ENABLED = false;
    public static final boolean DEFAULT_ALWAYS_SHOW_RADIUS_VISUALIZATION = false;
    public static final boolean DEFAULT_ENABLE_DEV_RULES = false;
    public static final EffectMode DEFAULT_EFFECT_MODE = EffectMode.EXTERNAL_IF_PRESENT;
    public static final boolean DEFAULT_DYNAMIC_RADIUS_ENABLED = true;
    public static final double DEFAULT_DYNAMIC_RADIUS_SCALE = 0.5D;
    public static final double DEFAULT_DYNAMIC_RADIUS_MAX_CAP = 8.0D;
    public static final String DEFAULT_DYNAMIC_RADIUS_FORMULA = "log2_scaled";
    public static final boolean DEFAULT_CREATE_TRANSIENT_CARRIERS_ENABLED = true;
    public static final boolean DEFAULT_CREATE_TRANSIENT_CARRIER_NBT_SCAN_ENABLED = true;
    public static final int DEFAULT_CREATE_TRANSIENT_CARRIER_MAX_SCAN_RADIUS = 8;
    public static final int DEFAULT_CREATE_TRANSIENT_CARRIER_DIAGNOSTIC_SAMPLE_CAP = 20;
    public static final int DEFAULT_CREATE_TRANSIENT_CARRIER_PATH_SAMPLE_CAP = 5;
    public static final boolean DEFAULT_ENTITY_CARRIERS_ENABLED = true;
    public static final boolean DEFAULT_ENTITY_DROPPED_ITEMS_ENABLED = true;
    public static final boolean DEFAULT_ENTITY_ITEM_FRAMES_ENABLED = true;
    public static final boolean DEFAULT_ENTITY_PLAYER_AURA_ENABLED = true;
    public static final int DEFAULT_ENTITY_CARRIER_MAX_SCAN_RADIUS = 8;
    public static final int DEFAULT_ENTITY_CARRIER_DIAGNOSTIC_SAMPLE_CAP = 20;
    public static final boolean DEFAULT_ENTITY_CHEST_BOATS_ENABLED = true;
    public static final boolean DEFAULT_ENTITY_PACK_ANIMALS_ENABLED = true;
    public static final boolean DEFAULT_ENTITY_GENERIC_INVENTORY_CAPABILITY_ENABLED = true;
    public static final int DEFAULT_ENTITY_INVENTORY_DIAGNOSTIC_SAMPLE_CAP = 20;
    public static final int DEFAULT_WORLD_FLUID_CLUSTER_DISCOVERY_RADIUS = 10;

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue GAMEPLAY_ENABLED;
    private static final ModConfigSpec.BooleanValue AUTO_APPLY_EFFECT;
    private static final ModConfigSpec.DoubleValue EXPOSURE_THRESHOLD;
    private static final ModConfigSpec.IntValue EFFECT_DURATION_TICKS;
    private static final ModConfigSpec.IntValue SCAN_INTERVAL_TICKS;
    private static final ModConfigSpec.BooleanValue APPLY_EFFECT_TO_PLAYERS;
    private static final ModConfigSpec.BooleanValue APPLY_EFFECT_TO_LIVING_ENTITIES;
    private static final ModConfigSpec.BooleanValue APPLY_EFFECT_TO_MOBS;
    private static final ModConfigSpec.BooleanValue APPLY_EFFECT_TO_ARMOR_STANDS;
    private static final ModConfigSpec.IntValue MAX_LIVING_TARGETS_PER_SCAN;
    private static final ModConfigSpec.IntValue LIVING_TARGET_SCAN_RADIUS;
    private static final ModConfigSpec.BooleanValue APPLY_SHIELDING_TO_LIVING_ENTITIES;
    private static final ModConfigSpec.BooleanValue DAMAGE_ENABLED;
    private static final ModConfigSpec.BooleanValue ALWAYS_SHOW_RADIUS_VISUALIZATION;
    private static final ModConfigSpec.EnumValue<EffectMode> EFFECT_MODE;
    private static final ModConfigSpec.BooleanValue ENABLE_DEV_RULES;
    private static final ModConfigSpec.BooleanValue DYNAMIC_RADIUS_ENABLED;
    private static final ModConfigSpec.DoubleValue DYNAMIC_RADIUS_SCALE;
    private static final ModConfigSpec.DoubleValue DYNAMIC_RADIUS_MAX_CAP;
    private static final ModConfigSpec.ConfigValue<String> DYNAMIC_RADIUS_FORMULA;
    private static final ModConfigSpec.BooleanValue CREATE_TRANSIENT_CARRIERS_ENABLED;
    private static final ModConfigSpec.BooleanValue CREATE_TRANSIENT_CARRIER_NBT_SCAN_ENABLED;
    private static final ModConfigSpec.IntValue CREATE_TRANSIENT_CARRIER_MAX_SCAN_RADIUS;
    private static final ModConfigSpec.IntValue CREATE_TRANSIENT_CARRIER_DIAGNOSTIC_SAMPLE_CAP;
    private static final ModConfigSpec.IntValue CREATE_TRANSIENT_CARRIER_PATH_SAMPLE_CAP;
    private static final ModConfigSpec.BooleanValue ENTITY_CARRIERS_ENABLED;
    private static final ModConfigSpec.BooleanValue ENTITY_DROPPED_ITEMS_ENABLED;
    private static final ModConfigSpec.BooleanValue ENTITY_ITEM_FRAMES_ENABLED;
    private static final ModConfigSpec.BooleanValue ENTITY_PLAYER_AURA_ENABLED;
    private static final ModConfigSpec.IntValue ENTITY_CARRIER_MAX_SCAN_RADIUS;
    private static final ModConfigSpec.IntValue ENTITY_CARRIER_DIAGNOSTIC_SAMPLE_CAP;
    private static final ModConfigSpec.BooleanValue ENTITY_CHEST_BOATS_ENABLED;
    private static final ModConfigSpec.BooleanValue ENTITY_PACK_ANIMALS_ENABLED;
    private static final ModConfigSpec.BooleanValue ENTITY_GENERIC_INVENTORY_CAPABILITY_ENABLED;
    private static final ModConfigSpec.IntValue ENTITY_INVENTORY_DIAGNOSTIC_SAMPLE_CAP;
    private static final ModConfigSpec.IntValue WORLD_FLUID_CLUSTER_DISCOVERY_RADIUS;

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
        APPLY_EFFECT_TO_PLAYERS = builder
                .comment("When true, player auto-apply pipeline stays enabled.")
                .define("applyEffectToPlayers", DEFAULT_APPLY_EFFECT_TO_PLAYERS);
        APPLY_EFFECT_TO_LIVING_ENTITIES = builder
                .comment("When true, nearby non-player living entities can receive radiation effect.")
                .define("applyEffectToLivingEntities", DEFAULT_APPLY_EFFECT_TO_LIVING_ENTITIES);
        APPLY_EFFECT_TO_MOBS = builder
                .comment("When true, mob/other-living targets are eligible for living-entity auto-apply.")
                .define("applyEffectToMobs", DEFAULT_APPLY_EFFECT_TO_MOBS);
        APPLY_EFFECT_TO_ARMOR_STANDS = builder
                .comment("When true, armor stands are included in living-target effect pass.")
                .define("applyEffectToArmorStands", DEFAULT_APPLY_EFFECT_TO_ARMOR_STANDS);
        MAX_LIVING_TARGETS_PER_SCAN = builder
                .comment("Maximum nearby living targets processed per scan step.")
                .defineInRange("maxLivingTargetsPerScan", DEFAULT_MAX_LIVING_TARGETS_PER_SCAN, 1, 256);
        LIVING_TARGET_SCAN_RADIUS = builder
                .comment("Nearby living-target selection radius for effect auto-apply.")
                .defineInRange("livingTargetScanRadius", DEFAULT_LIVING_TARGET_SCAN_RADIUS, 1, 32);
        APPLY_SHIELDING_TO_LIVING_ENTITIES = builder
                .comment("When true, non-player living targets use shielding checks for positioned external sources.")
                .define("applyShieldingToLivingEntities", DEFAULT_APPLY_SHIELDING_TO_LIVING_ENTITIES);
        DAMAGE_ENABLED = builder
                .comment("Reserved for POST_BETA. Damage is not implemented in beta.")
                .define("damageEnabled", DEFAULT_DAMAGE_ENABLED);
        ALWAYS_SHOW_RADIUS_VISUALIZATION = builder
                .comment("When true, radiation radius visualization is shown continuously without /radworks radius show.")
                .define("alwaysShowRadiusVisualization", DEFAULT_ALWAYS_SHOW_RADIUS_VISUALIZATION);
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
        builder.push("integrations");
        CREATE_TRANSIENT_CARRIERS_ENABLED = builder
                .comment("Enable optional Create transient/internal carrier source scanning.")
                .define("createTransientCarriersEnabled", DEFAULT_CREATE_TRANSIENT_CARRIERS_ENABLED);
        CREATE_TRANSIENT_CARRIER_NBT_SCAN_ENABLED = builder
                .comment("Enable safe known-path block entity tag extraction for Create transient carriers.")
                .define("createTransientCarrierNbtScanEnabled", DEFAULT_CREATE_TRANSIENT_CARRIER_NBT_SCAN_ENABLED);
        CREATE_TRANSIENT_CARRIER_MAX_SCAN_RADIUS = builder
                .comment("Maximum scan radius for Create transient carrier provider.")
                .defineInRange(
                        "createTransientCarrierMaxScanRadius",
                        DEFAULT_CREATE_TRANSIENT_CARRIER_MAX_SCAN_RADIUS,
                        1,
                        64);
        CREATE_TRANSIENT_CARRIER_DIAGNOSTIC_SAMPLE_CAP = builder
                .comment("Maximum number of Create transient diagnostic block samples in dump.")
                .defineInRange(
                        "createTransientCarrierDiagnosticSampleCap",
                        DEFAULT_CREATE_TRANSIENT_CARRIER_DIAGNOSTIC_SAMPLE_CAP,
                        1,
                        200);
        CREATE_TRANSIENT_CARRIER_PATH_SAMPLE_CAP = builder
                .comment("Maximum number of path/content entries per Create transient diagnostic sample.")
                .defineInRange(
                        "createTransientCarrierPathSampleCap",
                        DEFAULT_CREATE_TRANSIENT_CARRIER_PATH_SAMPLE_CAP,
                        1,
                        20);
        ENTITY_CARRIERS_ENABLED = builder
                .comment("Enable entity-carried radiation source scanning.")
                .define("entityCarriersEnabled", DEFAULT_ENTITY_CARRIERS_ENABLED);
        ENTITY_DROPPED_ITEMS_ENABLED = builder
                .comment("Enable dropped ItemEntity radiation source scanning.")
                .define("entityDroppedItemsEnabled", DEFAULT_ENTITY_DROPPED_ITEMS_ENABLED);
        ENTITY_ITEM_FRAMES_ENABLED = builder
                .comment("Enable ItemFrame/GlowItemFrame radiation source scanning.")
                .define("entityItemFramesEnabled", DEFAULT_ENTITY_ITEM_FRAMES_ENABLED);
        ENTITY_PLAYER_AURA_ENABLED = builder
                .comment("Enable nearby other-player inventory aura radiation source scanning.")
                .define("entityPlayerAuraEnabled", DEFAULT_ENTITY_PLAYER_AURA_ENABLED);
        ENTITY_CARRIER_MAX_SCAN_RADIUS = builder
                .comment("Maximum scan radius for entity carrier provider.")
                .defineInRange("entityCarrierMaxScanRadius", DEFAULT_ENTITY_CARRIER_MAX_SCAN_RADIUS, 1, 64);
        ENTITY_CARRIER_DIAGNOSTIC_SAMPLE_CAP = builder
                .comment("Maximum number of entity carrier diagnostic skip samples in dump.")
                .defineInRange(
                        "entityCarrierDiagnosticSampleCap",
                        DEFAULT_ENTITY_CARRIER_DIAGNOSTIC_SAMPLE_CAP,
                        1,
                        200);
        ENTITY_CHEST_BOATS_ENABLED = builder
                .comment("Enable chest boat inventory entity carrier scanning.")
                .define("entityChestBoatsEnabled", DEFAULT_ENTITY_CHEST_BOATS_ENABLED);
        ENTITY_PACK_ANIMALS_ENABLED = builder
                .comment("Enable pack-animal inventory entity carrier scanning.")
                .define("entityPackAnimalsEnabled", DEFAULT_ENTITY_PACK_ANIMALS_ENABLED);
        ENTITY_GENERIC_INVENTORY_CAPABILITY_ENABLED = builder
                .comment("Enable generic entity ItemHandler capability scanning.")
                .define(
                        "entityGenericInventoryCapabilityEnabled",
                        DEFAULT_ENTITY_GENERIC_INVENTORY_CAPABILITY_ENABLED);
        ENTITY_INVENTORY_DIAGNOSTIC_SAMPLE_CAP = builder
                .comment("Maximum number of entity inventory diagnostic samples in dump.")
                .defineInRange(
                        "entityInventoryDiagnosticSampleCap",
                        DEFAULT_ENTITY_INVENTORY_DIAGNOSTIC_SAMPLE_CAP,
                        1,
                        200);
        WORLD_FLUID_CLUSTER_DISCOVERY_RADIUS = builder
                .comment("Discovery radius for world fluid cluster scan.")
                .defineInRange(
                        "worldFluidClusterDiscoveryRadius",
                        DEFAULT_WORLD_FLUID_CLUSTER_DISCOVERY_RADIUS,
                        1,
                        32);
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
        double configured = getDouble(EXPOSURE_THRESHOLD, DEFAULT_EXPOSURE_THRESHOLD);
        return Math.max(0.0D, Math.min(DEFAULT_EXPOSURE_THRESHOLD, configured));
    }

    public static int effectDurationTicks() {
        return getInt(EFFECT_DURATION_TICKS, DEFAULT_EFFECT_DURATION_TICKS);
    }

    public static int scanIntervalTicks() {
        return getInt(SCAN_INTERVAL_TICKS, DEFAULT_SCAN_INTERVAL_TICKS);
    }

    public static boolean applyEffectToPlayers() {
        return getBoolean(APPLY_EFFECT_TO_PLAYERS, DEFAULT_APPLY_EFFECT_TO_PLAYERS);
    }

    public static boolean applyEffectToLivingEntities() {
        return getBoolean(APPLY_EFFECT_TO_LIVING_ENTITIES, DEFAULT_APPLY_EFFECT_TO_LIVING_ENTITIES);
    }

    public static boolean applyEffectToMobs() {
        return getBoolean(APPLY_EFFECT_TO_MOBS, DEFAULT_APPLY_EFFECT_TO_MOBS);
    }

    public static boolean applyEffectToArmorStands() {
        return getBoolean(APPLY_EFFECT_TO_ARMOR_STANDS, DEFAULT_APPLY_EFFECT_TO_ARMOR_STANDS);
    }

    public static int maxLivingTargetsPerScan() {
        return getInt(MAX_LIVING_TARGETS_PER_SCAN, DEFAULT_MAX_LIVING_TARGETS_PER_SCAN);
    }

    public static int livingTargetScanRadius() {
        return getInt(LIVING_TARGET_SCAN_RADIUS, DEFAULT_LIVING_TARGET_SCAN_RADIUS);
    }

    public static boolean applyShieldingToLivingEntities() {
        return getBoolean(APPLY_SHIELDING_TO_LIVING_ENTITIES, DEFAULT_APPLY_SHIELDING_TO_LIVING_ENTITIES);
    }

    public static boolean damageEnabled() {
        return getBoolean(DAMAGE_ENABLED, DEFAULT_DAMAGE_ENABLED);
    }

    public static boolean alwaysShowRadiusVisualization() {
        return getBoolean(ALWAYS_SHOW_RADIUS_VISUALIZATION, DEFAULT_ALWAYS_SHOW_RADIUS_VISUALIZATION);
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

    public static boolean createTransientCarriersEnabled() {
        return getBoolean(CREATE_TRANSIENT_CARRIERS_ENABLED, DEFAULT_CREATE_TRANSIENT_CARRIERS_ENABLED);
    }

    public static boolean createTransientCarrierNbtScanEnabled() {
        return getBoolean(
                CREATE_TRANSIENT_CARRIER_NBT_SCAN_ENABLED,
                DEFAULT_CREATE_TRANSIENT_CARRIER_NBT_SCAN_ENABLED);
    }

    public static int createTransientCarrierMaxScanRadius() {
        return getInt(
                CREATE_TRANSIENT_CARRIER_MAX_SCAN_RADIUS,
                DEFAULT_CREATE_TRANSIENT_CARRIER_MAX_SCAN_RADIUS);
    }

    public static int createTransientCarrierDiagnosticSampleCap() {
        return getInt(
                CREATE_TRANSIENT_CARRIER_DIAGNOSTIC_SAMPLE_CAP,
                DEFAULT_CREATE_TRANSIENT_CARRIER_DIAGNOSTIC_SAMPLE_CAP);
    }

    public static int createTransientCarrierPathSampleCap() {
        return getInt(
                CREATE_TRANSIENT_CARRIER_PATH_SAMPLE_CAP,
                DEFAULT_CREATE_TRANSIENT_CARRIER_PATH_SAMPLE_CAP);
    }

    public static boolean entityCarriersEnabled() {
        return getBoolean(ENTITY_CARRIERS_ENABLED, DEFAULT_ENTITY_CARRIERS_ENABLED);
    }

    public static boolean entityDroppedItemsEnabled() {
        return getBoolean(ENTITY_DROPPED_ITEMS_ENABLED, DEFAULT_ENTITY_DROPPED_ITEMS_ENABLED);
    }

    public static boolean entityItemFramesEnabled() {
        return getBoolean(ENTITY_ITEM_FRAMES_ENABLED, DEFAULT_ENTITY_ITEM_FRAMES_ENABLED);
    }

    public static boolean entityPlayerAuraEnabled() {
        return getBoolean(ENTITY_PLAYER_AURA_ENABLED, DEFAULT_ENTITY_PLAYER_AURA_ENABLED);
    }

    public static int entityCarrierMaxScanRadius() {
        return getInt(ENTITY_CARRIER_MAX_SCAN_RADIUS, DEFAULT_ENTITY_CARRIER_MAX_SCAN_RADIUS);
    }

    public static int entityCarrierDiagnosticSampleCap() {
        return getInt(ENTITY_CARRIER_DIAGNOSTIC_SAMPLE_CAP, DEFAULT_ENTITY_CARRIER_DIAGNOSTIC_SAMPLE_CAP);
    }

    public static boolean entityChestBoatsEnabled() {
        return getBoolean(ENTITY_CHEST_BOATS_ENABLED, DEFAULT_ENTITY_CHEST_BOATS_ENABLED);
    }

    public static boolean entityPackAnimalsEnabled() {
        return getBoolean(ENTITY_PACK_ANIMALS_ENABLED, DEFAULT_ENTITY_PACK_ANIMALS_ENABLED);
    }

    public static boolean entityGenericInventoryCapabilityEnabled() {
        return getBoolean(
                ENTITY_GENERIC_INVENTORY_CAPABILITY_ENABLED,
                DEFAULT_ENTITY_GENERIC_INVENTORY_CAPABILITY_ENABLED);
    }

    public static int entityInventoryDiagnosticSampleCap() {
        return getInt(
                ENTITY_INVENTORY_DIAGNOSTIC_SAMPLE_CAP,
                DEFAULT_ENTITY_INVENTORY_DIAGNOSTIC_SAMPLE_CAP);
    }

    public static int worldFluidClusterDiscoveryRadius() {
        return getInt(
                WORLD_FLUID_CLUSTER_DISCOVERY_RADIUS,
                DEFAULT_WORLD_FLUID_CLUSTER_DISCOVERY_RADIUS);
    }

    public static JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("gameplayEnabled", gameplayEnabled());
        json.addProperty("autoApplyEffect", autoApplyEffect());
        json.addProperty("exposureThreshold", exposureThreshold());
        json.addProperty("effectDurationTicks", effectDurationTicks());
        json.addProperty("scanIntervalTicks", scanIntervalTicks());
        json.addProperty("applyEffectToPlayers", applyEffectToPlayers());
        json.addProperty("applyEffectToLivingEntities", applyEffectToLivingEntities());
        json.addProperty("applyEffectToMobs", applyEffectToMobs());
        json.addProperty("applyEffectToArmorStands", applyEffectToArmorStands());
        json.addProperty("maxLivingTargetsPerScan", maxLivingTargetsPerScan());
        json.addProperty("livingTargetScanRadius", livingTargetScanRadius());
        json.addProperty("applyShieldingToLivingEntities", applyShieldingToLivingEntities());
        json.addProperty("damageEnabled", damageEnabled());
        json.addProperty("alwaysShowRadiusVisualization", alwaysShowRadiusVisualization());
        json.addProperty("effectMode", effectMode().id());
        json.addProperty("enableDevRules", enableDevRules());
        json.addProperty("dynamicRadiusEnabled", dynamicRadiusEnabled());
        json.addProperty("dynamicRadiusScale", dynamicRadiusScale());
        json.addProperty("dynamicRadiusMaxCap", dynamicRadiusMaxCap());
        json.addProperty("dynamicRadiusFormula", dynamicRadiusFormulaLabel());
        json.addProperty("createTransientCarriersEnabled", createTransientCarriersEnabled());
        json.addProperty("createTransientCarrierNbtScanEnabled", createTransientCarrierNbtScanEnabled());
        json.addProperty("createTransientCarrierMaxScanRadius", createTransientCarrierMaxScanRadius());
        json.addProperty("createTransientCarrierDiagnosticSampleCap", createTransientCarrierDiagnosticSampleCap());
        json.addProperty("createTransientCarrierPathSampleCap", createTransientCarrierPathSampleCap());
        json.addProperty("entityCarriersEnabled", entityCarriersEnabled());
        json.addProperty("entityDroppedItemsEnabled", entityDroppedItemsEnabled());
        json.addProperty("entityItemFramesEnabled", entityItemFramesEnabled());
        json.addProperty("entityPlayerAuraEnabled", entityPlayerAuraEnabled());
        json.addProperty("entityCarrierMaxScanRadius", entityCarrierMaxScanRadius());
        json.addProperty("entityCarrierDiagnosticSampleCap", entityCarrierDiagnosticSampleCap());
        json.addProperty("entityChestBoatsEnabled", entityChestBoatsEnabled());
        json.addProperty("entityPackAnimalsEnabled", entityPackAnimalsEnabled());
        json.addProperty(
                "entityGenericInventoryCapabilityEnabled",
                entityGenericInventoryCapabilityEnabled());
        json.addProperty("entityInventoryDiagnosticSampleCap", entityInventoryDiagnosticSampleCap());
        json.addProperty("worldFluidClusterDiscoveryRadius", worldFluidClusterDiscoveryRadius());
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
