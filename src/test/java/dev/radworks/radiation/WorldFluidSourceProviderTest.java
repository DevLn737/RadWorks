package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WorldFluidDiagnostics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class WorldFluidSourceProviderTest {
    @Test
    void singleBlockClusterProducesSingleAggregateSource() {
        RadiationRules rules = rulesWithFluids(fluidRule("createnuclear:uranium", 1.0D, 2.0D));
        List<WorldFluidSourceProvider.FluidSample> samples = List.of(sample(0, 64, 0, "createnuclear:uranium", rules));

        List<RadiationSource> sources = WorldFluidSourceProvider.collectFromSamples(
                new Vec3(0.5D, 64.5D, 0.5D),
                rules,
                samples,
                SourceScanSummary.builder(),
                WorldFluidDiagnostics.builder(),
                10,
                new BlockPos(0, 64, 0));

        assertEquals(1, sources.size());
        RadiationSource source = sources.get(0);
        assertEquals(RadiationSourceType.WORLD_FLUID, source.type());
        assertEquals(1000, source.aggregateAmountMb());
        assertEquals(1, source.contributingStacks());
        assertEquals(1.0D, source.finalContribution(), 1.0e-9);
        assertEquals(2.0D, source.effectiveRadius(), 1.0e-9);
    }

    @Test
    void eightConnectedBlocksAggregateIntoOneSource() {
        RadiationRules rules = rulesWithFluids(fluidRule("createnuclear:uranium", 1.0D, 2.0D));
        List<WorldFluidSourceProvider.FluidSample> samples = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            samples.add(sample(x, 64, 0, "createnuclear:uranium", rules));
        }

        List<RadiationSource> sources = WorldFluidSourceProvider.collectFromSamples(
                new Vec3(0.5D, 64.5D, 0.5D),
                rules,
                samples,
                SourceScanSummary.builder(),
                WorldFluidDiagnostics.builder(),
                10,
                new BlockPos(0, 64, 0));

        assertEquals(1, sources.size());
        RadiationSource source = sources.get(0);
        assertEquals(8000, source.aggregateAmountMb());
        assertEquals(8, source.contributingStacks());
        assertEquals(8.0D, source.finalContribution(), 1.0e-9);
        assertTrue(source.effectiveRadius() > 2.0D);
    }

    @Test
    void nineteenConnectedBlocksHaveLargerRadiusThanEightBlockCluster() {
        RadiationRules rules = rulesWithFluids(fluidRule("createnuclear:uranium", 1.0D, 2.0D));
        List<WorldFluidSourceProvider.FluidSample> eight = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            eight.add(sample(x, 64, 0, "createnuclear:uranium", rules));
        }
        List<WorldFluidSourceProvider.FluidSample> nineteen = new ArrayList<>();
        for (int x = 0; x < 19; x++) {
            nineteen.add(sample(x, 64, 0, "createnuclear:uranium", rules));
        }

        RadiationSource eightSource = WorldFluidSourceProvider.collectFromSamples(
                        new Vec3(0.5D, 64.5D, 0.5D),
                        rules,
                        eight,
                        SourceScanSummary.builder(),
                        WorldFluidDiagnostics.builder(),
                        10,
                        new BlockPos(0, 64, 0))
                .get(0);
        RadiationSource nineteenSource = WorldFluidSourceProvider.collectFromSamples(
                        new Vec3(0.5D, 64.5D, 0.5D),
                        rules,
                        nineteen,
                        SourceScanSummary.builder(),
                        WorldFluidDiagnostics.builder(),
                        10,
                        new BlockPos(0, 64, 0))
                .get(0);

        assertEquals(19.0D, nineteenSource.finalContribution(), 1.0e-9);
        assertTrue(nineteenSource.effectiveRadius() > eightSource.effectiveRadius());
    }

    @Test
    void disconnectedClustersProduceSeparateSources() {
        RadiationRules rules = rulesWithFluids(fluidRule("createnuclear:uranium", 1.0D, 2.0D));
        List<WorldFluidSourceProvider.FluidSample> samples = List.of(
                sample(0, 64, 0, "createnuclear:uranium", rules),
                sample(1, 64, 0, "createnuclear:uranium", rules),
                sample(4, 64, 0, "createnuclear:uranium", rules),
                sample(5, 64, 0, "createnuclear:uranium", rules));

        List<RadiationSource> sources = WorldFluidSourceProvider.collectFromSamples(
                new Vec3(2.5D, 64.5D, 0.5D),
                rules,
                samples,
                SourceScanSummary.builder(),
                WorldFluidDiagnostics.builder(),
                10,
                new BlockPos(2, 64, 0));

        assertEquals(2, sources.size());
        assertTrue(sources.stream().allMatch(source -> source.aggregateAmountMb() == 2000));
    }

    @Test
    void clusterDistanceUsesNearestFluidBlock() {
        RadiationRules rules = rulesWithFluids(fluidRule("createnuclear:uranium", 1.0D, 2.0D));
        List<WorldFluidSourceProvider.FluidSample> samples = List.of(
                sample(0, 64, 0, "createnuclear:uranium", rules),
                sample(1, 64, 0, "createnuclear:uranium", rules),
                sample(2, 64, 0, "createnuclear:uranium", rules),
                sample(3, 64, 0, "createnuclear:uranium", rules));
        Vec3 playerPos = new Vec3(0.5D, 64.5D, 0.5D);

        RadiationSource source = WorldFluidSourceProvider.collectFromSamples(
                        playerPos,
                        rules,
                        samples,
                        SourceScanSummary.builder(),
                        WorldFluidDiagnostics.builder(),
                        10,
                        new BlockPos(0, 64, 0))
                .get(0);

        assertEquals(0.0D, source.distance(), 1.0e-9);
    }

    @Test
    void sourceForFluidSampleSupportsExactAndFallback() {
        RadiationRules exactRules = rulesWithFluids(
                fluidRule("createnuclear:uranium", 1.0D, 2.0D),
                fluidRule("createnuclear:flowing_uranium", 2.0D, 2.0D));
        var exact = WorldFluidSourceProvider.sourceForFluidSample(
                exactRules,
                ResourceLocation.parse("createnuclear:flowing_uranium"),
                ResourceLocation.parse("minecraft:water"),
                new BlockPos(0, 64, 0),
                1000,
                0.0D);
        assertTrue(exact.isPresent());
        assertEquals("exact", exact.get().ruleMatchMode());

        RadiationRules fallbackRules = rulesWithFluids(fluidRule("createnuclear:uranium", 1.0D, 2.0D));
        var fallback = WorldFluidSourceProvider.sourceForFluidSample(
                fallbackRules,
                ResourceLocation.parse("createnuclear:flowing_uranium"),
                ResourceLocation.parse("minecraft:water"),
                new BlockPos(0, 64, 0),
                1000,
                0.0D);
        assertTrue(fallback.isPresent());
        assertEquals("fallback", fallback.get().ruleMatchMode());
    }

    @Test
    void playerMovementKeepsClusterMassAndRadiusStable() {
        RadiationRules rules = rulesWithFluids(fluidRule("createnuclear:uranium", 1.0D, 2.0D));
        List<WorldFluidSourceProvider.FluidSample> samples = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            for (int y = 64; y < 67; y++) {
                for (int z = 0; z < 3; z++) {
                    samples.add(sample(x, y, z, "createnuclear:uranium", rules));
                }
            }
        }

        RadiationSource sourceA = WorldFluidSourceProvider.collectFromSamples(
                        new Vec3(0.5D, 64.5D, 0.5D),
                        rules,
                        samples,
                        SourceScanSummary.builder(),
                        WorldFluidDiagnostics.builder(),
                        10,
                        new BlockPos(1, 65, 1))
                .get(0);
        RadiationSource sourceB = WorldFluidSourceProvider.collectFromSamples(
                        new Vec3(1.5D, 65.5D, 1.5D),
                        rules,
                        samples,
                        SourceScanSummary.builder(),
                        WorldFluidDiagnostics.builder(),
                        10,
                        new BlockPos(1, 65, 1))
                .get(0);

        assertEquals(36, sourceA.contributingStacks());
        assertEquals(36, sourceB.contributingStacks());
        assertEquals(sourceA.effectiveRadius(), sourceB.effectiveRadius(), 1.0e-9);
    }

    @Test
    void mixedSourceAndFlowingFluidsClusterTogetherByNormalizedId() {
        RadiationRules rules = rulesWithFluids(
                fluidRule("createnuclear:uranium", 1.0D, 2.0D),
                fluidRule("createnuclear:flowing_uranium", 2.0D, 2.0D));
        List<WorldFluidSourceProvider.FluidSample> samples = List.of(
                sample(0, 64, 0, "createnuclear:uranium", rules),
                sample(1, 64, 0, "createnuclear:flowing_uranium", rules),
                sample(2, 64, 0, "createnuclear:flowing_uranium", rules));

        List<RadiationSource> sources = WorldFluidSourceProvider.collectFromSamples(
                new Vec3(0.5D, 64.5D, 0.5D),
                rules,
                samples,
                SourceScanSummary.builder(),
                WorldFluidDiagnostics.builder(),
                10,
                new BlockPos(0, 64, 0));

        assertEquals(1, sources.size());
        assertEquals(3000, sources.get(0).aggregateAmountMb());
        assertEquals(3, sources.get(0).contributingStacks());
    }

    @Test
    void scanVolumeReflectsDiscoveryRadius() {
        assertEquals(9261, WorldFluidSourceProvider.scanVolumeForRadius(10));
        assertEquals(4913, WorldFluidSourceProvider.scanVolumeForRadius(8));
    }

    private static WorldFluidSourceProvider.FluidSample sample(int x, int y, int z, String fluidId, RadiationRules rules) {
        ResourceLocation observed = ResourceLocation.parse(fluidId);
        RadiationRules.FluidRuleMatch match = rules.resolveFluidRule(observed).orElseThrow();
        return new WorldFluidSourceProvider.FluidSample(
                new BlockPos(x, y, z),
                observed,
                WorldFluidSourceProvider.normalizeFluidIdForCluster(observed),
                ResourceLocation.parse("createnuclear:uranium"),
                match.rule(),
                match.matchedRuleId(),
                match.mode());
    }

    private static RadiationRules rulesWithFluids(RadiationRule... fluidRules) {
        return new RadiationRules(
                true,
                "test",
                List.of(fluidRules),
                0,
                fluidRules.length,
                0,
                0,
                List.of(),
                new RadiationRuleValidationResult());
    }

    private static RadiationRule fluidRule(String id, double strength, double radius) {
        return new RadiationRule(
                RadiationRuleType.FLUID,
                ResourceLocation.parse(id),
                strength,
                radius,
                true,
                true,
                RadiationRuleProfile.BETA,
                false,
                "createnuclear",
                "real_candidate",
                "test",
                "test");
    }
}
