package org.mcaccess.minecraftaccess.features.crosshair;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.WorldNarrator;
import org.mcaccess.minecraftaccess.features.NarrateCrosshair;
import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;
import org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils;

/**
 * Single Source of Truth and coordinator for crosshair targeting and facing orientation feedback.
 * Formats unified atomic messages, eliminates race conditions between crosshair ticks and camera rotations,
 * and handles debouncing and modular token composition.
 */
@Slf4j
public final class CrosshairFeedbackManager {
    private static @Nullable Object currentTarget = null;
    private static @Nullable String currentNarration = null;
    private static @Nullable Integer currentDistance = null;
    private static long lastNarrationTime = 0;
    private static long lastDistanceNarrationTime = 0;
    private static final long DEBOUNCE_GRACE_PERIOD_MS = 80;

    private CrosshairFeedbackManager() {
    }

    public static void onCrosshairMiss() {
        currentTarget = null;
        currentNarration = null;
        currentDistance = null;
    }

    public static void synchronizeTarget(@Nullable HitResult rayCast, @Nullable String narration) {
        if (rayCast == null || rayCast.getType() == HitResult.Type.MISS) {
            onCrosshairMiss();
            return;
        }
        Config.NarrateCrosshair config = Config.getInstance().narrateCrosshair;
        Object target = switch (rayCast) {
            case net.minecraft.world.phys.BlockHitResult blockHitResult -> config.disableNarratingConsecutiveBlocks ? null : blockHitResult.getBlockPos();
            case net.minecraft.world.phys.EntityHitResult entityHitResult -> entityHitResult.getEntity();
            default -> rayCast;
        };
        currentTarget = target;
        currentNarration = narration;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            currentDistance = (int) Math.round(client.player.getEyePosition().distanceTo(rayCast.getLocation()));
        }
    }

    /**
     * Triggered directly by ObstacleDetector when an obstacle warning is active.
     */
    public static void onObstacleDetected(
            @NotNull ObstacleDetectionUtils.ObstacleScanResult result,
            @NotNull String obstacleMsg,
            double relAngle
    ) {
        if (obstacleMsg.isBlank()) return;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        String frontPrefix = I18n.get("minecraft_access.obstacle_detector.dir_forward");
        if (frontPrefix.equals("minecraft_access.obstacle_detector.dir_forward")) {
            frontPrefix = "Davanti";
        }

        boolean isFrontal = obstacleMsg.startsWith(frontPrefix);
        String message;

        if (isFrontal) {
            Vec3 targetCenter = Vec3.atCenterOf(result.targetFootPos());
            int distance = Math.max(1, (int) Math.round(Math.sqrt(player.distanceToSqr(targetCenter))));
            String distStr = (distance <= 1)
                    ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco")
                    : getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", distance);
            message = getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", obstacleMsg, distStr);
        } else {
            if (currentNarration != null && !currentNarration.isBlank()) {
                String distStr = (currentDistance != null && currentDistance > 1)
                        ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", currentDistance)
                        : getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco");
                String frontTarget = getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", currentNarration, distStr);
                message = obstacleMsg + ". " + frontPrefix + ": " + frontTarget;
            } else {
                message = obstacleMsg;
            }
        }

        if (!Strings.isEmpty(message)) {
            lastNarrationTime = System.currentTimeMillis();
            lastDistanceNarrationTime = lastNarrationTime;
            MainClass.narrate(message, true);
        }
    }

    public static void processCrosshairTick(
            @NotNull HitResult rayCast,
            @Nullable Object target,
            @Nullable String targetName,
            double distance,
            boolean inActiveMovement
    ) {
        Config.NarrateCrosshair config = Config.getInstance().narrateCrosshair;
        if (!config.enabled) return;
        if (targetName == null || targetName.isBlank()) return;

        long now = System.currentTimeMillis();

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        int roundedDistance = (int) Math.round(distance);

        boolean isTargetMutation = !java.util.Objects.equals(target, currentTarget) || !java.util.Objects.equals(targetName, currentNarration);
        boolean isDistanceProgression = inActiveMovement && config.narrateDistanceChangeInMovement
                && (currentDistance == null || !currentDistance.equals(roundedDistance));

        if (!isTargetMutation && !isDistanceProgression) {
            return;
        }

        // Target Mutation: priority transition (instant on block change)
        if (isTargetMutation) {
            String elevationText = null;
            if (config.relativePositionSoundCue.isVoiceEnabled()) {
                Integer deltaY = calculateRelativeElevation(rayCast, player);
                elevationText = formatElevationText(deltaY, config.relativePositionSoundCue.narrationStyle, config.relativePositionSoundCue.narrateSameLevel);
            }

            String message;
            if (inActiveMovement) {
                if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.OFF) {
                    currentTarget = target;
                    currentNarration = targetName;
                    return;
                } else if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.TARGET_ONLY) {
                    String msg = targetName.trim();
                    if (elevationText != null && !elevationText.isBlank()) {
                        msg = msg + ", " + elevationText.trim();
                    }
                    message = msg;
                } else if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.TARGET_AND_DISTANCE) {
                    String baseTarget = targetName.trim();
                    if (elevationText != null && !elevationText.isBlank()) {
                        baseTarget = baseTarget + ", " + elevationText.trim();
                    }
                    String distStr = (roundedDistance <= 1)
                            ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco")
                            : getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", roundedDistance);
                    message = getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", baseTarget, distStr);
                } else {
                    String cardinal = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
                    int degrees = PlayerPositionUtils.getCompassDegrees();
                    String pitch = PlayerPositionUtils.getVerticalFacingDirectionInWords();
                    message = formatFeedback(
                            targetName,
                            elevationText,
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
                }
            } else {
                String cardinal = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
                int degrees = PlayerPositionUtils.getCompassDegrees();
                String pitch = PlayerPositionUtils.getVerticalFacingDirectionInWords();
                message = formatFeedback(
                        targetName,
                        elevationText,
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
            }

            if (!Strings.isEmpty(message)) {
                // ATOMIC STATE COMMITMENT: Only commit when actually spoken!
                currentTarget = target;
                currentNarration = targetName;
                currentDistance = roundedDistance;
                lastNarrationTime = now;
                lastDistanceNarrationTime = now;
                MainClass.narrate(message, true);
            }
            return;
        }

        // Distance Progression on the SAME target: throttled at movementDebounceIntervalMs (e.g. 350ms)
        if (isDistanceProgression) {
            if (now < movementSuppressedUntil) return;
            if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.OFF) return;
            if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.TARGET_ONLY) {
                currentDistance = roundedDistance;
                return;
            }
            if (now - lastDistanceNarrationTime < config.movementDebounceIntervalMs) {
                return;
            }

            String elevationText = null;
            if (config.relativePositionSoundCue.isVoiceEnabled()) {
                Integer deltaY = calculateRelativeElevation(rayCast, player);
                elevationText = formatElevationText(deltaY, config.relativePositionSoundCue.narrationStyle, config.relativePositionSoundCue.narrateSameLevel);
            }

            String message;
            if (config.movementFeedbackMode == Config.NarrateCrosshair.MovementFeedbackMode.TARGET_AND_DISTANCE) {
                String baseTarget = targetName.trim();
                if (elevationText != null && !elevationText.isBlank()) {
                    baseTarget = baseTarget + ", " + elevationText.trim();
                }
                String distStr = (roundedDistance <= 1)
                        ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco")
                        : getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", roundedDistance);
                message = getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", baseTarget, distStr);
            } else {
                String cardinal = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
                int degrees = PlayerPositionUtils.getCompassDegrees();
                String pitch = PlayerPositionUtils.getVerticalFacingDirectionInWords();
                message = formatFeedback(
                        targetName,
                        elevationText,
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
            }

            if (!Strings.isEmpty(message)) {
                // ATOMIC STATE COMMITMENT
                currentDistance = roundedDistance;
                lastDistanceNarrationTime = now;
                lastNarrationTime = now;
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

        String elevationText = null;
        if (rayCast != null && rayCast.getType() != HitResult.Type.MISS) {
            targetName = narrator.narrate(rayCast);
            distance = player.getEyePosition().distanceTo(rayCast.getLocation());
            synchronizeTarget(rayCast, targetName);
            if (config.relativePositionSoundCue.isVoiceEnabled()) {
                Integer deltaY = calculateRelativeElevation(rayCast, player);
                elevationText = formatElevationText(deltaY, config.relativePositionSoundCue.narrationStyle, config.relativePositionSoundCue.narrateSameLevel);
            }
        } else {
            targetName = getI18nString("minecraft_access.crosshair_feedback.no_target", "Nessun bersaglio");
            onCrosshairMiss();
        }

        String message = formatFeedback(
                targetName,
                elevationText,
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
            lastDistanceNarrationTime = lastNarrationTime;
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
        String elevationText = null;

        if (config.enabled && config.includeBlock) {
            WorldNarrator narrator = MainClass.registry(WorldNarrator.class).get(config.narrator);
            if (narrator != null) {
                HitResult rayCast = narrator.rayCast();
                if (rayCast != null && rayCast.getType() != HitResult.Type.MISS) {
                    targetName = narrator.narrate(rayCast);
                    distance = player.getEyePosition().distanceTo(rayCast.getLocation());
                    synchronizeTarget(rayCast, targetName);
                    if (config.relativePositionSoundCue.isVoiceEnabled()) {
                        Integer deltaY = calculateRelativeElevation(rayCast, player);
                        elevationText = formatElevationText(deltaY, config.relativePositionSoundCue.narrationStyle, config.relativePositionSoundCue.narrateSameLevel);
                    }
                } else {
                    onCrosshairMiss();
                }
            }
        }

        String message = formatFeedback(
                targetName,
                elevationText,
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
            lastDistanceNarrationTime = lastNarrationTime;
            MainClass.narrate(message, true);
        }
    }

    /**
     * Triggered when looking is centered / horizon leveled (e.g. Numpad 5 or key M).
     */
    public static void onCameraCentered(boolean includeVoicePrefix) {
        NarrateCrosshair.suppressNarration(150);

        Config.NarrateCrosshair config = Config.getInstance().narrateCrosshair;
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        String facing = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
        int degrees = PlayerPositionUtils.getCompassDegrees();

        String targetName = null;
        Double distance = null;
        String elevationText = null;

        WorldNarrator narrator = MainClass.registry(WorldNarrator.class).get(config.narrator);
        if (narrator != null) {
            HitResult rayCast = narrator.rayCast();
            if (rayCast != null && rayCast.getType() != HitResult.Type.MISS) {
                targetName = narrator.narrate(rayCast);
                distance = player.getEyePosition().distanceTo(rayCast.getLocation());
                synchronizeTarget(rayCast, targetName);
                if (config.relativePositionSoundCue.isVoiceEnabled()) {
                    Integer deltaY = calculateRelativeElevation(rayCast, player);
                    elevationText = formatElevationText(deltaY, config.relativePositionSoundCue.narrationStyle, config.relativePositionSoundCue.narrateSameLevel);
                }
            } else {
                onCrosshairMiss();
            }
        }

        StringBuilder sb = new StringBuilder();
        if (includeVoicePrefix) {
            sb.append(I18n.get("minecraft_access.numpad.look_centered")).append(", ").append(facing);
        }

        if (targetName != null && !targetName.isBlank()) {
            if (sb.length() > 0) {
                sb.append(". ");
            }
            int roundedDistance = (distance != null) ? (int) Math.round(distance) : 1;
            String distStr = (roundedDistance <= 1)
                    ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco")
                    : getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", roundedDistance);
            String baseTarget = targetName.trim();
            if (elevationText != null && !elevationText.isBlank()) {
                baseTarget = baseTarget + ", " + elevationText.trim();
            }
            sb.append(getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", baseTarget, distStr));
        }

        String msg = sb.toString().trim();
        if (!msg.isEmpty()) {
            lastNarrationTime = System.currentTimeMillis();
            lastDistanceNarrationTime = lastNarrationTime;
            MainClass.narrate(msg, true);
        }
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
     * @param elevationText     Spoken relative elevation (e.g. "+1 blocco", "1 blocco sopra", null if disabled or none)
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
            @Nullable String elevationText,
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
            if (elevationText != null && !elevationText.isBlank()) {
                targetPart = targetPart + ", " + elevationText.trim();
            }
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
                if (elevationText != null && !elevationText.isBlank()) {
                    inlineTarget = inlineTarget + ", " + elevationText.trim();
                }
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

    /**
     * Backward-compatible overload without elevationText parameter.
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
        return formatFeedback(targetName, null, distance, cardinalDirection, compassDegrees, pitchDirection,
                order, includeBlock, includeDistance, includeCardinal, includeDegrees, includePitch);
    }

    @Nullable
    public static Integer calculateRelativeElevation(@Nullable HitResult rayCast, @Nullable Player player) {
        if (rayCast == null || rayCast.getType() == HitResult.Type.MISS || player == null) {
            return null;
        }
        int playerFeetY = player.blockPosition().getY();
        return switch (rayCast) {
            case net.minecraft.world.phys.BlockHitResult bhr -> bhr.getBlockPos().getY() - playerFeetY;
            case net.minecraft.world.phys.EntityHitResult ehr -> (int) Math.floor(ehr.getEntity().getY()) - playerFeetY;
            default -> (int) Math.floor(rayCast.getLocation().y()) - playerFeetY;
        };
    }

    @Nullable
    public static String formatElevationText(
            @Nullable Integer deltaY,
            Config.NarrateCrosshair.ElevationNarrationStyle style,
            boolean narrateSameLevel
    ) {
        if (deltaY == null) {
            return null;
        }
        if (deltaY == 0) {
            if (!narrateSameLevel) {
                return null;
            }
            return switch (style) {
                case DESCRIPTIVE -> getI18nString("minecraft_access.crosshair_elevation.same_level.descriptive", "a livello piedi");
                case COMPACT -> getI18nString("minecraft_access.crosshair_elevation.same_level.compact", "livello 0");
                case DELTA_ONLY -> getI18nString("minecraft_access.crosshair_elevation.same_level.delta_only", "0");
            };
        } else if (deltaY > 0) {
            return switch (style) {
                case DESCRIPTIVE -> (deltaY == 1)
                        ? getI18nString("minecraft_access.crosshair_elevation.up_single.descriptive", "1 blocco sopra")
                        : getI18nString("minecraft_access.crosshair_elevation.up_multiple.descriptive", "%d blocchi sopra", deltaY);
                case COMPACT -> (deltaY == 1)
                        ? getI18nString("minecraft_access.crosshair_elevation.up_single.compact", "+1 blocco")
                        : getI18nString("minecraft_access.crosshair_elevation.up_multiple.compact", "+%d blocchi", deltaY);
                case DELTA_ONLY -> getI18nString("minecraft_access.crosshair_elevation.up.delta_only", "+%d", deltaY);
            };
        } else {
            int absDelta = Math.abs(deltaY);
            return switch (style) {
                case DESCRIPTIVE -> (absDelta == 1)
                        ? getI18nString("minecraft_access.crosshair_elevation.down_single.descriptive", "1 blocco sotto")
                        : getI18nString("minecraft_access.crosshair_elevation.down_multiple.descriptive", "%d blocchi sotto", absDelta);
                case COMPACT -> (absDelta == 1)
                        ? getI18nString("minecraft_access.crosshair_elevation.down_single.compact", "-1 blocco")
                        : getI18nString("minecraft_access.crosshair_elevation.down_multiple.compact", "-%d blocchi", absDelta);
                case DELTA_ONLY -> getI18nString("minecraft_access.crosshair_elevation.down.delta_only", "-%d", absDelta);
            };
        }
    }

    private static long movementSuppressedUntil = 0;

    /**
     * Temporarily suppresses the movement-based crosshair feed (e.g. during obstacle alerts).
     */
    public static void suppressMovementFeed(long durationMillis) {
        movementSuppressedUntil = System.currentTimeMillis() + durationMillis;
    }

    /**
     * Triggered when camera horizon is centered (e.g. Numpad 5).
     */
    public static void onLookCentered() {
        onCameraRotated(true);
    }
}
