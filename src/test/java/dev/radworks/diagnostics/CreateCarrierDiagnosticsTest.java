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

    @Test
    void fluidPathSamplesAreBoundedAndContainSkipReason() {
        CreateCarrierDiagnostics.Builder builder = CreateCarrierDiagnostics.builder();
        int cap = dev.radworks.config.RadWorksConfig.createTransientCarrierDiagnosticSampleCap();
        for (int index = 0; index < cap + 5; index++) {
            builder.fluidPathSample(
                    ResourceLocation.parse("create:fluid_pipe"),
                    new BlockPos(0, 64, index),
                    "fluid_pipe",
                    "east",
                    "east.Flow.Fluid",
                    true,
                    true,
                    ResourceLocation.parse("createnuclear:uranium"),
                    1,
                    "none",
                    "no_active_fluid_rule");
        }
        CreateCarrierDiagnostics.store(builder);
        JsonObject json = CreateCarrierDiagnostics.lastToJson().getAsJsonObject();
        assertTrue(json.getAsJsonArray("fluidPathSamples").size() <= cap);
        assertTrue(json.getAsJsonArray("fluidPathSamples")
                .get(0)
                .getAsJsonObject()
                .get("parsedAmountMb")
                .getAsInt() == 1);
        assertTrue(json.getAsJsonArray("fluidPathSamples")
                .get(0)
                .getAsJsonObject()
                .get("skippedReason")
                .getAsString()
                .equals("no_active_fluid_rule"));
    }

    @Test
    void fluidPathSampleCanStoreOutsideDynamicRadiusReason() {
        CreateCarrierDiagnostics.Builder builder = CreateCarrierDiagnostics.builder();
        builder.fluidPathSample(
                ResourceLocation.parse("create:glass_fluid_pipe"),
                new BlockPos(1, 64, 1),
                "glass_fluid_pipe",
                "west",
                "west.Flow.Fluid",
                true,
                true,
                ResourceLocation.parse("createnuclear:uranium"),
                1,
                "exact",
                "outside_dynamic_radius");
        CreateCarrierDiagnostics.store(builder);
        JsonObject json = CreateCarrierDiagnostics.lastToJson().getAsJsonObject();
        assertTrue(json.getAsJsonArray("fluidPathSamples")
                .get(0)
                .getAsJsonObject()
                .get("skippedReason")
                .getAsString()
                .equals("outside_dynamic_radius"));
    }
}
