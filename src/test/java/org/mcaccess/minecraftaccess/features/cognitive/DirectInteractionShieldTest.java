package org.mcaccess.minecraftaccess.features.cognitive;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DirectInteractionShield Policy, Priority Filtering & Critical Bypass Tests (Sotto-fase 4C)")
class DirectInteractionShieldTest {

    private final List<String> spokenMessages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        DirectInteractionShield.reset();
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
        CognitiveCoordinator.setCoordinatorEnabled(true);
        spokenMessages.clear();
        CognitiveCoordinator.setNarrationConsumer((text, interrupt) -> spokenMessages.add(text));
    }

    @AfterEach
    void tearDown() {
        DirectInteractionShield.reset();
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
    }

    @Test
    @DisplayName("1. Speech duration formula respects min(2500ms, words * 280ms + 600ms)")
    void testSpeechDurationFormula() {
        // Null or blank -> minimum base 600ms
        assertEquals(600L, DirectInteractionShield.calculateSpeechDurationMillis(null));
        assertEquals(600L, DirectInteractionShield.calculateSpeechDurationMillis(""));
        assertEquals(600L, DirectInteractionShield.calculateSpeechDurationMillis("   "));

        // 1 word -> 1 * 280 + 600 = 880ms
        assertEquals(880L, DirectInteractionShield.calculateSpeechDurationMillis("Pietra"));

        // 3 words -> 3 * 280 + 600 = 1440ms
        assertEquals(1440L, DirectInteractionShield.calculateSpeechDurationMillis("Pietra davanti saltabile"));

        // 10 words -> 10 * 280 + 600 = 3400ms -> clamped to max 2500ms
        String tenWords = "uno due tre quattro cinque sei sette otto nove dieci";
        assertEquals(2500L, DirectInteractionShield.calculateSpeechDurationMillis(tenWords));
    }

    @Test
    @DisplayName("2. Shorter activation does not reduce an existing longer shield expiration")
    void testMonotonicShieldExtension() {
        long t0 = 10000L;
        // First long message: 3 words -> 1440ms -> target 11440
        DirectInteractionShield.protectVoiceResponse("uno due tre", t0);
        assertTrue(DirectInteractionShield.isActive(t0 + 1000));
        assertTrue(DirectInteractionShield.isActive(t0 + 1439));
        assertFalse(DirectInteractionShield.isActive(t0 + 1440));

        // Shorter message at t0 + 100ms: 1 word -> 880ms -> candidate 10100 + 880 = 10980 < 11440
        DirectInteractionShield.protectVoiceResponse("uno", t0 + 100);
        // Original longer target (11440) must be preserved!
        assertTrue(DirectInteractionShield.isActive(t0 + 1200), "Shorter activation must not shrink remaining shield duration");
    }

    @Test
    @DisplayName("3. During active shield: PASSIVE dropped, CONTEXTUAL deferred, CRITICAL bypasses immediately")
    void testPriorityFilteringUnderShield() {
        long now = 10000L;
        // Activate shield for 2000ms until 12000L
        DirectInteractionShield.activate(2000L);
        assertTrue(DirectInteractionShield.isActive());

        // Event A: CRITICAL safety emergency (burrone)
        CognitiveEvent critical = CognitiveEvent.createCritical(
                SourceDomain.SAFETY,
                "safety.cliff",
                StateSignature.of(1, 3),
                "Burrone profondo davanti!",
                new BlockPos(0, 60, 0),
                1.0,
                null
        );

        // Event B: PASSIVE crosshair event
        CognitiveEvent passive = CognitiveEvent.createPassive(
                SourceDomain.EXPLORATION,
                "exploration.crosshair.target",
                StateSignature.of(3, 0),
                "Pietra a 3 blocchi",
                new BlockPos(0, 64, 3),
                3.0,
                SpatialDirection.FORWARD,
                null,
                false
        );

        // Event C: CONTEXTUAL obstacle event
        CognitiveEvent contextual = CognitiveEvent.createContextual(
                SourceDomain.SAFETY,
                "safety.step",
                StateSignature.of(1, 1),
                "Gradino saltabile",
                new BlockPos(0, 64, 1),
                1.0,
                SpatialDirection.FORWARD,
                null,
                false
        );

        // 1. Submit CRITICAL -> Must bypass shield immediately at 0ms latency!
        CognitiveCoordinator.submitEvent(critical, now);
        assertEquals(1, spokenMessages.size());
        assertEquals("Burrone profondo davanti!", spokenMessages.get(0));

        // Reset critical shield to isolate direct interaction shield test
        CognitiveCoordinator.clearAllBuffers();
        spokenMessages.clear();
        DirectInteractionShield.activate(2000L);

        // 2. Submit PASSIVE and CONTEXTUAL into tick buffer while direct shield is active
        CognitiveCoordinator.submitEvent(passive, now);
        CognitiveCoordinator.submitEvent(contextual, now);

        // Flush tick while shield is active
        CognitiveCoordinator.flushTick(now);

        // Under direct shield, PASSIVE is dropped, CONTEXTUAL is preserved in shortQueue, nothing spoken yet
        assertEquals(0, spokenMessages.size(), "Direct shield must prevent non-critical speech during active interaction");

        // 3. Reset shield to simulate speech completion
        DirectInteractionShield.reset();
        assertFalse(DirectInteractionShield.isActive());

        // Flush next tick: CONTEXTUAL should now be emitted from shortQueue!
        CognitiveCoordinator.flushTick(now + 50);
        assertEquals(1, spokenMessages.size(), "Deferred CONTEXTUAL event must be spoken once shield resets");
        assertEquals("Gradino saltabile", spokenMessages.get(0));
    }
}