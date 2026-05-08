package dev.radworks.radiation;

import dev.radworks.diagnostics.SourceScanSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class PlayerInventorySourceProvider {
    private PlayerInventorySourceProvider() {
    }

    public static List<RadiationSource> collect(ServerPlayer player, RadiationRules rules) {
        return collect(player, rules, SourceScanSummary.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
        List<RadiationSource> sources = new ArrayList<>();
        if (!rules.loaded()) {
            return sources;
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            summary.inventoryStackChecked();
            collectStack("inventory." + slot, inventory.items.get(slot), rules, sources, summary);
        }
        for (int slot = 0; slot < inventory.offhand.size(); slot++) {
            summary.inventoryStackChecked();
            collectStack("offhand." + slot, inventory.offhand.get(slot), rules, sources, summary);
        }
        return List.copyOf(sources);
    }

    private static void collectStack(
            String slot,
            ItemStack stack,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary) {
        if (stack.isEmpty()) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Optional<RadiationRule> rule = rules.itemRule(itemId);
        if (rule.isEmpty()) {
            return;
        }

        double contribution = stack.getCount() * rule.get().strength();
        summary.inventoryMatch();
        sources.add(RadiationSource.playerInventory(
                itemId,
                slot,
                stack.getCount(),
                rule.get().strength(),
                rule.get().radius(),
                rule.get().respectsShielding(),
                contribution,
                "active item rule matched type=item id=" + itemId));
    }
}
