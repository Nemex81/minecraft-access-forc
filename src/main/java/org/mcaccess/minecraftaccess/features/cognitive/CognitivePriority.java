package org.mcaccess.minecraftaccess.features.cognitive;

import lombok.Getter;

/**
 * Strict hierarchical priorities for cognitive events in Minecraft Access.
 * Higher level events always preempt or silence lower level ones.
 */
@Getter
public enum CognitivePriority {
    CRITICAL(4),
    OPERATIONAL(3),
    CONTEXTUAL(2),
    PASSIVE(1);

    private final int rank;

    CognitivePriority(int rank) {
        this.rank = rank;
    }

    public boolean isHigherThan(CognitivePriority other) {
        return this.rank > other.rank;
    }

    public boolean isHigherOrEqual(CognitivePriority other) {
        return this.rank >= other.rank;
    }
}
