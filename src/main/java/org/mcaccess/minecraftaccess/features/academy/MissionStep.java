package org.mcaccess.minecraftaccess.features.academy;

import java.util.function.Predicate;

import org.mcaccess.minecraftaccess.features.context.PlayerContextSnapshot;

/**
 * Represents a single interactive step within a training mission.
 */
public record MissionStep(
        int stepIndex,
        String instructionKey,
        Predicate<PlayerContextSnapshot> completionPredicate,
        String successKey
) {
    public boolean isCompleted(PlayerContextSnapshot snapshot) {
        return completionPredicate.test(snapshot);
    }
}
