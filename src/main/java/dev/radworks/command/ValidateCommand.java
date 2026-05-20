package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.WarningBuffer;
import dev.radworks.config.RadWorksConfig;
import dev.radworks.radiation.RadiationRuleValidationResult;
import dev.radworks.radiation.RadiationRules;
import dev.radworks.radiation.RadiationRulesLoader;
import dev.radworks.radiation.effects.EffectStrategyResult;
import dev.radworks.radiation.effects.EffectStrategyService;
import dev.radworks.radiation.shielding.ShieldingDiagnostics;
import net.neoforged.fml.ModList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class ValidateCommand {
    private static final int ISSUE_LIMIT = 8;

    private ValidateCommand() {
    }

    public static int run(CommandSourceStack source) {
        return PerformanceStats.timeCommand("validate", () -> runTimed(source));
    }

    private static int runTimed(CommandSourceStack source) {
        RadiationRules rules = RadiationRulesLoader.currentRules();
        if (!rules.loaded()) {
            String message = "RadWorks rules are not loaded yet. Start a world or run /reload, then try again.";
            WarningBuffer.add("RULES_NOT_LOADED", "validate", message);
            source.sendFailure(Component.literal(message));
            return 0;
        }

        RadiationRuleValidationResult validation = rules.validationResult();
        ShieldingDiagnostics.Report shieldingReport = ShieldingDiagnostics.report();
        EffectStrategyResult effectStrategy = EffectStrategyService.strategy();
        recordValidationIssues(validation);
        recordShieldingWarnings(shieldingReport);
        int totalRules = rules.activeRules().size() + rules.disabledRules() + rules.suppressedDevRules();
        int optionalPresent = rules.candidateCount("present");
        int optionalMissing = rules.candidateCount("missing_optional_mod");
        int optionalWarnings = rules.candidateCount("not_registered_optional");
        String devRulesState = RadWorksConfig.enableDevRules() ? "ON" : "OFF";

        source.sendSuccess(() -> Component.literal("[RadWorks] Validate"), false);
        source.sendSuccess(() -> Component.literal("Rules: "
                + (validation.hasErrors() ? "FAILED" : "OK")
                + " errors="
                + validation.errors().size()
                + " warnings="
                + validation.warnings().size()
                + " infos="
                + validation.infos().size()
                + " active="
                + rules.activeRules().size()
                + " total="
                + totalRules
                + " devRules="
                + devRulesState
                + " activeDev="
                + rules.activeDevRules()
                + " suppressedDev="
                + rules.suppressedDevRules()), false);
        source.sendSuccess(() -> Component.literal("Effects: mode="
                + effectStrategy.effectMode()
                + " selected="
                + valueOrDash(effectStrategy.selectedRuntimeEffectId())
                + " registered="
                + effectStrategy.selectedRuntimeEffectRegistered()
                + " externalPresent="
                + effectStrategy.externalEffectPresent()
                + " fallback="
                + effectStrategy.fallbackReason()), false);
        source.sendSuccess(() -> Component.literal("Gameplay: enabled="
                + RadWorksConfig.gameplayEnabled()
                + " autoApply="
                + RadWorksConfig.autoApplyEffect()
                + " applyPlayers="
                + RadWorksConfig.applyEffectToPlayers()
                + " applyLiving="
                + RadWorksConfig.applyEffectToLivingEntities()
                + " applyMobs="
                + RadWorksConfig.applyEffectToMobs()
                + " applyArmorStands="
                + RadWorksConfig.applyEffectToArmorStands()
                + " alwaysRadiusVisual="
                + RadWorksConfig.alwaysShowRadiusVisualization()
                + " threshold="
                + RadWorksConfig.exposureThreshold()
                + " interval="
                + RadWorksConfig.scanIntervalTicks()
                + "t duration="
                + RadWorksConfig.effectDurationTicks()
                + "t livingRadius="
                + RadWorksConfig.livingTargetScanRadius()
                + " livingCap="
                + RadWorksConfig.maxLivingTargetsPerScan()
                + " damage="
                + RadWorksConfig.damageEnabled()), false);
        source.sendSuccess(() -> Component.literal("Shielding: tag=#"
                + ShieldingDiagnostics.TAG_ID
                + " present="
                + shieldingReport.presentCount()
                + " missingOptionalMods="
                + shieldingReport.missingOptionalModCount()
                + " warnings="
                + shieldingReport.warnings().size()), false);
        source.sendSuccess(() -> Component.literal("Optional candidates: present="
                + optionalPresent
                + " missingOptional="
                + optionalMissing
                + " warnings="
                + optionalWarnings), false);
        source.sendSuccess(() -> Component.literal("Dynamic radius: enabled="
                + RadWorksConfig.dynamicRadiusEnabled()
                + " scale="
                + RadWorksConfig.dynamicRadiusScale()
                + " maxCap="
                + RadWorksConfig.dynamicRadiusMaxCap()
                + " formula="
                + RadWorksConfig.dynamicRadiusFormulaLabel()), false);
        source.sendSuccess(() -> Component.literal("Create transient carriers: createLoaded="
                + ModList.get().isLoaded("create")
                + " enabled="
                + RadWorksConfig.createTransientCarriersEnabled()
                + " nbtScan="
                + RadWorksConfig.createTransientCarrierNbtScanEnabled()
                + " maxScanRadius="
                + RadWorksConfig.createTransientCarrierMaxScanRadius()
                + " worldFluidDiscoveryRadius="
                + RadWorksConfig.worldFluidClusterDiscoveryRadius()
                + " diagCap="
                + RadWorksConfig.createTransientCarrierDiagnosticSampleCap()
                + " pathCap="
                + RadWorksConfig.createTransientCarrierPathSampleCap()), false);
        source.sendSuccess(() -> Component.literal(
                "Create patterns: placard(Item), mechanical_arm(HeldItem), fluid_pipe(side.Flow.Fluid), glass_fluid_pipe(side.Flow.Fluid), pipette(known paths)"),
                false);
        source.sendSuccess(() -> Component.literal("Fluid coverage: createnuclear:uranium="
                + candidateStatus(rules, "fluid", "createnuclear:uranium")
                + " createnuclear:flowing_uranium="
                + candidateStatus(rules, "fluid", "createnuclear:flowing_uranium")), false);
        source.sendSuccess(() -> Component.literal("Entity carriers: enabled="
                + RadWorksConfig.entityCarriersEnabled()
                + " droppedItems="
                + RadWorksConfig.entityDroppedItemsEnabled()
                + " itemFrames="
                + RadWorksConfig.entityItemFramesEnabled()
                + " playerAura="
                + RadWorksConfig.entityPlayerAuraEnabled()
                + " chestBoats="
                + RadWorksConfig.entityChestBoatsEnabled()
                + " packAnimals="
                + RadWorksConfig.entityPackAnimalsEnabled()
                + " genericCapability="
                + RadWorksConfig.entityGenericInventoryCapabilityEnabled()
                + " maxScanRadius="
                + RadWorksConfig.entityCarrierMaxScanRadius()
                + " diagCap="
                + RadWorksConfig.entityCarrierDiagnosticSampleCap()
                + " inventoryDiagCap="
                + RadWorksConfig.entityInventoryDiagnosticSampleCap()), false);
        source.sendSuccess(() -> Component.literal(
                "Living targets: decisions are bounded and available in /radworks dump -> gameplay.livingEntityEffectDecisions"),
                false);
        source.sendSuccess(() -> Component.literal("Dump: use /radworks dump for full details"), false);

        sendIssues(source, "ERROR", validation.errors());
        sendIssues(source, "WARNING", validation.warnings());
        sendIssues(source, "INFO", validation.infos());
        sendIssues(source, "SHIELDING WARNING", shieldingReport.warnings());
        sendIssues(source, "SHIELDING INFO", shieldingReport.infos());
        return validation.hasErrors() ? 0 : 1;
    }

    private static String valueOrDash(String value) {
        return value == null ? "-" : value;
    }

    private static String candidateStatus(RadiationRules rules, String type, String id) {
        for (var candidate : rules.candidateStatuses()) {
            if (candidate.type().equals(type) && candidate.id().equals(id)) {
                return candidate.status();
            }
        }
        return "missing_rule";
    }

    private static void recordValidationIssues(RadiationRuleValidationResult validation) {
        for (RadiationRuleValidationResult.Issue issue : validation.errors()) {
            WarningBuffer.add(issue.category(), "validate:" + issue.source(), issue.message());
        }
        for (RadiationRuleValidationResult.Issue issue : validation.warnings()) {
            WarningBuffer.add(issue.category(), "validate:" + issue.source(), issue.message());
        }
    }

    private static void recordShieldingWarnings(ShieldingDiagnostics.Report report) {
        for (RadiationRuleValidationResult.Issue issue : report.warnings()) {
            WarningBuffer.add(issue.category(), "validate:" + issue.source(), issue.message());
        }
    }

    private static void sendIssues(
            CommandSourceStack source,
            String level,
            java.util.List<RadiationRuleValidationResult.Issue> issues) {
        int shown = Math.min(issues.size(), ISSUE_LIMIT);
        for (int index = 0; index < shown; index++) {
            RadiationRuleValidationResult.Issue issue = issues.get(index);
            source.sendSuccess(() -> Component.literal(level + " " + issue.summary()), false);
        }

        if (issues.size() > shown) {
            source.sendSuccess(() -> Component.literal(level + " ... " + (issues.size() - shown) + " more"), false);
        }
    }
}
