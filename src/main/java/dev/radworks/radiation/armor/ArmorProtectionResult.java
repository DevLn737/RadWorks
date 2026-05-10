package dev.radworks.radiation.armor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

public record ArmorProtectionResult(
        String status,
        List<String> requiredPieces,
        List<String> equippedPieces,
        List<String> missingPieces,
        String protectionSource,
        boolean wouldBlockExposure,
        boolean wouldReduceExposure,
        boolean applied,
        double hypotheticalExposureIfArmorApplied) {
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("status", status);
        json.add("requiredPieces", toJsonArray(requiredPieces));
        json.add("equippedPieces", toJsonArray(equippedPieces));
        json.add("missingPieces", toJsonArray(missingPieces));
        json.addProperty("protectionSource", protectionSource);
        json.addProperty("wouldBlockExposure", wouldBlockExposure);
        json.addProperty("wouldReduceExposure", wouldReduceExposure);
        json.addProperty("applied", applied);
        json.addProperty("hypotheticalExposureIfArmorApplied", hypotheticalExposureIfArmorApplied);
        return json;
    }

    public static ArmorProtectionResult unknown(double currentExposure) {
        List<String> required = List.of("head", "chest", "legs", "feet");
        return new ArmorProtectionResult(
                "unknown",
                required,
                List.of(),
                required,
                "unknown",
                false,
                false,
                false,
                currentExposure);
    }

    private static JsonArray toJsonArray(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
