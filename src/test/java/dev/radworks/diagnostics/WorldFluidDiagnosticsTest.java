package dev.radworks.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class WorldFluidDiagnosticsTest {
    @Test
    void storesSkipAndMatchSamplesWithRuleMatchMode() {
        WorldFluidDiagnostics.Builder builder = WorldFluidDiagnostics.builder();
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
        WorldFluidDiagnostics.store(builder);

        JsonObject json = WorldFluidDiagnostics.lastToJson().getAsJsonObject();
        JsonArray skips = json.getAsJsonArray("skipSamples");
        JsonArray matches = json.getAsJsonArray("matchSamples");

        assertEquals(1, skips.size());
        assertEquals(1, matches.size());
        assertEquals("no_active_fluid_rule", skips.get(0).getAsJsonObject().get("reason").getAsString());
        assertEquals("fallback", matches.get(0).getAsJsonObject().get("ruleMatchMode").getAsString());
        assertTrue(matches.get(0).getAsJsonObject().has("matchedRuleId"));
    }
}
