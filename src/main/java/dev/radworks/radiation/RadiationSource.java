package dev.radworks.radiation;

import com.google.gson.JsonObject;
import dev.radworks.radiation.shielding.ShieldingResult;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record RadiationSource(
        RadiationSourceType type,
        ResourceLocation itemId,
        ResourceLocation fluidId,
        ResourceLocation blockId,
        String slot,
        String tank,
        int count,
        int amountMb,
        BlockPos position,
        String capabilityContext,
        double ruleStrength,
        double ruleRadius,
        double baseRadius,
        double effectiveRadius,
        double dynamicRadiusBonus,
        String radiusFormula,
        int aggregateCount,
        int aggregateAmountMb,
        int contributingStacks,
        double distance,
        boolean activeBecause,
        boolean respectsShielding,
        double rawContribution,
        String shielding,
        int shieldingBlocksHit,
        double shieldingMultiplier,
        double shieldingReduction,
        double finalContribution,
        String carrierKind,
        String carrierEntityType,
        String carrierEntityId,
        String carrierSourceKind,
        String dataPath,
        String extractionMode,
        String ruleMatchMode,
        String matchReason) {
    public static RadiationSource playerInventoryAggregate(
            ResourceLocation itemId,
            int aggregateCount,
            int contributingStacks,
            double ruleStrength,
            double baseRadius,
            double effectiveRadius,
            double contribution,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.PLAYER_INVENTORY,
                itemId,
                null,
                null,
                null,
                null,
                aggregateCount,
                0,
                null,
                null,
                ruleStrength,
                baseRadius,
                baseRadius,
                effectiveRadius,
                DynamicRadiusModel.dynamicRadiusBonus(baseRadius, effectiveRadius),
                DynamicRadiusModel.radiusFormulaLabel(),
                aggregateCount,
                0,
                contributingStacks,
                0.0D,
                true,
                true,
                contribution,
                "not_applicable",
                0,
                1.0D,
                0.0D,
                contribution,
                null,
                null,
                null,
                null,
                null,
                null,
                "exact",
                matchReason);
    }

    public static RadiationSource block(
            ResourceLocation blockId,
            BlockPos position,
            double ruleStrength,
            double ruleRadius,
            double distance,
            boolean respectsShielding,
            double contribution,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.BLOCK,
                null,
                null,
                blockId,
                null,
                null,
                0,
                0,
                position.immutable(),
                null,
                ruleStrength,
                ruleRadius,
                ruleRadius,
                ruleRadius,
                0.0D,
                "static",
                0,
                0,
                1,
                distance,
                distance <= ruleRadius,
                respectsShielding,
                contribution,
                respectsShielding ? "clear" : "not_applicable",
                0,
                1.0D,
                0.0D,
                contribution,
                null,
                null,
                null,
                null,
                null,
                null,
                "exact",
                matchReason);
    }

    public static RadiationSource worldFluid(
            ResourceLocation fluidId,
            ResourceLocation blockId,
            BlockPos position,
            int amountMb,
            int contributingFluidBlocks,
            double ruleStrength,
            double ruleRadius,
            double distance,
            boolean respectsShielding,
            String ruleMatchMode,
            String matchReason) {
        double units = DynamicRadiusModel.aggregateUnitsForFluids(amountMb);
        double effectiveRadius = DynamicRadiusModel.effectiveRadius(ruleRadius, units);
        double contribution = ruleStrength * ((double) amountMb / 1000.0D);
        return new RadiationSource(
                RadiationSourceType.WORLD_FLUID,
                null,
                fluidId,
                blockId,
                null,
                null,
                0,
                amountMb,
                position.immutable(),
                null,
                ruleStrength,
                ruleRadius,
                ruleRadius,
                effectiveRadius,
                DynamicRadiusModel.dynamicRadiusBonus(ruleRadius, effectiveRadius),
                DynamicRadiusModel.radiusFormulaLabel(),
                0,
                amountMb,
                Math.max(1, contributingFluidBlocks),
                distance,
                DynamicRadiusModel.isActive(distance, effectiveRadius),
                respectsShielding,
                contribution,
                respectsShielding ? "clear" : "not_applicable",
                0,
                1.0D,
                0.0D,
                contribution,
                null,
                null,
                null,
                null,
                null,
                null,
                ruleMatchMode,
                matchReason);
    }

    public static RadiationSource blockEntityInventoryAggregate(
            ResourceLocation blockId,
            BlockPos containerPos,
            ResourceLocation itemId,
            int aggregateCount,
            int contributingStacks,
            double ruleStrength,
            double baseRadius,
            double effectiveRadius,
            double distance,
            boolean respectsShielding,
            double contribution,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.BLOCK_ENTITY_INVENTORY,
                itemId,
                null,
                blockId,
                null,
                null,
                aggregateCount,
                0,
                containerPos.immutable(),
                null,
                ruleStrength,
                baseRadius,
                baseRadius,
                effectiveRadius,
                DynamicRadiusModel.dynamicRadiusBonus(baseRadius, effectiveRadius),
                DynamicRadiusModel.radiusFormulaLabel(),
                aggregateCount,
                0,
                contributingStacks,
                distance,
                DynamicRadiusModel.isActive(distance, effectiveRadius),
                respectsShielding,
                contribution,
                respectsShielding ? "clear" : "not_applicable",
                0,
                1.0D,
                0.0D,
                contribution,
                null,
                null,
                null,
                null,
                null,
                null,
                "exact",
                matchReason);
    }

    public static RadiationSource blockItemHandlerAggregate(
            ResourceLocation blockId,
            BlockPos position,
            String capabilityContext,
            ResourceLocation itemId,
            int aggregateCount,
            int contributingStacks,
            double ruleStrength,
            double baseRadius,
            double effectiveRadius,
            double distance,
            boolean respectsShielding,
            double contribution,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.BLOCK_ITEM_HANDLER,
                itemId,
                null,
                blockId,
                null,
                null,
                aggregateCount,
                0,
                position.immutable(),
                capabilityContext,
                ruleStrength,
                baseRadius,
                baseRadius,
                effectiveRadius,
                DynamicRadiusModel.dynamicRadiusBonus(baseRadius, effectiveRadius),
                DynamicRadiusModel.radiusFormulaLabel(),
                aggregateCount,
                0,
                contributingStacks,
                distance,
                DynamicRadiusModel.isActive(distance, effectiveRadius),
                respectsShielding,
                contribution,
                respectsShielding ? "clear" : "not_applicable",
                0,
                1.0D,
                0.0D,
                contribution,
                null,
                null,
                null,
                null,
                null,
                null,
                "exact",
                matchReason);
    }

    public static RadiationSource blockFluidHandlerAggregate(
            ResourceLocation blockId,
            BlockPos position,
            String capabilityContext,
            ResourceLocation fluidId,
            int aggregateAmountMb,
            int contributingStacks,
            double ruleStrength,
            double baseRadius,
            double effectiveRadius,
            double distance,
            boolean respectsShielding,
            double contribution,
            String ruleMatchMode,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.BLOCK_FLUID_HANDLER,
                null,
                fluidId,
                blockId,
                null,
                null,
                0,
                aggregateAmountMb,
                position.immutable(),
                capabilityContext,
                ruleStrength,
                baseRadius,
                baseRadius,
                effectiveRadius,
                DynamicRadiusModel.dynamicRadiusBonus(baseRadius, effectiveRadius),
                DynamicRadiusModel.radiusFormulaLabel(),
                0,
                aggregateAmountMb,
                contributingStacks,
                distance,
                DynamicRadiusModel.isActive(distance, effectiveRadius),
                respectsShielding,
                contribution,
                respectsShielding ? "clear" : "not_applicable",
                0,
                1.0D,
                0.0D,
                contribution,
                null,
                null,
                null,
                null,
                null,
                null,
                ruleMatchMode,
                matchReason);
    }

    public static RadiationSource createTransientItemAggregate(
            ResourceLocation blockId,
            BlockPos position,
            String carrierKind,
            String dataPath,
            ResourceLocation itemId,
            int aggregateCount,
            int contributingStacks,
            double ruleStrength,
            double baseRadius,
            double effectiveRadius,
            double distance,
            boolean respectsShielding,
            double contribution,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.CREATE_TRANSIENT_ITEM,
                itemId,
                null,
                blockId,
                null,
                null,
                aggregateCount,
                0,
                position.immutable(),
                "internal",
                ruleStrength,
                baseRadius,
                baseRadius,
                effectiveRadius,
                DynamicRadiusModel.dynamicRadiusBonus(baseRadius, effectiveRadius),
                DynamicRadiusModel.radiusFormulaLabel(),
                aggregateCount,
                0,
                contributingStacks,
                distance,
                DynamicRadiusModel.isActive(distance, effectiveRadius),
                respectsShielding,
                contribution,
                respectsShielding ? "clear" : "not_applicable",
                0,
                1.0D,
                0.0D,
                contribution,
                carrierKind,
                null,
                null,
                null,
                dataPath,
                "safe_data_path",
                "exact",
                matchReason);
    }

    public static RadiationSource createTransientFluidAggregate(
            ResourceLocation blockId,
            BlockPos position,
            String carrierKind,
            String dataPath,
            ResourceLocation fluidId,
            int aggregateAmountMb,
            int contributingStacks,
            double ruleStrength,
            double baseRadius,
            double effectiveRadius,
            double distance,
            boolean respectsShielding,
            double contribution,
            String ruleMatchMode,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.CREATE_TRANSIENT_FLUID,
                null,
                fluidId,
                blockId,
                null,
                null,
                0,
                aggregateAmountMb,
                position.immutable(),
                "internal",
                ruleStrength,
                baseRadius,
                baseRadius,
                effectiveRadius,
                DynamicRadiusModel.dynamicRadiusBonus(baseRadius, effectiveRadius),
                DynamicRadiusModel.radiusFormulaLabel(),
                0,
                aggregateAmountMb,
                contributingStacks,
                distance,
                DynamicRadiusModel.isActive(distance, effectiveRadius),
                respectsShielding,
                contribution,
                respectsShielding ? "clear" : "not_applicable",
                0,
                1.0D,
                0.0D,
                contribution,
                carrierKind,
                null,
                null,
                null,
                dataPath,
                "safe_data_path",
                ruleMatchMode,
                matchReason);
    }

    public static RadiationSource entityInventoryCarrierItem(
            String carrierSourceKind,
            String extractionMode,
            RadiationSourceType type,
            String carrierEntityType,
            String carrierEntityId,
            BlockPos position,
            ResourceLocation itemId,
            int aggregateCount,
            int contributingStacks,
            double ruleStrength,
            double baseRadius,
            double effectiveRadius,
            double distance,
            boolean respectsShielding,
            double contribution,
            String matchReason) {
        return new RadiationSource(
                type,
                itemId,
                null,
                null,
                null,
                null,
                aggregateCount,
                0,
                position.immutable(),
                null,
                ruleStrength,
                baseRadius,
                baseRadius,
                effectiveRadius,
                DynamicRadiusModel.dynamicRadiusBonus(baseRadius, effectiveRadius),
                DynamicRadiusModel.radiusFormulaLabel(),
                aggregateCount,
                0,
                contributingStacks,
                distance,
                DynamicRadiusModel.isActive(distance, effectiveRadius),
                respectsShielding,
                contribution,
                respectsShielding ? "clear" : "not_applicable",
                0,
                1.0D,
                0.0D,
                contribution,
                null,
                carrierEntityType,
                carrierEntityId,
                carrierSourceKind,
                null,
                extractionMode,
                "exact",
                matchReason);
    }

    public RadiationSource withShielding(ShieldingResult result) {
        return new RadiationSource(
                type,
                itemId,
                fluidId,
                blockId,
                slot,
                tank,
                count,
                amountMb,
                position,
                capabilityContext,
                ruleStrength,
                ruleRadius,
                baseRadius,
                effectiveRadius,
                dynamicRadiusBonus,
                radiusFormula,
                aggregateCount,
                aggregateAmountMb,
                contributingStacks,
                distance,
                activeBecause,
                respectsShielding,
                rawContribution,
                result.shielding(),
                result.shieldingBlocksHit(),
                result.shieldingMultiplier(),
                result.shieldingReduction(),
                result.finalContribution(),
                carrierKind,
                carrierEntityType,
                carrierEntityId,
                carrierSourceKind,
                dataPath,
                extractionMode,
                ruleMatchMode,
                matchReason);
    }

    public RadiationSource withMatchReasonSuffix(String suffix) {
        String nextReason = matchReason;
        if (suffix != null && !suffix.isBlank()) {
            nextReason = (matchReason == null || matchReason.isBlank())
                    ? suffix
                    : matchReason + " " + suffix;
        }
        return new RadiationSource(
                type,
                itemId,
                fluidId,
                blockId,
                slot,
                tank,
                count,
                amountMb,
                position,
                capabilityContext,
                ruleStrength,
                ruleRadius,
                baseRadius,
                effectiveRadius,
                dynamicRadiusBonus,
                radiusFormula,
                aggregateCount,
                aggregateAmountMb,
                contributingStacks,
                distance,
                activeBecause,
                respectsShielding,
                rawContribution,
                shielding,
                shieldingBlocksHit,
                shieldingMultiplier,
                shieldingReduction,
                finalContribution,
                carrierKind,
                carrierEntityType,
                carrierEntityId,
                carrierSourceKind,
                dataPath,
                extractionMode,
                ruleMatchMode,
                nextReason);
    }

    public RadiationSource withExtractionContext(String nestedDataPath, String nestedExtractionMode) {
        String nextDataPath = (nestedDataPath == null || nestedDataPath.isBlank()) ? dataPath : nestedDataPath;
        String nextExtractionMode = (nestedExtractionMode == null || nestedExtractionMode.isBlank())
                ? extractionMode
                : nestedExtractionMode;
        return new RadiationSource(
                type,
                itemId,
                fluidId,
                blockId,
                slot,
                tank,
                count,
                amountMb,
                position,
                capabilityContext,
                ruleStrength,
                ruleRadius,
                baseRadius,
                effectiveRadius,
                dynamicRadiusBonus,
                radiusFormula,
                aggregateCount,
                aggregateAmountMb,
                contributingStacks,
                distance,
                activeBecause,
                respectsShielding,
                rawContribution,
                shielding,
                shieldingBlocksHit,
                shieldingMultiplier,
                shieldingReduction,
                finalContribution,
                carrierKind,
                carrierEntityType,
                carrierEntityId,
                carrierSourceKind,
                nextDataPath,
                nextExtractionMode,
                ruleMatchMode,
                matchReason);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.id());
        if (itemId != null) {
            json.addProperty("itemId", itemId.toString());
        }
        if (fluidId != null) {
            json.addProperty("fluidId", fluidId.toString());
        }
        if (blockId != null) {
            json.addProperty("blockId", blockId.toString());
        }
        if (slot != null) {
            json.addProperty("slot", slot);
        }
        if (tank != null) {
            json.addProperty("tank", tank);
        }
        if (count > 0) {
            json.addProperty("count", count);
        }
        if (amountMb > 0) {
            json.addProperty("amountMb", amountMb);
        }
        if (position != null) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", position.getX());
            pos.addProperty("y", position.getY());
            pos.addProperty("z", position.getZ());
            if (type == RadiationSourceType.BLOCK_ENTITY_INVENTORY) {
                json.add("containerPos", pos);
            } else {
                json.add("position", pos);
            }
        }
        if (capabilityContext != null) {
            json.addProperty("capabilityContext", capabilityContext);
        }
        if (carrierKind != null) {
            json.addProperty("carrierKind", carrierKind);
        }
        if (carrierEntityType != null) {
            json.addProperty("carrierEntityType", carrierEntityType);
        }
        if (carrierEntityId != null) {
            json.addProperty("carrierEntityId", carrierEntityId);
        }
        if (carrierSourceKind != null) {
            json.addProperty("carrierSourceKind", carrierSourceKind);
        }
        if (dataPath != null) {
            json.addProperty("dataPath", dataPath);
        }
        if (extractionMode != null) {
            json.addProperty("extractionMode", extractionMode);
        }
        if (ruleMatchMode != null) {
            json.addProperty("ruleMatchMode", ruleMatchMode);
        }
        json.addProperty("ruleStrength", ruleStrength);
        json.addProperty("ruleRadius", ruleRadius);
        json.addProperty("baseRadius", baseRadius);
        json.addProperty("effectiveRadius", effectiveRadius);
        json.addProperty("dynamicRadiusBonus", dynamicRadiusBonus);
        json.addProperty("radiusFormula", radiusFormula);
        if (aggregateCount > 0) {
            json.addProperty("aggregateCount", aggregateCount);
        }
        if (aggregateAmountMb > 0) {
            json.addProperty("aggregateAmountMb", aggregateAmountMb);
        }
        json.addProperty("contributingStacks", contributingStacks);
        json.addProperty("distance", distance);
        json.addProperty("activeBecause", activeBecause);
        json.addProperty("respectsShielding", respectsShielding);
        json.addProperty("rawContribution", rawContribution);
        json.addProperty("shielding", shielding);
        json.addProperty("shieldingBlocksHit", shieldingBlocksHit);
        json.addProperty("shieldingMultiplier", shieldingMultiplier);
        json.addProperty("shieldingReduction", shieldingReduction);
        json.addProperty("finalContribution", finalContribution);
        json.addProperty("contribution", finalContribution);
        json.addProperty("matchReason", matchReason);
        return json;
    }

    public double contribution() {
        return finalContribution;
    }
}
