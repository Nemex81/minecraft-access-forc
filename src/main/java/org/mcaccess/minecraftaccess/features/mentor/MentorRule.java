package org.mcaccess.minecraftaccess.features.mentor;

import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.mcaccess.minecraftaccess.features.context.PlayerContextSnapshot;

/**
 * Definition of a contextual mentor rule.
 */
public record MentorRule(
        String id,
        boolean repeatable,
        long cooldownMillis,
        Predicate<PlayerContextSnapshot> condition,
        String messageKey,
        @Nullable Function<PlayerContextSnapshot, Object[]> argsProvider
) {
    public MentorRule(String id, boolean repeatable, long cooldownMillis, Predicate<PlayerContextSnapshot> condition, String messageKey) {
        this(id, repeatable, cooldownMillis, condition, messageKey, null);
    }

    public boolean evaluate(PlayerContextSnapshot snapshot) {
        return condition.test(snapshot);
    }

    public Object[] getArgs(PlayerContextSnapshot snapshot) {
        if (argsProvider != null) {
            return argsProvider.apply(snapshot);
        }
        return new Object[0];
    }
}

