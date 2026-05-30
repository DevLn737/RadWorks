package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.diagnostics.NestedContainerDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.Test;

class BlockEntityInventorySourceProviderContractTest {
    @Test
    void collectContainerSlots_whenContainerHasMatchingItems_shouldCreateAggregatedSource() throws Exception {
        SimpleContainer container = new SimpleContainer(3);
        container.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 2));
        container.setItem(1, new ItemStack(Items.ROTTEN_FLESH, 4));

        List<RadiationSource> sources = new ArrayList<>();
        invokeCollectContainerSlots(
                ResourceLocation.parse("minecraft:chest"),
                new BlockPos(1, 64, 1),
                1.0D,
                container,
                rulesWithItem("minecraft:rotten_flesh"),
                sources,
                SourceScanSummary.builder(),
                NestedContainerDiagnostics.builder(),
                ForceSourceCandidateSink.NO_OP);

        assertEquals(1, sources.size());
        RadiationSource source = sources.getFirst();
        assertEquals(RadiationSourceType.BLOCK_ENTITY_INVENTORY, source.type());
        assertEquals("minecraft:rotten_flesh", source.itemId().toString());
        assertEquals(6, source.aggregateCount());
        assertEquals(2, source.contributingStacks());
    }

    @Test
    void collectContainerSlots_whenNestedContainerHasMatchingItem_shouldMarkNestedMetadata() throws Exception {
        SimpleContainer container = new SimpleContainer(1);
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX, 1);
        shulker.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.ROTTEN_FLESH, 5))));
        container.setItem(0, shulker);

        List<RadiationSource> sources = new ArrayList<>();
        invokeCollectContainerSlots(
                ResourceLocation.parse("minecraft:chest"),
                new BlockPos(1, 64, 1),
                1.0D,
                container,
                rulesWithItem("minecraft:rotten_flesh"),
                sources,
                SourceScanSummary.builder(),
                NestedContainerDiagnostics.builder(),
                ForceSourceCandidateSink.NO_OP);

        assertEquals(1, sources.size());
        RadiationSource source = sources.getFirst();
        assertTrue(source.nested());
        assertEquals(1, source.nestedDepth());
        assertEquals("minecraft:shulker_box", source.containerItemId().toString());
        assertTrue(source.containerPath().contains("block_entity_inventory.pos"));
        assertTrue(source.matchReason().contains("nested=true"));
    }

    @Test
    void collectContainerSlots_whenNoMatchingRule_shouldEmitForceCandidate() throws Exception {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.APPLE, 1));
        ForceSourceCandidateCollector candidates = new ForceSourceCandidateCollector(RadiationTargetKind.PLAYER);

        List<RadiationSource> sources = new ArrayList<>();
        invokeCollectContainerSlots(
                ResourceLocation.parse("minecraft:chest"),
                new BlockPos(1, 64, 1),
                1.0D,
                container,
                rulesWithItem("minecraft:rotten_flesh"),
                sources,
                SourceScanSummary.builder(),
                NestedContainerDiagnostics.builder(),
                candidates);

        assertTrue(sources.isEmpty());
        assertEquals(1, candidates.snapshot().size());
        ForceSourceCandidate candidate = candidates.snapshot().getFirst();
        assertEquals(RadiationSourceType.BLOCK_ENTITY_INVENTORY, candidate.sourceType());
        assertEquals("minecraft:apple", candidate.itemId().toString());
        assertEquals("minecraft:chest", candidate.carrierBlockId().toString());
        assertFalse(candidate.nested());
    }

    private static void invokeCollectContainerSlots(
            ResourceLocation blockId,
            BlockPos containerPos,
            double distance,
            SimpleContainer container,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            ForceSourceCandidateSink candidateSink)
            throws Exception {
        Method method = BlockEntityInventorySourceProvider.class.getDeclaredMethod(
                "collectContainerSlots",
                ResourceLocation.class,
                BlockPos.class,
                double.class,
                net.minecraft.world.Container.class,
                RadiationRules.class,
                List.class,
                SourceScanSummary.Builder.class,
                NestedContainerDiagnostics.Builder.class,
                ForceSourceCandidateSink.class);
        method.setAccessible(true);
        method.invoke(
                null,
                blockId,
                containerPos,
                distance,
                container,
                rules,
                sources,
                summary,
                nestedDiagnostics,
                candidateSink);
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
