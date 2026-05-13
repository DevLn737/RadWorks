package dev.radworks.diagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EntityCarrierDiagnosticsTest {
    @Test
    void skipSamplesAreBounded() {
        EntityCarrierDiagnostics.Builder builder = EntityCarrierDiagnostics.builder();
        int cap = Math.min(
                RadWorksConfig.entityCarrierDiagnosticSampleCap(),
                RadWorksConfig.entityInventoryDiagnosticSampleCap());
        for (int i = 0; i < cap + 5; i++) {
            builder.skippedEntity(
                    "dropped_item",
                    ResourceLocation.parse("minecraft:item"),
                    "uuid-" + i,
                    ResourceLocation.parse("minecraft:rotten_flesh"),
                    1,
                    "no_active_rule");
        }
        EntityCarrierDiagnostics.store(builder);
        JsonObject json = EntityCarrierDiagnostics.lastToJson().getAsJsonObject();
        assertTrue(json.getAsJsonArray("skipSamples").size() <= cap);
    }

    @Test
    void inventoryCountersAreExposed() {
        EntityCarrierDiagnostics.Builder builder = EntityCarrierDiagnostics.builder();
        builder.entityInventoryEntityChecked();
        builder.entityInventoryAccessSucceeded();
        builder.entityInventoryAccessFailed();
        builder.matchedChestBoatSource();
        builder.matchedPackAnimalSource();
        builder.matchedGenericEntityInventorySource();

        EntityCarrierDiagnostics.store(builder);
        JsonObject json = EntityCarrierDiagnostics.lastToJson().getAsJsonObject();
        assertTrue(json.get("entityInventoryEntitiesChecked").getAsInt() >= 1);
        assertTrue(json.get("entityInventoryAccessSucceeded").getAsInt() >= 1);
        assertTrue(json.get("entityInventoryAccessFailed").getAsInt() >= 1);
        assertTrue(json.get("matchedChestBoatSources").getAsInt() >= 1);
        assertTrue(json.get("matchedPackAnimalSources").getAsInt() >= 1);
        assertTrue(json.get("matchedGenericEntityInventorySources").getAsInt() >= 1);
    }
}
