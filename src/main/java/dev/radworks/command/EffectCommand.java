package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.WarningBuffer;
import dev.radworks.radiation.effects.EffectStrategyService;
import dev.radworks.registry.RadWorksEffects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public final class EffectCommand {
    private static final String MANUAL_EFFECT_ID = "radworks:radiation";

    private EffectCommand() {
    }

    public static int applySelfManual(CommandSourceStack source) {
        return PerformanceStats.timeCommand("effect_apply", () -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                return applySelfTimed(source, player);
            }
            return failPlayerRequired(source, "apply-self");
        });
    }

    public static int clearSelfManual(CommandSourceStack source) {
        return PerformanceStats.timeCommand("effect_clear", () -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                return clearSelfTimed(source, player);
            }
            return failPlayerRequired(source, "clear-self");
        });
    }

    public static int statusSelf(CommandSourceStack source) {
        return PerformanceStats.timeCommand("effect_status", () -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                return statusSelfTimed(source, player);
            }
            return failPlayerRequired(source, "status");
        });
    }

    private static int applySelfTimed(CommandSourceStack source, ServerPlayer player) {
        MobEffectInstance instance = new MobEffectInstance(RadWorksEffects.RADIATION, 100, 0);
        boolean changed = player.addEffect(instance);
        source.sendSuccess(() -> Component.literal("[RadWorks] Effect apply-self"), false);
        source.sendSuccess(() -> Component.literal("Result: effectId="
                + MANUAL_EFFECT_ID
                + " durationTicks=100 amplifier=0 changed="
                + changed), false);
        return 1;
    }

    private static int clearSelfTimed(CommandSourceStack source, ServerPlayer player) {
        boolean removed = player.removeEffect(RadWorksEffects.RADIATION);
        source.sendSuccess(() -> Component.literal("[RadWorks] Effect clear-self"), false);
        source.sendSuccess(() -> Component.literal("Result: effectId=" + MANUAL_EFFECT_ID + " removed=" + removed), false);
        return 1;
    }

    private static int statusSelfTimed(CommandSourceStack source, ServerPlayer player) {
        MobEffectInstance active = player.getEffect(RadWorksEffects.RADIATION);
        var runtime = EffectStrategyService.resolveRuntimeSelection();

        source.sendSuccess(() -> Component.literal("[RadWorks] Effect status"), false);
        source.sendSuccess(() -> Component.literal("Manual effectId="
                + MANUAL_EFFECT_ID
                + " active="
                + (active != null)
                + (active == null
                        ? ""
                        : " durationTicks=" + active.getDuration() + " amplifier=" + active.getAmplifier())), false);
        source.sendSuccess(() -> Component.literal("Strategy mode="
                + runtime.effectMode().id()
                + " runtimeSelected="
                + valueOrDash(runtime.selectedRuntimeEffectId())), false);
        return 1;
    }

    private static int failPlayerRequired(CommandSourceStack source, String subcommand) {
        String message = "player required; use /radworks effect " + subcommand + " as a player";
        WarningBuffer.add("COMMAND_MISUSE", "effect." + subcommand, message);
        source.sendFailure(Component.literal(message));
        return 0;
    }

    private static String valueOrDash(String value) {
        return value == null ? "-" : value;
    }
}
