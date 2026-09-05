package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AutoWalk Harmonization & Facade Tests (ASTRALIS Fase 5C)")
class AutoWalkHarmonizationTest {

    private List<CognitiveEvent> submittedEvents;
    private List<LegacyVoiceCall> legacyVoiceCalls;
    private List<SoundCue> legacyAudioCalls;
    private Config.AutoWalk testConfig;

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

        testConfig = new Config.AutoWalk();
        testConfig.enabled = true;
        testConfig.maxRange = 64;
        testConfig.audioCueVolume = 0.8f;
        testConfig.voiceFeedback = true;
        testConfig.sprint = true;
        MovementCoordinator.setTestAutoWalkConfig(testConfig);
        MovementCoordinator.setTestNarrateHints(true);
    }

    @AfterEach
    void tearDown() {
        MovementCoordinator.resetTestSeams();
        CognitiveCoordinator.clearAllBuffers();
    }

    @Test
    @DisplayName("1. Facade: AutoWalkController delega fedelmente stato, bersaglio e attivita a MovementCoordinator")
    void testFacadeDelegationStateAndTarget() {
        RouteNavigator navigator = new RouteNavigator();
        AutoWalkMotor motor = new AutoWalkMotor();
        MovementCoordinator coordinator = new MovementCoordinator(navigator, motor);
        AutoWalkController controller = new AutoWalkController(coordinator);

        assertSame(coordinator, controller.getMovementCoordinator());
        assertEquals(AutoWalkController.State.IDLE, controller.getState());
        assertFalse(controller.isActive());
        assertNull(controller.getTargetObject());

        motor.setState(AutoWalkMotor.State.WALKING);
        assertEquals(AutoWalkController.State.WALKING, controller.getState());
        assertTrue(controller.isActive());

        BlockPos goal = new BlockPos(5, 64, 5);
        navigator.setTestRoute(List.of(goal), goal, goal);

        assertEquals(goal, controller.getTargetObject());

        motor.setState(AutoWalkMotor.State.JUMPING);
        assertEquals(AutoWalkController.State.JUMPING, controller.getState());

        motor.setState(AutoWalkMotor.State.SWIMMING);
        assertEquals(AutoWalkController.State.SWIMMING, controller.getState());

        motor.setState(AutoWalkMotor.State.ARRIVED);
        assertEquals(AutoWalkController.State.ARRIVED, controller.getState());

        motor.setState(AutoWalkMotor.State.CANCELLED);
        assertEquals(AutoWalkController.State.CANCELLED, controller.getState());
    }

    @Test
    @DisplayName("2. Facade: cancel tramite AutoWalkController ferma il motore, svuota la rotta ed emette CANCELLED")
    void testFacadeCancelDelegation() {
        RouteNavigator navigator = new RouteNavigator();
        AutoWalkMotor motor = new AutoWalkMotor();
        MovementCoordinator coordinator = new MovementCoordinator(navigator, motor);
        AutoWalkController controller = new AutoWalkController(coordinator);

        motor.setState(AutoWalkMotor.State.WALKING);
        BlockPos goal = new BlockPos(10, 64, 10);
        navigator.setTestRoute(List.of(goal), goal, goal);

        controller.cancel(true, null);

        assertEquals(AutoWalkController.State.CANCELLED, controller.getState());
        assertFalse(navigator.hasActiveRoute());
        assertFalse(controller.isActive());
        assertEquals(1, submittedEvents.size());
        assertEquals(MovementCoordinator.SEMANTIC_CANCELLED, submittedEvents.get(0).semanticKey());
        assertEquals(CognitivePriority.OPERATIONAL, submittedEvents.get(0).priority());
    }

    @Test
    @DisplayName("3. Facade: toggleSprint tramite AutoWalkController commuta configurazione e notifica con interrupt = true")
    void testFacadeToggleSprintDelegation() {
        AutoWalkController controller = new AutoWalkController();
        testConfig.sprint = true;

        controller.toggleSprint();

        assertFalse(testConfig.sprint);
        assertEquals(1, legacyVoiceCalls.size());
        assertTrue(legacyVoiceCalls.get(0).text().contains("sprint_disabled"));
        assertTrue(legacyVoiceCalls.get(0).interrupt(), "La notifica sprint deve sempre avere interrupt = true");

        controller.toggleSprint();

        assertTrue(testConfig.sprint);
        assertEquals(2, legacyVoiceCalls.size());
        assertTrue(legacyVoiceCalls.get(1).text().contains("sprint_enabled"));
        assertTrue(legacyVoiceCalls.get(1).interrupt());
    }

    @Test
    @DisplayName("4. Orchestrazione start: bersaglio fuori portata genera AUTOWALK_OUT_OF_RANGE e non avvia il motore")
    void testStartOutOfRange() {
        RouteNavigator navigator = new RouteNavigator();
        AutoWalkMotor motor = new AutoWalkMotor();
        MovementCoordinator coordinator = new MovementCoordinator(navigator, motor);

        LocalPlayer player = mock(LocalPlayer.class);
        when(player.position()).thenReturn(new Vec3(0, 64, 0));
        when(player.blockPosition()).thenReturn(new BlockPos(0, 64, 0));

        ClientLevel level = mock(ClientLevel.class);

        // Bersaglio a 100 blocchi con maxRange = 64
        BlockPos farGoal = new BlockPos(100, 64, 0);

        coordinator.start(null, player, level, farGoal);

        assertFalse(motor.isActive());
        assertEquals(AutoWalkMotor.State.IDLE, motor.getState());
        assertEquals(1, submittedEvents.size());
        assertEquals(MovementCoordinator.SEMANTIC_OUT_OF_RANGE, submittedEvents.get(0).semanticKey());
        assertEquals(CognitivePriority.OPERATIONAL, submittedEvents.get(0).priority());
        assertTrue(submittedEvents.get(0).narrationText().contains("out_of_range"));
    }

    @Test
    @DisplayName("5. Orchestrazione start: bersaglio non raggiungibile genera AUTOWALK_NO_PATH con NOTE_BLOCK_BASS a 0.6f")
    void testStartNoPath() {
        RouteNavigator navigator = new RouteNavigator();
        AutoWalkMotor motor = new AutoWalkMotor();
        MovementCoordinator coordinator = new MovementCoordinator(navigator, motor);

        LocalPlayer player = mock(LocalPlayer.class);
        when(player.position()).thenReturn(new Vec3(0, 64, 0));
        when(player.blockPosition()).thenReturn(new BlockPos(0, 64, 0));

        ClientLevel level = mock(ClientLevel.class);

        coordinator.start(null, player, level, "TargetInvalido");

        assertFalse(motor.isActive());
        assertEquals(AutoWalkMotor.State.IDLE, motor.getState());
        assertEquals(1, submittedEvents.size());
        CognitiveEvent event = submittedEvents.get(0);
        assertEquals(MovementCoordinator.SEMANTIC_NO_PATH, event.semanticKey());
        assertEquals(CognitivePriority.OPERATIONAL, event.priority());
        assertNotNull(event.soundCue());
        assertEquals(0.6f, event.soundCue().pitch());
        assertEquals(SoundEvents.NOTE_BLOCK_BASS.value(), event.soundCue().soundEvent());
    }

    @Test
    @DisplayName("6. Orchestrazione start: ALREADY_AT_TARGET porta ad ARRIVED con NOTE_BLOCK_BELL a 1.2f e soppressione passiva")
    void testStartAlreadyAtTarget() {
        RouteNavigator navigator = new RouteNavigator();
        AutoWalkMotor motor = new AutoWalkMotor();
        MovementCoordinator coordinator = new MovementCoordinator(navigator, motor);

        LocalPlayer player = mock(LocalPlayer.class);
        when(player.position()).thenReturn(new Vec3(10.2, 64.0, 10.2));
        when(player.blockPosition()).thenReturn(new BlockPos(10, 64, 10));

        ClientLevel level = mock(ClientLevel.class);
        when(level.getBlockState(any())).thenAnswer(invocation -> {
            BlockPos p = invocation.getArgument(0);
            if (p.getY() < 64) {
                return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            }
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        });

        // Bersaglio identico alla posizione corrente (distanza < 1.25)
        BlockPos currentPos = new BlockPos(10, 64, 10);

        coordinator.start(null, player, level, currentPos);

        assertFalse(motor.isActive());
        assertEquals(AutoWalkMotor.State.ARRIVED, motor.getState());
        assertFalse(navigator.hasActiveRoute());

        assertEquals(1, submittedEvents.size());
        CognitiveEvent event = submittedEvents.get(0);
        assertEquals(MovementCoordinator.SEMANTIC_ARRIVED, event.semanticKey());
        assertEquals(CognitivePriority.OPERATIONAL, event.priority());
        assertNotNull(event.soundCue());
        assertEquals(1.2f, event.soundCue().pitch());
        assertEquals(SoundEvents.NOTE_BLOCK_BELL.value(), event.soundCue().soundEvent());
    }

    @Test
    @DisplayName("7. Orchestrazione start: autoWalk disabilitata notifica disabled con interrupt = true e non avvia il motore")
    void testStartWhenDisabled() {
        RouteNavigator navigator = new RouteNavigator();
        AutoWalkMotor motor = new AutoWalkMotor();
        MovementCoordinator coordinator = new MovementCoordinator(navigator, motor);

        testConfig.enabled = false;

        LocalPlayer player = mock(LocalPlayer.class);
        when(player.position()).thenReturn(new Vec3(0, 64, 0));
        when(player.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        ClientLevel level = mock(ClientLevel.class);

        coordinator.start(null, player, level, new BlockPos(10, 64, 10));

        assertFalse(motor.isActive());
        assertEquals(0, submittedEvents.size());
        assertEquals(1, legacyVoiceCalls.size());
        assertTrue(legacyVoiceCalls.get(0).text().contains("autowalk.disabled"));
        assertTrue(legacyVoiceCalls.get(0).interrupt());
    }

    @Test
    @DisplayName("8. Orchestrazione tick: se autoWalk viene disabilitata a meta marcia, emette HAT 0.5f e annuncia disabilitato")
    void testTickDisabledCancelsMovement() {
        RouteNavigator navigator = new RouteNavigator();
        AutoWalkMotor motor = new AutoWalkMotor();
        MovementCoordinator coordinator = new MovementCoordinator(navigator, motor);

        motor.setState(AutoWalkMotor.State.WALKING);
        BlockPos goal = new BlockPos(10, 64, 10);
        navigator.setTestRoute(List.of(goal), goal, goal);

        Minecraft client = mock(Minecraft.class);
        LocalPlayer player = mock(LocalPlayer.class);
        ClientLevel level = mock(ClientLevel.class);
        client.player = player;
        client.level = level;

        when(player.position()).thenReturn(new Vec3(0, 64, 0));
        when(player.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        when(player.isDeadOrDying()).thenReturn(false);
        when(player.getHealth()).thenReturn(20.0f);
        when(player.getId()).thenReturn(1);

        testConfig.enabled = false;

        coordinator.tick(client, player, level);

        // 1. Verifica un solo cue HAT, SoundSource.BLOCKS, pitch 0.5f e volume configurato
        assertEquals(1, legacyAudioCalls.size(), "Deve essere registrato esattamente un cue audio HAT");
        SoundCue cue = legacyAudioCalls.get(0);
        assertEquals(SoundEvents.NOTE_BLOCK_HAT.value(), cue.soundEvent(), "Il suono deve essere NOTE_BLOCK_HAT");
        assertEquals(SoundSource.BLOCKS, cue.soundSource(), "La sorgente deve essere BLOCKS");
        assertEquals(0.5f, cue.pitch(), "Il pitch deve essere 0.5f");
        assertEquals(testConfig.audioCueVolume, cue.volume(), "Il volume deve rispettare la configurazione");

        // 2. Verifica una sola voce autowalk.disabled con interrupt = true
        assertEquals(1, legacyVoiceCalls.size(), "Deve essere registrata una sola notifica vocale");
        assertTrue(legacyVoiceCalls.get(0).text().contains("autowalk.disabled"), "Il messaggio deve annunciare la disabilitazione");
        assertTrue(legacyVoiceCalls.get(0).interrupt(), "L'annuncio vocale deve avere interrupt = true");

        // 3. Verifica assenza di eventi cognitivi aggiuntivi
        assertEquals(0, submittedEvents.size(), "Non devono essere emessi eventi cognitivi aggiuntivi");

        // 4. Verifica stato finale del motore
        assertFalse(motor.isActive(), "Il motore non deve essere attivo");
        assertEquals(AutoWalkMotor.State.CANCELLED, motor.getState(), "Lo stato finale deve essere CANCELLED");
    }

    @Test
    @DisplayName("9. Invariante 2: resetMovement e cancel azzerano lo stato del motore e lo sprint del giocatore")
    void testAllVirtualKeysReleasedOnCancel() {
        Minecraft client = mock(Minecraft.class);
        LocalPlayer player = mock(LocalPlayer.class);
        client.player = player;

        RouteNavigator navigator = new RouteNavigator();
        AutoWalkMotor motor = new AutoWalkMotor();
        MovementCoordinator coordinator = new MovementCoordinator(navigator, motor);

        motor.setState(AutoWalkMotor.State.WALKING);
        assertTrue(motor.isActive());

        coordinator.cancel(client, true, null);

        assertFalse(motor.isActive());
        assertEquals(AutoWalkMotor.State.CANCELLED, motor.getState());
        assertEquals(0, motor.getJumpHoldingTicks());
        assertFalse(motor.isMotorHoldingJump());
        verify(player).setSprinting(false);
    }

    @Test
    @DisplayName("10. AutoWalkManager: inizializzazione espone MovementCoordinator e la facciata AutoWalkController coerenti")
    void testAutoWalkManagerWiring() {
        AutoWalkManager manager = new AutoWalkManager();

        assertNotNull(manager.getMovementCoordinator());
        assertNotNull(manager.getController());
        assertSame(manager.getMovementCoordinator(), manager.getController().getMovementCoordinator());
        assertEquals(AutoWalkController.State.IDLE, manager.getController().getState());
        assertFalse(manager.getMovementCoordinator().isActive());
    }
}
