package dev.radworks.gameplay;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.radiation.ExposureBreakdown;
import dev.radworks.radiation.ExposureEngine;
import dev.radworks.radiation.RadiationRulesLoader;
import dev.radworks.radiation.RadiationTargetContext;
import dev.radworks.radiation.RadiationTargetKind;
import dev.radworks.radiation.effects.EffectMode;
import dev.radworks.radiation.effects.EffectStrategyService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class RadiationGameplayService {
    private static final int MAX_DECISIONS = 20;
    private static final int MAX_LIVING_DECISIONS = 50;
    private static final String ARMOR_POLICY = "player_only_beta_0_4_3";
    private static final String SHIELDING_POLICY = "target_aware_beta_0_4_4";

    private static final Map<UUID, Long> NEXT_ELIGIBLE_TICK = new LinkedHashMap<>();
    private static final Map<UUID, Long> NEXT_LIVING_ELIGIBLE_TICK = new LinkedHashMap<>();
    private static final Map<UUID, AutoApplyDecision> LAST_DECISIONS = new LinkedHashMap<>();
    private static final List<LivingEntityEffectDecision> LIVING_DECISIONS = new ArrayList<>();

    private static long livingTargetsChecked;
    private static long livingTargetsEligible;
    private static long livingTargetsSkipped;
    private static long livingTargetsEffectApplied;
    private static long livingTargetsCapped;

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
        notes.add("Target-aware shielding for living entities is enabled in Beta 0.4.4.");
        json.add("notes", notes);

        JsonArray decisions = new JsonArray();
        synchronized (LAST_DECISIONS) {
            for (AutoApplyDecision decision : LAST_DECISIONS.values()) {
                decisions.add(decision.toJson());
            }
        }
        json.add("lastAutoApplyDecisions", decisions);

        JsonArray livingDecisions = new JsonArray();
        synchronized (LIVING_DECISIONS) {
            for (LivingEntityEffectDecision decision : LIVING_DECISIONS) {
                livingDecisions.add(decision.toJson());
            }
        }
        json.add("livingEntityEffectDecisions", livingDecisions);

        JsonObject counters = new JsonObject();
        counters.addProperty("livingTargetsChecked", livingTargetsChecked);
        counters.addProperty("livingTargetsEligible", livingTargetsEligible);
        counters.addProperty("livingTargetsSkipped", livingTargetsSkipped);
        counters.addProperty("livingTargetsEffectApplied", livingTargetsEffectApplied);
        counters.addProperty("livingTargetsCapped", livingTargetsCapped);
        json.add("livingTargetCounters", counters);
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
            store(evaluateAndApplyPlayer(player, gameTime, gameTime + scanIntervalTicks, scanIntervalTicks));
        } catch (RuntimeException exception) {
            store(AutoApplyDecision.error(player, gameTime, gameTime + scanIntervalTicks, scanIntervalTicks, exception));
        }

        processNearbyLivingTargets(player, gameTime, scanIntervalTicks);
    }

    private static AutoApplyDecision evaluateAndApplyPlayer(
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
        if (!RadWorksConfig.applyEffectToPlayers()) {
            return AutoApplyDecision.skipped(player, gameTime, nextEligibleTick, scanIntervalTicks, "apply_to_players_disabled");
        }
        EffectStrategyService.RuntimeEffectSelection runtime = EffectStrategyService.resolveRuntimeSelection();
        if (runtime.effectMode() == EffectMode.DISABLED) {
            return AutoApplyDecision.skipped(player, gameTime, nextEligibleTick, scanIntervalTicks, "effect_mode_disabled", runtime);
        }
        if (runtime.selectedRuntimeEffectId() == null) {
            return AutoApplyDecision.skipped(player, gameTime, nextEligibleTick, scanIntervalTicks, "selected_effect_missing", runtime);
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
            applied = player.addEffect(new MobEffectInstance(runtimeEffect, RadWorksConfig.effectDurationTicks(), 0));
        }
        return AutoApplyDecision.fromExposure(
                player,
                gameTime,
                nextEligibleTick,
                scanIntervalTicks,
                applied ? "applied" : "selected_effect_missing",
                breakdown,
                threshold,
                applied,
                runtime);
    }

    private static void processNearbyLivingTargets(ServerPlayer player, long gameTime, int scanIntervalTicks) {
        if (!RadWorksConfig.gameplayEnabled()
                || !RadWorksConfig.autoApplyEffect()
                || !RadWorksConfig.applyEffectToLivingEntities()) {
            return;
        }
        if (!RadiationRulesLoader.currentRules().loaded()) {
            return;
        }

        int radius = Math.max(1, RadWorksConfig.livingTargetScanRadius());
        int maxTargets = Math.max(1, RadWorksConfig.maxLivingTargetsPerScan());
        AABB bounds = player.getBoundingBox().inflate(radius);

        List<LivingEntity> nearby = player.serverLevel()
                .getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity != null && entity.isAlive());
        nearby.sort(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(player.position())));

        int eligible = 0;
        int checked = 0;
        for (LivingEntity target : nearby) {
            if (target == player) {
                continue;
            }
            String selectionSkipReason = LivingTargetSelectionPolicy.skipReason(
                    target,
                    RadWorksConfig.applyEffectToMobs(),
                    RadWorksConfig.applyEffectToArmorStands());
            if (selectionSkipReason != null) {
                livingTargetsSkipped++;
                storeLivingDecision(LivingEntityEffectDecision.capped(target, gameTime, scanIntervalTicks));
                continue;
            }

            checked++;
            if (LivingTargetSelectionPolicy.isCapped(eligible, maxTargets)) {
                livingTargetsCapped++;
                livingTargetsSkipped++;
                storeLivingDecision(LivingEntityEffectDecision.capped(target, gameTime, scanIntervalTicks));
                continue;
            }
            eligible++;
            evaluateAndApplyLivingTarget(target, gameTime, scanIntervalTicks);
        }

        livingTargetsChecked += checked;
        livingTargetsEligible += eligible;
    }

    private static void evaluateAndApplyLivingTarget(LivingEntity target, long gameTime, int scanIntervalTicks) {
        UUID uuid = target.getUUID();
        Long nextEligibleTick = NEXT_LIVING_ELIGIBLE_TICK.get(uuid);
        if (nextEligibleTick != null && gameTime < nextEligibleTick) {
            livingTargetsSkipped++;
            return;
        }
        NEXT_LIVING_ELIGIBLE_TICK.put(uuid, gameTime + scanIntervalTicks);

        try {
            EffectStrategyService.RuntimeEffectSelection runtime = EffectStrategyService.resolveRuntimeSelection();
            if (runtime.effectMode() == EffectMode.DISABLED || runtime.selectedRuntimeEffectId() == null) {
                livingTargetsSkipped++;
                storeLivingDecision(LivingEntityEffectDecision.skipped(
                        target,
                        gameTime,
                        scanIntervalTicks,
                        runtime.effectMode() == EffectMode.DISABLED ? "effect_mode_disabled" : "selected_effect_missing",
                        runtime,
                        0.0D,
                        0,
                        ShieldingExposureMetrics.empty(),
                        false));
                return;
            }

            boolean shieldingForLiving = RadWorksConfig.applyShieldingToLivingEntities();
            RadiationTargetContext context = RadiationTargetContext.forLivingEntity(
                    (net.minecraft.server.level.ServerLevel) target.level(),
                    target,
                    false,
                    true,
                    shieldingForLiving);
            ExposureEngine.TargetExposure exposure = ExposureEngine.calculateForTarget(context, RadiationRulesLoader.currentRules());
            ShieldingExposureMetrics shieldingMetrics = ShieldingExposureMetrics.from(exposure.sources());
            double threshold = RadWorksConfig.exposureThreshold();
            LivingEntityEffectDecisionPolicy.Decision decision = LivingEntityEffectDecisionPolicy.evaluate(
                    runtime.effectMode(),
                    runtime.selectedRuntimeEffectId(),
                    runtime.selectedRuntimeEffectRegistered(),
                    shieldingMetrics.totalFinalExposure(),
                    threshold);
            if (!decision.shouldAttemptApply()) {
                livingTargetsSkipped++;
                storeLivingDecision(LivingEntityEffectDecision.skipped(
                        target,
                        gameTime,
                        scanIntervalTicks,
                        decision.reason(),
                        runtime,
                        shieldingMetrics.totalFinalExposure(),
                        exposure.sourceCount(),
                        shieldingMetrics,
                        shieldingForLiving));
                return;
            }

            Holder<MobEffect> runtimeEffect = BuiltInRegistries.MOB_EFFECT
                    .getHolder(ResourceLocation.parse(runtime.selectedRuntimeEffectId()))
                    .orElse(null);
            if (runtimeEffect == null) {
                livingTargetsSkipped++;
                storeLivingDecision(LivingEntityEffectDecision.skipped(
                        target,
                        gameTime,
                        scanIntervalTicks,
                        "selected_effect_missing",
                        runtime,
                        shieldingMetrics.totalFinalExposure(),
                        exposure.sourceCount(),
                        shieldingMetrics,
                        shieldingForLiving));
                return;
            }

            boolean applied = target.addEffect(new MobEffectInstance(runtimeEffect, RadWorksConfig.effectDurationTicks(), 0));
            if (applied) {
                livingTargetsEffectApplied++;
            } else {
                livingTargetsSkipped++;
            }
            storeLivingDecision(LivingEntityEffectDecision.fromExposure(
                    target,
                    gameTime,
                    scanIntervalTicks,
                    applied ? "applied" : "target_skipped",
                    runtime,
                    shieldingMetrics.totalFinalExposure(),
                    exposure.sourceCount(),
                    applied,
                    shieldingMetrics,
                    shieldingForLiving));
        } catch (RuntimeException exception) {
            livingTargetsSkipped++;
            storeLivingDecision(LivingEntityEffectDecision.error(target, gameTime, scanIntervalTicks, exception));
        }
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

    private static void storeLivingDecision(LivingEntityEffectDecision decision) {
        synchronized (LIVING_DECISIONS) {
            LIVING_DECISIONS.add(decision);
            while (LIVING_DECISIONS.size() > MAX_LIVING_DECISIONS) {
                LIVING_DECISIONS.remove(0);
            }
        }
    }

    private static AutoApplyDecision lastDecision(UUID playerUuid) {
        synchronized (LAST_DECISIONS) {
            return LAST_DECISIONS.get(playerUuid);
        }
    }

    private record ShieldingExposureMetrics(
            double totalRawExposure,
            double totalFinalExposure,
            int shieldingAppliedSources,
            int shieldingReducedSources,
            int shieldingSkippedSources) {
        static ShieldingExposureMetrics from(List<dev.radworks.radiation.RadiationSource> sources) {
            double raw = 0.0D;
            double fin = 0.0D;
            int applied = 0;
            int reduced = 0;
            int skipped = 0;
            for (dev.radworks.radiation.RadiationSource source : sources) {
                raw += source.rawContribution();
                fin += source.finalContribution();
                if ("not_applicable".equals(source.shielding())) {
                    skipped++;
                } else {
                    applied++;
                    if ("reduced".equals(source.shielding())) {
                        reduced++;
                    }
                }
            }
            return new ShieldingExposureMetrics(raw, fin, applied, reduced, skipped);
        }

        static ShieldingExposureMetrics empty() {
            return new ShieldingExposureMetrics(0.0D, 0.0D, 0, 0, 0);
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

    public record LivingEntityEffectDecision(
            Instant createdAt,
            long gameTime,
            UUID targetEntityUuid,
            String targetEntityId,
            String targetEntityType,
            String targetName,
            String targetKind,
            double totalExposure,
            double totalRawExposure,
            double totalFinalExposure,
            double threshold,
            int sourceCount,
            int shieldingAppliedSources,
            int shieldingReducedSources,
            int shieldingSkippedSources,
            boolean wouldApply,
            boolean appliedEffect,
            String reason,
            int scanIntervalTicks,
            String selectedRuntimeEffectId,
            boolean selectedRuntimeEffectRegistered,
            String effectMode,
            boolean shieldingEvaluated,
            String shieldingPolicy,
            boolean armorEvaluated,
            String armorPolicy,
            String errorMessage) {
        static LivingEntityEffectDecision capped(LivingEntity target, long gameTime, int scanIntervalTicks) {
            return base(
                    target,
                    gameTime,
                    scanIntervalTicks,
                    "target_skipped",
                    EffectStrategyService.resolveRuntimeSelection(),
                    0.0D,
                    0,
                    ShieldingExposureMetrics.empty(),
                    false,
                    false,
                    false,
                    null);
        }

        static LivingEntityEffectDecision skipped(
                LivingEntity target,
                long gameTime,
                int scanIntervalTicks,
                String reason,
                EffectStrategyService.RuntimeEffectSelection runtime,
                double totalExposure,
                int sourceCount,
                ShieldingExposureMetrics shieldingMetrics,
                boolean shieldingEvaluated) {
            return base(
                    target,
                    gameTime,
                    scanIntervalTicks,
                    reason,
                    runtime,
                    totalExposure,
                    sourceCount,
                    shieldingMetrics,
                    shieldingEvaluated,
                    false,
                    false,
                    null);
        }

        static LivingEntityEffectDecision fromExposure(
                LivingEntity target,
                long gameTime,
                int scanIntervalTicks,
                String reason,
                EffectStrategyService.RuntimeEffectSelection runtime,
                double totalExposure,
                int sourceCount,
                boolean appliedEffect,
                ShieldingExposureMetrics shieldingMetrics,
                boolean shieldingEvaluated) {
            return base(
                    target,
                    gameTime,
                    scanIntervalTicks,
                    reason,
                    runtime,
                    totalExposure,
                    sourceCount,
                    shieldingMetrics,
                    shieldingEvaluated,
                    true,
                    appliedEffect,
                    null);
        }

        static LivingEntityEffectDecision error(
                LivingEntity target,
                long gameTime,
                int scanIntervalTicks,
                RuntimeException exception) {
            return base(
                    target,
                    gameTime,
                    scanIntervalTicks,
                    "error",
                    EffectStrategyService.resolveRuntimeSelection(),
                    0.0D,
                    0,
                    ShieldingExposureMetrics.empty(),
                    false,
                    false,
                    false,
                    exception.getMessage());
        }

        private static LivingEntityEffectDecision base(
                LivingEntity target,
                long gameTime,
                int scanIntervalTicks,
                String reason,
                EffectStrategyService.RuntimeEffectSelection runtime,
                double totalExposure,
                int sourceCount,
                ShieldingExposureMetrics shieldingMetrics,
                boolean shieldingEvaluated,
                boolean wouldApply,
                boolean appliedEffect,
                String errorMessage) {
            String targetName = target.getName() == null ? null : target.getName().getString();
            return new LivingEntityEffectDecision(
                    Instant.now(),
                    gameTime,
                    target.getUUID(),
                    target.getStringUUID(),
                    BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString(),
                    targetName,
                    RadiationTargetContext.forLivingEntity((net.minecraft.server.level.ServerLevel) target.level(), target, false, true, false)
                            .targetKind()
                            .id(),
                    totalExposure,
                    shieldingMetrics.totalRawExposure(),
                    shieldingMetrics.totalFinalExposure(),
                    RadWorksConfig.exposureThreshold(),
                    sourceCount,
                    shieldingMetrics.shieldingAppliedSources(),
                    shieldingMetrics.shieldingReducedSources(),
                    shieldingMetrics.shieldingSkippedSources(),
                    wouldApply,
                    appliedEffect,
                    reason,
                    scanIntervalTicks,
                    runtime.selectedRuntimeEffectId(),
                    runtime.selectedRuntimeEffectRegistered(),
                    runtime.effectMode().id(),
                    shieldingEvaluated,
                    SHIELDING_POLICY,
                    false,
                    ARMOR_POLICY,
                    errorMessage);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("createdAt", createdAt.toString());
            json.addProperty("gameTime", gameTime);
            json.addProperty("targetEntityUuid", targetEntityUuid.toString());
            json.addProperty("targetEntityId", targetEntityId);
            json.addProperty("targetEntityType", targetEntityType);
            if (targetName != null && !targetName.isBlank()) {
                json.addProperty("targetName", targetName);
            }
            json.addProperty("targetKind", targetKind);
            json.addProperty("totalExposure", totalExposure);
            json.addProperty("totalRawExposure", totalRawExposure);
            json.addProperty("totalFinalExposure", totalFinalExposure);
            json.addProperty("threshold", threshold);
            json.addProperty("sourceCount", sourceCount);
            json.addProperty("shieldingAppliedSources", shieldingAppliedSources);
            json.addProperty("shieldingReducedSources", shieldingReducedSources);
            json.addProperty("shieldingSkippedSources", shieldingSkippedSources);
            json.addProperty("wouldApply", wouldApply);
            json.addProperty("appliedEffect", appliedEffect);
            json.addProperty("reason", reason);
            json.addProperty("scanIntervalTicks", scanIntervalTicks);
            if (selectedRuntimeEffectId != null) {
                json.addProperty("selectedRuntimeEffectId", selectedRuntimeEffectId);
            }
            json.addProperty("selectedRuntimeEffectRegistered", selectedRuntimeEffectRegistered);
            json.addProperty("effectMode", effectMode);
            json.addProperty("shieldingEvaluated", shieldingEvaluated);
            json.addProperty("shieldingPolicy", shieldingPolicy);
            json.addProperty("armorEvaluated", armorEvaluated);
            json.addProperty("armorPolicy", armorPolicy);
            if (errorMessage != null) {
                json.addProperty("errorMessage", errorMessage);
            }
            return json;
        }
    }
}
