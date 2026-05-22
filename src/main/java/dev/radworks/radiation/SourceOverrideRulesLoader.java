package dev.radworks.radiation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.radworks.radiation.SourceOverrideRules.SourceOverrideDiagnosticsSample;
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
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class SourceOverrideRulesLoader extends SimplePreparableReloadListener<SourceOverrideRules> {
    private static final Gson GSON = new Gson();
    private static final FileToIdConverter RULE_FILES = FileToIdConverter.json("source_override_rules");
    private static volatile SourceOverrideRules currentRules = SourceOverrideRules.notLoaded();

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SourceOverrideRulesLoader());
    }

    public static SourceOverrideRules currentRules() {
        return currentRules;
    }

    @Override
    protected SourceOverrideRules prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, JsonObject> docs = new HashMap<>();
        SourceOverrideRuleValidationResult validation = new SourceOverrideRuleValidationResult();
        for (Map.Entry<ResourceLocation, Resource> entry : RULE_FILES.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation fileId = RULE_FILES.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                if (json == null) {
                    validation.error("MALFORMED_JSON", fileId.toString(), "JSON object is empty");
                    continue;
                }
                docs.put(fileId.toString(), json);
            } catch (IOException | JsonParseException exception) {
                validation.error("MALFORMED_JSON", fileId.toString(), exception.getMessage());
            }
        }
        SourceOverrideRules parsed = parseForTests(docs);
        SourceOverrideRuleValidationResult merged = mergeValidation(validation, parsed.validationResult());
        return new SourceOverrideRules(
                parsed.loaded(),
                parsed.checksum(),
                parsed.rules(),
                merged,
                parsed.missingOptionalRuleTargets(),
                parsed.samples());
    }

    @Override
    protected void apply(SourceOverrideRules rules, ResourceManager resourceManager, ProfilerFiller profiler) {
        currentRules = rules;
    }

    static SourceOverrideRules parseForTests(Map<String, JsonObject> docs) {
        SourceOverrideRuleValidationResult validation = new SourceOverrideRuleValidationResult();
        List<SourceOverrideRule> rules = new ArrayList<>();
        List<SourceOverrideDiagnosticsSample> samples = new ArrayList<>();
        int missingOptionalTargets = 0;

        for (Map.Entry<String, JsonObject> entry : docs.entrySet()) {
            SourceOverrideRule rule = parseRule(entry.getValue(), entry.getKey(), validation);
            if (rule == null) {
                continue;
            }
            rules.add(rule);
            addSample(samples, new SourceOverrideDiagnosticsSample(
                    rule.enabled() ? "loaded_rule" : "disabled_rule",
                    rule.source(),
                    "Loaded source override rule",
                    rule.id().toString(),
                    rule.enabled()));
            if (rule.type() == SourceOverrideRuleType.FORCE) {
                addSample(samples, new SourceOverrideDiagnosticsSample(
                        "force_schema_only",
                        rule.source(),
                        "Force rules are not applied in beta 0.6.2; application starts in beta 0.6.4",
                        rule.id().toString(),
                        rule.enabled()));
            }
        }

        Map<ResourceLocation, SourceOverrideRule> byId = new HashMap<>();
        for (SourceOverrideRule rule : rules) {
            SourceOverrideRule existing = byId.putIfAbsent(rule.id(), rule);
            if (existing != null) {
                validation.warning(
                        "DUPLICATE_RULE_ID",
                        rule.source(),
                        "Duplicate override rule id: " + rule.id());
                addSample(samples, new SourceOverrideDiagnosticsSample(
                        "conflict_candidate",
                        rule.source(),
                        "Duplicate override rule id conflicts with " + existing.source(),
                        rule.id().toString(),
                        rule.enabled()));
            }

            if (!rule.selectors().hasAnySelector()) {
                validation.error(
                        "INVALID_SELECTOR",
                        rule.source(),
                        "At least one selector is required");
                addSample(samples, new SourceOverrideDiagnosticsSample(
                        "invalid_selector",
                        rule.source(),
                        "At least one selector is required",
                        rule.id().toString(),
                        rule.enabled()));
            }

            if (rule.optionalModId() != null && !ModList.get().isLoaded(rule.optionalModId())) {
                missingOptionalTargets++;
                String message = "optional mod '" + rule.optionalModId() + "' is not loaded";
                if (rule.required()) {
                    validation.warning("MISSING_REQUIRED_MOD", rule.source(), message);
                } else {
                    validation.info("MISSING_OPTIONAL_RULE_TARGET", rule.source(), message);
                }
                addSample(samples, new SourceOverrideDiagnosticsSample(
                        "missing_optional_target",
                        rule.source(),
                        message,
                        rule.id().toString(),
                        rule.enabled()));
            }

            if (rule.type() == SourceOverrideRuleType.CONTAIN) {
                validateContainRule(rule, validation, samples);
            } else if (rule.containmentMode() != null || rule.containmentMultiplier() != null) {
                validation.warning(
                        "UNUSED_CONTAIN_FIELDS",
                        rule.source(),
                        "Contain fields mode/multiplier are ignored for non-contain rules");
            }
        }

        return new SourceOverrideRules(
                true,
                checksum(rules),
                rules,
                validation,
                missingOptionalTargets,
                List.copyOf(samples));
    }

    private static SourceOverrideRule parseRule(
            JsonObject json,
            String source,
            SourceOverrideRuleValidationResult validation) {
        ResourceLocation id = readRuleId(json, source, validation);
        SourceOverrideRuleType type = readType(json, source, validation);
        Boolean enabled = readOptionalBoolean(json, "enabled", true, source, validation);
        SourceOverrideRuleSelector selectors = readSelectors(json, source, validation);
        Boolean required = readOptionalBoolean(json, "required", false, source, validation);
        String optionalModId = readOptionalModId(json, source, validation);
        String description = readOptionalString(json, "description", "");

        SourceContainmentMode mode = null;
        Double multiplier = null;
        if (type == SourceOverrideRuleType.CONTAIN) {
            mode = readContainMode(json, source, validation);
            multiplier = readContainMultiplier(json, source, validation);
        } else {
            if (json.has("mode")) {
                mode = readContainMode(json, source, validation);
            }
            if (json.has("multiplier")) {
                multiplier = readContainMultiplier(json, source, validation);
            }
        }

        if (id == null || type == null || enabled == null || selectors == null || required == null) {
            return null;
        }

        return new SourceOverrideRule(
                id,
                enabled,
                type,
                selectors,
                required,
                optionalModId,
                description,
                mode,
                multiplier,
                source);
    }

    private static ResourceLocation readRuleId(
            JsonObject json,
            String source,
            SourceOverrideRuleValidationResult validation) {
        if (!json.has("id")) {
            validation.error("MALFORMED_JSON", source, "Missing required field: id");
            return null;
        }
        String rawId = GsonHelper.getAsString(json, "id");
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null) {
            validation.error("INVALID_RULE_VALUE", source, "Invalid rule id: " + rawId);
        }
        return id;
    }

    private static SourceOverrideRuleType readType(
            JsonObject json,
            String source,
            SourceOverrideRuleValidationResult validation) {
        if (!json.has("type")) {
            validation.error("MALFORMED_JSON", source, "Missing required field: type");
            return null;
        }
        String rawType = GsonHelper.getAsString(json, "type");
        return SourceOverrideRuleType.fromId(rawType).orElseGet(() -> {
            validation.error("INVALID_RULE_VALUE", source, "Unknown override rule type: " + rawType);
            return null;
        });
    }

    private static SourceOverrideRuleSelector readSelectors(
            JsonObject json,
            String source,
            SourceOverrideRuleValidationResult validation) {
        if (!json.has("selectors") || !json.get("selectors").isJsonObject()) {
            validation.error("MALFORMED_JSON", source, "Missing required object field: selectors");
            return null;
        }
        JsonObject selectors = json.getAsJsonObject("selectors");

        RadiationSourceType sourceType = readSourceTypeSelector(selectors, "sourceType", source, validation);
        ResourceLocation blockId = readResourceLocationSelector(selectors, "blockId", source, validation);
        ResourceLocation itemId = readResourceLocationSelector(selectors, "itemId", source, validation);
        ResourceLocation fluidId = readResourceLocationSelector(selectors, "fluidId", source, validation);
        ResourceLocation carrierEntityType = readResourceLocationSelector(selectors, "carrierEntityType", source, validation);
        ResourceLocation containerItemId = readResourceLocationSelector(selectors, "containerItemId", source, validation);
        ResourceLocation carrierBlockId = readResourceLocationSelector(selectors, "carrierBlockId", source, validation);
        RadiationTargetKind targetKind = readTargetKindSelector(selectors, "targetKind", source, validation);
        return new SourceOverrideRuleSelector(
                sourceType,
                blockId,
                itemId,
                fluidId,
                carrierEntityType,
                containerItemId,
                carrierBlockId,
                targetKind);
    }

    private static RadiationSourceType readSourceTypeSelector(
            JsonObject selectors,
            String field,
            String source,
            SourceOverrideRuleValidationResult validation) {
        if (!selectors.has(field)) {
            return null;
        }
        String raw = GsonHelper.getAsString(selectors, field);
        return RadiationSourceType.fromId(raw).orElseGet(() -> {
            validation.error("INVALID_SELECTOR", source, "Unknown selector sourceType: " + raw);
            return null;
        });
    }

    private static RadiationTargetKind readTargetKindSelector(
            JsonObject selectors,
            String field,
            String source,
            SourceOverrideRuleValidationResult validation) {
        if (!selectors.has(field)) {
            return null;
        }
        String raw = GsonHelper.getAsString(selectors, field);
        return RadiationTargetKind.fromId(raw).orElseGet(() -> {
            validation.error("INVALID_SELECTOR", source, "Unknown selector targetKind: " + raw);
            return null;
        });
    }

    private static ResourceLocation readResourceLocationSelector(
            JsonObject selectors,
            String field,
            String source,
            SourceOverrideRuleValidationResult validation) {
        if (!selectors.has(field)) {
            return null;
        }
        String raw = GsonHelper.getAsString(selectors, field);
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            validation.error("INVALID_SELECTOR", source, "Invalid selector " + field + " id: " + raw);
        }
        return id;
    }

    private static SourceContainmentMode readContainMode(
            JsonObject json,
            String source,
            SourceOverrideRuleValidationResult validation) {
        if (!json.has("mode")) {
            return null;
        }
        String rawMode = GsonHelper.getAsString(json, "mode");
        return SourceContainmentMode.fromId(rawMode).orElseGet(() -> {
            validation.error("INVALID_RULE_VALUE", source, "Unknown contain mode: " + rawMode);
            return null;
        });
    }

    private static Double readContainMultiplier(
            JsonObject json,
            String source,
            SourceOverrideRuleValidationResult validation) {
        if (!json.has("multiplier")) {
            return null;
        }
        double value;
        try {
            value = GsonHelper.getAsDouble(json, "multiplier");
        } catch (RuntimeException exception) {
            validation.error("INVALID_RULE_VALUE", source, "Contain multiplier must be numeric");
            return null;
        }
        if (value < 0.0D || value > 1.0D) {
            validation.error(
                    "INVALID_CONTAIN_MULTIPLIER",
                    source,
                    "Contain multiplier must be in range [0.0, 1.0]");
            return null;
        }
        return value;
    }

    private static Boolean readOptionalBoolean(
            JsonObject json,
            String field,
            boolean fallback,
            String source,
            SourceOverrideRuleValidationResult validation) {
        if (!json.has(field)) {
            return fallback;
        }
        try {
            return GsonHelper.getAsBoolean(json, field);
        } catch (RuntimeException exception) {
            validation.error("INVALID_RULE_VALUE", source, "Field " + field + " must be true or false");
            return null;
        }
    }

    private static String readOptionalModId(
            JsonObject json,
            String source,
            SourceOverrideRuleValidationResult validation) {
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

    private static String readOptionalString(JsonObject json, String field, String fallback) {
        if (!json.has(field)) {
            return fallback;
        }
        return GsonHelper.getAsString(json, field);
    }

    private static void validateContainRule(
            SourceOverrideRule rule,
            SourceOverrideRuleValidationResult validation,
            List<SourceOverrideDiagnosticsSample> samples) {
        if (rule.containmentMode() == null) {
            validation.error("INVALID_CONTAIN_RULE", rule.source(), "Contain rule requires mode=suppress|scale");
            addSample(samples, new SourceOverrideDiagnosticsSample(
                    "invalid_contain_mode",
                    rule.source(),
                    "Contain rule requires mode=suppress|scale",
                    rule.id().toString(),
                    rule.enabled()));
            return;
        }
        if (rule.containmentMode() == SourceContainmentMode.SCALE) {
            if (rule.containmentMultiplier() == null) {
                validation.error(
                        "INVALID_CONTAIN_RULE",
                        rule.source(),
                        "Contain scale rule requires multiplier in range [0.0, 1.0]");
                addSample(samples, new SourceOverrideDiagnosticsSample(
                        "invalid_contain_multiplier",
                        rule.source(),
                        "Contain scale rule requires multiplier in range [0.0, 1.0]",
                        rule.id().toString(),
                        rule.enabled()));
            }
            return;
        }
        if (rule.containmentMode() == SourceContainmentMode.SUPPRESS && rule.containmentMultiplier() != null) {
            validation.warning(
                    "UNUSED_CONTAIN_MULTIPLIER",
                    rule.source(),
                    "Contain suppress rule ignores multiplier");
        }
    }

    private static SourceOverrideRuleValidationResult mergeValidation(
            SourceOverrideRuleValidationResult first,
            SourceOverrideRuleValidationResult second) {
        SourceOverrideRuleValidationResult merged = new SourceOverrideRuleValidationResult();
        for (SourceOverrideRuleValidationResult.Issue issue : first.errors()) {
            merged.error(issue.category(), issue.source(), issue.message());
        }
        for (SourceOverrideRuleValidationResult.Issue issue : first.warnings()) {
            merged.warning(issue.category(), issue.source(), issue.message());
        }
        for (SourceOverrideRuleValidationResult.Issue issue : first.infos()) {
            merged.info(issue.category(), issue.source(), issue.message());
        }
        for (SourceOverrideRuleValidationResult.Issue issue : second.errors()) {
            merged.error(issue.category(), issue.source(), issue.message());
        }
        for (SourceOverrideRuleValidationResult.Issue issue : second.warnings()) {
            merged.warning(issue.category(), issue.source(), issue.message());
        }
        for (SourceOverrideRuleValidationResult.Issue issue : second.infos()) {
            merged.info(issue.category(), issue.source(), issue.message());
        }
        return merged;
    }

    private static void addSample(List<SourceOverrideDiagnosticsSample> samples, SourceOverrideDiagnosticsSample sample) {
        int cap = Math.max(1, dev.radworks.config.RadWorksConfig.sourceOverrideDiagnosticSampleCap());
        if (samples.size() >= cap) {
            return;
        }
        samples.add(sample);
    }

    private static String checksum(List<SourceOverrideRule> rules) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            rules.stream()
                    .sorted(Comparator.comparing(rule -> rule.id().toString()))
                    .map(rule -> rule.toJson().toString())
                    .forEach(json -> digest.update(json.getBytes(StandardCharsets.UTF_8)));
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return "checksum_unavailable";
        }
    }
}
