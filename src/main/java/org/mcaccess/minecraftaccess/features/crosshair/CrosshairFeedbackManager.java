package org.mcaccess.minecraftaccess.features.crosshair;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.WorldNarrator;
import org.mcaccess.minecraftaccess.features.NarrateCrosshair;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;
import org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils;

/**
 * Single Source of Truth and coordinator for crosshair targeting and facing orientation feedback.
 * Formats unified atomic messages, eliminates race conditions between crosshair ticks and camera rotations,
 * and handles debouncing and modular token composition.
 */
@Slf4j
public final class CrosshairFeedbackManager {
    private static long lastNarrationTime = 0;
    private static final long DEBOUNCE_GRACE_PERIOD_MS = 80;

    private CrosshairFeedbackManager() {
    }

    private static String getI18nString(String key, String fallbackFormat, Object... args) {
        String result = I18n.get(key, args);
        if (result.equals(key) || result.startsWith("minecraft_access.")) {
            return String.format(fallbackFormat, args);
        }
        return result;
    }

    /**
     * Pure static formatting method for crosshair feedback message.
     *
     * @param targetName        Name or description of targeted block/entity (null if air/miss)
     * @param distance          Euclidean distance in meters/blocks (null if none)
     * @param cardinalDirection Horizontal cardinal direction (e.g. "South", "North-East")
     * @param compassDegrees    Horizontal compass degrees 0-359 (null if none)
     * @param pitchDirection    Vertical pitch direction (e.g. "Straight", "15 degrees Up", "Down")
     * @param order             Reading order enum
     * @param includeBlock      Whether to include block/entity target name
     * @param includeDistance   Whether to include distance in blocks
     * @param includeCardinal   Whether to include horizontal cardinal direction
     * @param includeDegrees    Whether to include horizontal compass degrees
     * @param includePitch      Whether to include vertical pitch direction
     * @return Formatted atomic narration string, or empty string if nothing to narrate
     */
    public static String formatFeedback(
            @Nullable String targetName,
            @Nullable Double distance,
            @Nullable String cardinalDirection,
            @Nullable Integer compassDegrees,
            @Nullable String pitchDirection,
            CrosshairReadingOrder order,
            boolean includeBlock,
            boolean includeDistance,
            boolean includeCardinal,
            boolean includeDegrees,
            boolean includePitch
    ) {
        String targetPart = null;
        if (includeBlock && targetName != null && !targetName.isBlank()) {
            targetPart = targetName.trim();
            if (includeDistance && distance != null) {
                int rounded = (int) Math.round(distance);
                String distStr = (rounded <= 1)
                        ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco")
                        : getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", rounded);
                targetPart = getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", targetPart, distStr);
            }
        }

        // Build orientation tokens
        List<String> orientationTokens = new ArrayList<>();
        if (includeCardinal && cardinalDirection != null && !cardinalDirection.isBlank()) {
            orientationTokens.add(cardinalDirection.trim());
        }
        if (includeDegrees && compassDegrees != null) {
            orientationTokens.add(getI18nString("minecraft_access.direction.degrees", "%s gradi", NarrationUtils.narrateNumber(compassDegrees)));
        }
        if (includePitch && pitchDirection != null && !pitchDirection.isBlank()) {
            orientationTokens.add(pitchDirection.trim());
        }

        String orientationPart = orientationTokens.isEmpty() ? null : String.join(", ", orientationTokens);

        if (targetPart == null && orientationPart == null) {
            return "";
        }
        if (targetPart == null) {
            return orientationPart;
        }
        if (orientationPart == null) {
            return targetPart;
        }

        // Both target and orientation are present
        if (order == CrosshairReadingOrder.TARGET_FIRST) {
            return getI18nString("minecraft_access.crosshair_feedback.block_then_facing", "%s, %s", targetPart, orientationPart);
        } else if (order == CrosshairReadingOrder.ORIENTATION_FIRST) {
            return getI18nString("minecraft_access.crosshair_feedback.facing_then_block", "%s: %s", orientationPart, targetPart);
        } else if (order == CrosshairReadingOrder.TARGET_CARDINAL_INLINE) {
            if (includeCardinal && cardinalDirection != null && !cardinalDirection.isBlank()) {
                String inlineTarget = getI18nString("minecraft_access.crosshair_feedback.block_and_cardinal", "%s a %s", targetName.trim(), cardinalDirection.trim());
                if (includeDistance && distance != null) {
                    int rounded = (int) Math.round(distance);
                    String distStr = (rounded <= 1)
                            ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco")
                            : getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", rounded);
                    inlineTarget = getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", inlineTarget, distStr);
                }

                // Remaining orientation tokens (degrees and pitch)
                List<String> remainingTokens = new ArrayList<>();
                if (includeDegrees && compassDegrees != null) {
                    remainingTokens.add(getI18nString("minecraft_access.direction.degrees", "%s gradi", NarrationUtils.narrateNumber(compassDegrees)));
                }
                if (includePitch && pitchDirection != null && !pitchDirection.isBlank()) {
                    remainingTokens.add(pitchDirection.trim());
                }

                if (remainingTokens.isEmpty()) {
                    return inlineTarget;
                } else {
                    return inlineTarget + ", " + String.join(", ", remainingTokens);
                }
            } else {
                return getI18nString("minecraft_access.crosshair_feedback.block_then_facing", "%s, %s", targetPart, orientationPart);
            }
        }

        return getI18nString("minecraft_access.crosshair_feedback.block_then_facing", "%s, %s", targetPart, orientationPart);
    }

    private static long movementSuppressedUntil = 0;

    /**
     * Temporarily suppresses the movement-based crosshair feed (e.g. during obstacle alerts).
     */
    public static void suppressMovementFeed(long durationMillis) {
        movementSuppressedUntil = System.currentTimeMillis() + durationMillis;
    }

    /**
     * Triggered by NarrateCrosshair when the targeted block or entity changes during normal movement.
     */
    public static void onCrosshairTargetChanged(@Nullable HitResult rayCast, @Nullable String targetName) {
        Config.NarrateCrosshair config = Config.getInstance().narrateCrosshair;
        if (!config.enabled) return;

        long now = System.currentTimeMillis();
        if (now - lastNarrationTime < DEBOUNCE_GRACE_PERIOD_MS) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        Double distance = (rayCast != null && rayCast.getType() != HitResult.Type.MISS)
                ? player.getEyePosition().distanceTo(rayCast.getLocation())
                : null;

        String cardinal = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
        int degrees = PlayerPositionUtils.getCompassDegrees();
        String pitch = PlayerPositionUtils.getVerticalFacingDirectionInWords();

        String message = formatFeedback(
                targetName,
                distance,
                cardinal,
                degrees,
                pitch,
                config.readingOrder,
                config.includeBlock,
                config.includeDistance,
                config.includeCardinal,
                config.includeCompassDegrees,
                config.includePitchAngle
        );

        if (!Strings.isEmpty(message)) {
            lastNarrationTime = now;
            MainClass.narrate(message, true);
        }
    }

    /**
     * Triggered during active linear player movement (WASD / strafe / walk) when target or distance changes.
     */
    public static void onCrosshairTargetChangedInMovement(@Nullable HitResult rayCast, @Nullable String targetName, double distance) {
        Config.NarrateCrosshair config = Config.getInstance().narrateCrosshair;
        if (!config.enabled) return;
        if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.OFF) return;

        long now = System.currentTimeMillis();
        if (now < movementSuppressedUntil) return;
        if (now - lastNarrationTime < config.movementDebounceIntervalMs) return;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.TARGET_ONLY) {
            if (targetName == null || targetName.isBlank()) return;
            lastNarrationTime = now;
            NarrateCrosshair.synchronizeTarget(rayCast, targetName);
            MainClass.narrate(targetName.trim(), true);
            return;
        }

        if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.TARGET_AND_DISTANCE) {
            if (targetName == null || targetName.isBlank()) return;
            int rounded = (int) Math.round(distance);
            String distStr = (rounded <= 1)
                    ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco")
                    : getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", rounded);
            String message = getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", targetName.trim(), distStr);
            lastNarrationTime = now;
            NarrateCrosshair.synchronizeTarget(rayCast, targetName);
            MainClass.narrate(message, true);
            return;
        }

        if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.FULL_FORMAT) {
            String cardinal = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
            int degrees = PlayerPositionUtils.getCompassDegrees();
            String pitch = PlayerPositionUtils.getVerticalFacingDirectionInWords();

            String message = formatFeedback(
                    targetName,
                    distance,
                    cardinal,
                    degrees,
                    pitch,
                    config.readingOrder,
                    config.includeBlock,
                    config.includeDistance,
                    config.includeCardinal,
                    config.includeCompassDegrees,
                    config.includePitchAngle
            );

            if (!Strings.isEmpty(message)) {
                lastNarrationTime = now;
                NarrateCrosshair.synchronizeTarget(rayCast, targetName);
                MainClass.narrate(message, true);
            }
        }
    }

    /**
     * Triggered when the user explicitly requests manual target narration (e.g. key B).
     */
    public static void onManualCrosshairRequested() {
        Config.NarrateCrosshair config = Config.getInstance().narrateCrosshair;
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        NarrateCrosshair.suppressNarration(100);

        String cardinal = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
        int degrees = PlayerPositionUtils.getCompassDegrees();
        String pitch = PlayerPositionUtils.getVerticalFacingDirectionInWords();

        String targetName = null;
        Double distance = null;

        WorldNarrator narrator = MainClass.registry(WorldNarrator.class).get(config.narrator);
        HitResult rayCast = (narrator != null) ? narrator.rayCast() : null;

        if (rayCast != null && rayCast.getType() != HitResult.Type.MISS) {
            targetName = narrator.narrate(rayCast);
            distance = player.getEyePosition().distanceTo(rayCast.getLocation());
            NarrateCrosshair.synchronizeTarget(rayCast, targetName);
        } else {
            targetName = getI18nString("minecraft_access.crosshair_feedback.no_target", "Nessun bersaglio");
            NarrateCrosshair.synchronizeTarget(null, null);
        }

        String message = formatFeedback(
                targetName,
                distance,
                cardinal,
                degrees,
                pitch,
                config.readingOrder,
                config.includeBlock,
                config.includeDistance,
                config.includeCardinal,
                config.includeCompassDegrees,
                config.includePitchAngle
        );

        if (!Strings.isEmpty(message)) {
            lastNarrationTime = System.currentTimeMillis();
            MainClass.narrate(message, true);
        }
    }

    /**
     * Triggered when camera rotation occurs via CameraControls or NumpadControls.
     */
    public static void onCameraRotated(boolean narrateChange) {
        if (!narrateChange) return;

        Config.NarrateCrosshair config = Config.getInstance().narrateCrosshair;
        boolean facingEnabled = Config.getInstance().features.facingDirectionEnabled;
        if (!config.enabled && !facingEnabled) return;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        NarrateCrosshair.suppressNarration(100);

        String cardinal = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
        int degrees = PlayerPositionUtils.getCompassDegrees();
        String pitch = PlayerPositionUtils.getVerticalFacingDirectionInWords();

        String targetName = null;
        Double distance = null;

        if (config.enabled && config.includeBlock) {
            WorldNarrator narrator = MainClass.registry(WorldNarrator.class).get(config.narrator);
            if (narrator != null) {
                HitResult rayCast = narrator.rayCast();
                if (rayCast != null && rayCast.getType() != HitResult.Type.MISS) {
                    targetName = narrator.narrate(rayCast);
                    distance = player.getEyePosition().distanceTo(rayCast.getLocation());
                    NarrateCrosshair.synchronizeTarget(rayCast, targetName);
                } else {
                    NarrateCrosshair.synchronizeTarget(null, null);
                }
            }
        }

        String message = formatFeedback(
                targetName,
                distance,
                cardinal,
                degrees,
                pitch,
                config.readingOrder,
                config.includeBlock,
                config.includeDistance,
                config.includeCardinal,
                config.includeCompassDegrees,
                config.includePitchAngle
        );

        if (!Strings.isEmpty(message)) {
            lastNarrationTime = System.currentTimeMillis();
            MainClass.narrate(message, true);
        }
    }

    /**
     * Triggered when camera horizon is centered (e.g. Numpad 5).
     */
    public static void onLookCentered() {
        onCameraRotated(true);
    }
}
