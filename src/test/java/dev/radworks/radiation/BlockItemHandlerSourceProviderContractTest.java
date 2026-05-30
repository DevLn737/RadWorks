package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.diagnostics.NestedContainerDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.Test;

class BlockItemHandlerSourceProviderContractTest {
    @Test
    void collectHandlerSlots_whenMatchingItemPresent_shouldCreateBlockItemHandlerSource() throws Exception {
        IItemHandler handler = new StaticItemHandler(List.of(new ItemStack(Items.ROTTEN_FLESH, 4)));
        List<RadiationSource> sources = new ArrayList<>();

        invokeCollectHandlerSlots(
                ResourceLocation.parse("minecraft:barrel"),
                new BlockPos(2, 64, 2),
                1.0D,
                handlerLookup(handler, "unsided"),
                rulesWithItem("minecraft:rotten_flesh"),
                sources,
                SourceScanSummary.builder(),
                NestedContainerDiagnostics.builder(),
                ForceSourceCandidateSink.NO_OP);

        assertEquals(1, sources.size());
        RadiationSource source = sources.getFirst();
        assertEquals(RadiationSourceType.BLOCK_ITEM_HANDLER, source.type());
        assertEquals("minecraft:rotten_flesh", source.itemId().toString());
        assertEquals(4, source.aggregateCount());
        assertEquals("unsided", source.capabilityContext());
    }

    @Test
    void collectHandlerSlots_whenNestedContainerContainsMatchingItem_shouldExposeNestedFields() throws Exception {
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX, 1);
        shulker.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.ROTTEN_FLESH, 3))));
        IItemHandler handler = new StaticItemHandler(List.of(shulker));

        List<RadiationSource> sources = new ArrayList<>();
        invokeCollectHandlerSlots(
                ResourceLocation.parse("minecraft:barrel"),
                new BlockPos(2, 64, 2),
                1.0D,
                handlerLookup(handler, "unsided"),
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
        assertTrue(source.containerPath().contains("block_item_handler.pos"));
    }

    @Test
    void collectHandlerSlots_whenNoMatchingItemRule_shouldEmitForceCandidate() throws Exception {
        IItemHandler handler = new StaticItemHandler(List.of(new ItemStack(Items.APPLE, 2)));
        ForceSourceCandidateCollector candidates = new ForceSourceCandidateCollector(RadiationTargetKind.PLAYER);
        List<RadiationSource> sources = new ArrayList<>();

        invokeCollectHandlerSlots(
                ResourceLocation.parse("minecraft:barrel"),
                new BlockPos(2, 64, 2),
                1.0D,
                handlerLookup(handler, "unsided"),
                rulesWithItem("minecraft:rotten_flesh"),
                sources,
                SourceScanSummary.builder(),
                NestedContainerDiagnostics.builder(),
                candidates);

        assertTrue(sources.isEmpty());
        assertEquals(1, candidates.snapshot().size());
        ForceSourceCandidate candidate = candidates.snapshot().getFirst();
        assertEquals(RadiationSourceType.BLOCK_ITEM_HANDLER, candidate.sourceType());
        assertEquals("minecraft:apple", candidate.itemId().toString());
        assertEquals("minecraft:barrel", candidate.carrierBlockId().toString());
        assertEquals(2, candidate.count());
    }

    private static Object handlerLookup(IItemHandler handler, String context) throws Exception {
        Class<?> lookupType = Class.forName("dev.radworks.radiation.BlockItemHandlerSourceProvider$HandlerLookup");
        Constructor<?> ctor = lookupType.getDeclaredConstructor(IItemHandler.class, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(handler, context);
    }

    private static void invokeCollectHandlerSlots(
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            Object handlerLookup,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            ForceSourceCandidateSink candidateSink)
            throws Exception {
        Class<?> lookupType = Class.forName("dev.radworks.radiation.BlockItemHandlerSourceProvider$HandlerLookup");
        Method method = BlockItemHandlerSourceProvider.class.getDeclaredMethod(
                "collectHandlerSlots",
                ResourceLocation.class,
                BlockPos.class,
                double.class,
                lookupType,
                RadiationRules.class,
                List.class,
                SourceScanSummary.Builder.class,
                NestedContainerDiagnostics.Builder.class,
                ForceSourceCandidateSink.class);
        method.setAccessible(true);
        method.invoke(
                null,
                blockId,
                pos,
                distance,
                handlerLookup,
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

    private static final class StaticItemHandler implements IItemHandler {
        private final List<ItemStack> stacks;

        private StaticItemHandler(List<ItemStack> stacks) {
            this.stacks = stacks;
        }

        @Override
        public int getSlots() {
            return stacks.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return stacks.get(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }
}
