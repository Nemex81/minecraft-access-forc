package org.mcaccess.minecraftaccess.features;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.ObstacleScanResult;
import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.ObstacleState;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;

/**
 * Pure factory for creating cognitive safety events and audio cues for ObstacleDetector.
 * Enforces determinism, boundary symmetry in spatial direction mapping, and consistent SoundCue building.
 */
public final class ObstacleSafetyEventFactory {

    private ObstacleSafetyEventFactory() {
    }

    /**
     * Normalizes an angle in degrees to [0.0, 360.0).
     */
    public static double normalizeAngle(double angle) {
        return ((angle % 360.0) + 360.0) % 360.0;
    }

    /**
     * Resolves relative angle in degrees to SpatialDirection using strictly symmetrical quadrants:
     * - [315°, 360°) and [0°, 45°) -> FORWARD
     * - [45°, 135°) -> RIGHT
     * - [135°, 225°) -> BACK
     * - [225°, 315°) -> LEFT
     */
    public static SpatialDirection resolveSpatialDirection(double relAngle) {
        double norm = normalizeAngle(relAngle);
        if (norm >= 315.0 || norm < 45.0) {
            return SpatialDirection.FORWARD;
        } else if (norm >= 45.0 && norm < 135.0) {
            return SpatialDirection.RIGHT;
        } else if (norm >= 135.0 && norm < 225.0) {
            return SpatialDirection.BACK;
        } else {
            return SpatialDirection.LEFT;
        }
    }

    /**
     * Creates the standard SoundCue for an obstacle scan result.
     * Single source of truth shared between cognitive event creation and legacy bypass.
     */
    public static SoundCue createSoundCue(@NotNull ObstacleScanResult result, float volume) {
        BlockPos soundPos = result.lookAtPos() != null ? result.lookAtPos() : result.targetFootPos();
        if (result.state() == ObstacleState.STEP_CLIMBABLE) {
            return SoundCue.of(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, soundPos, volume, 1.5f);
        } else {
            return SoundCue.of(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, soundPos, volume, 0.6f);
        }
    }

    /**
     * Builds an immutable CognitiveEvent for an obstacle warning.
     * Returns null if both voice and sound are disabled.
     */
    public static @Nullable CognitiveEvent createObstacleEvent(
            @NotNull ObstacleScanResult result,
            @NotNull String composedNarrationText,
            int obstacleDistance,
            double relAngle,
            boolean voiceWanted,
            boolean soundWanted,
            float volume,
            long timestamp
    ) {
        if (!voiceWanted && !soundWanted) {
            return null;
        }

        Objects.requireNonNull(composedNarrationText, "narrationText cannot be null");
        if (composedNarrationText.isBlank()) {
            throw new IllegalArgumentException("narrationText cannot be blank");
        }

        CognitiveEvent.OutputType outputType;
        if (voiceWanted && soundWanted) {
            outputType = CognitiveEvent.OutputType.VOICE_AND_SOUND;
        } else if (voiceWanted) {
            outputType = CognitiveEvent.OutputType.VOICE_ONLY;
        } else {
            outputType = CognitiveEvent.OutputType.SOUND_ONLY;
        }

        int severity = switch (result.state()) {
            case STEP_CLIMBABLE -> 1;
            case LOW_CEILING -> 2;
            case HEAD_OBSTACLE -> 3;
            case WALL -> 4;
            default -> 0;
        };

        String semanticKey = (result.state() == ObstacleState.STEP_CLIMBABLE)
                ? "safety.obstacle.step_climbable"
                : "safety.obstacle.barrier";

        String targetId = (result.primaryBlockState() != null && result.primaryBlockState().getBlock() != null)
                ? result.primaryBlockState().getBlock().getDescriptionId()
                : result.state().name();

        int clampedDist = Math.max(1, obstacleDistance);
        StateSignature signature = StateSignature.of(clampedDist, severity, targetId);

        BlockPos targetPos = (result.state() == ObstacleState.STEP_CLIMBABLE)
                ? result.targetFootPos()
                : (result.lookAtPos() != null ? result.lookAtPos() : result.targetFootPos());

        SpatialDirection direction = resolveSpatialDirection(relAngle);
        SoundCue soundCue = soundWanted ? createSoundCue(result, volume) : null;

        return CognitiveEvent.createSafetyAlert(
                semanticKey,
                CognitivePriority.CONTEXTUAL,
                signature,
                composedNarrationText,
                targetPos,
                clampedDist,
                direction,
                outputType,
                soundCue,
                2500,
                timestamp
        );
    }
}
