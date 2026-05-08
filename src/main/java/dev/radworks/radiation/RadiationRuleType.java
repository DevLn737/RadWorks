package dev.radworks.radiation;

import java.util.Locale;
import java.util.Optional;

public enum RadiationRuleType {
    ITEM("item"),
    BLOCK("block"),
    FLUID("fluid");

    private final String id;

    RadiationRuleType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<RadiationRuleType> fromId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (RadiationRuleType type : values()) {
            if (type.id.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
