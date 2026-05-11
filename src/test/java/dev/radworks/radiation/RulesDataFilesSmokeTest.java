package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class RulesDataFilesSmokeTest {
    @Test
    void devRottenFleshRuleIsPresentAndValid() {
        JsonObject rule = readJson("data/radworks/radiation_rules/dev_rotten_flesh.json");
        assertRuleFields(rule);
        assertEquals("dev", rule.get("profile").getAsString());
        assertTrue(!rule.get("required").getAsBoolean());
        assertEquals("dev_smoke", rule.get("role").getAsString());
    }

    @Test
    void devGoldBlockRuleIsPresentAndValid() {
        JsonObject rule = readJson("data/radworks/radiation_rules/dev_gold_block.json");
        assertRuleFields(rule);
        assertEquals("dev", rule.get("profile").getAsString());
        assertTrue(!rule.get("required").getAsBoolean());
        assertEquals("dev_smoke", rule.get("role").getAsString());
    }

    @Test
    void devWaterRuleIsPresentAndValid() {
        JsonObject rule = readJson("data/radworks/radiation_rules/dev_water.json");
        assertRuleFields(rule);
        assertEquals("dev", rule.get("profile").getAsString());
        assertTrue(!rule.get("required").getAsBoolean());
        assertEquals("dev_smoke", rule.get("role").getAsString());
    }

    @Test
    void betaCandidateRulesArePresentAndOptionalSafe() {
        List<String> betaRulePaths = List.of(
                "data/radworks/radiation_rules/beta_item_createnuclear_raw_uranium.json",
                "data/radworks/radiation_rules/beta_item_create_crushed_raw_uranium.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_raw_uranium_block.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_uranium_ore.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_deepslate_uranium_ore.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_enriched_soul_soil.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_enriching_campfire.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_uranium_powder.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_uranium_bucket.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_uranium_rod.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_yellowcake.json",
                "data/radworks/radiation_rules/beta_item_createnuclear_enriched_yellowcake.json",
                "data/radworks/radiation_rules/beta_block_createnuclear_uranium_ore.json",
                "data/radworks/radiation_rules/beta_block_createnuclear_deepslate_uranium_ore.json",
                "data/radworks/radiation_rules/beta_block_createnuclear_raw_uranium_block.json",
                "data/radworks/radiation_rules/beta_block_createnuclear_enriched_soul_soil.json",
                "data/radworks/radiation_rules/beta_block_createnuclear_enriching_fire.json",
                "data/radworks/radiation_rules/beta_block_createnuclear_enriching_campfire.json",
                "data/radworks/radiation_rules/beta_fluid_createnuclear_uranium.json",
                "data/radworks/radiation_rules/beta_fluid_createnuclear_flowing_uranium.json");

        for (String path : betaRulePaths) {
            JsonObject rule = readJson(path);
            assertRuleFields(rule);
            assertEquals("beta", rule.get("profile").getAsString());
            assertTrue(!rule.get("required").getAsBoolean());
            assertEquals("real_candidate", rule.get("role").getAsString());
            assertTrue(rule.has("optionalModId"), "beta candidate must declare optionalModId: " + path);
        }
    }

    private static JsonObject readJson(String path) {
        InputStream stream = RulesDataFilesSmokeTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing resource: " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("Failed to parse resource: " + path, exception);
        }
    }

    private static void assertRuleFields(JsonObject rule) {
        assertTrue(rule.has("type"), "rule.type is required");
        assertTrue(rule.has("id"), "rule.id is required");
        assertTrue(rule.has("strength"), "rule.strength is required");
        assertTrue(rule.has("radius"), "rule.radius is required");
        assertTrue(rule.has("respectsShielding"), "rule.respectsShielding is required");
        assertTrue(rule.has("enabled"), "rule.enabled is required");

        assertTrue(rule.get("strength").getAsDouble() > 0.0D, "strength must be > 0");
        assertTrue(rule.get("radius").getAsDouble() > 0.0D, "radius must be > 0");
    }
}
