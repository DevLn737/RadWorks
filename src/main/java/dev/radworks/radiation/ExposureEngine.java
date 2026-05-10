package dev.radworks.radiation;

import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.radiation.armor.ArmorProtectionResult;
import dev.radworks.radiation.armor.ArmorProtectionService;
import dev.radworks.radiation.effects.EffectPreviewResult;
import dev.radworks.radiation.effects.EffectStrategyService;
import dev.radworks.radiation.shielding.ShieldingEngine;
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
        ArmorProtectionResult armorProtection = ArmorProtectionService.evaluate(player, totalExposure);
        EffectPreviewResult effectPreview = EffectStrategyService.preview(totalExposure, armorProtection);

        ExposureBreakdown breakdown = new ExposureBreakdown(
                Instant.now(),
                rules.checksum(),
                player.getGameProfile().getName(),
                player.getUUID(),
                totalExposure,
                sources.size(),
                sources,
                armorProtection,
                effectPreview,
                DIAGNOSTIC_ONLY_NOTE);
        lastExposureSnapshot = breakdown;
        return breakdown;
    }

    public static List<RadiationSource> collectSources(ServerPlayer player, RadiationRules rules) {
        SourceScanSummary.Builder summary = SourceScanSummary.builder();
        List<RadiationSource> sources = new ArrayList<>();
        sources.addAll(PlayerInventorySourceProvider.collect(player, rules, summary));
        sources.addAll(BlockSourceProvider.collect(player, rules, summary));
        sources.addAll(BlockEntityInventorySourceProvider.collect(player, rules, summary));
        sources.addAll(BlockItemHandlerSourceProvider.collect(player, rules, summary));
        sources.addAll(BlockFluidHandlerSourceProvider.collect(player, rules, summary));
        List<RadiationSource> immutableSources = ShieldingEngine.apply(player, sources, summary);
        SourceScanSummary.store(summary, Math.min(20, immutableSources.size()), Math.max(0, immutableSources.size() - 20));
        return immutableSources;
    }

    public static ExposureBreakdown lastExposureSnapshot() {
        return lastExposureSnapshot;
    }
}
