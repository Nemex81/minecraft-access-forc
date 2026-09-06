package org.mcaccess.minecraftaccess.features.crosshair;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Crosshair Movement Suppression & Silent State Commit Pure Unit Tests (Fase 3B)")
class CrosshairMovementSuppressionTest {

    @BeforeEach
    void setUp() {
        CrosshairFeedbackManager.resetTestSeams();
    }

    @AfterEach
    void tearDown() {
        CrosshairFeedbackManager.resetTestSeams();
    }

    @Test
    @DisplayName("1. Automatic movement target mutation is absorbed and state is silently committed")
    void testAutomaticMovementMutationAbsorbedWithSilentCommit() {
        Object oldTarget = new Object();
        Object newTarget = new Object();

        CrosshairFeedbackManager.setTestState(oldTarget, "Vecchia Pietra", 5);

        long t0 = 1000;
        CrosshairFeedbackManager.setClock(() -> t0);
        CrosshairFeedbackManager.suppressAutomaticMovementFeedback(100);
        assertEquals(1100, CrosshairFeedbackManager.getAutomaticMovementSuppressedUntil());

        // At t0 + 50ms during active movement
        long now = t0 + 50;
        boolean absorbed = CrosshairFeedbackManager.absorbAutomaticMovementFeedbackIfSuppressed(
                true, // inActiveMovement
                true, // isTargetMutation
                false,
                newTarget,
                "Nuova Pietra",
                4,
                now
        );

        assertTrue(absorbed, "Target mutation must be absorbed during active movement suppression window");
        assertEquals(newTarget, CrosshairFeedbackManager.getCurrentTarget(), "Target must be silently committed");
        assertEquals("Nuova Pietra", CrosshairFeedbackManager.getCurrentNarration(), "Narration must be silently committed");
        assertEquals(4, CrosshairFeedbackManager.getCurrentDistance(), "Distance must be silently committed");
    }

    @Test
    @DisplayName("2. No late voice after window expires because state was already committed")
    void testNoLateVoiceAfterWindowExpires() {
        Object target = new Object();
        CrosshairFeedbackManager.setTestState(target, "Quercia", 3);

        long t0 = 1000;
        CrosshairFeedbackManager.setClock(() -> t0);
        CrosshairFeedbackManager.suppressAutomaticMovementFeedback(100);

        // Silent commit at t = 1050
        CrosshairFeedbackManager.absorbAutomaticMovementFeedbackIfSuppressed(
                true, true, false, target, "Quercia", 2, 1050
        );

        // After window expires at t = 1150
        long postExpiry = 1150;
        boolean isMutation = !java.util.Objects.equals(target, CrosshairFeedbackManager.getCurrentTarget())
                || !java.util.Objects.equals("Quercia", CrosshairFeedbackManager.getCurrentNarration());
        boolean isDistanceProgression = !java.util.Objects.equals(2, CrosshairFeedbackManager.getCurrentDistance());

        assertFalse(isMutation, "No target mutation should be flagged after expiration");
        assertFalse(isDistanceProgression, "No distance progression should be flagged after expiration");
    }

    @Test
    @DisplayName("3. Second suppression with shorter duration does not shorten existing active deadline")
    void testSecondSuppressionDoesNotShortenExistingDeadline() {
        long t0 = 1000;
        CrosshairFeedbackManager.setClock(() -> t0);
        CrosshairFeedbackManager.suppressAutomaticMovementFeedback(100); // deadline = 1100
        assertEquals(1100, CrosshairFeedbackManager.getAutomaticMovementSuppressedUntil());

        // At t = 1020, another alert requests 50ms (would be 1070 < 1100)
        CrosshairFeedbackManager.setClock(() -> 1020);
        CrosshairFeedbackManager.suppressAutomaticMovementFeedback(50);
        assertEquals(1100, CrosshairFeedbackManager.getAutomaticMovementSuppressedUntil(), "Deadline must remain 1100 (Math.max)");

        // At t = 1080, suppression is still active
        assertTrue(1080 < CrosshairFeedbackManager.getAutomaticMovementSuppressedUntil());

        // At t = 1110, suppression is expired
        assertFalse(1110 < CrosshairFeedbackManager.getAutomaticMovementSuppressedUntil());
    }

    @Test
    @DisplayName("4. Stationary player (inActiveMovement == false) is NOT suppressed")
    void testStationaryPlayerNotSuppressed() {
        Object target = new Object();
        CrosshairFeedbackManager.setTestState(null, null, null);

        CrosshairFeedbackManager.setClock(() -> 1000);
        CrosshairFeedbackManager.suppressAutomaticMovementFeedback(100);

        boolean absorbed = CrosshairFeedbackManager.absorbAutomaticMovementFeedbackIfSuppressed(
                false, // inActiveMovement = false (stationary)
                true,
                false,
                target,
                "Torcia",
                1,
                1050
        );

        assertFalse(absorbed, "Stationary player interaction must never be suppressed by movement feed barrier");
        assertNull(CrosshairFeedbackManager.getCurrentTarget(), "State must NOT be modified by stationary check");
    }

    @Test
    @DisplayName("5. Distance progression during active movement is also silently absorbed")
    void testDistanceProgressionSilentlyAbsorbed() {
        Object target = new Object();
        CrosshairFeedbackManager.setTestState(target, "Muro", 4);

        CrosshairFeedbackManager.setClock(() -> 1000);
        CrosshairFeedbackManager.suppressAutomaticMovementFeedback(100);

        boolean absorbed = CrosshairFeedbackManager.absorbAutomaticMovementFeedbackIfSuppressed(
                true,
                false, // isTargetMutation = false
                true,  // isDistanceProgression = true
                target,
                "Muro",
                3,
                1050
        );

        assertTrue(absorbed);
        assertEquals(3, CrosshairFeedbackManager.getCurrentDistance(), "Distance progression must be silently committed");
    }

    @Test
    @DisplayName("6. getNarrationContextSnapshot returns immutable copy of current crosshair state")
    void testNarrationContextSnapshot() {
        CrosshairFeedbackManager.setTestState(new Object(), "Tavolo da lavoro", 2);

        ObstacleNarrationContext snapshot = CrosshairFeedbackManager.getNarrationContextSnapshot();
        assertNotNull(snapshot);
        assertEquals("Tavolo da lavoro", snapshot.targetNarration());
        assertEquals(2, snapshot.targetDistance());
    }
}
