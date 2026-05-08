package dev.radworks.radiation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public final class ExposureEngine {
    public static final String DIAGNOSTIC_ONLY_NOTE = "diagnostic only, no gameplay effect";
    private static volatile ExposureBreakdown lastExposureSnapshot;

    private ExposureEngine() {
    }

    public static ExposureBreakdown calculate(ServerPlayer player) {
        RadiationRules rules = RadiationRulesLoader.currentRules();
        List<RadiationSource> sources = collectSources(player, rules);
        double totalExposure = 0.0D;
        for (RadiationSource source : sources) {
            totalExposure += source.contribution();
        }

        ExposureBreakdown breakdown = new ExposureBreakdown(
                Instant.now(),
                rules.checksum(),
                player.getGameProfile().getName(),
                player.getUUID(),
                totalExposure,
                sources.size(),
                sources,
                DIAGNOSTIC_ONLY_NOTE);
        lastExposureSnapshot = breakdown;
        return breakdown;
    }

    public static List<RadiationSource> collectSources(ServerPlayer player, RadiationRules rules) {
        List<RadiationSource> sources = new ArrayList<>();
        sources.addAll(PlayerInventorySourceProvider.collect(player, rules));
        sources.addAll(BlockSourceProvider.collect(player, rules));
        sources.addAll(BlockEntityInventorySourceProvider.collect(player, rules));
        sources.addAll(BlockItemHandlerSourceProvider.collect(player, rules));
        return List.copyOf(sources);
    }

    public static ExposureBreakdown lastExposureSnapshot() {
        return lastExposureSnapshot;
    }
}
