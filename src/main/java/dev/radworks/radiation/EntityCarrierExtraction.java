package dev.radworks.radiation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    static List<MatchedAggregate> aggregateRadioactiveStacks(Iterable<ItemStack> stacks, RadiationRules rules) {
        Map<Key, MatchedAggregateAccumulator> accumulators = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            MatchedStack match = matchRadioactiveStack(stack, rules).orElse(null);
            if (match == null) {
                continue;
            }
            Key key = new Key(match.itemId(), match.rule().key());
            MatchedAggregateAccumulator accumulator = accumulators.computeIfAbsent(
                    key,
                    ignored -> new MatchedAggregateAccumulator(match.itemId(), match.rule()));
            accumulator.totalCount += match.count();
            accumulator.contributingStacks += 1;
        }
        List<MatchedAggregate> aggregates = new ArrayList<>();
        for (MatchedAggregateAccumulator accumulator : accumulators.values()) {
            aggregates.add(new MatchedAggregate(
                    accumulator.itemId,
                    accumulator.totalCount,
                    accumulator.contributingStacks,
                    accumulator.rule));
        }
        return List.copyOf(aggregates);
    }

    static boolean shouldSkipSelfAura(UUID executingPlayerUuid, UUID auraPlayerUuid) {
        return executingPlayerUuid.equals(auraPlayerUuid);
    }

    record MatchedStack(ResourceLocation itemId, int count, RadiationRule rule) {
    }

    record MatchedAggregate(ResourceLocation itemId, int aggregateCount, int contributingStacks, RadiationRule rule) {
    }

    private record Key(ResourceLocation itemId, String ruleKey) {
    }

    private static final class MatchedAggregateAccumulator {
        private final ResourceLocation itemId;
        private final RadiationRule rule;
        private int totalCount;
        private int contributingStacks;

        private MatchedAggregateAccumulator(ResourceLocation itemId, RadiationRule rule) {
            this.itemId = itemId;
            this.rule = rule;
        }
    }
}
