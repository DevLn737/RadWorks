package dev.radworks.radiation.shielding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.radworks.radiation.RadiationRuleValidationResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public final class ShieldingDiagnostics {
    public static final String TAG_ID = "radworks:shielding_blocks";
    public static final String TAG_PATH = "src/main/resources/data/radworks/tags/block/shielding_blocks.json";
    private static final String SOURCE = "shielding:" + TAG_ID;

    private static final List<Candidate> CANDIDATES = List.of(
            new Candidate("minecraft:iron_block", true, "dev_test", null),
            new Candidate("tfmg:raw_lead_block", false, "real_candidate", "tfmg"),
            new Candidate("tfmg:lead_block", false, "real_candidate", "tfmg"),
            new Candidate("tfmg:lead_ore", false, "real_candidate", "tfmg"),
            new Candidate("createnuclear:reinforced_glass", false, "real_candidate", "createnuclear"));

    private ShieldingDiagnostics() {
    }

    public static Report report() {
        List<EntryStatus> entries = new ArrayList<>();
        List<RadiationRuleValidationResult.Issue> warnings = new ArrayList<>();
        List<RadiationRuleValidationResult.Issue> infos = new ArrayList<>();

        for (Candidate candidate : CANDIDATES) {
            EntryStatus status = status(candidate);
            entries.add(status);
            if (status.warning()) {
                warnings.add(new RadiationRuleValidationResult.Issue(
                        "UNKNOWN_REGISTRY_ID",
                        SOURCE,
                        status.id() + " is expected for shielding, but its namespace is loaded and the block is not registered"));
            } else if (status.status().equals("missing_optional_mod")) {
                infos.add(new RadiationRuleValidationResult.Issue(
                        "MISSING_OPTIONAL_MOD",
                        SOURCE,
                        status.id() + " is optional and cannot be tested because mod '" + status.optionalModId() + "' is absent"));
            } else if (status.status().equals("present")) {
                infos.add(new RadiationRuleValidationResult.Issue(
                        status.required() ? "SHIELDING_BLOCK_PRESENT" : "OPTIONAL_SHIELDING_BLOCK_PRESENT",
                        SOURCE,
                        status.id() + " is registered for " + status.role() + " shielding diagnostics"));
            } else {
                infos.add(new RadiationRuleValidationResult.Issue(
                        "OPTIONAL_SHIELDING_BLOCK_NOT_REGISTERED",
                        SOURCE,
                        status.id() + " is optional and not registered in this instance"));
            }
        }

        return new Report(entries, warnings, infos);
    }

    public static JsonObject toJson() {
        Report report = report();
        JsonObject json = new JsonObject();
        json.addProperty("tagId", "#" + TAG_ID);
        json.addProperty("tagPath", TAG_PATH);
        json.add("devTestEntries", entriesToJson(report.entries(), "dev_test"));
        json.add("optionalEntries", entriesToJson(report.entries(), "real_candidate"));

        JsonArray notes = new JsonArray();
        notes.add("Phase 5B: optional external shielding entries use required:false in the block tag.");
        notes.add("Missing TFMG/Create Nuclear blocks are diagnostics INFO in a clean dev environment, not errors.");
        notes.add("minecraft:iron_block remains a dev/test shielding entry, not final balance.");
        json.add("notes", notes);
        return json;
    }

    private static JsonArray entriesToJson(List<EntryStatus> entries, String role) {
        JsonArray array = new JsonArray();
        for (EntryStatus entry : entries) {
            if (entry.role().equals(role)) {
                array.add(entry.toJson());
            }
        }
        return array;
    }

    private static EntryStatus status(Candidate candidate) {
        ResourceLocation id = ResourceLocation.parse(candidate.id());
        boolean registered = BuiltInRegistries.BLOCK.containsKey(id);
        boolean modLoaded = candidate.optionalModId() == null || ModList.get().isLoaded(candidate.optionalModId());
        String status;
        boolean warning = false;
        if (registered) {
            status = "present";
        } else if (candidate.optionalModId() != null && !modLoaded) {
            status = "missing_optional_mod";
        } else {
            status = "not_registered";
            warning = candidate.required() || candidate.optionalModId() != null;
        }
        return new EntryStatus(
                candidate.id(),
                candidate.required(),
                candidate.role(),
                candidate.optionalModId(),
                modLoaded,
                registered,
                status,
                warning);
    }

    private record Candidate(String id, boolean required, String role, String optionalModId) {
    }

    public record Report(
            List<EntryStatus> entries,
            List<RadiationRuleValidationResult.Issue> warnings,
            List<RadiationRuleValidationResult.Issue> infos) {
        public int presentCount() {
            int count = 0;
            for (EntryStatus entry : entries) {
                if (entry.status().equals("present")) {
                    count++;
                }
            }
            return count;
        }

        public int missingOptionalModCount() {
            Set<String> missingMods = new HashSet<>();
            for (EntryStatus entry : entries) {
                if (entry.status().equals("missing_optional_mod")) {
                    missingMods.add(entry.optionalModId());
                }
            }
            return missingMods.size();
        }
    }

    public record EntryStatus(
            String id,
            boolean required,
            String role,
            String optionalModId,
            boolean modLoaded,
            boolean registered,
            String status,
            boolean warning) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            json.addProperty("required", required);
            json.addProperty("role", role);
            if (optionalModId != null) {
                json.addProperty("optionalModId", optionalModId);
            }
            json.addProperty("modLoaded", modLoaded);
            json.addProperty("registered", registered);
            json.addProperty("status", status);
            return json;
        }
    }
}
