package org.mcaccess.minecraftaccess.features.crosshair;

import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of crosshair targeting context, used for obstacle narration composition.
 */
public record ObstacleNarrationContext(
        @Nullable String targetNarration,
        @Nullable Integer targetDistance
) {
    public static final ObstacleNarrationContext EMPTY = new ObstacleNarrationContext(null, null);
}
