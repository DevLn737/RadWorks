package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class EntityCarrierExtractionTest {
    @Test
    void droppedItemStackDescriptorIsExtracted() {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        ItemStack one = new ItemStack(Items.ROTTEN_FLESH, 1);
        ItemStack sixtyFour = new ItemStack(Items.ROTTEN_FLESH, 64);

        var oneMatch = EntityCarrierExtraction.matchRadioactiveStack(one, rules);
        var sixtyFourMatch = EntityCarrierExtraction.matchRadioactiveStack(sixtyFour, rules);

        assertTrue(oneMatch.isPresent());
        assertTrue(sixtyFourMatch.isPresent());
        assertEquals(1, oneMatch.get().count());
        assertEquals(64, sixtyFourMatch.get().count());

        double oneRadius = DynamicRadiusModel.effectiveRadius(
                oneMatch.get().rule().radius(),
                DynamicRadiusModel.aggregateUnitsForItems(oneMatch.get().count()));
        double sixtyFourRadius = DynamicRadiusModel.effectiveRadius(
                sixtyFourMatch.get().rule().radius(),
                DynamicRadiusModel.aggregateUnitsForItems(sixtyFourMatch.get().count()));
        assertTrue(sixtyFourRadius >= oneRadius);
    }

    @Test
    void itemFrameDisplayedItemDescriptorIsExtracted() {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        ItemStack displayed = new ItemStack(Items.ROTTEN_FLESH, 8);
        var match = EntityCarrierExtraction.matchRadioactiveStack(displayed, rules);
        assertTrue(match.isPresent());
        assertEquals(8, match.get().count());
        assertEquals("minecraft:rotten_flesh", match.get().itemId().toString());
    }

    @Test
    void playerAuraAggregationAndSelfSkipContracts() {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        List<ItemStack> stacks = List.of(new ItemStack(Items.ROTTEN_FLESH, 1), new ItemStack(Items.ROTTEN_FLESH, 64));
        var aggregates = EntityCarrierExtraction.aggregateRadioactiveStacks(stacks, rules);
        assertEquals(1, aggregates.size());
        assertEquals(65, aggregates.get(0).aggregateCount());
        assertEquals(2, aggregates.get(0).contributingStacks());

        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        assertTrue(EntityCarrierExtraction.shouldSkipSelfAura(player, player));
        assertFalse(EntityCarrierExtraction.shouldSkipSelfAura(player, other));
    }

    private static RadiationRules rulesWithItem(String id) {
        RadiationRule rule = new RadiationRule(
                RadiationRuleType.ITEM,
                ResourceLocation.parse(id),
                1.0D,
                2.0D,
                true,
                true,
                RadiationRuleProfile.BETA,
                false,
                null,
                "test",
                "test",
                "test");
        return new RadiationRules(
                true,
                "test",
                List.of(rule),
                0,
                1,
                0,
                0,
                List.of(),
                new RadiationRuleValidationResult());
    }
}
