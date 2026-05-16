package dev.radworks.diagnostics;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class WorldFluidDiagnostics {
    private static final int MAX_SAMPLES = 20;
    private static volatile Snapshot lastSnapshot;

    private WorldFluidDiagnostics() {
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
        private final List<MatchSample> matchSamples = new ArrayList<>();

        public void skip(
                ResourceLocation fluidId,
                ResourceLocation matchedRuleId,
                String ruleMatchMode,
                BlockPos position,
                int amountMb,
                String reason) {
            if (skipSamples.size() >= MAX_SAMPLES) {
                return;
            }
            skipSamples.add(new SkipSample(
                    fluidId,
                    matchedRuleId,
                    ruleMatchMode,
                    position.immutable(),
                    amountMb,
                    reason));
        }

        public void match(
                ResourceLocation fluidId,
                ResourceLocation matchedRuleId,
                String ruleMatchMode,
                BlockPos position,
                int amountMb) {
            if (matchSamples.size() >= MAX_SAMPLES) {
                return;
            }
            matchSamples.add(new MatchSample(
                    fluidId,
                    matchedRuleId,
                    ruleMatchMode,
                    position.immutable(),
                    amountMb));
        }

        private Snapshot build() {
            return new Snapshot(
                    Instant.now(),
                    List.copyOf(skipSamples),
                    List.copyOf(matchSamples));
        }
    }

    private record Snapshot(
            Instant createdAt,
            List<SkipSample> skipSamples,
            List<MatchSample> matchSamples) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("createdAt", createdAt.toString());
            JsonArray skips = new JsonArray();
            for (SkipSample sample : skipSamples) {
                skips.add(sample.toJson());
            }
            json.add("skipSamples", skips);
            JsonArray matches = new JsonArray();
            for (MatchSample sample : matchSamples) {
                matches.add(sample.toJson());
            }
            json.add("matchSamples", matches);
            return json;
        }
    }

    private record SkipSample(
            ResourceLocation fluidId,
            ResourceLocation matchedRuleId,
            String ruleMatchMode,
            BlockPos position,
            int amountMb,
            String reason) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (fluidId != null) {
                json.addProperty("fluidId", fluidId.toString());
            }
            if (matchedRuleId != null) {
                json.addProperty("matchedRuleId", matchedRuleId.toString());
            }
            json.addProperty("ruleMatchMode", ruleMatchMode);
            JsonObject pos = new JsonObject();
            pos.addProperty("x", position.getX());
            pos.addProperty("y", position.getY());
            pos.addProperty("z", position.getZ());
            json.add("position", pos);
            json.addProperty("amountMb", amountMb);
            json.addProperty("reason", reason);
            return json;
        }
    }

    private record MatchSample(
            ResourceLocation fluidId,
            ResourceLocation matchedRuleId,
            String ruleMatchMode,
            BlockPos position,
            int amountMb) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("fluidId", fluidId.toString());
            json.addProperty("matchedRuleId", matchedRuleId.toString());
            json.addProperty("ruleMatchMode", ruleMatchMode);
            JsonObject pos = new JsonObject();
            pos.addProperty("x", position.getX());
            pos.addProperty("y", position.getY());
            pos.addProperty("z", position.getZ());
            json.add("position", pos);
            json.addProperty("amountMb", amountMb);
            return json;
        }
    }
}
