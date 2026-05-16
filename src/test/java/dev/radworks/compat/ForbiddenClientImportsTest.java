package dev.radworks.compat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ForbiddenClientImportsTest {
    private static final String MAIN_ROOT_RELATIVE = "src/main/java/dev/radworks";
    private static final Set<String> FORBIDDEN_IMPORT_PREFIXES = Set.of(
            "net.minecraft.client",
            "net.neoforged.neoforge.client",
            "com.mojang.blaze3d");

    // Allowlist intentionally empty for now. If an exception is required later, add it with a comment.
    private static final Set<String> ALLOWED_IMPORTS = Set.of();

    @Test
    void commonAndServerCodeMustNotImportClientOnlyPackages() throws IOException {
        Path mainRoot = resolveFromProjectRoot(MAIN_ROOT_RELATIVE);
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(mainRoot)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> collectViolations(path, violations));
        }

        assertTrue(
                violations.isEmpty(),
                "Client-only imports found in common/server code:\n" + String.join("\n", violations));
    }

    private static void collectViolations(Path file, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("import ")) {
                    continue;
                }
                String imported = trimmed.substring("import ".length()).replace(";", "").trim();
                if (ALLOWED_IMPORTS.contains(imported)) {
                    continue;
                }
                for (String forbiddenPrefix : FORBIDDEN_IMPORT_PREFIXES) {
                    if (imported.startsWith(forbiddenPrefix)) {
                        violations.add(file + " -> " + imported);
                        break;
                    }
                }
            }
        } catch (IOException exception) {
            violations.add(file + " -> IO error: " + exception.getMessage());
        }
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
