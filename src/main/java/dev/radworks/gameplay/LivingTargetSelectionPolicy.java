package dev.radworks.gameplay;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

final class LivingTargetSelectionPolicy {
    private LivingTargetSelectionPolicy() {
    }

    static String skipReason(
            LivingEntity target,
            boolean applyEffectToMobs,
            boolean applyEffectToArmorStands) {
        return skipReason(
                target instanceof Player,
                target instanceof ArmorStand,
                applyEffectToMobs,
                applyEffectToArmorStands);
    }

    static String skipReason(
            boolean isPlayer,
            boolean isArmorStand,
            boolean applyEffectToMobs,
            boolean applyEffectToArmorStands) {
        if (isPlayer) {
            return "target_skipped";
        }
        if (isArmorStand && !applyEffectToArmorStands) {
            return "target_skipped";
        }
        if (!applyEffectToMobs) {
            return "target_skipped";
        }
        return null;
    }

    static boolean isCapped(int eligibleProcessed, int maxLivingTargetsPerScan) {
        return eligibleProcessed >= maxLivingTargetsPerScan;
    }
}
