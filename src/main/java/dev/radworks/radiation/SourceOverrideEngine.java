package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.SourceOverrideDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SourceOverrideEngine {
    private SourceOverrideEngine() {
    }

    public static ApplicationResult apply(
            RadiationTargetContext context,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary,
            SourceOverrideDiagnostics.Builder diagnostics) {
        return applyForTargetKind(
                context.targetKind(),
                sources,
                SourceOverrideRulesLoader.currentRules(),
                summary,
                diagnostics);
    }

    static ApplicationResult applyForTargetKind(
            RadiationTargetKind targetKind,
            List<RadiationSource> sources,
            SourceOverrideRules rules,
            SourceScanSummary.Builder summary,
            SourceOverrideDiagnostics.Builder diagnostics) {
        List<SourceOverrideRule> enabledExcludeRules = new ArrayList<>();
        int enabledContainRules = 0;
        int enabledForceRules = 0;
        for (SourceOverrideRule rule : rules.rules()) {
            if (!rule.enabled()) {
                continue;
            }
            if (rule.type() == SourceOverrideRuleType.EXCLUDE) {
                enabledExcludeRules.add(rule);
            } else if (rule.type() == SourceOverrideRuleType.CONTAIN) {
                enabledContainRules++;
            } else if (rule.type() == SourceOverrideRuleType.FORCE) {
                enabledForceRules++;
            }
        }
        for (int i = 0; i < enabledContainRules; i++) {
            diagnostics.containRuleApplicationSkipped();
        }
        for (int i = 0; i < enabledForceRules; i++) {
            diagnostics.forceRuleApplicationSkipped();
        }

        if (!RadWorksConfig.sourceOverridesEnabled() || !RadWorksConfig.sourceExclusionsEnabled()) {
            diagnostics.sample(
                    "disabled_by_config",
                    null,
                    null,
                    null,
                    0.0D,
                    0.0D,
                    "source override exclusion application is disabled by config");
            for (int i = 0; i < sources.size(); i++) {
                summary.sourceKeptAfterOverrides();
                diagnostics.sourceKept();
            }
            return new ApplicationResult(List.copyOf(sources), List.of());
        }

        List<RadiationSource> kept = new ArrayList<>();
        List<RadiationSource> excluded = new ArrayList<>();
        Set<String> appliedRuleIds = new HashSet<>();
        for (RadiationSource source : sources) {
            diagnostics.sourceChecked();
            SourceOverrideRule matchedRule = null;
            for (SourceOverrideRule rule : enabledExcludeRules) {
                diagnostics.excludeRuleMatchAttempt();
                if (matches(rule, source, targetKind)) {
                    matchedRule = rule;
                    break;
                }
            }

            if (matchedRule == null) {
                kept.add(source);
                diagnostics.sourceKept();
                summary.sourceKeptAfterOverrides();
                continue;
            }

            appliedRuleIds.add(matchedRule.id().toString());
            RadiationSource excludedSource = source.withExcludedOverride(
                    matchedRule.id().toString(),
                    "excluded_by_override_rule");
            excluded.add(excludedSource);
            diagnostics.sourceExcluded();
            summary.sourceExcludedByOverride();
            diagnostics.sample(
                    "excluded_source",
                    matchedRule.id().toString(),
                    source.type().id(),
                    matchedSelectorsDescription(matchedRule.selectors()),
                    source.finalContribution(),
                    excludedSource.finalContribution(),
                    "matched exclude rule");
        }

        for (int i = 0; i < appliedRuleIds.size(); i++) {
            diagnostics.excludeRuleApplied();
        }
        return new ApplicationResult(List.copyOf(kept), List.copyOf(excluded));
    }

    private static boolean matches(
            SourceOverrideRule rule,
            RadiationSource source,
            RadiationTargetKind targetKind) {
        SourceOverrideRuleSelector selectors = rule.selectors();
        if (selectors.sourceType() != null && selectors.sourceType() != source.type()) {
            return false;
        }
        if (selectors.blockId() != null && !selectors.blockId().equals(source.blockId())) {
            return false;
        }
        if (selectors.itemId() != null && !selectors.itemId().equals(source.itemId())) {
            return false;
        }
        if (selectors.fluidId() != null && !selectors.fluidId().equals(source.fluidId())) {
            return false;
        }
        if (selectors.carrierEntityType() != null) {
            if (source.carrierEntityType() == null) {
                return false;
            }
            String normalizedSource = source.carrierEntityType().toLowerCase(Locale.ROOT);
            if (!selectors.carrierEntityType().toString().equals(normalizedSource)) {
                return false;
            }
        }
        if (selectors.containerItemId() != null && !selectors.containerItemId().equals(source.containerItemId())) {
            return false;
        }
        if (selectors.carrierBlockId() != null && !selectors.carrierBlockId().equals(source.blockId())) {
            return false;
        }
        if (selectors.targetKind() != null && selectors.targetKind() != targetKind) {
            return false;
        }
        return true;
    }

    private static String matchedSelectorsDescription(SourceOverrideRuleSelector selectors) {
        return selectors.toJson().toString();
    }

    public record ApplicationResult(List<RadiationSource> sourcesForShielding, List<RadiationSource> excludedSources) {
    }
}
