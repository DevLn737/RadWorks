package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.Test;

class NestedContainerExtractorTest {
    @Test
    void extractsVanillaContainerComponentStacks() {
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX, 1);
        ItemStack uranium = new ItemStack(Items.ROTTEN_FLESH, 7);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(uranium)));

        NestedContainerDiagnostics.Builder diagnostics = NestedContainerDiagnostics.builder();
        List<NestedContainerExtractor.ExtractedStack> extracted =
                NestedContainerExtractor.expand(shulker, "player_inventory.slot[5]", diagnostics);

        assertEquals(2, extracted.size());
        NestedContainerExtractor.ExtractedStack nested = extracted.get(1);
        assertTrue(nested.nested());
        assertEquals(1, nested.nestedDepth());
        assertEquals("data_component_container", nested.extractionMode());
        assertEquals("player_inventory.slot[5].slot[0]", nested.containerPath());
        assertEquals(BuiltInRegistries.ITEM.getKey(Items.ROTTEN_FLESH), nested.itemId());
        assertEquals(7, nested.count());
    }

    @Test
    void extractsBundleContentsStacks() {
        ItemStack bundle = new ItemStack(Items.BUNDLE, 1);
        ItemStack uranium = new ItemStack(Items.ROTTEN_FLESH, 3);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(uranium)));

        NestedContainerDiagnostics.Builder diagnostics = NestedContainerDiagnostics.builder();
        List<NestedContainerExtractor.ExtractedStack> extracted =
                NestedContainerExtractor.expand(bundle, "player_inventory.slot[1]", diagnostics);

        assertEquals(2, extracted.size());
        NestedContainerExtractor.ExtractedStack nested = extracted.get(1);
        assertTrue(nested.nested());
        assertEquals(1, nested.nestedDepth());
        assertEquals("bundle_contents", nested.extractionMode());
        assertEquals("player_inventory.slot[1].item[0]", nested.containerPath());
        assertEquals(3, nested.count());
    }

    @Test
    void respectsDepthLimitFromConfig() {
        ItemStack innerMost = new ItemStack(Items.ROTTEN_FLESH, 2);

        ItemStack level2 = new ItemStack(Items.SHULKER_BOX, 1);
        level2.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(innerMost)));

        ItemStack level1 = new ItemStack(Items.SHULKER_BOX, 1);
        level1.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(level2)));

        ItemStack root = new ItemStack(Items.SHULKER_BOX, 1);
        root.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(level1)));

        NestedContainerDiagnostics.Builder diagnostics = NestedContainerDiagnostics.builder();
        List<NestedContainerExtractor.ExtractedStack> extracted =
                NestedContainerExtractor.expand(root, "player_inventory.slot[0]", diagnostics);

        ResourceLocation rottenFlesh = BuiltInRegistries.ITEM.getKey(Items.ROTTEN_FLESH);
        assertFalse(extracted.stream().anyMatch(stack -> stack.itemId().equals(rottenFlesh) && stack.nestedDepth() > 2));
    }

    @Test
    void extractionDiagnosticsAreProducedForContainerLikeItems() {
        ItemStack chestLike = new ItemStack(Items.SHULKER_BOX, 1);
        NestedContainerDiagnostics.Builder diagnostics = NestedContainerDiagnostics.builder();

        List<NestedContainerExtractor.ExtractedStack> extracted =
                NestedContainerExtractor.expand(chestLike, "player_inventory.slot[2]", diagnostics);
        assertEquals(1, extracted.size());

        NestedContainerDiagnostics.store(diagnostics);
        JsonObject dump = NestedContainerDiagnostics.lastToJson().getAsJsonObject();
        assertTrue(dump.get("nestedContainersChecked").getAsInt() >= 1);
        int supported = dump.get("nestedContainersSupported").getAsInt();
        int unsupported = dump.get("nestedContainersUnsupported").getAsInt();
        assertTrue(supported + unsupported >= 1);
    }
}
