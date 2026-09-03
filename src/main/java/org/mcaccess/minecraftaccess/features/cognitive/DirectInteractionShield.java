package org.mcaccess.minecraftaccess.features.cognitive;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Temporary shield token for direct user input (typing in EditBox, navigating GUI container slots,
 * Numpad manual inspection, AccessMenu).
 * When active, defers passive and contextual background chatter from interrupting tactile focus,
 * but NEVER delays or blocks CRITICAL safety emergencies.
 * Uses atomic updates to eliminate multi-thread race conditions.
 */
public final class DirectInteractionShield {
    private static final AtomicLong shieldUntil = new AtomicLong(0);

    private DirectInteractionShield() {
    }

    /**
     * Activate or extend the direct interaction shield atomically.
     *
     * @param durationMillis duration in milliseconds
     */
    public static void activate(long durationMillis) {
        long target = System.currentTimeMillis() + durationMillis;
        shieldUntil.updateAndGet(current -> Math.max(current, target));
    }

    /**
     * Check if the direct interaction shield is currently active.
     */
    public static boolean isActive() {
        return System.currentTimeMillis() < shieldUntil.get();
    }

    /**
     * Reset the shield immediately.
     */
    public static void reset() {
        shieldUntil.set(0);
    }

    /**
     * Get remaining duration in milliseconds.
     */
    public static long getRemainingMillis() {
        long diff = shieldUntil.get() - System.currentTimeMillis();
        return Math.max(0, diff);
    }
}
