package dev.radworks.diagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DiagnosticsDumpSchemaContractTest {
    @Test
    void diagnosticsService_whenCreatingDump_shouldRegisterMandatorySections() throws IOException {
        String source = read("src/main/java/dev/radworks/diagnostics/DiagnosticsService.java");
        assertTrue(source.contains("root.add(\"sourceScanSummary\", SourceScanSummary.lastToJson())"));
        assertTrue(source.contains("root.add(\"handlerDiagnostics\", HandlerDiagnostics.lastToJson())"));
        assertTrue(source.contains("root.add(\"worldFluidDiagnostics\", WorldFluidDiagnostics.lastToJson())"));
        assertTrue(source.contains("root.add(\"createCarrierDiagnostics\", CreateCarrierDiagnostics.lastToJson())"));
        assertTrue(source.contains("root.add(\"entityCarrierDiagnostics\", EntityCarrierDiagnostics.lastToJson())"));
        assertTrue(source.contains("root.add(\"nestedContainerDiagnostics\", NestedContainerDiagnostics.lastToJson())"));
        assertTrue(source.contains("root.add(\"sourceOverrideDiagnostics\", SourceOverrideDiagnostics.toJson())"));
        assertTrue(source.contains("root.add(\"radiusVisualization\", RadiusVisualizationService.toJson(player))"));
    }

    @Test
    void sourceOverrideDiagnostics_whenNoRuntimeSnapshot_shouldStillExposeSchemaCounters() {
        JsonObject json = SourceOverrideDiagnostics.toJson();
        assertTrue(json.has("sourcesCheckedForOverrides"));
        assertTrue(json.has("sourcesExcluded"));
        assertTrue(json.has("sourcesContained"));
        assertTrue(json.has("forcedSourcesAdded"));
        assertTrue(json.has("forceCandidatesObserved"));
        assertTrue(json.has("containRulesApplicationSkipped"));
        assertTrue(json.has("forceRulesApplicationSkipped"));
    }

    @Test
    void sourceScanSummary_whenStored_shouldExposeOverrideAndForceCounters() {
        SourceScanSummary.Builder builder = SourceScanSummary.builder();
        builder.sourceExcludedByOverride();
        builder.sourceContainedByOverride();
        builder.forceCandidateObserved();
        builder.forcedSourceAdded();
        builder.sourceAfterForce();
        SourceScanSummary.store(builder, 1, 2);
        JsonObject json = SourceScanSummary.lastToJson().getAsJsonObject();
        assertTrue(json.has("sourcesExcludedByOverride"));
        assertTrue(json.has("sourcesContainedByOverride"));
        assertTrue(json.has("forceCandidatesObserved"));
        assertTrue(json.has("forcedSourcesAdded"));
        assertTrue(json.has("sourcesAfterForce"));
        assertTrue(json.has("sourcesShown"));
        assertTrue(json.has("sourcesOmitted"));
    }

    private static String read(String relativePath) throws IOException {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (cursor != null) {
            if (Files.exists(cursor.resolve("gradlew"))) {
                return Files.readString(cursor.resolve(relativePath));
            }
            cursor = cursor.getParent();
        }
        throw new IOException("project root with gradlew not found");
    }
}
