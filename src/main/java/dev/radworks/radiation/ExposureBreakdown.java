package dev.radworks.radiation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.radworks.radiation.armor.ArmorProtectionResult;
import dev.radworks.radiation.effects.EffectPreviewResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExposureBreakdown(
        Instant createdAt,
        String rulesChecksum,
        String playerName,
        UUID playerUuid,
        double totalExposure,
        int matchedStacks,
        List<RadiationSource> sources,
        ArmorProtectionResult armorProtection,
        EffectPreviewResult effectPreview,
        String notes) {
    public JsonObject toJson(int maxSources, String currentRulesChecksum) {
        JsonObject json = new JsonObject();
        json.addProperty("createdAt", createdAt.toString());
        json.addProperty("rulesChecksum", rulesChecksum);
        json.addProperty("stale", !rulesChecksum.equals(currentRulesChecksum));
        json.addProperty("playerName", playerName);
        json.addProperty("playerUuid", playerUuid.toString());
        json.addProperty("totalExposure", totalExposure);
        json.addProperty("matchedSources", sources.size());
        json.addProperty("matchedStacks", matchedStacks);
        json.addProperty("notes", notes);
        json.add("armorProtection", armorProtection.toJson());
        json.add("effectPreview", effectPreview.toJson());

        JsonArray sourceRows = new JsonArray();
        int shown = Math.min(maxSources, sources.size());
        for (int index = 0; index < shown; index++) {
            sourceRows.add(sources.get(index).toJson());
        }
        json.add("sources", sourceRows);
        json.addProperty("sourcesShown", shown);
        json.addProperty("sourcesOmitted", sources.size() - shown);
        return json;
    }
}
