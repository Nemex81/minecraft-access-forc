package org.mcaccess.minecraftaccess.features.cognitive;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cognitive Coordinator Pure Unit Tests (ASTRALIS v2.5.5 Phase 1 - Final Certification)")
class CognitiveCoordinatorTest {

    private final List<NarrationRecord> emittedNarrations = new ArrayList<>();
    private final List<SoundCue> emittedSounds = new ArrayList<>();

    record NarrationRecord(String text, boolean interrupt) {}

    @BeforeEach
    void setUp() {
        emittedNarrations.clear();
        emittedSounds.clear();
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.setChainedNarrationEnabled(true);
        CognitiveCoordinator.setDeduplicationWindowMs(1500);
        CognitiveCoordinator.setNarrationConsumer((text, interrupt) -> emittedNarrations.add(new NarrationRecord(text, interrupt)));
        CognitiveCoordinator.setAudioConsumer(emittedSounds::add);
        CognitiveCoordinator.setTemplateResolver((key, first, second) -> "[" + key + "] " + first + " + " + second);
    }

    @AfterEach
    void tearDown() {
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
    }

    @Test
    @DisplayName("1. [Bloccante 1] Critical shield suppresses operational event during emergency, then delivers it after expiry")
    void testCriticalShieldSuppressesOperationalAndDefersIt() {
        long t0 = 10000;

        CognitiveEvent cliff = CognitiveEvent.createCritical(
                SourceDomain.SAFETY, "pit:cliff", StateSignature.of(1, 3), "Burrone davanti!", BlockPos.ZERO, 1.0, null
        );
        CognitiveEvent destination = new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                "autowalk:arrival",
                StateSignature.EMPTY,
                "Destinazione raggiunta",
                BlockPos.ZERO,
                0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000,
                true,
                t0
        );

        // Submit both in same tick
        CognitiveCoordinator.submitEvent(cliff, t0);
        CognitiveCoordinator.submitEvent(destination, t0);

        // Fast-path immediately emitted the cliff warning
        assertEquals(1, emittedNarrations.size());
        assertEquals("Burrone davanti!", emittedNarrations.get(0).text());

        // Flush during active critical shield (t0 + 50ms)
        emittedNarrations.clear();
        CognitiveCoordinator.flushTick(t0 + 50);

        // OPERATIONAL must NOT be emitted during critical emergency shield!
        assertTrue(emittedNarrations.isEmpty(), "Operational event must be suppressed while critical shield is active");

        // Flush after critical shield expires (t0 + 1600ms), operational TTL (3000ms) is still valid
        CognitiveCoordinator.flushTick(t0 + 1600);

        assertEquals(1, emittedNarrations.size(), "Deferred operational event must be emitted once critical shield expires");
        assertEquals("Destinazione raggiunta", emittedNarrations.get(0).text());
    }

    @Test
    @DisplayName("2. [Bloccante 2A] VOICE_ONLY + SOUND_ONLY produces only first text and second cue")
    void testVoiceOnlyAndSoundOnlyChaining() {
        long now = 10000;
        SoundCue cue = SoundCue.of(null, SoundSource.BLOCKS, BlockPos.ZERO, 0.7f, 1.0f);

        CognitiveEvent obstacleSoundOnly = new CognitiveEvent(
                SourceDomain.SAFETY,
                CognitivePriority.CONTEXTUAL,
                "obstacle:step",
                StateSignature.EMPTY,
                "Testo ostacolo nascosto",
                BlockPos.ZERO,
                1.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.SOUND_ONLY,
                cue,
                2500,
                true,
                now
        );

        CognitiveEvent crosshairVoiceOnly = new CognitiveEvent(
                SourceDomain.EXPLORATION,
                CognitivePriority.PASSIVE,
                "crosshair:target",
                StateSignature.EMPTY,
                "Quercia a 1 blocco",
                BlockPos.ZERO,
                1.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                1500,
                true,
                now
        );

        CognitiveCoordinator.submitEvent(obstacleSoundOnly, now);
        CognitiveCoordinator.submitEvent(crosshairVoiceOnly, now);
        CognitiveCoordinator.flushTick(now);

        // Text of obstacle must NOT be spoken because it was SOUND_ONLY!
        assertEquals(1, emittedNarrations.size(), "Only the voice-enabled event should be narrated");
        assertEquals("Quercia a 1 blocco", emittedNarrations.get(0).text());

        // But sound cue of obstacle must be played
        assertEquals(1, emittedSounds.size(), "Sound cue of SOUND_ONLY event must be played");
    }

    @Test
    @DisplayName("3. [Bloccante 2B] SOUND_ONLY + SOUND_ONLY produces zero narration and both cues")
    void testDualSoundOnlyChaining() {
        long now = 10000;
        SoundCue cue1 = SoundCue.of(null, SoundSource.BLOCKS, BlockPos.ZERO, 0.7f, 1.0f);
        SoundCue cue2 = SoundCue.of(null, SoundSource.BLOCKS, BlockPos.ZERO, 0.8f, 1.2f);

        CognitiveEvent event1 = new CognitiveEvent(
                SourceDomain.SAFETY, CognitivePriority.CONTEXTUAL, "safety:sound", StateSignature.EMPTY,
                "Ignorato", BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.SOUND_ONLY,
                cue1, 2000, true, now
        );
        CognitiveEvent event2 = new CognitiveEvent(
                SourceDomain.EXPLORATION, CognitivePriority.PASSIVE, "expl:sound", StateSignature.EMPTY,
                "Ignorato", BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.SOUND_ONLY,
                cue2, 2000, true, now
        );

        CognitiveCoordinator.submitEvent(event1, now);
        CognitiveCoordinator.submitEvent(event2, now);
        CognitiveCoordinator.flushTick(now);

        assertTrue(emittedNarrations.isEmpty(), "No voice narration allowed when both events are SOUND_ONLY");
        assertEquals(2, emittedSounds.size(), "Both authorized sound cues must be played");
    }

    @Test
    @DisplayName("4. [Bloccante 2C] SILENT + VOICE_ONLY emits only second voice and zero cue from first")
    void testSilentAndVoiceOnlyChaining() {
        long now = 10000;
        CognitiveEvent silent = new CognitiveEvent(
                SourceDomain.SAFETY, CognitivePriority.CONTEXTUAL, "safety:silent", StateSignature.EMPTY,
                "Silente", BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.SILENT,
                null, 2000, true, now
        );
        CognitiveEvent voice = new CognitiveEvent(
                SourceDomain.EXPLORATION, CognitivePriority.PASSIVE, "expl:voice", StateSignature.EMPTY,
                "Tronco visibile", BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.VOICE_ONLY,
                null, 2000, true, now
        );

        CognitiveCoordinator.submitEvent(silent, now);
        CognitiveCoordinator.submitEvent(voice, now);
        CognitiveCoordinator.flushTick(now);

        assertEquals(1, emittedNarrations.size());
        assertEquals("Tronco visibile", emittedNarrations.get(0).text());
        assertTrue(emittedSounds.isEmpty(), "SILENT event must not generate any sound");
    }

    @Test
    @DisplayName("5. [Bloccante 3] Chaining uses semantic I18N template; rejects chaining if no template exists")
    void testI18nTemplateChaining() {
        long now = 10000;

        CognitiveEvent safety = CognitiveEvent.createContextual(
                SourceDomain.SAFETY, "step", StateSignature.EMPTY, "Gradino", BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, null, true
        );
        CognitiveEvent exploration = CognitiveEvent.createPassive(
                SourceDomain.EXPLORATION, "crosshair", StateSignature.EMPTY, "Quercia", BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, null, true
        );

        CognitiveCoordinator.submitEvent(safety, now);
        CognitiveCoordinator.submitEvent(exploration, now);
        CognitiveCoordinator.flushTick(now);

        assertEquals(1, emittedNarrations.size());
        assertEquals("[minecraft_access.cognitive.join_safety_exploration] Gradino + Quercia", emittedNarrations.get(0).text());

        // Pair without template (e.g. STATUS + GUIDANCE): chaining must be denied!
        emittedNarrations.clear();
        CognitiveEvent status = CognitiveEvent.createContextual(
                SourceDomain.STATUS, "hunger", StateSignature.EMPTY, "Fame", null, -1, SpatialDirection.OMNI, null, true
        );
        CognitiveEvent guidance = CognitiveEvent.createContextual(
                SourceDomain.GUIDANCE, "mentor", StateSignature.EMPTY, "Premi C", null, -1, SpatialDirection.OMNI, null, true
        );

        CognitiveCoordinator.submitEvent(status, now);
        CognitiveCoordinator.submitEvent(guidance, now);
        CognitiveCoordinator.flushTick(now + 100);

        // Chaining denied: dominant primary is emitted individually
        assertEquals(1, emittedNarrations.size());
        assertEquals("Fame", emittedNarrations.get(0).text());
    }

    @Test
    @DisplayName("6. [F1-1] Missing template resolution denies chaining and defers secondary without hardcoded punctuation")
    void testMissingTemplateResolutionDeniesChaining() {
        long now = 10000;
        // Mock resolver returning null (template not available in active language)
        CognitiveCoordinator.setTemplateResolver((key, first, second) -> null);

        CognitiveEvent safety = new CognitiveEvent(
                SourceDomain.SAFETY, CognitivePriority.CONTEXTUAL, "step", StateSignature.EMPTY, "Gradino",
                BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.VOICE_ONLY, null,
                2500, true, now
        );
        CognitiveEvent exploration = new CognitiveEvent(
                SourceDomain.EXPLORATION, CognitivePriority.PASSIVE, "crosshair", StateSignature.EMPTY, "Quercia",
                BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.VOICE_ONLY, null,
                1500, true, now
        );

        CognitiveCoordinator.submitEvent(safety, now);
        CognitiveCoordinator.submitEvent(exploration, now);

        // First flush: must NOT produce hardcoded "Gradino. Quercia"! Only primary emitted
        CognitiveCoordinator.flushTick(now);
        assertEquals(1, emittedNarrations.size(), "Without template, primary must be emitted alone");
        assertEquals("Gradino", emittedNarrations.get(0).text());

        // Second flush within TTL (now + 100ms): secondary PASSIVE is preserved in shortQueue and emitted
        emittedNarrations.clear();
        CognitiveCoordinator.flushTick(now + 100);
        assertEquals(1, emittedNarrations.size(), "Compatible secondary must be delivered on next flush within TTL");
        assertEquals("Quercia", emittedNarrations.get(0).text());
    }

    @Test
    @DisplayName("7. [F1-1 Variant] Missing template resolution drops secondary if expired before next flush")
    void testMissingTemplateResolutionWithExpiredSecondaryDropsIt() {
        long now = 10000;
        CognitiveCoordinator.setTemplateResolver((key, first, second) -> null);

        CognitiveEvent safety = new CognitiveEvent(
                SourceDomain.SAFETY, CognitivePriority.CONTEXTUAL, "step", StateSignature.EMPTY, "Gradino",
                BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.VOICE_ONLY, null,
                2500, true, now
        );
        CognitiveEvent exploration = new CognitiveEvent(
                SourceDomain.EXPLORATION, CognitivePriority.PASSIVE, "crosshair", StateSignature.EMPTY, "Quercia",
                BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.VOICE_ONLY, null,
                1500, true, now
        );

        CognitiveCoordinator.submitEvent(safety, now);
        CognitiveCoordinator.submitEvent(exploration, now);

        // First flush at now: primary emitted, secondary queued
        CognitiveCoordinator.flushTick(now);
        assertEquals(1, emittedNarrations.size());
        assertEquals("Gradino", emittedNarrations.get(0).text());

        // Second flush after TTL expires (exploration TTL is 1500ms; flush at now + 1600ms)
        emittedNarrations.clear();
        CognitiveCoordinator.flushTick(now + 1600);
        assertTrue(emittedNarrations.isEmpty(), "Secondary must be dropped if its TTL has expired");
    }

    @Test
    @DisplayName("7. [Bloccante 4] Spatial compatibility enforcement (FORWARD+FORWARD allowed; LEFT+RIGHT denied)")
    void testSpatialCompatibilityChaining() {
        long now = 10000;

        // 1. Compatible directions: both FORWARD
        CognitiveEvent obsForward = CognitiveEvent.createContextual(
                SourceDomain.SAFETY, "step", StateSignature.EMPTY, "Ostacolo avanti", BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, null, true
        );
        CognitiveEvent targetForward = CognitiveEvent.createPassive(
                SourceDomain.EXPLORATION, "target", StateSignature.EMPTY, "Porta avanti", BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, null, true
        );

        CognitiveCoordinator.submitEvent(obsForward, now);
        CognitiveCoordinator.submitEvent(targetForward, now);
        CognitiveCoordinator.flushTick(now);

        assertEquals(1, emittedNarrations.size());
        assertTrue(emittedNarrations.get(0).text().contains("Ostacolo avanti"));
        assertTrue(emittedNarrations.get(0).text().contains("Porta avanti"));

        // 2. Incompatible directions: LEFT threat vs RIGHT target
        emittedNarrations.clear();
        CognitiveEvent threatLeft = CognitiveEvent.createContextual(
                SourceDomain.SAFETY, "threat:zombie", StateSignature.EMPTY, "Zombie a sinistra", BlockPos.ZERO, 2.0, SpatialDirection.LEFT, null, true
        );
        CognitiveEvent resourceRight = CognitiveEvent.createPassive(
                SourceDomain.EXPLORATION, "resource:iron", StateSignature.EMPTY, "Ferro a destra", BlockPos.ZERO, 2.0, SpatialDirection.RIGHT, null, true
        );

        CognitiveCoordinator.submitEvent(threatLeft, now);
        CognitiveCoordinator.submitEvent(resourceRight, now);
        CognitiveCoordinator.flushTick(now + 100);

        assertEquals(1, emittedNarrations.size(), "Conflicting spatial directions must NOT be chained");
        assertEquals("Zombie a sinistra", emittedNarrations.get(0).text());
    }

    @Test
    @DisplayName("8. Dual critical events in same tick: true on first, false on second (micro-burst, no truncation)")
    void testDualCriticalFastPath() {
        long now = 10000;
        CognitiveEvent cliffDanger = CognitiveEvent.createCritical(
                SourceDomain.SAFETY, "pit:cliff", StateSignature.of(1, 3), "Burrone davanti!", BlockPos.ZERO, 1.0, null
        );
        CognitiveEvent damageDanger = CognitiveEvent.createCritical(
                SourceDomain.SAFETY, "damage:skeleton", StateSignature.of(0, 2), "Danno Scheletro", null, 0, null
        );

        CognitiveCoordinator.submitEvent(cliffDanger, now);
        CognitiveCoordinator.submitEvent(damageDanger, now);

        assertEquals(2, emittedNarrations.size());
        assertEquals("Burrone davanti!", emittedNarrations.get(0).text());
        assertTrue(emittedNarrations.get(0).interrupt());

        assertEquals("Danno Scheletro", emittedNarrations.get(1).text());
        assertFalse(emittedNarrations.get(1).interrupt(), "Second critical must be queued to prevent truncation");
    }

    @Test
    @DisplayName("9. [F1-2] CRITICAL emergency bypasses DirectInteractionShield immediately (0ms fast-path)")
    void testCriticalBypassesDirectInteractionShield() {
        long now = 10000;
        DirectInteractionShield.activate(5000); // User is typing in EditBox

        CognitiveEvent critical = CognitiveEvent.createCritical(
                SourceDomain.SAFETY, "lava:contact", StateSignature.of(0, 3), "Lava a contatto!", BlockPos.ZERO, 0.0, null
        );

        CognitiveCoordinator.submitEvent(critical, now);

        assertEquals(1, emittedNarrations.size(), "CRITICAL must bypass DirectInteractionShield immediately");
        assertEquals("Lava a contatto!", emittedNarrations.get(0).text());
        assertTrue(emittedNarrations.get(0).interrupt());
    }

    @Test
    @DisplayName("10. [S2] DirectInteractionShield defers operational and 1 contextual event, drops passives")
    void testDirectInteractionShieldDefersOperationalAndContextual() {
        long now = 5000;
        DirectInteractionShield.activate(1000);

        CognitiveEvent operational = CognitiveEvent.createOperational(
                SourceDomain.MOVEMENT, "turn", StateSignature.EMPTY, "Svolta completata", BlockPos.ZERO, 1.0, null
        );
        CognitiveEvent contextual = CognitiveEvent.createContextual(
                SourceDomain.STATUS, "light", StateSignature.EMPTY, "Buio fitto", null, -1, SpatialDirection.OMNI, null, false
        );
        CognitiveEvent passive = CognitiveEvent.createPassive(
                SourceDomain.EXPLORATION, "stone", StateSignature.EMPTY, "Pietra", BlockPos.ZERO, 1.0, SpatialDirection.FORWARD, null, false
        );

        CognitiveCoordinator.submitEvent(operational, now);
        CognitiveCoordinator.submitEvent(contextual, now);
        CognitiveCoordinator.submitEvent(passive, now);
        CognitiveCoordinator.flushTick(now);

        assertTrue(emittedNarrations.isEmpty(), "All events held while DirectInteractionShield is active");

        // Expire direct shield and flush next tick
        DirectInteractionShield.reset();
        CognitiveCoordinator.flushTick(now + 50);

        // Operational is primary, contextual preserved
        assertEquals(1, emittedNarrations.size());
        assertEquals("Svolta completata", emittedNarrations.get(0).text());

        // Next tick delivers the preserved contextual
        emittedNarrations.clear();
        CognitiveCoordinator.flushTick(now + 100);
        assertEquals(1, emittedNarrations.size());
        assertEquals("Buio fitto", emittedNarrations.get(0).text());
    }

    @Test
    @DisplayName("11. [S1] Critical duplicate audio debounce: repeated submit within cooldown plays sound only once, escalation plays new sound")
    void testCriticalDuplicateAudioDebounce() {
        long t0 = 10000;
        SoundCue cue = SoundCue.of(null, SoundSource.BLOCKS, BlockPos.ZERO, 0.8f, 1.0f);

        CognitiveEvent warning1 = CognitiveEvent.createCritical(
                SourceDomain.SAFETY, "pit:cliff", StateSignature.of(2, 2), "Burrone a 2 blocchi", BlockPos.ZERO, 2.0, cue
        );

        // 3 submits with identical signature on edge of cliff within cooldown
        CognitiveCoordinator.submitEvent(warning1, t0);
        CognitiveCoordinator.submitEvent(warning1, t0 + 100);
        CognitiveCoordinator.submitEvent(warning1, t0 + 200);

        // Voice emitted only once
        assertEquals(1, emittedNarrations.size());
        // Sound cue must also be debounced to 1 play!
        assertEquals(1, emittedSounds.size(), "Repeated identical critical events within cooldown must not spam audio cue");

        // Escalation: stepped closer to distance 1, severity 3
        CognitiveEvent escalated = CognitiveEvent.createCritical(
                SourceDomain.SAFETY, "pit:cliff", StateSignature.of(1, 3), "Burrone a 1 blocco!", BlockPos.ZERO, 1.0, cue
        );
        CognitiveCoordinator.submitEvent(escalated, t0 + 300);

        assertEquals(2, emittedNarrations.size());
        assertEquals(2, emittedSounds.size(), "Escalated critical event must play a new sound cue");
    }

    @Test
    @DisplayName("12. Verify coordinatorEnabled=false triggers direct legacy bypass")
    void testDisabledCoordinatorBypass() {
        CognitiveCoordinator.setCoordinatorEnabled(false);

        CognitiveEvent passive = CognitiveEvent.createPassive(
                SourceDomain.EXPLORATION, "crosshair:sand", StateSignature.EMPTY, "Sabbia", null, 1.0, SpatialDirection.FORWARD, null, false
        );

        CognitiveCoordinator.submitEvent(passive);

        assertEquals(1, emittedNarrations.size(), "Disabled coordinator must bypass directly without waiting for tick flush");
        assertEquals("Sabbia", emittedNarrations.get(0).text());
    }

    @Test
    @DisplayName("13. Verify clearAllBuffers clears attention memory and prevents phantom events")
    void testClearAllBuffers() {
        long now = 5000;
        CognitiveEvent warning = CognitiveEvent.createCritical(
                SourceDomain.SAFETY, "pit:cliff", StateSignature.EMPTY, "Burrone!", BlockPos.ZERO, 1.0, null
        );
        CognitiveCoordinator.submitEvent(warning, now);
        assertNotNull(CognitiveCoordinator.getAttentionMemory().getLastDanger());

        // Player dies or changes dimension
        CognitiveCoordinator.clearAllBuffers();

        assertNull(CognitiveCoordinator.getAttentionMemory().getLastDanger());
        assertFalse(DirectInteractionShield.isActive());

        // Ensure tick flush produces zero phantom narrations
        emittedNarrations.clear();
        CognitiveCoordinator.flushTick(now + 100);
        assertTrue(emittedNarrations.isEmpty());
    }

    @Test
    @DisplayName("14. [Fase 3A] Critical SOUND_ONLY events are properly debounced by deduplicationWindowMs")
    void testCriticalSoundOnlyDebounce() {
        long t0 = 10000;
        SoundCue cue = SoundCue.of(null, SoundSource.BLOCKS, BlockPos.ZERO, 0.8f, 1.0f);
        CognitiveEvent soundAlert = CognitiveEvent.createSafetyAlert(
                "safety.fall.warning",
                CognitivePriority.CRITICAL,
                StateSignature.of(1, 3, "fall:warning"),
                "Attenzione caduta",
                BlockPos.ZERO,
                1.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.SOUND_ONLY,
                cue,
                2000,
                t0
        );

        // First emission at t0: sound emitted, no narration
        CognitiveCoordinator.submitEvent(soundAlert, t0);
        assertEquals(1, emittedSounds.size(), "Sound must be emitted immediately on fast-path");
        assertTrue(emittedNarrations.isEmpty(), "SOUND_ONLY event must produce zero narrations");

        // Duplicate emission within window (t0 + 500ms < 1500ms window): must be debounced!
        CognitiveCoordinator.submitEvent(soundAlert, t0 + 500);
        assertEquals(1, emittedSounds.size(), "Duplicate sound within debounce window must be suppressed");
        assertTrue(emittedNarrations.isEmpty());

        // Emission after window expires (t0 + 1600ms > 1500ms window): must be emitted again
        CognitiveCoordinator.submitEvent(soundAlert, t0 + 1600);
        assertEquals(2, emittedSounds.size(), "Sound must be emitted again after debounce window expires");
        assertTrue(emittedNarrations.isEmpty());
    }
    @Test
    @DisplayName("15. [Fase 5B] Selective domain event clear cleans tickBuffer, shortQueue and deduplication cache only for specified domain")
    void testSelectiveDomainEventClear() {
        long t0 = 10000;

        // 1. Submit a critical safety event to engage the critical shield (suppressing lower priority events into shortQueue)
        CognitiveEvent cliff = CognitiveEvent.createCritical(
                SourceDomain.SAFETY, "pit:cliff", StateSignature.of(1, 3), "Burrone!", BlockPos.ZERO, 1.0, null
        );
        CognitiveCoordinator.submitEvent(cliff, t0);

        // 2. Submit a movement operational event and a safety contextual event during shield
        CognitiveEvent moveOp = new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                "autowalk:start",
                StateSignature.of(10, 5, "Waypoint1"),
                "Navigazione verso Waypoint1",
                BlockPos.ZERO,
                10.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000,
                false,
                t0
        );
        CognitiveEvent safeCtx = new CognitiveEvent(
                SourceDomain.SAFETY,
                CognitivePriority.CONTEXTUAL,
                "safety:ledge",
                StateSignature.of(2, 1, "ledge"),
                "Bordo sporgente",
                BlockPos.ZERO,
                2.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000,
                false,
                t0
        );
        CognitiveCoordinator.submitEvent(moveOp, t0);
        CognitiveCoordinator.submitEvent(safeCtx, t0);

        // 3. Flush tick during critical shield (t0 + 50ms): moveOp and safeCtx are deferred to shortQueue
        emittedNarrations.clear();
        CognitiveCoordinator.flushTick(t0 + 50);
        assertTrue(emittedNarrations.isEmpty(), "Both events deferred while shield is active");

        // 4. Now also submit new events into tickBuffer
        CognitiveEvent moveBuffer = new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                "autowalk:progress",
                StateSignature.EMPTY,
                "Ancora 5 passi",
                BlockPos.ZERO,
                5.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000,
                false,
                t0 + 100
        );
        CognitiveEvent safeBuffer = new CognitiveEvent(
                SourceDomain.SAFETY,
                CognitivePriority.OPERATIONAL,
                "safety:obstacle",
                StateSignature.EMPTY,
                "Ostacolo davanti",
                BlockPos.ZERO,
                1.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000,
                false,
                t0 + 100
        );
        CognitiveCoordinator.submitEvent(moveBuffer, t0 + 100);
        CognitiveCoordinator.submitEvent(safeBuffer, t0 + 100);

        // 5. Execute selective clear for MOVEMENT domain only!
        CognitiveCoordinator.clearDomainEvents(SourceDomain.MOVEMENT);

        // 6. Flush after critical shield expires (t0 + 1600ms): only SAFETY events must emerge!
        CognitiveCoordinator.flushTick(t0 + 1600);

        // SAFETY events: safeCtx (from shortQueue) and safeBuffer (from tickBuffer)
        boolean hasMovement = emittedNarrations.stream().anyMatch(r -> r.text().contains("Navigazione") || r.text().contains("passi"));
        boolean hasSafety = emittedNarrations.stream().anyMatch(r -> r.text().contains("Bordo") || r.text().contains("Ostacolo"));

        assertFalse(hasMovement, "No MOVEMENT events should survive clearDomainEvents(MOVEMENT)");
        assertTrue(hasSafety, "SAFETY events must remain intact after clearing MOVEMENT domain");

        // 7. Rigorous Test for Level 3 Deduplication Cache Invalidation:
        CognitiveCoordinator.clearAllBuffers();
        emittedNarrations.clear();

        long tDedup = 20000;
        CognitiveEvent startWalk = new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                "autowalk:start",
                StateSignature.of(10, 5, "WaypointAlpha"),
                "Avvio marcia verso WaypointAlpha",
                BlockPos.ZERO,
                10.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000,
                false,
                tDedup
        );
        CognitiveEvent safeAlert = new CognitiveEvent(
                SourceDomain.SAFETY,
                CognitivePriority.CONTEXTUAL,
                "safety:ledge",
                StateSignature.of(1, 1, "ledge"),
                "Attenzione bordo",
                BlockPos.ZERO,
                1.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000,
                false,
                tDedup
        );

        // First emission: unchained events are sequenced cleanly over 2 ticks to prevent TTS speech truncation
        CognitiveCoordinator.submitEvent(startWalk, tDedup);
        CognitiveCoordinator.submitEvent(safeAlert, tDedup);
        CognitiveCoordinator.flushTick(tDedup);
        assertEquals(1, emittedNarrations.size(), "Primary OPERATIONAL event emitted in first tick");
        CognitiveCoordinator.flushTick(tDedup + 50);
        assertEquals(2, emittedNarrations.size(), "Secondary CONTEXTUAL event delivered from shortQueue in next tick");

        // Now, at tDedup + 300ms (well within the 1500ms deduplication window):
        // We selectively clear MOVEMENT domain events. This invalidates recentEvents for MOVEMENT, but keeps SAFETY intact.
        CognitiveCoordinator.clearDomainEvents(SourceDomain.MOVEMENT);

        emittedNarrations.clear();
        // Re-submit identical events at tDedup + 300ms
        CognitiveCoordinator.submitEvent(startWalk, tDedup + 300);
        CognitiveCoordinator.submitEvent(safeAlert, tDedup + 300);
        CognitiveCoordinator.flushTick(tDedup + 300);
        CognitiveCoordinator.flushTick(tDedup + 350);

        // Verification:
        // startWalk MUST be emitted because its deduplication cache key was cleared!
        // safeAlert MUST NOT be emitted because it was NOT cleared and is within the 1500ms deduplication window!
        assertEquals(1, emittedNarrations.size(), "Only MOVEMENT event should be emitted; SAFETY event must be suppressed as duplicate");
        assertEquals("Avvio marcia verso WaypointAlpha", emittedNarrations.get(0).text());
    }
}
