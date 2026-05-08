package dev.radworks.command;

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
        if (source.getEntity() instanceof ServerPlayer player) {
            return run(source, player);
        }

        source.sendFailure(Component.literal("player required; use /radworks exposure <player>"));
        return 0;
    }

    public static int run(CommandSourceStack source, ServerPlayer player) {
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
