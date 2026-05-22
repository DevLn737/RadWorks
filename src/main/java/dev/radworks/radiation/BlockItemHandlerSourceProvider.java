package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.HandlerDiagnostics;
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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class BlockItemHandlerSourceProvider {
    public static final int MAX_SCAN_RADIUS = 8;
    private static final Direction[] SIDED_CONTEXTS = {
            Direction.UP,
            Direction.DOWN,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };
    private static final Set<String> WARNED_RADIUS_CLAMPS = ConcurrentHashMap.newKeySet();
    private static final Set<String> WARNED_SCAN_FAILURES = ConcurrentHashMap.newKeySet();

    private BlockItemHandlerSourceProvider() {
    }

    public static List<RadiationSource> collect(ServerPlayer player, RadiationRules rules) {
        return collect(
                player,
                rules,
                SourceScanSummary.builder(),
                HandlerDiagnostics.builder(),
                NestedContainerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
        return collect(player, rules, summary, HandlerDiagnostics.builder(), NestedContainerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            HandlerDiagnostics.Builder handlerDiagnostics) {
        return collect(
                player.serverLevel(),
                player.position(),
                player.blockPosition(),
                rules,
                summary,
                handlerDiagnostics,
                NestedContainerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            HandlerDiagnostics.Builder handlerDiagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        return collect(
                player.serverLevel(),
                player.position(),
                player.blockPosition(),
                rules,
                summary,
                handlerDiagnostics,
                nestedDiagnostics,
                ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            HandlerDiagnostics.Builder handlerDiagnostics) {
        return collect(level, targetPosition, center, rules, summary, handlerDiagnostics, NestedContainerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            HandlerDiagnostics.Builder handlerDiagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        return collect(
                level,
                targetPosition,
                center,
                rules,
                summary,
                handlerDiagnostics,
                nestedDiagnostics,
                ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            HandlerDiagnostics.Builder handlerDiagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            ForceSourceCandidateSink candidateSink) {
        return PerformanceStats.timeValue(
                "itemHandlerScan",
                () -> collectTimed(level, targetPosition, center, rules, summary, handlerDiagnostics, nestedDiagnostics, candidateSink));
    }

    private static List<RadiationSource> collectTimed(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos center,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            HandlerDiagnostics.Builder handlerDiagnostics,
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
            summary.itemHandlerPositionChecked();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container) {
                summary.skippedContainerBlockEntityForItemHandler();
                continue;
            }

            HandlerLookup lookup = findHandler(level, pos);
            if (lookup == null) {
                continue;
            }
            summary.itemHandlerFound();

            BlockState state = level.getBlockState(pos);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            double distance = targetPosition.distanceTo(Vec3.atCenterOf(pos));
            HandlerScanResult scanResult = collectHandlerSlots(
                    blockId,
                    pos,
                    distance,
                    lookup,
                    rules,
                    sources,
                    summary,
                    nestedDiagnostics,
                    candidateSink);
            if (scanResult.matches() == 0) {
                handlerDiagnostics.addItemHandlerSample(
                        blockId,
                        pos,
                        lookup.capabilityContext(),
                        scanResult.slotsChecked(),
                        scanResult.matches(),
                        scanResult.contents());
            }
        }

        return List.copyOf(sources);
    }

    private static HandlerLookup findHandler(ServerLevel level, BlockPos pos) {
        IItemHandler unsided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (unsided != null) {
            return new HandlerLookup(unsided, "unsided");
        }

        for (Direction side : SIDED_CONTEXTS) {
            IItemHandler sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            if (sided != null) {
                return new HandlerLookup(sided, side.getName());
            }
        }
        return null;
    }

    private static HandlerScanResult collectHandlerSlots(
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            HandlerLookup lookup,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            ForceSourceCandidateSink candidateSink) {
        final int maxContents = 5;
        int slotsChecked = 0;
        int matches = 0;
        List<HandlerDiagnostics.ContentSample> contents = new ArrayList<>(maxContents);
        Map<Key, AggregatedSourceAccumulator.ItemAggregate> aggregates = new LinkedHashMap<>();
        Map<Key, NestedAggregateMeta> nestedMeta = new LinkedHashMap<>();

        int slots;
        try {
            slots = lookup.handler().getSlots();
        } catch (RuntimeException exception) {
            warnScanFailure(blockId, pos, lookup.capabilityContext(), "getSlots failed: " + exception.getMessage());
            return new HandlerScanResult(slotsChecked, matches, contents);
        }

        for (int slot = 0; slot < slots; slot++) {
            summary.itemHandlerSlotChecked();
            slotsChecked++;
            ItemStack stack;
            try {
                stack = lookup.handler().getStackInSlot(slot);
            } catch (RuntimeException exception) {
                warnScanFailure(
                        blockId,
                        pos,
                        lookup.capabilityContext(),
                        "getStackInSlot(" + slot + ") failed: " + exception.getMessage());
                continue;
            }

            if (stack.isEmpty()) {
                addItemSample(contents, maxContents, slot, null, 0, "empty", distance, null, null, null);
                continue;
            }

            String sourcePath = "block_item_handler.pos["
                    + pos.getX()
                    + ","
                    + pos.getY()
                    + ","
                    + pos.getZ()
                    + "].slot["
                    + slot
                    + "]";
            List<NestedContainerExtractor.ExtractedStack> extractedStacks =
                    NestedContainerExtractor.expand(stack, sourcePath, nestedDiagnostics);
            boolean matchedInAny = false;
            for (NestedContainerExtractor.ExtractedStack extracted : extractedStacks) {
                RadiationRule rule = rules.itemRule(extracted.itemId()).orElse(null);
                if (rule == null) {
                    candidateSink.observe(new ForceSourceCandidate(
                            ForceSourceCandidate.CandidateKind.ITEM,
                            RadiationSourceType.BLOCK_ITEM_HANDLER,
                            blockId,
                            extracted.itemId(),
                            null,
                            pos.immutable(),
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
                            "block_item_handler_observed_without_item_rule"));
                    continue;
                }
                matchedInAny = true;
                Key key = new Key(extracted.itemId(), rule.key());
                AggregatedSourceAccumulator.ItemAggregate aggregate = aggregates.computeIfAbsent(
                        key,
                        ignored -> AggregatedSourceAccumulator.newItemAggregate(
                                new AggregatedSourceAccumulator.ItemGroupKey(
                                        RadiationSourceType.BLOCK_ITEM_HANDLER,
                                        pos.immutable(),
                                        blockId,
                                        lookup.capabilityContext(),
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
            if (!matchedInAny) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                addItemSample(contents, maxContents, slot, itemId, stack.getCount(), "no_active_rule", distance, null, null, null);
            }
        }

        for (AggregatedSourceAccumulator.ItemAggregate aggregate : aggregates.values()) {
            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            if (!DynamicRadiusModel.isActive(distance, effectiveRadius)) {
                addItemSample(
                        contents,
                        maxContents,
                        -1,
                        aggregate.key().itemId(),
                        aggregate.aggregateCount(),
                        DynamicRadiusModel.outsideDynamicRadiusReason(),
                        distance,
                        baseRadius,
                        effectiveRadius,
                        units);
                continue;
            }

            summary.itemHandlerMatch();
            summary.aggregateRowProduced();
            matches++;
            RadiationSource source = RadiationSource.blockItemHandlerAggregate(
                    blockId,
                    pos,
                    lookup.capabilityContext(),
                    aggregate.key().itemId(),
                    aggregate.aggregateCount(),
                    aggregate.contributingStacks(),
                    aggregate.rule().strength(),
                    baseRadius,
                    effectiveRadius,
                    distance,
                    aggregate.rule().respectsShielding(),
                    aggregate.rawContribution(),
                    "NeoForge ItemHandler aggregated source matched active item rule id="
                            + aggregate.key().itemId()
                            + " units="
                            + aggregate.aggregateCount());
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
        }
        return new HandlerScanResult(slotsChecked, matches, contents);
    }

    private static void addItemSample(
            List<HandlerDiagnostics.ContentSample> contents,
            int maxContents,
            int slot,
            ResourceLocation itemId,
            int count,
            String reason,
            Double distance,
            Double baseRadius,
            Double effectiveRadius,
            Double aggregateUnitsSnapshot) {
        if (contents.size() >= maxContents) {
            return;
        }
        String slotLabel = slot < 0 ? null : "item_handler." + slot;
        contents.add(HandlerDiagnostics.ContentSample.item(
                slotLabel,
                itemId,
                count,
                reason,
                distance,
                baseRadius,
                effectiveRadius,
                aggregateUnitsSnapshot));
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
                        "ITEM_HANDLER_SCAN_RADIUS_CLAMPED",
                        "itemHandlerScan",
                        "Item rule "
                                + rule.key()
                                + " radius="
                                + rule.radius()
                                + " exceeds Phase 4C max scan radius "
                                + MAX_SCAN_RADIUS
                                + "; command scan is clamped");
            }
        }
    }

    private static void warnScanFailure(
            ResourceLocation blockId,
            BlockPos pos,
            String capabilityContext,
            String message) {
        String warningKey = blockId
                + "@"
                + pos.getX()
                + ","
                + pos.getY()
                + ","
                + pos.getZ()
                + ":"
                + capabilityContext
                + ":"
                + message;
        if (WARNED_SCAN_FAILURES.add(warningKey)) {
            WarningBuffer.add(
                    "ITEM_HANDLER_SCAN_FAILED",
                    "itemHandlerScan",
                    blockId
                            + " at "
                            + pos.getX()
                            + ","
                            + pos.getY()
                            + ","
                            + pos.getZ()
                            + " context="
                            + capabilityContext
                            + ": "
                            + message);
        }
    }

    private record HandlerLookup(IItemHandler handler, String capabilityContext) {
    }

    private record HandlerScanResult(
            int slotsChecked,
            int matches,
            List<HandlerDiagnostics.ContentSample> contents) {
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
