package org.mcaccess.minecraftaccess.features;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fall Detector Cognitive Dispatcher & Integration Tests (Fase 3A)")
class FallDetectorCognitiveDispatchTest {

    private final List<NarrationRecord> emittedNarrations = new ArrayList<>();
    private final List<SoundCue> emittedSounds = new ArrayList<>();
    private final List<LegacyNarrationRecord> legacyNarrations = new ArrayList<>();
    private final List<SoundCue> legacySounds = new ArrayList<>();

    record NarrationRecord(String text, boolean interrupt) {}
    record LegacyNarrationRecord(String text, boolean interrupt) {}

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        emittedNarrations.clear();
        emittedSounds.clear();
        legacyNarrations.clear();
        legacySounds.clear();

        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.setChainedNarrationEnabled(true);
        CognitiveCoordinator.setDeduplicationWindowMs(1500);
        CognitiveCoordinator.setNarrationConsumer((text, interrupt) -> emittedNarrations.add(new NarrationRecord(text, interrupt)));
        CognitiveCoordinator.setAudioConsumer(emittedSounds::add);

        // Configure test seams on FallDetector (package-private access)
        FallDetector.resetTestSeams();
        FallDetector.setLegacyNarrationConsumer((text, interrupt) -> legacyNarrations.add(new LegacyNarrationRecord(text, interrupt)));
        FallDetector.setLegacyAudioConsumer(legacySounds::add);
        FallDetector.setCognitiveEventConsumer(event -> CognitiveCoordinator.submitEvent(event, event.timestamp()));
    }

    @AfterEach
    void tearDown() {
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
        FallDetector.resetTestSeams();
    }

    @Test
    @DisplayName("1. New fall danger emits critical fast-path with sound and voice immediately (0ms)")
    void testNewFallDangerEmitsCriticalFastPathWithSoundAndVoice() {
        BlockPos dangerPos = new BlockPos(10, 64, 20);
        long t0 = 10000;

        CognitiveEvent event = FallDetector.buildFallEvent(dangerPos, 5, 1.0, false, true, true, 0.8f, "Attenzione caduta!", t0);
        assertNotNull(event);

        FallDetector.dispatchFallAlert(event, true, true, true, "Attenzione caduta!", dangerPos, 0.8f);

        // Fast-path immediate verification
        assertEquals(1, emittedNarrations.size(), "Voice must be emitted immediately at 0ms");
        assertTrue(emittedNarrations.get(0).interrupt(), "Critical danger voice must interrupt background speech");
        assertEquals(1, emittedSounds.size(), "Audio cue must be emitted immediately");
        assertNotNull(emittedSounds.get(0).soundEvent());
        assertEquals(SoundEvents.ANVIL_HIT, emittedSounds.get(0).soundEvent());
        assertEquals(SoundSource.BLOCKS, emittedSounds.get(0).soundSource());
        assertEquals(0.8f, emittedSounds.get(0).volume());
        assertTrue(legacyNarrations.isEmpty(), "Legacy path must not be called when coordinator is active");
        assertTrue(legacySounds.isEmpty());
    }

    @Test
    @DisplayName("2. Fall danger with voice only produces narration and zero sound cues")
    void testFallDangerVoiceOnlyProducesNoSoundCue() {
        BlockPos dangerPos = new BlockPos(10, 64, 20);
        long t0 = 10000;

        CognitiveEvent event = FallDetector.buildFallEvent(dangerPos, 4, 1.2, false, true, false, 0.8f, "Attenzione caduta!", t0);
        assertNotNull(event);

        FallDetector.dispatchFallAlert(event, true, true, false, "Attenzione caduta!", dangerPos, 0.8f);

        assertEquals(1, emittedNarrations.size());
        assertTrue(emittedSounds.isEmpty(), "Sound cue list must be empty when sound is disabled");
    }

    @Test
    @DisplayName("3. Fall danger with sound only produces sound cue with ANVIL_HIT and zero spoken narrations")
    void testFallDangerSoundOnlyProducesNoSpokenText() {
        BlockPos dangerPos = new BlockPos(10, 64, 20);
        long t0 = 10000;

        CognitiveEvent event = FallDetector.buildFallEvent(dangerPos, 6, 0.8, false, false, true, 0.8f, "Attenzione caduta!", t0);
        assertNotNull(event);

        FallDetector.dispatchFallAlert(event, true, false, true, "Attenzione caduta!", dangerPos, 0.8f);

        assertEquals(1, emittedSounds.size(), "Sound cue must be emitted");
        assertNotNull(emittedSounds.get(0).soundEvent());
        assertEquals(SoundEvents.ANVIL_HIT, emittedSounds.get(0).soundEvent());
        assertEquals(SoundSource.BLOCKS, emittedSounds.get(0).soundSource());
        assertEquals(0.8f, emittedSounds.get(0).volume());
        assertTrue(emittedNarrations.isEmpty(), "Narration must be empty when voice is disabled");
    }

    @Test
    @DisplayName("4. Edge-bump debounce 1500ms is strictly preserved by the cognitive coordinator")
    void testEdgeBumpDebounce1500MsPreserved() {
        BlockPos dangerPos = new BlockPos(10, 64, 20);
        long t0 = 10000;

        CognitiveEvent event1 = FallDetector.buildFallEvent(dangerPos, 4, 0.0, true, true, true, 0.8f, "Sul ciglio", t0);
        FallDetector.dispatchFallAlert(event1, true, true, true, "Sul ciglio", dangerPos, 0.8f);

        assertEquals(1, emittedNarrations.size(), "Initial edge-bump must be emitted");
        assertEquals(1, emittedSounds.size());
        assertEquals(SoundEvents.ANVIL_HIT, emittedSounds.get(0).soundEvent());

        // Duplicate edge-bump within window (t0 + 600ms < 1500ms)
        CognitiveEvent event2 = FallDetector.buildFallEvent(dangerPos, 4, 0.0, true, true, true, 0.8f, "Sul ciglio", t0 + 600);
        FallDetector.dispatchFallAlert(event2, true, true, true, "Sul ciglio", dangerPos, 0.8f);

        assertEquals(1, emittedNarrations.size(), "Duplicate edge-bump must be debounced");
        assertEquals(1, emittedSounds.size());

        // Edge-bump after window expires (t0 + 1600ms > 1500ms)
        CognitiveEvent event3 = FallDetector.buildFallEvent(dangerPos, 4, 0.0, true, true, true, 0.8f, "Sul ciglio", t0 + 1600);
        FallDetector.dispatchFallAlert(event3, true, true, true, "Sul ciglio", dangerPos, 0.8f);

        assertEquals(2, emittedNarrations.size(), "Edge-bump must be emitted again once window expires");
        assertEquals(2, emittedSounds.size());
    }

    @Test
    @DisplayName("5. Critical fall danger silences concurrent non-critical events via critical shield")
    void testCriticalFallSilencesConcurrentNonCriticalEvents() {
        BlockPos dangerPos = new BlockPos(10, 64, 20);
        long t0 = 10000;

        CognitiveEvent fallEvent = FallDetector.buildFallEvent(dangerPos, 5, 1.0, false, true, true, 0.8f, "Attenzione caduta!", t0);
        CognitiveEvent passiveCrosshair = CognitiveEvent.createPassive(
                SourceDomain.EXPLORATION, "crosshair:stone", StateSignature.EMPTY, "Pietra", null, 2.0, SpatialDirection.FORWARD, null, false
        );

        // Submit fall alert via dispatcher
        FallDetector.dispatchFallAlert(fallEvent, true, true, true, "Attenzione caduta!", dangerPos, 0.8f);

        // Submit passive crosshair in same tick
        CognitiveCoordinator.submitEvent(passiveCrosshair, t0);

        // Fast-path immediately delivered the fall danger
        assertEquals(1, emittedNarrations.size());

        // Flush tick at t0 + 50ms during critical shield
        emittedNarrations.clear();
        CognitiveCoordinator.flushTick(t0 + 50);

        // Passive event must be dropped by critical shield
        assertTrue(emittedNarrations.isEmpty(), "Passive exploration event must be dropped while critical safety shield is active");
    }

    @Test
    @DisplayName("6. When coordinator is disabled, dispatchFallAlert executes direct legacy bypass")
    void testCoordinatorDisabledExecutesDirectLegacyBypass() {
        BlockPos dangerPos = new BlockPos(10, 64, 20);
        long t0 = 10000;

        CognitiveEvent event = FallDetector.buildFallEvent(dangerPos, 5, 1.0, false, true, true, 0.8f, "Legacy Attenzione caduta!", t0);

        // Dispatch with coordinatorEnabled = false
        FallDetector.dispatchFallAlert(event, false, true, true, "Legacy Attenzione caduta!", dangerPos, 0.8f);

        // Verify cognitive coordinator received nothing
        assertTrue(emittedNarrations.isEmpty(), "Cognitive coordinator must receive nothing on legacy bypass");
        assertTrue(emittedSounds.isEmpty());

        // Verify legacy delegates were invoked directly
        assertEquals(1, legacyNarrations.size(), "Legacy narration delegate must be called once");
        assertEquals("Legacy Attenzione caduta!", legacyNarrations.get(0).text());
        assertTrue(legacyNarrations.get(0).interrupt());
        assertEquals(1, legacySounds.size(), "Legacy audio delegate must be called once");
        assertNotNull(legacySounds.get(0).soundEvent());
        assertEquals(SoundEvents.ANVIL_HIT, legacySounds.get(0).soundEvent());
        assertEquals(SoundSource.BLOCKS, legacySounds.get(0).soundSource());
        assertEquals(0.8f, legacySounds.get(0).volume());
    }
}
