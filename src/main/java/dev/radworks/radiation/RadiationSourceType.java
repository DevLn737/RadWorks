package dev.radworks.radiation;

public enum RadiationSourceType {
    PLAYER_INVENTORY("player_inventory"),
    BLOCK("block"),
    BLOCK_ENTITY_INVENTORY("block_entity_inventory"),
    BLOCK_ITEM_HANDLER("block_item_handler"),
    BLOCK_FLUID_HANDLER("block_fluid_handler"),
    CREATE_TRANSIENT_ITEM("create_transient_item"),
    CREATE_TRANSIENT_FLUID("create_transient_fluid"),
    ENTITY_DROPPED_ITEM("entity_dropped_item"),
    ENTITY_ITEM_FRAME("entity_item_frame"),
    ENTITY_PLAYER_INVENTORY_AURA("entity_player_inventory_aura");

    private final String id;

    RadiationSourceType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
