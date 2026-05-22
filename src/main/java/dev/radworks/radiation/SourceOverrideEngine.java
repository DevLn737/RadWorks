package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.SourceOverrideDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class SourceOverrideEngine {
    private SourceOverrideEngine() {
    }

    public static ApplicationResult apply(
            RadiationTargetContext context,
            List<RadiationSource> sources,
            List<ForceSourceCandidate> candidates,
            SourceScanSummary.Builder summary,
            SourceOverrideDiagnostics.Builder diagnostics) {
        return applyForTargetKind(
                context.targetKind(),
                sources,
                candidates,
                SourceOverrideRulesLoader.currentRules(),
                summary,
                diagnostics);
    }

    static ApplicationResult applyForTargetKind(
            RadiationTargetKind targetKind,
            List<RadiationSource> sources,
            List<ForceSourceCandidate> candidates,
            SourceOverrideRules rules,
            SourceScanSummary.Builder summary,
            SourceOverrideDiagnostics.Builder diagnostics) {
        RuleBuckets ruleBuckets = bucketEnabledRules(rules.rules());
        boolean overridesEnabled = RadWorksConfig.sourceOverridesEnabled();
        boolean exclusionsEnabled = overridesEnabled && RadWorksConfig.sourceExclusionsEnabled();
        boolean containmentEnabled = overridesEnabled && RadWorksConfig.sourceContainmentEnabled();
        boolean forceEnabled = overridesEnabled && RadWorksConfig.forcedSourcesEnabled();

        if (!overridesEnabled) {
            for (int i = 0; i < ruleBuckets.enabledContainRules().size(); i++) {
                diagnostics.containRuleApplicationSkipped();
            }
            for (int i = 0; i < ruleBuckets.enabledForceRules().size(); i++) {
                diagnostics.forceRuleApplicationSkipped();
            }
            diagnostics.sample(
                    "disabled_by_config",
                    null,
                    null,
                    null,
                    0.0D,
                    0.0D,
                    "source override application is disabled by config");
            List<RadiationSource> kept = List.copyOf(sources);
            for (int i = 0; i < kept.size(); i++) {
                summary.sourceKeptAfterOverrides();
                summary.sourceAfterContainment();
                summary.sourceAfterForce();
                diagnostics.sourceKept();
            }
            return new ApplicationResult(kept, List.of(), List.of());
        }

        if (!exclusionsEnabled) {
            diagnostics.sample(
                    "exclude_disabled_by_config",
                    null,
                    null,
                    null,
                    0.0D,
                    0.0D,
                    "exclude application is disabled by config");
        }
        if (!containmentEnabled) {
            for (int i = 0; i < ruleBuckets.enabledContainRules().size(); i++) {
                diagnostics.containRuleApplicationSkipped();
            }
            diagnostics.sample(
                    "containment_disabled_by_config",
                    null,
                    null,
                    null,
                    0.0D,
                    0.0D,
                    "containment application is disabled by config");
        }
        if (!forceEnabled) {
            for (int i = 0; i < ruleBuckets.enabledForceRules().size(); i++) {
                diagnostics.forceRuleApplicationSkipped();
            }
            diagnostics.sample(
                    "force_disabled_by_config",
                    null,
                    null,
                    null,
                    0.0D,
                    0.0D,
                    "force application is disabled by config");
        }

        ExclusionResult exclusionResult = applyExclude(
                targetKind,
                sources,
                exclusionsEnabled ? ruleBuckets.enabledExcludeRules() : List.of(),
                summary,
                diagnostics);

        ContainmentPassResult normalContainment = applyContainmentPass(
                targetKind,
                exclusionResult.postExclude(),
                containmentEnabled ? ruleBuckets.enabledContainRules() : List.of(),
                summary,
                diagnostics,
                true);

        ForcePassResult forcePassResult = forceEnabled
                ? applyForce(
                        targetKind,
                        candidates,
                        ruleBuckets.enabledForceRules(),
                        ruleBuckets.enabledExcludeRules(),
                        exclusionResult.excluded(),
                        normalContainment.sourcesForShielding(),
                        normalContainment.suppressed(),
                        summary,
                        diagnostics)
                : new ForcePassResult(List.of(), 0);

        ContainmentPassResult forcedContainment = applyContainmentPass(
                targetKind,
                forcePassResult.forcedSources(),
                containmentEnabled ? ruleBuckets.enabledContainRules() : List.of(),
                summary,
                diagnostics,
                false);

        Set<String> appliedContainRuleIds = new HashSet<>();
        appliedContainRuleIds.addAll(normalContainment.appliedContainRuleIds());
        appliedContainRuleIds.addAll(forcedContainment.appliedContainRuleIds());
        for (int i = 0; i < exclusionResult.appliedExcludeRuleIds().size(); i++) {
            diagnostics.excludeRuleApplied();
        }
        for (int i = 0; i < appliedContainRuleIds.size(); i++) {
            diagnostics.containmentRuleApplied();
        }
        for (int i = 0; i < forcePassResult.appliedForceRuleCount(); i++) {
            diagnostics.forceRuleApplied();
        }

        List<RadiationSource> sourcesForShielding = new ArrayList<>(
                normalContainment.sourcesForShielding().size() + forcedContainment.sourcesForShielding().size());
        sourcesForShielding.addAll(normalContainment.sourcesForShielding());
        sourcesForShielding.addAll(forcedContainment.sourcesForShielding());

        List<RadiationSource> suppressed = new ArrayList<>(
                normalContainment.suppressed().size() + forcedContainment.suppressed().size());
        suppressed.addAll(normalContainment.suppressed());
        suppressed.addAll(forcedContainment.suppressed());

        for (int i = 0; i < sourcesForShielding.size(); i++) {
            summary.sourceAfterForce();
        }
        for (int i = 0; i < suppressed.size(); i++) {
            summary.sourceAfterForce();
        }

        return new ApplicationResult(
                List.copyOf(sourcesForShielding),
                List.copyOf(exclusionResult.excluded()),
                List.copyOf(suppressed));
    }

    static ApplicationResult applyForTargetKind(
            RadiationTargetKind targetKind,
            List<RadiationSource> sources,
            SourceOverrideRules rules,
            SourceScanSummary.Builder summary,
            SourceOverrideDiagnostics.Builder diagnostics) {
        return applyForTargetKind(targetKind, sources, List.of(), rules, summary, diagnostics);
    }

    private static ExclusionResult applyExclude(
            RadiationTargetKind targetKind,
            List<RadiationSource> sources,
            List<SourceOverrideRule> enabledExcludeRules,
            SourceScanSummary.Builder summary,
            SourceOverrideDiagnostics.Builder diagnostics) {
        List<RadiationSource> postExclude = new ArrayList<>();
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
                postExclude.add(source);
                continue;
            }
            appliedRuleIds.add(matchedRule.id().toString());
            RadiationSource excludedSource = source.withExcludedOverride(
                    matchedRule.id().toString(),
                    "excluded_by_override_rule");
            excluded.add(excludedSource);
            diagnostics.sourceExcluded();
            diagnostics.containmentSkippedBecauseExcluded();
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

        if (enabledExcludeRules.isEmpty()) {
            postExclude.clear();
            postExclude.addAll(sources);
        }
        return new ExclusionResult(
                List.copyOf(postExclude),
                List.copyOf(excluded),
                Set.copyOf(appliedRuleIds));
    }

    private static ContainmentPassResult applyContainmentPass(
            RadiationTargetKind targetKind,
            List<RadiationSource> input,
            List<SourceOverrideRule> containRules,
            SourceScanSummary.Builder summary,
            SourceOverrideDiagnostics.Builder diagnostics,
            boolean trackAfterContainmentInSummary) {
        List<RadiationSource> sourcesForShielding = new ArrayList<>();
        List<RadiationSource> suppressed = new ArrayList<>();
        Set<String> appliedContainRuleIds = new HashSet<>();

        for (RadiationSource source : input) {
            diagnostics.sourceCheckedForContainment();
            List<SourceOverrideRule> matches = new ArrayList<>();
            for (SourceOverrideRule rule : containRules) {
                SourceContainmentMode mode = rule.containmentMode();
                if (mode == null || (mode == SourceContainmentMode.SCALE && rule.containmentMultiplier() == null)) {
                    continue;
                }
                diagnostics.containmentRuleMatchAttempt();
                if (matches(rule, source, targetKind)) {
                    matches.add(rule);
                }
            }
            if (matches.isEmpty()) {
                sourcesForShielding.add(source);
                diagnostics.sourceKept();
                summary.sourceKeptAfterOverrides();
                if (trackAfterContainmentInSummary) {
                    summary.sourceAfterContainment();
                }
                continue;
            }

            ResolvedContainment resolved = resolveContainment(matches);
            SourceOverrideRule selectedRule = resolved.rule();
            SourceContainmentMode mode = selectedRule.containmentMode();
            double multiplier = mode == SourceContainmentMode.SUPPRESS
                    ? 0.0D
                    : selectedRule.containmentMultiplier();
            RadiationSource contained = source.withContainedOverride(
                    selectedRule.id().toString(),
                    mode,
                    multiplier,
                    mode == SourceContainmentMode.SUPPRESS
                            ? "contained_suppress_by_override_rule"
                            : "contained_scale_by_override_rule");
            diagnostics.sourceContained();
            summary.sourceContainedByOverride();
            appliedContainRuleIds.add(selectedRule.id().toString());

            if (resolved.conflictResolved()) {
                diagnostics.containmentConflictResolved();
                diagnostics.sample(
                        "contain_conflict_resolved",
                        selectedRule.id().toString(),
                        source.type().id(),
                        matchedSelectorsDescription(selectedRule.selectors()),
                        source.finalContribution(),
                        contained.finalContribution(),
                        "resolved from matching rules=" + matchedRuleIds(matches)
                                + " selectedMode=" + mode.id()
                                + " selectedMultiplier=" + multiplier);
            }

            if (mode == SourceContainmentMode.SUPPRESS) {
                diagnostics.containmentSuppressedSource();
                if (trackAfterContainmentInSummary) {
                    summary.sourceAfterContainment();
                }
                suppressed.add(contained);
                diagnostics.sample(
                        "contained_suppress",
                        selectedRule.id().toString(),
                        source.type().id(),
                        matchedSelectorsDescription(selectedRule.selectors()),
                        source.finalContribution(),
                        contained.finalContribution(),
                        "matched contain suppress rule");
                continue;
            }

            diagnostics.containmentScaledSource();
            sourcesForShielding.add(contained);
            diagnostics.sourceKept();
            summary.sourceKeptAfterOverrides();
            if (trackAfterContainmentInSummary) {
                summary.sourceAfterContainment();
            }
            diagnostics.sample(
                    "contained_scale",
                    selectedRule.id().toString(),
                    source.type().id(),
                    matchedSelectorsDescription(selectedRule.selectors()),
                    source.finalContribution(),
                    contained.finalContribution(),
                    "matched contain scale rule multiplier=" + multiplier);
        }

        return new ContainmentPassResult(
                List.copyOf(sourcesForShielding),
                List.copyOf(suppressed),
                Set.copyOf(appliedContainRuleIds));
    }

    private static ForcePassResult applyForce(
            RadiationTargetKind targetKind,
            List<ForceSourceCandidate> candidates,
            List<SourceOverrideRule> enabledForceRules,
            List<SourceOverrideRule> enabledExcludeRules,
            List<RadiationSource> excludedNormal,
            List<RadiationSource> keptNormalAfterContainment,
            List<RadiationSource> suppressedNormalAfterContainment,
            SourceScanSummary.Builder summary,
            SourceOverrideDiagnostics.Builder diagnostics) {
        List<RadiationSource> forcedSources = new ArrayList<>();
        Set<SourceIdentityKey> existing = new HashSet<>();
        Set<SourceIdentityKey> excluded = new HashSet<>();
        for (RadiationSource source : keptNormalAfterContainment) {
            existing.add(identityOf(source, targetKind));
        }
        for (RadiationSource source : suppressedNormalAfterContainment) {
            existing.add(identityOf(source, targetKind));
        }
        for (RadiationSource source : excludedNormal) {
            excluded.add(identityOf(source, targetKind));
        }

        Set<String> appliedForceRuleIds = new HashSet<>();
        for (ForceSourceCandidate candidate : candidates) {
            diagnostics.forceCandidateObserved();
            summary.forceCandidateObserved();

            SourceOverrideRule match = null;
            for (SourceOverrideRule rule : enabledForceRules) {
                if (!isValidForceRule(rule)) {
                    diagnostics.forceCandidateSkippedInvalidRule();
                    diagnostics.sample(
                            "force_invalid_rule",
                            rule.id().toString(),
                            candidate.sourceType().id(),
                            matchedSelectorsDescription(rule.selectors()),
                            0.0D,
                            0.0D,
                            "invalid force runtime fields");
                    continue;
                }
                if (!hasConcreteSelector(rule.selectors())) {
                    diagnostics.forceCandidateSkippedNoConcreteSelector();
                    diagnostics.sample(
                            "force_skipped_no_concrete_selector",
                            rule.id().toString(),
                            candidate.sourceType().id(),
                            matchedSelectorsDescription(rule.selectors()),
                            0.0D,
                            0.0D,
                            "force rule has no concrete selector");
                    continue;
                }
                if (matches(rule, candidate, targetKind)) {
                    match = rule;
                    break;
                }
            }
            if (match == null) {
                diagnostics.sample(
                        "force_candidate_no_match",
                        null,
                        candidate.sourceType().id(),
                        null,
                        0.0D,
                        0.0D,
                        candidate.candidateReason());
                continue;
            }

            RadiationSource forced = materializeForcedSource(candidate, match);
            if (forced == null) {
                diagnostics.forceCandidateSkippedInvalidRule();
                diagnostics.sample(
                        "force_invalid_rule",
                        match.id().toString(),
                        candidate.sourceType().id(),
                        matchedSelectorsDescription(match.selectors()),
                        0.0D,
                        0.0D,
                        "failed to materialize forced source");
                continue;
            }

            SourceIdentityKey identity = identityOf(forced, targetKind);
            if (existing.contains(identity)) {
                diagnostics.forceCandidateSkippedExistingSource();
                diagnostics.sample(
                        "force_skipped_existing_source",
                        match.id().toString(),
                        forced.type().id(),
                        matchedSelectorsDescription(match.selectors()),
                        0.0D,
                        0.0D,
                        "identity already present");
                continue;
            }
            if (excluded.contains(identity) || matchesAnyExclude(enabledExcludeRules, forced, targetKind)) {
                diagnostics.forceCandidateSkippedExcluded();
                diagnostics.sample(
                        "force_skipped_excluded_identity",
                        match.id().toString(),
                        forced.type().id(),
                        matchedSelectorsDescription(match.selectors()),
                        0.0D,
                        0.0D,
                        "identity excluded by override");
                continue;
            }

            forcedSources.add(forced);
            existing.add(identity);
            appliedForceRuleIds.add(match.id().toString());
            summary.forcedSourceAdded();
            diagnostics.forcedSourceAdded();
            diagnostics.sample(
                    "forced_source_added",
                    match.id().toString(),
                    forced.type().id(),
                    matchedSelectorsDescription(match.selectors()),
                    0.0D,
                    forced.finalContribution(),
                    "candidateKind=" + candidate.candidateKind().id()
                            + " forceUnitMode=" + match.forceUnitMode().id());
        }

        return new ForcePassResult(List.copyOf(forcedSources), appliedForceRuleIds.size());
    }

    private static RadiationSource materializeForcedSource(ForceSourceCandidate candidate, SourceOverrideRule rule) {
        double baseRadius = rule.forceRadius();
        double effectiveRadius;
        double contribution;
        switch (rule.forceUnitMode()) {
            case ITEM_COUNT -> {
                int count = Math.max(1, candidate.count());
                double units = DynamicRadiusModel.aggregateUnitsForItems(count);
                effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
                contribution = rule.forceStrength() * count;
            }
            case FLUID_MB -> {
                int amountMb = Math.max(1, candidate.amountMb());
                double units = DynamicRadiusModel.aggregateUnitsForFluids(amountMb);
                effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
                contribution = rule.forceStrength() * ((double) amountMb / 1000.0D);
            }
            case BLOCK, FIXED -> {
                effectiveRadius = baseRadius;
                contribution = rule.forceStrength();
            }
            default -> {
                return null;
            }
        }
        return RadiationSource.forcedFromCandidate(
                candidate,
                rule,
                baseRadius,
                effectiveRadius,
                contribution,
                "Forced source from observed candidate reason="
                        + candidate.candidateReason()
                        + " candidateKind="
                        + candidate.candidateKind().id()
                        + " forceUnitMode="
                        + rule.forceUnitMode().id());
    }

    private static boolean isValidForceRule(SourceOverrideRule rule) {
        return rule.forceStrength() != null
                && rule.forceStrength() > 0.0D
                && rule.forceRadius() != null
                && rule.forceRadius() > 0.0D
                && rule.forceUnitMode() != null;
    }

    private static boolean hasConcreteSelector(SourceOverrideRuleSelector selectors) {
        return selectors.itemId() != null
                || selectors.blockId() != null
                || selectors.fluidId() != null
                || selectors.containerItemId() != null
                || selectors.carrierBlockId() != null
                || selectors.carrierEntityType() != null;
    }

    private static boolean matchesAnyExclude(
            List<SourceOverrideRule> excludeRules,
            RadiationSource source,
            RadiationTargetKind targetKind) {
        for (SourceOverrideRule exclude : excludeRules) {
            if (matches(exclude, source, targetKind)) {
                return true;
            }
        }
        return false;
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

    private static boolean matches(
            SourceOverrideRule rule,
            ForceSourceCandidate candidate,
            RadiationTargetKind targetKind) {
        SourceOverrideRuleSelector selectors = rule.selectors();
        if (selectors.sourceType() != null && selectors.sourceType() != candidate.sourceType()) {
            return false;
        }
        if (selectors.blockId() != null) {
            if (candidate.blockId() == null || !selectors.blockId().equals(candidate.blockId())) {
                return false;
            }
        }
        if (selectors.itemId() != null) {
            if (candidate.itemId() == null || !selectors.itemId().equals(candidate.itemId())) {
                return false;
            }
        }
        if (selectors.fluidId() != null) {
            if (candidate.fluidId() == null || !selectors.fluidId().equals(candidate.fluidId())) {
                return false;
            }
        }
        if (selectors.carrierEntityType() != null) {
            if (candidate.carrierEntityType() == null) {
                return false;
            }
            String normalizedSource = candidate.carrierEntityType().toLowerCase(Locale.ROOT);
            if (!selectors.carrierEntityType().toString().equals(normalizedSource)) {
                return false;
            }
        }
        if (selectors.containerItemId() != null) {
            if (candidate.containerItemId() == null || !selectors.containerItemId().equals(candidate.containerItemId())) {
                return false;
            }
        }
        if (selectors.carrierBlockId() != null) {
            if (candidate.carrierBlockId() == null || !selectors.carrierBlockId().equals(candidate.carrierBlockId())) {
                return false;
            }
        }
        if (selectors.targetKind() != null && selectors.targetKind() != targetKind) {
            return false;
        }
        return true;
    }

    private static SourceIdentityKey identityOf(RadiationSource source, RadiationTargetKind targetKind) {
        String position = source.position() == null
                ? null
                : source.position().getX() + "," + source.position().getY() + "," + source.position().getZ();
        return new SourceIdentityKey(
                source.type(),
                source.blockId(),
                source.itemId(),
                source.fluidId(),
                position,
                source.carrierEntityId(),
                source.carrierEntityType(),
                source.containerItemId(),
                source.containerPath(),
                targetKind);
    }

    private static String matchedSelectorsDescription(SourceOverrideRuleSelector selectors) {
        return selectors.toJson().toString();
    }

    private static String matchedRuleIds(List<SourceOverrideRule> rules) {
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < rules.size(); index++) {
            if (index > 0) {
                sb.append(",");
            }
            sb.append(rules.get(index).id());
        }
        return sb.toString();
    }

    private static ResolvedContainment resolveContainment(List<SourceOverrideRule> matches) {
        SourceOverrideRule selected = matches.stream()
                .min(Comparator
                        .comparingInt((SourceOverrideRule rule) -> rule.containmentMode() == SourceContainmentMode.SUPPRESS ? 0 : 1)
                        .thenComparingDouble(rule -> {
                            if (rule.containmentMode() == SourceContainmentMode.SUPPRESS) {
                                return 0.0D;
                            }
                            return rule.containmentMultiplier();
                        })
                        .thenComparing(rule -> rule.id().toString()))
                .orElseThrow();
        return new ResolvedContainment(selected, matches.size() > 1);
    }

    private static RuleBuckets bucketEnabledRules(List<SourceOverrideRule> rules) {
        List<SourceOverrideRule> excludes = new ArrayList<>();
        List<SourceOverrideRule> contains = new ArrayList<>();
        List<SourceOverrideRule> forces = new ArrayList<>();
        for (SourceOverrideRule rule : rules) {
            if (!rule.enabled()) {
                continue;
            }
            if (rule.type() == SourceOverrideRuleType.EXCLUDE) {
                excludes.add(rule);
            } else if (rule.type() == SourceOverrideRuleType.CONTAIN) {
                contains.add(rule);
            } else if (rule.type() == SourceOverrideRuleType.FORCE) {
                forces.add(rule);
            }
        }
        return new RuleBuckets(List.copyOf(excludes), List.copyOf(contains), List.copyOf(forces));
    }

    public record ApplicationResult(
            List<RadiationSource> sourcesForShielding,
            List<RadiationSource> excludedSources,
            List<RadiationSource> containedSuppressedSources) {
    }

    private record RuleBuckets(
            List<SourceOverrideRule> enabledExcludeRules,
            List<SourceOverrideRule> enabledContainRules,
            List<SourceOverrideRule> enabledForceRules) {
    }

    private record ExclusionResult(
            List<RadiationSource> postExclude,
            List<RadiationSource> excluded,
            Set<String> appliedExcludeRuleIds) {
    }

    private record ContainmentPassResult(
            List<RadiationSource> sourcesForShielding,
            List<RadiationSource> suppressed,
            Set<String> appliedContainRuleIds) {
    }

    private record ForcePassResult(List<RadiationSource> forcedSources, int appliedForceRuleCount) {
    }

    private record ResolvedContainment(SourceOverrideRule rule, boolean conflictResolved) {
    }

    private record SourceIdentityKey(
            RadiationSourceType sourceType,
            ResourceLocation blockId,
            ResourceLocation itemId,
            ResourceLocation fluidId,
            String position,
            String carrierEntityId,
            String carrierEntityType,
            ResourceLocation containerItemId,
            String containerPath,
            RadiationTargetKind targetKind) {
    }
}

