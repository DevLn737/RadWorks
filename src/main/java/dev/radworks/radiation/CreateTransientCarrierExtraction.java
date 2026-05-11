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
        if (!root.contains(side, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag sideTag = root.getCompound(side);
        if (!sideTag.contains("Flow", Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag flowTag = sideTag.getCompound("Flow");
        if (!flowTag.contains("Fluid", Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return parseFluidCompound(flowTag.getCompound("Fluid"));
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
        ResourceLocation id = tryReadId(tag);
        if (id == null) {
            return Optional.empty();
        }
        int amount = tryReadPositiveAmount(tag);
        if (amount <= 0) {
            return Optional.empty();
        }
        return Optional.of(new FluidPayload(id, amount));
    }

    private static ResourceLocation tryReadId(CompoundTag tag) {
        if (!tag.contains("id", Tag.TAG_STRING)) {
            return null;
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

    private static int tryReadPositiveAmount(CompoundTag tag) {
        if (tag.contains("amount", Tag.TAG_ANY_NUMERIC)) {
            return tag.getInt("amount");
        }
        if (tag.contains("Amount", Tag.TAG_ANY_NUMERIC)) {
            return tag.getInt("Amount");
        }
        return 0;
    }

    record ItemPayload(ResourceLocation id, int count) {
    }

    record FluidPayload(ResourceLocation id, int amountMb) {
    }
}
