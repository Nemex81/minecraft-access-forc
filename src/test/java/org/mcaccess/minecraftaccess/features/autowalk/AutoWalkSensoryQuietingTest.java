package org.mcaccess.minecraftaccess.features.autowalk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.FallDetector;
import org.mcaccess.minecraftaccess.features.ObstacleDetector;
import org.mcaccess.minecraftaccess.features.cognitive.DirectInteractionShield;
import org.mcaccess.minecraftaccess.features.crosshair.CrosshairFeedbackManager;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AutoWalk Sensory Quieting Tests (ASTRALIS D1..D6)")
class AutoWalkSensoryQuietingTest {

    @BeforeEach
    void setUp() {
        MovementCoordinator.resetTestSeams();
        DirectInteractionShield.reset();
    }

    @AfterEach
    void tearDown() {
        MovementCoordinator.resetTestSeams();
        DirectInteractionShield.reset();
    }

    @Test
    @DisplayName("D1: Default Config Values for Sensory Quieting are all true")
    void testDefaultConfigValuesAreAllTrue() {
        Config.AutoWalk config = new Config.AutoWalk();
        assertTrue(config.silenceCrosshairDuringWalk, "silenceCrosshairDuringWalk must be true by default");
        assertTrue(config.silenceObstaclesDuringWalk, "silenceObstaclesDuringWalk must be true by default");
        assertTrue(config.silenceFallWarningsDuringWalk, "silenceFallWarningsDuringWalk must be true by default");
        assertTrue(config.voiceFeedback, "voiceFeedback must be true by default");
    }

    @Test
    @DisplayName("D2: MovementCoordinator.isAutoWalkActive test seam and state resolution")
    void testMovementCoordinatorIsAutoWalkActiveQuery() {
        // Initially without active instance or seam, false
        MovementCoordinator.setTestAutoWalkActive(null);
        MovementCoordinator.setActiveInstance(null);
        assertFalse(MovementCoordinator.isAutoWalkActive());

        // Test seam override true
        MovementCoordinator.setTestAutoWalkActive(true);
        assertTrue(MovementCoordinator.isAutoWalkActive());

        // Test seam override false
        MovementCoordinator.setTestAutoWalkActive(false);
        assertFalse(MovementCoordinator.isAutoWalkActive());

        // Reset clears seam
        MovementCoordinator.resetTestSeams();
        assertFalse(MovementCoordinator.isAutoWalkActive());
    }

    @Test
    @DisplayName("D3: CrosshairFeedbackManager.shouldSilenceCrosshair 4-state boolean matrix")
    void testCrosshairDecisionMatrix() {
        // Both true -> silenced
        assertTrue(CrosshairFeedbackManager.shouldSilenceCrosshair(true, true));

        // AutoWalk active but config false -> NOT silenced
        assertFalse(CrosshairFeedbackManager.shouldSilenceCrosshair(true, false));

        // AutoWalk inactive but config true -> NOT silenced
        assertFalse(CrosshairFeedbackManager.shouldSilenceCrosshair(false, true));

        // Both false -> NOT silenced
        assertFalse(CrosshairFeedbackManager.shouldSilenceCrosshair(false, false));
    }

    @Test
    @DisplayName("D4: ObstacleDetector.shouldSilenceObstacles 4-state boolean matrix")
    void testObstaclesDecisionMatrix() {
        // Both true -> silenced
        assertTrue(ObstacleDetector.shouldSilenceObstacles(true, true));

        // AutoWalk active but config false -> NOT silenced
        assertFalse(ObstacleDetector.shouldSilenceObstacles(true, false));

        // AutoWalk inactive but config true -> NOT silenced
        assertFalse(ObstacleDetector.shouldSilenceObstacles(false, true));

        // Both false -> NOT silenced
        assertFalse(ObstacleDetector.shouldSilenceObstacles(false, false));
    }

    @Test
    @DisplayName("D5: FallDetector.shouldSilenceFallVoiceWarnings 4-state boolean matrix")
    void testFallVoiceWarningsDecisionMatrix() {
        // Both true -> silenced
        assertTrue(FallDetector.shouldSilenceFallVoiceWarnings(true, true));

        // AutoWalk active but config false -> NOT silenced
        assertFalse(FallDetector.shouldSilenceFallVoiceWarnings(true, false));

        // AutoWalk inactive but config true -> NOT silenced
        assertFalse(FallDetector.shouldSilenceFallVoiceWarnings(false, true));

        // Both false -> NOT silenced
        assertFalse(FallDetector.shouldSilenceFallVoiceWarnings(false, false));
    }

    @Test
    @DisplayName("Invariante 2: DirectInteractionShield protects manual command responses during AutoWalk")
    void testDirectInteractionShieldPreservesManualCrosshair() {
        MovementCoordinator.setTestAutoWalkActive(true);
        assertTrue(MovementCoordinator.isAutoWalkActive());

        assertFalse(DirectInteractionShield.isActive());
        DirectInteractionShield.protectVoiceResponse("Pietra a 2 blocchi");
        assertTrue(DirectInteractionShield.isActive());
    }

    @Test
    @DisplayName("Invariante 3: Instant awakening when AutoWalk transitions to inactive")
    void testInstantAwakeningWhenAutoWalkTransitionsToInactive() {
        // Active
        MovementCoordinator.setTestAutoWalkActive(true);
        assertTrue(CrosshairFeedbackManager.shouldSilenceCrosshair(MovementCoordinator.isAutoWalkActive(), true));
        assertTrue(ObstacleDetector.shouldSilenceObstacles(MovementCoordinator.isAutoWalkActive(), true));
        assertTrue(FallDetector.shouldSilenceFallVoiceWarnings(MovementCoordinator.isAutoWalkActive(), true));

        // Inactive (e.g. arrival or cancellation)
        MovementCoordinator.setTestAutoWalkActive(false);
        assertFalse(CrosshairFeedbackManager.shouldSilenceCrosshair(MovementCoordinator.isAutoWalkActive(), true));
        assertFalse(ObstacleDetector.shouldSilenceObstacles(MovementCoordinator.isAutoWalkActive(), true));
        assertFalse(FallDetector.shouldSilenceFallVoiceWarnings(MovementCoordinator.isAutoWalkActive(), true));
    }
}
