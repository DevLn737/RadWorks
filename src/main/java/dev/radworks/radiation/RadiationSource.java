package dev.radworks.radiation;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record RadiationSource(
        RadiationSourceType type,
        ResourceLocation itemId,
        String slot,
        int count,
        double ruleStrength,
        double ruleRadius,
        double distance,
        String shielding,
        double contribution) {
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.id());
        json.addProperty("itemId", itemId.toString());
        json.addProperty("slot", slot);
        json.addProperty("count", count);
        json.addProperty("ruleStrength", ruleStrength);
        json.addProperty("ruleRadius", ruleRadius);
        json.addProperty("distance", distance);
        json.addProperty("shielding", shielding);
        json.addProperty("contribution", contribution);
        return json;
    }
}
