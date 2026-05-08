package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
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
        source.sendSuccess(() -> Component.literal("RadWorks sources for "
                + player.getGameProfile().getName()
                + ": matchedSources="
                + sources.size()
                + " scope=player_inventory+static_blocks+vanilla_containers+block_item_handlers"), false);

        int shown = Math.min(CHAT_SOURCE_LIMIT, sources.size());
        for (int index = 0; index < shown; index++) {
            RadiationSource radiationSource = sources.get(index);
            source.sendSuccess(() -> Component.literal("- " + sourceRow(radiationSource)), false);
        }

        if (sources.size() > shown) {
            source.sendSuccess(() -> Component.literal("... and " + (sources.size() - shown) + " more"), false);
        }
        source.sendSuccess(() -> Component.literal("sourcesShown="
                + shown
                + " sourcesOmitted="
                + (sources.size() - shown)), false);
        source.sendSuccess(() -> Component.literal("Note: diagnostic only, player inventory, static block, vanilla Container and block ItemHandler sources only"), false);
        source.sendSuccess(() -> Component.literal("Note: vanilla Container block entities are skipped by itemHandlerScan to avoid double counting"), false);
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
        row.append(" reason=").append(source.matchReason());
        return row.toString();
    }
}
