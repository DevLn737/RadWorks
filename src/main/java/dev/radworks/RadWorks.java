package dev.radworks;

import dev.radworks.command.RadWorksCommands;
import dev.radworks.radiation.RadiationRulesLoader;
import dev.radworks.registry.RadWorksEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RadWorks.MOD_ID)
public final class RadWorks {
    public static final String MOD_ID = "radworks";
    public static final String MOD_NAME = "RadWorks";

    public RadWorks(IEventBus modEventBus) {
        RadWorksEffects.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RadWorksCommands::register);
        NeoForge.EVENT_BUS.addListener(RadiationRulesLoader::addReloadListener);
    }
}
