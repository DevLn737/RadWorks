package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.diagnostics.SourceOverrideDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SourceOverrideExcludeContractTest {
    @Test
    void applyForTargetKind_whenExcludeMatches_shouldSuppressContributionAndMoveRowToExcluded() {
        RadiationSource source = RadiationSource.playerInventoryAggregate(
                ResourceLocation.parse("minecraft:rotten_flesh"),
                2,
                1,
                1.0D,
                2.0D,
                2.0D,
                2.0D,
                "test");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "exclude", parse("""
                        {
                          "id": "radworks:exclude_rotten_flesh",
                          "enabled": true,
                          "type": "exclude",
                          "selectors": { "itemId": "minecraft:rotten_flesh" }
                        }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertTrue(result.sourcesForShielding().isEmpty());
        assertEquals(1, result.excludedSources().size());
        assertEquals(0.0D, result.excludedSources().getFirst().finalContribution(), 1.0e-9);
        assertEquals("excluded", result.excludedSources().getFirst().overrideMode());
    }

    private static com.google.gson.JsonObject parse(String json) {
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }
}
