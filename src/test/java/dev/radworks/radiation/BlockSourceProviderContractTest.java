package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BlockSourceProviderContractTest {
    @Test
    void effectiveScanRadius_whenRuleRadiusExceedsCap_shouldClampToProviderMax() throws Exception {
        RadiationRules rules = rulesWithBlock("minecraft:gold_block", 1.0D, 20.0D);
        Method method = BlockSourceProvider.class.getDeclaredMethod("effectiveScanRadius", RadiationRules.class);
        method.setAccessible(true);
        int effective = (int) method.invoke(null, rules);
        assertEquals(BlockSourceProvider.MAX_SCAN_RADIUS, effective);
    }

    @Test
    void collectTimed_contractMarkersShouldIncludeMatchDistanceAndForceCandidatePaths() throws IOException {
        String source = read("src/main/java/dev/radworks/radiation/BlockSourceProvider.java");
        assertTrue(source.contains("if (rule == null)"));
        assertTrue(source.contains("candidateSink.observe(new ForceSourceCandidate("));
        assertTrue(source.contains("block_observed_without_block_rule"));
        assertTrue(source.contains("if (distance > rule.radius())"));
        assertTrue(source.contains("sources.add(RadiationSource.block("));
    }

    private static RadiationRules rulesWithBlock(String id, double strength, double radius) {
        RadiationRule rule = new RadiationRule(
                RadiationRuleType.BLOCK,
                ResourceLocation.parse(id),
                strength,
                radius,
                true,
                true,
                RadiationRuleProfile.BETA,
                false,
                null,
                "test",
                "test",
                "test");
        return new RadiationRules(
                true,
                "test",
                List.of(rule),
                0,
                0,
                0,
                1,
                List.of(),
                new RadiationRuleValidationResult());
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
