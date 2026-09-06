package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SafeDescentCandidate(
        @NotNull BlockPos entryPos,
        @NotNull BlockPos columnTopPos,
        @NotNull BlockPos landingPos,
        @NotNull SafeDescentType type,
        @Nullable Direction wallFacing,
        @NotNull String columnId
) {
    public static SafeDescentCandidate of(
            @NotNull BlockPos entryPos,
            @NotNull BlockPos columnTopPos,
            @NotNull BlockPos landingPos,
            @NotNull SafeDescentType type,
            @Nullable Direction wallFacing
    ) {
        String columnId = type.name().toLowerCase() + ":" + columnTopPos.getX() + "," + columnTopPos.getY() + "," + columnTopPos.getZ();
        return new SafeDescentCandidate(entryPos, columnTopPos, landingPos, type, wallFacing, columnId);
    }
}
