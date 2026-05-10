package dev.radworks.radiation.shielding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShieldingTagDataContractTest {
    @Test
    void shieldingTagContainsDevAndOptionalEntries() {
        JsonObject tag = readJson("data/radworks/tags/block/shielding_blocks.json");
        assertTrue(tag.has("values"), "shielding tag must contain values");
        JsonArray values = tag.getAsJsonArray("values");

        Set<String> foundIds = new HashSet<>();
        int optionalCount = 0;
        for (JsonElement value : values) {
            if (value.isJsonPrimitive()) {
                foundIds.add(value.getAsString());
                continue;
            }

            JsonObject object = value.getAsJsonObject();
            String id = object.get("id").getAsString();
            foundIds.add(id);
            assertTrue(object.has("required"), "optional entry must explicitly set required");
            assertTrue(!object.get("required").getAsBoolean(), "optional entry must use required=false");
            optionalCount++;
        }

        assertTrue(foundIds.contains("minecraft:iron_block"), "dev/test entry must include minecraft:iron_block");
        assertTrue(foundIds.contains("tfmg:raw_lead_block"), "optional tfmg:raw_lead_block must be present");
        assertTrue(foundIds.contains("tfmg:lead_block"), "optional tfmg:lead_block must be present");
        assertTrue(foundIds.contains("tfmg:lead_ore"), "optional tfmg:lead_ore must be present");
        assertTrue(foundIds.contains("createnuclear:reinforced_glass"), "optional createnuclear entry must be present");
        assertEquals(4, optionalCount, "expected exactly four optional shielding candidate entries");
    }

    private static JsonObject readJson(String path) {
        InputStream stream = ShieldingTagDataContractTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing resource: " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("Failed to parse resource: " + path, exception);
        }
    }
}
