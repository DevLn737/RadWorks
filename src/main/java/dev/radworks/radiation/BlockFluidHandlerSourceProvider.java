package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.HandlerDiagnostics;
import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WarningBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class BlockFluidHandlerSourceProvider {
    public static final int MAX_SCAN_RADIUS = 8;
    private static final Direction[] SIDED_CONTEXTS = {
            Direction.UP,
            Direction.DOWN,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };
    private static final Set<String> WARNED_RADIUS_CLAMPS = ConcurrentHashMap.newKeySet();
    private static final Set<String> WARNED_SCAN_FAILURES = ConcurrentHashMap.newKeySet();

    private BlockFluidHandlerSourceProvider() {
    }

    public static List<RadiationSource> collect(ServerPlayer player, RadiationRules rules) {
        return collect(player, rules, SourceScanSummary.builder(), HandlerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
        return collect(player, rules, summary, HandlerDiagnostics.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            HandlerDiagnostics.Builder handlerDiagnostics) {
        return PerformanceStats.timeValue("fluidHandlerScan", () -> collectTimed(player, rules, summary, handlerDiagnostics));
    }

    private static List<RadiationSource> collectTimed(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary,
            HandlerDiagnostics.Builder handlerDiagnostics) {
        List<RadiationSource> sources = new ArrayList<>();
        if (!rules.loaded() || rules.fluidRules() == 0) {
            return sources;
        }

        warnForClampedRules(rules);

        int scanRadius = effectiveScanRadius(rules);
        if (scanRadius <= 0) {
            return sources;
        }

        ServerLevel level = player.serverLevel();
        Vec3 playerPosition = player.position();
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-scanRadius, -scanRadius, -scanRadius);
        BlockPos max = center.offset(scanRadius, scanRadius, scanRadius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            summary.fluidHandlerPositionChecked();
            HandlerLookup lookup = findHandler(level, pos);
            if (lookup == null) {
                continue;
            }
            summary.fluidHandlerFound();

            BlockState state = level.getBlockState(pos);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            double distance = playerPosition.distanceTo(Vec3.atCenterOf(pos));
            HandlerScanResult scanResult = collectTanks(blockId, pos, distance, lookup, rules, sources, summary);
            if (scanResult.matches() == 0) {
                handlerDiagnostics.addFluidHandlerSample(
                        blockId,
                        pos,
                        lookup.capabilityContext(),
                        scanResult.tanksChecked(),
                        scanResult.matches(),
                        scanResult.contents());
            }
        }

        return List.copyOf(sources);
    }

    private static HandlerLookup findHandler(ServerLevel level, BlockPos pos) {
        IFluidHandler unsided = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (unsided != null) {
            return new HandlerLookup(unsided, "unsided");
        }

        for (Direction side : SIDED_CONTEXTS) {
            IFluidHandler sided = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
            if (sided != null) {
                return new HandlerLookup(sided, side.getName());
            }
        }
        return null;
    }

    private static HandlerScanResult collectTanks(
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            HandlerLookup lookup,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary) {
        final int maxContents = 5;
        int tanksChecked = 0;
        int matches = 0;
        List<HandlerDiagnostics.ContentSample> contents = new ArrayList<>(maxContents);
        Map<Key, AggregatedSourceAccumulator.FluidAggregate> aggregates = new LinkedHashMap<>();

        int tanks;
        try {
            tanks = lookup.handler().getTanks();
        } catch (RuntimeException exception) {
            warnScanFailure(blockId, pos, lookup.capabilityContext(), "getTanks failed: " + exception.getMessage());
            return new HandlerScanResult(tanksChecked, matches, contents);
        }

        for (int tank = 0; tank < tanks; tank++) {
            summary.fluidTankChecked();
            tanksChecked++;
            FluidStack stack;
            try {
                stack = lookup.handler().getFluidInTank(tank);
            } catch (RuntimeException exception) {
                warnScanFailure(
                        blockId,
                        pos,
                        lookup.capabilityContext(),
                        "getFluidInTank(" + tank + ") failed: " + exception.getMessage());
                continue;
            }

            if (stack.isEmpty()) {
                addFluidSample(contents, maxContents, tank, null, 0, "empty", distance, null, null, null);
                continue;
            }

            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(stack.getFluid());
            RadiationRules.FluidRuleMatch ruleMatch = rules.resolveFluidRule(fluidId).orElse(null);
            if (ruleMatch == null) {
                addFluidSample(contents, maxContents, tank, fluidId, stack.getAmount(), "no_active_rule", distance, null, null, null);
                continue;
            }
            RadiationRule rule = ruleMatch.rule();
            Key key = new Key(fluidId, rule.key());
            AggregatedSourceAccumulator.FluidAggregate aggregate = aggregates.computeIfAbsent(
                    key,
                    ignored -> AggregatedSourceAccumulator.newFluidAggregate(
                            new AggregatedSourceAccumulator.FluidGroupKey(
                                    RadiationSourceType.BLOCK_FLUID_HANDLER,
                                    pos.immutable(),
                                    blockId,
                                    lookup.capabilityContext(),
                                    fluidId,
                                    rule.key()),
                            rule,
                            distance));
            AggregatedSourceAccumulator.addFluidStack(aggregate, stack.getAmount());
        }

        for (AggregatedSourceAccumulator.FluidAggregate aggregate : aggregates.values()) {
            double baseRadius = aggregate.rule().radius();
            double units = DynamicRadiusModel.aggregateUnitsForFluids(aggregate.aggregateAmountMb());
            double effectiveRadius = DynamicRadiusModel.effectiveRadius(baseRadius, units);
            if (!DynamicRadiusModel.isActive(distance, effectiveRadius)) {
                addFluidSample(
                        contents,
                        maxContents,
                        -1,
                        aggregate.key().fluidId(),
                        aggregate.aggregateAmountMb(),
                        DynamicRadiusModel.outsideDynamicRadiusReason(),
                        distance,
                        baseRadius,
                        effectiveRadius,
                        units);
                continue;
            }

            summary.fluidMatch();
            summary.aggregateRowProduced();
            matches++;
            sources.add(RadiationSource.blockFluidHandlerAggregate(
                    blockId,
                    pos,
                    lookup.capabilityContext(),
                    aggregate.key().fluidId(),
                    aggregate.aggregateAmountMb(),
                    aggregate.contributingStacks(),
                    aggregate.rule().strength(),
                    baseRadius,
                    effectiveRadius,
                    distance,
                    aggregate.rule().respectsShielding(),
                    aggregate.rawContribution(),
                    ruleMatchModeForGroup(aggregate.key().fluidId(), rules),
                    "NeoForge FluidHandler aggregated source matched "
                            + ruleMatchModeForGroup(aggregate.key().fluidId(), rules)
                            + " fluid rule id="
                            + matchedFluidRuleIdForGroup(aggregate.key().fluidId(), rules)
                            + " observedFluidId="
                            + aggregate.key().fluidId()
                            + " amountMb="
                            + aggregate.aggregateAmountMb()));
        }
        return new HandlerScanResult(tanksChecked, matches, contents);
    }

    private static String ruleMatchModeForGroup(ResourceLocation fluidId, RadiationRules rules) {
        RadiationRules.FluidRuleMatch match = rules.resolveFluidRule(fluidId).orElse(null);
        return match == null ? "exact" : match.mode();
    }

    private static ResourceLocation matchedFluidRuleIdForGroup(ResourceLocation fluidId, RadiationRules rules) {
        RadiationRules.FluidRuleMatch match = rules.resolveFluidRule(fluidId).orElse(null);
        return match == null ? fluidId : match.matchedRuleId();
    }

    private static void addFluidSample(
            List<HandlerDiagnostics.ContentSample> contents,
            int maxContents,
            int tank,
            ResourceLocation fluidId,
            int amountMb,
            String reason,
            Double distance,
            Double baseRadius,
            Double effectiveRadius,
            Double aggregateUnitsSnapshot) {
        if (contents.size() >= maxContents) {
            return;
        }
        String tankLabel = tank < 0 ? null : "fluid_handler." + tank;
        contents.add(HandlerDiagnostics.ContentSample.fluid(
                tankLabel,
                fluidId,
                amountMb,
                reason,
                distance,
                baseRadius,
                effectiveRadius,
                aggregateUnitsSnapshot));
    }

    private static int effectiveScanRadius(RadiationRules rules) {
        double baseMax = rules.maxActiveFluidRuleRadius();
        double dynamicMax = RadWorksConfig.dynamicRadiusEnabled()
                ? Math.max(baseMax, RadWorksConfig.dynamicRadiusMaxCap())
                : baseMax;
        return (int) Math.ceil(Math.min(dynamicMax, MAX_SCAN_RADIUS));
    }

    private static void warnForClampedRules(RadiationRules rules) {
        for (RadiationRule rule : rules.activeFluidRules()) {
            if (rule.radius() <= MAX_SCAN_RADIUS) {
                continue;
            }

            String warningKey = rules.checksum() + ":" + rule.key();
            if (WARNED_RADIUS_CLAMPS.add(warningKey)) {
                WarningBuffer.add(
                        "FLUID_HANDLER_SCAN_RADIUS_CLAMPED",
                        "fluidHandlerScan",
                        "Fluid rule "
                                + rule.key()
                                + " radius="
                                + rule.radius()
                                + " exceeds Phase 4D max scan radius "
                                + MAX_SCAN_RADIUS
                                + "; command scan is clamped");
            }
        }
    }

    private static void warnScanFailure(
            ResourceLocation blockId,
            BlockPos pos,
            String capabilityContext,
            String message) {
        String warningKey = blockId
                + "@"
                + pos.getX()
                + ","
                + pos.getY()
                + ","
                + pos.getZ()
                + ":"
                + capabilityContext
                + ":"
                + message;
        if (WARNED_SCAN_FAILURES.add(warningKey)) {
            WarningBuffer.add(
                    "FLUID_HANDLER_SCAN_FAILED",
                    "fluidHandlerScan",
                    blockId
                            + " at "
                            + pos.getX()
                            + ","
                            + pos.getY()
                            + ","
                            + pos.getZ()
                            + " context="
                            + capabilityContext
                            + ": "
                            + message);
        }
    }

    private record HandlerLookup(IFluidHandler handler, String capabilityContext) {
    }

    private record HandlerScanResult(
            int tanksChecked,
            int matches,
            List<HandlerDiagnostics.ContentSample> contents) {
    }

    private record Key(ResourceLocation fluidId, String ruleKey) {
    }
}
