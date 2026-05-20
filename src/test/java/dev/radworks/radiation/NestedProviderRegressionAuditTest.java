package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

class NestedProviderRegressionAuditTest {
    @Test
    void nestedDisablePathSkipsChildrenButKeepsDirectItems() throws Exception {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        ItemStack direct = new ItemStack(Items.ROTTEN_FLESH, 2);
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX, 1);
        shulker.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.ROTTEN_FLESH, 5))));

        boolean previous = RadWorksConfig.nestedContainersEnabled();
        try {
            setNestedContainersEnabled(false);
            List<EntityCarrierExtraction.MatchedAggregate> disabledAggregates =
                    EntityCarrierExtraction.aggregateRadioactiveStacks(
                            List.of(direct, shulker),
                            "entity_inventory",
                            rules,
                            NestedContainerDiagnostics.builder());
            assertEquals(1, disabledAggregates.size());
            assertEquals(2, disabledAggregates.get(0).aggregateCount());

            setNestedContainersEnabled(true);
            List<EntityCarrierExtraction.MatchedAggregate> enabledAggregates =
                    EntityCarrierExtraction.aggregateRadioactiveStacks(
                            List.of(direct, shulker),
                            "entity_inventory",
                            rules,
                            NestedContainerDiagnostics.builder());
            assertEquals(1, enabledAggregates.size());
            assertEquals(7, enabledAggregates.get(0).aggregateCount());
        } finally {
            setNestedContainersEnabled(previous);
        }
    }

    @Test
    void directAndNestedSameItemAggregateWithoutDoubleCount() {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        ItemStack direct = new ItemStack(Items.ROTTEN_FLESH, 2);
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX, 1);
        shulker.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.ROTTEN_FLESH, 3))));

        List<EntityCarrierExtraction.MatchedAggregate> aggregates =
                EntityCarrierExtraction.aggregateRadioactiveStacks(
                        List.of(direct, shulker),
                        "entity_inventory",
                        rules,
                        NestedContainerDiagnostics.builder());

        assertEquals(1, aggregates.size());
        EntityCarrierExtraction.MatchedAggregate aggregate = aggregates.get(0);
        assertEquals(5, aggregate.aggregateCount());
        assertEquals(2, aggregate.contributingStacks());
        assertEquals(1, aggregate.nestedMatches());
    }

    @Test
    void nestedSourceFieldsAreStructuredOnRow() {
        RadiationSource source = RadiationSource.playerInventoryAggregate(
                        ResourceLocation.parse("minecraft:rotten_flesh"),
                        5,
                        2,
                        1.0D,
                        2.0D,
                        2.5D,
                        5.0D,
                        "test")
                .withExtractionContext("player_inventory.slot[4].slot[1]", "data_component_container")
                .withNestedContext(
                        2,
                        ResourceLocation.parse("minecraft:shulker_box"),
                        "player_inventory.slot[4].slot[1]");

        JsonObject json = source.toJson();
        assertTrue(json.get("nested").getAsBoolean());
        assertEquals(2, json.get("nestedDepth").getAsInt());
        assertEquals("minecraft:shulker_box", json.get("containerItemId").getAsString());
        assertEquals("player_inventory.slot[4].slot[1]", json.get("containerPath").getAsString());
        assertEquals("data_component_container", json.get("extractionMode").getAsString());
    }

    @Test
    void nestedDiagnosticsContainsExpectedCounters() {
        NestedContainerDiagnostics.Builder builder = NestedContainerDiagnostics.builder();
        builder.nestedContainerChecked();
        builder.nestedContainerSupported();
        builder.nestedContainerUnsupported();
        builder.nestedStackExtracted();
        builder.nestedRadioactiveMatch();
        builder.nestedDepthLimitHit();
        builder.nestedItemLimitHit();
        builder.nestedMalformedContainer();
        NestedContainerDiagnostics.store(builder);

        JsonObject json = NestedContainerDiagnostics.lastToJson().getAsJsonObject();
        assertTrue(json.has("nestedContainersChecked"));
        assertTrue(json.has("nestedContainersSupported"));
        assertTrue(json.has("nestedContainersUnsupported"));
        assertTrue(json.has("nestedStacksExtracted"));
        assertTrue(json.has("nestedRadioactiveMatches"));
        assertTrue(json.has("nestedDepthLimitHits"));
        assertTrue(json.has("nestedItemLimitHits"));
        assertTrue(json.has("nestedMalformedContainers"));
        assertFalse(json.has("rawNbt"));
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

    private static void setNestedContainersEnabled(boolean value) throws Exception {
        Field field = RadWorksConfig.class.getDeclaredField("NESTED_CONTAINERS_ENABLED");
        field.setAccessible(true);
        ModConfigSpec.BooleanValue configValue = (ModConfigSpec.BooleanValue) field.get(null);
        configValue.set(value);
        configValue.clearCache();
    }
}
