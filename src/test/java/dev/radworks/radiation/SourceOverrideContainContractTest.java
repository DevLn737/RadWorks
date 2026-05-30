package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.diagnostics.SourceOverrideDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SourceOverrideContainContractTest {
    @Test
    void applyForTargetKind_whenContainScaleMatches_shouldScaleContribution() {
        RadiationSource nestedSource = RadiationSource.playerInventoryAggregate(
                        ResourceLocation.parse("createnuclear:raw_uranium"),
                        4,
                        1,
                        1.0D,
                        2.0D,
                        2.0D,
                        4.0D,
                        "nested")
                .withNestedContext(1, ResourceLocation.parse("minecraft:shulker_box"), "player.slot[2].shulker.slot[0]");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "contain", parse("""
                        {
                          "id": "radworks:contain_shulker_half",
                          "enabled": true,
                          "type": "contain",
                          "selectors": { "containerItemId": "minecraft:shulker_box" },
                          "mode": "scale",
                          "multiplier": 0.5
                        }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(nestedSource),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertEquals(1, result.sourcesForShielding().size());
        assertTrue(result.containedSuppressedSources().isEmpty());
        RadiationSource contained = result.sourcesForShielding().getFirst();
        assertEquals("contained", contained.overrideMode());
        assertEquals(2.0D, contained.finalContribution(), 1.0e-9);
        assertEquals(2.0D, contained.suppressedContribution(), 1.0e-9);
    }

    private static com.google.gson.JsonObject parse(String json) {
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }
}
