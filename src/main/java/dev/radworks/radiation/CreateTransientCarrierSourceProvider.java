package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.CreateCarrierDiagnostics;
import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public final class CreateTransientCarrierSourceProvider {
    private static final ResourceLocation CREATE_PLACARD = ResourceLocation.parse("create:placard");
    private static final ResourceLocation CREATE_MECHANICAL_ARM = ResourceLocation.parse("create:mechanical_arm");
    private static final ResourceLocation CREATE_FLUID_PIPE = ResourceLocation.parse("create:fluid_pipe");
    private static final ResourceLocation CREATE_GLASS_FLUID_PIPE = ResourceLocation.parse("create:glass_fluid_pipe");
    private static final ResourceLocation FLUID_PIPETTE = ResourceLocation.parse("fluid:pipette");

    private CreateTransientCarrierSourceProvider() {
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics) {
        return collect(player.serverLevel(), player.position(), player.blockPosition(), rules, summary, diagnostics);
    }

    public static List<RadiationSource> collect(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics) {
        return collect(level, targetPosition, center, rules, summary, diagnostics, ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics,
            ForceSourceCandidateSink candidateSink) {
        return PerformanceStats.timeValue(
                "createTransientCarrierScan",
                () -> collectTimed(level, targetPosition, center, rules, summary, diagnostics, candidateSink));
    }

    private static List<RadiationSource> collectTimed(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics,
            ForceSourceCandidateSink candidateSink) {
        if (!rules.loaded()) {
            return List.of();
        }
        if (!ModList.get().isLoaded("create")) {
            return List.of();
        }
        if (!RadWorksConfig.createTransientCarriersEnabled()
                || !RadWorksConfig.createTransientCarrierNbtScanEnabled()) {
            return List.of();
        }
        if (rules.itemRules() == 0 && rules.fluidRules() == 0) {
            return List.of();
        }

        int scanRadius = effectiveScanRadius(rules);
        if (scanRadius <= 0) {
            return List.of();
        }

        BlockPos min = center.offset(-scanRadius, -scanRadius, -scanRadius);
        BlockPos max = center.offset(scanRadius, scanRadius, scanRadius);

        Map<AggregatedSourceAccumulator.ItemGroupKey, AggregatedSourceAccumulator.ItemAggregate> itemAggregates =
                new LinkedHashMap<>();
        Map<AggregatedSourceAccumulator.FluidGroupKey, FluidAggregateWithMode> fluidAggregates =
                new LinkedHashMap<>();

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                continue;
            }
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            CarrierKind carrierKind = carrierKind(blockId);
            if (carrierKind == CarrierKind.NONE) {
                continue;
            }

            summary.createCarrierBlockChecked();
            diagnostics.scannedCarrierBlock();
            double distance = targetPosition.distanceTo(Vec3.atCenterOf(pos));
            int itemAggregateSizeBefore = itemAggregates.size();
            int fluidAggregateSizeBefore = fluidAggregates.size();

            CompoundTag tag;
            try {
                tag = blockEntity.saveWithFullMetadata(level.registryAccess());
            } catch (RuntimeException exception) {
                summary.createCarrierUnexpectedStructure();
                diagnostics.unexpectedStructure(
                        blockId,
                        pos,
                        carrierKind.id,
                        "block_entity",
                        "failed_to_serialize: " + exception.getClass().getSimpleName());
                continue;
            }

            int localPathSamples = 0;
            localPathSamples = collectKnownItemPath(
                    tag,
                    "Item",
                    blockId,
                    pos,
                    distance,
                    carrierKind,
                    rules,
                    itemAggregates,
                    summary,
                    diagnostics,
                    localPathSamples,
                    candidateSink);
            localPathSamples = collectKnownItemPath(
                    tag,
                    "HeldItem",
                    blockId,
                    pos,
                    distance,
                    carrierKind,
                    rules,
                    itemAggregates,
                    summary,
                    diagnostics,
                    localPathSamples,
                    candidateSink);

            localPathSamples = collectFluidPipeSides(
                    tag,
                    blockId,
                    pos,
                    distance,
                    carrierKind,
                    rules,
                    fluidAggregates,
                    summary,
                    diagnostics,
                    localPathSamples,
                    candidateSink);

            if (carrierKind == CarrierKind.PIPETTE) {
                localPathSamples = collectOptionalFluidPath(
                        tag,
                        "Fluid",
                        blockId,
                        pos,
                        distance,
                        carrierKind,
                        rules,
                        fluidAggregates,
                        summary,
                        diagnostics,
                        localPathSamples,
                        candidateSink);
                localPathSamples = collectOptionalFluidPath(
                        tag,
                        "ContainedFluid",
                        blockId,
                        pos,
                        distance,
                        carrierKind,
                        rules,
                        fluidAggregates,
                        summary,
                        diagnostics,
                        localPathSamples,
                        candidateSink);
            }
            if (itemAggregates.size() == itemAggregateSizeBefore
                    && fluidAggregates.size() == fluidAggregateSizeBefore) {
                diagnostics.skippedCarrierBlock();
            }
        }

        List<RadiationSource> sources = new ArrayList<>();
        for (AggregatedSourceAccumulator.ItemAggregate aggregate : itemAggregates.values()) {
            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            if (!DynamicRadiusModel.isActive(aggregate.distance(), effectiveRadius)) {
                candidateSink.observe(new ForceSourceCandidate(
                        ForceSourceCandidate.CandidateKind.ITEM,
                        RadiationSourceType.CREATE_TRANSIENT_ITEM,
                        aggregate.key().blockId(),
                        aggregate.key().itemId(),
                        null,
                        aggregate.key().position(),
                        null,
                        null,
                        null,
                        null,
                        aggregate.key().blockId(),
                        null,
                        aggregate.aggregateCount(),
                        0,
                        aggregate.distance(),
                        aggregate.rule().respectsShielding(),
                        false,
                        0,
                        "safe_data_path",
                        DynamicRadiusModel.outsideDynamicRadiusReason()));
                continue;
            }
            summary.createCarrierItemMatch();
            summary.aggregateRowProduced();

            String context = aggregate.key().capabilityContext();
            String carrier = contextCarrier(context);
            String dataPath = contextDataPath(context);
            sources.add(RadiationSource.createTransientItemAggregate(
                    aggregate.key().blockId(),
                    aggregate.key().position(),
                    carrier,
                    dataPath,
                    aggregate.key().itemId(),
                    aggregate.aggregateCount(),
                    aggregate.contributingStacks(),
                    aggregate.rule().strength(),
                    baseRadius,
                    effectiveRadius,
                    aggregate.distance(),
                    aggregate.rule().respectsShielding(),
                    aggregate.rawContribution(),
                    "Create transient item source matched active rule id="
                            + aggregate.key().itemId()
                            + " path="
                            + dataPath));
            diagnostics.matchedItem();
        }

        for (FluidAggregateWithMode aggregateWithMode : fluidAggregates.values()) {
            AggregatedSourceAccumulator.FluidAggregate aggregate = aggregateWithMode.aggregate();
            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForFluids(aggregate.aggregateAmountMb());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            if (!DynamicRadiusModel.isActive(aggregate.distance(), effectiveRadius)) {
                candidateSink.observe(new ForceSourceCandidate(
                        ForceSourceCandidate.CandidateKind.FLUID,
                        RadiationSourceType.CREATE_TRANSIENT_FLUID,
                        aggregate.key().blockId(),
                        null,
                        aggregate.key().fluidId(),
                        aggregate.key().position(),
                        null,
                        null,
                        null,
                        null,
                        aggregate.key().blockId(),
                        null,
                        0,
                        aggregate.aggregateAmountMb(),
                        aggregate.distance(),
                        aggregate.rule().respectsShielding(),
                        false,
                        0,
                        "safe_data_path",
                        DynamicRadiusModel.outsideDynamicRadiusReason()));
                diagnostics.fluidPathSample(
                        aggregate.key().blockId(),
                        aggregate.key().position(),
                        contextCarrier(aggregate.key().capabilityContext()),
                        aggregateWithMode.side(),
                        aggregateWithMode.dataPath(),
                        true,
                        true,
                        aggregate.key().fluidId(),
                        aggregate.aggregateAmountMb(),
                        aggregateWithMode.ruleMatchMode(),
                        DynamicRadiusModel.outsideDynamicRadiusReason());
                continue;
            }
            summary.createCarrierFluidMatch();
            summary.aggregateRowProduced();

            String context = aggregate.key().capabilityContext();
            String carrier = contextCarrier(context);
            String dataPath = contextDataPath(context);
            sources.add(RadiationSource.createTransientFluidAggregate(
                    aggregate.key().blockId(),
                    aggregate.key().position(),
                    carrier,
                    dataPath,
                    aggregate.key().fluidId(),
                    aggregate.aggregateAmountMb(),
                    aggregate.contributingStacks(),
                    aggregate.rule().strength(),
                    baseRadius,
                    effectiveRadius,
                    aggregate.distance(),
                    aggregate.rule().respectsShielding(),
                    aggregate.rawContribution(),
                    aggregateWithMode.ruleMatchMode(),
                    "Create transient fluid source matched "
                            + aggregateWithMode.ruleMatchMode()
                            + " fluid rule id="
                            + aggregateWithMode.matchedRuleId()
                            + " observedFluidId="
                            + aggregate.key().fluidId()
                            + " path="
                            + dataPath));
            diagnostics.matchedFluid();
        }

        return List.copyOf(sources);
    }

    private static int collectKnownItemPath(
            CompoundTag root,
            String path,
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            CarrierKind carrierKind,
            RadiationRules rules,
            Map<AggregatedSourceAccumulator.ItemGroupKey, AggregatedSourceAccumulator.ItemAggregate> aggregates,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics,
            int localPathSamples,
            ForceSourceCandidateSink candidateSink) {
        if (!carrierKind.supportsItemPath(path)) {
            return localPathSamples;
        }
        var parsed = CreateTransientCarrierExtraction.parseItemAtRoot(root, path);
        if (parsed.isEmpty()) {
            return reportUnexpected(
                    diagnostics,
                    summary,
                    blockId,
                    pos,
                    carrierKind.id,
                    path,
                    "missing_or_invalid_item_payload",
                    localPathSamples);
        }
        ResourceLocation itemId = parsed.get().id();
        int count = parsed.get().count();
        RadiationRule rule = rules.itemRule(itemId).orElse(null);
        if (rule == null) {
            candidateSink.observe(new ForceSourceCandidate(
                    ForceSourceCandidate.CandidateKind.ITEM,
                    RadiationSourceType.CREATE_TRANSIENT_ITEM,
                    blockId,
                    itemId,
                    null,
                    pos.immutable(),
                    null,
                    null,
                    null,
                    path,
                    blockId,
                    null,
                    count,
                    0,
                    distance,
                    true,
                    false,
                    0,
                    "safe_data_path",
                    "create_transient_item_observed_without_item_rule"));
            return localPathSamples;
        }

        String context = carrierKind.id + "|" + path;
        AggregatedSourceAccumulator.ItemGroupKey key = new AggregatedSourceAccumulator.ItemGroupKey(
                RadiationSourceType.CREATE_TRANSIENT_ITEM,
                pos.immutable(),
                blockId,
                context,
                itemId,
                rule.key());
        AggregatedSourceAccumulator.ItemAggregate aggregate = aggregates.computeIfAbsent(
                key,
                ignored -> AggregatedSourceAccumulator.newItemAggregate(key, rule, distance));
        AggregatedSourceAccumulator.addItemStack(aggregate, count);
        return localPathSamples;
    }

    private static int collectFluidPipeSides(
            CompoundTag root,
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            CarrierKind carrierKind,
            RadiationRules rules,
            Map<AggregatedSourceAccumulator.FluidGroupKey, FluidAggregateWithMode> aggregates,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics,
            int localPathSamples,
            ForceSourceCandidateSink candidateSink) {
        if (!carrierKind.supportsSideFlows()) {
            return localPathSamples;
        }

        for (Direction direction : Direction.values()) {
            String side = direction.getName();
            String path = side + ".Flow.Fluid";
            var parseOutcome = CreateTransientCarrierExtraction.parseFluidAtSideFlowDetailed(root, side);
            if (parseOutcome.status() != CreateTransientCarrierExtraction.FluidParseStatus.SUCCESS) {
                localPathSamples = addFluidPathSample(
                        diagnostics,
                        blockId,
                        pos,
                        carrierKind.id,
                        side,
                        parseOutcome.dataPath(),
                        parseOutcome.status() != CreateTransientCarrierExtraction.FluidParseStatus.PATH_MISSING,
                        parseOutcome.status() != CreateTransientCarrierExtraction.FluidParseStatus.FLUID_COMPOUND_MISSING
                                && parseOutcome.status() != CreateTransientCarrierExtraction.FluidParseStatus.PATH_MISSING,
                        parseOutcome.parsedFluidId(),
                        parseOutcome.parsedAmountMb(),
                        "none",
                        statusToSkippedReason(parseOutcome.status()),
                        localPathSamples);
                continue;
            }
            var parsed = parseOutcome.payload().orElse(null);
            if (parsed == null) {
                continue;
            }
            localPathSamples = collectParsedFluidAtPath(
                    parsed,
                    side,
                    path,
                    blockId,
                    pos,
                    distance,
                    carrierKind,
                    rules,
                    aggregates,
                    summary,
                    diagnostics,
                    localPathSamples,
                    candidateSink);
        }
        return localPathSamples;
    }

    private static int collectOptionalFluidPath(
            CompoundTag root,
            String path,
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            CarrierKind carrierKind,
            RadiationRules rules,
            Map<AggregatedSourceAccumulator.FluidGroupKey, FluidAggregateWithMode> aggregates,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics,
            int localPathSamples,
            ForceSourceCandidateSink candidateSink) {
        if (!root.contains(path, Tag.TAG_COMPOUND)) {
            return localPathSamples;
        }
        var parsed = CreateTransientCarrierExtraction.parseFluidAtRoot(root, path);
        if (parsed.isEmpty()) {
            return reportUnexpected(
                    diagnostics,
                    summary,
                    blockId,
                    pos,
                    carrierKind.id,
                    path,
                    "missing_or_invalid_fluid_payload",
                    localPathSamples);
        }
        return collectParsedFluidAtPath(
                parsed.get(),
                "root",
                path,
                blockId,
                pos,
                distance,
                carrierKind,
                rules,
                aggregates,
                summary,
                diagnostics,
                localPathSamples,
                candidateSink);
    }

    private static int collectParsedFluidAtPath(
            CreateTransientCarrierExtraction.FluidPayload parsedFluid,
            String side,
            String path,
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            CarrierKind carrierKind,
            RadiationRules rules,
            Map<AggregatedSourceAccumulator.FluidGroupKey, FluidAggregateWithMode> aggregates,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics,
            int localPathSamples,
            ForceSourceCandidateSink candidateSink) {
        ResourceLocation fluidId = parsedFluid.id();
        int amountMb = parsedFluid.amountMb();

        RadiationRules.FluidRuleMatch ruleMatch = rules.resolveFluidRule(fluidId).orElse(null);
        if (ruleMatch == null) {
            candidateSink.observe(new ForceSourceCandidate(
                    ForceSourceCandidate.CandidateKind.FLUID,
                    RadiationSourceType.CREATE_TRANSIENT_FLUID,
                    blockId,
                    null,
                    fluidId,
                    pos.immutable(),
                    null,
                    null,
                    null,
                    path,
                    blockId,
                    null,
                    0,
                    amountMb,
                    distance,
                    true,
                    false,
                    0,
                    "safe_data_path",
                    "create_transient_fluid_observed_without_fluid_rule"));
            return addFluidPathSample(
                    diagnostics,
                    blockId,
                    pos,
                    carrierKind.id,
                    side,
                    path,
                    true,
                    true,
                    fluidId,
                    amountMb,
                    "none",
                    "no_active_fluid_rule",
                    localPathSamples);
        }
        localPathSamples = addFluidPathSample(
                diagnostics,
                blockId,
                pos,
                carrierKind.id,
                side,
                path,
                true,
                true,
                fluidId,
                amountMb,
                "exact".equals(ruleMatch.mode()) ? "exact" : "fallback",
                "exact".equals(ruleMatch.mode()) ? "matched_exact" : "matched_fallback",
                localPathSamples);
        RadiationRule rule = ruleMatch.rule();
        String context = carrierKind.id + "|" + path;
        AggregatedSourceAccumulator.FluidGroupKey key = new AggregatedSourceAccumulator.FluidGroupKey(
                RadiationSourceType.CREATE_TRANSIENT_FLUID,
                pos.immutable(),
                blockId,
                context,
                fluidId,
                rule.key());
        FluidAggregateWithMode aggregateWithMode = aggregates.computeIfAbsent(
                key,
                ignored -> new FluidAggregateWithMode(
                        AggregatedSourceAccumulator.newFluidAggregate(key, rule, distance),
                        ruleMatch.matchedRuleId(),
                        ruleMatch.mode(),
                        side,
                        path));
        AggregatedSourceAccumulator.addFluidStack(aggregateWithMode.aggregate(), amountMb);
        return localPathSamples;
    }

    private static int addFluidPathSample(
            CreateCarrierDiagnostics.Builder diagnostics,
            ResourceLocation blockId,
            BlockPos pos,
            String carrierKind,
            String side,
            String dataPath,
            boolean pathFound,
            boolean fluidFound,
            ResourceLocation parsedFluidId,
            Integer parsedAmountMb,
            String ruleMatchMode,
            String skippedReason,
            int localPathSamples) {
        if (localPathSamples >= RadWorksConfig.createTransientCarrierPathSampleCap()) {
            return localPathSamples;
        }
        diagnostics.fluidPathSample(
                blockId,
                pos,
                carrierKind,
                side,
                dataPath,
                pathFound,
                fluidFound,
                parsedFluidId,
                parsedAmountMb,
                ruleMatchMode,
                skippedReason);
        return localPathSamples + 1;
    }

    private static int reportUnexpected(
            CreateCarrierDiagnostics.Builder diagnostics,
            SourceScanSummary.Builder summary,
            ResourceLocation blockId,
            BlockPos pos,
            String carrierKind,
            String path,
            String message,
            int localPathSamples) {
        if (localPathSamples >= RadWorksConfig.createTransientCarrierPathSampleCap()) {
            return localPathSamples;
        }
        diagnostics.unexpectedStructure(blockId, pos, carrierKind, path, message);
        summary.createCarrierUnexpectedStructure();
        return localPathSamples + 1;
    }

    private static CarrierKind carrierKind(ResourceLocation blockId) {
        if (CREATE_PLACARD.equals(blockId)) {
            return CarrierKind.PLACARD;
        }
        if (CREATE_MECHANICAL_ARM.equals(blockId)) {
            return CarrierKind.MECHANICAL_ARM;
        }
        if (CREATE_FLUID_PIPE.equals(blockId)) {
            return CarrierKind.FLUID_PIPE;
        }
        if (CREATE_GLASS_FLUID_PIPE.equals(blockId)) {
            return CarrierKind.GLASS_FLUID_PIPE;
        }
        if (FLUID_PIPETTE.equals(blockId)) {
            return CarrierKind.PIPETTE;
        }
        return CarrierKind.NONE;
    }

    private static String contextCarrier(String context) {
        int separator = context.indexOf('|');
        return separator < 0 ? "unknown" : context.substring(0, separator);
    }

    private static String contextDataPath(String context) {
        int separator = context.indexOf('|');
        return separator < 0 ? context : context.substring(separator + 1);
    }

    private static String statusToSkippedReason(CreateTransientCarrierExtraction.FluidParseStatus status) {
        return switch (status) {
            case PATH_MISSING -> "path_missing";
            case FLUID_COMPOUND_MISSING -> "fluid_compound_missing";
            case INVALID_FLUID_ID -> "invalid_fluid_id";
            case AMOUNT_MISSING -> "amount_missing";
            case AMOUNT_NON_POSITIVE -> "amount_non_positive";
            case SUCCESS -> "matched_exact";
        };
    }

    private static int effectiveScanRadius(RadiationRules rules) {
        double baseMax = Math.max(rules.maxActiveItemRuleRadius(), rules.maxActiveFluidRuleRadius());
        double dynamicMax = RadWorksConfig.dynamicRadiusEnabled()
                ? Math.max(baseMax, RadWorksConfig.dynamicRadiusMaxCap())
                : baseMax;
        int requestedCap = RadWorksConfig.createTransientCarrierMaxScanRadius();
        return (int) Math.ceil(Math.min(dynamicMax, requestedCap));
    }

    private enum CarrierKind {
        NONE("unknown"),
        PLACARD("placard"),
        MECHANICAL_ARM("mechanical_arm"),
        FLUID_PIPE("fluid_pipe"),
        GLASS_FLUID_PIPE("glass_fluid_pipe"),
        PIPETTE("pipette");

        private final String id;

        CarrierKind(String id) {
            this.id = id;
        }

        private boolean supportsItemPath(String path) {
            return switch (this) {
                case PLACARD -> "Item".equals(path);
                case MECHANICAL_ARM -> "HeldItem".equals(path);
                default -> false;
            };
        }

        private boolean supportsSideFlows() {
            return this == FLUID_PIPE || this == GLASS_FLUID_PIPE || this == PIPETTE;
        }
    }

    private record FluidAggregateWithMode(
            AggregatedSourceAccumulator.FluidAggregate aggregate,
            ResourceLocation matchedRuleId,
            String ruleMatchMode,
            String side,
            String dataPath) {
    }
}
