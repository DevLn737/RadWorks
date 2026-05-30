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

class SourceOverridePipelineOrderContractTest {
    @Test
    void applyForTargetKind_pipelineShouldApplyExcludeThenContainThenForceThenContainForced() {
        RadiationSource excludedFirst = RadiationSource.block(
                ResourceLocation.parse("minecraft:stone"),
                BlockPos.ZERO,
                1.0D,
                2.0D,
                1.0D,
                true,
                1.0D,
                "block");
        RadiationSource containedSecond = RadiationSource.playerInventoryAggregate(
                        ResourceLocation.parse("createnuclear:raw_uranium"),
                        2,
                        1,
                        1.0D,
                        2.0D,
                        2.0D,
                        2.0D,
                        "nested")
                .withNestedContext(1, ResourceLocation.parse("minecraft:shulker_box"), "player.slot[2].shulker.slot[0]");
        ForceSourceCandidate forceCandidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.ITEM,
                RadiationSourceType.PLAYER_INVENTORY,
                null,
                ResourceLocation.parse("minecraft:apple"),
                null,
                null,
                null,
                null,
                ResourceLocation.parse("minecraft:shulker_box"),
                "player.slot[4].shulker.slot[1]",
                null,
                RadiationTargetKind.PLAYER,
                2,
                0,
                0.0D,
                true,
                true,
                1,
                "data_component_container",
                "observed_without_item_rule");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "exclude", parse("""
                        { "id":"radworks:exclude_stone", "enabled":true, "type":"exclude",
                          "selectors":{"blockId":"minecraft:stone"} }
                        """),
                "contain", parse("""
                        { "id":"radworks:contain_shulker_half", "enabled":true, "type":"contain",
                          "selectors":{"containerItemId":"minecraft:shulker_box"}, "mode":"scale", "multiplier":0.5 }
                        """),
                "force", parse("""
                        { "id":"radworks:force_apple", "enabled":true, "type":"force",
                          "selectors":{"itemId":"minecraft:apple","containerItemId":"minecraft:shulker_box"},
                          "forceStrength":2.0, "forceRadius":2.0, "forceUnitMode":"item_count" }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(excludedFirst, containedSecond),
                List.of(forceCandidate),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertEquals(1, result.excludedSources().size());
        assertEquals(0.0D, result.excludedSources().getFirst().finalContribution(), 1.0e-9);
        assertEquals(2, result.sourcesForShielding().size());
        assertTrue(result.sourcesForShielding().stream().allMatch(source -> "contained".equals(source.overrideMode())));
        assertEquals(1.0D, result.sourcesForShielding().get(0).finalContribution(), 1.0e-9);
        assertEquals(2.0D, result.sourcesForShielding().get(1).finalContribution(), 1.0e-9);
    }

    private static com.google.gson.JsonObject parse(String json) {
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }
}
