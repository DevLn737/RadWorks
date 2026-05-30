package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

class PlayerInventorySourceProviderContractTest {
    @Test
    void collectStack_whenDirectAndNestedRadioactiveItemsPresent_shouldAggregateBoth() throws Exception {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        ItemStack direct = new ItemStack(Items.ROTTEN_FLESH, 2);
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX, 1);
        shulker.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.ROTTEN_FLESH, 5))));

        Map<Object, Object> aggregates = new LinkedHashMap<>();
        Map<Object, Object> nestedMeta = new LinkedHashMap<>();
        SourceScanSummary.Builder summary = SourceScanSummary.builder();
        NestedContainerDiagnostics.Builder nestedDiagnostics = NestedContainerDiagnostics.builder();
        ForceSourceCandidateCollector candidates = new ForceSourceCandidateCollector(RadiationTargetKind.PLAYER);

        invokeCollectStack(direct, rules, aggregates, nestedMeta, summary, nestedDiagnostics, "player_inventory.slot[0]", candidates);
        invokeCollectStack(shulker, rules, aggregates, nestedMeta, summary, nestedDiagnostics, "player_inventory.slot[1]", candidates);

        assertEquals(1, aggregates.size());
        Object aggregate = aggregates.values().iterator().next();
        assertEquals(7, intGetter(aggregate, "aggregateCount"));
        assertEquals(2, intGetter(aggregate, "contributingStacks"));
        assertTrue(candidates.snapshot().stream().anyMatch(candidate ->
                candidate.itemId() != null && "minecraft:shulker_box".equals(candidate.itemId().toString())));
        assertFalse(nestedMeta.isEmpty());
    }

    @Test
    void collectStack_whenNestedDisabled_shouldKeepDirectAndSkipNested() throws Exception {
        RadiationRules rules = rulesWithItem("minecraft:rotten_flesh");
        ItemStack direct = new ItemStack(Items.ROTTEN_FLESH, 2);
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX, 1);
        shulker.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.ROTTEN_FLESH, 5))));

        boolean previousNested = RadWorksConfig.nestedContainersEnabled();
        try {
            setBoolean("NESTED_CONTAINERS_ENABLED", false);
            Map<Object, Object> aggregates = new LinkedHashMap<>();
            Map<Object, Object> nestedMeta = new LinkedHashMap<>();
            ForceSourceCandidateCollector candidates = new ForceSourceCandidateCollector(RadiationTargetKind.PLAYER);
            SourceScanSummary.Builder summary = SourceScanSummary.builder();
            NestedContainerDiagnostics.Builder nestedDiagnostics = NestedContainerDiagnostics.builder();

            invokeCollectStack(direct, rules, aggregates, nestedMeta, summary, nestedDiagnostics, "player_inventory.slot[0]", candidates);
            invokeCollectStack(shulker, rules, aggregates, nestedMeta, summary, nestedDiagnostics, "player_inventory.slot[1]", candidates);

            assertEquals(1, aggregates.size());
            Object aggregate = aggregates.values().iterator().next();
            assertEquals(2, intGetter(aggregate, "aggregateCount"));
            assertTrue(nestedMeta.isEmpty());
            assertEquals(1, candidates.snapshot().size());
            assertEquals("minecraft:shulker_box", candidates.snapshot().getFirst().itemId().toString());
        } finally {
            setBoolean("NESTED_CONTAINERS_ENABLED", previousNested);
        }
    }

    @Test
    void collectStack_whenNoMatchingItemRule_shouldEmitForceCandidate() throws Exception {
        RadiationRules rules = new RadiationRules(
                true,
                "test",
                List.of(),
                0,
                0,
                0,
                0,
                List.of(),
                new RadiationRuleValidationResult());
        ItemStack apple = new ItemStack(Items.APPLE, 3);

        Map<Object, Object> aggregates = new LinkedHashMap<>();
        Map<Object, Object> nestedMeta = new LinkedHashMap<>();
        ForceSourceCandidateCollector candidates = new ForceSourceCandidateCollector(RadiationTargetKind.PLAYER);

        invokeCollectStack(
                apple,
                rules,
                aggregates,
                nestedMeta,
                SourceScanSummary.builder(),
                NestedContainerDiagnostics.builder(),
                "player_inventory.slot[2]",
                candidates);

        assertTrue(aggregates.isEmpty());
        assertTrue(nestedMeta.isEmpty());
        assertEquals(1, candidates.snapshot().size());
        ForceSourceCandidate candidate = candidates.snapshot().getFirst();
        assertEquals(RadiationSourceType.PLAYER_INVENTORY, candidate.sourceType());
        assertEquals("minecraft:apple", candidate.itemId().toString());
        assertEquals(3, candidate.count());
        assertTrue(candidate.candidateReason().contains("player_inventory_observed_without_item_rule"));
    }

    private static void invokeCollectStack(
            ItemStack stack,
            RadiationRules rules,
            Map<Object, Object> aggregates,
            Map<Object, Object> nestedMeta,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            String sourcePath,
            ForceSourceCandidateSink candidateSink)
            throws Exception {
        Method method = PlayerInventorySourceProvider.class.getDeclaredMethod(
                "collectStack",
                ItemStack.class,
                RadiationRules.class,
                Map.class,
                Map.class,
                SourceScanSummary.Builder.class,
                NestedContainerDiagnostics.Builder.class,
                String.class,
                ForceSourceCandidateSink.class);
        method.setAccessible(true);
        method.invoke(null, stack, rules, aggregates, nestedMeta, summary, nestedDiagnostics, sourcePath, candidateSink);
    }

    private static int intGetter(Object aggregate, String methodName) throws Exception {
        Method getter = aggregate.getClass().getMethod(methodName);
        return (int) getter.invoke(aggregate);
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

    private static void setBoolean(String fieldName, boolean value) throws Exception {
        Field field = RadWorksConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ModConfigSpec.BooleanValue configValue = (ModConfigSpec.BooleanValue) field.get(null);
        configValue.set(value);
        configValue.clearCache();
    }
}
