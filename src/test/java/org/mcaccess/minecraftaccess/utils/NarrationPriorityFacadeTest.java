package org.mcaccess.minecraftaccess.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.DirectInteractionShield;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Narration Priority Facade / Adapter Unit Tests (ASTRALIS v2.5.5 Phase 2)")
class NarrationPriorityFacadeTest {

    private final List<EmittedNarration> emittedNarrations = new ArrayList<>();
    private final List<Long> suppressedScannerDurations = new ArrayList<>();
    private final AtomicLong simulatedTime = new AtomicLong(10000);

    record EmittedNarration(String text, boolean interrupt) {}

    @BeforeEach
    void setUp() {
        emittedNarrations.clear();
        suppressedScannerDurations.clear();
        simulatedTime.set(10000);

        // Inject package-private test seams into NarrationPriority
        NarrationPriority.narrationConsumer = (text, interrupt) -> emittedNarrations.add(new EmittedNarration(text, interrupt));
        NarrationPriority.timeSupplier = simulatedTime::get;
        NarrationPriority.scannerSuppressor = suppressedScannerDurations::add;

        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.setCoordinatorEnabled(true);
    }

    @AfterEach
    void tearDown() {
        NarrationPriority.resetTestSeams();
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
    }

    @Test
    @DisplayName("1. suppressBackgroundScanners keeps local shield active with coordinator enabled and does not activate DirectInteractionShield")
    void testLegacySuppressScannersKeepsLocalShieldWithCoordinatorEnabled() {
        CognitiveCoordinator.setCoordinatorEnabled(true);
        assertFalse(NarrationPriority.isShieldActive());
        assertFalse(DirectInteractionShield.isActive());

        NarrationPriority.suppressBackgroundScanners(1500);

        assertTrue(NarrationPriority.isShieldActive(), "Local shield must be active");
        assertEquals(1, suppressedScannerDurations.size());
        assertEquals(1500L, suppressedScannerDurations.get(0));

        // DirectInteractionShield represents keyboard/GUI input, NOT toasts/packets:
        assertFalse(DirectInteractionShield.isActive(), "DirectInteractionShield must NOT be activated by NarrationPriority");

        // Advance time past 1500ms
        simulatedTime.addAndGet(1600);
        assertFalse(NarrationPriority.isShieldActive(), "Shield must expire after duration has elapsed");
    }

    @Test
    @DisplayName("2. suppressBackgroundScanners keeps local shield active when coordinator is disabled")
    void testLegacySuppressScannersKeepsLocalShieldWhenCoordinatorDisabled() {
        CognitiveCoordinator.setCoordinatorEnabled(false);
        assertFalse(NarrationPriority.isShieldActive());

        NarrationPriority.suppressBackgroundScanners(2000);

        assertTrue(NarrationPriority.isShieldActive(), "Local shield must be active even if coordinator is disabled");
        assertEquals(1, suppressedScannerDurations.size());
        assertEquals(2000L, suppressedScannerDurations.get(0));

        simulatedTime.addAndGet(2100);
        assertFalse(NarrationPriority.isShieldActive(), "Local shield must expire cleanly");
    }

    @Test
    @DisplayName("3. narrateSalient emits directly via narrator exactly once without double speech")
    void testNarrateSalientEmitsDirectlyWithoutDoubleSpeech() {
        NarrationPriority.narrateSalient("Progresso sbloccato", 1200);

        // Must emit directly exactly 1 time
        assertEquals(1, emittedNarrations.size(), "Salient narration must be emitted exactly once");
        assertEquals("Progresso sbloccato", emittedNarrations.get(0).text());
        assertTrue(emittedNarrations.get(0).interrupt(), "narrateSalient must interrupt background noise");
        assertTrue(NarrationPriority.isShieldActive(), "Shield must be active after salient call");
        assertEquals(1, suppressedScannerDurations.size());
        assertEquals(1200L, suppressedScannerDurations.get(0));
    }

    @Test
    @DisplayName("4. narrateSalientQueued emits directly with interrupt=false")
    void testNarrateSalientQueuedEmitsDirectlyWithFalseInterrupt() {
        NarrationPriority.narrateSalientQueued("Ricetta sbloccata", 1200);

        assertEquals(1, emittedNarrations.size());
        assertEquals("Ricetta sbloccata", emittedNarrations.get(0).text());
        assertFalse(emittedNarrations.get(0).interrupt(), "narrateSalientQueued must have interrupt=false to queue");
        assertTrue(NarrationPriority.isShieldActive());
        assertEquals(1, suppressedScannerDurations.size());
        assertEquals(1200L, suppressedScannerDurations.get(0));
    }

    @Test
    @DisplayName("5. isShieldActive matches expiry deterministically using controlled time supplier")
    void testIsShieldActiveMatchesExpiry() {
        NarrationPriority.suppressBackgroundScanners(1000);

        // At t = 10000 + 500 = 10500 (active)
        simulatedTime.set(10500);
        assertTrue(NarrationPriority.isShieldActive());

        // At t = 10000 + 999 = 10999 (still active)
        simulatedTime.set(10999);
        assertTrue(NarrationPriority.isShieldActive());

        // At t = 10000 + 1000 = 11000 (expired)
        simulatedTime.set(11000);
        assertFalse(NarrationPriority.isShieldActive());
    }
}
