package dev.radworks.diagnostics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RadiusVisualizationServerSafetyTest {
    private static final String SERVICE_FILE_RELATIVE = "src/main/java/dev/radworks/diagnostics/RadiusVisualizationService.java";

    @Test
    void radiusVisualizationServiceHasNoClientOnlyImportsOrCalls() throws IOException {
        Path serviceFile = resolveFromProjectRoot(SERVICE_FILE_RELATIVE);
        List<String> lines = Files.readAllLines(serviceFile);
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("import ")) {
                continue;
            }
            assertFalse(trimmed.contains("net.minecraft.client"), "client import found: " + trimmed);
            assertFalse(trimmed.contains("net.neoforged.neoforge.client"), "client import found: " + trimmed);
            assertFalse(trimmed.contains("com.mojang.blaze3d"), "client import found: " + trimmed);
        }

        String text = String.join("\n", lines);
        assertFalse(text.contains("Minecraft.getInstance()"), "client call found: Minecraft.getInstance()");
    }

    @Test
    void radiusVisualizationCapsAreBounded() {
        assertTrue(RadiusVisualizationSamples.DEFAULT_DURATION_SECONDS > 0);
        assertTrue(RadiusVisualizationSamples.MAX_DURATION_SECONDS >= RadiusVisualizationSamples.DEFAULT_DURATION_SECONDS);
        assertTrue(RadiusVisualizationSamples.PULSE_INTERVAL_TICKS > 0);
        assertTrue(RadiusVisualizationSamples.MAX_VISUALIZED_SOURCES > 0);
        assertTrue(RadiusVisualizationSamples.MAX_PARTICLES_PER_SOURCE > 0);
        assertTrue(RadiusVisualizationSamples.HARD_MAX_VISUAL_RADIUS > 0.0D);
    }

    private static Path resolveFromProjectRoot(String relativePath) throws IOException {
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
            Path candidate = cursor.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Cannot resolve path from project root: " + relativePath + " (from " + current + ")");
    }
}
