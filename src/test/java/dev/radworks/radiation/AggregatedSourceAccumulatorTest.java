package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AggregatedSourceAccumulatorTest {
    @Test
    void itemStacksAggregateIntoSingleContribution() {
        ResourceLocation itemId = ResourceLocation.parse("createnuclear:raw_uranium");
        RadiationRule rule = new RadiationRule(
                RadiationRuleType.ITEM,
                itemId,
                1.0D,
                2.0D,
                true,
                true,
                RadiationRuleProfile.BETA,
                false,
                "createnuclear",
                "real_candidate",
                "test",
                "test");

        AggregatedSourceAccumulator.ItemAggregate aggregate = AggregatedSourceAccumulator.newItemAggregate(
                new AggregatedSourceAccumulator.ItemGroupKey(
                        RadiationSourceType.BLOCK_ENTITY_INVENTORY,
                        null,
                        ResourceLocation.parse("minecraft:chest"),
                        null,
                        itemId,
                        rule.key()),
                rule,
                1.0D);

        AggregatedSourceAccumulator.addItemStack(aggregate, 64);
        AggregatedSourceAccumulator.addItemStack(aggregate, 64);

        assertEquals(128, aggregate.aggregateCount());
        assertEquals(2, aggregate.contributingStacks());
        assertEquals(128.0D, aggregate.rawContribution(), 1.0e-9);
    }

    @Test
    void fluidTanksAggregateContribution() {
        ResourceLocation fluidId = ResourceLocation.parse("createnuclear:uranium");
        RadiationRule rule = new RadiationRule(
                RadiationRuleType.FLUID,
                fluidId,
                2.0D,
                2.0D,
                true,
                true,
                RadiationRuleProfile.BETA,
                false,
                "createnuclear",
                "real_candidate",
                "test",
                "test");

        AggregatedSourceAccumulator.FluidAggregate aggregate = AggregatedSourceAccumulator.newFluidAggregate(
                new AggregatedSourceAccumulator.FluidGroupKey(
                        RadiationSourceType.BLOCK_FLUID_HANDLER,
                        null,
                        ResourceLocation.parse("create:fluid_tank"),
                        "unsided",
                        fluidId,
                        rule.key()),
                rule,
                1.0D);

        AggregatedSourceAccumulator.addFluidStack(aggregate, 1000);
        AggregatedSourceAccumulator.addFluidStack(aggregate, 500);

        assertEquals(1500, aggregate.aggregateAmountMb());
        assertEquals(2, aggregate.contributingStacks());
        assertEquals(3.0D, aggregate.rawContribution(), 1.0e-9);
    }
}
