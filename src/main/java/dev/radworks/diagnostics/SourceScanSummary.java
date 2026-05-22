package dev.radworks.diagnostics;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.time.Instant;

public record SourceScanSummary(
        Instant createdAt,
        int inventoryStacksChecked,
        int inventoryMatches,
        int blockPositionsChecked,
        int blockMatches,
        int worldFluidPositionsChecked,
        int worldFluidStatesFound,
        int worldFluidMatches,
        int worldFluidSkipped,
        int worldFluidDiscoveryRadius,
        int worldFluidClustersBuilt,
        int blockEntitiesChecked,
        int containerBlockEntitiesFound,
        int containerSlotsChecked,
        int containerMatches,
        int itemHandlerPositionsChecked,
        int itemHandlersFound,
        int itemHandlerSlotsChecked,
        int itemHandlerMatches,
        int skippedContainerBlockEntitiesForItemHandler,
        int fluidHandlerPositionsChecked,
        int fluidHandlersFound,
        int fluidTanksChecked,
        int fluidMatches,
        int createCarrierBlocksChecked,
        int createCarrierItemMatches,
        int createCarrierFluidMatches,
        int createCarrierUnexpectedStructures,
        int entityCarrierEntitiesChecked,
        int entityCarrierDroppedItemMatches,
        int entityCarrierItemFrameMatches,
        int entityCarrierPlayerAuraMatches,
        int entityCarrierInventoryEntitiesChecked,
        int entityCarrierInventoryAccessSucceeded,
        int entityCarrierInventoryAccessFailed,
        int entityCarrierChestBoatMatches,
        int entityCarrierPackAnimalMatches,
        int entityCarrierGenericInventoryMatches,
        int entityCarrierSkipped,
        int shieldingSourcesChecked,
        int shieldingSourcesApplicable,
        int shieldingSamplesChecked,
        int shieldingBlocksHit,
        int shieldingSourcesReduced,
        int livingShieldingSourcesChecked,
        int livingShieldingSourcesReduced,
        int livingShieldingSamplesChecked,
        int aggregateRowsProduced,
        int sourcesExcludedByOverride,
        int sourcesAfterOverrides,
        int sourcesContainedByOverride,
        int sourcesAfterContainment,
        int forceCandidatesObserved,
        int forcedSourcesAdded,
        int sourcesAfterForce,
        int sourcesShown,
        int sourcesOmitted) {
    private static volatile SourceScanSummary lastSummary;

    public static Builder builder() {
        return new Builder();
    }

    public static synchronized void store(Builder builder, int sourcesShown, int sourcesOmitted) {
        lastSummary = builder.build(sourcesShown, sourcesOmitted);
    }

    public static synchronized void updateOutputBounds(int sourcesShown, int sourcesOmitted) {
        if (lastSummary == null) {
            return;
        }
        lastSummary = new SourceScanSummary(
                lastSummary.createdAt,
                lastSummary.inventoryStacksChecked,
                lastSummary.inventoryMatches,
                lastSummary.blockPositionsChecked,
                lastSummary.blockMatches,
                lastSummary.worldFluidPositionsChecked,
                lastSummary.worldFluidStatesFound,
                lastSummary.worldFluidMatches,
                lastSummary.worldFluidSkipped,
                lastSummary.worldFluidDiscoveryRadius,
                lastSummary.worldFluidClustersBuilt,
                lastSummary.blockEntitiesChecked,
                lastSummary.containerBlockEntitiesFound,
                lastSummary.containerSlotsChecked,
                lastSummary.containerMatches,
                lastSummary.itemHandlerPositionsChecked,
                lastSummary.itemHandlersFound,
                lastSummary.itemHandlerSlotsChecked,
                lastSummary.itemHandlerMatches,
                lastSummary.skippedContainerBlockEntitiesForItemHandler,
                lastSummary.fluidHandlerPositionsChecked,
                lastSummary.fluidHandlersFound,
                lastSummary.fluidTanksChecked,
                lastSummary.fluidMatches,
                lastSummary.createCarrierBlocksChecked,
                lastSummary.createCarrierItemMatches,
                lastSummary.createCarrierFluidMatches,
                lastSummary.createCarrierUnexpectedStructures,
                lastSummary.entityCarrierEntitiesChecked,
                lastSummary.entityCarrierDroppedItemMatches,
                lastSummary.entityCarrierItemFrameMatches,
                lastSummary.entityCarrierPlayerAuraMatches,
                lastSummary.entityCarrierInventoryEntitiesChecked,
                lastSummary.entityCarrierInventoryAccessSucceeded,
                lastSummary.entityCarrierInventoryAccessFailed,
                lastSummary.entityCarrierChestBoatMatches,
                lastSummary.entityCarrierPackAnimalMatches,
                lastSummary.entityCarrierGenericInventoryMatches,
                lastSummary.entityCarrierSkipped,
                lastSummary.shieldingSourcesChecked,
                lastSummary.shieldingSourcesApplicable,
                lastSummary.shieldingSamplesChecked,
                lastSummary.shieldingBlocksHit,
                lastSummary.shieldingSourcesReduced,
                lastSummary.livingShieldingSourcesChecked,
                lastSummary.livingShieldingSourcesReduced,
                lastSummary.livingShieldingSamplesChecked,
                lastSummary.aggregateRowsProduced,
                lastSummary.sourcesExcludedByOverride,
                lastSummary.sourcesAfterOverrides,
                lastSummary.sourcesContainedByOverride,
                lastSummary.sourcesAfterContainment,
                lastSummary.forceCandidatesObserved,
                lastSummary.forcedSourcesAdded,
                lastSummary.sourcesAfterForce,
                sourcesShown,
                sourcesOmitted);
    }

    public static synchronized com.google.gson.JsonElement lastToJson() {
        if (lastSummary == null) {
            return JsonNull.INSTANCE;
        }
        return lastSummary.toJson();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("createdAt", createdAt.toString());
        json.addProperty("inventoryStacksChecked", inventoryStacksChecked);
        json.addProperty("inventoryMatches", inventoryMatches);
        json.addProperty("blockPositionsChecked", blockPositionsChecked);
        json.addProperty("blockMatches", blockMatches);
        json.addProperty("worldFluidPositionsChecked", worldFluidPositionsChecked);
        json.addProperty("worldFluidStatesFound", worldFluidStatesFound);
        json.addProperty("worldFluidMatches", worldFluidMatches);
        json.addProperty("worldFluidSkipped", worldFluidSkipped);
        json.addProperty("worldFluidDiscoveryRadius", worldFluidDiscoveryRadius);
        json.addProperty("worldFluidClustersBuilt", worldFluidClustersBuilt);
        json.addProperty("blockEntitiesChecked", blockEntitiesChecked);
        json.addProperty("containerBlockEntitiesFound", containerBlockEntitiesFound);
        json.addProperty("containerSlotsChecked", containerSlotsChecked);
        json.addProperty("containerMatches", containerMatches);
        json.addProperty("itemHandlerPositionsChecked", itemHandlerPositionsChecked);
        json.addProperty("itemHandlersFound", itemHandlersFound);
        json.addProperty("itemHandlerSlotsChecked", itemHandlerSlotsChecked);
        json.addProperty("itemHandlerMatches", itemHandlerMatches);
        json.addProperty("skippedContainerBlockEntitiesForItemHandler", skippedContainerBlockEntitiesForItemHandler);
        json.addProperty("fluidHandlerPositionsChecked", fluidHandlerPositionsChecked);
        json.addProperty("fluidHandlersFound", fluidHandlersFound);
        json.addProperty("fluidTanksChecked", fluidTanksChecked);
        json.addProperty("fluidMatches", fluidMatches);
        json.addProperty("createCarrierBlocksChecked", createCarrierBlocksChecked);
        json.addProperty("createCarrierItemMatches", createCarrierItemMatches);
        json.addProperty("createCarrierFluidMatches", createCarrierFluidMatches);
        json.addProperty("createCarrierUnexpectedStructures", createCarrierUnexpectedStructures);
        json.addProperty("entityCarrierEntitiesChecked", entityCarrierEntitiesChecked);
        json.addProperty("entityCarrierDroppedItemMatches", entityCarrierDroppedItemMatches);
        json.addProperty("entityCarrierItemFrameMatches", entityCarrierItemFrameMatches);
        json.addProperty("entityCarrierPlayerAuraMatches", entityCarrierPlayerAuraMatches);
        json.addProperty("entityCarrierInventoryEntitiesChecked", entityCarrierInventoryEntitiesChecked);
        json.addProperty("entityCarrierInventoryAccessSucceeded", entityCarrierInventoryAccessSucceeded);
        json.addProperty("entityCarrierInventoryAccessFailed", entityCarrierInventoryAccessFailed);
        json.addProperty("entityCarrierChestBoatMatches", entityCarrierChestBoatMatches);
        json.addProperty("entityCarrierPackAnimalMatches", entityCarrierPackAnimalMatches);
        json.addProperty("entityCarrierGenericInventoryMatches", entityCarrierGenericInventoryMatches);
        json.addProperty("entityCarrierSkipped", entityCarrierSkipped);
        json.addProperty("shieldingSourcesChecked", shieldingSourcesChecked);
        json.addProperty("shieldingSourcesApplicable", shieldingSourcesApplicable);
        json.addProperty("shieldingSamplesChecked", shieldingSamplesChecked);
        json.addProperty("shieldingBlocksHit", shieldingBlocksHit);
        json.addProperty("shieldingSourcesReduced", shieldingSourcesReduced);
        json.addProperty("livingShieldingSourcesChecked", livingShieldingSourcesChecked);
        json.addProperty("livingShieldingSourcesReduced", livingShieldingSourcesReduced);
        json.addProperty("livingShieldingSamplesChecked", livingShieldingSamplesChecked);
        json.addProperty("aggregateRowsProduced", aggregateRowsProduced);
        json.addProperty("sourcesExcludedByOverride", sourcesExcludedByOverride);
        json.addProperty("sourcesAfterOverrides", sourcesAfterOverrides);
        json.addProperty("sourcesContainedByOverride", sourcesContainedByOverride);
        json.addProperty("sourcesAfterContainment", sourcesAfterContainment);
        json.addProperty("forceCandidatesObserved", forceCandidatesObserved);
        json.addProperty("forcedSourcesAdded", forcedSourcesAdded);
        json.addProperty("sourcesAfterForce", sourcesAfterForce);
        json.addProperty("sourcesShown", sourcesShown);
        json.addProperty("sourcesOmitted", sourcesOmitted);

        JsonArray notes = new JsonArray();
        if (skippedContainerBlockEntitiesForItemHandler > 0) {
            notes.add("itemHandlerScan skipped vanilla Container block entities to avoid double counting with block_entity_inventory sources");
        }
        json.add("diagnosticNotes", notes);
        return json;
    }

    public static final class Builder {
        private int inventoryStacksChecked;
        private int inventoryMatches;
        private int blockPositionsChecked;
        private int blockMatches;
        private int worldFluidPositionsChecked;
        private int worldFluidStatesFound;
        private int worldFluidMatches;
        private int worldFluidSkipped;
        private int worldFluidDiscoveryRadius;
        private int worldFluidClustersBuilt;
        private int blockEntitiesChecked;
        private int containerBlockEntitiesFound;
        private int containerSlotsChecked;
        private int containerMatches;
        private int itemHandlerPositionsChecked;
        private int itemHandlersFound;
        private int itemHandlerSlotsChecked;
        private int itemHandlerMatches;
        private int skippedContainerBlockEntitiesForItemHandler;
        private int fluidHandlerPositionsChecked;
        private int fluidHandlersFound;
        private int fluidTanksChecked;
        private int fluidMatches;
        private int createCarrierBlocksChecked;
        private int createCarrierItemMatches;
        private int createCarrierFluidMatches;
        private int createCarrierUnexpectedStructures;
        private int entityCarrierEntitiesChecked;
        private int entityCarrierDroppedItemMatches;
        private int entityCarrierItemFrameMatches;
        private int entityCarrierPlayerAuraMatches;
        private int entityCarrierInventoryEntitiesChecked;
        private int entityCarrierInventoryAccessSucceeded;
        private int entityCarrierInventoryAccessFailed;
        private int entityCarrierChestBoatMatches;
        private int entityCarrierPackAnimalMatches;
        private int entityCarrierGenericInventoryMatches;
        private int entityCarrierSkipped;
        private int shieldingSourcesChecked;
        private int shieldingSourcesApplicable;
        private int shieldingSamplesChecked;
        private int shieldingBlocksHit;
        private int shieldingSourcesReduced;
        private int livingShieldingSourcesChecked;
        private int livingShieldingSourcesReduced;
        private int livingShieldingSamplesChecked;
        private int aggregateRowsProduced;
        private int sourcesExcludedByOverride;
        private int sourcesAfterOverrides;
        private int sourcesContainedByOverride;
        private int sourcesAfterContainment;
        private int forceCandidatesObserved;
        private int forcedSourcesAdded;
        private int sourcesAfterForce;

        public void inventoryStackChecked() {
            inventoryStacksChecked++;
        }

        public void inventoryMatch() {
            inventoryMatches++;
        }

        public void blockPositionChecked() {
            blockPositionsChecked++;
        }

        public void blockMatch() {
            blockMatches++;
        }

        public void worldFluidPositionChecked() {
            worldFluidPositionsChecked++;
        }

        public void worldFluidStateFound() {
            worldFluidStatesFound++;
        }

        public void worldFluidMatch() {
            worldFluidMatches++;
        }

        public void worldFluidSkipped() {
            worldFluidSkipped++;
        }

        public void worldFluidDiscoveryRadius(int radius) {
            worldFluidDiscoveryRadius = Math.max(0, radius);
        }

        public void worldFluidClusterBuilt() {
            worldFluidClustersBuilt++;
        }

        public void blockEntityChecked() {
            blockEntitiesChecked++;
        }

        public void containerBlockEntityFound() {
            containerBlockEntitiesFound++;
        }

        public void containerSlotChecked() {
            containerSlotsChecked++;
        }

        public void containerMatch() {
            containerMatches++;
        }

        public void itemHandlerPositionChecked() {
            itemHandlerPositionsChecked++;
        }

        public void itemHandlerFound() {
            itemHandlersFound++;
        }

        public void itemHandlerSlotChecked() {
            itemHandlerSlotsChecked++;
        }

        public void itemHandlerMatch() {
            itemHandlerMatches++;
        }

        public void skippedContainerBlockEntityForItemHandler() {
            skippedContainerBlockEntitiesForItemHandler++;
        }

        public void fluidHandlerPositionChecked() {
            fluidHandlerPositionsChecked++;
        }

        public void fluidHandlerFound() {
            fluidHandlersFound++;
        }

        public void fluidTankChecked() {
            fluidTanksChecked++;
        }

        public void fluidMatch() {
            fluidMatches++;
        }

        public void createCarrierBlockChecked() {
            createCarrierBlocksChecked++;
        }

        public void createCarrierItemMatch() {
            createCarrierItemMatches++;
        }

        public void createCarrierFluidMatch() {
            createCarrierFluidMatches++;
        }

        public void createCarrierUnexpectedStructure() {
            createCarrierUnexpectedStructures++;
        }

        public void entityCarrierEntityChecked() {
            entityCarrierEntitiesChecked++;
        }

        public void entityCarrierDroppedItemMatch() {
            entityCarrierDroppedItemMatches++;
        }

        public void entityCarrierItemFrameMatch() {
            entityCarrierItemFrameMatches++;
        }

        public void entityCarrierPlayerAuraMatch() {
            entityCarrierPlayerAuraMatches++;
        }

        public void entityCarrierInventoryEntityChecked() {
            entityCarrierInventoryEntitiesChecked++;
        }

        public void entityCarrierInventoryAccessSucceeded() {
            entityCarrierInventoryAccessSucceeded++;
        }

        public void entityCarrierInventoryAccessFailed() {
            entityCarrierInventoryAccessFailed++;
        }

        public void entityCarrierChestBoatMatch() {
            entityCarrierChestBoatMatches++;
        }

        public void entityCarrierPackAnimalMatch() {
            entityCarrierPackAnimalMatches++;
        }

        public void entityCarrierGenericInventoryMatch() {
            entityCarrierGenericInventoryMatches++;
        }

        public void entityCarrierSkipped() {
            entityCarrierSkipped++;
        }

        public void shieldingSourceChecked() {
            shieldingSourcesChecked++;
        }

        public void shieldingSourceApplicable() {
            shieldingSourcesApplicable++;
        }

        public void shieldingSampleChecked() {
            shieldingSamplesChecked++;
        }

        public void shieldingBlocksHit(int blocksHit) {
            shieldingBlocksHit += blocksHit;
        }

        public void shieldingSourceReduced() {
            shieldingSourcesReduced++;
        }

        public void livingShieldingSourceChecked() {
            livingShieldingSourcesChecked++;
        }

        public void livingShieldingSourceReduced() {
            livingShieldingSourcesReduced++;
        }

        public void livingShieldingSampleChecked() {
            livingShieldingSamplesChecked++;
        }

        public void aggregateRowProduced() {
            aggregateRowsProduced++;
        }

        public void sourceExcludedByOverride() {
            sourcesExcludedByOverride++;
        }

        public void sourceKeptAfterOverrides() {
            sourcesAfterOverrides++;
        }

        public void sourceContainedByOverride() {
            sourcesContainedByOverride++;
        }

        public void sourceAfterContainment() {
            sourcesAfterContainment++;
        }

        public void forceCandidateObserved() {
            forceCandidatesObserved++;
        }

        public void forcedSourceAdded() {
            forcedSourcesAdded++;
        }

        public void sourceAfterForce() {
            sourcesAfterForce++;
        }

        private SourceScanSummary build(int sourcesShown, int sourcesOmitted) {
            return new SourceScanSummary(
                    Instant.now(),
                    inventoryStacksChecked,
                    inventoryMatches,
                    blockPositionsChecked,
                    blockMatches,
                    worldFluidPositionsChecked,
                    worldFluidStatesFound,
                    worldFluidMatches,
                    worldFluidSkipped,
                    worldFluidDiscoveryRadius,
                    worldFluidClustersBuilt,
                    blockEntitiesChecked,
                    containerBlockEntitiesFound,
                    containerSlotsChecked,
                    containerMatches,
                    itemHandlerPositionsChecked,
                    itemHandlersFound,
                    itemHandlerSlotsChecked,
                    itemHandlerMatches,
                    skippedContainerBlockEntitiesForItemHandler,
                    fluidHandlerPositionsChecked,
                    fluidHandlersFound,
                    fluidTanksChecked,
                    fluidMatches,
                    createCarrierBlocksChecked,
                    createCarrierItemMatches,
                    createCarrierFluidMatches,
                    createCarrierUnexpectedStructures,
                    entityCarrierEntitiesChecked,
                    entityCarrierDroppedItemMatches,
                    entityCarrierItemFrameMatches,
                    entityCarrierPlayerAuraMatches,
                    entityCarrierInventoryEntitiesChecked,
                    entityCarrierInventoryAccessSucceeded,
                    entityCarrierInventoryAccessFailed,
                    entityCarrierChestBoatMatches,
                    entityCarrierPackAnimalMatches,
                    entityCarrierGenericInventoryMatches,
                    entityCarrierSkipped,
                    shieldingSourcesChecked,
                    shieldingSourcesApplicable,
                    shieldingSamplesChecked,
                    shieldingBlocksHit,
                    shieldingSourcesReduced,
                    livingShieldingSourcesChecked,
                    livingShieldingSourcesReduced,
                    livingShieldingSamplesChecked,
                    aggregateRowsProduced,
                    sourcesExcludedByOverride,
                    sourcesAfterOverrides,
                    sourcesContainedByOverride,
                    sourcesAfterContainment,
                    forceCandidatesObserved,
                    forcedSourcesAdded,
                    sourcesAfterForce,
                    sourcesShown,
                    sourcesOmitted);
        }
    }
}
