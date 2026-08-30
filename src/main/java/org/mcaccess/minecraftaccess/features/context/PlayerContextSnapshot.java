package org.mcaccess.minecraftaccess.features.context;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Immutable snapshot of the player's 5-axis state at a specific tick.
 * Used by ContextualMentor and AcademyManager to evaluate conditions.
 */
public record PlayerContextSnapshot(
        Vec3 pos,
        BlockPos blockPos,
        String biome,
        int blockLight,
        boolean isStuckAgainstWall,
        boolean isMoving,
        boolean isSneaking,
        boolean isSprinting,
        boolean isFlying,
        boolean isInWater,
        HitResult crosshairTarget,
        double crosshairDistance,
        int woodLogsCount,
        int planksCount,
        int cobblestoneCount,
        int torchesCount,
        int foodCount,
        int craftingTableCount,
        float health,
        float maxHealth,
        int foodLevel,
        long timeOfDay,
        int nearbyHostilesCount,
        GameType gameMode,
        int idleTicks
) {
    public boolean isSurvivalOrAdventure() {
        return gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE;
    }

    public boolean isCreative() {
        return gameMode == GameType.CREATIVE;
    }

    public boolean isSpectator() {
        return gameMode == GameType.SPECTATOR;
    }
}
