package org.mcaccess.minecraftaccess.features;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.ObstacleScanResult;
import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.ObstacleState;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Obstacle Safety Event Factory Pure Unit Tests (Fase 3B)")
class ObstacleSafetyEventFactoryTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("1. Null or blank narration message throws contract exceptions")
    void testContractValidation() {
        BlockPos pos = new BlockPos(5, 64, 5);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.STEP_CLIMBABLE, pos, pos.above(), pos.above(2), pos.above(2), pos, null);

        assertThrows(NullPointerException.class, () -> ObstacleSafetyEventFactory.createObstacleEvent(
                result, null, 1, 0.0, true, true, 0.8f, 1000
        ));
        assertThrows(IllegalArgumentException.class, () -> ObstacleSafetyEventFactory.createObstacleEvent(
                result, "   ", 1, 0.0, true, true, 0.8f, 1000
        ));
    }

    @Test
    @DisplayName("2. Returns null when both voice and sound are disabled")
    void testNullWhenBothDisabled() {
        BlockPos pos = new BlockPos(5, 64, 5);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.WALL, pos, pos.above(), pos.above(2), pos.above(2), pos.above(), null);

        CognitiveEvent event = ObstacleSafetyEventFactory.createObstacleEvent(
                result, "Davanti: muro", 1, 0.0, false, false, 0.8f, 1000
        );
        assertNull(event, "When both voice and sound are disabled, no event must be built");
    }

    @Test
    @DisplayName("3. Symmetrical angle normalization and boundary mapping for SpatialDirection")
    void testSpatialDirectionMappingAndBoundarySymmetry() {
        // [315°, 360°) and [0°, 45°) -> FORWARD
        assertEquals(SpatialDirection.FORWARD, ObstacleSafetyEventFactory.resolveSpatialDirection(0.0));
        assertEquals(SpatialDirection.FORWARD, ObstacleSafetyEventFactory.resolveSpatialDirection(20.0));
        assertEquals(SpatialDirection.FORWARD, ObstacleSafetyEventFactory.resolveSpatialDirection(44.999));
        assertEquals(SpatialDirection.FORWARD, ObstacleSafetyEventFactory.resolveSpatialDirection(315.0));
        assertEquals(SpatialDirection.FORWARD, ObstacleSafetyEventFactory.resolveSpatialDirection(340.0));
        assertEquals(SpatialDirection.FORWARD, ObstacleSafetyEventFactory.resolveSpatialDirection(359.999));

        // Negative angles
        assertEquals(SpatialDirection.FORWARD, ObstacleSafetyEventFactory.resolveSpatialDirection(-10.0));
        assertEquals(SpatialDirection.FORWARD, ObstacleSafetyEventFactory.resolveSpatialDirection(-45.0)); // = 315.0

        // [45°, 135°) -> RIGHT
        assertEquals(SpatialDirection.RIGHT, ObstacleSafetyEventFactory.resolveSpatialDirection(45.0));
        assertEquals(SpatialDirection.RIGHT, ObstacleSafetyEventFactory.resolveSpatialDirection(90.0));
        assertEquals(SpatialDirection.RIGHT, ObstacleSafetyEventFactory.resolveSpatialDirection(134.999));

        // [135°, 225°) -> BACK
        assertEquals(SpatialDirection.BACK, ObstacleSafetyEventFactory.resolveSpatialDirection(135.0));
        assertEquals(SpatialDirection.BACK, ObstacleSafetyEventFactory.resolveSpatialDirection(180.0));
        assertEquals(SpatialDirection.BACK, ObstacleSafetyEventFactory.resolveSpatialDirection(224.999));

        // [225°, 315°) -> LEFT
        assertEquals(SpatialDirection.LEFT, ObstacleSafetyEventFactory.resolveSpatialDirection(225.0));
        assertEquals(SpatialDirection.LEFT, ObstacleSafetyEventFactory.resolveSpatialDirection(270.0));
        assertEquals(SpatialDirection.LEFT, ObstacleSafetyEventFactory.resolveSpatialDirection(314.999));
        assertEquals(SpatialDirection.LEFT, ObstacleSafetyEventFactory.resolveSpatialDirection(-90.0)); // = 270.0
    }

    @Test
    @DisplayName("4. STEP_CLIMBABLE creates CONTEXTUAL event with NOTE_BLOCK_PLING, pitch 1.5, severity 1")
    void testClimbableStepEventCreation() {
        BlockPos foot = new BlockPos(10, 64, 20);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.STEP_CLIMBABLE, foot, foot.above(), foot.above(2), foot.above(2), foot, null);
        long now = 5000;

        CognitiveEvent event = ObstacleSafetyEventFactory.createObstacleEvent(
                result, "Davanti: gradino, a 1 blocco", 1, 0.0, true, true, 0.8f, now
        );

        assertNotNull(event);
        assertEquals(SourceDomain.SAFETY, event.domain());
        assertEquals(CognitivePriority.CONTEXTUAL, event.priority());
        assertEquals("safety.obstacle.step_climbable", event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_AND_SOUND, event.outputType());
        assertEquals(foot, event.targetPos());
        assertEquals(1, event.stateSignature().distanceBucket());
        assertEquals(1, event.stateSignature().severityLevel());
        assertEquals("STEP_CLIMBABLE", event.stateSignature().targetId());
        assertEquals(SpatialDirection.FORWARD, event.direction());
        assertFalse(event.canChain());
        assertEquals(2500, event.ttlMillis());
        assertEquals(now, event.timestamp());

        // Audio cue check
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_PLING.value(), event.soundCue().soundEvent());
        assertEquals(SoundSource.BLOCKS, event.soundCue().soundSource());
        assertEquals(0.8f, event.soundCue().volume());
        assertEquals(1.5f, event.soundCue().pitch());
        assertEquals(foot, event.soundCue().position());
    }

    @Test
    @DisplayName("5. Blocking obstacles (WALL, HEAD, LOW_CEILING) create barrier event with NOTE_BLOCK_BASS, pitch 0.6")
    void testBarrierObstaclesEventCreation() {
        BlockPos foot = new BlockPos(10, 64, 20);
        BlockPos lookAt = foot.above();

        // WALL: severity 4
        ObstacleScanResult wallResult = new ObstacleScanResult(ObstacleState.WALL, foot, lookAt, foot.above(2), foot.above(2), lookAt, null);
        CognitiveEvent wallEvent = ObstacleSafetyEventFactory.createObstacleEvent(
                wallResult, "Davanti: muro, a 2 blocchi", 2, 90.0, true, true, 0.7f, 1000
        );
        assertNotNull(wallEvent);
        assertEquals("safety.obstacle.barrier", wallEvent.semanticKey());
        assertEquals(4, wallEvent.stateSignature().severityLevel());
        assertEquals(lookAt, wallEvent.targetPos());
        assertEquals(SpatialDirection.RIGHT, wallEvent.direction());
        assertNotNull(wallEvent.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_BASS.value(), wallEvent.soundCue().soundEvent());
        assertEquals(0.6f, wallEvent.soundCue().pitch());

        // HEAD_OBSTACLE: severity 3
        ObstacleScanResult headResult = new ObstacleScanResult(ObstacleState.HEAD_OBSTACLE, foot, lookAt, foot.above(2), foot.above(2), lookAt, null);
        CognitiveEvent headEvent = ObstacleSafetyEventFactory.createObstacleEvent(
                headResult, "Ostacolo alla testa", 1, 0.0, true, true, 0.7f, 1000
        );
        assertNotNull(headEvent);
        assertEquals(3, headEvent.stateSignature().severityLevel());

        // LOW_CEILING: severity 2
        ObstacleScanResult ceilingResult = new ObstacleScanResult(ObstacleState.LOW_CEILING, foot, lookAt, foot.above(2), foot.above(2), foot.above(2), null);
        CognitiveEvent ceilingEvent = ObstacleSafetyEventFactory.createObstacleEvent(
                ceilingResult, "Soffitto basso", 1, 0.0, true, true, 0.7f, 1000
        );
        assertNotNull(ceilingEvent);
        assertEquals(2, ceilingEvent.stateSignature().severityLevel());
    }

    @Test
    @DisplayName("6. OutputType matrix: VOICE_ONLY and SOUND_ONLY behave deterministically")
    void testOutputTypeMatrix() {
        BlockPos foot = new BlockPos(10, 64, 20);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.WALL, foot, foot.above(), foot.above(2), foot.above(2), foot.above(), null);

        // VOICE_ONLY
        CognitiveEvent voiceOnly = ObstacleSafetyEventFactory.createObstacleEvent(
                result, "Davanti: muro, a 1 blocco", 1, 0.0, true, false, 0.8f, 1000
        );
        assertNotNull(voiceOnly);
        assertEquals(CognitiveEvent.OutputType.VOICE_ONLY, voiceOnly.outputType());
        assertTrue(voiceOnly.isVoiceEnabled());
        assertFalse(voiceOnly.isSoundEnabled());
        assertNull(voiceOnly.soundCue());

        // SOUND_ONLY
        CognitiveEvent soundOnly = ObstacleSafetyEventFactory.createObstacleEvent(
                result, "Davanti: muro, a 1 blocco", 1, 0.0, false, true, 0.8f, 1000
        );
        assertNotNull(soundOnly);
        assertEquals(CognitiveEvent.OutputType.SOUND_ONLY, soundOnly.outputType());
        assertFalse(soundOnly.isVoiceEnabled());
        assertTrue(soundOnly.isSoundEnabled());
        assertNotNull(soundOnly.soundCue());
        assertEquals("Davanti: muro, a 1 blocco", soundOnly.narrationText(), "narrationText preserved in contract even for SOUND_ONLY");
    }

    @Test
    @DisplayName("7. createSoundCue produces identical cue for both cognitive and legacy sinks")
    void testCreateSoundCueContract() {
        BlockPos foot = new BlockPos(10, 64, 20);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.STEP_CLIMBABLE, foot, foot.above(), foot.above(2), foot.above(2), foot, null);

        SoundCue cue = ObstacleSafetyEventFactory.createSoundCue(result, 0.75f);
        assertNotNull(cue);
        assertEquals(SoundEvents.NOTE_BLOCK_PLING.value(), cue.soundEvent());
        assertEquals(SoundSource.BLOCKS, cue.soundSource());
        assertEquals(foot, cue.position());
        assertEquals(0.75f, cue.volume());
        assertEquals(1.5f, cue.pitch());
    }
}
