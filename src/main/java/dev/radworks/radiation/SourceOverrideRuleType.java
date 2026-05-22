package dev.radworks.radiation;

import java.util.Locale;
import java.util.Optional;

public enum SourceOverrideRuleType {
    EXCLUDE("exclude"),
    CONTAIN("contain"),
    FORCE("force");

    private final String id;

    SourceOverrideRuleType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<SourceOverrideRuleType> fromId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        for (SourceOverrideRuleType value : values()) {
            if (value.id.equals(normalized)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
