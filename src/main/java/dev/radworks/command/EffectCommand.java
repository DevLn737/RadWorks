package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.WarningBuffer;
import dev.radworks.radiation.ExposureBreakdown;
import dev.radworks.radiation.ExposureEngine;
import dev.radworks.radiation.effects.EffectPreviewResult;
import dev.radworks.radiation.effects.EffectStrategyResult;
import dev.radworks.radiation.effects.EffectStrategyService;
import dev.radworks.registry.RadWorksEffects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public final class EffectCommand {
    private EffectCommand() {
    }

    public static int applySelf(CommandSourceStack source) {
        return PerformanceStats.timeCommand("effect_apply", () -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                return applyTimed(source, player);
            }
            return failPlayerRequired(source, "apply");
        });
    }

    public static int apply(CommandSourceStack source, ServerPlayer player) {
        return PerformanceStats.timeCommand("effect_apply", () -> applyTimed(source, player));
    }

    public static int clearSelf(CommandSourceStack source) {
        return PerformanceStats.timeCommand("effect_clear", () -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                return clearTimed(source, player);
            }
            return failPlayerRequired(source, "clear");
        });
    }

    public static int clear(CommandSourceStack source, ServerPlayer player) {
        return PerformanceStats.timeCommand("effect_clear", () -> clearTimed(source, player));
    }

    public static int statusSelf(CommandSourceStack source) {
        return PerformanceStats.timeCommand("effect_status", () -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                return statusTimed(source, player);
            }
            return failPlayerRequired(source, "status");
        });
    }

    public static int status(CommandSourceStack source, ServerPlayer player) {
        return PerformanceStats.timeCommand("effect_status", () -> statusTimed(source, player));
    }

    private static int applyTimed(CommandSourceStack source, ServerPlayer player) {
        EffectStrategyResult strategy = EffectStrategyService.strategy();
        ExposureBreakdown breakdown = ExposureEngine.calculate(player);
        EffectPreviewResult preview = breakdown.effectPreview();

        if (!strategy.selectedEffectRegistered()) {
            String message = "selected effect is not registered: " + strategy.selectedEffectId();
            WarningBuffer.add("EFFECT_NOT_REGISTERED", "effect.apply", message);
            source.sendFailure(Component.literal("RadWorks effect apply blocked for "
                    + player.getGameProfile().getName()
                    + ": "
                    + message));
            return 0;
        }

        if (!preview.wouldApply()) {
            WarningBuffer.add("EFFECT_APPLY_BLOCKED", "effect.apply", "reason=" + preview.reason());
            source.sendFailure(Component.literal("RadWorks effect apply blocked for "
                    + player.getGameProfile().getName()
                    + ": reason="
                    + preview.reason()
                    + " blockedByArmor="
                    + preview.blockedByArmor()
                    + " exposureUsed="
                    + preview.exposureUsed()
                    + " threshold="
                    + preview.threshold()));
            return 0;
        }

        MobEffectInstance instance = new MobEffectInstance(
                RadWorksEffects.RADIATION,
                preview.durationTicks(),
                preview.amplifier());
        boolean changed = player.addEffect(instance);
        source.sendSuccess(() -> Component.literal("RadWorks effect applied to "
                + player.getGameProfile().getName()
                + ": effectId="
                + strategy.selectedEffectId()
                + " durationTicks="
                + preview.durationTicks()
                + " amplifier="
                + preview.amplifier()
                + " changed="
                + changed
                + " previewApplied="
                + preview.applied()), false);
        return 1;
    }

    private static int clearTimed(CommandSourceStack source, ServerPlayer player) {
        boolean removed = player.removeEffect(RadWorksEffects.RADIATION);
        source.sendSuccess(() -> Component.literal("RadWorks effect clear for "
                + player.getGameProfile().getName()
                + ": effectId="
                + EffectStrategyService.SELECTED_EFFECT_ID
                + " removed="
                + removed), false);
        return 1;
    }

    private static int statusTimed(CommandSourceStack source, ServerPlayer player) {
        EffectStrategyResult strategy = EffectStrategyService.strategy();
        ExposureBreakdown breakdown = ExposureEngine.calculate(player);
        EffectPreviewResult preview = breakdown.effectPreview();
        MobEffectInstance active = player.getEffect(RadWorksEffects.RADIATION);

        StringBuilder status = new StringBuilder();
        status.append("RadWorks effect status for ")
                .append(player.getGameProfile().getName())
                .append(": effectId=")
                .append(strategy.selectedEffectId())
                .append(" selectedEffectRegistered=")
                .append(strategy.selectedEffectRegistered())
                .append(" active=")
                .append(active != null);

        if (active != null) {
            status.append(" durationTicks=").append(active.getDuration());
            status.append(" amplifier=").append(active.getAmplifier());
        }

        status.append(" wouldApply=").append(preview.wouldApply());
        status.append(" reason=").append(preview.reason());
        status.append(" blockedByArmor=").append(preview.blockedByArmor());
        status.append(" applied=").append(preview.applied());

        source.sendSuccess(() -> Component.literal(status.toString()), false);
        return 1;
    }

    private static int failPlayerRequired(CommandSourceStack source, String subcommand) {
        String message = "player required; use /radworks effect " + subcommand + " <player>";
        WarningBuffer.add("COMMAND_MISUSE", "effect." + subcommand, message);
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
