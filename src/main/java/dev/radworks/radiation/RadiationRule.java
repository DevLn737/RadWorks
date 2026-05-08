package dev.radworks.radiation;

import net.minecraft.resources.ResourceLocation;

public record RadiationRule(
        RadiationRuleType type,
        ResourceLocation id,
        double strength,
        double radius,
        boolean respectsShielding,
        boolean enabled,
        String comment,
        String source) {
    public String key() {
        return type.id() + ":" + id;
    }
}
