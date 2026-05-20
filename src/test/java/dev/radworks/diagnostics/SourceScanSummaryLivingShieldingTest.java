package dev.radworks.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class SourceScanSummaryLivingShieldingTest {
    @Test
    void livingShieldingCountersAreSerialized() {
        SourceScanSummary.Builder builder = SourceScanSummary.builder();
        builder.livingShieldingSourceChecked();
        builder.livingShieldingSourceChecked();
        builder.livingShieldingSampleChecked();
        builder.livingShieldingSourceReduced();
        SourceScanSummary.store(builder, 0, 0);

        JsonObject json = SourceScanSummary.lastToJson().getAsJsonObject();
        assertEquals(2, json.get("livingShieldingSourcesChecked").getAsInt());
        assertEquals(1, json.get("livingShieldingSourcesReduced").getAsInt());
        assertEquals(1, json.get("livingShieldingSamplesChecked").getAsInt());
    }
}
