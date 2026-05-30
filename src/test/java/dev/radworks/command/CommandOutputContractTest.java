package dev.radworks.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CommandOutputContractTest {
    @Test
    void validateCommand_shouldExposeStableValidationMarkers() throws IOException {
        String source = read("src/main/java/dev/radworks/command/ValidateCommand.java");
        assertTrue(source.contains("[RadWorks] Validate"));
        assertTrue(source.contains("Source overrides: loaded="));
        assertTrue(source.contains("Source override config: enabled="));
        assertTrue(source.contains("Dynamic radius: enabled="));
    }

    @Test
    void sourcesAndExposureCommands_shouldExposeStableSummaryMarkers() throws IOException {
        String sources = read("src/main/java/dev/radworks/command/SourcesCommand.java");
        assertTrue(sources.contains("[RadWorks] Sources"));
        assertTrue(sources.contains("Total sources: "));
        assertTrue(sources.contains("Output: sourcesShown="));

        String exposure = read("src/main/java/dev/radworks/command/ExposureCommand.java");
        assertTrue(exposure.contains("[RadWorks] Exposure"));
        assertTrue(exposure.contains("Total: "));
        assertTrue(exposure.contains("Effect: mode="));
        assertTrue(exposure.contains("Output: sourcesShown="));
    }

    @Test
    void radiusAndEffectCommands_shouldExposeStableRuntimeMarkers() throws IOException {
        String radius = read("src/main/java/dev/radworks/command/RadiusCommand.java");
        assertTrue(radius.contains("[RadWorks] Radius visualization"));
        assertTrue(radius.contains("[RadWorks] Radius visualization status"));
        assertTrue(radius.contains("Result: visualizedSources="));

        String effect = read("src/main/java/dev/radworks/command/EffectCommand.java");
        assertTrue(effect.contains("[RadWorks] Effect apply-self"));
        assertTrue(effect.contains("[RadWorks] Effect clear-self"));
        assertTrue(effect.contains("[RadWorks] Effect status"));
        assertTrue(effect.contains("player required; use /radworks effect"));
    }

    @Test
    void commandRegistration_shouldKeepExpectedRadworksSubcommands() throws IOException {
        String commands = read("src/main/java/dev/radworks/command/RadWorksCommands.java");
        assertTrue(commands.contains("Commands.literal(\"validate\")"));
        assertTrue(commands.contains("Commands.literal(\"sources\")"));
        assertTrue(commands.contains("Commands.literal(\"exposure\")"));
        assertTrue(commands.contains("Commands.literal(\"dump\")"));
        assertTrue(commands.contains("Commands.literal(\"radius\")"));
        assertTrue(commands.contains("Commands.literal(\"effect\")"));
        assertTrue(commands.contains("Commands.literal(\"apply-self\")"));
        assertTrue(commands.contains("Commands.literal(\"clear-self\")"));
        assertTrue(commands.contains("Commands.literal(\"status\")"));
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
