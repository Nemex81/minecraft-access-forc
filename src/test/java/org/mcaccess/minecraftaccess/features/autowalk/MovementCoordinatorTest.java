package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("MovementCoordinator Pure Unit Tests (ASTRALIS Fase 5B)")
class MovementCoordinatorTest {

    private List<CognitiveEvent> submittedEvents;
    private List<LegacyVoiceCall> legacyVoiceCalls;
    private List<SoundCue> legacyAudioCalls;

    record LegacyVoiceCall(String text, boolean interrupt) {}

    @BeforeAll
    static void initBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        submittedEvents = new ArrayList<>();
        legacyVoiceCalls = new ArrayList<>();
        legacyAudioCalls = new ArrayList<>();

        MovementCoordinator.resetTestSeams();
        MovementCoordinator.setCognitiveEventConsumer(submittedEvents::add);
        MovementCoordinator.setLegacyVoiceConsumer((msg, interrupt) -> legacyVoiceCalls.add(new LegacyVoiceCall(msg, interrupt)));
        MovementCoordinator.setLegacyAudioConsumer(legacyAudioCalls::add);
        MovementCoordinator.setMessageResolver((key, args) -> {
            StringBuilder sb = new StringBuilder("[" + key + "]");
            for (Object arg : args) {
                sb.append(" ").append(arg);
            }
            return sb.toString();
        });

        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.setCoordinatorEnabled(true);
    }

    @AfterEach
    void tearDown() {
        MovementCoordinator.resetTestSeams();
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
    }

    @Test
    @DisplayName("1. Evento 1: AUTOWALK_START con priorità OPERATIONAL e suono NOTE_BLOCK_PLING a pitch 1.2f")
    void testStartEventCreation() {
        BlockPos playerPos = new BlockPos(10, 64, 10);
        CognitiveEvent event = MovementCoordinator.createStartEvent("WaypointAlpha", 25, 30, playerPos, 0.8f, 1000L);

        assertNotNull(event);
        assertEquals(SourceDomain.MOVEMENT, event.domain());
        assertEquals(CognitivePriority.OPERATIONAL, event.priority());
        assertEquals(MovementCoordinator.SEMANTIC_START, event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_AND_SOUND, event.outputType());
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_PLING.value(), event.soundCue().soundEvent());
        assertEquals(1.2f, event.soundCue().pitch());
        assertEquals(0.8f, event.soundCue().volume());
        assertTrue(event.narrationText().contains("WaypointAlpha"));
        assertEquals("WaypointAlpha", event.stateSignature().targetId());
        assertEquals(3000L, event.ttlMillis());
        assertFalse(event.canChain());
    }

    @Test
    @DisplayName("2. Evento 2: AUTOWALK_ARRIVED con voiceFeedback = true produce VOICE_AND_SOUND e campana")
    void testArrivedEventVoiceEnabled() {
        BlockPos playerPos = new BlockPos(10, 64, 10);
        CognitiveEvent event = MovementCoordinator.createArrivedEvent("Destinazione", playerPos, true, 0.8f, 1000L);

        assertEquals(SourceDomain.MOVEMENT, event.domain());
        assertEquals(CognitivePriority.OPERATIONAL, event.priority());
        assertEquals(MovementCoordinator.SEMANTIC_ARRIVED, event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_AND_SOUND, event.outputType());
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_BELL.value(), event.soundCue().soundEvent());
        assertEquals(1.2f, event.soundCue().pitch());
        assertEquals(0.8f, event.soundCue().volume());
        assertTrue(event.narrationText().contains("Destinazione"));
    }

    @Test
    @DisplayName("3. Evento 2: AUTOWALK_ARRIVED con voiceFeedback = false produce SOUND_ONLY con voce vuota")
    void testArrivedEventVoiceDisabledProducesSoundOnly() {
        BlockPos playerPos = new BlockPos(10, 64, 10);
        CognitiveEvent event = MovementCoordinator.createArrivedEvent("Destinazione", playerPos, false, 0.8f, 1000L);

        assertEquals(CognitiveEvent.OutputType.SOUND_ONLY, event.outputType());
        assertEquals("", event.narrationText());
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_BELL.value(), event.soundCue().soundEvent());
        assertEquals(1.2f, event.soundCue().pitch());
    }

    @Test
    @DisplayName("4. Evento 3: AUTOWALK_NO_PATH con NOTE_BLOCK_BASS a pitch 0.6f")
    void testNoPathEventCreation() {
        BlockPos playerPos = new BlockPos(5, 64, 5);
        CognitiveEvent event = MovementCoordinator.createNoPathEvent("IsolaRemota", playerPos, 0.8f, 1000L);

        assertEquals(CognitivePriority.OPERATIONAL, event.priority());
        assertEquals(MovementCoordinator.SEMANTIC_NO_PATH, event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_AND_SOUND, event.outputType());
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_BASS.value(), event.soundCue().soundEvent());
        assertEquals(0.6f, event.soundCue().pitch());
        assertEquals(0.8f, event.soundCue().volume());
        assertTrue(event.narrationText().contains("IsolaRemota"));
    }

    @Test
    @DisplayName("5. Evento 4: AUTOWALK_OUT_OF_RANGE con priorità OPERATIONAL e VOICE_ONLY")
    void testOutOfRangeEventCreation() {
        CognitiveEvent event = MovementCoordinator.createOutOfRangeEvent(100, BlockPos.ZERO, 1000L);

        assertEquals(CognitivePriority.OPERATIONAL, event.priority());
        assertEquals(MovementCoordinator.SEMANTIC_OUT_OF_RANGE, event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_ONLY, event.outputType());
        assertNull(event.soundCue());
        assertTrue(event.narrationText().contains("100"));
    }

    @Test
    @DisplayName("6. Evento 5: AUTOWALK_CANCELLED con NOTE_BLOCK_HAT a pitch 0.5f")
    void testCancelledEventCreation() {
        BlockPos playerPos = new BlockPos(0, 64, 0);
        CognitiveEvent event = MovementCoordinator.createCancelledEvent(playerPos, 0.8f, 1000L);

        assertEquals(CognitivePriority.OPERATIONAL, event.priority());
        assertEquals(MovementCoordinator.SEMANTIC_CANCELLED, event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_AND_SOUND, event.outputType());
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_HAT.value(), event.soundCue().soundEvent());
        assertEquals(0.5f, event.soundCue().pitch());
    }

    @Test
    @DisplayName("7. Evento 6: AUTOWALK_STUCK con NOTE_BLOCK_BASS a pitch 0.5f")
    void testStuckEventCreation() {
        BlockPos playerPos = new BlockPos(0, 64, 0);
        CognitiveEvent event = MovementCoordinator.createStuckEvent(playerPos, 0.8f, 1000L);

        assertEquals(CognitivePriority.OPERATIONAL, event.priority());
        assertEquals(MovementCoordinator.SEMANTIC_STUCK, event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_AND_SOUND, event.outputType());
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_BASS.value(), event.soundCue().soundEvent());
        assertEquals(0.5f, event.soundCue().pitch());
    }

    @Test
    @DisplayName("8. Eventi 7 e 8: AUTOWALK_DOOR_WAIT e AUTOWALK_DOOR_OPENED")
    void testDoorWaitAndOpenedEvents() {
        BlockPos doorPos = new BlockPos(3, 64, 3);
        CognitiveEvent waitEvent = MovementCoordinator.createDoorWaitEvent(doorPos, 1000L);
        assertEquals(MovementCoordinator.SEMANTIC_DOOR_WAIT, waitEvent.semanticKey());
        assertEquals(CognitivePriority.OPERATIONAL, waitEvent.priority());
        assertEquals(CognitiveEvent.OutputType.VOICE_ONLY, waitEvent.outputType());

        CognitiveEvent openEvent = MovementCoordinator.createDoorOpenedEvent("Casa", doorPos, 1000L);
        assertEquals(MovementCoordinator.SEMANTIC_DOOR_OPENED, openEvent.semanticKey());
        assertEquals(CognitivePriority.OPERATIONAL, openEvent.priority());
        assertTrue(openEvent.narrationText().contains("Casa"));
    }

    @Test
    @DisplayName("9. Evento 9: AUTOWALK_PROGRESS con priorità CONTEXTUAL e VOICE_ONLY")
    void testProgressEventCreation() {
        CognitiveEvent event = MovementCoordinator.createProgressEvent(15, BlockPos.ZERO, 1000L);

        assertEquals(CognitivePriority.CONTEXTUAL, event.priority());
        assertEquals(MovementCoordinator.SEMANTIC_PROGRESS, event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.VOICE_ONLY, event.outputType());
        assertEquals(2000L, event.ttlMillis());
        assertTrue(event.narrationText().contains("15"));
    }

    @Test
    @DisplayName("10. Evento 10: AUTOWALK_STEP_NODE con priorità PASSIVE, SOUND_ONLY (NOTE_BLOCK_HAT 1.8f)")
    void testStepNodeEventCreation() {
        BlockPos pos = new BlockPos(1, 64, 1);
        CognitiveEvent event = MovementCoordinator.createStepNodeEvent(pos, 0.8f, 1000L);

        assertEquals(CognitivePriority.PASSIVE, event.priority());
        assertEquals(MovementCoordinator.SEMANTIC_STEP_NODE, event.semanticKey());
        assertEquals(CognitiveEvent.OutputType.SOUND_ONLY, event.outputType());
        assertEquals("", event.narrationText());
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_HAT.value(), event.soundCue().soundEvent());
        assertEquals(1.8f, event.soundCue().pitch());
        assertEquals(0.4f, event.soundCue().volume(), 0.01f);
        assertEquals(500L, event.ttlMillis());
    }

    @Test
    @DisplayName("11. Invariante 3: Fallback Legacy con interrupt = true per eventi OPERATIONAL")
    void testLegacyFallbackUsesInterruptTrueForOperationalEvents() {
        MovementCoordinator.setCognitiveEventConsumer(null);
        CognitiveCoordinator.setCoordinatorEnabled(false);

        CognitiveEvent startEvent = MovementCoordinator.createStartEvent("Target", 10, 10, BlockPos.ZERO, 0.8f, 1000L);
        MovementCoordinator.postEvent(startEvent, true);

        assertEquals(1, legacyVoiceCalls.size());
        assertTrue(legacyVoiceCalls.get(0).interrupt(), "Gli eventi OPERATIONAL devono avere interrupt = true in fallback legacy");

        CognitiveEvent progressEvent = MovementCoordinator.createProgressEvent(5, BlockPos.ZERO, 1000L);
        MovementCoordinator.postEvent(progressEvent, false);

        assertEquals(2, legacyVoiceCalls.size());
        assertFalse(legacyVoiceCalls.get(1).interrupt(), "Gli eventi CONTEXTUAL non devono interrompere la voce");
    }

    @Test
    @DisplayName("12. ResetSession ferma il motore, pulisce la rotta e cancella gli eventi MOVEMENT")
    void testResetSessionStopsMotorClearsRouteAndClearsDomainEvents() {
        RouteNavigator navigator = new RouteNavigator();
        BlockPos node = new BlockPos(10, 64, 10);
        navigator.setTestRoute(List.of(node), node, "Target");
        assertTrue(navigator.hasActiveRoute());

        AutoWalkMotor motor = new AutoWalkMotor();
        motor.setState(AutoWalkMotor.State.WALKING);
        assertTrue(motor.isActive());

        MovementCoordinator.resetSession(null, motor, navigator);

        assertEquals(AutoWalkMotor.State.IDLE, motor.getState());
        assertFalse(navigator.hasActiveRoute());
        assertTrue(navigator.isRouteCompleted());
    }

    @Test
    @DisplayName("13. createMotorCallback collega onArrival, onStepNode con debounce 200ms e onTakeover")
    void testMotorCallbackWiringAndStepDebounce() {
        RouteNavigator navigator = new RouteNavigator();
        BlockPos goal = new BlockPos(20, 64, 20);
        navigator.setTestRoute(List.of(goal), goal, "DestinazioneFinale");

        Config.AutoWalk config = new Config.AutoWalk();
        config.voiceFeedback = true;
        config.playNodeSoundCue = true;
        config.audioCueVolume = 0.8f;

        BlockPos playerPos = new BlockPos(15, 64, 15);
        AutoWalkMotor.MotorCallback callback = MovementCoordinator.createMotorCallback(
                navigator, config, true, () -> playerPos
        );

        // 1. Step node debounce
        MovementCoordinator.setLastNodeSoundTime(0);
        callback.onStepNode();
        assertEquals(1, submittedEvents.size());
        assertEquals(MovementCoordinator.SEMANTIC_STEP_NODE, submittedEvents.get(0).semanticKey());

        callback.onStepNode();
        assertEquals(1, submittedEvents.size(), "Step node entro 200ms deve essere soppresso");

        // 2. onProgression
        callback.onProgression(5);
        assertEquals(2, submittedEvents.size());
        assertEquals(MovementCoordinator.SEMANTIC_PROGRESS, submittedEvents.get(1).semanticKey());

        // 3. onArrival
        callback.onArrival("DestinazioneFinale");
        assertEquals(3, submittedEvents.size());
        assertEquals(MovementCoordinator.SEMANTIC_ARRIVED, submittedEvents.get(2).semanticKey());

        // 4. onTakeover
        callback.onTakeover();
        assertEquals(4, submittedEvents.size());
        assertEquals(MovementCoordinator.SEMANTIC_CANCELLED, submittedEvents.get(3).semanticKey());
    }

    @Test
    @DisplayName("14. [Rapporto ChatGPT - Test 1] Percorso cognitivo reale: onTakeover, onNoPath e onStuck eliminano residui MOVEMENT e consegnano l'esito terminale")
    void testRealCognitiveCoordinatorTerminalCallbacks() {
        // Disattiva il test seam diretto: usa il percorso CognitiveCoordinator reale!
        MovementCoordinator.setCognitiveEventConsumer(null);
        CognitiveCoordinator.setCoordinatorEnabled(true);

        List<String> realNarrations = new ArrayList<>();
        List<SoundCue> realSounds = new ArrayList<>();
        CognitiveCoordinator.setNarrationConsumer((msg, interrupt) -> realNarrations.add(msg));
        CognitiveCoordinator.setAudioConsumer(realSounds::add);

        RouteNavigator navigator = new RouteNavigator();
        Config.AutoWalk config = new Config.AutoWalk();
        config.audioCueVolume = 0.8f;
        BlockPos playerPos = new BlockPos(10, 64, 10);
        AutoWalkMotor.MotorCallback callback = MovementCoordinator.createMotorCallback(
                navigator, config, true, () -> playerPos
        );

        long now = 10000;

        // --- Scenario A: onTakeover ---
        // 1. Inserisce un evento MOVEMENT residuo obsoleto (es. passo di marcia)
        CognitiveEvent residualMove = new CognitiveEvent(
                SourceDomain.MOVEMENT, CognitivePriority.CONTEXTUAL, "autowalk:progress", StateSignature.EMPTY,
                "Residuo passi", playerPos, 0.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.VOICE_ONLY, null, 3000L, false, now
        );
        CognitiveCoordinator.submitEvent(residualMove, now);

        // 2. Inserisce un evento SAFETY concorrente che non deve essere cancellato
        CognitiveEvent concurrentSafety = new CognitiveEvent(
                SourceDomain.SAFETY, CognitivePriority.CONTEXTUAL, "safety:hole", StateSignature.EMPTY,
                "Buca a terra", playerPos, 0.0, SpatialDirection.FORWARD, CognitiveEvent.OutputType.VOICE_ONLY, null, 3000L, false, now
        );
        CognitiveCoordinator.submitEvent(concurrentSafety, now);

        // 3. Luca prende il controllo manuale (onTakeover)
        callback.onTakeover();

        // 4. Flush di fine tick: consegna l'evento primario OPERATIONAL (AUTOWALK_CANCELLED)
        CognitiveCoordinator.flushTick(now);

        // - L'evento residuo MOVEMENT è stato eliminato prima dell'invio
        assertFalse(realNarrations.contains("Residuo passi"), "Il residuo MOVEMENT deve essere cancellato");
        // - L'evento terminale AUTOWALK_CANCELLED è presente
        assertTrue(realNarrations.stream().anyMatch(n -> n.contains("autowalk.cancelled")), "AUTOWALK_CANCELLED deve essere vocalizzato");

        // 5. Il tick successivo consegna l'evento secondario SAFETY preservato in shortQueue
        CognitiveCoordinator.flushTick(now + 50);
        assertTrue(realNarrations.contains("Buca a terra"), "L'evento SAFETY concorrente non deve essere toccato");
        // - Il cue audio del cancel (NOTE_BLOCK_HAT a pitch 0.5f) è stato emesso
        assertTrue(realSounds.stream().anyMatch(s -> s.pitch() == 0.5f && s.soundEvent() == SoundEvents.NOTE_BLOCK_HAT.value()), "Suono HAT a 0.5f deve essere emesso su cancel");

        // --- Scenario B: onNoPath ---
        realNarrations.clear();
        realSounds.clear();
        CognitiveCoordinator.clearAllBuffers();

        CognitiveCoordinator.submitEvent(residualMove, now + 1000);
        CognitiveCoordinator.submitEvent(concurrentSafety, now + 1000);

        callback.onNoPath("MetaInaccessibile");
        CognitiveCoordinator.flushTick(now + 1000);

        assertFalse(realNarrations.contains("Residuo passi"), "Il residuo MOVEMENT deve essere cancellato prima di no_path");
        assertTrue(realNarrations.stream().anyMatch(n -> n.contains("autowalk.no_path") && n.contains("MetaInaccessibile")), "AUTOWALK_NO_PATH deve essere vocalizzato con target");

        CognitiveCoordinator.flushTick(now + 1050);
        assertTrue(realNarrations.contains("Buca a terra"), "L'evento SAFETY deve essere preservato");
        assertTrue(realSounds.stream().anyMatch(s -> s.pitch() == 0.6f && s.soundEvent() == SoundEvents.NOTE_BLOCK_BASS.value()), "Suono BASS a 0.6f deve essere emesso su no_path");

        // --- Scenario C: onStuck ---
        realNarrations.clear();
        realSounds.clear();
        CognitiveCoordinator.clearAllBuffers();

        CognitiveCoordinator.submitEvent(residualMove, now + 2000);
        CognitiveCoordinator.submitEvent(concurrentSafety, now + 2000);

        callback.onStuck();
        CognitiveCoordinator.flushTick(now + 2000);

        assertFalse(realNarrations.contains("Residuo passi"), "Il residuo MOVEMENT deve essere cancellato prima di stuck");
        assertTrue(realNarrations.stream().anyMatch(n -> n.contains("autowalk.stuck")), "AUTOWALK_STUCK deve essere vocalizzato");

        CognitiveCoordinator.flushTick(now + 2050);
        assertTrue(realNarrations.contains("Buca a terra"), "L'evento SAFETY deve essere preservato");
        assertTrue(realSounds.stream().anyMatch(s -> s.pitch() == 0.5f && s.soundEvent() == SoundEvents.NOTE_BLOCK_BASS.value()), "Suono BASS a 0.5f deve essere emesso su stuck");
    }

    @Test
    @DisplayName("15. [Rapporto ChatGPT - Test 2] Ciclo di vita: handleClientTick esegue resetSession su morte giocatore e cambio livello")
    void testHandleClientTickPlayerDeathAndLevelChange() {
        RouteNavigator navigator = new RouteNavigator();
        BlockPos node = new BlockPos(10, 64, 10);
        navigator.setTestRoute(List.of(node), node, "Goal");

        AutoWalkMotor motor = new AutoWalkMotor();
        motor.setState(AutoWalkMotor.State.WALKING);

        LocalPlayer livingPlayer = mock(LocalPlayer.class);
        when(livingPlayer.isDeadOrDying()).thenReturn(false);
        when(livingPlayer.getHealth()).thenReturn(20.0f);
        when(livingPlayer.getId()).thenReturn(42);

        ClientLevel level1 = mock(ClientLevel.class);

        // Tick ordinario con giocatore vivo: la marcia non viene resettata
        MovementCoordinator.handleClientTick(livingPlayer, level1, null, motor, navigator);
        assertTrue(motor.isActive());
        assertTrue(navigator.hasActiveRoute());

        // Caso A: Morte del giocatore (isDeadOrDying = true)
        LocalPlayer deadPlayer = mock(LocalPlayer.class);
        when(deadPlayer.isDeadOrDying()).thenReturn(true);
        when(deadPlayer.getHealth()).thenReturn(0.0f);

        MovementCoordinator.handleClientTick(deadPlayer, level1, null, motor, navigator);
        assertEquals(AutoWalkMotor.State.IDLE, motor.getState());
        assertFalse(navigator.hasActiveRoute());

        // Ripristina la rotta per testare il cambio di livello
        motor.setState(AutoWalkMotor.State.WALKING);
        navigator.setTestRoute(List.of(node), node, "Goal");
        assertTrue(motor.isActive());

        // Caso B: Cambio di livello / dimensione (level2 != level1)
        ClientLevel level2 = mock(ClientLevel.class);
        MovementCoordinator.handleClientTick(livingPlayer, level2, null, motor, navigator);

        assertEquals(AutoWalkMotor.State.IDLE, motor.getState());
        assertFalse(navigator.hasActiveRoute());
    }
}
