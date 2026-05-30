package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.diagnostics.SourceOverrideDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SourceOverrideForceContractTest {
    @Test
    void applyForTargetKind_whenForceMatchesObservedCandidate_shouldCreateForcedSource() {
        ForceSourceCandidate candidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.ITEM,
                RadiationSourceType.PLAYER_INVENTORY,
                null,
                ResourceLocation.parse("minecraft:apple"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                RadiationTargetKind.PLAYER,
                3,
                0,
                0.0D,
                true,
                false,
                0,
                null,
                "player_inventory_observed_without_item_rule");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "force", parse("""
                        {
                          "id": "radworks:force_apple",
                          "enabled": true,
                          "type": "force",
                          "selectors": { "itemId": "minecraft:apple" },
                          "forceStrength": 2.0,
                          "forceRadius": 3.0,
                          "forceUnitMode": "item_count"
                        }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(),
                List.of(candidate),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertEquals(1, result.sourcesForShielding().size());
        RadiationSource forced = result.sourcesForShielding().getFirst();
        assertEquals("forced", forced.overrideMode());
        assertEquals(6.0D, forced.finalContribution(), 1.0e-9);
    }

    @Test
    void applyForTargetKind_whenIdentityAlreadyExists_shouldSkipForcedDuplicate() {
        RadiationSource normal = RadiationSource.playerInventoryAggregate(
                ResourceLocation.parse("minecraft:apple"),
                2,
                1,
                1.0D,
                2.0D,
                2.0D,
                2.0D,
                "normal");
        ForceSourceCandidate candidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.ITEM,
                RadiationSourceType.PLAYER_INVENTORY,
                null,
                ResourceLocation.parse("minecraft:apple"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                RadiationTargetKind.PLAYER,
                2,
                0,
                0.0D,
                true,
                false,
                0,
                null,
                "candidate_duplicate");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "force", parse("""
                        {
                          "id": "radworks:force_apple",
                          "enabled": true,
                          "type": "force",
                          "selectors": { "itemId": "minecraft:apple" },
                          "forceStrength": 2.0,
                          "forceRadius": 3.0,
                          "forceUnitMode": "item_count"
                        }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(normal),
                List.of(candidate),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertEquals(1, result.sourcesForShielding().size());
        assertEquals("none", result.sourcesForShielding().getFirst().overrideMode());
        assertTrue(result.excludedSources().isEmpty());
    }

    private static com.google.gson.JsonObject parse(String json) {
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }
}
