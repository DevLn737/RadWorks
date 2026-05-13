package dev.radworks.radiation;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

final class CreateTransientCarrierExtraction {
    private CreateTransientCarrierExtraction() {
    }

    static Optional<ItemPayload> parseItemAtRoot(CompoundTag root, String path) {
        if (!root.contains(path, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return parseItemCompound(root.getCompound(path));
    }

    static Optional<FluidPayload> parseFluidAtRoot(CompoundTag root, String path) {
        if (!root.contains(path, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return parseFluidCompound(root.getCompound(path));
    }

    static Optional<FluidPayload> parseFluidAtSideFlow(CompoundTag root, String side) {
        return parseFluidAtSideFlowDetailed(root, side).payload();
    }

    static FluidParseOutcome parseFluidAtSideFlowDetailed(CompoundTag root, String side) {
        CompoundTag sideTag = compoundForCaseVariants(root, side);
        if (sideTag == null) {
            return FluidParseOutcome.pathMissing(side + ".Flow.Fluid");
        }

        CompoundTag flowTag = compoundForCaseVariants(sideTag, "Flow");
        if (flowTag == null) {
            return FluidParseOutcome.fluidCompoundMissing(side + ".Flow.Fluid");
        }

        CompoundTag fluidTag = compoundForCaseVariants(flowTag, "Fluid");
        if (fluidTag == null) {
            return FluidParseOutcome.fluidCompoundMissing(side + ".Flow.Fluid");
        }
        return parseFluidCompoundDetailed(fluidTag, side + ".Flow.Fluid");
    }

    static Optional<ItemPayload> parseItemCompound(CompoundTag tag) {
        ResourceLocation id = tryReadId(tag);
        if (id == null) {
            return Optional.empty();
        }
        int count = tryReadPositiveCount(tag);
        if (count <= 0) {
            return Optional.empty();
        }
        return Optional.of(new ItemPayload(id, count));
    }

    static Optional<FluidPayload> parseFluidCompound(CompoundTag tag) {
        return parseFluidCompoundDetailed(tag, "Fluid").payload();
    }

    static FluidParseOutcome parseFluidCompoundDetailed(CompoundTag tag, String dataPath) {
        ResourceLocation id = tryReadId(tag);
        if (id == null) {
            return FluidParseOutcome.invalidFluidId(dataPath);
        }
        AmountReadResult amountResult = tryReadAmount(tag);
        if (!amountResult.present()) {
            return FluidParseOutcome.amountMissing(dataPath, id);
        }
        if (amountResult.amountMb() <= 0) {
            return FluidParseOutcome.amountNonPositive(dataPath, id, amountResult.amountMb());
        }
        return FluidParseOutcome.success(dataPath, id, amountResult.amountMb());
    }

    private static ResourceLocation tryReadId(CompoundTag tag) {
        if (!tag.contains("id", Tag.TAG_STRING)) {
            if (!tag.contains("Id", Tag.TAG_STRING)) {
                return null;
            }
            return ResourceLocation.tryParse(tag.getString("Id"));
        }
        return ResourceLocation.tryParse(tag.getString("id"));
    }

    private static int tryReadPositiveCount(CompoundTag tag) {
        if (tag.contains("count", Tag.TAG_ANY_NUMERIC)) {
            return tag.getInt("count");
        }
        if (tag.contains("Count", Tag.TAG_ANY_NUMERIC)) {
            return tag.getInt("Count");
        }
        return 1;
    }

    private static AmountReadResult tryReadAmount(CompoundTag tag) {
        if (tag.contains("amount", Tag.TAG_ANY_NUMERIC)) {
            return new AmountReadResult(true, tag.getInt("amount"));
        }
        if (tag.contains("Amount", Tag.TAG_ANY_NUMERIC)) {
            return new AmountReadResult(true, tag.getInt("Amount"));
        }
        if (tag.contains("amount", Tag.TAG_STRING)) {
            Integer parsed = parseInt(tag.getString("amount"));
            return parsed == null ? new AmountReadResult(false, 0) : new AmountReadResult(true, parsed);
        }
        if (tag.contains("Amount", Tag.TAG_STRING)) {
            Integer parsed = parseInt(tag.getString("Amount"));
            return parsed == null ? new AmountReadResult(false, 0) : new AmountReadResult(true, parsed);
        }
        return new AmountReadResult(false, 0);
    }

    private static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static CompoundTag compoundForCaseVariants(CompoundTag parent, String canonicalKey) {
        if (parent.contains(canonicalKey, Tag.TAG_COMPOUND)) {
            return parent.getCompound(canonicalKey);
        }
        String variant = capitalize(canonicalKey);
        if (!variant.equals(canonicalKey) && parent.contains(variant, Tag.TAG_COMPOUND)) {
            return parent.getCompound(variant);
        }
        String lower = canonicalKey.toLowerCase();
        if (!lower.equals(canonicalKey) && parent.contains(lower, Tag.TAG_COMPOUND)) {
            return parent.getCompound(lower);
        }
        return null;
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toUpperCase();
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    record ItemPayload(ResourceLocation id, int count) {
    }

    record FluidPayload(ResourceLocation id, int amountMb) {
    }

    enum FluidParseStatus {
        SUCCESS,
        PATH_MISSING,
        FLUID_COMPOUND_MISSING,
        INVALID_FLUID_ID,
        AMOUNT_MISSING,
        AMOUNT_NON_POSITIVE
    }

    record FluidParseOutcome(
            FluidParseStatus status,
            String dataPath,
            ResourceLocation parsedFluidId,
            int parsedAmountMb,
            Optional<FluidPayload> payload) {
        static FluidParseOutcome success(String dataPath, ResourceLocation fluidId, int amountMb) {
            return new FluidParseOutcome(
                    FluidParseStatus.SUCCESS,
                    dataPath,
                    fluidId,
                    amountMb,
                    Optional.of(new FluidPayload(fluidId, amountMb)));
        }

        static FluidParseOutcome pathMissing(String dataPath) {
            return new FluidParseOutcome(FluidParseStatus.PATH_MISSING, dataPath, null, 0, Optional.empty());
        }

        static FluidParseOutcome fluidCompoundMissing(String dataPath) {
            return new FluidParseOutcome(FluidParseStatus.FLUID_COMPOUND_MISSING, dataPath, null, 0, Optional.empty());
        }

        static FluidParseOutcome invalidFluidId(String dataPath) {
            return new FluidParseOutcome(FluidParseStatus.INVALID_FLUID_ID, dataPath, null, 0, Optional.empty());
        }

        static FluidParseOutcome amountMissing(String dataPath, ResourceLocation parsedFluidId) {
            return new FluidParseOutcome(
                    FluidParseStatus.AMOUNT_MISSING,
                    dataPath,
                    parsedFluidId,
                    0,
                    Optional.empty());
        }

        static FluidParseOutcome amountNonPositive(String dataPath, ResourceLocation parsedFluidId, int parsedAmountMb) {
            return new FluidParseOutcome(
                    FluidParseStatus.AMOUNT_NON_POSITIVE,
                    dataPath,
                    parsedFluidId,
                    parsedAmountMb,
                    Optional.empty());
        }
    }

    private record AmountReadResult(boolean present, int amountMb) {
    }
}
