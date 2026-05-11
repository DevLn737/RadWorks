package dev.radworks.diagnostics;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

public final class RadiusVisualizationSamples {
    public static final int DEFAULT_DURATION_SECONDS = 5;
    public static final int MAX_DURATION_SECONDS = 30;
    public static final int PULSE_INTERVAL_TICKS = 10;
    public static final int MAX_VISUALIZED_SOURCES = 8;
    public static final int MAX_PARTICLES_PER_SOURCE = 64;
    public static final double HARD_MAX_VISUAL_RADIUS = 16.0D;

    private RadiusVisualizationSamples() {
    }

    public static int clampDurationSeconds(int requestedSeconds) {
        int normalized = requestedSeconds <= 0 ? DEFAULT_DURATION_SECONDS : requestedSeconds;
        return Math.max(1, Math.min(MAX_DURATION_SECONDS, normalized));
    }

    public static List<Vec3> shellPoints(Vec3 center, double radius, int maxPoints) {
        if (radius <= 0.0D || maxPoints <= 0) {
            return List.of();
        }

        int firstRingPoints = Math.max(8, Math.min(32, maxPoints / 2));
        int secondRingPoints = Math.max(0, maxPoints - firstRingPoints);
        List<Vec3> points = new ArrayList<>(firstRingPoints + secondRingPoints);
        addHorizontalRing(points, center, radius, firstRingPoints);
        if (secondRingPoints > 0) {
            addVerticalRing(points, center, radius, secondRingPoints);
        }
        if (points.size() > maxPoints) {
            return List.copyOf(points.subList(0, maxPoints));
        }
        return List.copyOf(points);
    }

    private static void addHorizontalRing(List<Vec3> points, Vec3 center, double radius, int pointCount) {
        for (int index = 0; index < pointCount; index++) {
            double angle = (Math.PI * 2.0D * index) / pointCount;
            points.add(new Vec3(
                    center.x + Math.cos(angle) * radius,
                    center.y,
                    center.z + Math.sin(angle) * radius));
        }
    }

    private static void addVerticalRing(List<Vec3> points, Vec3 center, double radius, int pointCount) {
        for (int index = 0; index < pointCount; index++) {
            double angle = (Math.PI * 2.0D * index) / pointCount;
            points.add(new Vec3(
                    center.x + Math.cos(angle) * radius,
                    center.y + Math.sin(angle) * radius,
                    center.z));
        }
    }
}
