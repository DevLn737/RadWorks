package dev.radworks.diagnostics;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        private int worldFluidDiscoveryRadius;
        private int clusterCount;
        private int matchedWorldFluidClusters;
        private int contributingFluidBlocks;
        private int aggregateAmountMb;
        private final List<SkipSample> skipSamples = new ArrayList<>();
        private final List<MatchSample> matchSamples = new ArrayList<>();
        private final List<ClusterSample> clusterSamples = new ArrayList<>();

        public void worldFluidDiscoveryRadius(int radius) {
            this.worldFluidDiscoveryRadius = Math.max(0, radius);
        }

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

        public void clusterSample(
                int clusterIndex,
                ResourceLocation normalizedFluidId,
                ResourceLocation matchedRuleId,
                String ruleMatchMode,
                Set<ResourceLocation> observedFluidIds,
                int contributingFluidBlocksValue,
                int aggregateAmountMbValue,
                BlockPos clusterMinPos,
                BlockPos clusterMaxPos,
                BlockPos representativePosition,
                BlockPos nearestFluidPosition,
                double distanceToFluidCluster,
                double effectiveRadius,
                boolean active,
                String reason,
                boolean maybeClippedByDiscoveryRadius) {
            clusterCount++;
            contributingFluidBlocks += contributingFluidBlocksValue;
            aggregateAmountMb += aggregateAmountMbValue;
            if (active) {
                matchedWorldFluidClusters++;
            }
            if (clusterSamples.size() >= MAX_SAMPLES) {
                return;
            }
            List<String> observed = observedFluidIds.stream().map(ResourceLocation::toString).sorted().toList();
            clusterSamples.add(new ClusterSample(
                    clusterIndex,
                    normalizedFluidId == null ? null : normalizedFluidId.toString(),
                    matchedRuleId == null ? null : matchedRuleId.toString(),
                    ruleMatchMode,
                    observed,
                    contributingFluidBlocksValue,
                    aggregateAmountMbValue,
                    clusterMinPos.immutable(),
                    clusterMaxPos.immutable(),
                    representativePosition.immutable(),
                    nearestFluidPosition.immutable(),
                    distanceToFluidCluster,
                    effectiveRadius,
                    active,
                    reason,
                    maybeClippedByDiscoveryRadius));
        }

        private Snapshot build() {
            return new Snapshot(
                    Instant.now(),
                    worldFluidDiscoveryRadius,
                    clusterCount,
                    matchedWorldFluidClusters,
                    contributingFluidBlocks,
                    aggregateAmountMb,
                    List.copyOf(skipSamples),
                    List.copyOf(matchSamples),
                    List.copyOf(clusterSamples));
        }
    }

    private record Snapshot(
            Instant createdAt,
            int worldFluidDiscoveryRadius,
            int clusterCount,
            int matchedWorldFluidClusters,
            int contributingFluidBlocks,
            int aggregateAmountMb,
            List<SkipSample> skipSamples,
            List<MatchSample> matchSamples,
            List<ClusterSample> clusterSamples) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("createdAt", createdAt.toString());
            json.addProperty("worldFluidDiscoveryRadius", worldFluidDiscoveryRadius);
            json.addProperty("clusterCount", clusterCount);
            json.addProperty("matchedWorldFluidClusters", matchedWorldFluidClusters);
            json.addProperty("contributingFluidBlocks", contributingFluidBlocks);
            json.addProperty("aggregateAmountMb", aggregateAmountMb);

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

            JsonArray clusters = new JsonArray();
            for (ClusterSample sample : clusterSamples) {
                clusters.add(sample.toJson());
            }
            json.add("clusterSamples", clusters);

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

    private record ClusterSample(
            int clusterIndex,
            String normalizedFluidId,
            String matchedRuleId,
            String ruleMatchMode,
            List<String> observedFluidIds,
            int contributingFluidBlocks,
            int aggregateAmountMb,
            BlockPos clusterMinPos,
            BlockPos clusterMaxPos,
            BlockPos representativePosition,
            BlockPos nearestFluidPosition,
            double distanceToFluidCluster,
            double effectiveRadius,
            boolean active,
            String reason,
            boolean maybeClippedByDiscoveryRadius) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("clusterIndex", clusterIndex);
            if (normalizedFluidId != null) {
                json.addProperty("normalizedFluidId", normalizedFluidId);
            }
            if (matchedRuleId != null) {
                json.addProperty("matchedRuleId", matchedRuleId);
            }
            json.addProperty("ruleMatchMode", ruleMatchMode);
            JsonArray observed = new JsonArray();
            for (String fluidId : observedFluidIds) {
                observed.add(fluidId);
            }
            json.add("observedFluidIds", observed);
            json.addProperty("contributingFluidBlocks", contributingFluidBlocks);
            json.addProperty("aggregateAmountMb", aggregateAmountMb);
            JsonObject bbox = new JsonObject();
            bbox.add("min", positionToJson(clusterMinPos));
            bbox.add("max", positionToJson(clusterMaxPos));
            json.add("clusterBoundingBox", bbox);
            json.add("representativePosition", positionToJson(representativePosition));
            json.add("nearestFluidPosition", positionToJson(nearestFluidPosition));
            json.addProperty("distanceToFluidCluster", distanceToFluidCluster);
            json.addProperty("effectiveRadius", effectiveRadius);
            json.addProperty("active", active);
            json.addProperty("reason", reason);
            json.addProperty("maybeClippedByDiscoveryRadius", maybeClippedByDiscoveryRadius);
            return json;
        }
    }

    private static JsonObject positionToJson(BlockPos position) {
        JsonObject pos = new JsonObject();
        pos.addProperty("x", position.getX());
        pos.addProperty("y", position.getY());
        pos.addProperty("z", position.getZ());
        return pos;
    }
}
