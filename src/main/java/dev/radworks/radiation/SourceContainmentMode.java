package dev.radworks.radiation;

import java.util.Locale;
import java.util.Optional;

public enum SourceContainmentMode {
    SUPPRESS("suppress"),
    SCALE("scale");

    private final String id;

    SourceContainmentMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<SourceContainmentMode> fromId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        for (SourceContainmentMode value : values()) {
            if (value.id.equals(normalized)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
