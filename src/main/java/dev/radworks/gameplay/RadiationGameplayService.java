package dev.radworks.gameplay;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.radiation.ExposureBreakdown;
import dev.radworks.radiation.ExposureEngine;
import dev.radworks.radiation.RadiationRulesLoader;
import dev.radworks.radiation.effects.EffectStrategyService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class RadiationGameplayService {
    private static final int MAX_DECISIONS = 20;
    private static final Map<UUID, Long> NEXT_ELIGIBLE_TICK = new LinkedHashMap<>();
    private static final Map<UUID, AutoApplyDecision> LAST_DECISIONS = new LinkedHashMap<>();

    private RadiationGameplayService() {
    }

    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        PerformanceStats.timeValue("gameplay_auto_apply", () -> {
            processPlayer(player);
            return Boolean.TRUE;
        });
    }

    public static JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.add("config", RadWorksConfig.toJson());

        JsonArray notes = new JsonArray();
        notes.add("Beta gameplay auto-apply uses config-driven runtime effect selection.");
        notes.add("Damage/exhaustion are disabled and not implemented in this beta.");
        json.add("notes", notes);

        JsonArray decisions = new JsonArray();
        synchronized (LAST_DECISIONS) {
            for (AutoApplyDecision decision : LAST_DECISIONS.values()) {
                decisions.add(decision.toJson());
            }
        }
        json.add("lastAutoApplyDecisions", decisions);
        return json;
    }

    public static String compactStatus(ServerPlayer player) {
        AutoApplyDecision decision = lastDecision(player.getUUID());
        return "autoApplyEffect="
                + RadWorksConfig.autoApplyEffect()
                + " gameplayEnabled="
                + RadWorksConfig.gameplayEnabled()
                + " effectMode="
                + RadWorksConfig.effectMode().id()
                + " threshold="
                + RadWorksConfig.exposureThreshold()
                + " scanIntervalTicks="
                + RadWorksConfig.scanIntervalTicks()
                + " selectedRuntimeEffect="
                + (decision == null ? "unknown" : decision.selectedRuntimeEffectId())
                + " lastDecision="
                + (decision == null ? "none" : decision.reason());
    }

    private static void processPlayer(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        int scanIntervalTicks = Math.max(1, RadWorksConfig.scanIntervalTicks());
        UUID playerUuid = player.getUUID();
        Long nextEligibleTick = NEXT_ELIGIBLE_TICK.get(playerUuid);

        if (nextEligibleTick != null && gameTime < nextEligibleTick) {
            return;
        }
        NEXT_ELIGIBLE_TICK.put(playerUuid, gameTime + scanIntervalTicks);

        try {
            store(evaluateAndApply(player, gameTime, gameTime + scanIntervalTicks, scanIntervalTicks));
        } catch (RuntimeException exception) {
            store(AutoApplyDecision.error(player, gameTime, gameTime + scanIntervalTicks, scanIntervalTicks, exception));
        }
    }

    private static AutoApplyDecision evaluateAndApply(
            ServerPlayer player,
            long gameTime,
            long nextEligibleTick,
            int scanIntervalTicks) {
        if (!RadWorksConfig.gameplayEnabled()) {
            return AutoApplyDecision.skipped(player, gameTime, nextEligibleTick, scanIntervalTicks, "disabled");
        }
        if (!RadWorksConfig.autoApplyEffect()) {
            return AutoApplyDecision.skipped(player, gameTime, nextEligibleTick, scanIntervalTicks, "auto_apply_disabled");
        }
        EffectStrategyService.RuntimeEffectSelection runtime = EffectStrategyService.resolveRuntimeSelection();
        if (runtime.selectedRuntimeEffectId() == null) {
            return AutoApplyDecision.skipped(player, gameTime, nextEligibleTick, scanIntervalTicks, runtime.fallbackReason(), runtime);
        }
        if (!RadiationRulesLoader.currentRules().loaded()) {
            return AutoApplyDecision.skipped(player, gameTime, nextEligibleTick, scanIntervalTicks, "no_rules", runtime);
        }

        ExposureBreakdown breakdown = ExposureEngine.calculate(player);
        double threshold = RadWorksConfig.exposureThreshold();
        String armorStatus = breakdown.armorProtection().status();
        if ("full".equals(armorStatus)) {
            return AutoApplyDecision.fromExposure(
                    player,
                    gameTime,
                    nextEligibleTick,
                    scanIntervalTicks,
                    "blocked_by_full_armor",
                    breakdown,
                    threshold,
                    false,
                    runtime);
        }
        if (breakdown.totalExposure() < threshold) {
            return AutoApplyDecision.fromExposure(
                    player,
                    gameTime,
                    nextEligibleTick,
                    scanIntervalTicks,
                    "below_threshold",
                    breakdown,
                    threshold,
                    false,
                    runtime);
        }

        Holder<MobEffect> runtimeEffect = BuiltInRegistries.MOB_EFFECT
                .getHolder(ResourceLocation.parse(runtime.selectedRuntimeEffectId()))
                .orElse(null);
        boolean applied = false;
        if (runtimeEffect != null) {
            applied = player.addEffect(new MobEffectInstance(
                    runtimeEffect,
                    RadWorksConfig.effectDurationTicks(),
                    0));
        }
        return AutoApplyDecision.fromExposure(
                player,
                gameTime,
                nextEligibleTick,
                scanIntervalTicks,
                applied ? "applied" : "runtime_effect_missing",
                breakdown,
                threshold,
                applied,
                runtime);
    }

    private static void store(AutoApplyDecision decision) {
        synchronized (LAST_DECISIONS) {
            LAST_DECISIONS.put(decision.playerUuid(), decision);
            while (LAST_DECISIONS.size() > MAX_DECISIONS) {
                UUID first = LAST_DECISIONS.keySet().iterator().next();
                LAST_DECISIONS.remove(first);
            }
        }
    }

    private static AutoApplyDecision lastDecision(UUID playerUuid) {
        synchronized (LAST_DECISIONS) {
            return LAST_DECISIONS.get(playerUuid);
        }
    }

    public record AutoApplyDecision(
            Instant createdAt,
            long gameTime,
            long nextEligibleTick,
            UUID playerUuid,
            String playerName,
            String reason,
            double totalExposure,
            String armorStatus,
            double threshold,
            boolean appliedEffect,
            int scanIntervalTicks,
            String effectMode,
            String selectedRuntimeEffectId,
            boolean selectedRuntimeEffectRegistered,
            String fallbackReason,
            String errorMessage) {
        private static AutoApplyDecision waiting(
                ServerPlayer player,
                long gameTime,
                long nextEligibleTick,
                int scanIntervalTicks) {
            return base(
                    player,
                    gameTime,
                    nextEligibleTick,
                    scanIntervalTicks,
                    "waiting_for_interval",
                    0.0D,
                    "unknown",
                    false,
                    EffectStrategyService.resolveRuntimeSelection(),
                    null);
        }

        private static AutoApplyDecision skipped(
                ServerPlayer player,
                long gameTime,
                long nextEligibleTick,
                int scanIntervalTicks,
                String reason) {
            return skipped(
                    player,
                    gameTime,
                    nextEligibleTick,
                    scanIntervalTicks,
                    reason,
                    EffectStrategyService.resolveRuntimeSelection());
        }

        private static AutoApplyDecision skipped(
                ServerPlayer player,
                long gameTime,
                long nextEligibleTick,
                int scanIntervalTicks,
                String reason,
                EffectStrategyService.RuntimeEffectSelection runtime) {
            return base(player, gameTime, nextEligibleTick, scanIntervalTicks, reason, 0.0D, "unknown", false, runtime, null);
        }

        private static AutoApplyDecision fromExposure(
                ServerPlayer player,
                long gameTime,
                long nextEligibleTick,
                int scanIntervalTicks,
                String reason,
                ExposureBreakdown breakdown,
                double threshold,
                boolean appliedEffect,
                EffectStrategyService.RuntimeEffectSelection runtime) {
            return new AutoApplyDecision(
                    Instant.now(),
                    gameTime,
                    nextEligibleTick,
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    reason,
                    breakdown.totalExposure(),
                    breakdown.armorProtection().status(),
                    threshold,
                    appliedEffect,
                    scanIntervalTicks,
                    runtime.effectMode().id(),
                    runtime.selectedRuntimeEffectId(),
                    runtime.selectedRuntimeEffectRegistered(),
                    runtime.fallbackReason(),
                    null);
        }

        private static AutoApplyDecision error(
                ServerPlayer player,
                long gameTime,
                long nextEligibleTick,
                int scanIntervalTicks,
                RuntimeException exception) {
            return base(
                    player,
                    gameTime,
                    nextEligibleTick,
                    scanIntervalTicks,
                    "error",
                    0.0D,
                    "unknown",
                    false,
                    EffectStrategyService.resolveRuntimeSelection(),
                    exception.getMessage());
        }

        private static AutoApplyDecision base(
                ServerPlayer player,
                long gameTime,
                long nextEligibleTick,
                int scanIntervalTicks,
                String reason,
                double totalExposure,
                String armorStatus,
                boolean appliedEffect,
                EffectStrategyService.RuntimeEffectSelection runtime,
                String errorMessage) {
            return new AutoApplyDecision(
                    Instant.now(),
                    gameTime,
                    nextEligibleTick,
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    reason,
                    totalExposure,
                    armorStatus,
                    RadWorksConfig.exposureThreshold(),
                    appliedEffect,
                    scanIntervalTicks,
                    runtime.effectMode().id(),
                    runtime.selectedRuntimeEffectId(),
                    runtime.selectedRuntimeEffectRegistered(),
                    runtime.fallbackReason(),
                    errorMessage);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("createdAt", createdAt.toString());
            json.addProperty("gameTime", gameTime);
            json.addProperty("nextEligibleTick", nextEligibleTick);
            json.addProperty("playerUuid", playerUuid.toString());
            json.addProperty("playerName", playerName);
            json.addProperty("reason", reason);
            json.addProperty("totalExposure", totalExposure);
            json.addProperty("armorStatus", armorStatus);
            json.addProperty("threshold", threshold);
            json.addProperty("appliedEffect", appliedEffect);
            json.addProperty("scanIntervalTicks", scanIntervalTicks);
            json.addProperty("effectMode", effectMode);
            if (selectedRuntimeEffectId != null) {
                json.addProperty("selectedRuntimeEffectId", selectedRuntimeEffectId);
            }
            json.addProperty("selectedRuntimeEffectRegistered", selectedRuntimeEffectRegistered);
            json.addProperty("fallbackReason", fallbackReason);
            if (errorMessage != null) {
                json.addProperty("errorMessage", errorMessage);
            }
            return json;
        }
    }
}
