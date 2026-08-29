package org.mcaccess.minecraftaccess.utils;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.NarrateCrosshair;
import org.mcaccess.minecraftaccess.features.ObstacleDetector;

/**
 * Centralized narration priority and shield manager.
 * Protects salient events (item pickup, toasts, continuous orientation) from being
 * interrupted by background ambient scanners (crosshair, obstacle warnings).
 */
public final class NarrationPriority {
    private static long shieldUntil = 0;

    private NarrationPriority() {
    }

    /**
     * Suppress background ambient scanners (crosshair and obstacle detector) for the given duration.
     *
     * @param durationMillis duration in milliseconds
     */
    public static void suppressBackgroundScanners(long durationMillis) {
        long target = System.currentTimeMillis() + durationMillis;
        if (target > shieldUntil) {
            shieldUntil = target;
        }
        NarrateCrosshair.suppressNarration(durationMillis);
        ObstacleDetector.suppressWarnings(durationMillis);
    }

    /**
     * Check if the narration shield is currently active.
     */
    public static boolean isShieldActive() {
        return System.currentTimeMillis() < shieldUntil;
    }

    /**
     * Narrate a salient message immediately, interrupting prior background chatter
     * and shielding the narration from ambient scanners for the specified duration.
     *
     * @param text             message to narrate
     * @param protectionMillis shield protection duration in milliseconds
     */
    public static void narrateSalient(String text, long protectionMillis) {
        suppressBackgroundScanners(protectionMillis);
        MainClass.narrate(text, true);
    }

    /**
     * Narrate a salient message queued (interrupt: false), extending the shield.
     * Used when multiple salient events occur concurrently (e.g. item pickup + recipe unlock).
     *
     * @param text             message to narrate
     * @param protectionMillis shield protection duration in milliseconds
     */
    public static void narrateSalientQueued(String text, long protectionMillis) {
        suppressBackgroundScanners(protectionMillis);
        MainClass.narrate(text, false);
    }
}