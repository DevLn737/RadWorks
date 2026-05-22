package dev.radworks.radiation;

import java.util.Locale;
import java.util.Optional;

public enum ForceUnitMode {
    ITEM_COUNT("item_count"),
    FLUID_MB("fluid_mb"),
    BLOCK("block"),
    FIXED("fixed");

    private final String id;

    ForceUnitMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<ForceUnitMode> fromId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        for (ForceUnitMode value : values()) {
            if (value.id.equals(normalized)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}

