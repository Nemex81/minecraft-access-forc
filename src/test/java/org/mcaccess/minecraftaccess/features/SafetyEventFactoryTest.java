package org.mcaccess.minecraftaccess.features;

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
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Safety Event Factory & Fall Detector Event Mapper Pure Unit Tests (Fase 3A)")
class SafetyEventFactoryTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        FallDetector.resetTestSeams();
    }

    @AfterEach
    void tearDown() {
        FallDetector.resetTestSeams();
    }

    @Test
    @DisplayName("1. createSafetyAlert enforces NotNull contract on text and parameters")
    void testCreateSafetyAlertContract() {
        assertThrows(NullPointerException.class, () -> CognitiveEvent.createSafetyAlert(
                "safety.test",
                CognitivePriority.CRITICAL,
                StateSignature.EMPTY,
                null,
                BlockPos.ZERO,
                1.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_AND_SOUND,
                null,
                2000,
                1000
        ));
    }

    @Test
    @DisplayName("2. buildFallEvent throws on null or blank narration message")
    void testBuildFallEventThrowsOnNullOrBlankMessage() {
        BlockPos dangerPos = new BlockPos(10, 60, 20);
        assertThrows(NullPointerException.class, () -> FallDetector.buildFallEvent(
                dangerPos, 5, 1.2, false, true, true, 0.8f, null, 1000
        ));
        assertThrows(IllegalArgumentException.class, () -> FallDetector.buildFallEvent(
                dangerPos, 5, 1.2, false, true, true, 0.8f, "   ", 1000
        ));
    }

    @Test
    @DisplayName("3. buildFallEvent returns null when both voice and sound are disabled")
    void testBuildFallEventReturnsNullWhenBothDisabled() {
        BlockPos dangerPos = new BlockPos(10, 60, 20);
        CognitiveEvent event = FallDetector.buildFallEvent(
                dangerPos, 5, 1.2, false, false, false, 0.8f, "Attenzione caduta", 1000
        );
        assertNull(event, "When both voice and sound are disabled, no event must be built");
    }

    @Test
    @DisplayName("4. buildFallEvent creates VOICE_AND_SOUND with warning key, full state signature and real ANVIL_HIT")
    void testBuildFallEventVoiceAndSound() {
        BlockPos dangerPos = new BlockPos(10, 60, 20);
        long now = 10000;
        CognitiveEvent event = FallDetector.buildFallEvent(
                dangerPos, 4, 1.3, false, true, true, 0.8f, "Attenzione caduta davanti", now
        );

        assertNotNull(event);
        assertEquals(SourceDomain.SAFETY, event.domain());
        assertEquals(CognitivePriority.CRITICAL, event.priority());
        assertEquals("safety.fall.warning", event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_AND_SOUND, event.outputType());
        assertTrue(event.isVoiceEnabled());
        assertTrue(event.isSoundEnabled());
        assertNotNull(event.soundCue());
        assertNotNull(event.soundCue().soundEvent());
        assertEquals(SoundEvents.ANVIL_HIT, event.soundCue().soundEvent());
        assertEquals(SoundSource.BLOCKS, event.soundCue().soundSource());
        assertEquals(0.8f, event.soundCue().volume());
        assertEquals(1.0f, event.soundCue().pitch());
        assertEquals(dangerPos, event.soundCue().position());
        assertEquals(1, event.stateSignature().distanceBucket());
        assertEquals(4, event.stateSignature().severityLevel());
        assertEquals("fall:warning", event.stateSignature().targetId());
        assertEquals(SpatialDirection.FORWARD, event.direction());
        assertFalse(event.canChain());
        assertEquals(2000, event.ttlMillis());
        assertEquals(now, event.timestamp());
    }

    @Test
    @DisplayName("5. buildFallEvent creates VOICE_ONLY without sound cue")
    void testBuildFallEventVoiceOnly() {
        BlockPos dangerPos = new BlockPos(10, 60, 20);
        CognitiveEvent event = FallDetector.buildFallEvent(
                dangerPos, 6, 2.0, false, true, false, 0.8f, "Attenzione caduta davanti", 10000
        );

        assertNotNull(event);
        assertEquals(CognitiveEvent.OutputType.VOICE_ONLY, event.outputType());
        assertTrue(event.isVoiceEnabled());
        assertFalse(event.isSoundEnabled());
        assertNull(event.soundCue());
        assertEquals("Attenzione caduta davanti", event.narrationText());
    }

    @Test
    @DisplayName("6. buildFallEvent creates SOUND_ONLY preserving semantic text in record with real ANVIL_HIT")
    void testBuildFallEventSoundOnly() {
        BlockPos dangerPos = new BlockPos(10, 60, 20);
        CognitiveEvent event = FallDetector.buildFallEvent(
                dangerPos, 3, 0.8, false, false, true, 0.75f, "Attenzione caduta davanti", 10000
        );

        assertNotNull(event);
        assertEquals(CognitiveEvent.OutputType.SOUND_ONLY, event.outputType());
        assertFalse(event.isVoiceEnabled(), "isVoiceEnabled must be false for SOUND_ONLY");
        assertTrue(event.isSoundEnabled(), "isSoundEnabled must be true for SOUND_ONLY with cue");
        assertNotNull(event.soundCue());
        assertNotNull(event.soundCue().soundEvent());
        assertEquals(SoundEvents.ANVIL_HIT, event.soundCue().soundEvent());
        assertEquals(SoundSource.BLOCKS, event.soundCue().soundSource());
        assertEquals(0.75f, event.soundCue().volume());
        assertEquals("Attenzione caduta davanti", event.narrationText(), "narrationText must be preserved in contract data even for SOUND_ONLY");
    }

    @Test
    @DisplayName("7. buildFallEvent for edge-bump uses edge_bump key, distanceBucket 0 and edge_bump targetId")
    void testBuildFallEventEdgeBump() {
        BlockPos dangerPos = new BlockPos(10, 60, 20);
        CognitiveEvent event = FallDetector.buildFallEvent(
                dangerPos, 8, 0.3, true, true, true, 0.8f, "Sul ciglio", 10000
        );

        assertNotNull(event);
        assertEquals("safety.fall.edge_bump", event.semanticKey());
        assertEquals(0, event.stateSignature().distanceBucket());
        assertEquals(8, event.stateSignature().severityLevel());
        assertEquals("fall:edge_bump", event.stateSignature().targetId());
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.ANVIL_HIT, event.soundCue().soundEvent());
    }
}
