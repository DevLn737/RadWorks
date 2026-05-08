package dev.radworks.radiation;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record RadiationSource(
        RadiationSourceType type,
        ResourceLocation itemId,
        ResourceLocation blockId,
        String slot,
        int count,
        BlockPos position,
        double ruleStrength,
        double ruleRadius,
        double distance,
        String shielding,
        double contribution,
        String matchReason) {
    public static RadiationSource playerInventory(
            ResourceLocation itemId,
            String slot,
            int count,
            double ruleStrength,
            double ruleRadius,
            double contribution,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.PLAYER_INVENTORY,
                itemId,
                null,
                slot,
                count,
                null,
                ruleStrength,
                ruleRadius,
                0.0D,
                "not_applied",
                contribution,
                matchReason);
    }

    public static RadiationSource block(
            ResourceLocation blockId,
            BlockPos position,
            double ruleStrength,
            double ruleRadius,
            double distance,
            double contribution,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.BLOCK,
                null,
                blockId,
                null,
                0,
                position.immutable(),
                ruleStrength,
                ruleRadius,
                distance,
                "not_applied",
                contribution,
                matchReason);
    }

    public static RadiationSource blockEntityInventory(
            ResourceLocation blockId,
            BlockPos containerPos,
            String slot,
            ResourceLocation itemId,
            int count,
            double ruleStrength,
            double ruleRadius,
            double distance,
            double contribution,
            String matchReason) {
        return new RadiationSource(
                RadiationSourceType.BLOCK_ENTITY_INVENTORY,
                itemId,
                blockId,
                slot,
                count,
                containerPos.immutable(),
                ruleStrength,
                ruleRadius,
                distance,
                "not_applied",
                contribution,
                matchReason);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.id());
        if (itemId != null) {
            json.addProperty("itemId", itemId.toString());
        }
        if (blockId != null) {
            json.addProperty("blockId", blockId.toString());
        }
        if (slot != null) {
            json.addProperty("slot", slot);
        }
        if (count > 0) {
            json.addProperty("count", count);
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
        json.addProperty("ruleStrength", ruleStrength);
        json.addProperty("ruleRadius", ruleRadius);
        json.addProperty("distance", distance);
        json.addProperty("shielding", shielding);
        json.addProperty("contribution", contribution);
        json.addProperty("matchReason", matchReason);
        return json;
    }
}
