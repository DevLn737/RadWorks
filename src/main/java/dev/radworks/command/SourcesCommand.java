package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WarningBuffer;
import dev.radworks.radiation.ExposureEngine;
import dev.radworks.radiation.RadiationRules;
import dev.radworks.radiation.RadiationRulesLoader;
import dev.radworks.radiation.RadiationSource;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SourcesCommand {
    private static final int CHAT_SOURCE_LIMIT = 10;

    private SourcesCommand() {
    }

    public static int runSelf(CommandSourceStack source) {
        return PerformanceStats.timeCommand("sources", () -> runSelfTimed(source));
    }

    private static int runSelfTimed(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return runTimed(source, player);
        }

        String message = "player required; use /radworks sources <player>";
        WarningBuffer.add("COMMAND_MISUSE", "sources", message);
        source.sendFailure(Component.literal(message));
        return 0;
    }

    public static int run(CommandSourceStack source, ServerPlayer player) {
        return PerformanceStats.timeCommand("sources", () -> runTimed(source, player));
    }

    private static int runTimed(CommandSourceStack source, ServerPlayer player) {
        RadiationRules rules = RadiationRulesLoader.currentRules();
        if (!rules.loaded()) {
            String message = "RadWorks rules are not loaded yet. Start a world or run /reload, then try again.";
            WarningBuffer.add("RULES_NOT_LOADED", "sources", message);
            source.sendFailure(Component.literal(message));
            return 0;
        }

        List<RadiationSource> sources = ExposureEngine.collectSources(player, rules);
        source.sendSuccess(() -> Component.literal("[RadWorks] Sources"), false);
        source.sendSuccess(() -> Component.literal("Total sources: "
                + sources.size()
                + " scope=player_inventory+block+block_entity_inventory+block_item_handler+block_fluid_handler+create_transient_item+create_transient_fluid"), false);

        int shown = Math.min(CHAT_SOURCE_LIMIT, sources.size());
        source.sendSuccess(() -> Component.literal("Rows:"), false);
        for (int index = 0; index < shown; index++) {
            RadiationSource radiationSource = sources.get(index);
            source.sendSuccess(() -> Component.literal("- " + sourceRow(radiationSource)), false);
        }

        if (sources.size() > shown) {
            source.sendSuccess(() -> Component.literal("... and " + (sources.size() - shown) + " more (see /radworks dump)"), false);
        }
        SourceScanSummary.updateOutputBounds(shown, sources.size() - shown);
        source.sendSuccess(() -> Component.literal("Output: sourcesShown="
                + shown
                + " sourcesOmitted="
                + (sources.size() - shown)), false);
        source.sendSuccess(() -> Component.literal("Note: use /radworks dump for full rule + handler diagnostics"), false);
        return 1;
    }

    private static String sourceRow(RadiationSource source) {
        StringBuilder row = new StringBuilder();
        row.append("type=").append(source.type().id());
        if (source.itemId() != null) {
            row.append(" itemId=").append(source.itemId());
        }
        if (source.fluidId() != null) {
            row.append(" fluidId=").append(source.fluidId());
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
        if (source.carrierKind() != null) {
            row.append(" carrierKind=").append(source.carrierKind());
        }
        if (source.dataPath() != null) {
            row.append(" dataPath=").append(source.dataPath());
        }
        if (source.slot() != null) {
            row.append(" slot=").append(source.slot());
        }
        if (source.tank() != null) {
            row.append(" tank=").append(source.tank());
        }
        if (source.count() > 0) {
            row.append(" count=").append(source.count());
        }
        if (source.amountMb() > 0) {
            row.append(" amountMb=").append(source.amountMb());
        }
        if (source.aggregateCount() > 0) {
            row.append(" aggregateCount=").append(source.aggregateCount());
        }
        if (source.aggregateAmountMb() > 0) {
            row.append(" aggregateAmountMb=").append(source.aggregateAmountMb());
        }
        if (source.contributingStacks() > 0) {
            row.append(" contributingStacks=").append(source.contributingStacks());
        }
        row.append(" distance=").append(round(source.distance()));
        row.append(" effectiveRadius=").append(round(source.effectiveRadius()));
        row.append(" strength=").append(source.ruleStrength());
        row.append(" final=").append(source.finalContribution());
        if (source.ruleMatchMode() != null && !source.ruleMatchMode().isBlank()) {
            row.append(" ruleMatch=").append(source.ruleMatchMode());
        }
        row.append(" reason=").append(source.matchReason());
        return row.toString();
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
