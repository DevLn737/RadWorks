package dev.radworks.radiation;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WarningBuffer;
import java.util.ArrayList;
import java.util.List;
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
        return collect(player, rules, SourceScanSummary.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
        return PerformanceStats.timeValue("fluidHandlerScan", () -> collectTimed(player, rules, summary));
    }

    private static List<RadiationSource> collectTimed(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
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
            collectTanks(blockId, pos, distance, lookup, rules, sources, summary);
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

    private static void collectTanks(
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            HandlerLookup lookup,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary) {
        int tanks;
        try {
            tanks = lookup.handler().getTanks();
        } catch (RuntimeException exception) {
            warnScanFailure(blockId, pos, lookup.capabilityContext(), "getTanks failed: " + exception.getMessage());
            return;
        }

        for (int tank = 0; tank < tanks; tank++) {
            summary.fluidTankChecked();
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
                continue;
            }

            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(stack.getFluid());
            RadiationRule rule = rules.fluidRule(fluidId).orElse(null);
            if (rule == null || distance > rule.radius()) {
                continue;
            }

            int amountMb = stack.getAmount();
            double contribution = rule.strength() * amountMb / 1000.0D;
            summary.fluidMatch();
            sources.add(RadiationSource.blockFluidHandler(
                    blockId,
                    pos,
                    lookup.capabilityContext(),
                    "fluid_handler." + tank,
                    fluidId,
                    amountMb,
                    rule.strength(),
                    rule.radius(),
                    distance,
                    contribution,
                    "NeoForge FluidHandler block capability matched active fluid rule type=fluid id=" + fluidId));
        }
    }

    private static int effectiveScanRadius(RadiationRules rules) {
        return (int) Math.ceil(Math.min(rules.maxActiveFluidRuleRadius(), MAX_SCAN_RADIUS));
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
}
