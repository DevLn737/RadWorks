package dev.radworks.radiation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

final class EntityInventoryCarrierExtraction {
    private EntityInventoryCarrierExtraction() {
    }

    static List<ItemStack> stacksFromContainer(Container container) {
        int size = Math.max(0, container.getContainerSize());
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            stacks.add(stack);
        }
        return List.copyOf(stacks);
    }

    static List<ItemStack> stacksFromItemHandler(IItemHandler handler) {
        int size = Math.max(0, handler.getSlots());
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            stacks.add(stack);
        }
        return List.copyOf(stacks);
    }

    static Optional<EntityCarrierExtraction.MatchedStack> matchRadioactiveStack(ItemStack stack, RadiationRules rules) {
        return EntityCarrierExtraction.matchRadioactiveStack(stack, rules);
    }

    static List<EntityCarrierExtraction.MatchedAggregate> aggregateRadioactiveStacks(
            Iterable<ItemStack> stacks,
            RadiationRules rules) {
        return aggregateRadioactiveStacks(stacks, "entity_inventory", rules, NestedContainerDiagnostics.builder());
    }

    static List<EntityCarrierExtraction.MatchedAggregate> aggregateRadioactiveStacks(
            Iterable<ItemStack> stacks,
            String sourcePathPrefix,
            RadiationRules rules,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        return EntityCarrierExtraction.aggregateRadioactiveStacks(stacks, sourcePathPrefix, rules, nestedDiagnostics);
    }

    static String dedupeKey(
            String entityUuid,
            ResourceLocation itemId,
            String ruleKey,
            int countSnapshot,
            String logicalGroup) {
        return entityUuid
                + "|"
                + itemId
                + "|"
                + ruleKey
                + "|"
                + countSnapshot
                + "|"
                + logicalGroup;
    }

    static ResourceLocation itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
