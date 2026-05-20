package dev.radworks.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

public record RadiationTargetContext(
        ServerLevel level,
        LivingEntity target,
        Vec3 targetPos,
        BlockPos targetBlockPos,
        boolean includePlayerInventory,
        boolean includeSelfEntityInventory,
        boolean applyShielding,
        RadiationTargetKind targetKind) {
    public static RadiationTargetContext forPlayer(ServerPlayer player) {
        return new RadiationTargetContext(
                player.serverLevel(),
                player,
                player.position(),
                player.blockPosition(),
                true,
                false,
                true,
                RadiationTargetKind.PLAYER);
    }

    public static RadiationTargetContext forLivingEntity(
            ServerLevel level,
            LivingEntity target,
            boolean includePlayerInventory,
            boolean includeSelfEntityInventory,
            boolean applyShielding) {
        return new RadiationTargetContext(
                level,
                target,
                target.position(),
                target.blockPosition(),
                includePlayerInventory,
                includeSelfEntityInventory,
                applyShielding,
                classify(target));
    }

    private static RadiationTargetKind classify(LivingEntity target) {
        if (target instanceof ServerPlayer) {
            return RadiationTargetKind.PLAYER;
        }
        if (target instanceof ArmorStand) {
            return RadiationTargetKind.ARMOR_STAND;
        }
        if (target instanceof Mob) {
            return RadiationTargetKind.MOB;
        }
        return RadiationTargetKind.OTHER_LIVING;
    }
}
