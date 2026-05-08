package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.WarningBuffer;
import dev.radworks.radiation.RadiationRuleValidationResult;
import dev.radworks.radiation.RadiationRules;
import dev.radworks.radiation.RadiationRulesLoader;
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
        recordValidationIssues(validation);
        source.sendSuccess(() -> Component.literal("RadWorks rules validation: loaded="
                + (rules.activeRules().size() + rules.disabledRules())
                + " enabled="
                + rules.activeRules().size()
                + " disabled="
                + rules.disabledRules()
                + " errors="
                + validation.errors().size()
                + " warnings="
                + validation.warnings().size()
                + " mode="
                + RadiationRules.VALIDATION_MODE
                + " checksum="
                + rules.shortChecksum()), false);

        sendIssues(source, "ERROR", validation.errors());
        sendIssues(source, "WARNING", validation.warnings());
        sendIssues(source, "INFO", validation.infos());
        return validation.hasErrors() ? 0 : 1;
    }

    private static void recordValidationIssues(RadiationRuleValidationResult validation) {
        for (RadiationRuleValidationResult.Issue issue : validation.errors()) {
            WarningBuffer.add(issue.category(), "validate:" + issue.source(), issue.message());
        }
        for (RadiationRuleValidationResult.Issue issue : validation.warnings()) {
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
