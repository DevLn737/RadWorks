package dev.radworks.radiation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class EntityCarrierExtraction {
    private EntityCarrierExtraction() {
    }

    static Optional<MatchedStack> matchRadioactiveStack(ItemStack stack, RadiationRules rules) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        RadiationRule rule = rules.itemRule(itemId).orElse(null);
        if (rule == null) {
            return Optional.empty();
        }
        int count = stack.getCount();
        if (count <= 0) {
            return Optional.empty();
        }
        return Optional.of(new MatchedStack(itemId, count, rule));
    }

    static List<MatchedAggregate> aggregateRadioactiveStackWithNested(
            ItemStack stack,
            String sourcePath,
            RadiationRules rules,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        return aggregateExtractedStacks(
                NestedContainerExtractor.expand(stack, sourcePath, nestedDiagnostics),
                rules,
                nestedDiagnostics);
    }

    static List<MatchedAggregate> aggregateRadioactiveStacks(Iterable<ItemStack> stacks, RadiationRules rules) {
        return aggregateRadioactiveStacks(stacks, "entity_inventory", rules, NestedContainerDiagnostics.builder());
    }

    static List<MatchedAggregate> aggregateRadioactiveStacks(
            Iterable<ItemStack> stacks,
            String sourcePathPrefix,
            RadiationRules rules,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        Map<Key, MatchedAggregateAccumulator> accumulators = new LinkedHashMap<>();
        int index = 0;
        for (ItemStack stack : stacks) {
            String sourcePath = sourcePathPrefix + ".slot[" + index + "]";
            List<NestedContainerExtractor.ExtractedStack> extracted =
                    NestedContainerExtractor.expand(stack, sourcePath, nestedDiagnostics);
            mergeExtracted(accumulators, extracted, rules, nestedDiagnostics);
            index++;
        }
        return toAggregates(accumulators);
    }

    private static List<MatchedAggregate> aggregateExtractedStacks(
            List<NestedContainerExtractor.ExtractedStack> extractedStacks,
            RadiationRules rules,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        Map<Key, MatchedAggregateAccumulator> accumulators = new LinkedHashMap<>();
        mergeExtracted(accumulators, extractedStacks, rules, nestedDiagnostics);
        return toAggregates(accumulators);
    }

    private static void mergeExtracted(
            Map<Key, MatchedAggregateAccumulator> accumulators,
            List<NestedContainerExtractor.ExtractedStack> extractedStacks,
            RadiationRules rules,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        for (NestedContainerExtractor.ExtractedStack extracted : extractedStacks) {
            RadiationRule rule = rules.itemRule(extracted.itemId()).orElse(null);
            if (rule == null) {
                continue;
            }
            if (extracted.nested()) {
                nestedDiagnostics.nestedRadioactiveMatch();
            }
            Key key = new Key(extracted.itemId(), rule.key());
            MatchedAggregateAccumulator accumulator = accumulators.computeIfAbsent(
                    key,
                    ignored -> new MatchedAggregateAccumulator(extracted.itemId(), rule));
            accumulator.totalCount += extracted.count();
            accumulator.contributingStacks += 1;
            accumulator.recordNested(extracted);
        }
    }

    private static List<MatchedAggregate> toAggregates(Map<Key, MatchedAggregateAccumulator> accumulators) {
        List<MatchedAggregate> aggregates = new ArrayList<>();
        for (MatchedAggregateAccumulator accumulator : accumulators.values()) {
            aggregates.add(new MatchedAggregate(
                    accumulator.itemId,
                    accumulator.totalCount,
                    accumulator.contributingStacks,
                    accumulator.rule,
                    accumulator.nestedMatches,
                    accumulator.maxNestedDepth,
                    accumulator.firstContainerItemId,
                    accumulator.firstContainerPath,
                    accumulator.firstExtractionMode));
        }
        return List.copyOf(aggregates);
    }

    static boolean shouldSkipSelfAura(UUID executingPlayerUuid, UUID auraPlayerUuid) {
        return executingPlayerUuid.equals(auraPlayerUuid);
    }

    record MatchedStack(ResourceLocation itemId, int count, RadiationRule rule) {
    }

    record MatchedAggregate(
            ResourceLocation itemId,
            int aggregateCount,
            int contributingStacks,
            RadiationRule rule,
            int nestedMatches,
            int maxNestedDepth,
            ResourceLocation firstContainerItemId,
            String firstContainerPath,
            String firstExtractionMode) {
    }

    private record Key(ResourceLocation itemId, String ruleKey) {
    }

    private static final class MatchedAggregateAccumulator {
        private final ResourceLocation itemId;
        private final RadiationRule rule;
        private int totalCount;
        private int contributingStacks;
        private int nestedMatches;
        private int maxNestedDepth;
        private ResourceLocation firstContainerItemId;
        private String firstContainerPath;
        private String firstExtractionMode;

        private MatchedAggregateAccumulator(ResourceLocation itemId, RadiationRule rule) {
            this.itemId = itemId;
            this.rule = rule;
        }

        private void recordNested(NestedContainerExtractor.ExtractedStack extracted) {
            if (!extracted.nested()) {
                return;
            }
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
