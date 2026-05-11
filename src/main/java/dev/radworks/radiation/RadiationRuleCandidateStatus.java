package dev.radworks.radiation;

import com.google.gson.JsonObject;

public record RadiationRuleCandidateStatus(
        String type,
        String id,
        String profile,
        boolean required,
        String optionalModId,
        String role,
        boolean active,
        String status,
        String severity) {
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("id", id);
        json.addProperty("profile", profile);
        json.addProperty("required", required);
        if (optionalModId != null && !optionalModId.isBlank()) {
            json.addProperty("optionalModId", optionalModId);
        }
        if (role != null && !role.isBlank()) {
            json.addProperty("role", role);
        }
        json.addProperty("active", active);
        json.addProperty("status", status);
        json.addProperty("severity", severity);
        return json;
    }
}
