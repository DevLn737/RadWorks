package dev.radworks.radiation.shielding;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.radiation.RadiationSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public final class ShieldingEngine {
    public static final double SAMPLE_STEP = 0.25D;
    public static final int MAX_SAMPLES = 64;
    public static final TagKey<Block> SHIELDING_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("radworks", "shielding_blocks"));

    private ShieldingEngine() {
    }

    public static List<RadiationSource> apply(
            ServerPlayer player,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary) {
        return PerformanceStats.timeValue("shielding", () -> applyTimed(player, sources, summary));
    }

    private static List<RadiationSource> applyTimed(
            ServerPlayer player,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary) {
        List<RadiationSource> shieldedSources = new ArrayList<>(sources.size());
        for (RadiationSource source : sources) {
            summary.shieldingSourceChecked();
            ShieldingResult result = calculate(player, source, summary);
            if (result.shieldingBlocksHit() > 0) {
                summary.shieldingBlocksHit(result.shieldingBlocksHit());
                summary.shieldingSourceReduced();
            }
            shieldedSources.add(source.withShielding(result));
        }
        return List.copyOf(shieldedSources);
    }

    private static ShieldingResult calculate(
            ServerPlayer player,
            RadiationSource source,
            SourceScanSummary.Builder summary) {
        if (!source.respectsShielding() || source.position() == null) {
            return ShieldingResult.notApplicable(source.rawContribution());
        }

        summary.shieldingSourceApplicable();
        int shieldingBlocksHit = countShieldingBlocks(player.serverLevel(), source.position(), player, summary);
        if (shieldingBlocksHit == 0) {
            return ShieldingResult.clear(source.rawContribution());
        }
        return ShieldingResult.reduced(source.rawContribution(), shieldingBlocksHit);
    }

    private static int countShieldingBlocks(
            ServerLevel level,
            BlockPos sourcePos,
            ServerPlayer player,
            SourceScanSummary.Builder summary) {
        Vec3 sourceCenter = Vec3.atCenterOf(sourcePos);
        Vec3 playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        double distance = sourceCenter.distanceTo(playerCenter);
        if (distance <= 0.0D) {
            return 0;
        }

        int steps = Math.max(1, (int) Math.ceil(distance / SAMPLE_STEP));
        int maxStep = Math.min(steps - 1, MAX_SAMPLES);
        Set<BlockPos> hitPositions = new HashSet<>();
        BlockPos playerBlock = player.blockPosition();

        for (int step = 1; step <= maxStep; step++) {
            double progress = (double) step / (double) steps;
            Vec3 sample = sourceCenter.lerp(playerCenter, progress);
            BlockPos samplePos = BlockPos.containing(sample);
            if (samplePos.equals(sourcePos) || samplePos.equals(playerBlock)) {
                continue;
            }

            summary.shieldingSampleChecked();
            if (level.getBlockState(samplePos).is(SHIELDING_BLOCKS)) {
                hitPositions.add(samplePos.immutable());
            }
        }

        return hitPositions.size();
    }
}
