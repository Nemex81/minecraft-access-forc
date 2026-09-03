package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record TraversalSafetyContext(
        @NotNull Vec3 playerPos,
        @NotNull AABB playerBoundingBox,
        int playerBaseY,
        @Nullable Vec3 movementIntent,
        boolean hasMovementIntent,
        int dangerDropThreshold,
        @NotNull BlockGetter level
) {
}
