package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WorldFluidDiagnostics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class WorldFluidSourceProvider {
    public static final int MAX_SCAN_RADIUS_CAP = 32;
    static final int WORLD_FLUID_FULL_BLOCK_AMOUNT_MB = 1000;

    private WorldFluidSourceProvider() {
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            WorldFluidDiagnostics.Builder diagnostics) {
        return collect(
                player.serverLevel(),
                player.position(),
                player.blockPosition(),
                rules,
                summary,
                diagnostics,
                ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos targetCenter,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            WorldFluidDiagnostics.Builder diagnostics) {
        return collect(level, targetPosition, targetCenter, rules, summary, diagnostics, ForceSourceCandidateSink.NO_OP);
    }

    public static List<RadiationSource> collect(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos targetCenter,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            WorldFluidDiagnostics.Builder diagnostics,
            ForceSourceCandidateSink candidateSink) {
        return PerformanceStats.timeValue(
                "worldFluidScan",
                () -> collectTimed(level, targetPosition, targetCenter, rules, summary, diagnostics, candidateSink));
    }

    private static List<RadiationSource> collectTimed(
            ServerLevel level,
            Vec3 targetPosition,
            BlockPos targetCenter,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            WorldFluidDiagnostics.Builder diagnostics,
            ForceSourceCandidateSink candidateSink) {
        if (!rules.loaded() || rules.fluidRules() == 0) {
            return List.of();
        }

        int discoveryRadius = effectiveDiscoveryRadius();
        summary.worldFluidDiscoveryRadius(discoveryRadius);
        diagnostics.worldFluidDiscoveryRadius(discoveryRadius);

        BlockPos min = targetCenter.offset(-discoveryRadius, -discoveryRadius, -discoveryRadius);
        BlockPos max = targetCenter.offset(discoveryRadius, discoveryRadius, discoveryRadius);
        List<FluidSample> samples = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            summary.worldFluidPositionChecked();

            FluidState fluidState = level.getFluidState(pos);
            if (fluidState.isEmpty()) {
                summary.worldFluidSkipped();
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
                        WORLD_FLUID_FULL_BLOCK_AMOUNT_MB,
                        "invalid_registry_id");
                continue;
            }

            RadiationRules.FluidRuleMatch ruleMatch = rules.resolveFluidRule(fluidId).orElse(null);
            if (ruleMatch == null) {
                summary.worldFluidSkipped();
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
                double distance = targetPosition.distanceTo(Vec3.atCenterOf(pos));
                candidateSink.observe(new ForceSourceCandidate(
                        ForceSourceCandidate.CandidateKind.FLUID,
                        RadiationSourceType.WORLD_FLUID,
                        blockId,
                        null,
                        fluidId,
                        pos.immutable(),
                        null,
                        null,
                        null,
                        null,
                        blockId,
                        null,
                        0,
                        WORLD_FLUID_FULL_BLOCK_AMOUNT_MB,
                        distance,
                        true,
                        false,
                        0,
                        null,
                        "world_fluid_observed_without_fluid_rule"));
                diagnostics.skip(
                        fluidId,
                        null,
                        "none",
                        pos,
                        WORLD_FLUID_FULL_BLOCK_AMOUNT_MB,
                        "no_active_fluid_rule");
                continue;
            }

            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            ResourceLocation normalizedFluidId = normalizeFluidIdForCluster(fluidId);
            samples.add(new FluidSample(
                    pos.immutable(),
                    fluidId,
                    normalizedFluidId,
                    blockId,
                    ruleMatch.rule(),
                    ruleMatch.matchedRuleId(),
                    ruleMatch.mode()));
        }

        return collectFromSamples(
                targetPosition,
                rules,
                samples,
                summary,
                diagnostics,
                discoveryRadius,
                targetCenter,
                candidateSink);
    }

    static List<RadiationSource> collectFromSamples(
            Vec3 playerPosition,
            RadiationRules rules,
            List<FluidSample> samples,
            SourceScanSummary.Builder summary,
            WorldFluidDiagnostics.Builder diagnostics,
            int discoveryRadius,
            BlockPos discoveryCenter) {
        return collectFromSamples(
                playerPosition,
                rules,
                samples,
                summary,
                diagnostics,
                discoveryRadius,
                discoveryCenter,
                ForceSourceCandidateSink.NO_OP);
    }

    static List<RadiationSource> collectFromSamples(
            Vec3 playerPosition,
            RadiationRules rules,
            List<FluidSample> samples,
            SourceScanSummary.Builder summary,
            WorldFluidDiagnostics.Builder diagnostics,
            int discoveryRadius,
            BlockPos discoveryCenter,
            ForceSourceCandidateSink candidateSink) {
        if (!rules.loaded() || rules.fluidRules() == 0 || samples.isEmpty()) {
            return List.of();
        }

        Map<BlockPos, FluidSample> sampleByPos = new HashMap<>();
        for (FluidSample sample : samples) {
            sampleByPos.put(sample.position(), sample);
        }

        BlockPos min = discoveryCenter.offset(-discoveryRadius, -discoveryRadius, -discoveryRadius);
        BlockPos max = discoveryCenter.offset(discoveryRadius, discoveryRadius, discoveryRadius);

        List<RadiationSource> sources = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        int clusterIndex = 0;
        for (FluidSample seed : samples) {
            if (visited.contains(seed.position())) {
                continue;
            }

            Cluster cluster = buildCluster(seed, sampleByPos, visited, min, max);
            summary.worldFluidClusterBuilt();
            clusterIndex++;

            int aggregateAmountMb = cluster.contributingFluidBlocks * WORLD_FLUID_FULL_BLOCK_AMOUNT_MB;
            FluidSample nearest = nearestSample(cluster.members, playerPosition);
            double distance = playerPosition.distanceTo(Vec3.atCenterOf(nearest.position()));
            String clusterRuleMatchMode = clusterRuleMatchMode(cluster.ruleMatchModes);
            RuleSelection selection = chooseRuleForCluster(cluster, rules);

            RadiationSource matched = RadiationSource.worldFluid(
                    selection.primaryFluidId(),
                    nearest.blockId(),
                    nearest.position(),
                    aggregateAmountMb,
                    cluster.contributingFluidBlocks,
                    selection.rule().strength(),
                    selection.rule().radius(),
                    distance,
                    selection.rule().respectsShielding(),
                    clusterRuleMatchMode,
                    "World fluid cluster matched normalizedFluidId="
                            + cluster.normalizedFluidId
                            + " matchedRuleId="
                            + selection.matchedRuleId()
                            + " observedFluidIds="
                            + cluster.observedFluidIds
                            + " contributingFluidBlocks="
                            + cluster.contributingFluidBlocks
                            + " aggregateAmountMb="
                            + aggregateAmountMb
                            + " maybeClippedByDiscoveryRadius="
                            + cluster.maybeClippedByDiscoveryRadius);

            if (!matched.activeBecause()) {
                summary.worldFluidSkipped();
                candidateSink.observe(new ForceSourceCandidate(
                        ForceSourceCandidate.CandidateKind.FLUID,
                        RadiationSourceType.WORLD_FLUID,
                        nearest.blockId(),
                        null,
                        selection.primaryFluidId(),
                        nearest.position(),
                        null,
                        null,
                        null,
                        null,
                        nearest.blockId(),
                        null,
                        0,
                        aggregateAmountMb,
                        distance,
                        selection.rule().respectsShielding(),
                        false,
                        0,
                        null,
                        DynamicRadiusModel.outsideDynamicRadiusReason()));
                diagnostics.skip(
                        selection.primaryFluidId(),
                        selection.matchedRuleId(),
                        clusterRuleMatchMode,
                        nearest.position(),
                        aggregateAmountMb,
                        DynamicRadiusModel.outsideDynamicRadiusReason());
                diagnostics.clusterSample(
                        clusterIndex,
                        cluster.normalizedFluidId,
                        selection.matchedRuleId(),
                        clusterRuleMatchMode,
                        cluster.observedFluidIds,
                        cluster.contributingFluidBlocks,
                        aggregateAmountMb,
                        cluster.minPos,
                        cluster.maxPos,
                        seed.position(),
                        nearest.position(),
                        distance,
                        matched.effectiveRadius(),
                        false,
                        DynamicRadiusModel.outsideDynamicRadiusReason(),
                        cluster.maybeClippedByDiscoveryRadius);
                continue;
            }

            summary.worldFluidMatch();
            sources.add(matched);
            diagnostics.match(
                    selection.primaryFluidId(),
                    selection.matchedRuleId(),
                    clusterRuleMatchMode,
                    nearest.position(),
                    aggregateAmountMb);
            diagnostics.clusterSample(
                    clusterIndex,
                    cluster.normalizedFluidId,
                    selection.matchedRuleId(),
                    clusterRuleMatchMode,
                    cluster.observedFluidIds,
                    cluster.contributingFluidBlocks,
                    aggregateAmountMb,
                    cluster.minPos,
                    cluster.maxPos,
                    seed.position(),
                    nearest.position(),
                    distance,
                    matched.effectiveRadius(),
                    true,
                    "cluster_active",
                    cluster.maybeClippedByDiscoveryRadius);
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
                Math.max(1, amountMb / WORLD_FLUID_FULL_BLOCK_AMOUNT_MB),
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

    static ResourceLocation normalizeFluidIdForCluster(ResourceLocation fluidId) {
        String path = fluidId.getPath();
        if (path.startsWith("flowing_") && path.length() > "flowing_".length()) {
            return fluidId.withPath(path.substring("flowing_".length()));
        }
        return fluidId;
    }

    static int scanVolumeForRadius(int radius) {
        int diameter = radius * 2 + 1;
        return diameter * diameter * diameter;
    }

    private static int effectiveDiscoveryRadius() {
        return Math.min(Math.max(1, RadWorksConfig.worldFluidClusterDiscoveryRadius()), MAX_SCAN_RADIUS_CAP);
    }

    private static Cluster buildCluster(
            FluidSample seed,
            Map<BlockPos, FluidSample> sampleByPos,
            Set<BlockPos> visited,
            BlockPos minBound,
            BlockPos maxBound) {
        List<FluidSample> members = new ArrayList<>();
        Set<ResourceLocation> observedFluidIds = new HashSet<>();
        Set<String> matchModes = new HashSet<>();
        Set<ResourceLocation> matchedRuleIds = new HashSet<>();
        List<BlockPos> stack = new ArrayList<>();
        stack.add(seed.position());

        int minX = seed.position().getX();
        int minY = seed.position().getY();
        int minZ = seed.position().getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;
        boolean clipped = false;

        while (!stack.isEmpty()) {
            BlockPos pos = stack.remove(stack.size() - 1);
            if (visited.contains(pos)) {
                continue;
            }

            FluidSample sample = sampleByPos.get(pos);
            if (sample == null) {
                continue;
            }
            if (!sample.normalizedFluidId().equals(seed.normalizedFluidId())) {
                continue;
            }
            visited.add(pos);

            members.add(sample);
            observedFluidIds.add(sample.observedFluidId());
            matchModes.add(sample.ruleMatchMode());
            matchedRuleIds.add(sample.matchedRuleId());
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
            clipped |= onScanBoundary(pos, minBound, maxBound);

            for (Direction direction : Direction.values()) {
                BlockPos adjacent = pos.relative(direction);
                FluidSample adjacentSample = sampleByPos.get(adjacent);
                if (adjacentSample == null) {
                    continue;
                }
                if (!adjacentSample.normalizedFluidId().equals(seed.normalizedFluidId())) {
                    continue;
                }
                if (!visited.contains(adjacent)) {
                    stack.add(adjacent);
                }
            }
        }

        return new Cluster(
                seed.normalizedFluidId(),
                members,
                observedFluidIds,
                matchedRuleIds,
                matchModes,
                members.size(),
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ),
                clipped);
    }

    private static boolean onScanBoundary(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() == min.getX()
                || pos.getY() == min.getY()
                || pos.getZ() == min.getZ()
                || pos.getX() == max.getX()
                || pos.getY() == max.getY()
                || pos.getZ() == max.getZ();
    }

    private static FluidSample nearestSample(List<FluidSample> members, Vec3 target) {
        FluidSample nearest = members.get(0);
        double bestDistance = target.distanceTo(Vec3.atCenterOf(nearest.position()));
        for (int index = 1; index < members.size(); index++) {
            FluidSample sample = members.get(index);
            double distance = target.distanceTo(Vec3.atCenterOf(sample.position()));
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = sample;
            }
        }
        return nearest;
    }

    private static RuleSelection chooseRuleForCluster(Cluster cluster, RadiationRules rules) {
        // Prefer exact normalized (still fluid) rule when present.
        RadiationRule normalizedRule = rules.fluidRule(cluster.normalizedFluidId).orElse(null);
        if (normalizedRule != null) {
            boolean allFallback = cluster.ruleMatchModes.size() == 1 && cluster.ruleMatchModes.contains("fallback");
            String mode = allFallback ? "fallback" : "exact";
            return new RuleSelection(normalizedRule, cluster.normalizedFluidId, cluster.normalizedFluidId, mode);
        }

        // Otherwise pick a deterministic exact observed rule if present.
        FluidSample exactObserved = cluster.members.stream()
                .filter(sample -> "exact".equals(sample.ruleMatchMode()))
                .sorted((left, right) -> left.observedFluidId().toString().compareTo(right.observedFluidId().toString()))
                .findFirst()
                .orElse(null);
        if (exactObserved != null) {
            return new RuleSelection(
                    exactObserved.rule(),
                    exactObserved.matchedRuleId(),
                    exactObserved.observedFluidId(),
                    "exact");
        }

        FluidSample fallbackSample = cluster.members.get(0);
        return new RuleSelection(
                fallbackSample.rule(),
                fallbackSample.matchedRuleId(),
                fallbackSample.observedFluidId(),
                "fallback");
    }

    private static String clusterRuleMatchMode(Set<String> modes) {
        if (modes.size() == 1) {
            return modes.iterator().next();
        }
        return "mixed";
    }

    record FluidSample(
            BlockPos position,
            ResourceLocation observedFluidId,
            ResourceLocation normalizedFluidId,
            ResourceLocation blockId,
            RadiationRule rule,
            ResourceLocation matchedRuleId,
            String ruleMatchMode) {
    }

    private record RuleSelection(
            RadiationRule rule,
            ResourceLocation matchedRuleId,
            ResourceLocation primaryFluidId,
            String mode) {
    }

    private record Cluster(
            ResourceLocation normalizedFluidId,
            List<FluidSample> members,
            Set<ResourceLocation> observedFluidIds,
            Set<ResourceLocation> matchedRuleIds,
            Set<String> ruleMatchModes,
            int contributingFluidBlocks,
            BlockPos minPos,
            BlockPos maxPos,
            boolean maybeClippedByDiscoveryRadius) {
    }
}
