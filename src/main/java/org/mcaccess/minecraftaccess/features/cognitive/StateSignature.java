package org.mcaccess.minecraftaccess.features.cognitive;

import org.jetbrains.annotations.Nullable;

/**
 * Immutable state signature used for high-fidelity semantic deduplication.
 * Ensures that changes in distance, severity escalation, or target identity
 * are recognized as state changes and not incorrectly suppressed as duplicates.
 */
public record StateSignature(
        int distanceBucket,
        int severityLevel,
        @Nullable String targetId
) {
    public static final StateSignature EMPTY = new StateSignature(0, 0, null);

    public static StateSignature of(int distanceBucket, int severityLevel) {
        return new StateSignature(distanceBucket, severityLevel, null);
    }

    public static StateSignature of(int distanceBucket, int severityLevel, @Nullable String targetId) {
        return new StateSignature(distanceBucket, severityLevel, targetId);
    }
}
