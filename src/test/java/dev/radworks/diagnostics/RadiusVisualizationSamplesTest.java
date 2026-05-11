package dev.radworks.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RadiusVisualizationSamplesTest {
    @Test
    void radiusLessOrEqualZeroProducesNoSamples() {
        List<Vec3> samples = RadiusVisualizationSamples.shellPoints(new Vec3(0.0D, 64.0D, 0.0D), 0.0D, 64);
        assertTrue(samples.isEmpty());
    }

    @Test
    void sampleCountIsBoundedByCap() {
        List<Vec3> samples = RadiusVisualizationSamples.shellPoints(new Vec3(0.0D, 64.0D, 0.0D), 6.0D, 24);
        assertTrue(samples.size() <= 24);
    }

    @Test
    void durationClampUsesMaxThirtySeconds() {
        assertEquals(30, RadiusVisualizationSamples.clampDurationSeconds(120));
        assertEquals(5, RadiusVisualizationSamples.clampDurationSeconds(0));
        assertEquals(1, RadiusVisualizationSamples.clampDurationSeconds(1));
    }
}
