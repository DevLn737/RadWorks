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
        return PerformanceStats.timeValue(
                "createTransientCarrierScan",
                () -> collectTimed(player, rules, summary, diagnostics));
    }

    private static List<RadiationSource> collectTimed(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics) {
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

        ServerLevel level = player.serverLevel();
        Vec3 playerPosition = player.position();
        BlockPos center = player.blockPosition();
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
            double distance = playerPosition.distanceTo(Vec3.atCenterOf(pos));
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
                    localPathSamples);
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
                    localPathSamples);

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
                    localPathSamples);

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
                        localPathSamples);
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
                        localPathSamples);
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
            int localPathSamples) {
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
            int localPathSamples) {
        if (!carrierKind.supportsSideFlows()) {
            return localPathSamples;
        }

        for (Direction direction : Direction.values()) {
            String side = direction.getName();
            String path = side + ".Flow.Fluid";
            var parsed = CreateTransientCarrierExtraction.parseFluidAtSideFlow(root, side);
            if (parsed.isEmpty()) {
                continue;
            }
            localPathSamples = collectParsedFluidAtPath(
                    parsed.get(),
                    path,
                    blockId,
                    pos,
                    distance,
                    carrierKind,
                    rules,
                    aggregates,
                    summary,
                    diagnostics,
                    localPathSamples);
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
            int localPathSamples) {
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
                path,
                blockId,
                pos,
                distance,
                carrierKind,
                rules,
                aggregates,
                summary,
                diagnostics,
                localPathSamples);
    }

    private static int collectParsedFluidAtPath(
            CreateTransientCarrierExtraction.FluidPayload parsedFluid,
            String path,
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            CarrierKind carrierKind,
            RadiationRules rules,
            Map<AggregatedSourceAccumulator.FluidGroupKey, FluidAggregateWithMode> aggregates,
            SourceScanSummary.Builder summary,
            CreateCarrierDiagnostics.Builder diagnostics,
            int localPathSamples) {
        ResourceLocation fluidId = parsedFluid.id();
        int amountMb = parsedFluid.amountMb();

        RadiationRules.FluidRuleMatch ruleMatch = rules.resolveFluidRule(fluidId).orElse(null);
        if (ruleMatch == null) {
            return localPathSamples;
        }
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
                        ruleMatch.mode()));
        AggregatedSourceAccumulator.addFluidStack(aggregateWithMode.aggregate(), amountMb);
        return localPathSamples;
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
            String ruleMatchMode) {
    }
}
