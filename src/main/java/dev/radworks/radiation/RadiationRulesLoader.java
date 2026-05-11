package dev.radworks.radiation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import dev.radworks.config.RadWorksConfig;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class RadiationRulesLoader extends SimplePreparableReloadListener<RadiationRules> {
    private static final Gson GSON = new Gson();
    private static final FileToIdConverter RULE_FILES = FileToIdConverter.json("radiation_rules");
    private static volatile RadiationRules currentRules = RadiationRules.notLoaded();

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new RadiationRulesLoader());
    }

    public static RadiationRules currentRules() {
        return currentRules;
    }

    @Override
    protected RadiationRules prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        RadiationRuleValidationResult validation = new RadiationRuleValidationResult();
        List<RadiationRule> parsedRules = new ArrayList<>();

        for (Map.Entry<ResourceLocation, Resource> entry : RULE_FILES.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation fileId = RULE_FILES.fileToId(entry.getKey());
            ParsedRule parsedRule = parseRule(entry.getValue(), fileId.toString(), validation);
            if (parsedRule == null) {
                continue;
            }

            parsedRules.add(parsedRule.rule());
        }

        ValidatedRules validatedRules = validateRules(parsedRules, validation);
        return new RadiationRules(
                true,
                checksum(parsedRules),
                validatedRules.activeRules(),
                validatedRules.disabledRules(),
                validatedRules.activeAlwaysOrBetaRules(),
                validatedRules.activeDevRules(),
                validatedRules.suppressedDevRules(),
                validatedRules.candidateStatuses(),
                validation);
    }

    @Override
    protected void apply(RadiationRules rules, ResourceManager resourceManager, ProfilerFiller profiler) {
        currentRules = rules;
    }

    private static ParsedRule parseRule(Resource resource, String source, RadiationRuleValidationResult validation) {
        JsonObject json;
        try (Reader reader = resource.openAsReader()) {
            json = GsonHelper.fromJson(GSON, reader, JsonObject.class);
        } catch (IOException | JsonParseException exception) {
            validation.error("MALFORMED_JSON", source, exception.getMessage());
            return null;
        }

        if (json == null) {
            validation.error("MALFORMED_JSON", source, "JSON object is empty");
            return null;
        }

        RadiationRuleType type;
        ResourceLocation id;
        Double strength;
        Double radius;
        Boolean respectsShielding;
        Boolean enabled;
        RadiationRuleProfile profile;
        Boolean required;
        String optionalModId;
        String role;
        String comment;
        try {
            type = readType(json, source, validation);
            id = readId(json, source, validation);
            strength = readPositiveDouble(json, "strength", source, validation);
            radius = readPositiveDouble(json, "radius", source, validation);
            respectsShielding = readBoolean(json, "respectsShielding", source, validation);
            enabled = readBoolean(json, "enabled", source, validation);
            profile = readProfile(json, source, validation);
            required = readOptionalBoolean(json, "required", true, source, validation);
            optionalModId = readOptionalModId(json, source, validation);
            role = readOptionalString(json, "role", "");
            comment = json.has("comment")
                    ? GsonHelper.getAsString(json, "comment")
                    : GsonHelper.getAsString(json, "notes", "");
        } catch (IllegalArgumentException | JsonParseException exception) {
            validation.error("MALFORMED_JSON", source, exception.getMessage());
            return null;
        }

        if (type == null
                || id == null
                || strength == null
                || radius == null
                || respectsShielding == null
                || enabled == null
                || profile == null
                || required == null) {
            return null;
        }

        RadiationRule rule = new RadiationRule(
                type,
                id,
                strength,
                radius,
                respectsShielding,
                enabled,
                profile,
                required,
                optionalModId,
                role,
                comment,
                source);
        validateRegistryId(rule, validation);
        return new ParsedRule(rule);
    }

    private static RadiationRuleType readType(JsonObject json, String source, RadiationRuleValidationResult validation) {
        if (!json.has("type")) {
            validation.error("MALFORMED_JSON", source, "Missing required field: type");
            return null;
        }

        String typeId = GsonHelper.getAsString(json, "type");
        return RadiationRuleType.fromId(typeId).orElseGet(() -> {
            validation.error("INVALID_RULE_VALUE", source, "Unknown rule type: " + typeId);
            return null;
        });
    }

    private static ResourceLocation readId(JsonObject json, String source, RadiationRuleValidationResult validation) {
        if (!json.has("id")) {
            validation.error("MALFORMED_JSON", source, "Missing required field: id");
            return null;
        }

        String id = GsonHelper.getAsString(json, "id");
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            validation.error("INVALID_RULE_VALUE", source, "Invalid registry id: " + id);
        }
        return location;
    }

    private static Double readPositiveDouble(
            JsonObject json,
            String field,
            String source,
            RadiationRuleValidationResult validation) {
        if (!json.has(field)) {
            validation.error("MALFORMED_JSON", source, "Missing required field: " + field);
            return null;
        }

        double value;
        try {
            value = GsonHelper.getAsDouble(json, field);
        } catch (JsonSyntaxException exception) {
            validation.error("INVALID_RULE_VALUE", source, "Field " + field + " must be a number");
            return null;
        }

        if (value <= 0.0D) {
            validation.error("INVALID_RULE_VALUE", source, "Field " + field + " must be > 0");
            return null;
        }
        return value;
    }

    private static RadiationRuleProfile readProfile(
            JsonObject json,
            String source,
            RadiationRuleValidationResult validation) {
        if (!json.has("profile")) {
            validation.info("MISSING_PROFILE", source, "Missing profile; defaulting to profile=always");
            return RadiationRuleProfile.ALWAYS;
        }
        String profileId = GsonHelper.getAsString(json, "profile");
        return RadiationRuleProfile.fromId(profileId).orElseGet(() -> {
            validation.error("INVALID_RULE_VALUE", source, "Unknown profile: " + profileId);
            return null;
        });
    }

    private static Boolean readBoolean(
            JsonObject json,
            String field,
            String source,
            RadiationRuleValidationResult validation) {
        if (!json.has(field)) {
            validation.error("MALFORMED_JSON", source, "Missing required field: " + field);
            return null;
        }

        try {
            return GsonHelper.getAsBoolean(json, field);
        } catch (JsonSyntaxException exception) {
            validation.error("INVALID_RULE_VALUE", source, "Field " + field + " must be true or false");
            return null;
        }
    }

    private static Boolean readOptionalBoolean(
            JsonObject json,
            String field,
            boolean defaultValue,
            String source,
            RadiationRuleValidationResult validation) {
        if (!json.has(field)) {
            return defaultValue;
        }
        try {
            return GsonHelper.getAsBoolean(json, field);
        } catch (JsonSyntaxException exception) {
            validation.error("INVALID_RULE_VALUE", source, "Field " + field + " must be true or false");
            return null;
        }
    }

    private static String readOptionalString(JsonObject json, String field, String defaultValue) {
        if (!json.has(field)) {
            return defaultValue;
        }
        return GsonHelper.getAsString(json, field);
    }

    private static String readOptionalModId(
            JsonObject json,
            String source,
            RadiationRuleValidationResult validation) {
        if (!json.has("optionalModId")) {
            return null;
        }
        String optionalModId = GsonHelper.getAsString(json, "optionalModId", "").trim();
        if (optionalModId.isEmpty()) {
            validation.error("INVALID_RULE_VALUE", source, "Field optionalModId must not be empty");
            return null;
        }
        return optionalModId;
    }

    private static void validateRegistryId(RadiationRule rule, RadiationRuleValidationResult validation) {
        Registry<?> registry = switch (rule.type()) {
            case ITEM -> BuiltInRegistries.ITEM;
            case BLOCK -> BuiltInRegistries.BLOCK;
            case FLUID -> BuiltInRegistries.FLUID;
        };

        if (registry.containsKey(rule.id())) {
            return;
        }

        if (rule.required()) {
            validation.error("UNKNOWN_REGISTRY_ID", rule.source(), rule.type().id() + " id is not registered: " + rule.id());
            if (rule.type() == RadiationRuleType.BLOCK && BuiltInRegistries.ITEM.containsKey(rule.id())) {
                validation.warning(
                        "BLOCK_RULE_ID_NOT_BLOCK",
                        rule.source(),
                        "Block rule id "
                                + rule.id()
                                + " is not registered as a block, but an item with the same id exists; placed block matching will not work");
            }
            return;
        }

        if (rule.optionalModId() != null && !ModList.get().isLoaded(rule.optionalModId())) {
            validation.info(
                    "MISSING_OPTIONAL_MOD",
                    rule.source(),
                    "Optional " + rule.type().id() + " id " + rule.id()
                            + " is unavailable because mod '" + rule.optionalModId() + "' is not loaded");
            return;
        }

        validation.warning("UNKNOWN_REGISTRY_ID", rule.source(), rule.type().id() + " id is not registered: " + rule.id());
        if (rule.type() == RadiationRuleType.BLOCK && BuiltInRegistries.ITEM.containsKey(rule.id())) {
            validation.warning(
                    "BLOCK_RULE_ID_NOT_BLOCK",
                    rule.source(),
                    "Block rule id "
                            + rule.id()
                            + " is not registered as a block, but an item with the same id exists; placed block matching will not work");
        }
    }

    private static boolean isProfileEnabled(RadiationRule rule) {
        return switch (rule.profile()) {
            case ALWAYS, BETA -> true;
            case DEV -> RadWorksConfig.enableDevRules();
        };
    }

    private static boolean isDevRule(RadiationRule rule) {
        return rule.profile() == RadiationRuleProfile.DEV;
    }

    private static ValidatedRules validateRules(
            List<RadiationRule> parsedRules,
            RadiationRuleValidationResult validation) {
        Map<String, RadiationRule> firstEnabledRuleByKey = new HashMap<>();
        Map<String, RadiationRule> firstDisabledRuleByKey = new HashMap<>();
        List<RadiationRule> activeRules = new ArrayList<>();
        int disabledRules = 0;
        int activeAlwaysOrBetaRules = 0;
        int activeDevRules = 0;
        int suppressedDevRules = 0;
        List<RadiationRuleCandidateStatus> candidateStatuses = new ArrayList<>();

        for (RadiationRule rule : parsedRules) {
            boolean profileEnabled = isProfileEnabled(rule);
            if (!profileEnabled) {
                if (isDevRule(rule)) {
                    suppressedDevRules++;
                    validation.info(
                            "DEV_RULE_SUPPRESSED",
                            rule.source(),
                            "Development rule is suppressed in beta mode (rules.enableDevRules=false)");
                }
                candidateStatuses.add(toCandidateStatus(rule, false, "suppressed_dev_profile", "info"));
                continue;
            }

            if (!rule.enabled()) {
                disabledRules++;
                validation.info("DISABLED_RULE", rule.source(), "Rule is disabled and is not active");
                candidateStatuses.add(toCandidateStatus(rule, false, "disabled", "info"));

                RadiationRule firstDisabledRule = firstDisabledRuleByKey.putIfAbsent(rule.key(), rule);
                if (firstDisabledRule != null) {
                    validation.warning(
                            "DUPLICATE_RULE",
                            rule.source(),
                            "Duplicate disabled rule for " + rule.key() + "; first seen in " + firstDisabledRule.source());
                }
                continue;
            }

            String status = registryStatus(rule);
            String severity = statusSeverity(status);
            RadiationRule firstEnabledRule = firstEnabledRuleByKey.putIfAbsent(rule.key(), rule);
            if (firstEnabledRule != null) {
                validation.error(
                        "DUPLICATE_RULE",
                        rule.source(),
                        "Duplicate enabled rule for " + rule.key() + "; first seen in " + firstEnabledRule.source());
                candidateStatuses.add(toCandidateStatus(rule, false, status, severity));
                continue;
            }
            activeRules.add(rule);
            candidateStatuses.add(toCandidateStatus(rule, true, status, severity));
            if (isDevRule(rule)) {
                activeDevRules++;
            } else {
                activeAlwaysOrBetaRules++;
            }
        }

        activeRules.sort(Comparator.comparing(RadiationRule::key));
        return new ValidatedRules(
                activeRules,
                disabledRules,
                activeAlwaysOrBetaRules,
                activeDevRules,
                suppressedDevRules,
                candidateStatuses);
    }

    private static RadiationRuleCandidateStatus toCandidateStatus(
            RadiationRule rule,
            boolean active,
            String status,
            String severity) {
        return new RadiationRuleCandidateStatus(
                rule.type().id(),
                rule.id().toString(),
                rule.profile().id(),
                rule.required(),
                rule.optionalModId(),
                rule.role(),
                active,
                status,
                severity);
    }

    private static String registryStatus(RadiationRule rule) {
        Registry<?> registry = switch (rule.type()) {
            case ITEM -> BuiltInRegistries.ITEM;
            case BLOCK -> BuiltInRegistries.BLOCK;
            case FLUID -> BuiltInRegistries.FLUID;
        };
        if (registry.containsKey(rule.id())) {
            return "present";
        }
        if (rule.required()) {
            return "missing_required";
        }
        if (rule.optionalModId() != null && !ModList.get().isLoaded(rule.optionalModId())) {
            return "missing_optional_mod";
        }
        return "not_registered_optional";
    }

    private static String statusSeverity(String status) {
        return switch (status) {
            case "present", "missing_optional_mod", "suppressed_dev_profile", "disabled" -> "info";
            case "not_registered_optional" -> "warning";
            case "missing_required" -> "error";
            default -> "info";
        };
    }

    private static String checksum(List<RadiationRule> validRules) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(("validationMode=" + RadiationRules.VALIDATION_MODE + "\n").getBytes(StandardCharsets.UTF_8));

            List<RadiationRule> sortedRules = new ArrayList<>(validRules);
            sortedRules.sort(Comparator.comparing(RadiationRule::key).thenComparing(RadiationRule::source));

            for (RadiationRule rule : sortedRules) {
                String line = rule.key()
                        + "|strength=" + rule.strength()
                        + "|radius=" + rule.radius()
                        + "|respectsShielding=" + rule.respectsShielding()
                        + "|enabled=" + rule.enabled()
                        + "|profile=" + rule.profile().id()
                        + "|required=" + rule.required()
                        + "|optionalModId=" + (rule.optionalModId() == null ? "" : rule.optionalModId())
                        + "\n";
                digest.update(line.getBytes(StandardCharsets.UTF_8));
            }

            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private record ParsedRule(RadiationRule rule) {
    }

    private record ValidatedRules(
            List<RadiationRule> activeRules,
            int disabledRules,
            int activeAlwaysOrBetaRules,
            int activeDevRules,
            int suppressedDevRules,
            List<RadiationRuleCandidateStatus> candidateStatuses) {
    }
}
