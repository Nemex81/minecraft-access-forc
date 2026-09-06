package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record TraversalSafetyResult(
        @NotNull TraversalSafetyStatus status,
        @Nullable SafeDescentCandidate candidate,
        @Nullable BlockPos dangerPos,
        int dangerDepth,
        @NotNull String diagnosticReason
) {
    public static TraversalSafetyResult safeDescent(@NotNull SafeDescentCandidate candidate, @NotNull String reason) {
        return new TraversalSafetyResult(TraversalSafetyStatus.SAFE_DESCENT_AVAILABLE, candidate, null, 0, reason);
    }

    public static TraversalSafetyResult dangerousDrop(@NotNull BlockPos dangerPos, int depth, @NotNull String reason) {
        return new TraversalSafetyResult(TraversalSafetyStatus.DANGEROUS_DROP, null, dangerPos, depth, reason);
    }

    public static TraversalSafetyResult ambiguousOrUnsafe(@NotNull String reason) {
        return new TraversalSafetyResult(TraversalSafetyStatus.AMBIGUOUS_OR_UNSAFE_DESCENT, null, null, 0, reason);
    }

    public static TraversalSafetyResult notApplicable(@NotNull String reason) {
        return new TraversalSafetyResult(TraversalSafetyStatus.NOT_APPLICABLE, null, null, 0, reason);
    }
}
