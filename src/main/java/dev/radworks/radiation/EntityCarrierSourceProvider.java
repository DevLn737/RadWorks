package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.EntityCarrierDiagnostics;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        return collect(
                player,
                player,
                rules,
                summary,
                diagnostics,
                NestedContainerDiagnostics.builder(),
                false,
                ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics) {
        return collect(
                player,
                player,
                rules,
                summary,
                diagnostics,
                nestedDiagnostics,
                false,
                ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            LivingEntity target,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            boolean includeSelfEntityInventory) {
        return collect(
                target,
                target,
                rules,
                summary,
                diagnostics,
                NestedContainerDiagnostics.builder(),
                includeSelfEntityInventory,
                ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            LivingEntity target,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            boolean includeSelfEntityInventory) {
        return collect(
                target,
                target,
                rules,
                summary,
                diagnostics,
                nestedDiagnostics,
                includeSelfEntityInventory,
                ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            LivingEntity target,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            boolean includeSelfEntityInventory,
            ForceSourceCandidateSink candidateSink) {
        return collect(
                target,
                target,
                rules,
                summary,
                diagnostics,
                nestedDiagnostics,
                includeSelfEntityInventory,
                candidateSink);
    }

    private static List<RadiationSource> collect(
            LivingEntity target,
            Entity excludedEntityForQuery,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            boolean includeSelfEntityInventory,
            ForceSourceCandidateSink candidateSink) {
        return PerformanceStats.timeValue(
                "entityCarrierScan",
                () -> collectTimed(
                        target,
                        excludedEntityForQuery,
                        rules,
                        summary,
                        diagnostics,
                        nestedDiagnostics,
                        includeSelfEntityInventory,
                        candidateSink));
    }

    private static List<RadiationSource> collectTimed(
            LivingEntity target,
            Entity excludedEntityForQuery,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            boolean includeSelfEntityInventory,
            ForceSourceCandidateSink candidateSink) {
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

        ServerLevel level = (ServerLevel) target.level();
        Vec3 targetPosition = target.position();
        AABB bounds = target.getBoundingBox().inflate(scanRadius);
        List<RadiationSource> sources = new ArrayList<>();

        if (includeSelfEntityInventory && !(target instanceof ServerPlayer)) {
            handleEntityInventorySource(
                    target.getUUID(),
                    targetPosition,
                    target,
                    rules,
                    summary,
                    diagnostics,
                    nestedDiagnostics,
                    sources,
                    candidateSink);
        }

        List<Entity> entities = level.getEntities(excludedEntityForQuery, bounds, EntityCarrierSourceProvider::isRelevantEntity);
        for (Entity entity : entities) {
            summary.entityCarrierEntityChecked();
            diagnostics.scannedEntity();

            if (entity instanceof ItemEntity itemEntity) {
                handleDroppedItemSource(targetPosition, itemEntity, rules, summary, diagnostics, nestedDiagnostics, sources, candidateSink);
                continue;
            }
            if (entity instanceof ItemFrame itemFrame) {
                handleItemFrameSource(targetPosition, itemFrame, rules, summary, diagnostics, nestedDiagnostics, sources, candidateSink);
                continue;
            }
            if (entity instanceof ServerPlayer auraPlayer) {
                handlePlayerAuraSource(
                        target.getUUID(),
                        targetPosition,
                        auraPlayer,
                        rules,
                        summary,
                        diagnostics,
                        nestedDiagnostics,
                        sources,
                        candidateSink);
                continue;
            }
            handleEntityInventorySource(
                    target.getUUID(),
                    targetPosition,
                    entity,
                    rules,
                    summary,
                    diagnostics,
                    nestedDiagnostics,
                    sources,
                    candidateSink);
        }

        return List.copyOf(sources);
    }

    private static boolean isRelevantEntity(Entity entity) {
        return (RadWorksConfig.entityDroppedItemsEnabled() && entity instanceof ItemEntity)
                || (RadWorksConfig.entityItemFramesEnabled() && entity instanceof ItemFrame)
                || (RadWorksConfig.entityPlayerAuraEnabled() && entity instanceof ServerPlayer)
                || isPotentialInventoryCarrier(entity);
    }

    private static boolean isPotentialInventoryCarrier(Entity entity) {
        if (!RadWorksConfig.entityCarriersEnabled() || entity instanceof ServerPlayer) {
            return false;
        }
        ResourceLocation typeId = EntityInventoryCarrierAdapter.entityTypeId(entity);
        String path = typeId.getPath();
        if (RadWorksConfig.entityChestBoatsEnabled() && EntityInventoryCarrierAdapter.isChestBoatPath(path)) {
            return true;
        }
        if (RadWorksConfig.entityPackAnimalsEnabled() && EntityInventoryCarrierAdapter.isPackAnimalPath(path)) {
            return true;
        }
        return RadWorksConfig.entityGenericInventoryCapabilityEnabled();
    }

    private static void handleDroppedItemSource(
            Vec3 playerPosition,
            ItemEntity itemEntity,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            List<RadiationSource> sources,
            ForceSourceCandidateSink candidateSink) {
        ItemStack stack = itemEntity.getItem();
        List<EntityCarrierExtraction.MatchedAggregate> aggregates = EntityCarrierExtraction.aggregateRadioactiveStackWithNested(
                stack,
                "entity_dropped_item.entity[" + itemEntity.getStringUUID() + "]",
                rules,
                nestedDiagnostics);
        if (aggregates.isEmpty()) {
            observeStackCandidate(
                    candidateSink,
                    RadiationSourceType.ENTITY_DROPPED_ITEM,
                    itemEntity.blockPosition(),
                    entityTypeId(itemEntity),
                    itemEntity.getStringUUID(),
                    stack,
                    "entity_dropped_item.entity[" + itemEntity.getStringUUID() + "]",
                    nestedDiagnostics,
                    playerPosition.distanceTo(Vec3.atCenterOf(itemEntity.blockPosition())),
                    "dropped_item_observed_without_item_rule");
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
        for (EntityCarrierExtraction.MatchedAggregate aggregate : aggregates) {
            addEntityAggregateSource(
                    RadiationSourceType.ENTITY_DROPPED_ITEM,
                    "dropped_item",
                    "entity_direct",
                    playerPosition,
                    itemEntity.blockPosition(),
                    entityTypeId(itemEntity),
                    itemEntity.getStringUUID(),
                    aggregate,
                    summary,
                    diagnostics,
                    sources,
                    candidateSink);
        }
    }

    private static void handleItemFrameSource(
            Vec3 playerPosition,
            ItemFrame itemFrame,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            List<RadiationSource> sources,
            ForceSourceCandidateSink candidateSink) {
        ItemStack displayed = itemFrame.getItem();
        List<EntityCarrierExtraction.MatchedAggregate> aggregates = EntityCarrierExtraction.aggregateRadioactiveStackWithNested(
                displayed,
                "entity_item_frame.entity[" + itemFrame.getStringUUID() + "]",
                rules,
                nestedDiagnostics);
        if (aggregates.isEmpty()) {
            observeStackCandidate(
                    candidateSink,
                    RadiationSourceType.ENTITY_ITEM_FRAME,
                    itemFrame.blockPosition(),
                    entityTypeId(itemFrame),
                    itemFrame.getStringUUID(),
                    displayed,
                    "entity_item_frame.entity[" + itemFrame.getStringUUID() + "]",
                    nestedDiagnostics,
                    playerPosition.distanceTo(Vec3.atCenterOf(itemFrame.blockPosition())),
                    "item_frame_observed_without_item_rule");
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
        for (EntityCarrierExtraction.MatchedAggregate aggregate : aggregates) {
            addEntityAggregateSource(
                    RadiationSourceType.ENTITY_ITEM_FRAME,
                    "item_frame",
                    "entity_direct",
                    playerPosition,
                    itemFrame.blockPosition(),
                    entityTypeId(itemFrame),
                    itemFrame.getStringUUID(),
                    aggregate,
                    summary,
                    diagnostics,
                    sources,
                    candidateSink);
        }
    }

    private static void handlePlayerAuraSource(
            java.util.UUID targetUuid,
            Vec3 targetPosition,
            ServerPlayer auraPlayer,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            List<RadiationSource> sources,
            ForceSourceCandidateSink candidateSink) {
        if (EntityCarrierExtraction.shouldSkipSelfAura(targetUuid, auraPlayer.getUUID())) {
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
                EntityCarrierExtraction.aggregateRadioactiveStacks(
                        stacks,
                        "entity_player_aura.entity[" + auraPlayer.getStringUUID() + "]",
                        rules,
                        nestedDiagnostics);
        if (aggregates.isEmpty()) {
            for (int slot = 0; slot < inventory.items.size(); slot++) {
                observeStackCandidate(
                        candidateSink,
                        RadiationSourceType.ENTITY_PLAYER_INVENTORY_AURA,
                        auraPlayer.blockPosition(),
                        entityTypeId(auraPlayer),
                        auraPlayer.getStringUUID(),
                        inventory.items.get(slot),
                        "entity_player_aura.entity[" + auraPlayer.getStringUUID() + "].slot[" + slot + "]",
                        nestedDiagnostics,
                        targetPosition.distanceTo(Vec3.atCenterOf(auraPlayer.blockPosition())),
                        "player_aura_observed_without_item_rule");
            }
            for (int slot = 0; slot < inventory.offhand.size(); slot++) {
                observeStackCandidate(
                        candidateSink,
                        RadiationSourceType.ENTITY_PLAYER_INVENTORY_AURA,
                        auraPlayer.blockPosition(),
                        entityTypeId(auraPlayer),
                        auraPlayer.getStringUUID(),
                        inventory.offhand.get(slot),
                        "entity_player_aura.entity[" + auraPlayer.getStringUUID() + "].offhand[" + slot + "]",
                        nestedDiagnostics,
                        targetPosition.distanceTo(Vec3.atCenterOf(auraPlayer.blockPosition())),
                        "player_aura_observed_without_item_rule");
            }
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
        Set<String> dedupe = new HashSet<>();
        double distance = targetPosition.distanceTo(Vec3.atCenterOf(sourcePos));
        for (EntityCarrierExtraction.MatchedAggregate aggregate : aggregates) {
            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            if (!DynamicRadiusModel.isActive(distance, effectiveRadius)) {
                candidateSink.observe(new ForceSourceCandidate(
                        ForceSourceCandidate.CandidateKind.ITEM,
                        RadiationSourceType.ENTITY_PLAYER_INVENTORY_AURA,
                        null,
                        aggregate.itemId(),
                        null,
                        sourcePos.immutable(),
                        entityType.toString(),
                        entityId,
                        aggregate.firstContainerItemId(),
                        aggregate.firstContainerPath(),
                        null,
                        null,
                        aggregate.aggregateCount(),
                        0,
                        distance,
                        aggregate.rule().respectsShielding(),
                        aggregate.nestedMatches() > 0,
                        aggregate.maxNestedDepth(),
                        "player_inventory_aura",
                        DynamicRadiusModel.outsideDynamicRadiusReason()));
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
            String dedupeKey = EntityInventoryCarrierExtraction.dedupeKey(
                    entityId,
                    aggregate.itemId(),
                    aggregate.rule().key(),
                    aggregate.aggregateCount(),
                    "player_world_source");
            if (!dedupe.add(dedupeKey)) {
                diagnostics.skippedEntity(
                        "player_world_source",
                        entityType,
                        entityId,
                        aggregate.itemId(),
                        aggregate.aggregateCount(),
                        "duplicate_inventory_access");
                summary.entityCarrierSkipped();
                continue;
            }
            summary.entityCarrierPlayerAuraMatch();
            summary.aggregateRowProduced();
            diagnostics.matchedPlayerAuraSource();
            sources.add(RadiationSource.entityInventoryCarrierItem(
                    "player_world_source",
                    "player_inventory_aura",
                    RadiationSourceType.ENTITY_PLAYER_INVENTORY_AURA,
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
                            + aggregate.aggregateCount()).withExtractionContext(
                                    aggregate.firstContainerPath(),
                                    aggregate.firstExtractionMode()).withNestedContext(
                                            aggregate.maxNestedDepth(),
                                            aggregate.firstContainerItemId(),
                                            aggregate.firstContainerPath()).withMatchReasonSuffix(
                                            nestedSuffix(aggregate)));
        }
    }

    private static void handleEntityInventorySource(
            java.util.UUID targetUuid,
            Vec3 targetPosition,
            Entity entity,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            List<RadiationSource> sources,
            ForceSourceCandidateSink candidateSink) {
        if (!RadWorksConfig.entityCarriersEnabled() || entity instanceof ServerPlayer) {
            return;
        }
        if (EntityCarrierExtraction.shouldSkipSelfAura(targetUuid, entity.getUUID())) {
            diagnostics.skippedEntity(
                    "entity_inventory",
                    entityTypeId(entity),
                    entity.getStringUUID(),
                    null,
                    0,
                    "self_player_skipped");
            summary.entityCarrierSkipped();
            return;
        }

        summary.entityCarrierInventoryEntityChecked();
        diagnostics.entityInventoryEntityChecked();

        Set<String> emitted = new HashSet<>();
        boolean anyAttempted = false;
        boolean anyMatched = false;
        boolean knownSuccess = false;

        EntityInventoryCarrierAdapter.AccessResult known = EntityInventoryCarrierAdapter.tryKnownVanilla(entity);
        if (known.applicable()) {
            anyAttempted = true;
            if (known.success()) {
                knownSuccess = true;
                summary.entityCarrierInventoryAccessSucceeded();
                diagnostics.entityInventoryAccessSucceeded();
                anyMatched |= addInventoryViewSources(
                        entity,
                        targetPosition,
                        known.view(),
                        rules,
                        emitted,
                        summary,
                        diagnostics,
                        nestedDiagnostics,
                        sources,
                        candidateSink);
            } else {
                summary.entityCarrierInventoryAccessFailed();
                diagnostics.entityInventoryAccessFailed();
                diagnostics.skippedEntity(
                        known.sourceKind(),
                        entityTypeId(entity),
                        entity.getStringUUID(),
                        null,
                        0,
                        known.failureReason());
                summary.entityCarrierSkipped();
            }
        }

        EntityInventoryCarrierAdapter.AccessResult capability = EntityInventoryCarrierAdapter.tryGenericCapability(entity);
        if (capability.applicable()) {
            anyAttempted = true;
            if (capability.success()) {
                summary.entityCarrierInventoryAccessSucceeded();
                diagnostics.entityInventoryAccessSucceeded();
                anyMatched |= addInventoryViewSources(
                        entity,
                        targetPosition,
                        capability.view(),
                        rules,
                        emitted,
                        summary,
                        diagnostics,
                        nestedDiagnostics,
                        sources,
                        candidateSink);
            } else {
                if (knownSuccess && "no_inventory_capability".equals(capability.failureReason())) {
                    return;
                }
                summary.entityCarrierInventoryAccessFailed();
                diagnostics.entityInventoryAccessFailed();
                diagnostics.skippedEntity(
                        capability.sourceKind(),
                        entityTypeId(entity),
                        entity.getStringUUID(),
                        null,
                        0,
                        capability.failureReason());
                summary.entityCarrierSkipped();
            }
        }

        if (!anyAttempted) {
            diagnostics.skippedEntity(
                    "entity_inventory",
                    entityTypeId(entity),
                    entity.getStringUUID(),
                    null,
                    0,
                    "unsupported_entity_inventory");
            summary.entityCarrierSkipped();
        }
    }

    private static boolean addInventoryViewSources(
            Entity entity,
            Vec3 playerPosition,
            EntityInventoryCarrierAdapter.InventoryView view,
            RadiationRules rules,
            Set<String> emitted,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            List<RadiationSource> sources,
            ForceSourceCandidateSink candidateSink) {
        List<EntityCarrierExtraction.MatchedAggregate> aggregates =
                EntityInventoryCarrierExtraction.aggregateRadioactiveStacks(
                        view.stacks(),
                        "entity_inventory.entity[" + entity.getStringUUID() + "]",
                        rules,
                        nestedDiagnostics);
        if (aggregates.isEmpty()) {
            int index = 0;
            for (ItemStack stack : view.stacks()) {
                observeStackCandidate(
                        candidateSink,
                        RadiationSourceType.ENTITY_INVENTORY,
                        entity.blockPosition(),
                        entityTypeId(entity),
                        entity.getStringUUID(),
                        stack,
                        "entity_inventory.entity[" + entity.getStringUUID() + "].slot[" + index + "]",
                        nestedDiagnostics,
                        playerPosition.distanceTo(Vec3.atCenterOf(entity.blockPosition())),
                        "entity_inventory_observed_without_item_rule");
                index++;
            }
            diagnostics.skippedEntity(
                    view.carrierSourceKind(),
                    entityTypeId(entity),
                    entity.getStringUUID(),
                    null,
                    0,
                    "no_radioactive_contents");
            summary.entityCarrierSkipped();
            return false;
        }

        boolean matched = false;
        ResourceLocation entityType = entityTypeId(entity);
        String entityUuid = entity.getStringUUID();
        BlockPos sourcePos = entity.blockPosition();
        double distance = playerPosition.distanceTo(Vec3.atCenterOf(sourcePos));
        for (EntityCarrierExtraction.MatchedAggregate aggregate : aggregates) {
            String dedupeKey = EntityInventoryCarrierExtraction.dedupeKey(
                    entityUuid,
                    aggregate.itemId(),
                    aggregate.rule().key(),
                    aggregate.aggregateCount(),
                    view.logicalGroup());
            if (!emitted.add(dedupeKey)) {
                diagnostics.skippedEntity(
                        view.carrierSourceKind(),
                        entityType,
                        entityUuid,
                        aggregate.itemId(),
                        aggregate.aggregateCount(),
                        "duplicate_inventory_access");
                summary.entityCarrierSkipped();
                continue;
            }

            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            if (!DynamicRadiusModel.isActive(distance, effectiveRadius)) {
                candidateSink.observe(new ForceSourceCandidate(
                        ForceSourceCandidate.CandidateKind.ITEM,
                        RadiationSourceType.ENTITY_INVENTORY,
                        null,
                        aggregate.itemId(),
                        null,
                        sourcePos.immutable(),
                        entityType.toString(),
                        entityUuid,
                        aggregate.firstContainerItemId(),
                        aggregate.firstContainerPath(),
                        null,
                        null,
                        aggregate.aggregateCount(),
                        0,
                        distance,
                        aggregate.rule().respectsShielding(),
                        aggregate.nestedMatches() > 0,
                        aggregate.maxNestedDepth(),
                        view.extractionMode(),
                        DynamicRadiusModel.outsideDynamicRadiusReason()));
                diagnostics.skippedEntity(
                        view.carrierSourceKind(),
                        entityType,
                        entityUuid,
                        aggregate.itemId(),
                        aggregate.aggregateCount(),
                        DynamicRadiusModel.outsideDynamicRadiusReason());
                summary.entityCarrierSkipped();
                continue;
            }

            matched = true;
            summary.aggregateRowProduced();
            if ("entity_chest_boat_inventory".equals(view.carrierSourceKind())) {
                summary.entityCarrierChestBoatMatch();
                diagnostics.matchedChestBoatSource();
            } else if ("entity_pack_animal_inventory".equals(view.carrierSourceKind())) {
                summary.entityCarrierPackAnimalMatch();
                diagnostics.matchedPackAnimalSource();
            } else {
                summary.entityCarrierGenericInventoryMatch();
                diagnostics.matchedGenericEntityInventorySource();
            }
            sources.add(RadiationSource.entityInventoryCarrierItem(
                    view.carrierSourceKind(),
                    view.extractionMode(),
                    RadiationSourceType.ENTITY_INVENTORY,
                    entityType.toString(),
                    entityUuid,
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
                    "Entity inventory source matched active item rule id="
                            + aggregate.itemId()
                            + " count="
                            + aggregate.aggregateCount()
                            + " carrier="
                            + view.carrierSourceKind()).withExtractionContext(
                                    aggregate.firstContainerPath(),
                                    aggregate.firstExtractionMode()).withNestedContext(
                                            aggregate.maxNestedDepth(),
                                            aggregate.firstContainerItemId(),
                                            aggregate.firstContainerPath()).withMatchReasonSuffix(
                                            nestedSuffix(aggregate)));
        }
        return matched;
    }

    private static void addEntityAggregateSource(
            RadiationSourceType sourceType,
            String sourceKind,
            String extractionMode,
            Vec3 playerPosition,
            BlockPos sourcePos,
            ResourceLocation entityType,
            String entityId,
            EntityCarrierExtraction.MatchedAggregate aggregate,
            SourceScanSummary.Builder summary,
            EntityCarrierDiagnostics.Builder diagnostics,
            List<RadiationSource> sources,
            ForceSourceCandidateSink candidateSink) {
        double distance = playerPosition.distanceTo(Vec3.atCenterOf(sourcePos));
        double baseRadius = aggregate.rule().radius();
        double units = DynamicRadiusModel.aggregateUnitsForItems(aggregate.aggregateCount());
        double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
        if (!DynamicRadiusModel.isActive(distance, effectiveRadius)) {
            candidateSink.observe(new ForceSourceCandidate(
                    ForceSourceCandidate.CandidateKind.ITEM,
                    sourceType,
                    null,
                    aggregate.itemId(),
                    null,
                    sourcePos.immutable(),
                    entityType.toString(),
                    entityId,
                    aggregate.firstContainerItemId(),
                    aggregate.firstContainerPath(),
                    null,
                    null,
                    aggregate.aggregateCount(),
                    0,
                    distance,
                    aggregate.rule().respectsShielding(),
                    aggregate.nestedMatches() > 0,
                    aggregate.maxNestedDepth(),
                    extractionMode,
                    DynamicRadiusModel.outsideDynamicRadiusReason()));
            diagnostics.skippedEntity(
                    sourceKind,
                    entityType,
                    entityId,
                    aggregate.itemId(),
                    aggregate.aggregateCount(),
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
        sources.add(RadiationSource.entityInventoryCarrierItem(
                sourceKind,
                extractionMode,
                sourceType,
                entityType.toString(),
                entityId,
                sourcePos,
                aggregate.itemId(),
                aggregate.aggregateCount(),
                Math.max(1, aggregate.contributingStacks()),
                aggregate.rule().strength(),
                baseRadius,
                effectiveRadius,
                distance,
                aggregate.rule().respectsShielding(),
                aggregate.aggregateCount() * aggregate.rule().strength(),
                "Entity carried item source matched active rule id="
                        + aggregate.itemId()
                        + " count="
                        + aggregate.aggregateCount()).withExtractionContext(
                                aggregate.firstContainerPath(),
                                aggregate.firstExtractionMode()).withNestedContext(
                                        aggregate.maxNestedDepth(),
                                        aggregate.firstContainerItemId(),
                                        aggregate.firstContainerPath()).withMatchReasonSuffix(
                                        nestedSuffix(aggregate)));
    }

    private static String nestedSuffix(EntityCarrierExtraction.MatchedAggregate aggregate) {
        if (aggregate.nestedMatches() <= 0) {
            return null;
        }
        return "nested=true nestedMatches="
                + aggregate.nestedMatches()
                + " nestedDepth="
                + aggregate.maxNestedDepth()
                + " containerItemId="
                + aggregate.firstContainerItemId();
    }

    private static void observeStackCandidate(
            ForceSourceCandidateSink candidateSink,
            RadiationSourceType sourceType,
            BlockPos sourcePos,
            ResourceLocation carrierEntityType,
            String carrierEntityId,
            ItemStack stack,
            String sourcePath,
            NestedContainerDiagnostics.Builder nestedDiagnostics,
            double distance,
            String reason) {
        if (stack.isEmpty()) {
            return;
        }
        for (NestedContainerExtractor.ExtractedStack extracted : NestedContainerExtractor.expand(stack, sourcePath, nestedDiagnostics)) {
            candidateSink.observe(new ForceSourceCandidate(
                    ForceSourceCandidate.CandidateKind.ITEM,
                    sourceType,
                    null,
                    extracted.itemId(),
                    null,
                    sourcePos.immutable(),
                    carrierEntityType == null ? null : carrierEntityType.toString(),
                    carrierEntityId,
                    extracted.containerItemId(),
                    extracted.containerPath(),
                    null,
                    null,
                    extracted.count(),
                    0,
                    distance,
                    true,
                    extracted.nested(),
                    extracted.nestedDepth(),
                    extracted.extractionMode(),
                    reason));
        }
    }

    private static ResourceLocation entityTypeId(Entity entity) {
        return EntityInventoryCarrierAdapter.entityTypeId(entity);
    }

    private static int effectiveScanRadius(RadiationRules rules) {
        double baseMax = rules.maxActiveItemRuleRadius();
        double dynamicMax = RadWorksConfig.dynamicRadiusEnabled()
                ? Math.max(baseMax, RadWorksConfig.dynamicRadiusMaxCap())
                : baseMax;
        return (int) Math.ceil(Math.min(dynamicMax, RadWorksConfig.entityCarrierMaxScanRadius()));
    }
}
