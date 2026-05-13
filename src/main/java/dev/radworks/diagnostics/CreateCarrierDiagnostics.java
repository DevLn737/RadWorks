package dev.radworks.diagnostics;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class CreateCarrierDiagnostics {
    private static volatile Snapshot lastSnapshot;

    private CreateCarrierDiagnostics() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static synchronized void store(Builder builder) {
        lastSnapshot = builder.build();
    }

    public static synchronized com.google.gson.JsonElement lastToJson() {
        if (lastSnapshot == null) {
            return JsonNull.INSTANCE;
        }
        return lastSnapshot.toJson();
    }

    public static final class Builder {
        private final List<UnexpectedStructureSample> unexpectedStructureSamples = new ArrayList<>();
        private final List<FluidPathSample> fluidPathSamples = new ArrayList<>();
        private int scannedCreateCarrierBlocks;
        private int matchedCreateCarrierItems;
        private int matchedCreateCarrierFluids;
        private int skippedCreateCarrierBlocks;

        public void scannedCarrierBlock() {
            scannedCreateCarrierBlocks++;
        }

        public void matchedItem() {
            matchedCreateCarrierItems++;
        }

        public void matchedFluid() {
            matchedCreateCarrierFluids++;
        }

        public void skippedCarrierBlock() {
            skippedCreateCarrierBlocks++;
        }

        public void unexpectedStructure(
                ResourceLocation blockId,
                BlockPos position,
                String carrierKind,
                String dataPath,
                String message) {
            int cap = RadWorksConfig.createTransientCarrierDiagnosticSampleCap();
            if (unexpectedStructureSamples.size() >= cap) {
                return;
            }
            unexpectedStructureSamples.add(new UnexpectedStructureSample(
                    blockId,
                    position.immutable(),
                    carrierKind,
                    dataPath,
                    message));
        }

        public void fluidPathSample(
                ResourceLocation blockId,
                BlockPos position,
                String carrierKind,
                String side,
                String dataPath,
                boolean pathFound,
                boolean fluidFound,
                ResourceLocation parsedFluidId,
                Integer parsedAmountMb,
                String ruleMatchMode,
                String skippedReason) {
            int cap = RadWorksConfig.createTransientCarrierDiagnosticSampleCap();
            if (fluidPathSamples.size() >= cap) {
                return;
            }
            fluidPathSamples.add(new FluidPathSample(
                    blockId,
                    position.immutable(),
                    carrierKind,
                    side,
                    dataPath,
                    pathFound,
                    fluidFound,
                    parsedFluidId,
                    parsedAmountMb,
                    ruleMatchMode,
                    skippedReason));
        }

        private Snapshot build() {
            return new Snapshot(
                    Instant.now(),
                    scannedCreateCarrierBlocks,
                    matchedCreateCarrierItems,
                    matchedCreateCarrierFluids,
                    skippedCreateCarrierBlocks,
                    RadWorksConfig.createTransientCarrierPathSampleCap(),
                    List.copyOf(unexpectedStructureSamples),
                    List.copyOf(fluidPathSamples));
        }
    }

    private record UnexpectedStructureSample(
            ResourceLocation blockId,
            BlockPos position,
            String carrierKind,
            String dataPath,
            String message) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("blockId", blockId.toString());
            JsonObject pos = new JsonObject();
            pos.addProperty("x", position.getX());
            pos.addProperty("y", position.getY());
            pos.addProperty("z", position.getZ());
            json.add("position", pos);
            json.addProperty("carrierKind", carrierKind);
            json.addProperty("dataPath", dataPath);
            json.addProperty("message", message);
            return json;
        }
    }

    private record FluidPathSample(
            ResourceLocation blockId,
            BlockPos position,
            String carrierKind,
            String side,
            String dataPath,
            boolean pathFound,
            boolean fluidFound,
            ResourceLocation parsedFluidId,
            Integer parsedAmountMb,
            String ruleMatchMode,
            String skippedReason) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("blockId", blockId.toString());
            JsonObject pos = new JsonObject();
            pos.addProperty("x", position.getX());
            pos.addProperty("y", position.getY());
            pos.addProperty("z", position.getZ());
            json.add("position", pos);
            json.addProperty("carrierKind", carrierKind);
            json.addProperty("side", side);
            json.addProperty("dataPath", dataPath);
            json.addProperty("pathFound", pathFound);
            json.addProperty("fluidFound", fluidFound);
            if (parsedFluidId != null) {
                json.addProperty("parsedFluidId", parsedFluidId.toString());
            }
            if (parsedAmountMb != null) {
                json.addProperty("parsedAmountMb", parsedAmountMb);
            }
            json.addProperty("ruleMatchMode", ruleMatchMode);
            json.addProperty("skippedReason", skippedReason);
            return json;
        }
    }

    private record Snapshot(
            Instant createdAt,
            int scannedCreateCarrierBlocks,
            int matchedCreateCarrierItems,
            int matchedCreateCarrierFluids,
            int skippedCreateCarrierBlocks,
            int pathSampleCap,
            List<UnexpectedStructureSample> unexpectedStructureSamples,
            List<FluidPathSample> fluidPathSamples) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("createdAt", createdAt.toString());
            json.addProperty("scannedCreateCarrierBlocks", scannedCreateCarrierBlocks);
            json.addProperty("matchedCreateCarrierItems", matchedCreateCarrierItems);
            json.addProperty("matchedCreateCarrierFluids", matchedCreateCarrierFluids);
            json.addProperty("skippedCreateCarrierBlocks", skippedCreateCarrierBlocks);
            json.addProperty("pathSampleCap", pathSampleCap);
            JsonArray samples = new JsonArray();
            for (UnexpectedStructureSample sample : unexpectedStructureSamples) {
                samples.add(sample.toJson());
            }
            json.add("unexpectedStructureSamples", samples);
            JsonArray fluidSamples = new JsonArray();
            for (FluidPathSample sample : fluidPathSamples) {
                fluidSamples.add(sample.toJson());
            }
            json.add("fluidPathSamples", fluidSamples);
            return json;
        }
    }
}
