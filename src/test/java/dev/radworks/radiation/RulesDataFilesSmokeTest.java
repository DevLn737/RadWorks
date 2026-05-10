package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RulesDataFilesSmokeTest {
    @Test
    void devRottenFleshRuleIsPresentAndValid() {
        JsonObject rule = readJson("data/radworks/radiation_rules/dev_rotten_flesh.json");
        assertRuleFields(rule);
    }

    @Test
    void devGoldBlockRuleIsPresentAndValid() {
        JsonObject rule = readJson("data/radworks/radiation_rules/dev_gold_block.json");
        assertRuleFields(rule);
    }

    @Test
    void devWaterRuleIsPresentAndValid() {
        JsonObject rule = readJson("data/radworks/radiation_rules/dev_water.json");
        assertRuleFields(rule);
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
