package dev.radworks.radiation;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record SourceOverrideRuleSelector(
        RadiationSourceType sourceType,
        ResourceLocation blockId,
        ResourceLocation itemId,
        ResourceLocation fluidId,
        ResourceLocation carrierEntityType,
        ResourceLocation containerItemId,
        ResourceLocation carrierBlockId,
        RadiationTargetKind targetKind) {
    public boolean hasAnySelector() {
        return sourceType != null
                || blockId != null
                || itemId != null
                || fluidId != null
                || carrierEntityType != null
                || containerItemId != null
                || carrierBlockId != null
                || targetKind != null;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (sourceType != null) {
            json.addProperty("sourceType", sourceType.id());
        }
        if (blockId != null) {
            json.addProperty("blockId", blockId.toString());
        }
        if (itemId != null) {
            json.addProperty("itemId", itemId.toString());
        }
        if (fluidId != null) {
            json.addProperty("fluidId", fluidId.toString());
        }
        if (carrierEntityType != null) {
            json.addProperty("carrierEntityType", carrierEntityType.toString());
        }
        if (containerItemId != null) {
            json.addProperty("containerItemId", containerItemId.toString());
        }
        if (carrierBlockId != null) {
            json.addProperty("carrierBlockId", carrierBlockId.toString());
        }
        if (targetKind != null) {
            json.addProperty("targetKind", targetKind.id());
        }
        return json;
    }
}
