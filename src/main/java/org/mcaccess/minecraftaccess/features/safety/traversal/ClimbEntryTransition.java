package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcaccess.minecraftaccess.features.autowalk.RouteSegment;

/**
 * Record immutabile che descrive una transizione d'ingresso in una colonna arrampicabile (Contratto D0).
 * Modella la transizione fisica Surface -> Aperture -> Entry per l'AutoWalk.
 */
public record ClimbEntryTransition(
        @NotNull BlockPos surfacePos,
        @NotNull BlockPos aperturePos,
        @NotNull BlockPos entryPos,
        @NotNull Direction approachDirection,
        @NotNull ClimbTraversal traversal,
        @NotNull RouteSegment.ClimbLeg climbLeg,
        @NotNull PassageRequirement passageRequirement,
        @Nullable BlockPos trapdoorPos,
        @NotNull String columnId
) {
    public enum PassageRequirement {
        CLEAR,
        OPEN_TRAPDOOR,
        OPENABLE_WOODEN_TRAPDOOR
    }

    public static ClimbEntryTransition ofTopDescent(
            @NotNull BlockPos surfacePos,
            @NotNull BlockPos aperturePos,
            @NotNull BlockPos entryPos,
            @NotNull Direction approachDirection,
            @NotNull ClimbTraversal traversal,
            @NotNull PassageRequirement passageRequirement,
            @Nullable BlockPos trapdoorPos
    ) {
        return new ClimbEntryTransition(
                surfacePos,
                aperturePos,
                entryPos,
                approachDirection,
                traversal,
                RouteSegment.ClimbLeg.MOUNT,
                passageRequirement,
                trapdoorPos,
                traversal.columnId()
        );
    }
}