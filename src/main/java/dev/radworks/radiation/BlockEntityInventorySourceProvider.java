package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WarningBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BlockEntityInventorySourceProvider {
    public static final int MAX_SCAN_RADIUS = 8;
    private static final Set<String> WARNED_RADIUS_CLAMPS = ConcurrentHashMap.newKeySet();

    private BlockEntityInventorySourceProvider() {
    }

    public static List<RadiationSource> collect(ServerPlayer player, RadiationRules rules) {
        return collect(player, rules, SourceScanSummary.builder(), NestedContainerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
        return collect(
                player.serverLevel(),
                player.position(),
                player.blockPosition(),
                rules,
                summary,
                NestedContainerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        return collect(
                player.serverLevel(),
                player.position(),
                player.blockPosition(),
                rules,
                summary,
                nestedDiagnostics,
                ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        return collect(level, targetPosition, center, rules, summary, nestedDiagnostics, ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            ForceSourceCandidateSink candidateSink) {
        return PerformanceStats.timeValue(
                "blockEntityInventoryScan",
                () -> collectTimed(level, targetPosition, center, rules, summary, nestedDiagnostics, candidateSink));
    }

    private static List<RadiationSource> collectTimed(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            ForceSourceCandidateSink candidateSink) {
        List<RadiationSource> sources = new ArrayList<>();
        if (!rules.loaded() || rules.itemRules() == 0) {
            return sources;
        }

        warnForClampedRules(rules);

        int scanRadius = effectiveScanRadius(rules);
        if (scanRadius <= 0) {
            return sources;
        }

        BlockPos min = center.offset(-scanRadius, -scanRadius, -scanRadius);
        BlockPos max = center.offset(scanRadius, scanRadius, scanRadius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            summary.blockEntityChecked();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof Container container)) {
                continue;
            }
            summary.containerBlockEntityFound();

            BlockState state = level.getBlockState(pos);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            double distance = targetPosition.distanceTo(Vec3.atCenterOf(pos));
            collectContainerSlots(
                    blockId,
                    pos,
                    distance,
                    container,
                    rules,
                    sources,
                    summary,
                    nestedDiagnostics,
                    candidateSink);
        }

        return List.copyOf(sources);
    }

    private static void collectContainerSlots(
            ResourceLocation blockId,
            BlockPos containerPos,
            double distance,
            Container container,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            ForceSourceCandidateSink candidateSink) {
        Map<Key, AggregatedSourceAccumulator.ItemAggregate> aggregates = new LinkedHashMap<>();
        Map<Key, NestedAggregateMeta> nestedMeta = new LinkedHashMap<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            summary.containerSlotChecked();
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            List<NestedContainerExtractor.ExtractedStack> extractedStacks = NestedContainerExtractor.expand(
                    stack,
                    "block_entity_inventory.pos["
                            + containerPos.getX()
                            + ","
                            + containerPos.getY()
                            + ","
                            + containerPos.getZ()
                            + "].slot["
                            + slot
                            + "]",
                    nestedDiagnostics);
            for (NestedContainerExtractor.ExtractedStack extracted : extractedStacks) {
                RadiationRule rule = rules.itemRule(extracted.itemId()).orElse(null);
                if (rule == null) {
                    candidateSink.observe(new ForceSourceCandidate(
                            ForceSourceCandidate.CandidateKind.ITEM,
                            RadiationSourceType.BLOCK_ENTITY_INVENTORY,
                            blockId,
                            extracted.itemId(),
                            null,
                            containerPos.immutable(),
                            null,
                            null,
                            extracted.containerItemId(),
                            extracted.containerPath(),
                            blockId,
                            null,
                            extracted.count(),
                            0,
                            distance,
                            true,
                            extracted.nested(),
                            extracted.nestedDepth(),
                            extracted.extractionMode(),
                            "block_entity_inventory_observed_without_item_rule"));
                    continue;
                }
                Key key = new Key(extracted.itemId(), rule.key());
                AggregatedSourceAccumulator.ItemAggregate aggregate = aggregates.computeIfAbsent(
                        key,
                        ignored -> AggregatedSourceAccumulator.newItemAggregate(
                                new AggregatedSourceAccumulator.ItemGroupKey(
                                        RadiationSourceType.BLOCK_ENTITY_INVENTORY,
                                        containerPos.immutable(),
                                        blockId,
                                        null,
                                        extracted.itemId(),
                                        rule.key()),
                                rule,
                                distance));
                AggregatedSourceAccumulator.addItemStack(aggregate, extracted.count());
                if (extracted.nested()) {
                    nestedDiagnostics.nestedRadioactiveMatch();
                    NestedAggregateMeta meta = nestedMeta.computeIfAbsent(key, ignored -> new NestedAggregateMeta());
                    meta.record(extracted);
                }
            }
        }

        for (AggregatedSourceAccumulator.ItemAggregate aggregate : aggregates.values()) {
            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            if (!DynamicRadiusModel.isActive(distance, effectiveRadius)) {
                continue;
            }
            RadiationSource source = RadiationSource.blockEntityInventoryAggregate(
                    blockId,
                    containerPos,
                    aggregate.key().itemId(),
                    aggregate.aggregateCount(),
                    aggregate.contributingStacks(),
                    aggregate.rule().strength(),
                    baseRadius,
                    effectiveRadius,
                    distance,
                    aggregate.rule().respectsShielding(),
                    aggregate.rawContribution(),
                    "vanilla Container aggregated item source units="
                            + aggregate.aggregateCount()
                            + " id="
                            + aggregate.key().itemId());
            NestedAggregateMeta nested = nestedMeta.get(new Key(aggregate.key().itemId(), aggregate.rule().key()));
            if (nested != null && nested.nestedMatches > 0) {
                source = source.withExtractionContext(nested.firstContainerPath, nested.firstExtractionMode)
                        .withNestedContext(
                                nested.maxNestedDepth,
                                nested.firstContainerItemId,
                                nested.firstContainerPath)
                        .withMatchReasonSuffix(
                                "nested=true nestedMatches="
                                        + nested.nestedMatches
                                        + " nestedDepth="
                                        + nested.maxNestedDepth
                                        + " containerItemId="
                                        + nested.firstContainerItemId);
            }
            sources.add(source);
            summary.containerMatch();
            summary.aggregateRowProduced();
        }
    }

    private static int effectiveScanRadius(RadiationRules rules) {
        double baseMax = rules.maxActiveItemRuleRadius();
        double dynamicMax = RadWorksConfig.dynamicRadiusEnabled()
                ? Math.max(baseMax, RadWorksConfig.dynamicRadiusMaxCap())
                : baseMax;
        return (int) Math.ceil(Math.min(dynamicMax, MAX_SCAN_RADIUS));
    }

    private static void warnForClampedRules(RadiationRules rules) {
        for (RadiationRule rule : rules.activeItemRules()) {
            if (rule.radius() <= MAX_SCAN_RADIUS) {
                continue;
            }

            String warningKey = rules.checksum() + ":" + rule.key();
            if (WARNED_RADIUS_CLAMPS.add(warningKey)) {
                WarningBuffer.add(
                        "BLOCK_ENTITY_INVENTORY_SCAN_RADIUS_CLAMPED",
                        "blockEntityInventoryScan",
                        "Item rule "
                                + rule.key()
                                + " radius="
                                + rule.radius()
                                + " exceeds Phase 4B max scan radius "
                                + MAX_SCAN_RADIUS
                                + "; command scan is clamped");
            }
        }
    }

    private record Key(ResourceLocation itemId, String ruleKey) {
    }

    private static final class NestedAggregateMeta {
        private int nestedMatches;
        private int maxNestedDepth;
        private ResourceLocation firstContainerItemId;
        private String firstContainerPath;
        private String firstExtractionMode;

        private void record(NestedContainerExtractor.ExtractedStack extracted) {
            nestedMatches++;
            maxNestedDepth = Math.max(maxNestedDepth, extracted.nestedDepth());
            if (firstContainerItemId == null) {
                firstContainerItemId = extracted.containerItemId();
            }
            if (firstContainerPath == null) {
                firstContainerPath = extracted.containerPath();
            }
            if (firstExtractionMode == null) {
                firstExtractionMode = extracted.extractionMode();
            }
        }
    }
}
