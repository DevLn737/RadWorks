package dev.radworks.diagnostics;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class EntityCarrierDiagnostics {
    private static volatile Snapshot lastSnapshot;

    private EntityCarrierDiagnostics() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static synchronized void store(Builder builder) {
        lastSnapshot = builder.build();
    }

    public static synchronized com.google.gson.JsonElement lastToJson() {
        if (lastSnapshot == null) {
            return JsonNull.INSTANCE;
        }
        return lastSnapshot.toJson();
    }

    public static final class Builder {
        private final List<SkipSample> skipSamples = new ArrayList<>();
        private int scannedEntities;
        private int matchedDroppedItemSources;
        private int matchedItemFrameSources;
        private int matchedPlayerAuraSources;
        private int skippedEntities;

        public void scannedEntity() {
            scannedEntities++;
        }

        public void matchedDroppedItemSource() {
            matchedDroppedItemSources++;
        }

        public void matchedItemFrameSource() {
            matchedItemFrameSources++;
        }

        public void matchedPlayerAuraSource() {
            matchedPlayerAuraSources++;
        }

        public void skippedEntity(
                String carrierSourceKind,
                ResourceLocation carrierEntityType,
                String carrierEntityId,
                ResourceLocation itemId,
                int count,
                String reason) {
            skippedEntities++;
            int cap = RadWorksConfig.entityCarrierDiagnosticSampleCap();
            if (skipSamples.size() >= cap) {
                return;
            }
            skipSamples.add(new SkipSample(
                    carrierSourceKind,
                    carrierEntityType,
                    carrierEntityId,
                    itemId,
                    count,
                    reason));
        }

        private Snapshot build() {
            return new Snapshot(
                    Instant.now(),
                    scannedEntities,
                    matchedDroppedItemSources,
                    matchedItemFrameSources,
                    matchedPlayerAuraSources,
                    skippedEntities,
                    List.copyOf(skipSamples));
        }
    }

    private record Snapshot(
            Instant createdAt,
            int scannedEntities,
            int matchedDroppedItemSources,
            int matchedItemFrameSources,
            int matchedPlayerAuraSources,
            int skippedEntities,
            List<SkipSample> skipSamples) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("createdAt", createdAt.toString());
            json.addProperty("scannedEntities", scannedEntities);
            json.addProperty("matchedDroppedItemSources", matchedDroppedItemSources);
            json.addProperty("matchedItemFrameSources", matchedItemFrameSources);
            json.addProperty("matchedPlayerAuraSources", matchedPlayerAuraSources);
            json.addProperty("skippedEntities", skippedEntities);
            JsonArray samples = new JsonArray();
            for (SkipSample sample : skipSamples) {
                samples.add(sample.toJson());
            }
            json.add("skipSamples", samples);
            return json;
        }
    }

    private record SkipSample(
            String carrierSourceKind,
            ResourceLocation carrierEntityType,
            String carrierEntityId,
            ResourceLocation itemId,
            int count,
            String reason) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("carrierSourceKind", carrierSourceKind);
            if (carrierEntityType != null) {
                json.addProperty("carrierEntityType", carrierEntityType.toString());
            }
            if (carrierEntityId != null) {
                json.addProperty("carrierEntityId", carrierEntityId);
            }
            if (itemId != null) {
                json.addProperty("itemId", itemId.toString());
            }
            if (count > 0) {
                json.addProperty("count", count);
            }
            json.addProperty("reason", reason);
            return json;
        }
    }
}
