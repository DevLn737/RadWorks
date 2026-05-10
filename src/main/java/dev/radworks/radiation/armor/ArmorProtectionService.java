package dev.radworks.radiation.armor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArmorProtectionService {
    public static final TagKey<Item> RADIATION_PROTECTION_ARMOR = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("radworks", "radiation_protection_armor"));

    private static final Map<String, EquipmentSlot> SLOT_BY_ID = Map.of(
            "head", EquipmentSlot.HEAD,
            "chest", EquipmentSlot.CHEST,
            "legs", EquipmentSlot.LEGS,
            "feet", EquipmentSlot.FEET);

    private static final Map<String, ResourceLocation> DIAMOND_BY_SLOT = Map.of(
            "head", ResourceLocation.withDefaultNamespace("diamond_helmet"),
            "chest", ResourceLocation.withDefaultNamespace("diamond_chestplate"),
            "legs", ResourceLocation.withDefaultNamespace("diamond_leggings"),
            "feet", ResourceLocation.withDefaultNamespace("diamond_boots"));

    private ArmorProtectionService() {
    }

    public static ArmorProtectionResult evaluate(ServerPlayer player, double currentExposure) {
        String protectionSource;
        try {
            protectionSource = useTagDefinition() ? "tag" : "dev_diamond_set";
        } catch (RuntimeException exception) {
            return ArmorProtectionResult.unknown(currentExposure);
        }

        List<String> requiredPieces = List.of("head", "chest", "legs", "feet");
        List<String> equippedPieces = new ArrayList<>();
        List<String> missingPieces = new ArrayList<>();
        for (String slotId : requiredPieces) {
            EquipmentSlot equipmentSlot = SLOT_BY_ID.get(slotId);
            Item expectedItem = expectedItem(slotId);
            ItemStack equipped = player.getItemBySlot(equipmentSlot);
            if (!equipped.isEmpty() && expectedItem != Items.AIR && equipped.is(expectedItem)) {
                equippedPieces.add(slotId);
            } else {
                missingPieces.add(slotId);
            }
        }

        String status = status(equippedPieces.size());
        boolean full = "full".equals(status);
        return new ArmorProtectionResult(
                status,
                requiredPieces,
                List.copyOf(equippedPieces),
                List.copyOf(missingPieces),
                protectionSource,
                full,
                full,
                false,
                full ? 0.0D : currentExposure);
    }

    private static boolean useTagDefinition() {
        if (BuiltInRegistries.ITEM.getTag(RADIATION_PROTECTION_ARMOR).isEmpty()) {
            return false;
        }

        for (ResourceLocation id : DIAMOND_BY_SLOT.values()) {
            Item expectedItem = BuiltInRegistries.ITEM.get(id);
            if (expectedItem == Items.AIR || !expectedItem.builtInRegistryHolder().is(RADIATION_PROTECTION_ARMOR)) {
                return false;
            }
        }
        return true;
    }

    private static Item expectedItem(String slotId) {
        ResourceLocation id = DIAMOND_BY_SLOT.get(slotId);
        if (id == null) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.get(id);
    }

    private static String status(int equippedCount) {
        if (equippedCount <= 0) {
            return "none";
        }
        if (equippedCount == 4) {
            return "full";
        }
        return "partial";
    }
}
