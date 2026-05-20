package dev.radworks.radiation;

import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
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
        return collect(player, rules, SourceScanSummary.builder(), NestedContainerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
        return collect(player, rules, summary, NestedContainerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        Map<Key, AggregatedSourceAccumulator.ItemAggregate> aggregates = new LinkedHashMap<>();
        Map<Key, NestedAggregateMeta> nestedMeta = new LinkedHashMap<>();
        if (!rules.loaded()) {
            return List.of();
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            summary.inventoryStackChecked();
            collectStack(
                    inventory.items.get(slot),
                    rules,
                    aggregates,
                    nestedMeta,
                    summary,
                    nestedDiagnostics,
                    "player_inventory.slot[" + slot + "]");
        }
        for (int slot = 0; slot < inventory.offhand.size(); slot++) {
            summary.inventoryStackChecked();
            collectStack(
                    inventory.offhand.get(slot),
                    rules,
                    aggregates,
                    nestedMeta,
                    summary,
                    nestedDiagnostics,
                    "player_inventory.offhand[" + slot + "]");
        }

        List<RadiationSource> sources = new ArrayList<>();
        for (AggregatedSourceAccumulator.ItemAggregate aggregate : aggregates.values()) {
            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            RadiationSource source = RadiationSource.playerInventoryAggregate(
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
                            + aggregate.key().itemId());
            NestedAggregateMeta nested = nestedMeta.get(new Key(aggregate.key().itemId(), aggregate.rule().key()));
            if (nested != null && nested.nestedMatches > 0) {
                source = source.withExtractionContext(
                                nested.firstContainerPath,
                                nested.firstExtractionMode)
                        .withMatchReasonSuffix(
                                "nested=true nestedMatches="
                                        + nested.nestedMatches
                                        + " nestedDepth="
                                        + nested.maxNestedDepth
                                        + " containerItemId="
                                        + nested.firstContainerItemId);
            }
            sources.add(source);
            summary.aggregateRowProduced();
        }
        return List.copyOf(sources);
    }

    private static void collectStack(
            ItemStack stack,
            RadiationRules rules,
            Map<Key, AggregatedSourceAccumulator.ItemAggregate> aggregates,
            Map<Key, NestedAggregateMeta> nestedMeta,
            SourceScanSummary.Builder summary,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            String sourcePath) {
        if (stack.isEmpty()) {
            return;
        }

        List<NestedContainerExtractor.ExtractedStack> extractedStacks =
                NestedContainerExtractor.expand(stack, sourcePath, nestedDiagnostics);
        for (NestedContainerExtractor.ExtractedStack extracted : extractedStacks) {
            Optional<RadiationRule> rule = rules.itemRule(extracted.itemId());
            if (rule.isEmpty()) {
                continue;
            }
            summary.inventoryMatch();
            Key key = new Key(extracted.itemId(), rule.get().key());
            AggregatedSourceAccumulator.ItemAggregate aggregate = aggregates.computeIfAbsent(
                    key,
                    ignored -> AggregatedSourceAccumulator.newItemAggregate(
                            new AggregatedSourceAccumulator.ItemGroupKey(
                                    RadiationSourceType.PLAYER_INVENTORY,
                                    null,
                                    null,
                                    null,
                                    extracted.itemId(),
                                    rule.get().key()),
                            rule.get(),
                            0.0D));
            AggregatedSourceAccumulator.addItemStack(aggregate, extracted.count());
            if (extracted.nested()) {
                nestedDiagnostics.nestedRadioactiveMatch();
                NestedAggregateMeta meta = nestedMeta.computeIfAbsent(key, ignored -> new NestedAggregateMeta());
                meta.record(extracted);
            }
        }
    }

    private record Key(ResourceLocation itemId, String ruleKey) {
    }

    private static final class NestedAggregateMeta {
        private int nestedMatches;
        private int maxNestedDepth;
        private ResourceLocation firstContainerItemId;
        private String firstContainerPath;
        private String firstExtractionMode;

        private void record(NestedContainerExtractor.ExtractedStack extracted) {
            nestedMatches++;
            maxNestedDepth = Math.max(maxNestedDepth, extracted.nestedDepth());
            if (firstContainerItemId == null) {
                firstContainerItemId = extracted.containerItemId();
            }
            if (firstContainerPath == null) {
                firstContainerPath = extracted.containerPath();
            }
            if (firstExtractionMode == null) {
                firstExtractionMode = extracted.extractionMode();
            }
        }
    }
}
