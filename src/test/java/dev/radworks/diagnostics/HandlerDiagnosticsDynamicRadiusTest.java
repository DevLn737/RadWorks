package dev.radworks.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class HandlerDiagnosticsDynamicRadiusTest {
    @Test
    void itemHandlerSampleIncludesDynamicRadiusContext() {
        HandlerDiagnostics.Builder builder = HandlerDiagnostics.builder();
        builder.addItemHandlerSample(
                ResourceLocation.parse("create:item_vault"),
                new BlockPos(10, 64, 10),
                "unsided",
                20,
                0,
                java.util.List.of(HandlerDiagnostics.ContentSample.item(
                        null,
                        ResourceLocation.parse("createnuclear:raw_uranium"),
                        64,
                        "outside_dynamic_radius",
                        6.0D,
                        2.0D,
                        5.0D,
                        64.0D)));
        HandlerDiagnostics.store(builder);

        JsonObject json = HandlerDiagnostics.lastToJson().getAsJsonObject();
        JsonArray itemSamples = json.getAsJsonArray("itemHandlerNonMatchingSamples");
        assertEquals(1, itemSamples.size());
        JsonObject content = itemSamples.get(0).getAsJsonObject()
                .getAsJsonArray("contents")
                .get(0)
                .getAsJsonObject();
        assertEquals("outside_dynamic_radius", content.get("reason").getAsString());
        assertEquals(2.0D, content.get("baseRadius").getAsDouble(), 1.0e-9);
        assertEquals(5.0D, content.get("effectiveRadius").getAsDouble(), 1.0e-9);
        assertTrue(content.has("aggregateUnitsSnapshot"));
    }
}
