package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.diagnostics.SourceOverrideDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.radiation.shielding.ShieldingResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ExposureEnginePipelineContractTest {
    @Test
    void applyForTargetKind_whenExcludeContainForceContainForced_thenFinalExposureUsesPostShieldingValues() {
        RadiationSource excludedNormal = RadiationSource.block(
                ResourceLocation.parse("minecraft:stone"),
                new BlockPos(0, 64, 0),
                8.0D,
                4.0D,
                1.0D,
                true,
                8.0D,
                "normal_block");
        RadiationSource containedNormal = RadiationSource.playerInventoryAggregate(
                        ResourceLocation.parse("createnuclear:raw_uranium"),
                        4,
                        1,
                        1.0D,
                        2.0D,
                        2.0D,
                        4.0D,
                        "normal_nested")
                .withNestedContext(
                        1,
                        ResourceLocation.parse("minecraft:shulker_box"),
                        "player_inventory.slot[1].shulker.slot[0]");
        ForceSourceCandidate forcedCandidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.ITEM,
                RadiationSourceType.PLAYER_INVENTORY,
                null,
                ResourceLocation.parse("minecraft:apple"),
                null,
                null,
                null,
                null,
                ResourceLocation.parse("minecraft:shulker_box"),
                "player_inventory.slot[3].shulker.slot[1]",
                null,
                RadiationTargetKind.PLAYER,
                3,
                0,
                0.0D,
                true,
                true,
                1,
                "data_component_container",
                "player_inventory_observed_without_item_rule");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "exclude", parse("""
                        {
                          "id": "radworks:exclude_stone",
                          "enabled": true,
                          "type": "exclude",
                          "selectors": { "blockId": "minecraft:stone" }
                        }
                        """),
                "contain", parse("""
                        {
                          "id": "radworks:contain_shulker_scale_half",
                          "enabled": true,
                          "type": "contain",
                          "selectors": { "containerItemId": "minecraft:shulker_box" },
                          "mode": "scale",
                          "multiplier": 0.5
                        }
                        """),
                "force", parse("""
                        {
                          "id": "radworks:force_apple",
                          "enabled": true,
                          "type": "force",
                          "selectors": {
                            "itemId": "minecraft:apple",
                            "containerItemId": "minecraft:shulker_box"
                          },
                          "forceStrength": 2.0,
                          "forceRadius": 2.0,
                          "forceUnitMode": "item_count"
                        }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(excludedNormal, containedNormal),
                List.of(forcedCandidate),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertEquals(1, result.excludedSources().size());
        assertEquals(0.0D, result.excludedSources().getFirst().finalContribution(), 1.0e-9);
        assertEquals(2, result.sourcesForShielding().size());

        List<RadiationSource> shielded = new ArrayList<>();
        for (RadiationSource source : result.sourcesForShielding()) {
            shielded.add(source.withShielding(ShieldingResult.reduced(source.finalContribution(), 1)));
        }

        double totalExposure = 0.0D;
        for (RadiationSource source : shielded) {
            totalExposure += source.finalContribution();
        }
        for (RadiationSource source : result.excludedSources()) {
            totalExposure += source.finalContribution();
        }
        for (RadiationSource source : result.containedSuppressedSources()) {
            totalExposure += source.finalContribution();
        }
        assertEquals(2.5D, totalExposure, 1.0e-9);
    }

    @Test
    void applyForTargetKind_whenIdentityWasExcluded_forceMustNotResurrectIt() {
        RadiationSource excludedNormal = RadiationSource.block(
                ResourceLocation.parse("minecraft:stone"),
                new BlockPos(0, 64, 0),
                2.0D,
                4.0D,
                1.0D,
                true,
                2.0D,
                "normal_block");
        ForceSourceCandidate sameIdentityCandidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.BLOCK,
                RadiationSourceType.BLOCK,
                ResourceLocation.parse("minecraft:stone"),
                null,
                null,
                new BlockPos(0, 64, 0),
                null,
                null,
                null,
                null,
                ResourceLocation.parse("minecraft:stone"),
                RadiationTargetKind.PLAYER,
                0,
                0,
                1.0D,
                true,
                false,
                0,
                null,
                "block_observed_without_block_rule");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "exclude", parse("""
                        {
                          "id": "radworks:exclude_stone",
                          "enabled": true,
                          "type": "exclude",
                          "selectors": { "blockId": "minecraft:stone" }
                        }
                        """),
                "force", parse("""
                        {
                          "id": "radworks:force_stone",
                          "enabled": true,
                          "type": "force",
                          "selectors": { "blockId": "minecraft:stone" },
                          "forceStrength": 4.0,
                          "forceRadius": 4.0,
                          "forceUnitMode": "block"
                        }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(excludedNormal),
                List.of(sameIdentityCandidate),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertTrue(result.sourcesForShielding().isEmpty());
        assertEquals(1, result.excludedSources().size());
        assertEquals(0.0D, result.excludedSources().getFirst().finalContribution(), 1.0e-9);
    }

    private static com.google.gson.JsonObject parse(String json) {
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }
}
