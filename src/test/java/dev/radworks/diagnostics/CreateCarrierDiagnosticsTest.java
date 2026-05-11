package dev.radworks.diagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class CreateCarrierDiagnosticsTest {
    @Test
    void unexpectedSamplesAreBounded() {
        CreateCarrierDiagnostics.Builder builder = CreateCarrierDiagnostics.builder();
        int cap = dev.radworks.config.RadWorksConfig.createTransientCarrierDiagnosticSampleCap();
        for (int index = 0; index < cap + 5; index++) {
            builder.unexpectedStructure(
                    ResourceLocation.parse("create:placard"),
                    new BlockPos(index, 64, 0),
                    "placard",
                    "Item",
                    "missing_or_invalid_item_payload");
        }
        CreateCarrierDiagnostics.store(builder);
        JsonObject json = CreateCarrierDiagnostics.lastToJson().getAsJsonObject();
        assertTrue(json.getAsJsonArray("unexpectedStructureSamples").size() <= cap);
    }
}
