package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

final class EntityInventoryCarrierAdapter {
    private EntityInventoryCarrierAdapter() {
    }

    static AccessResult tryKnownVanilla(Entity entity) {
        ResourceLocation entityTypeId = entityTypeId(entity);
        String path = entityTypeId.getPath();
        if (RadWorksConfig.entityChestBoatsEnabled() && isChestBoatPath(path)) {
            return fromVanillaContainer(entity, "entity_chest_boat_inventory", "chest_boat");
        }
        if (RadWorksConfig.entityPackAnimalsEnabled() && isPackAnimalPath(path)) {
            return fromVanillaContainer(entity, "entity_pack_animal_inventory", "pack_animal");
        }
        return AccessResult.notApplicable();
    }

    static AccessResult tryGenericCapability(Entity entity) {
        if (!RadWorksConfig.entityGenericInventoryCapabilityEnabled()) {
            return AccessResult.notApplicable();
        }
        String logicalGroup = logicalGroupForEntity(entityTypeId(entity).getPath());
        IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        if (handler == null) {
            return AccessResult.failure("entity_generic_inventory_capability", logicalGroup, "no_inventory_capability");
        }
        List<ItemStack> stacks;
        try {
            stacks = EntityInventoryCarrierExtraction.stacksFromItemHandler(handler);
        } catch (RuntimeException exception) {
            return AccessResult.failure(
                    "entity_generic_inventory_capability",
                    logicalGroup,
                    "capability_scan_failed");
        }
        if (stacks.isEmpty()) {
            return AccessResult.failure("entity_generic_inventory_capability", logicalGroup, "inventory_empty");
        }
        return AccessResult.success(new InventoryView(
                "entity_generic_inventory_capability",
                "entity_item_handler_capability",
                logicalGroup,
                stacks));
    }

    static ResourceLocation entityTypeId(Entity entity) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (key != null) {
            return key;
        }
        return ResourceLocation.parse("minecraft:unknown_entity");
    }

    private static AccessResult fromVanillaContainer(Entity entity, String sourceKind, String logicalGroup) {
        if (!(entity instanceof Container container)) {
            return AccessResult.failure(sourceKind, logicalGroup, "unsupported_entity_inventory");
        }
        List<ItemStack> stacks;
        try {
            stacks = EntityInventoryCarrierExtraction.stacksFromContainer(container);
        } catch (RuntimeException exception) {
            return AccessResult.failure(sourceKind, logicalGroup, "vanilla_inventory_scan_failed");
        }
        if (stacks.isEmpty()) {
            return AccessResult.failure(sourceKind, logicalGroup, "inventory_empty");
        }
        return AccessResult.success(new InventoryView(sourceKind, "vanilla_inventory", logicalGroup, stacks));
    }

    static boolean isChestBoatPath(String path) {
        return path.endsWith("_chest_boat") || path.endsWith("_chest_raft") || "chest_boat".equals(path);
    }

    static boolean isPackAnimalPath(String path) {
        return "donkey".equals(path)
                || "mule".equals(path)
                || "llama".equals(path)
                || "trader_llama".equals(path);
    }

    private static String logicalGroupForEntity(String path) {
        if (isChestBoatPath(path)) {
            return "chest_boat";
        }
        if (isPackAnimalPath(path)) {
            return "pack_animal";
        }
        return "generic_entity_inventory";
    }

    static final class AccessResult {
        private static final AccessResult NOT_APPLICABLE = new AccessResult(null, null, null);

        private final InventoryView view;
        private final String sourceKind;
        private final String failureReason;

        private AccessResult(InventoryView view, String sourceKind, String failureReason) {
            this.view = view;
            this.sourceKind = sourceKind;
            this.failureReason = failureReason;
        }

        static AccessResult notApplicable() {
            return NOT_APPLICABLE;
        }

        static AccessResult success(InventoryView view) {
            return new AccessResult(view, null, null);
        }

        static AccessResult failure(String sourceKind, String logicalGroup, String reason) {
            return new AccessResult(new InventoryView(sourceKind, null, logicalGroup, List.of()), sourceKind, reason);
        }

        boolean applicable() {
            return this != NOT_APPLICABLE;
        }

        boolean success() {
            return view != null && !view.stacks().isEmpty() && failureReason == null;
        }

        InventoryView view() {
            return view;
        }

        String sourceKind() {
            return sourceKind;
        }

        String failureReason() {
            return failureReason;
        }
    }

    record InventoryView(
            String carrierSourceKind,
            String extractionMode,
            String logicalGroup,
            List<ItemStack> stacks) {
    }
}
