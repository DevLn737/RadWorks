package dev.radworks.radiation;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.WarningBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BlockSourceProvider {
    public static final int MAX_SCAN_RADIUS = 8;
    private static final Set<String> WARNED_RADIUS_CLAMPS = ConcurrentHashMap.newKeySet();

    private BlockSourceProvider() {
    }

    public static List<RadiationSource> collect(ServerPlayer player, RadiationRules rules) {
        return PerformanceStats.timeValue("blockScan", () -> collectTimed(player, rules));
    }

    private static List<RadiationSource> collectTimed(ServerPlayer player, RadiationRules rules) {
        List<RadiationSource> sources = new ArrayList<>();
        if (!rules.loaded() || rules.blockRules() == 0) {
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
            BlockState state = player.serverLevel().getBlockState(pos);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            RadiationRule rule = rules.blockRule(blockId).orElse(null);
            if (rule == null) {
                continue;
            }

            double distance = playerPosition.distanceTo(Vec3.atCenterOf(pos));
            if (distance > rule.radius()) {
                continue;
            }

            sources.add(RadiationSource.block(
                    blockId,
                    pos,
                    rule.strength(),
                    rule.radius(),
                    distance,
                    rule.strength(),
                    "active block rule matched type=block id=" + blockId));
        }

        return List.copyOf(sources);
    }

    private static int effectiveScanRadius(RadiationRules rules) {
        return (int) Math.ceil(Math.min(rules.maxActiveBlockRuleRadius(), MAX_SCAN_RADIUS));
    }

    private static void warnForClampedRules(RadiationRules rules) {
        for (RadiationRule rule : rules.activeBlockRules()) {
            if (rule.radius() <= MAX_SCAN_RADIUS) {
                continue;
            }

            String warningKey = rules.checksum() + ":" + rule.key();
            if (WARNED_RADIUS_CLAMPS.add(warningKey)) {
                WarningBuffer.add(
                        "BLOCK_SCAN_RADIUS_CLAMPED",
                        "blockScan",
                        "Block rule "
                                + rule.key()
                                + " radius="
                                + rule.radius()
                                + " exceeds Phase 4A max scan radius "
                                + MAX_SCAN_RADIUS
                                + "; command scan is clamped");
            }
        }
    }
}
