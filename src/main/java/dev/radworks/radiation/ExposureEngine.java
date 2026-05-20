package dev.radworks.radiation;

import dev.radworks.diagnostics.CreateCarrierDiagnostics;
import dev.radworks.diagnostics.EntityCarrierDiagnostics;
import dev.radworks.diagnostics.HandlerDiagnostics;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WorldFluidDiagnostics;
import dev.radworks.config.RadWorksConfig;
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

    public static TargetExposure calculateForTarget(RadiationTargetContext context, RadiationRules rules) {
        List<RadiationSource> sources = collectSourcesForTarget(context, rules);
        double totalExposure = 0.0D;
        for (RadiationSource source : sources) {
            totalExposure += source.contribution();
        }
        return new TargetExposure(totalExposure, sources.size(), sources);
    }

    public static List<RadiationSource> collectSources(ServerPlayer player, RadiationRules rules) {
        RadiationTargetContext context = RadiationTargetContext.forPlayer(player);
        return collectSourcesForTarget(context, rules);
    }

    public static List<RadiationSource> collectSourcesForTarget(RadiationTargetContext context, RadiationRules rules) {
        SourceScanSummary.Builder summary = SourceScanSummary.builder();
        HandlerDiagnostics.Builder handlerDiagnostics = HandlerDiagnostics.builder();
        CreateCarrierDiagnostics.Builder createCarrierDiagnostics = CreateCarrierDiagnostics.builder();
        EntityCarrierDiagnostics.Builder entityCarrierDiagnostics = EntityCarrierDiagnostics.builder();
        WorldFluidDiagnostics.Builder worldFluidDiagnostics = WorldFluidDiagnostics.builder();
        NestedContainerDiagnostics.Builder nestedContainerDiagnostics = NestedContainerDiagnostics.builder();
        List<RadiationSource> sources = new ArrayList<>();
        if (context.includePlayerInventory() && context.target() instanceof ServerPlayer serverPlayer) {
            sources.addAll(PlayerInventorySourceProvider.collect(serverPlayer, rules, summary, nestedContainerDiagnostics));
        }
        sources.addAll(BlockSourceProvider.collect(
                context.level(),
                context.targetPos(),
                context.targetBlockPos(),
                rules,
                summary));
        sources.addAll(WorldFluidSourceProvider.collect(
                context.level(),
                context.targetPos(),
                context.targetBlockPos(),
                rules,
                summary,
                worldFluidDiagnostics));
        sources.addAll(BlockEntityInventorySourceProvider.collect(
                context.level(),
                context.targetPos(),
                context.targetBlockPos(),
                rules,
                summary,
                nestedContainerDiagnostics));
        sources.addAll(BlockItemHandlerSourceProvider.collect(
                context.level(),
                context.targetPos(),
                context.targetBlockPos(),
                rules,
                summary,
                handlerDiagnostics,
                nestedContainerDiagnostics));
        sources.addAll(BlockFluidHandlerSourceProvider.collect(
                context.level(),
                context.targetPos(),
                context.targetBlockPos(),
                rules,
                summary,
                handlerDiagnostics));
        sources.addAll(CreateTransientCarrierSourceProvider.collect(
                context.level(),
                context.targetPos(),
                context.targetBlockPos(),
                rules,
                summary,
                createCarrierDiagnostics));
        sources.addAll(EntityCarrierSourceProvider.collect(
                context.target(),
                rules,
                summary,
                entityCarrierDiagnostics,
                nestedContainerDiagnostics,
                context.includeSelfEntityInventory()));
        boolean useShielding = context.applyShielding()
                && (context.targetKind() == RadiationTargetKind.PLAYER || RadWorksConfig.applyShieldingToLivingEntities());
        List<RadiationSource> immutableSources = useShielding
                ? ShieldingEngine.apply(context, sources, summary)
                : List.copyOf(sources);
        SourceScanSummary.store(summary, Math.min(20, immutableSources.size()), Math.max(0, immutableSources.size() - 20));
        HandlerDiagnostics.store(handlerDiagnostics);
        CreateCarrierDiagnostics.store(createCarrierDiagnostics);
        EntityCarrierDiagnostics.store(entityCarrierDiagnostics);
        WorldFluidDiagnostics.store(worldFluidDiagnostics);
        NestedContainerDiagnostics.store(nestedContainerDiagnostics);
        return immutableSources;
    }

    public static ExposureBreakdown lastExposureSnapshot() {
        return lastExposureSnapshot;
    }

    public record TargetExposure(double totalExposure, int sourceCount, List<RadiationSource> sources) {
    }
}
