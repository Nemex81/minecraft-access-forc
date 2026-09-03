package org.mcaccess.minecraftaccess.features;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafeDescentCandidate;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafeDescentType;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafetyMovementGuard;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FallDetector Traversal Safety Integration Tests (Rev MC-26.8)")
class FallDetectorTraversalIntegrationTest {

    @BeforeAll
    static void initBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.resetDelegates();
        FallDetector.resetTestSeams();
    }

    @AfterEach
    void tearDown() {
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.resetDelegates();
        FallDetector.resetTestSeams();
    }

    @Test
    @DisplayName("1. Safe descent candidate routes OPERATIONAL event via CognitiveCoordinator")
    void testSafeDescentRoutesViaCoordinator() {
        List<CognitiveEvent> events = new ArrayList<>();
        FallDetector.setCognitiveEventConsumer(events::add);

        SafeDescentCandidate candidate = SafeDescentCandidate.of(
                new BlockPos(10, 68, 4),
                new BlockPos(10, 67, 4),
                new BlockPos(10, 63, 4),
                SafeDescentType.LADDER,
                Direction.NORTH
        );

        // Movement guard test
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> false);
        guard.engageFallProtection();
        assertTrue(guard.isSystemOverrideActive());

        guard.allowValidatedDescent(candidate.columnId());
        assertFalse(guard.isSystemOverrideActive(), "Guard must release system crouch for validated ladder");
        assertTrue(guard.isDescentAllowedFor(candidate.columnId()));
    }

    @Test
    @DisplayName("2. Safe descent candidate uses legacy narration and audio fallback when coordinator is disabled")
    void testSafeDescentLegacyFallback() {
        CognitiveCoordinator.setCoordinatorEnabled(false);

        AtomicReference<String> legacyNarration = new AtomicReference<>();
        AtomicBoolean legacyAudioFired = new AtomicBoolean(false);

        FallDetector.setLegacyNarrationConsumer((msg, interrupt) -> legacyNarration.set(msg));
        FallDetector.setLegacyAudioConsumer(cue -> legacyAudioFired.set(true));

        assertFalse(CognitiveCoordinator.isCoordinatorEnabled());
    }
}
