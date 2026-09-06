package org.mcaccess.minecraftaccess.features;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.ObstacleScanResult;
import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.ObstacleState;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.crosshair.CrosshairFeedbackManager;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ObstacleDetector Cognitive Dispatch & Legacy Bypass Unit Tests (Fase 3B)")
class ObstacleDetectorCognitiveDispatchTest {

    private List<CognitiveEvent> cognitiveEvents;
    private List<String> legacyVoiceCalls;
    private List<SoundCue> legacyAudioCalls;

    private static final ObstacleDetectionUtils.NarrationStyle STYLE = ObstacleDetectionUtils.NarrationStyle.BLOCK;
    private static final Config.ObstacleDetector.DirectionFeedbackMode DIR_MODE = Config.ObstacleDetector.DirectionFeedbackMode.FOUR_DIRECTIONS;
    private static final float VOLUME = 0.8f;

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        cognitiveEvents = new ArrayList<>();
        legacyVoiceCalls = new ArrayList<>();
        legacyAudioCalls = new ArrayList<>();

        ObstacleDetector.setCognitiveEventConsumer(cognitiveEvents::add);
        ObstacleDetector.setLegacyVoiceConsumer((res, msg, angle) -> legacyVoiceCalls.add(msg));
        ObstacleDetector.setLegacyAudioConsumer((level, cue) -> legacyAudioCalls.add(cue));

        CrosshairFeedbackManager.resetTestSeams();

        CognitiveCoordinator.setCoordinatorEnabled(true);
    }

    @AfterEach
    void tearDown() {
        ObstacleDetector.resetTestSeams();
        CrosshairFeedbackManager.resetTestSeams();
        CognitiveCoordinator.setCoordinatorEnabled(true);
    }

    @Test
    @DisplayName("1. Cognitive dispatch when coordinator is enabled submits single event and suppresses crosshair")
    void testCognitiveDispatchWhenCoordinatorEnabled() {
        BlockPos pos = new BlockPos(10, 64, 20);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.WALL, pos, pos.above(), pos.above(2), pos.above(2), pos.above(), null);
        long now = 1000;
        CrosshairFeedbackManager.setClock(() -> now);

        ObstacleDetector.dispatchObstacleAlert(result, 0.0, 1, true, true, VOLUME, STYLE, DIR_MODE, now);

        assertEquals(1, cognitiveEvents.size(), "Exactly one cognitive event must be submitted");
        assertTrue(legacyVoiceCalls.isEmpty(), "Legacy voice sink must not be called when coordinator is active");
        assertTrue(legacyAudioCalls.isEmpty(), "Legacy audio sink must not be called when coordinator is active");

        CognitiveEvent event = cognitiveEvents.get(0);
        assertEquals(CognitivePriority.CONTEXTUAL, event.priority());
        assertEquals("safety.obstacle.barrier", event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_AND_SOUND, event.outputType());
        assertNotNull(event.narrationText());
        assertFalse(event.narrationText().isBlank());
        String expectedLegacyText = ObstacleNarrationComposer.composeFinalNarration(
                ObstacleDetectionUtils.getNarrationMessage(result, STYLE, 0.0, DIR_MODE),
                1,
                CrosshairFeedbackManager.getNarrationContextSnapshot()
        );
        assertEquals(expectedLegacyText, event.narrationText(),
                "Cognitive event must use the same final narration composition as the legacy path");
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_BASS.value(), event.soundCue().soundEvent());
        assertEquals(0.6f, event.soundCue().pitch());

        // Crosshair movement suppression must be active for 100ms
        assertEquals(now + 100, CrosshairFeedbackManager.getAutomaticMovementSuppressedUntil());
    }

    @Test
    @DisplayName("2. Single narration producer: legacyVoiceConsumer is never called when coordinator is enabled")
    void testSingleNarrationProducerWhenCoordinatorActive() {
        BlockPos pos = new BlockPos(5, 64, 5);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.WALL, pos, pos.above(), pos.above(2), pos.above(2), pos.above(), null);

        ObstacleDetector.dispatchObstacleAlert(result, 0.0, 2, true, true, VOLUME, STYLE, DIR_MODE, 1000);

        assertEquals(1, cognitiveEvents.size());
        assertEquals(0, legacyVoiceCalls.size(), "Guarantees zero duplicate voice emission");
    }

    @Test
    @DisplayName("3. SOUND_ONLY obstacle alert does not trigger crosshair movement suppression")
    void testSoundOnlyObstacleDoesNotSuppressAutomaticCrosshairFeedback() {
        BlockPos pos = new BlockPos(10, 64, 20);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.WALL, pos, pos.above(), pos.above(2), pos.above(2), pos.above(), null);
        long now = 2000;

        ObstacleDetector.dispatchObstacleAlert(result, 0.0, 1, false, true, VOLUME, STYLE, DIR_MODE, now);

        assertEquals(1, cognitiveEvents.size());
        CognitiveEvent event = cognitiveEvents.get(0);
        assertEquals(CognitiveEvent.OutputType.SOUND_ONLY, event.outputType());
        assertFalse(event.isVoiceEnabled());
        assertTrue(event.isSoundEnabled());

        // Must NOT suppress crosshair feedback
        assertEquals(0, CrosshairFeedbackManager.getAutomaticMovementSuppressedUntil());
    }

    @Test
    @DisplayName("4. VOICE_ONLY obstacle alert creates event without SoundCue and suppresses crosshair")
    void testVoiceOnlyObstacleAlert() {
        BlockPos pos = new BlockPos(10, 64, 20);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.WALL, pos, pos.above(), pos.above(2), pos.above(2), pos.above(), null);
        long now = 3000;
        CrosshairFeedbackManager.setClock(() -> now);

        ObstacleDetector.dispatchObstacleAlert(result, 0.0, 1, true, false, VOLUME, STYLE, DIR_MODE, now);

        assertEquals(1, cognitiveEvents.size());
        CognitiveEvent event = cognitiveEvents.get(0);
        assertEquals(CognitiveEvent.OutputType.VOICE_ONLY, event.outputType());
        assertTrue(event.isVoiceEnabled());
        assertFalse(event.isSoundEnabled());
        assertNull(event.soundCue());

        assertEquals(now + 100, CrosshairFeedbackManager.getAutomaticMovementSuppressedUntil());
    }

    @Test
    @DisplayName("5. Disabled voice and sound emits nothing")
    void testDisabledVoiceAndSoundDoesNothing() {
        BlockPos pos = new BlockPos(10, 64, 20);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.WALL, pos, pos.above(), pos.above(2), pos.above(2), pos.above(), null);

        ObstacleDetector.dispatchObstacleAlert(result, 0.0, 1, false, false, VOLUME, STYLE, DIR_MODE, 1000);

        assertTrue(cognitiveEvents.isEmpty());
        assertTrue(legacyVoiceCalls.isEmpty());
        assertTrue(legacyAudioCalls.isEmpty());
    }

    @Test
    @DisplayName("6. Legacy bypass when coordinator is disabled plays identical SoundCue and calls legacy voice sink")
    void testLegacyBypassWhenCoordinatorDisabledAndPlaysIdenticalSoundCue() {
        CognitiveCoordinator.setCoordinatorEnabled(false);

        BlockPos pos = new BlockPos(12, 64, 25);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.STEP_CLIMBABLE, pos, pos.above(), pos.above(2), pos.above(2), pos, null);
        long now = 4000;

        ObstacleDetector.dispatchObstacleAlert(result, 0.0, 1, true, true, VOLUME, STYLE, DIR_MODE, now);

        assertTrue(cognitiveEvents.isEmpty(), "Cognitive pipeline must not be entered when coordinator is disabled");
        assertEquals(1, legacyVoiceCalls.size(), "Legacy voice sink must be invoked directly");
        assertEquals(1, legacyAudioCalls.size(), "Legacy audio sink must be invoked directly");

        SoundCue legacyCue = legacyAudioCalls.get(0);
        SoundCue expectedCue = ObstacleSafetyEventFactory.createSoundCue(result, VOLUME);

        assertEquals(expectedCue.soundEvent(), legacyCue.soundEvent(), "Legacy bypass must play identical SoundEvent");
        assertEquals(expectedCue.soundSource(), legacyCue.soundSource());
        assertEquals(expectedCue.position(), legacyCue.position());
        assertEquals(expectedCue.volume(), legacyCue.volume());
        assertEquals(expectedCue.pitch(), legacyCue.pitch());
        assertEquals(SoundEvents.NOTE_BLOCK_PLING.value(), legacyCue.soundEvent());
        assertEquals(1.5f, legacyCue.pitch());
    }

    @Test
    @DisplayName("7. STEP_CLIMBABLE event has step_climbable semantic key and NOTE_BLOCK_PLING cue")
    void testClimbableStepCognitiveDispatch() {
        BlockPos pos = new BlockPos(10, 64, 20);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.STEP_CLIMBABLE, pos, pos.above(), pos.above(2), pos.above(2), pos, null);

        ObstacleDetector.dispatchObstacleAlert(result, 0.0, 1, true, true, VOLUME, STYLE, DIR_MODE, 1000);

        assertEquals(1, cognitiveEvents.size());
        CognitiveEvent event = cognitiveEvents.get(0);
        assertEquals("safety.obstacle.step_climbable", event.semanticKey());
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_PLING.value(), event.soundCue().soundEvent());
        assertEquals(1.5f, event.soundCue().pitch());
    }

    @Test
    @DisplayName("8. Cognitive narration preserves legacy text semantics in non-default direction modes")
    void testCognitiveNarrationMatchesLegacyComposerForNonDefaultDirectionModes() {
        BlockPos pos = new BlockPos(18, 64, 30);
        ObstacleScanResult result = new ObstacleScanResult(ObstacleState.WALL, pos, pos.above(), pos.above(2), pos.above(2), pos.above(), null);
        CrosshairFeedbackManager.setTestState(new Object(), "Pietra", 3);

        Config.ObstacleDetector.DirectionFeedbackMode[] modes = {
                Config.ObstacleDetector.DirectionFeedbackMode.EIGHT_DIRECTIONS,
                Config.ObstacleDetector.DirectionFeedbackMode.OMIT_FORWARD,
                Config.ObstacleDetector.DirectionFeedbackMode.OFF
        };
        double[] angles = {30.0, 0.0, 0.0};

        for (int index = 0; index < modes.length; index++) {
            cognitiveEvents.clear();
            String rawMessage = ObstacleDetectionUtils.getNarrationMessage(result, STYLE, angles[index], modes[index]);
            String expectedLegacyText = ObstacleNarrationComposer.composeFinalNarration(
                    rawMessage,
                    2,
                    CrosshairFeedbackManager.getNarrationContextSnapshot()
            );

            ObstacleDetector.dispatchObstacleAlert(
                    result,
                    angles[index],
                    2,
                    true,
                    false,
                    VOLUME,
                    STYLE,
                    modes[index],
                    5000 + index
            );

            assertEquals(1, cognitiveEvents.size(), "A cognitive event must be emitted for " + modes[index]);
            assertEquals(expectedLegacyText, cognitiveEvents.get(0).narrationText(),
                    "Cognitive narration must match legacy composition for " + modes[index]);
            assertTrue(cognitiveEvents.get(0).narrationText().contains("Pietra"),
                    "Non-default direction mode must preserve the crosshair context for " + modes[index]);
        }
    }
}
