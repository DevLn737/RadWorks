package dev.radworks.diagnostics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import dev.radworks.radiation.SourceOverrideRules;
import dev.radworks.radiation.SourceOverrideRulesLoader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SourceOverrideDiagnostics {
    private static volatile Snapshot lastSnapshot;

    private SourceOverrideDiagnostics() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static synchronized void store(Builder builder) {
        lastSnapshot = builder.build();
    }

    public static synchronized JsonObject toJson() {
        SourceOverrideRules rules = SourceOverrideRulesLoader.currentRules();
        JsonObject json = rules.toDiagnosticsJson();
        Snapshot snapshot = lastSnapshot;
        if (snapshot == null) {
            json.addProperty("sourcesCheckedForOverrides", 0);
            json.addProperty("sourcesExcluded", 0);
            json.addProperty("sourcesKeptAfterOverrides", 0);
            json.addProperty("excludeRulesApplied", 0);
            json.addProperty("excludeRuleMatchAttempts", 0);
            json.addProperty("containRulesApplicationSkipped", 0);
            json.addProperty("forceRulesApplicationSkipped", 0);
            json.addProperty("applicationRuntimeAvailable", false);
            json.add("applicationSamples", new JsonArray());
            return json;
        }
        json.addProperty("applicationRuntimeAvailable", true);
        json.addProperty("createdAt", snapshot.createdAt.toString());
        json.addProperty("sourcesCheckedForOverrides", snapshot.sourcesCheckedForOverrides);
        json.addProperty("sourcesExcluded", snapshot.sourcesExcluded);
        json.addProperty("sourcesKeptAfterOverrides", snapshot.sourcesKeptAfterOverrides);
        json.addProperty("excludeRulesApplied", snapshot.excludeRulesApplied);
        json.addProperty("excludeRuleMatchAttempts", snapshot.excludeRuleMatchAttempts);
        json.addProperty("containRulesApplicationSkipped", snapshot.containRulesApplicationSkipped);
        json.addProperty("forceRulesApplicationSkipped", snapshot.forceRulesApplicationSkipped);
        JsonArray samples = new JsonArray();
        for (Sample sample : snapshot.samples) {
            samples.add(sample.toJson());
        }
        json.add("applicationSamples", samples);
        return json;
    }

    public static final class Builder {
        private final List<Sample> samples = new ArrayList<>();
        private int sourcesCheckedForOverrides;
        private int sourcesExcluded;
        private int sourcesKeptAfterOverrides;
        private int excludeRulesApplied;
        private int excludeRuleMatchAttempts;
        private int containRulesApplicationSkipped;
        private int forceRulesApplicationSkipped;

        public void sourceChecked() {
            sourcesCheckedForOverrides++;
        }

        public void sourceExcluded() {
            sourcesExcluded++;
        }

        public void sourceKept() {
            sourcesKeptAfterOverrides++;
        }

        public void excludeRuleApplied() {
            excludeRulesApplied++;
        }

        public void excludeRuleMatchAttempt() {
            excludeRuleMatchAttempts++;
        }

        public void containRuleApplicationSkipped() {
            containRulesApplicationSkipped++;
        }

        public void forceRuleApplicationSkipped() {
            forceRulesApplicationSkipped++;
        }

        public void sample(
                String sampleType,
                String ruleId,
                String sourceType,
                String matchedSelectors,
                double originalContribution,
                double finalContribution,
                String reason) {
            int cap = Math.max(1, RadWorksConfig.sourceOverrideDiagnosticSampleCap());
            if (samples.size() >= cap) {
                return;
            }
            samples.add(new Sample(
                    sampleType,
                    ruleId,
                    sourceType,
                    matchedSelectors,
                    originalContribution,
                    finalContribution,
                    reason));
        }

        private Snapshot build() {
            return new Snapshot(
                    Instant.now(),
                    sourcesCheckedForOverrides,
                    sourcesExcluded,
                    sourcesKeptAfterOverrides,
                    excludeRulesApplied,
                    excludeRuleMatchAttempts,
                    containRulesApplicationSkipped,
                    forceRulesApplicationSkipped,
                    List.copyOf(samples));
        }
    }

    private record Snapshot(
            Instant createdAt,
            int sourcesCheckedForOverrides,
            int sourcesExcluded,
            int sourcesKeptAfterOverrides,
            int excludeRulesApplied,
            int excludeRuleMatchAttempts,
            int containRulesApplicationSkipped,
            int forceRulesApplicationSkipped,
            List<Sample> samples) {
    }

    private record Sample(
            String sampleType,
            String ruleId,
            String sourceType,
            String matchedSelectors,
            double originalContribution,
            double finalContribution,
            String reason) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("sampleType", sampleType);
            if (ruleId != null) {
                json.addProperty("ruleId", ruleId);
            }
            if (sourceType != null) {
                json.addProperty("sourceType", sourceType);
            }
            if (matchedSelectors != null) {
                json.addProperty("matchedSelectors", matchedSelectors);
            }
            json.addProperty("originalContribution", originalContribution);
            json.addProperty("finalContribution", finalContribution);
            if (reason != null) {
                json.addProperty("reason", reason);
            }
            return json;
        }
    }
}
