package dev.radworks;

import dev.radworks.command.RadWorksCommands;
import dev.radworks.config.RadWorksConfig;
import dev.radworks.gameplay.RadiationGameplayService;
import dev.radworks.radiation.RadiationRulesLoader;
import dev.radworks.registry.RadWorksEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RadWorks.MOD_ID)
public final class RadWorks {
    public static final String MOD_ID = "radworks";
    public static final String MOD_NAME = "RadWorks";

    public RadWorks(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, RadWorksConfig.SPEC);
        RadWorksEffects.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RadWorksCommands::register);
        NeoForge.EVENT_BUS.addListener(RadiationRulesLoader::addReloadListener);
        NeoForge.EVENT_BUS.addListener(RadiationGameplayService::onPlayerTickPost);
    }
}
