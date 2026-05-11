package dev.radworks.radiation.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

public record EffectStrategyResult(
        String mode,
        String selectedEffectId,
        boolean selectedEffectRegistered,
        String externalEffectId,
        boolean externalEffectPresent,
        String effectMode,
        String selectedRuntimeEffectId,
        boolean selectedRuntimeEffectRegistered,
        String fallbackReason,
        double threshold,
        List<String> notes) {
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", mode);
        json.addProperty("selectedEffectId", selectedEffectId);
        json.addProperty("selectedEffectRegistered", selectedEffectRegistered);
        json.addProperty("externalEffectId", externalEffectId);
        json.addProperty("externalEffectPresent", externalEffectPresent);
        json.addProperty("effectMode", effectMode);
        if (selectedRuntimeEffectId != null) {
            json.addProperty("selectedRuntimeEffectId", selectedRuntimeEffectId);
        }
        json.addProperty("selectedRuntimeEffectRegistered", selectedRuntimeEffectRegistered);
        json.addProperty("fallbackReason", fallbackReason);
        json.addProperty("threshold", threshold);

        JsonArray noteArray = new JsonArray();
        for (String note : notes) {
            noteArray.add(note);
        }
        json.add("notes", noteArray);
        return json;
    }
}
