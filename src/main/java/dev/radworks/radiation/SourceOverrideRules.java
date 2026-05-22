package dev.radworks.radiation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import java.util.List;

public final class SourceOverrideRules {
    public static final String VALIDATION_MODE = "schema_only/beta_0_6_1";
    public static final String APPLICATION_PHASE = "exclude_contain_force_applied_beta_0_6_4";

    private static final SourceOverrideRules NOT_LOADED = new SourceOverrideRules(
            false,
            "not_loaded",
            List.of(),
            new SourceOverrideRuleValidationResult(),
            0,
            List.of());

    private final boolean loaded;
    private final String checksum;
    private final List<SourceOverrideRule> rules;
    private final SourceOverrideRuleValidationResult validationResult;
    private final int missingOptionalRuleTargets;
    private final List<SourceOverrideDiagnosticsSample> samples;

    public SourceOverrideRules(
            boolean loaded,
            String checksum,
            List<SourceOverrideRule> rules,
            SourceOverrideRuleValidationResult validationResult,
            int missingOptionalRuleTargets,
            List<SourceOverrideDiagnosticsSample> samples) {
        this.loaded = loaded;
        this.checksum = checksum;
        this.rules = List.copyOf(rules);
        this.validationResult = validationResult;
        this.missingOptionalRuleTargets = Math.max(0, missingOptionalRuleTargets);
        this.samples = List.copyOf(samples);
    }

    public static SourceOverrideRules notLoaded() {
        return NOT_LOADED;
    }

    public boolean loaded() {
        return loaded;
    }

    public String checksum() {
        return checksum;
    }

    public List<SourceOverrideRule> rules() {
        return rules;
    }

    public SourceOverrideRuleValidationResult validationResult() {
        return validationResult;
    }

    public int missingOptionalRuleTargets() {
        return missingOptionalRuleTargets;
    }

    public List<SourceOverrideDiagnosticsSample> samples() {
        return samples;
    }

    public int overrideRulesLoaded() {
        return rules.size();
    }

    public int overrideRulesEnabled() {
        int count = 0;
        for (SourceOverrideRule rule : rules) {
            if (rule.enabled()) {
                count++;
            }
        }
        return count;
    }

    public int overrideRulesDisabled() {
        return overrideRulesLoaded() - overrideRulesEnabled();
    }

    public int excludeRulesLoaded() {
        return countType(SourceOverrideRuleType.EXCLUDE);
    }

    public int containRulesLoaded() {
        return countType(SourceOverrideRuleType.CONTAIN);
    }

    public int forceRulesLoaded() {
        return countType(SourceOverrideRuleType.FORCE);
    }

    public JsonObject toDiagnosticsJson() {
        JsonObject json = new JsonObject();
        json.addProperty("loaded", loaded);
        json.addProperty("checksum", checksum);
        json.addProperty("validationMode", VALIDATION_MODE);
        json.addProperty("applicationPhase", APPLICATION_PHASE);
        json.addProperty("overrideRulesLoaded", overrideRulesLoaded());
        json.addProperty("overrideRulesEnabled", overrideRulesEnabled());
        json.addProperty("overrideRulesDisabled", overrideRulesDisabled());
        json.addProperty("excludeRulesLoaded", excludeRulesLoaded());
        json.addProperty("containRulesLoaded", containRulesLoaded());
        json.addProperty("forceRulesLoaded", forceRulesLoaded());
        json.addProperty("missingOptionalRuleTargets", missingOptionalRuleTargets);
        json.addProperty("overrideRuleWarnings", validationResult.warnings().size());
        json.addProperty("overrideRuleErrors", validationResult.errors().size());
        json.addProperty("excludeApplicationActive", true);
        json.addProperty("containApplicationActive", true);
        json.addProperty("forceApplicationActive", true);

        JsonObject config = new JsonObject();
        config.addProperty("sourceOverridesEnabled", RadWorksConfig.sourceOverridesEnabled());
        config.addProperty("sourceExclusionsEnabled", RadWorksConfig.sourceExclusionsEnabled());
        config.addProperty("sourceContainmentEnabled", RadWorksConfig.sourceContainmentEnabled());
        config.addProperty("forcedSourcesEnabled", RadWorksConfig.forcedSourcesEnabled());
        config.addProperty("sourceOverrideDiagnosticSampleCap", RadWorksConfig.sourceOverrideDiagnosticSampleCap());
        json.add("config", config);

        json.add("errors", validationResult.toJson().getAsJsonArray("errors"));
        json.add("warnings", validationResult.toJson().getAsJsonArray("warnings"));
        json.add("infos", validationResult.toJson().getAsJsonArray("infos"));

        JsonArray sampleArray = new JsonArray();
        for (SourceOverrideDiagnosticsSample sample : samples) {
            sampleArray.add(sample.toJson());
        }
        json.add("samples", sampleArray);
        return json;
    }

    private int countType(SourceOverrideRuleType type) {
        int count = 0;
        for (SourceOverrideRule rule : rules) {
            if (rule.type() == type) {
                count++;
            }
        }
        return count;
    }

    public record SourceOverrideDiagnosticsSample(
            String sampleType,
            String source,
            String message,
            String ruleId,
            boolean enabled) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("sampleType", sampleType);
            json.addProperty("source", source);
            json.addProperty("message", message);
            if (ruleId != null) {
                json.addProperty("ruleId", ruleId);
            }
            json.addProperty("enabled", enabled);
            return json;
        }
    }
}
