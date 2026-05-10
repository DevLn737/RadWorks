package dev.radworks.registry;

import dev.radworks.RadWorks;
import dev.radworks.radiation.effects.RadiationMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RadWorksEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, RadWorks.MOD_ID);

    public static final DeferredHolder<MobEffect, RadiationMobEffect> RADIATION = EFFECTS.register(
            "radiation",
            RadiationMobEffect::new);

    private RadWorksEffects() {
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
