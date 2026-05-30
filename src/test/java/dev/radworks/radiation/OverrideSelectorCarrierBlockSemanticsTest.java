package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.diagnostics.SourceOverrideDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class OverrideSelectorCarrierBlockSemanticsTest {
    @Test
    void applyForTargetKind_whenCarrierBlockIdSelectorMatchesSourceBlockId_shouldApplyExclude() {
        RadiationSource blockEntitySource = RadiationSource.blockEntityInventoryAggregate(
                ResourceLocation.parse("minecraft:chest"),
                new BlockPos(5, 64, 5),
                ResourceLocation.parse("createnuclear:raw_uranium"),
                3,
                1,
                1.0D,
                2.0D,
                2.0D,
                1.0D,
                true,
                3.0D,
                "container");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "excludeByCarrierBlock", parse("""
                        {
                          "id": "radworks:exclude_chest_by_carrier_block",
                          "enabled": true,
                          "type": "exclude",
                          "selectors": { "carrierBlockId": "minecraft:chest" }
                        }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(blockEntitySource),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertTrue(result.sourcesForShielding().isEmpty());
        assertEquals(1, result.excludedSources().size());
    }

    @Test
    void applyForTargetKind_whenCarrierBlockIdSelectorDoesNotMatch_shouldKeepSource() {
        RadiationSource blockEntitySource = RadiationSource.blockEntityInventoryAggregate(
                ResourceLocation.parse("minecraft:barrel"),
                new BlockPos(5, 64, 5),
                ResourceLocation.parse("createnuclear:raw_uranium"),
                3,
                1,
                1.0D,
                2.0D,
                2.0D,
                1.0D,
                true,
                3.0D,
                "container");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "excludeByCarrierBlock", parse("""
                        {
                          "id": "radworks:exclude_chest_by_carrier_block",
                          "enabled": true,
                          "type": "exclude",
                          "selectors": { "carrierBlockId": "minecraft:chest" }
                        }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(blockEntitySource),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertEquals(1, result.sourcesForShielding().size());
        assertTrue(result.excludedSources().isEmpty());
    }

    @Test
    void applyForTargetKind_whenForceUsesCarrierBlockIdCandidate_shouldCreateForcedSource() {
        ForceSourceCandidate candidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.ITEM,
                RadiationSourceType.BLOCK_ENTITY_INVENTORY,
                null,
                ResourceLocation.parse("minecraft:apple"),
                null,
                new BlockPos(5, 64, 5),
                null,
                null,
                null,
                null,
                ResourceLocation.parse("minecraft:chest"),
                RadiationTargetKind.PLAYER,
                2,
                0,
                1.0D,
                true,
                false,
                0,
                null,
                "candidate_without_rule");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "forceByCarrierBlock", parse("""
                        {
                          "id": "radworks:force_from_chest_context",
                          "enabled": true,
                          "type": "force",
                          "selectors": {
                            "carrierBlockId": "minecraft:chest",
                            "itemId": "minecraft:apple"
                          },
                          "forceStrength": 1.0,
                          "forceRadius": 2.0,
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

        // SPEC_CODE_MISMATCH_CANDIDATE: carrierBlockId selector semantics must stay explicit and tested.
        assertEquals(1, result.sourcesForShielding().size());
        assertEquals("forced", result.sourcesForShielding().getFirst().overrideMode());
    }

    private static com.google.gson.JsonObject parse(String json) {
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }
}
