package org.mcaccess.minecraftaccess.features.mentor;

import java.util.function.Predicate;

import org.mcaccess.minecraftaccess.features.context.PlayerContextSnapshot;

/**
 * Definition of a contextual mentor rule.
 */
public record MentorRule(
        String id,
        boolean repeatable,
        long cooldownMillis,
        Predicate<PlayerContextSnapshot> condition,
        String messageKey
) {
    public boolean evaluate(PlayerContextSnapshot snapshot) {
        return condition.test(snapshot);
    }
}
