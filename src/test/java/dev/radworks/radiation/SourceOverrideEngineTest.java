package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.SourceOverrideDiagnostics;
import dev.radworks.diagnostics.SourceScanSummary;
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
    void containAndForceRulesRemainNotAppliedInBeta062() {
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
                          "selectors":{"sourceType":"player_inventory"} }
                        """)));

        SourceOverrideDiagnostics.Builder diagnostics = SourceOverrideDiagnostics.builder();
        SourceOverrideEngine.ApplicationResult result = SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source),
                rules,
                SourceScanSummary.builder(),
                diagnostics);
        SourceOverrideDiagnostics.store(diagnostics);
        JsonObject json = SourceOverrideDiagnostics.toJson();

        assertEquals(1, result.sourcesForShielding().size());
        assertEquals(0, result.excludedSources().size());
        assertTrue(json.get("containRulesApplicationSkipped").getAsInt() >= 1);
        assertTrue(json.get("forceRulesApplicationSkipped").getAsInt() >= 1);
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
        SourceOverrideEngine.applyForTargetKind(
                RadiationTargetKind.PLAYER,
                List.of(source),
                rules,
                summary,
                SourceOverrideDiagnostics.builder());
        SourceScanSummary.store(summary, 0, 0);
        JsonObject json = SourceScanSummary.lastToJson().getAsJsonObject();
        assertEquals(1, json.get("sourcesExcludedByOverride").getAsInt());
        assertEquals(0, json.get("sourcesAfterOverrides").getAsInt());
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
