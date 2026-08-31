package org.mcaccess.minecraftaccess.features.survival_tracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable record describing a detected survival resource target.
 */
public record SurvivalResourceTarget(
        SurvivalResourceType type,
        @Nullable BlockPos blockPos,
        @Nullable Entity entity,
        String name,
        double distance,
        int diffY,
        String relativeDirection,
        String compassDirection,
        String altitudeDirection
) {
}
