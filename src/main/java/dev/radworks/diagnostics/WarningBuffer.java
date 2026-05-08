package dev.radworks.diagnostics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class WarningBuffer {
    private static final int MAX_ENTRIES = 100;
    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    private WarningBuffer() {
    }

    public static synchronized void add(String category, String source, String message) {
        while (ENTRIES.size() >= MAX_ENTRIES) {
            ENTRIES.removeFirst();
        }
        ENTRIES.addLast(new Entry(Instant.now(), category, source, message));
    }

    public static synchronized JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (Entry entry : new ArrayList<>(ENTRIES)) {
            array.add(entry.toJson());
        }
        return array;
    }

    public static synchronized List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    public record Entry(Instant createdAt, String category, String source, String message) {
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("createdAt", createdAt.toString());
            json.addProperty("category", category);
            json.addProperty("source", source);
            json.addProperty("message", message);
            return json;
        }
    }
}
