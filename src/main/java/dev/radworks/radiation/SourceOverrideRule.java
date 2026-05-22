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
        Double forceStrength,
        Double forceRadius,
        ForceUnitMode forceUnitMode,
        Boolean forceRespectsShielding,
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
        if (forceStrength != null) {
            json.addProperty("forceStrength", forceStrength);
        }
        if (forceRadius != null) {
            json.addProperty("forceRadius", forceRadius);
        }
        if (forceUnitMode != null) {
            json.addProperty("forceUnitMode", forceUnitMode.id());
        }
        if (forceRespectsShielding != null) {
            json.addProperty("forceRespectsShielding", forceRespectsShielding);
        }
        json.addProperty("source", source);
        return json;
    }
}
