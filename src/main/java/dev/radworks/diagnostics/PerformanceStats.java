package dev.radworks.diagnostics;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PerformanceStats {
    private static final Map<String, OperationStats> STATS = new LinkedHashMap<>();

    static {
        STATS.put("validate", new OperationStats());
        STATS.put("exposure", new OperationStats());
        STATS.put("sources", new OperationStats());
        STATS.put("effect_apply", new OperationStats());
        STATS.put("effect_clear", new OperationStats());
        STATS.put("effect_status", new OperationStats());
        STATS.put("gameplay_auto_apply", new OperationStats());
        STATS.put("blockScan", new OperationStats());
        STATS.put("blockEntityInventoryScan", new OperationStats());
        STATS.put("itemHandlerScan", new OperationStats());
        STATS.put("fluidHandlerScan", new OperationStats());
        STATS.put("shielding", new OperationStats());
        STATS.put("dump", new OperationStats());
    }

    private PerformanceStats() {
    }

    public static int timeCommand(String operation, Supplier<Integer> command) {
        long start = System.nanoTime();
        try {
            return command.get();
        } finally {
            record(operation, elapsedMillis(start));
        }
    }

    public static <T> T timeValue(String operation, Supplier<T> supplier) {
        long start = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            record(operation, elapsedMillis(start));
        }
    }

    public static JsonObject toJson() {
        JsonObject root = new JsonObject();
        synchronized (STATS) {
            for (Map.Entry<String, OperationStats> entry : STATS.entrySet()) {
                root.add(entry.getKey(), entry.getValue().toJson());
            }
        }
        return root;
    }

    private static void record(String operation, double millis) {
        synchronized (STATS) {
            STATS.computeIfAbsent(operation, ignored -> new OperationStats()).record(millis);
        }
    }

    private static double elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000.0D;
    }

    private static final class OperationStats {
        private double lastMillis;
        private long count;
        private double totalMillis;
        private double maxMillis;

        private void record(double millis) {
            lastMillis = millis;
            count++;
            totalMillis += millis;
            maxMillis = Math.max(maxMillis, millis);
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("lastMillis", lastMillis);
            json.addProperty("count", count);
            json.addProperty("averageMillis", count == 0 ? 0.0D : totalMillis / count);
            json.addProperty("maxMillis", maxMillis);
            return json;
        }
    }
}
