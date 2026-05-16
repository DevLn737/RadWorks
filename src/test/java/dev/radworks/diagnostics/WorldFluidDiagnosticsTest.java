package dev.radworks.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class WorldFluidDiagnosticsTest {
    @Test
    void storesSkipMatchAndClusterSamplesWithAggregationFields() {
        WorldFluidDiagnostics.Builder builder = WorldFluidDiagnostics.builder();
        builder.worldFluidDiscoveryRadius(10);
        builder.skip(
                ResourceLocation.parse("minecraft:water"),
                null,
                "none",
                new BlockPos(1, 64, 1),
                1000,
                "no_active_fluid_rule");
        builder.match(
                ResourceLocation.parse("createnuclear:flowing_uranium"),
                ResourceLocation.parse("createnuclear:uranium"),
                "fallback",
                new BlockPos(2, 64, 2),
                1000);
        builder.clusterSample(
                1,
                ResourceLocation.parse("createnuclear:uranium"),
                ResourceLocation.parse("createnuclear:uranium"),
                "mixed",
                Set.of(
                        ResourceLocation.parse("createnuclear:uranium"),
                        ResourceLocation.parse("createnuclear:flowing_uranium")),
                8,
                8000,
                new BlockPos(1, 64, 1),
                new BlockPos(3, 65, 3),
                new BlockPos(2, 64, 2),
                new BlockPos(2, 64, 2),
                1.5D,
                3.5D,
                true,
                "cluster_active",
                false);
        WorldFluidDiagnostics.store(builder);

        JsonObject json = WorldFluidDiagnostics.lastToJson().getAsJsonObject();
        JsonArray skips = json.getAsJsonArray("skipSamples");
        JsonArray matches = json.getAsJsonArray("matchSamples");
        JsonArray clusters = json.getAsJsonArray("clusterSamples");

        assertEquals(1, skips.size());
        assertEquals(1, matches.size());
        assertEquals(1, clusters.size());
        assertEquals(10, json.get("worldFluidDiscoveryRadius").getAsInt());
        assertEquals(1, json.get("clusterCount").getAsInt());
        assertEquals(1, json.get("matchedWorldFluidClusters").getAsInt());
        assertEquals(8, json.get("contributingFluidBlocks").getAsInt());
        assertEquals(8000, json.get("aggregateAmountMb").getAsInt());
        assertEquals("no_active_fluid_rule", skips.get(0).getAsJsonObject().get("reason").getAsString());
        assertEquals("fallback", matches.get(0).getAsJsonObject().get("ruleMatchMode").getAsString());
        assertTrue(matches.get(0).getAsJsonObject().has("matchedRuleId"));
        assertEquals("mixed", clusters.get(0).getAsJsonObject().get("ruleMatchMode").getAsString());
    }
}
