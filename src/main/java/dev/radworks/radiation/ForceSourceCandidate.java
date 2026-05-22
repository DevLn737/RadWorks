package dev.radworks.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record ForceSourceCandidate(
        CandidateKind candidateKind,
        RadiationSourceType sourceType,
        ResourceLocation blockId,
        ResourceLocation itemId,
        ResourceLocation fluidId,
        BlockPos position,
        String carrierEntityType,
        String carrierEntityId,
        ResourceLocation containerItemId,
        String containerPath,
        ResourceLocation carrierBlockId,
        RadiationTargetKind targetKind,
        int count,
        int amountMb,
        double distance,
        boolean respectsShieldingHint,
        boolean nested,
        int nestedDepth,
        String extractionMode,
        String candidateReason) {
    public enum CandidateKind {
        ITEM("item"),
        FLUID("fluid"),
        BLOCK("block");

        private final String id;

        CandidateKind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }
}

