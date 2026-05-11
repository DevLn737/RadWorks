package dev.radworks.radiation;

import dev.radworks.diagnostics.SourceScanSummary;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
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
        Map<Key, AggregatedSourceAccumulator.ItemAggregate> aggregates = new LinkedHashMap<>();
        if (!rules.loaded()) {
            return List.of();
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            summary.inventoryStackChecked();
            collectStack(inventory.items.get(slot), rules, aggregates, summary);
        }
        for (int slot = 0; slot < inventory.offhand.size(); slot++) {
            summary.inventoryStackChecked();
            collectStack(inventory.offhand.get(slot), rules, aggregates, summary);
        }

        List<RadiationSource> sources = new ArrayList<>();
        for (AggregatedSourceAccumulator.ItemAggregate aggregate : aggregates.values()) {
            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            sources.add(RadiationSource.playerInventoryAggregate(
                    aggregate.key().itemId(),
                    aggregate.aggregateCount(),
                    aggregate.contributingStacks(),
                    aggregate.rule().strength(),
                    baseRadius,
                    effectiveRadius,
                    aggregate.rawContribution(),
                    "active item rule matched aggregated inventory units="
                            + aggregate.aggregateCount()
                            + " id="
                            + aggregate.key().itemId()));
            summary.aggregateRowProduced();
        }
        return List.copyOf(sources);
    }

    private static void collectStack(
            ItemStack stack,
            RadiationRules rules,
            Map<Key, AggregatedSourceAccumulator.ItemAggregate> aggregates,
            SourceScanSummary.Builder summary) {
        if (stack.isEmpty()) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Optional<RadiationRule> rule = rules.itemRule(itemId);
        if (rule.isEmpty()) {
            return;
        }

        summary.inventoryMatch();
        Key key = new Key(itemId, rule.get().key());
        AggregatedSourceAccumulator.ItemAggregate aggregate = aggregates.computeIfAbsent(
                key,
                ignored -> AggregatedSourceAccumulator.newItemAggregate(
                        new AggregatedSourceAccumulator.ItemGroupKey(
                                RadiationSourceType.PLAYER_INVENTORY,
                                null,
                                null,
                                null,
                                itemId,
                                rule.get().key()),
                        rule.get(),
                        0.0D));
        AggregatedSourceAccumulator.addItemStack(aggregate, stack.getCount());
    }

    private record Key(ResourceLocation itemId, String ruleKey) {
    }
}
