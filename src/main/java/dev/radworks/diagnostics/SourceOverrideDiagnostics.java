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
            json.addProperty("sourcesCheckedForContainment", 0);
            json.addProperty("sourcesExcluded", 0);
            json.addProperty("sourcesContained", 0);
            json.addProperty("sourcesKeptAfterOverrides", 0);
            json.addProperty("excludeRulesApplied", 0);
            json.addProperty("excludeRuleMatchAttempts", 0);
            json.addProperty("containmentRulesApplied", 0);
            json.addProperty("containmentRuleMatchAttempts", 0);
            json.addProperty("containmentSuppressedSources", 0);
            json.addProperty("containmentScaledSources", 0);
            json.addProperty("containmentConflictsResolved", 0);
            json.addProperty("containmentSkippedBecauseExcluded", 0);
            json.addProperty("forceCandidatesObserved", 0);
            json.addProperty("forceRulesApplied", 0);
            json.addProperty("forcedSourcesAdded", 0);
            json.addProperty("forceCandidatesSkippedExistingSource", 0);
            json.addProperty("forceCandidatesSkippedExcluded", 0);
            json.addProperty("forceCandidatesSkippedInvalidRule", 0);
            json.addProperty("forceCandidatesSkippedNoConcreteSelector", 0);
            json.addProperty("containRulesApplicationSkipped", 0);
            json.addProperty("forceRulesApplicationSkipped", 0);
            json.addProperty("applicationRuntimeAvailable", false);
            json.add("applicationSamples", new JsonArray());
            return json;
        }
        json.addProperty("applicationRuntimeAvailable", true);
        json.addProperty("createdAt", snapshot.createdAt.toString());
        json.addProperty("sourcesCheckedForOverrides", snapshot.sourcesCheckedForOverrides);
        json.addProperty("sourcesCheckedForContainment", snapshot.sourcesCheckedForContainment);
        json.addProperty("sourcesExcluded", snapshot.sourcesExcluded);
        json.addProperty("sourcesContained", snapshot.sourcesContained);
        json.addProperty("sourcesKeptAfterOverrides", snapshot.sourcesKeptAfterOverrides);
        json.addProperty("excludeRulesApplied", snapshot.excludeRulesApplied);
        json.addProperty("excludeRuleMatchAttempts", snapshot.excludeRuleMatchAttempts);
        json.addProperty("containmentRulesApplied", snapshot.containmentRulesApplied);
        json.addProperty("containmentRuleMatchAttempts", snapshot.containmentRuleMatchAttempts);
        json.addProperty("containmentSuppressedSources", snapshot.containmentSuppressedSources);
        json.addProperty("containmentScaledSources", snapshot.containmentScaledSources);
        json.addProperty("containmentConflictsResolved", snapshot.containmentConflictsResolved);
        json.addProperty("containmentSkippedBecauseExcluded", snapshot.containmentSkippedBecauseExcluded);
        json.addProperty("forceCandidatesObserved", snapshot.forceCandidatesObserved);
        json.addProperty("forceRulesApplied", snapshot.forceRulesApplied);
        json.addProperty("forcedSourcesAdded", snapshot.forcedSourcesAdded);
        json.addProperty("forceCandidatesSkippedExistingSource", snapshot.forceCandidatesSkippedExistingSource);
        json.addProperty("forceCandidatesSkippedExcluded", snapshot.forceCandidatesSkippedExcluded);
        json.addProperty("forceCandidatesSkippedInvalidRule", snapshot.forceCandidatesSkippedInvalidRule);
        json.addProperty("forceCandidatesSkippedNoConcreteSelector", snapshot.forceCandidatesSkippedNoConcreteSelector);
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
        private int sourcesCheckedForContainment;
        private int sourcesExcluded;
        private int sourcesContained;
        private int sourcesKeptAfterOverrides;
        private int excludeRulesApplied;
        private int excludeRuleMatchAttempts;
        private int containmentRulesApplied;
        private int containmentRuleMatchAttempts;
        private int containmentSuppressedSources;
        private int containmentScaledSources;
        private int containmentConflictsResolved;
        private int containmentSkippedBecauseExcluded;
        private int forceCandidatesObserved;
        private int forceRulesApplied;
        private int forcedSourcesAdded;
        private int forceCandidatesSkippedExistingSource;
        private int forceCandidatesSkippedExcluded;
        private int forceCandidatesSkippedInvalidRule;
        private int forceCandidatesSkippedNoConcreteSelector;
        private int containRulesApplicationSkipped;
        private int forceRulesApplicationSkipped;

        public void sourceChecked() {
            sourcesCheckedForOverrides++;
        }

        public void sourceExcluded() {
            sourcesExcluded++;
        }

        public void sourceCheckedForContainment() {
            sourcesCheckedForContainment++;
        }

        public void sourceContained() {
            sourcesContained++;
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

        public void containmentRuleApplied() {
            containmentRulesApplied++;
        }

        public void containmentRuleMatchAttempt() {
            containmentRuleMatchAttempts++;
        }

        public void containmentSuppressedSource() {
            containmentSuppressedSources++;
        }

        public void containmentScaledSource() {
            containmentScaledSources++;
        }

        public void containmentConflictResolved() {
            containmentConflictsResolved++;
        }

        public void containmentSkippedBecauseExcluded() {
            containmentSkippedBecauseExcluded++;
        }

        public void forceCandidateObserved() {
            forceCandidatesObserved++;
        }

        public void forceRuleApplied() {
            forceRulesApplied++;
        }

        public void forcedSourceAdded() {
            forcedSourcesAdded++;
        }

        public void forceCandidateSkippedExistingSource() {
            forceCandidatesSkippedExistingSource++;
        }

        public void forceCandidateSkippedExcluded() {
            forceCandidatesSkippedExcluded++;
        }

        public void forceCandidateSkippedInvalidRule() {
            forceCandidatesSkippedInvalidRule++;
        }

        public void forceCandidateSkippedNoConcreteSelector() {
            forceCandidatesSkippedNoConcreteSelector++;
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
                    sourcesCheckedForContainment,
                    sourcesExcluded,
                    sourcesContained,
                    sourcesKeptAfterOverrides,
                    excludeRulesApplied,
                    excludeRuleMatchAttempts,
                    containmentRulesApplied,
                    containmentRuleMatchAttempts,
                    containmentSuppressedSources,
                    containmentScaledSources,
                    containmentConflictsResolved,
                    containmentSkippedBecauseExcluded,
                    forceCandidatesObserved,
                    forceRulesApplied,
                    forcedSourcesAdded,
                    forceCandidatesSkippedExistingSource,
                    forceCandidatesSkippedExcluded,
                    forceCandidatesSkippedInvalidRule,
                    forceCandidatesSkippedNoConcreteSelector,
                    containRulesApplicationSkipped,
                    forceRulesApplicationSkipped,
                    List.copyOf(samples));
        }
    }

    private record Snapshot(
            Instant createdAt,
            int sourcesCheckedForOverrides,
            int sourcesCheckedForContainment,
            int sourcesExcluded,
            int sourcesContained,
            int sourcesKeptAfterOverrides,
            int excludeRulesApplied,
            int excludeRuleMatchAttempts,
            int containmentRulesApplied,
            int containmentRuleMatchAttempts,
            int containmentSuppressedSources,
            int containmentScaledSources,
            int containmentConflictsResolved,
            int containmentSkippedBecauseExcluded,
            int forceCandidatesObserved,
            int forceRulesApplied,
            int forcedSourcesAdded,
            int forceCandidatesSkippedExistingSource,
            int forceCandidatesSkippedExcluded,
            int forceCandidatesSkippedInvalidRule,
            int forceCandidatesSkippedNoConcreteSelector,
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
