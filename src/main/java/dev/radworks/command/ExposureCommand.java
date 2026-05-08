package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
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
                + " matchedSources="
                + breakdown.sources().size()), false);

        int shown = Math.min(CHAT_SOURCE_LIMIT, breakdown.sources().size());
        for (int index = 0; index < shown; index++) {
            RadiationSource radiationSource = breakdown.sources().get(index);
            source.sendSuccess(() -> Component.literal("- " + sourceRow(radiationSource)), false);
        }

        if (breakdown.sources().size() > shown) {
            source.sendSuccess(() -> Component.literal("... and " + (breakdown.sources().size() - shown) + " more"), false);
        }
        SourceScanSummary.updateOutputBounds(shown, breakdown.sources().size() - shown);
        source.sendSuccess(() -> Component.literal("sourcesShown="
                + shown
                + " sourcesOmitted="
                + (breakdown.sources().size() - shown)), false);
        source.sendSuccess(() -> Component.literal("Note: " + breakdown.notes()), false);
        return 1;
    }

    private static String sourceRow(RadiationSource source) {
        StringBuilder row = new StringBuilder();
        row.append("type=").append(source.type().id());
        if (source.itemId() != null) {
            row.append(" itemId=").append(source.itemId());
        }
        if (source.blockId() != null) {
            row.append(" blockId=").append(source.blockId());
        }
        if (source.position() != null) {
            if (source.type() == dev.radworks.radiation.RadiationSourceType.BLOCK_ENTITY_INVENTORY) {
                row.append(" containerPos=");
            } else {
                row.append(" position=");
            }
            row.append(source.position().getX())
                    .append(",")
                    .append(source.position().getY())
                    .append(",")
                    .append(source.position().getZ());
        }
        if (source.capabilityContext() != null) {
            row.append(" capabilityContext=").append(source.capabilityContext());
        }
        if (source.slot() != null) {
            row.append(" slot=").append(source.slot());
        }
        if (source.count() > 0) {
            row.append(" count=").append(source.count());
        }
        row.append(" distance=").append(source.distance());
        row.append(" ruleRadius=").append(source.ruleRadius());
        row.append(" ruleStrength=").append(source.ruleStrength());
        row.append(" contribution=").append(source.contribution());
        row.append(" shielding=").append(source.shielding());
        return row.toString();
    }
}
