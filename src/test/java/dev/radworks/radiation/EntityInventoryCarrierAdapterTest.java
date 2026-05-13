package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

class EntityInventoryCarrierAdapterTest {
    @Test
    void classifiesChestBoatAndPackAnimalPaths() {
        assertTrue(EntityInventoryCarrierAdapter.isChestBoatPath("oak_chest_boat"));
        assertTrue(EntityInventoryCarrierAdapter.isChestBoatPath("bamboo_chest_raft"));
        assertTrue(EntityInventoryCarrierAdapter.isPackAnimalPath("donkey"));
        assertTrue(EntityInventoryCarrierAdapter.isPackAnimalPath("mule"));
        assertTrue(EntityInventoryCarrierAdapter.isPackAnimalPath("llama"));
        assertTrue(EntityInventoryCarrierAdapter.isPackAnimalPath("trader_llama"));
    }

    @Test
    void containerDescriptorCreatesAggregates() {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        SimpleContainer container = new SimpleContainer(5);
        container.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 1));
        container.setItem(1, new ItemStack(Items.ROTTEN_FLESH, 64));

        List<ItemStack> stacks = EntityInventoryCarrierExtraction.stacksFromContainer(container);
        var aggregates = EntityInventoryCarrierExtraction.aggregateRadioactiveStacks(stacks, rules);

        assertEquals(1, aggregates.size());
        assertEquals(65, aggregates.get(0).aggregateCount());
        assertEquals(2, aggregates.get(0).contributingStacks());
        assertEquals(65.0D, aggregates.get(0).aggregateCount() * aggregates.get(0).rule().strength(), 1.0e-9);

        double radiusOne = DynamicRadiusModel.effectiveRadius(
                aggregates.get(0).rule().radius(),
                DynamicRadiusModel.aggregateUnitsForItems(1));
        double radiusMany = DynamicRadiusModel.effectiveRadius(
                aggregates.get(0).rule().radius(),
                DynamicRadiusModel.aggregateUnitsForItems(aggregates.get(0).aggregateCount()));
        assertTrue(radiusMany >= radiusOne);
    }

    @Test
    void capabilityBackedDescriptorCreatesAggregates() {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        ItemStackHandler handler = new ItemStackHandler(3);
        handler.setStackInSlot(0, new ItemStack(Items.ROTTEN_FLESH, 8));

        List<ItemStack> stacks = EntityInventoryCarrierExtraction.stacksFromItemHandler(handler);
        var aggregates = EntityInventoryCarrierExtraction.aggregateRadioactiveStacks(stacks, rules);
        assertEquals(1, aggregates.size());
        assertEquals(8, aggregates.get(0).aggregateCount());
    }

    @Test
    void dedupeKeyIncludesLogicalGroupAndCountSnapshot() {
        String left = EntityInventoryCarrierExtraction.dedupeKey(
                "uuid",
                ResourceLocation.parse("minecraft:rotten_flesh"),
                "item:minecraft:rotten_flesh",
                64,
                "chest_boat");
        String right = EntityInventoryCarrierExtraction.dedupeKey(
                "uuid",
                ResourceLocation.parse("minecraft:rotten_flesh"),
                "item:minecraft:rotten_flesh",
                64,
                "generic_entity_inventory");
        assertNotEquals(left, right);
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
