package dev.radworks.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class AggregatedSourceAccumulator {
    private AggregatedSourceAccumulator() {
    }

    public static void addItemStack(ItemAggregate aggregate, int stackCount) {
        if (stackCount <= 0) {
            return;
        }
        aggregate.aggregateCount += stackCount;
        aggregate.contributingStacks += 1;
        aggregate.rawContribution += stackCount * aggregate.rule.strength();
    }

    public static void addFluidStack(FluidAggregate aggregate, int amountMb) {
        if (amountMb <= 0) {
            return;
        }
        aggregate.aggregateAmountMb += amountMb;
        aggregate.contributingStacks += 1;
        aggregate.rawContribution += aggregate.rule.strength() * amountMb / 1000.0D;
    }

    public static ItemAggregate newItemAggregate(ItemGroupKey key, RadiationRule rule, double distance) {
        return new ItemAggregate(key, rule, distance);
    }

    public static FluidAggregate newFluidAggregate(FluidGroupKey key, RadiationRule rule, double distance) {
        return new FluidAggregate(key, rule, distance);
    }

    public record ItemGroupKey(
            RadiationSourceType sourceType,
            BlockPos position,
            ResourceLocation blockId,
            String capabilityContext,
            ResourceLocation itemId,
            String ruleKey) {
    }

    public record FluidGroupKey(
            RadiationSourceType sourceType,
            BlockPos position,
            ResourceLocation blockId,
            String capabilityContext,
            ResourceLocation fluidId,
            String ruleKey) {
    }

    public static final class ItemAggregate {
        private final ItemGroupKey key;
        private final RadiationRule rule;
        private final double distance;
        private int aggregateCount;
        private int contributingStacks;
        private double rawContribution;

        private ItemAggregate(ItemGroupKey key, RadiationRule rule, double distance) {
            this.key = key;
            this.rule = rule;
            this.distance = distance;
        }

        public ItemGroupKey key() {
            return key;
        }

        public RadiationRule rule() {
            return rule;
        }

        public double distance() {
            return distance;
        }

        public int aggregateCount() {
            return aggregateCount;
        }

        public int contributingStacks() {
            return contributingStacks;
        }

        public double rawContribution() {
            return rawContribution;
        }
    }

    public static final class FluidAggregate {
        private final FluidGroupKey key;
        private final RadiationRule rule;
        private final double distance;
        private int aggregateAmountMb;
        private int contributingStacks;
        private double rawContribution;

        private FluidAggregate(FluidGroupKey key, RadiationRule rule, double distance) {
            this.key = key;
            this.rule = rule;
            this.distance = distance;
        }

        public FluidGroupKey key() {
            return key;
        }

        public RadiationRule rule() {
            return rule;
        }

        public double distance() {
            return distance;
        }

        public int aggregateAmountMb() {
            return aggregateAmountMb;
        }

        public int contributingStacks() {
            return contributingStacks;
        }

        public double rawContribution() {
            return rawContribution;
        }
    }
}
