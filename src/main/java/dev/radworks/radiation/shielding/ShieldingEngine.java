package dev.radworks.radiation.shielding;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.radiation.RadiationSource;
import dev.radworks.radiation.RadiationTargetContext;
import dev.radworks.radiation.RadiationTargetKind;
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
import net.minecraft.world.entity.LivingEntity;
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
        return apply(RadiationTargetContext.forPlayer(player), sources, summary);
    }

    public static List<RadiationSource> apply(
            RadiationTargetContext context,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary) {
        return PerformanceStats.timeValue("shielding", () -> applyTimed(context, sources, summary));
    }

    private static List<RadiationSource> applyTimed(
            RadiationTargetContext context,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary) {
        List<RadiationSource> shieldedSources = new ArrayList<>(sources.size());
        for (RadiationSource source : sources) {
            summary.shieldingSourceChecked();
            if (context.targetKind() != RadiationTargetKind.PLAYER) {
                summary.livingShieldingSourceChecked();
            }
            boolean selfCarried = isSelfCarriedSourceForTarget(source, context.target());
            ShieldingResult result = calculate(context, source, summary);
            if (result.shieldingBlocksHit() > 0) {
                summary.shieldingBlocksHit(result.shieldingBlocksHit());
                summary.shieldingSourceReduced();
                if (context.targetKind() != RadiationTargetKind.PLAYER) {
                    summary.livingShieldingSourceReduced();
                }
            }
            RadiationSource next = source.withShielding(result);
            if (selfCarried) {
                next = next.withMatchReasonSuffix("self_carried_source_not_shielded");
            }
            shieldedSources.add(next);
        }
        return List.copyOf(shieldedSources);
    }

    private static ShieldingResult calculate(
            RadiationTargetContext context,
            RadiationSource source,
            SourceScanSummary.Builder summary) {
        if (!source.respectsShielding() || source.position() == null) {
            return ShieldingResult.notApplicable(source.rawContribution());
        }
        if (isSelfCarriedSourceForTarget(source, context.target())) {
            return ShieldingResult.notApplicable(source.rawContribution());
        }

        summary.shieldingSourceApplicable();
        int shieldingBlocksHit = countShieldingBlocks(context.level(), source.position(), context.target(), summary, context);
        if (shieldingBlocksHit == 0) {
            return ShieldingResult.clear(source.rawContribution());
        }
        return ShieldingResult.reduced(source.rawContribution(), shieldingBlocksHit);
    }

    static boolean isSelfCarriedSourceForTarget(RadiationSource source, LivingEntity target) {
        return isSelfCarriedSourceForTarget(source.carrierEntityId(), target.getStringUUID());
    }

    static boolean isSelfCarriedSourceForTarget(String carrierEntityId, String targetEntityId) {
        return carrierEntityId != null
                && carrierEntityId.equals(targetEntityId);
    }

    static Vec3 targetCenter(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private static int countShieldingBlocks(
            ServerLevel level,
            BlockPos sourcePos,
            LivingEntity target,
            SourceScanSummary.Builder summary,
            RadiationTargetContext context) {
        Vec3 sourceCenter = Vec3.atCenterOf(sourcePos);
        Vec3 targetCenter = targetCenter(target);
        double distance = sourceCenter.distanceTo(targetCenter);
        if (distance <= 0.0D) {
            return 0;
        }

        int steps = Math.max(1, (int) Math.ceil(distance / SAMPLE_STEP));
        int maxStep = Math.min(steps - 1, MAX_SAMPLES);
        Set<BlockPos> hitPositions = new HashSet<>();
        BlockPos targetBlock = target.blockPosition();

        for (int step = 1; step <= maxStep; step++) {
            double progress = (double) step / (double) steps;
            Vec3 sample = sourceCenter.lerp(targetCenter, progress);
            BlockPos samplePos = BlockPos.containing(sample);
            if (samplePos.equals(sourcePos) || samplePos.equals(targetBlock)) {
                continue;
            }

            summary.shieldingSampleChecked();
            if (context.targetKind() != RadiationTargetKind.PLAYER) {
                summary.livingShieldingSampleChecked();
            }
            if (level.getBlockState(samplePos).is(SHIELDING_BLOCKS)) {
                hitPositions.add(samplePos.immutable());
            }
        }

        return hitPositions.size();
    }
}
