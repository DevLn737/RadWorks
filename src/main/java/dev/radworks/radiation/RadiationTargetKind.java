package dev.radworks.radiation;

public enum RadiationTargetKind {
    PLAYER("player"),
    MOB("mob"),
    ARMOR_STAND("armor_stand"),
    OTHER_LIVING("other_living");

    private final String id;

    RadiationTargetKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
