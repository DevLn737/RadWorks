package dev.radworks.radiation;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WorldFluidDiagnostics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class WorldFluidSourceProvider {
    public static final int MAX_SCAN_RADIUS = 8;
    static final int WORLD_FLUID_DEFAULT_AMOUNT_MB = 1;

    private WorldFluidSourceProvider() {
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            WorldFluidDiagnostics.Builder diagnostics) {
        return PerformanceStats.timeValue("worldFluidScan", () -> collectTimed(player, rules, summary, diagnostics));
    }

    private static List<RadiationSource> collectTimed(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            WorldFluidDiagnostics.Builder diagnostics) {
        List<RadiationSource> sources = new ArrayList<>();
        if (!rules.loaded() || rules.fluidRules() == 0) {
            return sources;
        }

        int scanRadius = effectiveScanRadius(rules);
        if (scanRadius <= 0) {
            return sources;
        }

        Vec3 playerPosition = player.position();
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-scanRadius, -scanRadius, -scanRadius);
        BlockPos max = center.offset(scanRadius, scanRadius, scanRadius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            summary.worldFluidPositionChecked();

            FluidState fluidState = player.serverLevel().getFluidState(pos);
            if (fluidState.isEmpty()) {
                summary.worldFluidSkipped();
                diagnostics.skip(
                        null,
                        null,
                        "none",
                        pos,
                        0,
                        "empty_fluid_state");
                continue;
            }
            summary.worldFluidStateFound();

            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluidState.getType());
            if (fluidId == null) {
                summary.worldFluidSkipped();
                diagnostics.skip(
                        null,
                        null,
                        "none",
                        pos,
                        WORLD_FLUID_DEFAULT_AMOUNT_MB,
                        "invalid_registry_id");
                continue;
            }

            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(player.serverLevel().getBlockState(pos).getBlock());
            double distance = playerPosition.distanceTo(Vec3.atCenterOf(pos));
            var source = sourceForFluidSample(
                    rules,
                    fluidId,
                    blockId,
                    pos.immutable(),
                    WORLD_FLUID_DEFAULT_AMOUNT_MB,
                    distance);
            if (source.isEmpty()) {
                summary.worldFluidSkipped();
                diagnostics.skip(
                        fluidId,
                        null,
                        "none",
                        pos,
                        WORLD_FLUID_DEFAULT_AMOUNT_MB,
                        "no_active_fluid_rule");
                continue;
            }

            RadiationSource matched = source.get();
            if (!matched.activeBecause()) {
                summary.worldFluidSkipped();
                diagnostics.skip(
                        fluidId,
                        resolveMatchedRuleId(rules, fluidId),
                        matched.ruleMatchMode(),
                        pos,
                        WORLD_FLUID_DEFAULT_AMOUNT_MB,
                        DynamicRadiusModel.outsideDynamicRadiusReason());
                continue;
            }

            summary.worldFluidMatch();
            sources.add(matched);
            diagnostics.match(
                    fluidId,
                    resolveMatchedRuleId(rules, fluidId),
                    matched.ruleMatchMode(),
                    pos,
                    WORLD_FLUID_DEFAULT_AMOUNT_MB);
        }

        return List.copyOf(sources);
    }

    static java.util.Optional<RadiationSource> sourceForFluidSample(
            RadiationRules rules,
            ResourceLocation fluidId,
            ResourceLocation blockId,
            BlockPos pos,
            int amountMb,
            double distance) {
        RadiationRules.FluidRuleMatch ruleMatch = rules.resolveFluidRule(fluidId).orElse(null);
        if (ruleMatch == null) {
            return java.util.Optional.empty();
        }
        RadiationRule rule = ruleMatch.rule();
        RadiationSource source = RadiationSource.worldFluid(
                fluidId,
                blockId,
                pos,
                amountMb,
                rule.strength(),
                rule.radius(),
                distance,
                rule.respectsShielding(),
                ruleMatch.mode(),
                "World fluid source matched "
                        + ruleMatch.mode()
                        + " fluid rule id="
                        + ruleMatch.matchedRuleId()
                        + " observedFluidId="
                        + fluidId
                        + " amountMb="
                        + amountMb);
        return java.util.Optional.of(source);
    }

    private static ResourceLocation resolveMatchedRuleId(RadiationRules rules, ResourceLocation fluidId) {
        RadiationRules.FluidRuleMatch match = rules.resolveFluidRule(fluidId).orElse(null);
        return match == null ? null : match.matchedRuleId();
    }

    private static int effectiveScanRadius(RadiationRules rules) {
        return (int) Math.ceil(Math.min(rules.maxActiveFluidRuleRadius(), MAX_SCAN_RADIUS));
    }
}
