package org.mcaccess.minecraftaccess.utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.NarrateCrosshair;
import org.mcaccess.minecraftaccess.features.ObstacleDetector;

/**
 * Centralized narration priority and shield manager (Facade / Adapter).
 * Protects salient events (item pickup, toasts, continuous orientation) from being
 * interrupted by background ambient scanners (crosshair, obstacle warnings).
 * Preserves legacy sync API while allowing pure unit testing via package-private test seams.
 */
public final class NarrationPriority {
    private static long shieldUntil = 0;

    // Package-private test seams (restored in @AfterEach)
    static BiConsumer<String, Boolean> narrationConsumer = MainClass::narrate;
    static LongSupplier timeSupplier = System::currentTimeMillis;
    static Consumer<Long> scannerSuppressor = NarrationPriority::defaultSuppressScanners;

    private NarrationPriority() {
    }

    /**
     * Suppress background ambient scanners (crosshair and obstacle detector) for the given duration.
     *
     * @param durationMillis duration in milliseconds
     */
    public static void suppressBackgroundScanners(long durationMillis) {
        long target = timeSupplier.getAsLong() + durationMillis;
        if (target > shieldUntil) {
            shieldUntil = target;
        }
        scannerSuppressor.accept(durationMillis);
    }

    /**
     * Check if the narration shield is currently active.
     */
    public static boolean isShieldActive() {
        return timeSupplier.getAsLong() < shieldUntil;
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
        narrationConsumer.accept(text, true);
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
        narrationConsumer.accept(text, false);
    }

    private static void defaultSuppressScanners(long durationMillis) {
        try {
            NarrateCrosshair.suppressNarration(durationMillis);
        } catch (Throwable ignored) {
            // Safely ignored in headless test environments without Minecraft/AutoConfig runtime
        }
        try {
            ObstacleDetector.suppressWarnings(durationMillis);
        } catch (Throwable ignored) {
            // Safely ignored in headless test environments without Minecraft/AutoConfig runtime
        }
    }

    /**
     * Package-private reset for test suites.
     */
    static void resetTestSeams() {
        narrationConsumer = MainClass::narrate;
        timeSupplier = System::currentTimeMillis;
        scannerSuppressor = NarrationPriority::defaultSuppressScanners;
        shieldUntil = 0;
    }
}