package dev.radworks.diagnostics;

import com.google.gson.JsonObject;

public final class DiagnosticsState {
    private static volatile boolean debugEnabled;

    private DiagnosticsState() {
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", debugEnabled);
        return json;
    }
}
