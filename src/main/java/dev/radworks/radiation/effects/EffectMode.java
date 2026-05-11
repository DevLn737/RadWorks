package dev.radworks.radiation.effects;

public enum EffectMode {
    OWN("own"),
    EXTERNAL_IF_PRESENT("external_if_present"),
    EXTERNAL_ONLY("external_only"),
    DISABLED("disabled");

    private final String id;

    EffectMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
