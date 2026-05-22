package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SourceOverrideRulesLoaderTest {
    @Test
    void validExcludeRuleLoads() {
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:exclude",
                parse(
                        """
                        {
                          "id": "radworks:exclude_test",
                          "enabled": true,
                          "type": "exclude",
                          "selectors": { "blockId": "minecraft:chest" }
                        }
                        """)));
        assertEquals(1, rules.overrideRulesLoaded());
        assertEquals(1, rules.excludeRulesLoaded());
        assertEquals(0, rules.validationResult().errors().size());
    }

    @Test
    void validContainRulesLoad() {
        SourceOverrideRules suppressRules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:contain_suppress",
                parse(
                        """
                        {
                          "id": "radworks:contain_suppress_test",
                          "enabled": true,
                          "type": "contain",
                          "selectors": { "containerItemId": "minecraft:shulker_box" },
                          "mode": "suppress"
                        }
                        """)));
        assertEquals(1, suppressRules.containRulesLoaded());
        assertEquals(0, suppressRules.validationResult().errors().size());

        SourceOverrideRules scaleRules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:contain_scale",
                parse(
                        """
                        {
                          "id": "radworks:contain_scale_test",
                          "enabled": true,
                          "type": "contain",
                          "selectors": { "containerItemId": "minecraft:barrel" },
                          "mode": "scale",
                          "multiplier": 0.5
                        }
                        """)));
        assertEquals(1, scaleRules.containRulesLoaded());
        assertEquals(0, scaleRules.validationResult().errors().size());
    }

    @Test
    void validForceRuleLoads() {
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:force",
                parse(
                        """
                        {
                          "id": "radworks:force_test",
                          "enabled": true,
                          "type": "force",
                          "selectors": { "fluidId": "createnuclear:uranium" },
                          "forceStrength": 1.5,
                          "forceRadius": 4.0,
                          "forceUnitMode": "fluid_mb",
                          "forceRespectsShielding": true
                        }
                        """)));
        assertEquals(1, rules.forceRulesLoaded());
        assertEquals(0, rules.validationResult().errors().size());
        JsonObject diagnostics = rules.toDiagnosticsJson();
        assertEquals(
                SourceOverrideRules.APPLICATION_PHASE,
                diagnostics.get("applicationPhase").getAsString());
    }

    @Test
    void invalidForceRuleMissingRuntimeFieldsProducesValidationIssues() {
        SourceOverrideRules missingStrength = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:force_missing_strength",
                parse(
                        """
                        {
                          "id": "radworks:force_missing_strength",
                          "enabled": true,
                          "type": "force",
                          "selectors": { "itemId": "minecraft:apple" },
                          "forceRadius": 4.0,
                          "forceUnitMode": "item_count"
                        }
                        """)));
        assertTrue(missingStrength.validationResult().hasErrors());

        SourceOverrideRules missingRadius = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:force_missing_radius",
                parse(
                        """
                        {
                          "id": "radworks:force_missing_radius",
                          "enabled": true,
                          "type": "force",
                          "selectors": { "itemId": "minecraft:apple" },
                          "forceStrength": 1.0,
                          "forceUnitMode": "item_count"
                        }
                        """)));
        assertTrue(missingRadius.validationResult().hasErrors());

        SourceOverrideRules missingUnit = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:force_missing_unit",
                parse(
                        """
                        {
                          "id": "radworks:force_missing_unit",
                          "enabled": true,
                          "type": "force",
                          "selectors": { "itemId": "minecraft:apple" },
                          "forceStrength": 1.0,
                          "forceRadius": 4.0
                        }
                        """)));
        assertTrue(missingUnit.validationResult().hasErrors());
    }

    @Test
    void forceRuleRequiresConcreteSelector() {
        SourceOverrideRules noConcreteSelector = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:force_no_selector",
                parse(
                        """
                        {
                          "id": "radworks:force_no_selector",
                          "enabled": true,
                          "type": "force",
                          "selectors": { "sourceType": "player_inventory", "targetKind": "player" },
                          "forceStrength": 2.0,
                          "forceRadius": 4.0,
                          "forceUnitMode": "item_count"
                        }
                        """)));
        assertTrue(noConcreteSelector.validationResult().hasErrors());
    }

    @Test
    void disabledRuleCountedAndMissingOptionalModIsNonFatal() {
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:disabled_optional",
                parse(
                        """
                        {
                          "id": "radworks:disabled_optional_test",
                          "enabled": false,
                          "type": "exclude",
                          "required": false,
                          "optionalModId": "definitely_missing_optional_mod",
                          "selectors": { "itemId": "definitelymissingmod:thing" }
                        }
                        """)));
        assertEquals(1, rules.overrideRulesLoaded());
        assertEquals(0, rules.overrideRulesEnabled());
        assertEquals(1, rules.overrideRulesDisabled());
        assertTrue(rules.missingOptionalRuleTargets() >= 1);
        assertFalse(rules.validationResult().hasErrors());
    }

    @Test
    void invalidTypeAndInvalidSelectorsProduceValidationIssues() {
        SourceOverrideRules invalidType = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:invalid_type",
                parse(
                        """
                        {
                          "id": "radworks:invalid_type_test",
                          "enabled": true,
                          "type": "bad_type",
                          "selectors": { "blockId": "minecraft:stone" }
                        }
                        """)));
        assertTrue(invalidType.validationResult().hasErrors());

        SourceOverrideRules invalidSelector = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:invalid_selector",
                parse(
                        """
                        {
                          "id": "radworks:invalid_selector_test",
                          "enabled": true,
                          "type": "exclude",
                          "selectors": { "itemId": "Not Valid" }
                        }
                        """)));
        assertTrue(invalidSelector.validationResult().hasErrors());
    }

    @Test
    void invalidContainMultiplierProducesValidationIssue() {
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:invalid_multiplier",
                parse(
                        """
                        {
                          "id": "radworks:invalid_multiplier_test",
                          "enabled": true,
                          "type": "contain",
                          "selectors": { "sourceType": "block_entity_inventory" },
                          "mode": "scale",
                          "multiplier": 1.5
                        }
                        """)));
        assertTrue(rules.validationResult().hasErrors());
    }

    @Test
    void diagnosticsCountersAndSamplesAreProduced() {
        SourceOverrideRules rules = SourceOverrideRulesLoader.parseForTests(Map.of(
                "test:good",
                parse(
                        """
                        {
                          "id": "radworks:good_rule",
                          "enabled": true,
                          "type": "exclude",
                          "selectors": { "sourceType": "world_fluid" }
                        }
                        """),
                "test:bad",
                parse(
                        """
                        {
                          "id": "radworks:bad_rule",
                          "enabled": true,
                          "type": "contain",
                          "selectors": {},
                          "mode": "scale"
                        }
                        """)));

        JsonObject diagnostics = rules.toDiagnosticsJson();
        assertEquals(2, diagnostics.get("overrideRulesLoaded").getAsInt());
        assertTrue(diagnostics.get("overrideRuleErrors").getAsInt() >= 1);
        assertTrue(diagnostics.getAsJsonArray("samples").size() >= 1);
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
