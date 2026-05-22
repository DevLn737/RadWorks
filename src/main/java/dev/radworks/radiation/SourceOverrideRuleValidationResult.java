package dev.radworks.radiation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public final class SourceOverrideRuleValidationResult {
    private final List<Issue> errors = new ArrayList<>();
    private final List<Issue> warnings = new ArrayList<>();
    private final List<Issue> infos = new ArrayList<>();

    public void error(String category, String source, String message) {
        errors.add(new Issue(category, source, message));
    }

    public void warning(String category, String source, String message) {
        warnings.add(new Issue(category, source, message));
    }

    public void info(String category, String source, String message) {
        infos.add(new Issue(category, source, message));
    }

    public List<Issue> errors() {
        return List.copyOf(errors);
    }

    public List<Issue> warnings() {
        return List.copyOf(warnings);
    }

    public List<Issue> infos() {
        return List.copyOf(infos);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.add("errors", toJsonArray(errors));
        json.add("warnings", toJsonArray(warnings));
        json.add("infos", toJsonArray(infos));
        return json;
    }

    private static JsonArray toJsonArray(List<Issue> issues) {
        JsonArray array = new JsonArray();
        for (Issue issue : issues) {
            array.add(issue.toJson());
        }
        return array;
    }

    public record Issue(String category, String source, String message) {
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("category", category);
            json.addProperty("source", source);
            json.addProperty("message", message);
            return json;
        }

        public String summary() {
            return category + " " + source + ": " + message;
        }
    }
}
