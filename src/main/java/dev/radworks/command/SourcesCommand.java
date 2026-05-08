package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.WarningBuffer;
import dev.radworks.radiation.PlayerInventorySourceProvider;
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

        List<RadiationSource> sources = PlayerInventorySourceProvider.collect(player, rules);
        source.sendSuccess(() -> Component.literal("RadWorks sources for "
                + player.getGameProfile().getName()
                + ": matchedSources="
                + sources.size()
                + " scope=player_inventory"), false);

        int shown = Math.min(CHAT_SOURCE_LIMIT, sources.size());
        for (int index = 0; index < shown; index++) {
            RadiationSource radiationSource = sources.get(index);
            source.sendSuccess(() -> Component.literal("- "
                    + radiationSource.itemId()
                    + " type="
                    + radiationSource.type().id()
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
                    + " reason="
                    + radiationSource.matchReason()), false);
        }

        if (sources.size() > shown) {
            source.sendSuccess(() -> Component.literal("... and " + (sources.size() - shown) + " more"), false);
        }
        source.sendSuccess(() -> Component.literal("Note: diagnostic only, player inventory sources only"), false);
        return 1;
    }
}
