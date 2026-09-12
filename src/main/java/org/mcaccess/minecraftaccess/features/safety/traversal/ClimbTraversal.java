package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Record immutabile che descrive una transizione verticale continua lungo una colonna arrampicabile (Contratto D0).
 */
public record ClimbTraversal(
        @NotNull Direction.AxisDirection direction, // POSITIVE = UP, NEGATIVE = DOWN
        @NotNull ClimbableGeometry.ClimbType climbType,
        @NotNull BlockPos entryPos,
        @NotNull BlockPos columnTopPos,
        @NotNull BlockPos columnBottomPos,
        @NotNull BlockPos landingPos,
        @Nullable Direction wallFacing,
        @Nullable BlockPos trapdoorPos,
        @NotNull String columnId
) {
    public boolean isAscent() {
        return direction == Direction.AxisDirection.POSITIVE;
    }

    public boolean isDescent() {
        return direction == Direction.AxisDirection.NEGATIVE;
    }

    public int getHeight() {
        return columnTopPos.getY() - columnBottomPos.getY() + 1;
    }

    public static ClimbTraversal of(
            @NotNull Direction.AxisDirection direction,
            @NotNull ClimbableGeometry.ClimbType climbType,
            @NotNull BlockPos entryPos,
            @NotNull BlockPos columnTopPos,
            @NotNull BlockPos columnBottomPos,
            @NotNull BlockPos landingPos,
            @Nullable Direction wallFacing,
            @Nullable BlockPos trapdoorPos
    ) {
        String colId = climbType.name().toLowerCase() + ":" + columnTopPos.getX() + "," + columnBottomPos.getY() + ".." + columnTopPos.getY() + "," + columnTopPos.getZ();
        return new ClimbTraversal(direction, climbType, entryPos, columnTopPos, columnBottomPos, landingPos, wallFacing, trapdoorPos, colId);
    }
}
