package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.diagnostics.SourceScanSummary;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.junit.jupiter.api.Test;

class BlockFluidHandlerSourceProviderContractTest {
    @Test
    void collectTanks_whenMatchingFluidPresent_shouldCreateBlockFluidHandlerSource() throws Exception {
        IFluidHandler handler = new StaticFluidHandler(new FluidStack(Fluids.WATER, 1000));
        List<RadiationSource> sources = new ArrayList<>();

        invokeCollectTanks(
                ResourceLocation.parse("minecraft:water_cauldron"),
                new BlockPos(3, 64, 3),
                1.0D,
                handlerLookup(handler, "unsided"),
                rulesWithFluid("minecraft:water"),
                sources,
                SourceScanSummary.builder(),
                ForceSourceCandidateSink.NO_OP);

        assertEquals(1, sources.size());
        RadiationSource source = sources.getFirst();
        assertEquals(RadiationSourceType.BLOCK_FLUID_HANDLER, source.type());
        assertEquals("minecraft:water", source.fluidId().toString());
        assertEquals(1000, source.aggregateAmountMb());
        assertEquals(1.0D, source.finalContribution(), 1.0e-9);
    }

    @Test
    void collectTanks_whenAmountIsOneMillibucket_shouldKeepFineGrainedContribution() throws Exception {
        IFluidHandler handler = new StaticFluidHandler(new FluidStack(Fluids.WATER, 1));
        List<RadiationSource> sources = new ArrayList<>();

        invokeCollectTanks(
                ResourceLocation.parse("minecraft:water_cauldron"),
                new BlockPos(3, 64, 3),
                1.0D,
                handlerLookup(handler, "unsided"),
                rulesWithFluid("minecraft:water"),
                sources,
                SourceScanSummary.builder(),
                ForceSourceCandidateSink.NO_OP);

        assertEquals(1, sources.size());
        assertEquals(1, sources.getFirst().aggregateAmountMb());
        assertEquals(0.001D, sources.getFirst().finalContribution(), 1.0e-9);
    }

    @Test
    void collectTanks_whenNoMatchingFluidRule_shouldEmitForceCandidate() throws Exception {
        IFluidHandler handler = new StaticFluidHandler(new FluidStack(Fluids.LAVA, 1000));
        ForceSourceCandidateCollector candidates = new ForceSourceCandidateCollector(RadiationTargetKind.PLAYER);
        List<RadiationSource> sources = new ArrayList<>();

        invokeCollectTanks(
                ResourceLocation.parse("minecraft:lava_cauldron"),
                new BlockPos(3, 64, 3),
                1.0D,
                handlerLookup(handler, "unsided"),
                rulesWithFluid("minecraft:water"),
                sources,
                SourceScanSummary.builder(),
                candidates);

        assertTrue(sources.isEmpty());
        assertEquals(1, candidates.snapshot().size());
        ForceSourceCandidate candidate = candidates.snapshot().getFirst();
        assertEquals(RadiationSourceType.BLOCK_FLUID_HANDLER, candidate.sourceType());
        assertEquals("minecraft:lava", candidate.fluidId().toString());
        assertEquals(1000, candidate.amountMb());
        assertEquals("minecraft:lava_cauldron", candidate.carrierBlockId().toString());
    }

    private static Object handlerLookup(IFluidHandler handler, String context) throws Exception {
        Class<?> lookupType = Class.forName("dev.radworks.radiation.BlockFluidHandlerSourceProvider$HandlerLookup");
        Constructor<?> ctor = lookupType.getDeclaredConstructor(IFluidHandler.class, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(handler, context);
    }

    private static void invokeCollectTanks(
            ResourceLocation blockId,
            BlockPos pos,
            double distance,
            Object handlerLookup,
            RadiationRules rules,
            List<RadiationSource> sources,
            SourceScanSummary.Builder summary,
            ForceSourceCandidateSink candidateSink)
            throws Exception {
        Class<?> lookupType = Class.forName("dev.radworks.radiation.BlockFluidHandlerSourceProvider$HandlerLookup");
        Method method = BlockFluidHandlerSourceProvider.class.getDeclaredMethod(
                "collectTanks",
                ResourceLocation.class,
                BlockPos.class,
                double.class,
                lookupType,
                RadiationRules.class,
                List.class,
                SourceScanSummary.Builder.class,
                ForceSourceCandidateSink.class);
        method.setAccessible(true);
        method.invoke(null, blockId, pos, distance, handlerLookup, rules, sources, summary, candidateSink);
    }

    private static RadiationRules rulesWithFluid(String id) {
        RadiationRule rule = new RadiationRule(
                RadiationRuleType.FLUID,
                ResourceLocation.parse(id),
                1.0D,
                2.0D,
                true,
                true,
                RadiationRuleProfile.BETA,
                false,
                null,
                "test",
                "test",
                "test");
        return new RadiationRules(
                true,
                "test",
                List.of(rule),
                0,
                0,
                1,
                0,
                List.of(),
                new RadiationRuleValidationResult());
    }

    private static final class StaticFluidHandler implements IFluidHandler {
        private final FluidStack stack;

        private StaticFluidHandler(FluidStack stack) {
            this.stack = stack;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return stack;
        }

        @Override
        public int getTankCapacity(int tank) {
            return stack.getAmount();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
