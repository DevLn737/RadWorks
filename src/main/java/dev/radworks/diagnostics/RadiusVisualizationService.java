package dev.radworks.diagnostics;

import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import dev.radworks.radiation.ExposureBreakdown;
import dev.radworks.radiation.ExposureEngine;
import dev.radworks.radiation.RadiationSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class RadiusVisualizationService {
    private static final long CAP_WARNING_COOLDOWN_TICKS = 200L;
    private static final Map<UUID, ActiveVisualization> ACTIVE = new LinkedHashMap<>();
    private static final Map<UUID, Long> ALWAYS_ON_NEXT_PULSE = new LinkedHashMap<>();
    private static final Map<UUID, Long> LAST_CAP_WARNING_TICK = new LinkedHashMap<>();
    private static final Map<UUID, VisualState> LAST_STATE = new LinkedHashMap<>();

    private RadiusVisualizationService() {
    }

    public static ShowResult show(ServerPlayer player, int requestedSeconds) {
        int durationSeconds = RadiusVisualizationSamples.clampDurationSeconds(requestedSeconds);
        long gameTime = player.serverLevel().getGameTime();
        long expiresAtGameTime = gameTime + durationSeconds * 20L;
        RenderSummary summary = renderNow(player, gameTime);
        synchronized (ACTIVE) {
            ACTIVE.put(player.getUUID(), new ActiveVisualization(
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    gameTime,
                    expiresAtGameTime,
                    gameTime + RadiusVisualizationSamples.PULSE_INTERVAL_TICKS));
        }
        synchronized (ALWAYS_ON_NEXT_PULSE) {
            ALWAYS_ON_NEXT_PULSE.remove(player.getUUID());
        }
        updateLastState(player, expiresAtGameTime, summary);
        return new ShowResult(
                durationSeconds,
                summary.visualizedSources(),
                summary.skippedSources(),
                summary.maxRadiusSeen(),
                "visual only, no gameplay effect");
    }

    public static boolean clear(ServerPlayer player) {
        boolean removed;
        synchronized (ACTIVE) {
            removed = ACTIVE.remove(player.getUUID()) != null;
        }
        synchronized (ALWAYS_ON_NEXT_PULSE) {
            ALWAYS_ON_NEXT_PULSE.remove(player.getUUID());
        }
        synchronized (LAST_CAP_WARNING_TICK) {
            LAST_CAP_WARNING_TICK.remove(player.getUUID());
        }
        updateLastState(player, 0L, new RenderSummary(0, 0, 0.0D));
        return removed;
    }

    public static Status status(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        VisualState visualState;
        synchronized (LAST_STATE) {
            visualState = LAST_STATE.get(player.getUUID());
        }
        boolean alwaysOn = RadWorksConfig.alwaysShowRadiusVisualization();
        if (visualState == null) {
            return new Status(alwaysOn, alwaysOn ? -1L : 0L, 0, 0, 0.0D, alwaysOn);
        }
        long remainingTicks = alwaysOn ? -1L : Math.max(0L, visualState.expiresAtGameTime() - gameTime);
        return new Status(
                alwaysOn || remainingTicks > 0L,
                remainingTicks,
                visualState.lastVisualizedSources(),
                visualState.lastSkippedSources(),
                visualState.maxRadiusSeen(),
                alwaysOn);
    }

    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        boolean alwaysOn = RadWorksConfig.alwaysShowRadiusVisualization();
        ActiveVisualization activeVisualization;
        synchronized (ACTIVE) {
            if (ACTIVE.isEmpty()) {
                activeVisualization = null;
            } else {
                activeVisualization = ACTIVE.get(player.getUUID());
            }
        }
        if (activeVisualization != null && gameTime >= activeVisualization.expiresAtGameTime()) {
            synchronized (ACTIVE) {
                ACTIVE.remove(player.getUUID());
            }
            activeVisualization = null;
        }
        if (activeVisualization == null && !alwaysOn) {
            synchronized (ALWAYS_ON_NEXT_PULSE) {
                ALWAYS_ON_NEXT_PULSE.remove(player.getUUID());
            }
            return;
        }

        long nextPulseGameTime = resolveNextPulseGameTime(player.getUUID(), activeVisualization, gameTime);
        if (gameTime < nextPulseGameTime) {
            return;
        }
        boolean manualActive = activeVisualization != null;

        PerformanceStats.timeValue("radius_visualization", () -> {
            RenderSummary summary = renderNow(player, gameTime);
            synchronized (ACTIVE) {
                ActiveVisualization current = ACTIVE.get(player.getUUID());
                if (current != null) {
                    ACTIVE.put(player.getUUID(), new ActiveVisualization(
                            current.playerUuid(),
                            current.playerName(),
                            current.startedAtGameTime(),
                            current.expiresAtGameTime(),
                            gameTime + RadiusVisualizationSamples.PULSE_INTERVAL_TICKS));
                    updateLastState(player, current.expiresAtGameTime(), summary);
                }
            }
            if (alwaysOn) {
                synchronized (ALWAYS_ON_NEXT_PULSE) {
                    ALWAYS_ON_NEXT_PULSE.put(player.getUUID(), gameTime + RadiusVisualizationSamples.PULSE_INTERVAL_TICKS);
                }
                if (!manualActive) {
                    updateLastState(player, gameTime + RadiusVisualizationSamples.PULSE_INTERVAL_TICKS, summary);
                }
            }
            return Boolean.TRUE;
        });
    }

    public static JsonObject toJson(ServerPlayer currentPlayer) {
        JsonObject json = new JsonObject();
        json.addProperty("activeCount", activeCount());
        if (currentPlayer == null) {
            json.addProperty("active", false);
            return json;
        }

        Status status = status(currentPlayer);
        json.addProperty("active", status.active());
        json.addProperty("remainingTicks", status.remainingTicks());
        json.addProperty("remainingSeconds", status.remainingTicks() < 0L ? -1.0D : status.remainingTicks() / 20.0D);
        json.addProperty("lastVisualizedSources", status.lastVisualizedSources());
        json.addProperty("lastSkippedSources", status.lastSkippedSources());
        json.addProperty("maxRadiusSeen", status.maxRadiusSeen());
        json.addProperty("alwaysOnFromConfig", status.alwaysOnFromConfig());
        return json;
    }

    private static int activeCount() {
        synchronized (ACTIVE) {
            return ACTIVE.size();
        }
    }

    private static RenderSummary renderNow(ServerPlayer player, long gameTime) {
        ExposureBreakdown breakdown = ExposureEngine.calculate(player);
        List<RadiationSource> sources = breakdown.sources();

        int visualized = 0;
        int skipped = 0;
        double maxRadiusSeen = 0.0D;
        double radiusCeiling = Math.min(
                RadiusVisualizationSamples.HARD_MAX_VISUAL_RADIUS,
                Math.max(0.0D, RadWorksConfig.dynamicRadiusMaxCap()));

        for (RadiationSource source : sources) {
            if (source.position() == null) {
                skipped++;
                continue;
            }
            if (source.effectiveRadius() <= 0.0D) {
                skipped++;
                continue;
            }
            if (visualized >= RadiusVisualizationSamples.MAX_VISUALIZED_SOURCES) {
                skipped++;
                continue;
            }

            double radius = Math.min(radiusCeiling, source.effectiveRadius());
            if (radius <= 0.0D) {
                skipped++;
                continue;
            }
            emitSourceParticles(player, source.position(), radius);
            visualized++;
            maxRadiusSeen = Math.max(maxRadiusSeen, radius);
        }

        if (sources.size() > RadiusVisualizationSamples.MAX_VISUALIZED_SOURCES
                && shouldEmitCapWarning(player.getUUID(), gameTime)) {
            WarningBuffer.add(
                    "RADIUS_VISUALIZATION_SOURCE_CAP",
                    "radius_visualization",
                    "Visualization capped at " + RadiusVisualizationSamples.MAX_VISUALIZED_SOURCES + " sources");
        }
        return new RenderSummary(visualized, skipped, maxRadiusSeen);
    }

    private static long resolveNextPulseGameTime(UUID playerUuid, ActiveVisualization activeVisualization, long gameTime) {
        if (activeVisualization != null) {
            return activeVisualization.nextPulseGameTime();
        }
        synchronized (ALWAYS_ON_NEXT_PULSE) {
            return ALWAYS_ON_NEXT_PULSE.getOrDefault(playerUuid, gameTime);
        }
    }

    private static boolean shouldEmitCapWarning(UUID playerUuid, long gameTime) {
        synchronized (LAST_CAP_WARNING_TICK) {
            long previous = LAST_CAP_WARNING_TICK.getOrDefault(playerUuid, Long.MIN_VALUE);
            if (gameTime - previous < CAP_WARNING_COOLDOWN_TICKS) {
                return false;
            }
            LAST_CAP_WARNING_TICK.put(playerUuid, gameTime);
            return true;
        }
    }

    private static void emitSourceParticles(ServerPlayer player, BlockPos sourcePosition, double radius) {
        Vec3 center = Vec3.atCenterOf(sourcePosition);
        List<Vec3> points = RadiusVisualizationSamples.shellPoints(
                center,
                radius,
                RadiusVisualizationSamples.MAX_PARTICLES_PER_SOURCE);
        for (Vec3 point : points) {
            player.serverLevel().sendParticles(
                    ParticleTypes.END_ROD,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private static void updateLastState(ServerPlayer player, long expiresAtGameTime, RenderSummary summary) {
        synchronized (LAST_STATE) {
            LAST_STATE.put(player.getUUID(), new VisualState(
                    Instant.now(),
                    expiresAtGameTime,
                    summary.visualizedSources(),
                    summary.skippedSources(),
                    summary.maxRadiusSeen()));
        }
    }

    public record ShowResult(
            int durationSeconds,
            int visualizedSources,
            int skippedSources,
            double maxRadiusSeen,
            String note) {
    }

    public record Status(
            boolean active,
            long remainingTicks,
            int lastVisualizedSources,
            int lastSkippedSources,
            double maxRadiusSeen,
            boolean alwaysOnFromConfig) {
    }

    private record ActiveVisualization(
            UUID playerUuid,
            String playerName,
            long startedAtGameTime,
            long expiresAtGameTime,
            long nextPulseGameTime) {
    }

    private record VisualState(
            Instant updatedAt,
            long expiresAtGameTime,
            int lastVisualizedSources,
            int lastSkippedSources,
            double maxRadiusSeen) {
    }

    private record RenderSummary(int visualizedSources, int skippedSources, double maxRadiusSeen) {
    }
}
