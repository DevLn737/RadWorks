package dev.radworks.radiation.effects;

import com.google.gson.JsonObject;

public record EffectPreviewResult(
        boolean wouldApply,
        String reason,
        int durationTicks,
        int amplifier,
        boolean blockedByArmor,
        boolean applied,
        double threshold,
        double exposureUsed,
        String armorStatus) {
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("wouldApply", wouldApply);
        json.addProperty("reason", reason);
        json.addProperty("durationTicks", durationTicks);
        json.addProperty("amplifier", amplifier);
        json.addProperty("blockedByArmor", blockedByArmor);
        json.addProperty("applied", applied);
        json.addProperty("threshold", threshold);
        json.addProperty("exposureUsed", exposureUsed);
        json.addProperty("armorStatus", armorStatus);
        return json;
    }
}
