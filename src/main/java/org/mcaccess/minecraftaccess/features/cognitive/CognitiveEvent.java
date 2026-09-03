package org.mcaccess.minecraftaccess.features.cognitive;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single immutable contract representing a cognitive event emitted by domain managers
 * and processed by the central Cognitive Coordinator.
 */
public record CognitiveEvent(
        @NotNull SourceDomain domain,
        @NotNull CognitivePriority priority,
        @NotNull String semanticKey,
        @NotNull StateSignature stateSignature,
        @NotNull String narrationText,
        @Nullable BlockPos targetPos,
        double distance,
        @NotNull SpatialDirection direction,
        @NotNull OutputType outputType,
        @Nullable SoundCue soundCue,
        long ttlMillis,
        boolean canChain,
        long timestamp
) {
    public enum OutputType {
        VOICE_AND_SOUND,
        VOICE_ONLY,
        SOUND_ONLY,
        SILENT
    }

    public boolean isVoiceEnabled() {
        return outputType == OutputType.VOICE_AND_SOUND || outputType == OutputType.VOICE_ONLY;
    }

    public boolean isSoundEnabled() {
        return (outputType == OutputType.VOICE_AND_SOUND || outputType == OutputType.SOUND_ONLY) && soundCue != null;
    }

    public boolean isExpired(long currentTimeMillis) {
        return ttlMillis > 0 && (currentTimeMillis - timestamp) > ttlMillis;
    }

    public boolean isSpatiallyCompatible(CognitiveEvent other) {
        return this.direction.isCompatibleWith(other.direction);
    }

    /**
     * Helper factory for critical fast-path safety alerts.
     */
    public static CognitiveEvent createCritical(
            SourceDomain domain,
            String semanticKey,
            StateSignature signature,
            String text,
            @Nullable BlockPos targetPos,
            double distance,
            @Nullable SoundCue soundCue
    ) {
        return new CognitiveEvent(
                domain,
                CognitivePriority.CRITICAL,
                semanticKey,
                signature,
                text,
                targetPos,
                distance,
                SpatialDirection.FORWARD,
                soundCue != null ? OutputType.VOICE_AND_SOUND : OutputType.VOICE_ONLY,
                soundCue,
                2000,
                false,
                System.currentTimeMillis()
        );
    }

    /**
     * Helper factory for operational events (AutoWalk actions, explicit inspections, waypoint locks).
     */
    public static CognitiveEvent createOperational(
            SourceDomain domain,
            String semanticKey,
            StateSignature signature,
            String text,
            @Nullable BlockPos targetPos,
            double distance,
            @Nullable SoundCue soundCue
    ) {
        return new CognitiveEvent(
                domain,
                CognitivePriority.OPERATIONAL,
                semanticKey,
                signature,
                text,
                targetPos,
                distance,
                SpatialDirection.FORWARD,
                soundCue != null ? OutputType.VOICE_AND_SOUND : OutputType.VOICE_ONLY,
                soundCue,
                3000,
                true,
                System.currentTimeMillis()
        );
    }

    /**
     * Helper factory for contextual events (step-climbable obstacles, light variations, non-lethal status).
     */
    public static CognitiveEvent createContextual(
            SourceDomain domain,
            String semanticKey,
            StateSignature signature,
            String text,
            @Nullable BlockPos targetPos,
            double distance,
            SpatialDirection direction,
            @Nullable SoundCue soundCue,
            boolean canChain
    ) {
        return new CognitiveEvent(
                domain,
                CognitivePriority.CONTEXTUAL,
                semanticKey,
                signature,
                text,
                targetPos,
                distance,
                direction,
                soundCue != null ? OutputType.VOICE_AND_SOUND : OutputType.VOICE_ONLY,
                soundCue,
                2500,
                canChain,
                System.currentTimeMillis()
        );
    }

    /**
     * Helper factory for passive continuous exploration events (crosshair scanning, background orientation).
     */
    public static CognitiveEvent createPassive(
            SourceDomain domain,
            String semanticKey,
            StateSignature signature,
            String text,
            @Nullable BlockPos targetPos,
            double distance,
            SpatialDirection direction,
            @Nullable SoundCue soundCue,
            boolean canChain
    ) {
        return new CognitiveEvent(
                domain,
                CognitivePriority.PASSIVE,
                semanticKey,
                signature,
                text,
                targetPos,
                distance,
                direction,
                soundCue != null ? OutputType.VOICE_AND_SOUND : OutputType.VOICE_ONLY,
                soundCue,
                1500,
                canChain,
                System.currentTimeMillis()
        );
    }
}
