package dev.radworks.command;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.RadiusVisualizationSamples;
import dev.radworks.diagnostics.RadiusVisualizationService;
import dev.radworks.diagnostics.WarningBuffer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RadiusCommand {
    private RadiusCommand() {
    }

    public static int showDefault(CommandSourceStack source) {
        return show(source, RadiusVisualizationSamples.DEFAULT_DURATION_SECONDS);
    }

    public static int show(CommandSourceStack source, int requestedSeconds) {
        return PerformanceStats.timeCommand("radius_visualization", () -> {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                return failPlayerRequired(source, "show");
            }

            RadiusVisualizationService.ShowResult result = RadiusVisualizationService.show(player, requestedSeconds);
            source.sendSuccess(() -> Component.literal("[RadWorks] Radius visualization"), false);
            source.sendSuccess(() -> Component.literal("Result: visualizedSources="
                    + result.visualizedSources()
                    + " skippedSources="
                    + result.skippedSources()
                    + " durationSeconds="
                    + result.durationSeconds()
                    + " maxRadiusSeen="
                    + round(result.maxRadiusSeen())), false);
            source.sendSuccess(() -> Component.literal("Note: " + result.note()), false);
            if (result.skippedSources() > 0) {
                source.sendSuccess(() -> Component.literal("Skipped note: player_inventory_has_no_world_radius_visual or source cap reached"), false);
            }
            return 1;
        });
    }

    public static int clear(CommandSourceStack source) {
        return PerformanceStats.timeCommand("radius_visualization", () -> {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                return failPlayerRequired(source, "clear");
            }

            boolean cleared = RadiusVisualizationService.clear(player);
            source.sendSuccess(() -> Component.literal("[RadWorks] Radius visualization clear: activeBeforeClear=" + cleared), false);
            if (RadWorksConfig.alwaysShowRadiusVisualization()) {
                source.sendSuccess(() -> Component.literal("Note: alwaysShowRadiusVisualization=true in config, visualization will continue automatically."), false);
            }
            return 1;
        });
    }

    public static int status(CommandSourceStack source) {
        return PerformanceStats.timeCommand("radius_visualization", () -> {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                return failPlayerRequired(source, "status");
            }

            RadiusVisualizationService.Status status = RadiusVisualizationService.status(player);
            source.sendSuccess(() -> Component.literal("[RadWorks] Radius visualization status"), false);
            source.sendSuccess(() -> Component.literal("active="
                    + status.active()
                    + " alwaysOnFromConfig="
                    + status.alwaysOnFromConfig()
                    + " remainingTicks="
                    + status.remainingTicks()
                    + " remainingSeconds="
                    + (status.remainingTicks() < 0L ? "infinite" : round(status.remainingTicks() / 20.0D))
                    + " lastVisualizedSources="
                    + status.lastVisualizedSources()
                    + " lastSkippedSources="
                    + status.lastSkippedSources()
                    + " maxRadiusSeen="
                    + round(status.maxRadiusSeen())), false);
            return 1;
        });
    }

    private static int failPlayerRequired(CommandSourceStack source, String subcommand) {
        String message = "player required; use /radworks radius " + subcommand + " as a player";
        WarningBuffer.add("COMMAND_MISUSE", "radius." + subcommand, message);
        source.sendFailure(Component.literal(message));
        return 0;
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
