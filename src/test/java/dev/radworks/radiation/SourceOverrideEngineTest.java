package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.SourceOverrideDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.radiation.shielding.ShieldingResult;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

class SourceOverrideEngineTest {
    @Test
    void excludeBySourceTypeSuppressesMatchingSource() {
        RadiationSource source = RadiationSource.playerInventoryAggregate(
                ResourceLocation.parse("minecraft:rotten_flesh"),
                4,
                1,
                1.0D,
                2.0D,
                2.0D,
                4.0D,
                "test");
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:exclude_player_inventory",
                  "enabled": true,
                  "type": "exclude",
                  "selectors": { "sourceType": "player_inventory" }
                }
                """);
        SourceScanSummary.Builder summary = SourceScanSummary.builder();
        SourceOverrideDiagnostics.Builder diagnostics = SourceOverrideDiagnostics.builder();
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source),
                rules,
                summary,
                diagnostics);

        assertEquals(0, result.sourcesForShielding().size());
        assertEquals(1, result.excludedSources().size());
        RadiationSource excluded = result.excludedSources().getFirst();
        assertEquals("excluded", excluded.overrideMode());
        assertEquals("radworks:exclude_player_inventory", excluded.overrideRuleId());
        assertEquals(0.0D, excluded.finalContribution(), 1.0e-9);
        assertEquals(4.0D, excluded.originalContribution(), 1.0e-9);
        assertEquals(4.0D, excluded.suppressedContribution(), 1.0e-9);
    }

    @Test
    void selectorMatchingSupportsItemBlockFluidContainerAndCarrierEntityType() {
        RadiationSource itemSource = RadiationSource.playerInventoryAggregate(
                ResourceLocation.parse("createnuclear:raw_uranium"),
                1,
                1,
                1.0D,
                2.0D,
                2.0D,
                1.0D,
                "item");
        RadiationSource blockSource = RadiationSource.block(
                ResourceLocation.parse("minecraft:gold_block"),
                BlockPos.ZERO,
                1.0D,
                2.0D,
                1.0D,
                true,
                1.0D,
                "block");
        RadiationSource fluidSource = RadiationSource.worldFluid(
                ResourceLocation.parse("createnuclear:uranium"),
                ResourceLocation.parse("createnuclear:uranium"),
                BlockPos.ZERO,
                1000,
                1,
                1.0D,
                2.0D,
                1.0D,
                true,
                "exact",
                "fluid");
        RadiationSource nestedSource = RadiationSource.playerInventoryAggregate(
                        ResourceLocation.parse("minecraft:rotten_flesh"),
                        2,
                        1,
                        1.0D,
                        2.0D,
                        2.0D,
                        2.0D,
                        "nested")
                .withNestedContext(1, ResourceLocation.parse("minecraft:shulker_box"), "player.slot[1].shulker.slot[0]");
        RadiationSource carrierSource = RadiationSource.entityInventoryCarrierItem(
                "entity_inventory",
                "vanilla_inventory",
                RadiationSourceType.ENTITY_INVENTORY,
                "minecraft:donkey",
                "uuid-1",
                BlockPos.ZERO,
                ResourceLocation.parse("createnuclear:raw_uranium"),
                5,
                1,
                1.0D,
                2.0D,
                2.0D,
                1.0D,
                true,
                5.0D,
                "carrier");

        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "item", parse("""
                        { "id":"radworks:exclude_item", "enabled":true, "type":"exclude",
                          "selectors":{"itemId":"createnuclear:raw_uranium"} }
                        """),
                "block", parse("""
                        { "id":"radworks:exclude_block", "enabled":true, "type":"exclude",
                          "selectors":{"blockId":"minecraft:gold_block"} }
                        """),
                "fluid", parse("""
                        { "id":"radworks:exclude_fluid", "enabled":true, "type":"exclude",
                          "selectors":{"fluidId":"createnuclear:uranium"} }
                        """),
                "container", parse("""
                        { "id":"radworks:exclude_container", "enabled":true, "type":"exclude",
                          "selectors":{"containerItemId":"minecraft:shulker_box"} }
                        """),
                "carrier", parse("""
                        { "id":"radworks:exclude_carrier", "enabled":true, "type":"exclude",
                          "selectors":{"carrierEntityType":"minecraft:donkey"} }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(itemSource, blockSource, fluidSource, nestedSource, carrierSource),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertEquals(0, result.sourcesForShielding().size());
        assertEquals(5, result.excludedSources().size());
    }

    @Test
    void nonMatchingAndDisabledRulesDoNotApply() {
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
                "disabled", parse("""
                        { "id":"radworks:disabled", "enabled":false, "type":"exclude",
                          "selectors":{"itemId":"minecraft:rotten_flesh"} }
                        """),
                "nonmatch", parse("""
                        { "id":"radworks:nonmatch", "enabled":true, "type":"exclude",
                          "selectors":{"itemId":"minecraft:apple"} }
                        """)));

        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());

        assertEquals(1, result.sourcesForShielding().size());
        assertEquals(0, result.excludedSources().size());
        assertEquals(2.0D, result.sourcesForShielding().getFirst().finalContribution(), 1.0e-9);
    }

    @Test
    void sourceOverrideConfigDisablePathKeepsSources() throws Exception {
        RadiationSource source = RadiationSource.playerInventoryAggregate(
                ResourceLocation.parse("minecraft:rotten_flesh"),
                2,
                1,
                1.0D,
                2.0D,
                2.0D,
                2.0D,
                "test");
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:exclude_player_inventory",
                  "enabled": true,
                  "type": "exclude",
                  "selectors": { "sourceType": "player_inventory" }
                }
                """);

        boolean previousOverrides = RadWorksConfig.sourceOverridesEnabled();
        boolean previousExclusions = RadWorksConfig.sourceExclusionsEnabled();
        try {
            setBoolean("SOURCE_OVERRIDES_ENABLED", false);
            setBoolean("SOURCE_EXCLUSIONS_ENABLED", true);
            SourceOverrideEngine.ApplicationResult overridesDisabled = SourceOverrideEngine.applyForTargetKind(
                    RadiationTargetKind.PLAYER,
                    List.of(source),
                    rules,
                    SourceScanSummary.builder(),
                    SourceOverrideDiagnostics.builder());
            assertEquals(1, overridesDisabled.sourcesForShielding().size());
            assertEquals(0, overridesDisabled.excludedSources().size());

            setBoolean("SOURCE_OVERRIDES_ENABLED", true);
            setBoolean("SOURCE_EXCLUSIONS_ENABLED", false);
            SourceOverrideEngine.ApplicationResult exclusionsDisabled = SourceOverrideEngine.applyForTargetKind(
                    RadiationTargetKind.PLAYER,
                    List.of(source),
                    rules,
                    SourceScanSummary.builder(),
                    SourceOverrideDiagnostics.builder());
            assertEquals(1, exclusionsDisabled.sourcesForShielding().size());
            assertEquals(0, exclusionsDisabled.excludedSources().size());
        } finally {
            setBoolean("SOURCE_OVERRIDES_ENABLED", previousOverrides);
            setBoolean("SOURCE_EXCLUSIONS_ENABLED", previousExclusions);
        }
    }

    @Test
    void excludedSourcesAreSeparatedFromShieldingInput() {
        RadiationSource source = RadiationSource.block(
                ResourceLocation.parse("minecraft:gold_block"),
                BlockPos.ZERO,
                1.0D,
                2.0D,
                1.0D,
                true,
                1.0D,
                "block");
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:exclude_block",
                  "enabled": true,
                  "type": "exclude",
                  "selectors": { "blockId": "minecraft:gold_block" }
                }
                """);
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertTrue(result.sourcesForShielding().isEmpty());
        assertEquals(1, result.excludedSources().size());
    }

    @Test
    void targetKindCanScopeExclusionAndLivingTargetsUsePostExclusion() {
        RadiationSource source = RadiationSource.entityInventoryCarrierItem(
                "entity_inventory",
                "vanilla_inventory",
                RadiationSourceType.ENTITY_INVENTORY,
                "minecraft:donkey",
                "uuid-2",
                BlockPos.ZERO,
                ResourceLocation.parse("createnuclear:raw_uranium"),
                10,
                1,
                1.0D,
                2.0D,
                2.0D,
                1.0D,
                true,
                10.0D,
                "carrier");
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:exclude_mob_only",
                  "enabled": true,
                  "type": "exclude",
                  "selectors": { "sourceType": "entity_inventory", "targetKind": "mob" }
                }
                """);

        SourceOverrideEngine.ApplicationResult mobResult = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.MOB,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(0, mobResult.sourcesForShielding().size());
        assertEquals(1, mobResult.excludedSources().size());
        assertEquals(0.0D, mobResult.excludedSources().getFirst().contribution(), 1.0e-9);

        SourceOverrideEngine.ApplicationResult playerResult = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(1, playerResult.sourcesForShielding().size());
        assertTrue(playerResult.excludedSources().isEmpty());
    }

    @Test
    void targetKindCanScopeContainmentForMobTargetsOnly() {
        RadiationSource source = RadiationSource.entityInventoryCarrierItem(
                "entity_inventory",
                "vanilla_inventory",
                RadiationSourceType.ENTITY_INVENTORY,
                "minecraft:donkey",
                "uuid-2",
                BlockPos.ZERO,
                ResourceLocation.parse("createnuclear:raw_uranium"),
                10,
                1,
                1.0D,
                2.0D,
                2.0D,
                1.0D,
                true,
                10.0D,
                "carrier");
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:contain_mob_only",
                  "enabled": true,
                  "type": "contain",
                  "selectors": { "sourceType": "entity_inventory", "targetKind": "mob" },
                  "mode": "scale",
                  "multiplier": 0.25
                }
                """);

        SourceOverrideEngine.ApplicationResult mobResult = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.MOB,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(1, mobResult.sourcesForShielding().size());
        assertEquals(2.5D, mobResult.sourcesForShielding().getFirst().finalContribution(), 1.0e-9);

        SourceOverrideEngine.ApplicationResult playerResult = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(1, playerResult.sourcesForShielding().size());
        assertEquals(10.0D, playerResult.sourcesForShielding().getFirst().finalContribution(), 1.0e-9);
    }

    @Test
    void containSuppressByContainerItemIdSuppressesNestedSource() {
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
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:contain_shulker_suppress",
                  "enabled": true,
                  "type": "contain",
                  "selectors": { "containerItemId": "minecraft:shulker_box" },
                  "mode": "suppress"
                }
                """);
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(nestedSource),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertTrue(result.sourcesForShielding().isEmpty());
        assertEquals(1, result.containedSuppressedSources().size());
        RadiationSource suppressed = result.containedSuppressedSources().getFirst();
        assertEquals("contained", suppressed.overrideMode());
        assertEquals("radworks:contain_shulker_suppress", suppressed.containmentRuleId());
        assertEquals(0.0D, suppressed.finalContribution(), 1.0e-9);
        assertEquals(4.0D, suppressed.suppressedContribution(), 1.0e-9);
    }

    @Test
    void containScaleByCarrierEntityTypeScalesEntityInventorySource() {
        RadiationSource source = RadiationSource.entityInventoryCarrierItem(
                "entity_inventory",
                "vanilla_inventory",
                RadiationSourceType.ENTITY_INVENTORY,
                "minecraft:donkey",
                "uuid-2",
                BlockPos.ZERO,
                ResourceLocation.parse("createnuclear:raw_uranium"),
                10,
                1,
                1.0D,
                2.0D,
                2.0D,
                1.0D,
                true,
                10.0D,
                "carrier");
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:contain_donkey_scale",
                  "enabled": true,
                  "type": "contain",
                  "selectors": { "carrierEntityType": "minecraft:donkey" },
                  "mode": "scale",
                  "multiplier": 0.5
                }
                """);
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.MOB,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(1, result.sourcesForShielding().size());
        RadiationSource scaled = result.sourcesForShielding().getFirst();
        assertEquals("contained", scaled.overrideMode());
        assertEquals(5.0D, scaled.finalContribution(), 1.0e-9);
        assertEquals(5.0D, scaled.suppressedContribution(), 1.0e-9);
        assertTrue(result.containedSuppressedSources().isEmpty());
    }

    @Test
    void directNonNestedItemDoesNotMatchContainerItemContainmentRule() {
        RadiationSource source = RadiationSource.playerInventoryAggregate(
                ResourceLocation.parse("createnuclear:raw_uranium"),
                2,
                1,
                1.0D,
                2.0D,
                2.0D,
                2.0D,
                "direct");
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:contain_only_nested",
                  "enabled": true,
                  "type": "contain",
                  "selectors": { "containerItemId": "minecraft:shulker_box" },
                  "mode": "suppress"
                }
                """);
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(1, result.sourcesForShielding().size());
        assertEquals(2.0D, result.sourcesForShielding().getFirst().finalContribution(), 1.0e-9);
        assertTrue(result.containedSuppressedSources().isEmpty());
    }

    @Test
    void exclusionWinsOverContainment() {
        RadiationSource nestedSource = RadiationSource.playerInventoryAggregate(
                        ResourceLocation.parse("createnuclear:raw_uranium"),
                        3,
                        1,
                        1.0D,
                        2.0D,
                        2.0D,
                        3.0D,
                        "nested")
                .withNestedContext(1, ResourceLocation.parse("minecraft:shulker_box"), "player.slot[2].shulker.slot[0]");
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "exclude", parse("""
                        { "id":"radworks:exclude_nested", "enabled":true, "type":"exclude",
                          "selectors":{"containerItemId":"minecraft:shulker_box"} }
                        """),
                "contain", parse("""
                        { "id":"radworks:contain_nested", "enabled":true, "type":"contain",
                          "selectors":{"containerItemId":"minecraft:shulker_box"}, "mode":"suppress" }
                        """)));
        SourceOverrideDiagnostics.Builder diagnostics = SourceOverrideDiagnostics.builder();
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(nestedSource),
                rules,
                SourceScanSummary.builder(),
                diagnostics);
        SourceOverrideDiagnostics.store(diagnostics);
        JsonObject json = SourceOverrideDiagnostics.toJson();
        assertTrue(result.sourcesForShielding().isEmpty());
        assertEquals(1, result.excludedSources().size());
        assertTrue(result.containedSuppressedSources().isEmpty());
        assertTrue(json.get("containmentSkippedBecauseExcluded").getAsInt() >= 1);
    }

    @Test
    void suppressBeatsScaleAndMinScaleIsDeterministic() {
        RadiationSource nestedSource = RadiationSource.playerInventoryAggregate(
                        ResourceLocation.parse("createnuclear:raw_uranium"),
                        10,
                        1,
                        1.0D,
                        2.0D,
                        2.0D,
                        10.0D,
                        "nested")
                .withNestedContext(1, ResourceLocation.parse("minecraft:shulker_box"), "player.slot[2].shulker.slot[0]");
        SourceOverrideRules suppressVsScale = SourceOverrideRulesLoader.parseForTests(Map.of(
                "scale", parse("""
                        { "id":"radworks:scale_nested", "enabled":true, "type":"contain",
                          "selectors":{"containerItemId":"minecraft:shulker_box"}, "mode":"scale", "multiplier":0.7 }
                        """),
                "suppress", parse("""
                        { "id":"radworks:suppress_nested", "enabled":true, "type":"contain",
                          "selectors":{"containerItemId":"minecraft:shulker_box"}, "mode":"suppress" }
                        """)));
        SourceOverrideEngine.ApplicationResult suppressResult = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(nestedSource),
                suppressVsScale,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertTrue(suppressResult.sourcesForShielding().isEmpty());
        assertEquals(1, suppressResult.containedSuppressedSources().size());
        assertEquals(0.0D, suppressResult.containedSuppressedSources().getFirst().finalContribution(), 1.0e-9);

        SourceOverrideRules scaleVsScale = SourceOverrideRulesLoader.parseForTests(Map.of(
                "scale70", parse("""
                        { "id":"radworks:scale_nested_70", "enabled":true, "type":"contain",
                          "selectors":{"containerItemId":"minecraft:shulker_box"}, "mode":"scale", "multiplier":0.7 }
                        """),
                "scale40", parse("""
                        { "id":"radworks:scale_nested_40", "enabled":true, "type":"contain",
                          "selectors":{"containerItemId":"minecraft:shulker_box"}, "mode":"scale", "multiplier":0.4 }
                        """)));
        SourceOverrideEngine.ApplicationResult scaleResult = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(nestedSource),
                scaleVsScale,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(1, scaleResult.sourcesForShielding().size());
        assertEquals(4.0D, scaleResult.sourcesForShielding().getFirst().finalContribution(), 1.0e-9);
    }

    @Test
    void containmentDisablePathsAndForceRemainNotApplied() throws Exception {
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
                "contain", parse("""
                        { "id":"radworks:contain_rule", "enabled":true, "type":"contain",
                          "selectors":{"sourceType":"player_inventory"}, "mode":"suppress" }
                        """),
                "force", parse("""
                        { "id":"radworks:force_rule", "enabled":true, "type":"force",
                          "selectors":{"itemId":"minecraft:rotten_flesh"},
                          "forceStrength":1.0,
                          "forceRadius":2.0,
                          "forceUnitMode":"item_count" }
                        """)));

        boolean previousOverrides = RadWorksConfig.sourceOverridesEnabled();
        boolean previousContainment = RadWorksConfig.sourceContainmentEnabled();
        try {
            setBoolean("SOURCE_OVERRIDES_ENABLED", true);
            setBoolean("SOURCE_CONTAINMENT_ENABLED", false);
            SourceOverrideDiagnostics.Builder containmentOffDiagnostics = SourceOverrideDiagnostics.builder();
            SourceOverrideEngine.ApplicationResult containmentOff = SourceOverrideEngine.applyForTargetKind(
                    RadiationTargetKind.PLAYER,
                    List.of(source),
                    rules,
                    SourceScanSummary.builder(),
                    containmentOffDiagnostics);
            SourceOverrideDiagnostics.store(containmentOffDiagnostics);
            JsonObject containmentOffJson = SourceOverrideDiagnostics.toJson();
            assertEquals(1, containmentOff.sourcesForShielding().size());
            assertEquals(0, containmentOff.excludedSources().size());
            assertEquals(0, containmentOff.containedSuppressedSources().size());
            assertTrue(containmentOffJson.get("containRulesApplicationSkipped").getAsInt() >= 1);
            assertEquals(0, containmentOffJson.get("forceRulesApplicationSkipped").getAsInt());

            setBoolean("SOURCE_OVERRIDES_ENABLED", false);
            setBoolean("SOURCE_CONTAINMENT_ENABLED", true);
            SourceOverrideEngine.ApplicationResult overridesOff = SourceOverrideEngine.applyForTargetKind(
                    RadiationTargetKind.PLAYER,
                    List.of(source),
                    rules,
                    SourceScanSummary.builder(),
                    SourceOverrideDiagnostics.builder());
            assertEquals(1, overridesOff.sourcesForShielding().size());
            assertTrue(overridesOff.excludedSources().isEmpty());
            assertTrue(overridesOff.containedSuppressedSources().isEmpty());
        } finally {
            setBoolean("SOURCE_OVERRIDES_ENABLED", previousOverrides);
            setBoolean("SOURCE_CONTAINMENT_ENABLED", previousContainment);
        }
    }

    @Test
    void containmentHappensBeforeShieldingContract() {
        RadiationSource source = RadiationSource.entityInventoryCarrierItem(
                "entity_inventory",
                "vanilla_inventory",
                RadiationSourceType.ENTITY_INVENTORY,
                "minecraft:donkey",
                "uuid-3",
                BlockPos.ZERO,
                ResourceLocation.parse("createnuclear:raw_uranium"),
                10,
                1,
                1.0D,
                2.0D,
                2.0D,
                1.0D,
                true,
                10.0D,
                "carrier");
        SourceOverrideRules scaleRules = rules(
                """
                {
                  "id": "radworks:contain_scale_half",
                  "enabled": true,
                  "type": "contain",
                  "selectors": { "sourceType": "entity_inventory" },
                  "mode": "scale",
                  "multiplier": 0.5
                }
                """);
        SourceOverrideEngine.ApplicationResult scaleResult = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.MOB,
                List.of(source),
                scaleRules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(1, scaleResult.sourcesForShielding().size());
        RadiationSource scaled = scaleResult.sourcesForShielding().getFirst();
        assertEquals(5.0D, scaled.finalContribution(), 1.0e-9);
        RadiationSource scaledThenShielded = scaled.withShielding(ShieldingResult.reduced(scaled.finalContribution(), 1));
        assertEquals(2.5D, scaledThenShielded.finalContribution(), 1.0e-9);

        SourceOverrideRules suppressRules = rules(
                """
                {
                  "id": "radworks:contain_suppress_all",
                  "enabled": true,
                  "type": "contain",
                  "selectors": { "sourceType": "entity_inventory" },
                  "mode": "suppress"
                }
                """);
        SourceOverrideEngine.ApplicationResult suppressResult = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.MOB,
                List.of(source),
                suppressRules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertTrue(suppressResult.sourcesForShielding().isEmpty());
        assertEquals(1, suppressResult.containedSuppressedSources().size());
        assertEquals(0.0D, suppressResult.containedSuppressedSources().getFirst().finalContribution(), 1.0e-9);
    }

    @Test
    void forceItemCandidateCreatesSourceWhenNoNormalSourceExists() {
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:force_player_apple",
                  "enabled": true,
                  "type": "force",
                  "selectors": { "itemId": "minecraft:apple" },
                  "forceStrength": 2.0,
                  "forceRadius": 3.0,
                  "forceUnitMode": "item_count",
                  "forceRespectsShielding": true
                }
                """);
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
                "player_inventory",
                "candidate_without_normal_rule");
        SourceOverrideDiagnostics.Builder diagnostics = SourceOverrideDiagnostics.builder();
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(),
                List.of(candidate),
                rules,
                SourceScanSummary.builder(),
                diagnostics);
        SourceOverrideDiagnostics.store(diagnostics);
        JsonObject json = SourceOverrideDiagnostics.toJson();

        assertEquals(1, result.sourcesForShielding().size());
        RadiationSource forced = result.sourcesForShielding().getFirst();
        assertEquals("forced", forced.overrideMode());
        assertEquals("radworks:force_player_apple", forced.overrideRuleId());
        assertEquals(6.0D, forced.finalContribution(), 1.0e-9);
        assertEquals("not_applicable", forced.shielding());
        assertEquals(1, json.get("forcedSourcesAdded").getAsInt());
    }

    @Test
    void forcePositionedCandidateCanBeShielded() {
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:force_block_stone",
                  "enabled": true,
                  "type": "force",
                  "selectors": { "blockId": "minecraft:stone" },
                  "forceStrength": 4.0,
                  "forceRadius": 4.0,
                  "forceUnitMode": "block",
                  "forceRespectsShielding": true
                }
                """);
        ForceSourceCandidate candidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.BLOCK,
                RadiationSourceType.BLOCK,
                ResourceLocation.parse("minecraft:stone"),
                null,
                null,
                BlockPos.ZERO,
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
                "block",
                "candidate_without_normal_rule");
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(),
                List.of(candidate),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(1, result.sourcesForShielding().size());
        RadiationSource forced = result.sourcesForShielding().getFirst();
        assertEquals("clear", forced.shielding());
        assertEquals(4.0D, forced.finalContribution(), 1.0e-9);
        RadiationSource shielded = forced.withShielding(ShieldingResult.reduced(forced.finalContribution(), 1));
        assertEquals(2.0D, shielded.finalContribution(), 1.0e-9);
    }

    @Test
    void existingNormalSourcePreventsDuplicateForcedSource() {
        RadiationSource normal = RadiationSource.block(
                ResourceLocation.parse("minecraft:stone"),
                BlockPos.ZERO,
                2.0D,
                4.0D,
                1.0D,
                true,
                2.0D,
                "normal");
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:force_block_stone",
                  "enabled": true,
                  "type": "force",
                  "selectors": { "blockId": "minecraft:stone" },
                  "forceStrength": 4.0,
                  "forceRadius": 4.0,
                  "forceUnitMode": "block"
                }
                """);
        ForceSourceCandidate candidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.BLOCK,
                RadiationSourceType.BLOCK,
                ResourceLocation.parse("minecraft:stone"),
                null,
                null,
                BlockPos.ZERO,
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
                "block",
                "candidate_duplicate");
        SourceOverrideDiagnostics.Builder diagnostics = SourceOverrideDiagnostics.builder();
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(normal),
                List.of(candidate),
                rules,
                SourceScanSummary.builder(),
                diagnostics);
        SourceOverrideDiagnostics.store(diagnostics);
        JsonObject json = SourceOverrideDiagnostics.toJson();
        assertEquals(1, result.sourcesForShielding().size());
        assertEquals("none", result.sourcesForShielding().getFirst().overrideMode());
        assertEquals(0, json.get("forcedSourcesAdded").getAsInt());
        assertTrue(json.get("forceCandidatesSkippedExistingSource").getAsInt() >= 1);
    }

    @Test
    void excludeRulePreventsForceForCandidateIdentity() {
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "exclude", parse("""
                        { "id":"radworks:exclude_block_stone", "enabled":true, "type":"exclude",
                          "selectors":{"blockId":"minecraft:stone"} }
                        """),
                "force", parse("""
                        { "id":"radworks:force_block_stone", "enabled":true, "type":"force",
                          "selectors":{"blockId":"minecraft:stone"},
                          "forceStrength":4.0,
                          "forceRadius":4.0,
                          "forceUnitMode":"block" }
                        """)));
        ForceSourceCandidate candidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.BLOCK,
                RadiationSourceType.BLOCK,
                ResourceLocation.parse("minecraft:stone"),
                null,
                null,
                BlockPos.ZERO,
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
                "block",
                "candidate_excluded");
        SourceOverrideDiagnostics.Builder diagnostics = SourceOverrideDiagnostics.builder();
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(),
                List.of(candidate),
                rules,
                SourceScanSummary.builder(),
                diagnostics);
        SourceOverrideDiagnostics.store(diagnostics);
        JsonObject json = SourceOverrideDiagnostics.toJson();
        assertTrue(result.sourcesForShielding().isEmpty());
        assertEquals(0, json.get("forcedSourcesAdded").getAsInt());
        assertTrue(json.get("forceCandidatesSkippedExcluded").getAsInt() >= 1);
    }

    @Test
    void forcedSourceCanBeContained() {
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "force", parse("""
                        { "id":"radworks:force_nested_item", "enabled":true, "type":"force",
                          "selectors":{"itemId":"createnuclear:raw_uranium","containerItemId":"minecraft:shulker_box"},
                          "forceStrength":2.0,
                          "forceRadius":2.0,
                          "forceUnitMode":"item_count" }
                        """),
                "contain", parse("""
                        { "id":"radworks:contain_shulker_half", "enabled":true, "type":"contain",
                          "selectors":{"containerItemId":"minecraft:shulker_box"}, "mode":"scale", "multiplier":0.5 }
                        """)));
        ForceSourceCandidate candidate = new ForceSourceCandidate(
                ForceSourceCandidate.CandidateKind.ITEM,
                RadiationSourceType.ENTITY_INVENTORY,
                null,
                ResourceLocation.parse("createnuclear:raw_uranium"),
                null,
                BlockPos.ZERO,
                "minecraft:donkey",
                "uuid-10",
                ResourceLocation.parse("minecraft:shulker_box"),
                "entity_inventory.entity[uuid-10].slot[0].shulker.slot[1]",
                null,
                RadiationTargetKind.MOB,
                4,
                0,
                1.0D,
                true,
                true,
                1,
                "vanilla_inventory",
                "nested_candidate_without_normal_rule");
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.MOB,
                List.of(),
                List.of(candidate),
                rules,
                SourceScanSummary.builder(),
                SourceOverrideDiagnostics.builder());
        assertEquals(1, result.sourcesForShielding().size());
        RadiationSource containedForced = result.sourcesForShielding().getFirst();
        assertEquals("contained", containedForced.overrideMode());
        assertEquals("radworks:contain_shulker_half", containedForced.containmentRuleId());
        assertEquals(4.0D, containedForced.finalContribution(), 1.0e-9);
    }

    @Test
    void forceDisabledByConfigDoesNotApply() throws Exception {
        SourceOverrideRules rules = rules(
                """
                {
                  "id": "radworks:force_player_apple",
                  "enabled": true,
                  "type": "force",
                  "selectors": { "itemId": "minecraft:apple" },
                  "forceStrength": 1.0,
                  "forceRadius": 2.0,
                  "forceUnitMode": "item_count"
                }
                """);
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
                "player_inventory",
                "candidate_without_normal_rule");
        boolean previousOverrides = RadWorksConfig.sourceOverridesEnabled();
        boolean previousForce = RadWorksConfig.forcedSourcesEnabled();
        try {
            setBoolean("SOURCE_OVERRIDES_ENABLED", true);
            setBoolean("FORCED_SOURCES_ENABLED", false);
            SourceOverrideDiagnostics.Builder diagnostics = SourceOverrideDiagnostics.builder();
            SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                    RadiationTargetKind.PLAYER,
                    List.of(),
                    List.of(candidate),
                    rules,
                    SourceScanSummary.builder(),
                    diagnostics);
            SourceOverrideDiagnostics.store(diagnostics);
            JsonObject json = SourceOverrideDiagnostics.toJson();
            assertTrue(result.sourcesForShielding().isEmpty());
            assertTrue(json.get("forceRulesApplicationSkipped").getAsInt() >= 1);
        } finally {
            setBoolean("SOURCE_OVERRIDES_ENABLED", previousOverrides);
            setBoolean("FORCED_SOURCES_ENABLED", previousForce);
        }
    }

    @Test
    void sourceScanSummaryIncludesOverrideCounters() {
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
                        { "id":"radworks:exclude_rotten_flesh", "enabled":true, "type":"exclude",
                          "selectors":{"itemId":"minecraft:rotten_flesh"} }
                        """),
                "contain", parse("""
                        { "id":"radworks:contain_nested_scale", "enabled":true, "type":"contain",
                          "selectors":{"containerItemId":"minecraft:shulker_box"}, "mode":"scale", "multiplier":0.5 }
                        """)));
        RadiationSource nested = RadiationSource.playerInventoryAggregate(
                        ResourceLocation.parse("createnuclear:raw_uranium"),
                        3,
                        1,
                        1.0D,
                        2.0D,
                        2.0D,
                        3.0D,
                        "nested")
                .withNestedContext(
                        1,
                        ResourceLocation.parse("minecraft:shulker_box"),
                        "player.slot[2].shulker.slot[0]");
        SourceScanSummary.Builder summary = SourceScanSummary.builder();
        SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source, nested),
                rules,
                summary,
                SourceOverrideDiagnostics.builder());
        SourceScanSummary.store(summary, 0, 0);
        JsonObject json = SourceScanSummary.lastToJson().getAsJsonObject();
        assertEquals(1, json.get("sourcesExcludedByOverride").getAsInt());
        assertEquals(1, json.get("sourcesAfterOverrides").getAsInt());
        assertEquals(1, json.get("sourcesContainedByOverride").getAsInt());
        assertEquals(1, json.get("sourcesAfterContainment").getAsInt());
    }

    private static SourceOverrideRules rules(String json) {
        return SourceOverrideRulesLoader.parseForTests(Map.of("test:rule", parse(json)));
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static void setBoolean(String fieldName, boolean value) throws Exception {
        Field field = RadWorksConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ModConfigSpec.BooleanValue configValue = (ModConfigSpec.BooleanValue) field.get(null);
        configValue.set(value);
        configValue.clearCache();
    }
}
