package dev.radworks.diagnostics;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class HandlerDiagnostics {
    private static volatile Snapshot lastSnapshot;

    private HandlerDiagnostics() {
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
        private static final int MAX_HANDLERS = 20;
        private final List<HandlerSample> itemHandlerNonMatchingSamples = new ArrayList<>();
        private final List<HandlerSample> fluidHandlerNonMatchingSamples = new ArrayList<>();

        public void addItemHandlerSample(
                ResourceLocation blockId,
                BlockPos position,
                String capabilityContext,
                int slotsChecked,
                int matches,
                List<ContentSample> contents) {
            addSample(itemHandlerNonMatchingSamples, blockId, position, capabilityContext, slotsChecked, matches, contents);
        }

        public void addFluidHandlerSample(
                ResourceLocation blockId,
                BlockPos position,
                String capabilityContext,
                int tanksChecked,
                int matches,
                List<ContentSample> contents) {
            addSample(fluidHandlerNonMatchingSamples, blockId, position, capabilityContext, tanksChecked, matches, contents);
        }

        private static void addSample(
                List<HandlerSample> target,
                ResourceLocation blockId,
                BlockPos position,
                String capabilityContext,
                int checked,
                int matches,
                List<ContentSample> contents) {
            if (target.size() >= MAX_HANDLERS) {
                return;
            }
            target.add(new HandlerSample(
                    blockId,
                    position.immutable(),
                    capabilityContext,
                    checked,
                    matches,
                    List.copyOf(contents)));
        }

        private Snapshot build() {
            return new Snapshot(
                    Instant.now(),
                    List.copyOf(itemHandlerNonMatchingSamples),
                    List.copyOf(fluidHandlerNonMatchingSamples));
        }
    }

    public record ContentSample(
            String slot,
            String tank,
            String itemId,
            String fluidId,
            int count,
            int amountMb,
            String reason,
            Double distance,
            Double baseRadius,
            Double effectiveRadius,
            Double aggregateUnitsSnapshot) {
        public static ContentSample item(
                String slot,
                ResourceLocation itemId,
                int count,
                String reason,
                Double distance,
                Double baseRadius,
                Double effectiveRadius,
                Double aggregateUnitsSnapshot) {
            return new ContentSample(
                    slot,
                    null,
                    itemId == null ? null : itemId.toString(),
                    null,
                    count,
                    0,
                    reason,
                    distance,
                    baseRadius,
                    effectiveRadius,
                    aggregateUnitsSnapshot);
        }

        public static ContentSample fluid(
                String tank,
                ResourceLocation fluidId,
                int amountMb,
                String reason,
                Double distance,
                Double baseRadius,
                Double effectiveRadius,
                Double aggregateUnitsSnapshot) {
            return new ContentSample(
                    null,
                    tank,
                    null,
                    fluidId == null ? null : fluidId.toString(),
                    0,
                    amountMb,
                    reason,
                    distance,
                    baseRadius,
                    effectiveRadius,
                    aggregateUnitsSnapshot);
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (slot != null) {
                json.addProperty("slot", slot);
            }
            if (tank != null) {
                json.addProperty("tank", tank);
            }
            if (itemId != null) {
                json.addProperty("itemId", itemId);
            }
            if (fluidId != null) {
                json.addProperty("fluidId", fluidId);
            }
            if (count > 0) {
                json.addProperty("count", count);
            }
            if (amountMb > 0) {
                json.addProperty("amountMb", amountMb);
            }
            json.addProperty("reason", reason);
            if (distance != null) {
                json.addProperty("distance", distance);
            }
            if (baseRadius != null) {
                json.addProperty("baseRadius", baseRadius);
            }
            if (effectiveRadius != null) {
                json.addProperty("effectiveRadius", effectiveRadius);
            }
            if (aggregateUnitsSnapshot != null) {
                json.addProperty("aggregateUnitsSnapshot", aggregateUnitsSnapshot);
            }
            return json;
        }
    }

    private record HandlerSample(
            ResourceLocation blockId,
            BlockPos position,
            String capabilityContext,
            int checked,
            int matches,
            List<ContentSample> contents) {
        private JsonObject toJson(String checkedFieldName) {
            JsonObject json = new JsonObject();
            json.addProperty("blockId", blockId.toString());
            JsonObject pos = new JsonObject();
            pos.addProperty("x", position.getX());
            pos.addProperty("y", position.getY());
            pos.addProperty("z", position.getZ());
            json.add("position", pos);
            json.addProperty("capabilityContext", capabilityContext);
            json.addProperty(checkedFieldName, checked);
            json.addProperty("matches", matches);
            JsonArray contentsArray = new JsonArray();
            for (ContentSample content : contents) {
                contentsArray.add(content.toJson());
            }
            json.add("contents", contentsArray);
            return json;
        }
    }

    private record Snapshot(
            Instant createdAt,
            List<HandlerSample> itemHandlerNonMatchingSamples,
            List<HandlerSample> fluidHandlerNonMatchingSamples) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("createdAt", createdAt.toString());
            JsonArray itemSamples = new JsonArray();
            for (HandlerSample sample : itemHandlerNonMatchingSamples) {
                itemSamples.add(sample.toJson("slotsChecked"));
            }
            JsonArray fluidSamples = new JsonArray();
            for (HandlerSample sample : fluidHandlerNonMatchingSamples) {
                fluidSamples.add(sample.toJson("tanksChecked"));
            }
            json.add("itemHandlerNonMatchingSamples", itemSamples);
            json.add("fluidHandlerNonMatchingSamples", fluidSamples);
            return json;
        }
    }
}
