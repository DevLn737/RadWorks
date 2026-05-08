package dev.radworks.radiation;

public enum RadiationSourceType {
    PLAYER_INVENTORY("player_inventory");

    private final String id;

    RadiationSourceType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
