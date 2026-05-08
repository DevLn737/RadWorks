package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.WarningBuffer;
import dev.radworks.radiation.ExposureBreakdown;
import dev.radworks.radiation.ExposureEngine;
import dev.radworks.radiation.RadiationSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ExposureCommand {
    private static final int CHAT_SOURCE_LIMIT = 10;

    private ExposureCommand() {
    }

    public static int runSelf(CommandSourceStack source) {
        return PerformanceStats.timeCommand("exposure", () -> runSelfTimed(source));
    }

    private static int runSelfTimed(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return runTimed(source, player);
        }

        String message = "player required; use /radworks exposure <player>";
        WarningBuffer.add("COMMAND_MISUSE", "exposure", message);
        source.sendFailure(Component.literal(message));
        return 0;
    }

    public static int run(CommandSourceStack source, ServerPlayer player) {
        return PerformanceStats.timeCommand("exposure", () -> runTimed(source, player));
    }

    private static int runTimed(CommandSourceStack source, ServerPlayer player) {
        ExposureBreakdown breakdown = ExposureEngine.calculate(player);
        source.sendSuccess(() -> Component.literal("RadWorks exposure for "
                + breakdown.playerName()
                + ": totalExposure="
                + breakdown.totalExposure()
                + " matchedStacks="
                + breakdown.matchedStacks()), false);

        int shown = Math.min(CHAT_SOURCE_LIMIT, breakdown.sources().size());
        for (int index = 0; index < shown; index++) {
            RadiationSource radiationSource = breakdown.sources().get(index);
            source.sendSuccess(() -> Component.literal("- "
                    + radiationSource.itemId()
                    + " slot="
                    + radiationSource.slot()
                    + " count="
                    + radiationSource.count()
                    + " strength="
                    + radiationSource.ruleStrength()
                    + " radius="
                    + radiationSource.ruleRadius()
                    + " contribution="
                    + radiationSource.contribution()
                    + " shielding="
                    + radiationSource.shielding()), false);
        }

        if (breakdown.sources().size() > shown) {
            source.sendSuccess(() -> Component.literal("... and " + (breakdown.sources().size() - shown) + " more"), false);
        }
        source.sendSuccess(() -> Component.literal("Note: " + breakdown.notes()), false);
        return 1;
    }
}
