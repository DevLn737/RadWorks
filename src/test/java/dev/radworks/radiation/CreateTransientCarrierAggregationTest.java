package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class CreateTransientCarrierAggregationTest {
    @Test
    void oneItemProducesDescriptorWithDynamicRadius() {
        RadiationRule rule = itemRule("createnuclear:raw_uranium", 1.0D, 2.0D);
        var key = new AggregatedSourceAccumulator.ItemGroupKey(
                RadiationSourceType.CREATE_TRANSIENT_ITEM,
                new BlockPos(0, 64, 0),
                ResourceLocation.parse("create:placard"),
                "placard|Item",
                ResourceLocation.parse("createnuclear:raw_uranium"),
                rule.key());
        var aggregate = AggregatedSourceAccumulator.newItemAggregate(key, rule, 1.0D);
        AggregatedSourceAccumulator.addItemStack(aggregate, 1);

        assertEquals(1, aggregate.aggregateCount());
        assertEquals(1, aggregate.contributingStacks());
        assertEquals(1.0D, aggregate.rawContribution(), 1.0e-9);

        double effectiveRadius = DynamicRadiusModel.effectiveRadius(
                rule.radius(),
                DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount()));
        assertTrue(effectiveRadius >= rule.radius());
    }

    @Test
    void oneMilliBucketFluidProducesDescriptorAndPreservesContribution() {
        RadiationRule rule = fluidRule("createnuclear:uranium", 1.0D, 2.0D);
        var key = new AggregatedSourceAccumulator.FluidGroupKey(
                RadiationSourceType.CREATE_TRANSIENT_FLUID,
                new BlockPos(0, 64, 0),
                ResourceLocation.parse("create:fluid_pipe"),
                "fluid_pipe|east.Flow.Fluid",
                ResourceLocation.parse("createnuclear:uranium"),
                rule.key());
        var aggregate = AggregatedSourceAccumulator.newFluidAggregate(key, rule, 1.0D);
        AggregatedSourceAccumulator.addFluidStack(aggregate, 1);

        assertEquals(1, aggregate.aggregateAmountMb());
        assertEquals(1, aggregate.contributingStacks());
        assertEquals(0.001D, aggregate.rawContribution(), 1.0e-9);

        double units = DynamicRadiusModel.aggregateUnitsForFluids(aggregate.aggregateAmountMb());
        assertEquals(1.0D, units, 1.0e-9);
        double effectiveRadius = DynamicRadiusModel.effectiveRadius(rule.radius(), units);
        assertTrue(effectiveRadius >= rule.radius());
    }

    @Test
    void glassPipeFlowUsesSameAggregationModel() {
        RadiationRule rule = fluidRule("createnuclear:uranium", 1.0D, 2.0D);
        var key = new AggregatedSourceAccumulator.FluidGroupKey(
                RadiationSourceType.CREATE_TRANSIENT_FLUID,
                new BlockPos(1, 64, 1),
                ResourceLocation.parse("create:glass_fluid_pipe"),
                "glass_fluid_pipe|west.Flow.Fluid",
                ResourceLocation.parse("createnuclear:uranium"),
                rule.key());
        var aggregate = AggregatedSourceAccumulator.newFluidAggregate(key, rule, 2.0D);
        AggregatedSourceAccumulator.addFluidStack(aggregate, 1);

        assertEquals(1, aggregate.aggregateAmountMb());
        assertEquals(1, aggregate.contributingStacks());
        assertTrue(aggregate.rawContribution() > 0.0D);
    }

    @Test
    void dynamicRadiusGrowsWithAggregateUnits() {
        double base = 2.0D;
        double oneUnitRadius = DynamicRadiusModel.effectiveRadius(base, 1.0D);
        double sixtyFourUnitRadius = DynamicRadiusModel.effectiveRadius(base, 64.0D);
        assertTrue(sixtyFourUnitRadius >= oneUnitRadius);
    }

    private static RadiationRule itemRule(String id, double strength, double radius) {
        return new RadiationRule(
                RadiationRuleType.ITEM,
                ResourceLocation.parse(id),
                strength,
                radius,
                true,
                true,
                RadiationRuleProfile.BETA,
                false,
                "createnuclear",
                "real_candidate",
                "test",
                "test");
    }

    private static RadiationRule fluidRule(String id, double strength, double radius) {
        return new RadiationRule(
                RadiationRuleType.FLUID,
                ResourceLocation.parse(id),
                strength,
                radius,
                true,
                true,
                RadiationRuleProfile.BETA,
                false,
                "createnuclear",
                "real_candidate",
                "test",
                "test");
    }
}
