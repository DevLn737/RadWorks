package dev.radworks.radiation;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WarningBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BlockEntityInventorySourceProvider {
    public static final int MAX_SCAN_RADIUS = 8;
    private static final Set<String> WARNED_RADIUS_CLAMPS = ConcurrentHashMap.newKeySet();

    private BlockEntityInventorySourceProvider() {
    }

    public static List<RadiationSource> collect(ServerPlayer player, RadiationRules rules) {
        return collect(player, rules, SourceScanSummary.builder());
    }

    public static List<RadiationSource> collect(
            ServerPlayer player,
            RadiationRules rules,
            SourceScanSummary.Builder summary) {
        return PerformanceStats.timeValue("blockEntityInventoryScan", () -> collectTimed(player, rules, summary));
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

        Vec3 playerPosition = player.position();
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-scanRadius, -scanRadius, -scanRadius);
        BlockPos max = center.offset(scanRadius, scanRadius, scanRadius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            summary.blockEntityChecked();
            BlockEntity blockEntity = player.serverLevel().getBlockEntity(pos);
            if (!(blockEntity instanceof Container container)) {
                continue;
            }
            summary.containerBlockEntityFound();

            BlockState state = player.serverLevel().getBlockState(pos);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            double distance = playerPosition.distanceTo(Vec3.atCenterOf(pos));
            collectContainerSlots(blockId, pos, distance, container, rules, sources, summary);
        }

        return List.copyOf(sources);
    }

    private static void collectContainerSlots(
            ResourceLocation blockId,
            BlockPos containerPos,
            double distance,
            Container container,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            summary.containerSlotChecked();
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            RadiationRule rule = rules.itemRule(itemId).orElse(null);
            if (rule == null || distance > rule.radius()) {
                continue;
            }

            double contribution = stack.getCount() * rule.strength();
            summary.containerMatch();
            sources.add(RadiationSource.blockEntityInventory(
                    blockId,
                    containerPos,
                    "container." + slot,
                    itemId,
                    stack.getCount(),
                    rule.strength(),
                    rule.radius(),
                    distance,
                    contribution,
                    "vanilla Container slot matched active item rule type=item id=" + itemId));
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
                        "BLOCK_ENTITY_INVENTORY_SCAN_RADIUS_CLAMPED",
                        "blockEntityInventoryScan",
                        "Item rule "
                                + rule.key()
                                + " radius="
                                + rule.radius()
                                + " exceeds Phase 4B max scan radius "
                                + MAX_SCAN_RADIUS
                                + "; command scan is clamped");
            }
        }
    }
}
