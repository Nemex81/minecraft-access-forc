package org.mcaccess.minecraftaccess.features.safety.fall;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.safety.traversal.CrouchIntent;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafetyMovementGuard;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ProximityFallDetector - Protezione Cinetica e Mutua Esclusione Acustica (Rev MC-26.18)")
class ProximityFallDetectorTest {

    private Minecraft client;
    private Player player;
    private Level level;
    private List<CognitiveEvent> emittedEvents;
    private List<SoundCue> emittedSounds;
    private List<String> emittedNarrations;
    private Config.FallDetector testConfig;

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        client = mock(Minecraft.class);
        player = mock(Player.class);
        level = mock(Level.class);

        emittedEvents = new ArrayList<>();
        emittedSounds = new ArrayList<>();
        emittedNarrations = new ArrayList<>();

        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.setChainedNarrationEnabled(true);
        CognitiveCoordinator.setNarrationConsumer((text, interrupt) -> emittedNarrations.add(text));
        CognitiveCoordinator.setAudioConsumer(emittedSounds::add);

        ProximityFallDetector.resetTestSeams();
        ProximityFallDetector.setCognitiveEventConsumer(emittedEvents::add);
        ProximityFallDetector.setLegacyNarrationConsumer((text, interrupt) -> emittedNarrations.add(text));
        ProximityFallDetector.setLegacyAudioConsumer(emittedSounds::add);

        testConfig = new Config.FallDetector();
        testConfig.enabled = true;
        testConfig.proximityEnabled = true;
        testConfig.proximityMinRange = 1;
        testConfig.proximityMaxRange = 6;
        testConfig.warningDepth = 3;
        testConfig.autoSneakDepth = 4;
        testConfig.autoSneakOnEdge = true;
        testConfig.autoSlowdown = true;
        testConfig.voiceWarning = true;
        testConfig.playAudioCues = true;
    }

    @AfterEach
    void tearDown() {
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
        ProximityFallDetector.resetTestSeams();
    }

    @Test
    @DisplayName("1. Routine validateAndNormalize esegue clamping rigido [1, 6] e Swap Guard")
    void testConfigNormalizationAndSwapGuard() {
        Config.FallDetector cfg = new Config.FallDetector();

        // Test inversione (Swap Guard)
        cfg.proximityMinRange = 5;
        cfg.proximityMaxRange = 2;
        cfg.validateAndNormalize();
        assertEquals(2, cfg.proximityMinRange, "Min deve essere scambiato con Max");
        assertEquals(5, cfg.proximityMaxRange, "Max deve essere scambiato con Min");

        // Test clamping fuori dai limiti assoluti
        cfg.proximityMinRange = -3;
        cfg.proximityMaxRange = 10;
        cfg.validateAndNormalize();
        assertEquals(1, cfg.proximityMinRange, "Min deve essere clampato a 1");
        assertEquals(6, cfg.proximityMaxRange, "Max deve essere clampato a 6");
    }

    @Test
    @DisplayName("2. Zona 1 (2..6m): emette NOTE_BLOCK_IRON_XYLOPHONE, rallenta la corsa e NON ingaggia autoSneak")
    void testZone1XylophoneAudioWarning() {
        when(player.isSprinting()).thenReturn(true);

        Clock clock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, false), crouching -> {});
        ProximityFallDetector detector = new ProximityFallDetector(clock, testConfig, guard);

        BlockPos dangerPos = new BlockPos(10, 64, 25);
        // Distanza 3.5m (Zona 1)
        ProximityFallDetector.ProximityStatus status = detector.handleDangerDetected(
                player, dangerPos, 5, 3.5, false, false
        );

        assertEquals(ProximityFallDetector.ProximityStatus.WARNING_ZONE_1, status);
        verify(player).setSprinting(false);
        assertFalse(ProximityFallDetector.isAutoSneakActive(), "Zona 1 NON deve attivare auto-accovacciamento");
        assertFalse(guard.isSystemOverrideActive());

        assertEquals(1, emittedEvents.size());
        CognitiveEvent ev = emittedEvents.get(0);
        assertEquals(CognitivePriority.OPERATIONAL, ev.priority());
        assertNotNull(ev.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), ev.soundCue().soundEvent());
    }

    @Test
    @DisplayName("3. Zona 2A (1.0..1.5m): Pre-Freno emette ANVIL_LAND, mutua esclusione xilofono e postura eretta")
    void testZone2APreBrakeWithAcousticMutualExclusivity() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, false), crouching -> {});
        ProximityFallDetector detector = new ProximityFallDetector(clock, testConfig, guard);

        BlockPos dangerPos = new BlockPos(10, 64, 25);
        // Distanza 1.2m (Zona 2A)
        ProximityFallDetector.ProximityStatus status = detector.handleDangerDetected(
                player, dangerPos, 5, 1.2, false, false
        );

        assertEquals(ProximityFallDetector.ProximityStatus.PRE_BRAKE_ZONE_2A, status);
        assertFalse(ProximityFallDetector.isAutoSneakActive(), "Zona 2A NON deve attivare auto-accovacciamento (permette rilascio manuale)");
        assertFalse(guard.isSystemOverrideActive());

        assertEquals(1, emittedEvents.size());
        CognitiveEvent ev = emittedEvents.get(0);
        assertEquals(CognitivePriority.CRITICAL, ev.priority());
        assertNotNull(ev.soundCue());

        // Mutua Esclusione Acustica: emette ANVIL_LAND e zero xilofono
        assertEquals(SoundEvents.ANVIL_LAND, ev.soundCue().soundEvent());
        assertNotEquals(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), ev.soundCue().soundEvent());
    }

    @Test
    @DisplayName("4. Zona 2B (<=0.85m): Salvataggio Meccanico ingaggia autoSneak ed emette ANVIL_LAND")
    void testZone2BMechanicalAutoSneak() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, false), crouching -> {});
        ProximityFallDetector detector = new ProximityFallDetector(clock, testConfig, guard);

        BlockPos dangerPos = new BlockPos(10, 64, 25);
        // Distanza 0.5m (Zona 2B)
        ProximityFallDetector.ProximityStatus status = detector.handleDangerDetected(
                player, dangerPos, 5, 0.5, false, false
        );

        assertEquals(ProximityFallDetector.ProximityStatus.MECHANICAL_BRAKE_ZONE_2B, status);
        assertTrue(ProximityFallDetector.isAutoSneakActive(), "Zona 2B DEVE attivare autoSneakActive");
        assertTrue(guard.isSystemOverrideActive(), "Zona 2B DEVE ingaggiare il blocco meccanico di SafetyMovementGuard");

        assertEquals(1, emittedEvents.size());
        CognitiveEvent ev = emittedEvents.get(0);
        assertEquals(CognitivePriority.CRITICAL, ev.priority());
        assertNotNull(ev.soundCue());
        assertEquals(SoundEvents.ANVIL_LAND, ev.soundCue().soundEvent());
    }

    @Test
    @DisplayName("5. In AutoWalk la voce e lo xilofono ordinario sono zittiti a monte")
    void testAutoWalkSensoryQuieting() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, false), crouching -> {});
        ProximityFallDetector detector = new ProximityFallDetector(clock, testConfig, guard);

        BlockPos dangerPos = new BlockPos(10, 64, 25);
        // Distanza 3.0m in marcia AutoWalk con silenceFallVoiceWarnings = true
        ProximityFallDetector.ProximityStatus status = detector.handleDangerDetected(
                player, dangerPos, 5, 3.0, true, true
        );

        assertEquals(ProximityFallDetector.ProximityStatus.WARNING_ZONE_1, status);
        // Con voce e suono zittiti in AutoWalk, nessun evento o suono deve essere emesso a vuoto
        assertTrue(emittedEvents.isEmpty(), "In AutoWalk durante Zona 1 non devono essere sparati eventi passivi");
        assertTrue(emittedSounds.isEmpty());
        assertTrue(emittedNarrations.isEmpty());
    }

    @Test
    @DisplayName("6. Matrice booleana shouldSilenceFallVoiceWarnings")
    void testShouldSilenceFallVoiceWarningsMatrix() {
        assertTrue(ProximityFallDetector.shouldSilenceFallVoiceWarnings(true, true));
        assertFalse(ProximityFallDetector.shouldSilenceFallVoiceWarnings(true, false));
        assertFalse(ProximityFallDetector.shouldSilenceFallVoiceWarnings(false, true));
        assertFalse(ProximityFallDetector.shouldSilenceFallVoiceWarnings(false, false));
    }

    @Test
    @DisplayName("7. Transizione continua sulla stessa buca: Zona 1 emette xilofono, poi avanzamento a 1.2m emette incudine (PRAPI-1)")
    void testZone1ToZone2AEscalationOnSameDanger() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, false), crouching -> {});
        ProximityFallDetector detector = new ProximityFallDetector(clock, testConfig, guard);

        BlockPos sameDangerPos = new BlockPos(10, 64, 25);

        // Passo 1: il giocatore si trova a 3.5m (Zona 1)
        detector.handleDangerDetected(player, sameDangerPos, 5, 3.5, false, false);
        assertEquals(1, emittedEvents.size(), "Zona 1 deve emettere il primo avviso");
        assertEquals(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), emittedEvents.get(0).soundCue().soundEvent());

        // Passo 2: il giocatore avanza verso la STESSA buca a 1.2m (Zona 2A)
        detector.handleDangerDetected(player, sameDangerPos, 5, 1.2, false, false);
        assertEquals(2, emittedEvents.size(), "L'avanzamento in Zona 2A deve emettere un secondo evento anche se il BlockPos è identico");
        assertEquals(SoundEvents.ANVIL_LAND, emittedEvents.get(1).soundCue().soundEvent(), "Il secondo evento deve essere ANVIL_LAND");
        assertEquals(CognitivePriority.CRITICAL, emittedEvents.get(1).priority());
    }
}
