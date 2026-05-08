package dev.radworks.command;

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
        RadiationRules rules = RadiationRulesLoader.currentRules();
        if (!rules.loaded()) {
            source.sendFailure(Component.literal("RadWorks rules are not loaded yet. Start a world or run /reload, then try again."));
            return 0;
        }

        RadiationRuleValidationResult validation = rules.validationResult();
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
