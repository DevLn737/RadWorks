package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NestedProviderIntegrationAuditTest {
    @Test
    void providerWiringStillUsesNestedExtractor() throws IOException {
        String playerProvider = read("src/main/java/dev/radworks/radiation/PlayerInventorySourceProvider.java");
        String blockEntityProvider = read("src/main/java/dev/radworks/radiation/BlockEntityInventorySourceProvider.java");
        String blockItemHandlerProvider = read("src/main/java/dev/radworks/radiation/BlockItemHandlerSourceProvider.java");
        String entityCarrierProvider = read("src/main/java/dev/radworks/radiation/EntityCarrierSourceProvider.java");

        assertTrue(playerProvider.contains("NestedContainerExtractor.expand("));
        assertTrue(blockEntityProvider.contains("NestedContainerExtractor.expand("));
        assertTrue(blockItemHandlerProvider.contains("NestedContainerExtractor.expand("));
        assertTrue(entityCarrierProvider.contains("aggregateRadioactiveStackWithNested("));
        assertTrue(entityCarrierProvider.contains("entity_dropped_item"));
        assertTrue(entityCarrierProvider.contains("entity_item_frame"));
        assertTrue(entityCarrierProvider.contains("entity_inventory"));
    }

    @Test
    void chatRowsRemainCompactAndNoRawNbtDumpInCommands() throws IOException {
        String sources = read("src/main/java/dev/radworks/command/SourcesCommand.java");
        String exposure = read("src/main/java/dev/radworks/command/ExposureCommand.java");
        String sourcesLower = sources.toLowerCase();
        String exposureLower = exposure.toLowerCase();
        assertTrue(!sourcesLower.contains("nbt"));
        assertTrue(!exposureLower.contains("nbt"));
        assertTrue(sources.contains("nested=true"));
        assertTrue(exposure.contains("nested=true"));
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
