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
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class BlockItemHandlerSourceProvider {
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

    private BlockItemHandlerSourceProvider() {
    }

    public static List<RadiationSource> collect(ServerPlayer player, RadiationRules rules) {
        return collect(player, rules, SourceScanSummary.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
        return PerformanceStats.timeValue("itemHandlerScan", () -> collectTimed(player, rules, summary));
    }

    private static List<RadiationSource> collectTimed(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
        List<RadiationSource> sources = new ArrayList<>();
        if (!rules.loaded() || rules.itemRules() == 0) {
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
            summary.itemHandlerPositionChecked();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container) {
                summary.skippedContainerBlockEntityForItemHandler();
                continue;
            }

            HandlerLookup lookup = findHandler(level, pos);
            if (lookup == null) {
                continue;
            }
            summary.itemHandlerFound();

            BlockState state = level.getBlockState(pos);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            double distance = playerPosition.distanceTo(Vec3.atCenterOf(pos));
            collectHandlerSlots(blockId, pos, distance, lookup, rules, sources, summary);
        }

        return List.copyOf(sources);
    }

    private static HandlerLookup findHandler(ServerLevel level, BlockPos pos) {
        IItemHandler unsided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (unsided != null) {
            return new HandlerLookup(unsided, "unsided");
        }

        for (Direction side : SIDED_CONTEXTS) {
            IItemHandler sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            if (sided != null) {
                return new HandlerLookup(sided, side.getName());
            }
        }
        return null;
    }

    private static void collectHandlerSlots(
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            HandlerLookup lookup,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary) {
        int slots;
        try {
            slots = lookup.handler().getSlots();
        } catch (RuntimeException exception) {
            warnScanFailure(blockId, pos, lookup.capabilityContext(), "getSlots failed: " + exception.getMessage());
            return;
        }

        for (int slot = 0; slot < slots; slot++) {
            summary.itemHandlerSlotChecked();
            ItemStack stack;
            try {
                stack = lookup.handler().getStackInSlot(slot);
            } catch (RuntimeException exception) {
                warnScanFailure(
                        blockId,
                        pos,
                        lookup.capabilityContext(),
                        "getStackInSlot(" + slot + ") failed: " + exception.getMessage());
                continue;
            }

            if (stack.isEmpty()) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            RadiationRule rule = rules.itemRule(itemId).orElse(null);
            if (rule == null || distance > rule.radius()) {
                continue;
            }

            double contribution = stack.getCount() * rule.strength();
            summary.itemHandlerMatch();
            sources.add(RadiationSource.blockItemHandler(
                    blockId,
                    pos,
                    lookup.capabilityContext(),
                    "item_handler." + slot,
                    itemId,
                    stack.getCount(),
                    rule.strength(),
                    rule.radius(),
                    distance,
                    contribution,
                    "NeoForge ItemHandler block capability matched active item rule type=item id=" + itemId));
        }
    }

    private static int effectiveScanRadius(RadiationRules rules) {
        return (int) Math.ceil(Math.min(rules.maxActiveItemRuleRadius(), MAX_SCAN_RADIUS));
    }

    private static void warnForClampedRules(RadiationRules rules) {
        for (RadiationRule rule : rules.activeItemRules()) {
            if (rule.radius() <= MAX_SCAN_RADIUS) {
                continue;
            }

            String warningKey = rules.checksum() + ":" + rule.key();
            if (WARNED_RADIUS_CLAMPS.add(warningKey)) {
                WarningBuffer.add(
                        "ITEM_HANDLER_SCAN_RADIUS_CLAMPED",
                        "itemHandlerScan",
                        "Item rule "
                                + rule.key()
                                + " radius="
                                + rule.radius()
                                + " exceeds Phase 4C max scan radius "
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
                    "ITEM_HANDLER_SCAN_FAILED",
                    "itemHandlerScan",
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

    private record HandlerLookup(IItemHandler handler, String capabilityContext) {
    }
}
