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

        private SourceScanSummary build(int sourcesShown, int sourcesOmitted) {
            return new SourceScanSummary(
                    Instant.now(),
                    inventoryStacksChecked,
                    inventoryMatches,
                    blockPositionsChecked,
                    blockMatches,
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
                    sourcesShown,
                    sourcesOmitted);
        }
    }
}
