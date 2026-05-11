package dev.radworks.radiation;

import java.util.Optional;

public enum RadiationRuleProfile {
    ALWAYS("always"),
    BETA("beta"),
    DEV("dev");

    private final String id;

    RadiationRuleProfile(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<RadiationRuleProfile> fromId(String id) {
        for (RadiationRuleProfile profile : values()) {
            if (profile.id.equals(id)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }
}
