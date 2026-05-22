package dev.radworks.command;

import dev.radworks.diagnostics.PerformanceStats;
import dev.radworks.diagnostics.SourceScanSummary;
import dev.radworks.diagnostics.WarningBuffer;
import dev.radworks.gameplay.RadiationGameplayService;
import dev.radworks.radiation.ExposureBreakdown;
import dev.radworks.radiation.ExposureEngine;
import dev.radworks.radiation.RadiationSource;
import dev.radworks.radiation.effects.EffectStrategyService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ExposureCommand {
    private static final int CHAT_SOURCE_LIMIT = 10;

    private ExposureCommand() {
    }

    public static int runSelf(CommandSourceStack source) {
        return PerformanceStats.timeCommand("exposure", () -> runSelfTimed(source));
    }

    private static int runSelfTimed(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return runTimed(source, player);
        }

        String message = "player required; use /radworks exposure <player>";
        WarningBuffer.add("COMMAND_MISUSE", "exposure", message);
        source.sendFailure(Component.literal(message));
        return 0;
    }

    public static int run(CommandSourceStack source, ServerPlayer player) {
        return PerformanceStats.timeCommand("exposure", () -> runTimed(source, player));
    }

    private static int runTimed(CommandSourceStack source, ServerPlayer player) {
        ExposureBreakdown breakdown = ExposureEngine.calculate(player);
        var runtime = EffectStrategyService.resolveRuntimeSelection();
        source.sendSuccess(() -> Component.literal("[RadWorks] Exposure"), false);
        source.sendSuccess(() -> Component.literal("Total: "
                + breakdown.totalExposure()
                + " threshold="
                + breakdown.effectPreview().threshold()
                + " matchedSources="
                + breakdown.sources().size()), false);
        source.sendSuccess(() -> Component.literal("Armor: status="
                + breakdown.armorProtection().status()
                + " blocked="
                + breakdown.armorProtection().wouldBlockExposure()
                + " applied="
                + breakdown.armorProtection().applied()), false);
        source.sendSuccess(() -> Component.literal("Effect: mode="
                + runtime.effectMode().id()
                + " selected="
                + valueOrDash(runtime.selectedRuntimeEffectId())
                + " wouldApply="
                + breakdown.effectPreview().wouldApply()
                + " reason="
                + breakdown.effectPreview().reason()
                + " applied="
                + breakdown.effectPreview().applied()), false);
        source.sendSuccess(() -> Component.literal("Sources:"), false);

        int shown = Math.min(CHAT_SOURCE_LIMIT, breakdown.sources().size());
        for (int index = 0; index < shown; index++) {
            RadiationSource radiationSource = breakdown.sources().get(index);
            source.sendSuccess(() -> Component.literal("- " + sourceRow(radiationSource)), false);
        }

        if (breakdown.sources().size() > shown) {
            source.sendSuccess(() -> Component.literal("... and " + (breakdown.sources().size() - shown) + " more (see /radworks dump)"), false);
        }
        SourceScanSummary.updateOutputBounds(shown, breakdown.sources().size() - shown);
        source.sendSuccess(() -> Component.literal("Output: sourcesShown="
                + shown
                + " sourcesOmitted="
                + (breakdown.sources().size() - shown)), false);
        source.sendSuccess(() -> Component.literal("Gameplay: " + RadiationGameplayService.compactStatus(player)), false);
        source.sendSuccess(() -> Component.literal("Note: detailed fields are in /radworks dump"), false);
        return 1;
    }

    private static String sourceRow(RadiationSource source) {
        StringBuilder row = new StringBuilder();
        row.append("type=").append(source.type().id());
        if (source.itemId() != null) {
            row.append(" itemId=").append(source.itemId());
        }
        if (source.fluidId() != null) {
            row.append(" fluidId=").append(source.fluidId());
        }
        if (source.blockId() != null) {
            row.append(" blockId=").append(source.blockId());
        }
        if (source.position() != null) {
            if (source.type() == dev.radworks.radiation.RadiationSourceType.BLOCK_ENTITY_INVENTORY) {
                row.append(" containerPos=");
            } else {
                row.append(" position=");
            }
            row.append(source.position().getX())
                    .append(",")
                    .append(source.position().getY())
                    .append(",")
                    .append(source.position().getZ());
        }
        if (source.capabilityContext() != null) {
            row.append(" capabilityContext=").append(source.capabilityContext());
        }
        if (source.carrierKind() != null) {
            row.append(" carrierKind=").append(source.carrierKind());
        }
        if (source.carrierSourceKind() != null) {
            row.append(" carrierSourceKind=").append(source.carrierSourceKind());
        }
        if (source.carrierEntityType() != null) {
            row.append(" carrierEntityType=").append(source.carrierEntityType());
        }
        if (source.carrierEntityId() != null) {
            row.append(" carrierEntityId=").append(source.carrierEntityId());
        }
        if (source.dataPath() != null) {
            row.append(" dataPath=").append(source.dataPath());
        }
        if (source.extractionMode() != null) {
            row.append(" extractionMode=").append(source.extractionMode());
        }
        if (source.nested()) {
            row.append(" nested=true");
            if (source.nestedDepth() > 0) {
                row.append(" nestedDepth=").append(source.nestedDepth());
            }
            if (source.containerItemId() != null) {
                row.append(" containerItemId=").append(source.containerItemId());
            }
            if (source.containerPath() != null) {
                row.append(" containerPath=").append(source.containerPath());
            }
        }
        if (source.slot() != null) {
            row.append(" slot=").append(source.slot());
        }
        if (source.tank() != null) {
            row.append(" tank=").append(source.tank());
        }
        if (source.count() > 0) {
            row.append(" count=").append(source.count());
        }
        if (source.amountMb() > 0) {
            row.append(" amountMb=").append(source.amountMb());
        }
        if (source.aggregateCount() > 0) {
            row.append(" aggregateCount=").append(source.aggregateCount());
        }
        if (source.aggregateAmountMb() > 0) {
            row.append(" aggregateAmountMb=").append(source.aggregateAmountMb());
        }
        if (source.contributingStacks() > 0) {
            row.append(" contributingStacks=").append(source.contributingStacks());
        }
        row.append(" distance=").append(round(source.distance()));
        row.append(" effectiveRadius=").append(round(source.effectiveRadius()));
        row.append(" strength=").append(source.ruleStrength());
        row.append(" final=").append(source.finalContribution());
        if (source.overrideMode() != null && !"none".equals(source.overrideMode())) {
            row.append(" overrideMode=").append(source.overrideMode());
            if (source.overrideRuleId() != null) {
                row.append(" overrideRuleId=").append(source.overrideRuleId());
            }
        }
        if (source.ruleMatchMode() != null && !source.ruleMatchMode().isBlank()) {
            row.append(" ruleMatch=").append(source.ruleMatchMode());
        }
        row.append(" shielding=").append(source.shielding());
        return row.toString();
    }

    private static String valueOrDash(String value) {
        return value == null ? "-" : value;
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
