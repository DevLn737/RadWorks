package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.EntityCarrierDiagnostics;
import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EntityCarrierSourceProvider {
    private EntityCarrierSourceProvider() {
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics) {
        return PerformanceStats.timeValue("entityCarrierScan", () -> collectTimed(player, rules, summary, diagnostics));
    }

    private static List<RadiationSource> collectTimed(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics) {
        if (!rules.loaded() || rules.itemRules() == 0) {
            return List.of();
        }
        if (!RadWorksConfig.entityCarriersEnabled()) {
            return List.of();
        }

        int scanRadius = effectiveScanRadius(rules);
        if (scanRadius <= 0) {
            return List.of();
        }

        ServerLevel level = player.serverLevel();
        Vec3 playerPosition = player.position();
        AABB bounds = player.getBoundingBox().inflate(scanRadius);
        List<RadiationSource> sources = new ArrayList<>();

        List<Entity> entities = level.getEntities(player, bounds, EntityCarrierSourceProvider::isRelevantEntity);
        for (Entity entity : entities) {
            summary.entityCarrierEntityChecked();
            diagnostics.scannedEntity();

            if (entity instanceof ItemEntity itemEntity) {
                handleDroppedItemSource(playerPosition, itemEntity, rules, summary, diagnostics, sources);
                continue;
            }
            if (entity instanceof ItemFrame itemFrame) {
                handleItemFrameSource(playerPosition, itemFrame, rules, summary, diagnostics, sources);
                continue;
            }
            if (entity instanceof ServerPlayer auraPlayer) {
                handlePlayerAuraSource(player, playerPosition, auraPlayer, rules, summary, diagnostics, sources);
                continue;
            }

            diagnostics.skippedEntity(
                    "unknown",
                    entityTypeId(entity),
                    entity.getStringUUID(),
                    null,
                    0,
                    "unsupported_entity_type");
            summary.entityCarrierSkipped();
        }

        return List.copyOf(sources);
    }

    private static boolean isRelevantEntity(Entity entity) {
        return (RadWorksConfig.entityDroppedItemsEnabled() && entity instanceof ItemEntity)
                || (RadWorksConfig.entityItemFramesEnabled() && entity instanceof ItemFrame)
                || (RadWorksConfig.entityPlayerAuraEnabled() && entity instanceof ServerPlayer);
    }

    private static void handleDroppedItemSource(
            Vec3 playerPosition,
            ItemEntity itemEntity,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            List<RadiationSource> sources) {
        ItemStack stack = itemEntity.getItem();
        EntityCarrierExtraction.MatchedStack match = EntityCarrierExtraction.matchRadioactiveStack(stack, rules).orElse(null);
        if (match == null) {
            diagnostics.skippedEntity(
                    "dropped_item",
                    entityTypeId(itemEntity),
                    itemEntity.getStringUUID(),
                    stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()),
                    stack.getCount(),
                    stack.isEmpty() ? "empty_stack" : "no_active_rule");
            summary.entityCarrierSkipped();
            return;
        }
        addEntityItemSource(
                RadiationSourceType.ENTITY_DROPPED_ITEM,
                "dropped_item",
                playerPosition,
                itemEntity.blockPosition(),
                entityTypeId(itemEntity),
                itemEntity.getStringUUID(),
                match,
                summary,
                diagnostics,
                sources);
    }

    private static void handleItemFrameSource(
            Vec3 playerPosition,
            ItemFrame itemFrame,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            List<RadiationSource> sources) {
        ItemStack displayed = itemFrame.getItem();
        EntityCarrierExtraction.MatchedStack match = EntityCarrierExtraction.matchRadioactiveStack(displayed, rules).orElse(null);
        if (match == null) {
            diagnostics.skippedEntity(
                    "item_frame",
                    entityTypeId(itemFrame),
                    itemFrame.getStringUUID(),
                    displayed.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(displayed.getItem()),
                    displayed.getCount(),
                    displayed.isEmpty() ? "empty_stack" : "no_active_rule");
            summary.entityCarrierSkipped();
            return;
        }
        addEntityItemSource(
                RadiationSourceType.ENTITY_ITEM_FRAME,
                "item_frame",
                playerPosition,
                itemFrame.blockPosition(),
                entityTypeId(itemFrame),
                itemFrame.getStringUUID(),
                match,
                summary,
                diagnostics,
                sources);
    }

    private static void handlePlayerAuraSource(
            ServerPlayer executingPlayer,
            Vec3 playerPosition,
            ServerPlayer auraPlayer,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            List<RadiationSource> sources) {
        if (EntityCarrierExtraction.shouldSkipSelfAura(executingPlayer.getUUID(), auraPlayer.getUUID())) {
            diagnostics.skippedEntity(
                    "player_world_source",
                    entityTypeId(auraPlayer),
                    auraPlayer.getStringUUID(),
                    null,
                    0,
                    "self_player_skipped");
            summary.entityCarrierSkipped();
            return;
        }

        Inventory inventory = auraPlayer.getInventory();
        List<ItemStack> stacks = new ArrayList<>(inventory.items.size() + inventory.offhand.size());
        stacks.addAll(inventory.items);
        stacks.addAll(inventory.offhand);
        List<EntityCarrierExtraction.MatchedAggregate> aggregates =
                EntityCarrierExtraction.aggregateRadioactiveStacks(stacks, rules);
        if (aggregates.isEmpty()) {
            diagnostics.skippedEntity(
                    "player_world_source",
                    entityTypeId(auraPlayer),
                    auraPlayer.getStringUUID(),
                    null,
                    0,
                    "no_active_rule");
            summary.entityCarrierSkipped();
            return;
        }

        BlockPos sourcePos = auraPlayer.blockPosition();
        ResourceLocation entityType = entityTypeId(auraPlayer);
        String entityId = auraPlayer.getStringUUID();
        double distance = playerPosition.distanceTo(Vec3.atCenterOf(sourcePos));
        for (EntityCarrierExtraction.MatchedAggregate aggregate : aggregates) {
            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            if (!DynamicRadiusModel.isActive(distance, effectiveRadius)) {
                diagnostics.skippedEntity(
                        "player_world_source",
                        entityType,
                        entityId,
                        aggregate.itemId(),
                        aggregate.aggregateCount(),
                        DynamicRadiusModel.outsideDynamicRadiusReason());
                summary.entityCarrierSkipped();
                continue;
            }
            summary.entityCarrierPlayerAuraMatch();
            summary.aggregateRowProduced();
            diagnostics.matchedPlayerAuraSource();
            sources.add(RadiationSource.entityCarrierItem(
                    RadiationSourceType.ENTITY_PLAYER_INVENTORY_AURA,
                    "player_world_source",
                    entityType.toString(),
                    entityId,
                    sourcePos,
                    aggregate.itemId(),
                    aggregate.aggregateCount(),
                    aggregate.contributingStacks(),
                    aggregate.rule().strength(),
                    baseRadius,
                    effectiveRadius,
                    distance,
                    aggregate.rule().respectsShielding(),
                    aggregate.aggregateCount() * aggregate.rule().strength(),
                    "Entity player aura source from nearby inventory id="
                            + aggregate.itemId()
                            + " count="
                            + aggregate.aggregateCount()));
        }
    }

    private static void addEntityItemSource(
            RadiationSourceType sourceType,
            String sourceKind,
            Vec3 playerPosition,
            BlockPos sourcePos,
            ResourceLocation entityType,
            String entityId,
            EntityCarrierExtraction.MatchedStack match,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            List<RadiationSource> sources) {
        double distance = playerPosition.distanceTo(Vec3.atCenterOf(sourcePos));
        double baseRadius = match.rule().radius();
        double units = DynamicRadiusModel.aggregateUnitsForItems(match.count());
        double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
        if (!DynamicRadiusModel.isActive(distance, effectiveRadius)) {
            diagnostics.skippedEntity(
                    sourceKind,
                    entityType,
                    entityId,
                    match.itemId(),
                    match.count(),
                    DynamicRadiusModel.outsideDynamicRadiusReason());
            summary.entityCarrierSkipped();
            return;
        }

        if (sourceType == RadiationSourceType.ENTITY_DROPPED_ITEM) {
            summary.entityCarrierDroppedItemMatch();
            diagnostics.matchedDroppedItemSource();
        } else if (sourceType == RadiationSourceType.ENTITY_ITEM_FRAME) {
            summary.entityCarrierItemFrameMatch();
            diagnostics.matchedItemFrameSource();
        }
        summary.aggregateRowProduced();
        sources.add(RadiationSource.entityCarrierItem(
                sourceType,
                sourceKind,
                entityType.toString(),
                entityId,
                sourcePos,
                match.itemId(),
                match.count(),
                1,
                match.rule().strength(),
                baseRadius,
                effectiveRadius,
                distance,
                match.rule().respectsShielding(),
                match.count() * match.rule().strength(),
                "Entity carried item source matched active rule id="
                        + match.itemId()
                        + " count="
                        + match.count()));
    }

    private static ResourceLocation entityTypeId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    private static int effectiveScanRadius(RadiationRules rules) {
        double baseMax = rules.maxActiveItemRuleRadius();
        double dynamicMax = RadWorksConfig.dynamicRadiusEnabled()
                ? Math.max(baseMax, RadWorksConfig.dynamicRadiusMaxCap())
                : baseMax;
        return (int) Math.ceil(Math.min(dynamicMax, RadWorksConfig.entityCarrierMaxScanRadius()));
    }
}
