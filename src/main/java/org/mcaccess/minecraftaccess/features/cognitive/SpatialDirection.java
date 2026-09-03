package org.mcaccess.minecraftaccess.features.cognitive;

/**
 * Relative spatial direction of a cognitive event.
 * Used to verify spatial compatibility before chaining events,
 * preventing confusing combinations such as an obstacle on the left with a target on the right.
 */
public enum SpatialDirection {
    FORWARD,
    LEFT,
    RIGHT,
    BACK,
    UP,
    DOWN,
    OMNI;

    /**
     * Determines whether two spatial directions are compatible for chaining.
     * OMNI events (global status, non-directional warnings) are compatible with any direction.
     * Directional events are only compatible if they share the exact same direction.
     */
    public boolean isCompatibleWith(SpatialDirection other) {
        if (this == OMNI || other == OMNI) {
            return true;
        }
        return this == other;
    }
}
