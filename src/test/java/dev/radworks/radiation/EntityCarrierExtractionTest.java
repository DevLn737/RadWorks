package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
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

    @Test
    void droppedContainerStackNestedContentsAreAggregated() {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX, 1);
        shulker.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.ROTTEN_FLESH, 5))));

        List<EntityCarrierExtraction.MatchedAggregate> aggregates =
                EntityCarrierExtraction.aggregateRadioactiveStackWithNested(
                        shulker,
                        "entity_dropped_item.entity[test]",
                        rules,
                        NestedContainerDiagnostics.builder());

        assertEquals(1, aggregates.size());
        EntityCarrierExtraction.MatchedAggregate aggregate = aggregates.get(0);
        assertEquals("minecraft:rotten_flesh", aggregate.itemId().toString());
        assertEquals(5, aggregate.aggregateCount());
        assertEquals(1, aggregate.contributingStacks());
        assertEquals(1, aggregate.nestedMatches());
        assertEquals(1, aggregate.maxNestedDepth());
        assertEquals("data_component_container", aggregate.firstExtractionMode());
        assertTrue(aggregate.firstContainerPath().contains("slot[0]"));
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
