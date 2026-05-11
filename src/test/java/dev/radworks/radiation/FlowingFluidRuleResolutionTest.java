package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class FlowingFluidRuleResolutionTest {
    @Test
    void exactFlowingRuleMatchIsPreferred() {
        RadiationRules rules = rulesWithFluids(
                fluidRule("createnuclear:uranium"),
                fluidRule("createnuclear:flowing_uranium"));

        var match = rules.resolveFluidRule(ResourceLocation.parse("createnuclear:flowing_uranium"));
        assertTrue(match.isPresent());
        assertEquals("exact", match.get().mode());
        assertEquals("createnuclear:flowing_uranium", match.get().matchedRuleId().toString());
    }

    @Test
    void fallbackFlowingToStillRuleWorksWhenExactMissing() {
        RadiationRules rules = rulesWithFluids(fluidRule("createnuclear:uranium"));

        var match = rules.resolveFluidRule(ResourceLocation.parse("createnuclear:flowing_uranium"));
        assertTrue(match.isPresent());
        assertEquals("fallback", match.get().mode());
        assertEquals("createnuclear:uranium", match.get().matchedRuleId().toString());
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

    private static RadiationRule fluidRule(String id) {
        return new RadiationRule(
                RadiationRuleType.FLUID,
                ResourceLocation.parse(id),
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
    }
}
