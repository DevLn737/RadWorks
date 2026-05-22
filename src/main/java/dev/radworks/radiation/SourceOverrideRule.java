package dev.radworks.radiation;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record SourceOverrideRule(
        ResourceLocation id,
        boolean enabled,
        SourceOverrideRuleType type,
        SourceOverrideRuleSelector selectors,
        boolean required,
        String optionalModId,
        String description,
        SourceContainmentMode containmentMode,
        Double containmentMultiplier,
        String source) {
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("enabled", enabled);
        json.addProperty("type", type.id());
        json.add("selectors", selectors.toJson());
        json.addProperty("required", required);
        if (optionalModId != null) {
            json.addProperty("optionalModId", optionalModId);
        }
        if (description != null && !description.isBlank()) {
            json.addProperty("description", description);
        }
        if (containmentMode != null) {
            json.addProperty("mode", containmentMode.id());
        }
        if (containmentMultiplier != null) {
            json.addProperty("multiplier", containmentMultiplier);
        }
        json.addProperty("source", source);
        return json;
    }
}
