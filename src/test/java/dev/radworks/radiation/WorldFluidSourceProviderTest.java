package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class WorldFluidSourceProviderTest {
    @Test
    void createsWorldFluidSourceForExactUraniumRule() {
        RadiationRules rules = rulesWithFluids(
                fluidRule("createnuclear:uranium", 2.0D, 8.0D),
                fluidRule("createnuclear:flowing_uranium", 3.0D, 8.0D));

        var source = WorldFluidSourceProvider.sourceForFluidSample(
                rules,
                ResourceLocation.parse("createnuclear:uranium"),
                ResourceLocation.parse("minecraft:water"),
                new BlockPos(0, 64, 0),
                1,
                1.0D);

        assertTrue(source.isPresent());
        assertEquals(RadiationSourceType.WORLD_FLUID, source.get().type());
        assertEquals("exact", source.get().ruleMatchMode());
        assertEquals(0.002D, source.get().finalContribution(), 1.0e-9);
        assertEquals(1, source.get().amountMb());
        assertEquals(
                DynamicRadiusModel.effectiveRadius(8.0D, DynamicRadiusModel.aggregateUnitsForFluids(1)),
                source.get().effectiveRadius(),
                1.0e-9);
    }

    @Test
    void createsWorldFluidSourceForExactFlowingRule() {
        RadiationRules rules = rulesWithFluids(
                fluidRule("createnuclear:uranium", 1.0D, 8.0D),
                fluidRule("createnuclear:flowing_uranium", 4.0D, 8.0D));

        var source = WorldFluidSourceProvider.sourceForFluidSample(
                rules,
                ResourceLocation.parse("createnuclear:flowing_uranium"),
                ResourceLocation.parse("minecraft:water"),
                new BlockPos(0, 64, 0),
                1000,
                1.0D);

        assertTrue(source.isPresent());
        assertEquals("exact", source.get().ruleMatchMode());
        assertEquals(4.0D, source.get().finalContribution(), 1.0e-9);
    }

    @Test
    void fallsBackFromFlowingToStillWhenExactMissing() {
        RadiationRules rules = rulesWithFluids(fluidRule("createnuclear:uranium", 1.5D, 8.0D));

        var source = WorldFluidSourceProvider.sourceForFluidSample(
                rules,
                ResourceLocation.parse("createnuclear:flowing_uranium"),
                ResourceLocation.parse("minecraft:water"),
                new BlockPos(0, 64, 0),
                1000,
                1.0D);

        assertTrue(source.isPresent());
        assertEquals("fallback", source.get().ruleMatchMode());
        assertEquals(1.5D, source.get().finalContribution(), 1.0e-9);
    }

    @Test
    void returnsEmptyWhenNoActiveFluidRule() {
        RadiationRules rules = rulesWithFluids(fluidRule("createnuclear:uranium", 1.0D, 8.0D));

        var source = WorldFluidSourceProvider.sourceForFluidSample(
                rules,
                ResourceLocation.parse("minecraft:water"),
                ResourceLocation.parse("minecraft:water"),
                new BlockPos(0, 64, 0),
                1000,
                1.0D);

        assertTrue(source.isEmpty());
    }

    private static RadiationRules rulesWithFluids(RadiationRule... fluidRules) {
        return new RadiationRules(
                true,
                "test",
                List.of(fluidRules),
                0,
                fluidRules.length,
                0,
                0,
                List.of(),
                new RadiationRuleValidationResult());
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
