package dev.radworks.radiation;

public enum RadiationSourceType {
    PLAYER_INVENTORY("player_inventory"),
    BLOCK("block"),
    BLOCK_ENTITY_INVENTORY("block_entity_inventory"),
    BLOCK_ITEM_HANDLER("block_item_handler");

    private final String id;

    RadiationSourceType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
