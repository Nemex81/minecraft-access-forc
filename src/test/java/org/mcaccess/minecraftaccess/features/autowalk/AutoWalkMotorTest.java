package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import org.mcaccess.minecraftaccess.features.safety.traversal.CrouchIntent;
import org.mcaccess.minecraftaccess.features.safety.traversal.CrouchIntentProbe;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder.PathResult;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutoWalkMotorTest {

    private AutoWalkMotor motor;

    @BeforeAll
    static void initBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static class TestMotorCallback implements AutoWalkMotor.MotorCallback {
        final List<String> events = new ArrayList<>();
        Object lastArrivalTarget = null;
        Object lastNoPathTarget = null;
        int repathRequestedCount = 0;

        @Override
        public void onArrival(Object target) {
            events.add("ARRIVAL");
            lastArrivalTarget = target;
        }

        @Override
        public void onStepNode() {
            events.add("STEP_NODE");
        }

        @Override
        public void onProgression(int remainingSteps) {
            events.add("PROGRESSION_" + remainingSteps);
        }

        @Override
        public void onDoorClosed() {
            events.add("DOOR_CLOSED");
        }

        @Override
        public void onDoorOpened(Object target) {
            events.add("DOOR_OPENED");
        }

        @Override
        public void onTakeover() {
            events.add("TAKEOVER");
        }

        @Override
        public void onNoPath(Object target) {
            events.add("NO_PATH");
            lastNoPathTarget = target;
        }

        @Override
        public void onStuck() {
            events.add("STUCK");
        }

        @Override
        public void onRepathRequested() {
            events.add("REPATH_REQUESTED");
            repathRequestedCount++;
        }
    }

    @BeforeEach
    void setUp() {
        motor = new AutoWalkMotor();
    }

    @Test
    @DisplayName("Takeover manuale: attivo su S, A, D, Shift dopo i 10 tick di grazia")
    void testHumanTakeoverOnMovementKeys() {
        motor.setStartupGraceTicks(10);
        assertFalse(motor.evaluateTakeover(true, true)); // grazia attiva

        motor.setStartupGraceTicks(0);
        assertTrue(motor.evaluateTakeover(true, true));
    }

    @Test
    @DisplayName("Guardia Cloth Config: takeover disattivato quando stopOnManualInput = false")
    void testManualInputIgnoredWhenSettingDisabled() {
        motor.setStartupGraceTicks(0);
        assertFalse(motor.evaluateTakeover(false, true));
    }

    @Test
    @DisplayName("Valutazione salto assistito (Step-Up Auto-Jump)")
    void testStepJumpCondition() {
        assertTrue(AutoWalkMotor.evaluateStepJump(true, 0.80, false, 1.0, true));
        assertTrue(AutoWalkMotor.evaluateStepJump(true, 1.40, true, 1.0, true));
        assertFalse(AutoWalkMotor.evaluateStepJump(false, 0.80, false, 1.0, true));
        assertFalse(AutoWalkMotor.evaluateStepJump(true, 0.80, false, 1.50, true));
        assertFalse(AutoWalkMotor.evaluateStepJump(true, 0.80, false, 0.20, true));
        assertFalse(AutoWalkMotor.evaluateStepJump(true, 0.80, false, 1.0, false));
    }

    @Test
    @DisplayName("Avanzamento waypoint: soglie orizzontali e vincolo verticale Math.abs(deltaY) < 1.0")
    void testWaypointAdvanceConditions() {
        assertTrue(AutoWalkMotor.evaluateWaypointAdvance(0.40, 0.5, false, false));
        assertFalse(AutoWalkMotor.evaluateWaypointAdvance(0.50, 0.5, false, false));

        assertTrue(AutoWalkMotor.evaluateWaypointAdvance(0.65, 0.5, true, true));
        assertFalse(AutoWalkMotor.evaluateWaypointAdvance(0.75, 0.5, true, true));

        // Vincolo verticale Math.abs(deltaY) < 1.0
        assertTrue(AutoWalkMotor.evaluateWaypointAdvance(0.30, 0.8, false, false));
        assertTrue(AutoWalkMotor.evaluateWaypointAdvance(0.30, -0.8, false, false));
        assertFalse(AutoWalkMotor.evaluateWaypointAdvance(0.30, 1.2, false, false));
        assertFalse(AutoWalkMotor.evaluateWaypointAdvance(0.30, -1.2, false, false));
    }

    @Test
    @DisplayName("Sterzata progressiva: rotazione clamped a 20 gradi per tick")
    void testSteeringClamp() {
        float newYawEast = AutoWalkMotor.calculateSteering(0.0f, 10.0, 0.0, 20.0f);
        assertEquals(-20.0f, newYawEast, 0.01f);

        float newYawWest = AutoWalkMotor.calculateSteering(0.0f, -10.0, 0.0, 20.0f);
        assertEquals(20.0f, newYawWest, 0.01f);

        float newYawFine = AutoWalkMotor.calculateSteering(-85.0f, 10.0, 0.0, 20.0f);
        assertEquals(-90.0f, newYawFine, 0.01f);
    }

    @Test
    @DisplayName("Frenata in curva stretta (|yawDiff| > 55 gradi e distH > 0.6m)")
    void testTurnBrakeCondition() {
        assertTrue(AutoWalkMotor.shouldBrakeForTurn(60.0f, 1.0));
        assertFalse(AutoWalkMotor.shouldBrakeForTurn(60.0f, 0.5));
        assertFalse(AutoWalkMotor.shouldBrakeForTurn(30.0f, 1.0));
    }

    @Test
    @DisplayName("Isteresi di sprint post-curva (20 tick di cooldown post deviazione > 15 gradi)")
    void testSprintCooldownHysteresis() {
        assertEquals(20, AutoWalkMotor.updateSprintCooldown(25.0f, 0));
        assertEquals(19, AutoWalkMotor.updateSprintCooldown(5.0f, 20));
        assertEquals(0, AutoWalkMotor.updateSprintCooldown(5.0f, 1));
        assertEquals(0, AutoWalkMotor.updateSprintCooldown(5.0f, 0));
    }

    @Test
    @DisplayName("Condizioni per lo sprint: abilitazione config, cooldown nullo, no shift, cibo > 6")
    void testCanSprintGuard() {
        assertTrue(AutoWalkMotor.canSprint(true, 0, false, 10.0f));
        assertFalse(AutoWalkMotor.canSprint(false, 0, false, 10.0f));
        assertFalse(AutoWalkMotor.canSprint(true, 5, false, 10.0f));
        assertFalse(AutoWalkMotor.canSprint(true, 0, true, 10.0f));
        assertFalse(AutoWalkMotor.canSprint(true, 0, false, 6.0f));
    }

    @Test
    @DisplayName("Watchdog anti-blocco: soglia repath a 12 tick, abort a 24 tick")
    void testStuckWatchdogTwoThresholds() {
        assertEquals(AutoWalkMotor.StuckAction.NONE, AutoWalkMotor.evaluateStuck(0.10, true, 0));
        assertEquals(AutoWalkMotor.StuckAction.NONE, AutoWalkMotor.evaluateStuck(0.01, true, 0));
        assertEquals(AutoWalkMotor.StuckAction.REPATH, AutoWalkMotor.evaluateStuck(0.01, true, 11));
        assertEquals(AutoWalkMotor.StuckAction.ABORT, AutoWalkMotor.evaluateStuck(0.01, true, 23));
    }

    @Test
    @DisplayName("Gestione esito repath: ALREADY_AT_TARGET porta ad ARRIVED ed è terminale")
    void testHandleRepathResultAlreadyAtTarget() {
        RouteNavigator navigator = new RouteNavigator();
        BlockPos goal = new BlockPos(10, 64, 10);
        navigator.setTestRoute(List.of(goal), goal, "TargetTest");

        Config.AutoWalk config = new Config.AutoWalk();
        TestMotorCallback callback = new TestMotorCallback();
        motor.setState(AutoWalkMotor.State.WALKING);

        PathResult alreadyAtTargetResult = PathResult.alreadyAtTarget(goal);
        boolean isTerminal = motor.handleRepathResult(alreadyAtTargetResult, null, null, navigator, config, "TargetTest", callback);

        assertTrue(isTerminal, "ALREADY_AT_TARGET deve essere terminale");
        assertEquals(AutoWalkMotor.State.ARRIVED, motor.getState());
        assertEquals("TargetTest", callback.lastArrivalTarget);
        assertEquals(0, callback.repathRequestedCount, "onRepathRequested non deve essere chiamato su esito terminale");
        assertFalse(navigator.hasActiveRoute(), "La rotta deve essere pulita all'arrivo");
    }

    @Test
    @DisplayName("Gestione esito repath: NO_PATH porta a CANCELLED, pulisce rotta e preserva bersaglio")
    void testHandleRepathResultNoPath() {
        RouteNavigator navigator = new RouteNavigator();
        BlockPos goal = new BlockPos(20, 64, 20);
        navigator.setTestRoute(List.of(goal), goal, "DestinazioneMontagna");

        Config.AutoWalk config = new Config.AutoWalk();
        TestMotorCallback callback = new TestMotorCallback();
        motor.setState(AutoWalkMotor.State.WALKING);

        PathResult noPathResult = PathResult.noPath();
        boolean isTerminal = motor.handleRepathResult(noPathResult, null, null, navigator, config, "DestinazioneMontagna", callback);

        assertTrue(isTerminal, "NO_PATH deve essere terminale");
        assertEquals(AutoWalkMotor.State.CANCELLED, motor.getState(), "Stato deve essere CANCELLED per parità storica");
        assertEquals("DestinazioneMontagna", callback.lastNoPathTarget, "Il bersaglio deve essere preservato per la notifica vocale");
        assertEquals(0, callback.repathRequestedCount, "onRepathRequested non deve essere chiamato su NO_PATH");
        assertFalse(navigator.hasActiveRoute(), "La rotta deve essere pulita su NO_PATH");
    }

    @Test
    @DisplayName("Gestione esito repath: FOUND prosegue la marcia e notifica onRepathRequested")
    void testHandleRepathResultFound() {
        RouteNavigator navigator = new RouteNavigator();
        BlockPos goal = new BlockPos(30, 64, 30);
        navigator.setTestRoute(List.of(goal), goal, "NuovaMeta");

        Config.AutoWalk config = new Config.AutoWalk();
        TestMotorCallback callback = new TestMotorCallback();
        motor.setState(AutoWalkMotor.State.WALKING);

        PathResult foundResult = PathResult.found(List.of(goal), 10.0, goal);
        boolean isYield = motor.handleRepathResult(foundResult, null, null, navigator, config, "NuovaMeta", callback);

        assertTrue(isYield, "FOUND deve sospendere il tick corrente per consentire la ripartenza pulita al tick successivo");
        assertEquals(AutoWalkMotor.State.WALKING, motor.getState());
        assertEquals(1, callback.repathRequestedCount, "onRepathRequested deve essere invocato per aggiornare il coordinatore");
    }

    @Test
    @DisplayName("Nuoto assistito: il motore assume il possesso del salto solo in acqua con autoSwim attivo")
    void testAutoSwimJumpOwnershipCondition() {
        assertTrue(AutoWalkMotor.shouldHoldJumpForAutoSwim(true, true));
        assertFalse(AutoWalkMotor.shouldHoldJumpForAutoSwim(true, false));
        assertFalse(AutoWalkMotor.shouldHoldJumpForAutoSwim(false, true));

        assertTrue(AutoWalkMotor.shouldReleaseMotorJump(true, false, true, 4), "Disabilitare autoSwim in acqua rilascia il salto del motore");
        assertTrue(AutoWalkMotor.shouldReleaseMotorJump(false, true, true, 0), "Uscire dall'acqua rilascia il salto del motore");
        assertFalse(AutoWalkMotor.shouldReleaseMotorJump(true, true, true, 0), "Il nuoto automatico attivo mantiene il salto");
        assertFalse(AutoWalkMotor.shouldReleaseMotorJump(false, true, true, 1), "Il salto su gradino mantiene i propri 4 tick");
        assertFalse(AutoWalkMotor.shouldReleaseMotorJump(false, true, false, 0), "Il motore non rilascia un salto non suo");
    }

    @Test
    @DisplayName("Livellamento sguardo a meta: lookAtTarget orienta lo yaw e livella il pitch a 0.0f per BlockPos e Waypoint")
    void testLookAtTargetLevelsPitchOnWaypointsAndBlocks() {
        LocalPlayer player = mock(LocalPlayer.class);
        when(player.getEyeY()).thenReturn(65.62);

        BlockPos targetPos = new BlockPos(10, 64, 20);
        motor.lookAtTarget(player, targetPos);

        // Verifica che lo sguardo sia orientato a quota occhi (dy = 0) e che il pitch sia forzato a 0.0f
        verify(player).lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(10.5, 65.62, 20.5));
        verify(player).setXRot(0.0f);

        // Verifica anche per Waypoint
        Waypoint wp = mock(Waypoint.class);
        when(wp.pos()).thenReturn(new BlockPos(5, 64, 15));
        motor.lookAtTarget(player, wp);

        verify(player).lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(5.5, 65.62, 15.5));
        verify(player, times(2)).setXRot(0.0f);
    }

    @Test
    @DisplayName("Sblocco sguardo: lookAt e livellamento pitch eseguiti una tantum su porta chiusa, visuale libera nei tick successivi")
    void testDoorWaitLookAtOneShotAndFreedomOfRotation() {
        TestMotorCallback callback = new TestMotorCallback();
        BlockPos doorPos = new BlockPos(10, 64, 10);
        java.util.concurrent.atomic.AtomicInteger stopCount = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger lookAtCount = new java.util.concurrent.atomic.AtomicInteger();

        Runnable stopAction = stopCount::incrementAndGet;
        java.util.function.Consumer<BlockPos> lookAtAction = pos -> lookAtCount.incrementAndGet();

        // Tick 1: Primo ingresso in attesa porta chiusa
        boolean waiting1 = motor.processDoorWait(doorPos, true, 2.0, true, callback, stopAction, lookAtAction, "PortaTarget");
        assertTrue(waiting1, "Il motore deve essere in attesa porta");
        assertEquals(1, stopCount.get(), "Il movimento deve essere fermato");
        assertEquals(1, lookAtCount.get(), "lookAt deve essere chiamato al primo ingresso");
        assertEquals(1, callback.events.stream().filter(e -> e.equals("DOOR_CLOSED")).count(), "Avviso vocale emesso una sola volta");
        assertEquals(doorPos, motor.getWaitingClosedDoorPos());

        // Tick 2: Stessa porta ancora chiusa nei tick successivi
        boolean waiting2 = motor.processDoorWait(doorPos, true, 2.0, true, callback, stopAction, lookAtAction, "PortaTarget");
        assertTrue(waiting2, "Il motore resta in attesa porta");
        assertEquals(2, stopCount.get(), "Il movimento resta fermo");
        assertEquals(1, lookAtCount.get(), "lookAt NON deve essere richiamato nei tick successivi (visuale libera!)");
        assertEquals(1, callback.events.stream().filter(e -> e.equals("DOOR_CLOSED")).count(), "Nessuno spam vocale");

        // Tick 3: Diversa porta chiusa lungo la rotta
        BlockPos otherDoorPos = new BlockPos(15, 64, 10);
        boolean waiting3 = motor.processDoorWait(otherDoorPos, true, 2.0, true, callback, stopAction, lookAtAction, "PortaTarget");
        assertTrue(waiting3);
        assertEquals(3, stopCount.get());
        assertEquals(2, lookAtCount.get(), "lookAt deve attivarsi per la nuova porta distinta");
        assertEquals(2, callback.events.stream().filter(e -> e.equals("DOOR_CLOSED")).count());

        // Tick 4: Porta aperta!
        boolean waiting4 = motor.processDoorWait(otherDoorPos, false, 2.0, true, callback, stopAction, lookAtAction, "PortaTarget");
        assertFalse(waiting4, "Il motore non è più in attesa porta aperta");
        assertNull(motor.getWaitingClosedDoorPos(), "La porta in attesa deve essere resettata a null");
        assertEquals(1, callback.events.stream().filter(e -> e.equals("DOOR_OPENED")).count(), "Notifica di porta aperta emessa");
    }

    @Test
    @DisplayName("5D.4 - Test 1: Tutela tasto spazio manuale in resetMovement (Addendum 10.5)")
    void testResetMovementPreservesManualSpaceKey() {
        net.minecraft.client.KeyMapping keyUp = mock(net.minecraft.client.KeyMapping.class);
        net.minecraft.client.KeyMapping keyJump = mock(net.minecraft.client.KeyMapping.class);

        // Caso 1: Salto premuto manualmente da Luca (motorHoldingJump = false)
        motor.setMotorHoldingJump(false);
        motor.resetMovement(keyUp, keyJump, null);

        verify(keyUp).setDown(false);
        verify(keyJump, never()).setDown(false);
        assertFalse(motor.isMotorHoldingJump());

        // Caso 2: Salto posseduto dal motore (motorHoldingJump = true)
        motor.setMotorHoldingJump(true);
        motor.resetMovement(keyUp, keyJump, null);

        verify(keyJump).setDown(false);
        assertFalse(motor.isMotorHoldingJump());
    }

    @Test
    @DisplayName("5D.4 - Test 2: FSM attesa porta: abbandono silenzioso su deviazione rotta o porta rimossa (Addendum 10.2)")
    void testDoorWaitFsmSilentAbandonmentOnRouteDivertedOrReplaced() {
        TestMotorCallback callback = new TestMotorCallback();
        BlockPos doorPos = new BlockPos(10, 64, 10);
        java.util.concurrent.atomic.AtomicInteger stopCount = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger lookAtCount = new java.util.concurrent.atomic.AtomicInteger();

        Runnable stopAction = stopCount::incrementAndGet;
        java.util.function.Consumer<BlockPos> lookAtAction = pos -> lookAtCount.incrementAndGet();

        // 1. Ingresso in attesa porta chiusa a doorPos
        assertTrue(motor.processDoorWait(doorPos, true, 2.0, true, callback, stopAction, lookAtAction, "Porta1"));
        assertEquals(doorPos, motor.getWaitingClosedDoorPos());
        assertEquals(1, callback.events.stream().filter(e -> e.equals("DOOR_CLOSED")).count());

        // 2. Stato 3: Rotta deviata/ricolcolata altrove (doorPos corrente è diverso o non chiusa)
        // Non deve emettere DOOR_OPENED! Deve azzerare waitingClosedDoorPos in silenzio
        boolean waitingAfterDivert = motor.processDoorWait(new BlockPos(20, 64, 20), false, 2.0, true, callback, stopAction, lookAtAction, "Porta1");
        assertFalse(waitingAfterDivert);
        assertNull(motor.getWaitingClosedDoorPos(), "La porta in attesa deve essere resettata");
        assertEquals(0, callback.events.stream().filter(e -> e.equals("DOOR_OPENED")).count(),
                "Non deve essere emessa alcuna notifica di porta aperta se la rotta ha semplicemente deviato");
    }

    @Test
    @DisplayName("5D.4 - Test 3: Disimpegno iniziale da nicchia porta e allineamento primo nodo (C1-C4)")
    void testInitialNodeDisengagementFixture() {
        RouteNavigator navigator = new RouteNavigator();
        BlockPos rootPos = new BlockPos(10, 64, 10);
        BlockPos nextPos = new BlockPos(10, 64, 12);
        BlockPos goalPos = new BlockPos(10, 64, 15);

        // Simulazione percorso da findPath con 3 nodi: [root, next, goal]
        List<BlockPos> path = List.of(rootPos, nextPos, goalPos);
        AutoWalkPathfinder.PathResult result = AutoWalkPathfinder.PathResult.found(path, 5.0, goalPos);

        // Installazione della rotta nel navigatore
        Vec3 startContinuous = new Vec3(10.15, 64.0, 10.50); // sfalsato all'interno del vano
        navigator.installRoute(result, startContinuous);

        // Verifica Contratto C1:
        // 1. root preservato in path
        assertEquals(rootPos, navigator.getRootBlockPos());
        // 2. currentPathIndex = 1 (punta a nextPos)
        assertEquals(1, navigator.getCurrentPathIndex());
        assertEquals(nextPos, navigator.getCurrentNodePos());
        // 3. firstSegmentPending = true
        assertTrue(navigator.isFirstSegmentPending());
        // 4. non deve essere considerato arrivato al goal
        assertFalse(navigator.isAtFinalGoal(startContinuous, null));
        // 5. passi rimanenti coerenti (2 passi dal nodo 1 al goal)
        assertEquals(2, navigator.getRemainingSteps());

        // Completamento del primo segmento
        navigator.completeFirstSegment();
        assertFalse(navigator.isFirstSegmentPending());
    }

    @Test
    @DisplayName("5D.7-R3: Shift fisico premuto attiva il Takeover manuale")
    void testManualShiftFromPhysicalKeyboardTriggersTakeover() {
        KeyMapping keyShift = mock(KeyMapping.class);
        when(keyShift.isDown()).thenReturn(true);

        // Probe GLFW fisico affidabile con Shift premuto dall'utente
        CrouchIntentProbe physicalShiftProbe = () -> new CrouchIntent(true, true);
        AutoWalkMotor motorWithProbe = new AutoWalkMotor(physicalShiftProbe);

        assertTrue(motorWithProbe.isManualMovementKeyPressed(null, null, null, keyShift),
                "Lo Shift fisico premuto dall'utente deve attivare il Takeover");
    }

    @Test
    @DisplayName("5D.7-R3: Shift sintetico di sicurezza (FallDetector) NON attiva il Takeover")
    void testSyntheticShiftFromFallProtectionIgnoredByMotor() {
        KeyMapping keyShift = mock(KeyMapping.class);
        // In Minecraft keyShift.isDown() restituisce true perché forzato da MinecraftSneakOverridePort
        when(keyShift.isDown()).thenReturn(true);

        // Probe GLFW fisico affidabile ma NON premuto dall'utente (premuto solo a livello sintetico)
        CrouchIntentProbe syntheticOnlyProbe = () -> new CrouchIntent(false, true);
        AutoWalkMotor motorWithProbe = new AutoWalkMotor(syntheticOnlyProbe);

        assertFalse(motorWithProbe.isManualMovementKeyPressed(null, null, null, keyShift),
                "L'accovacciamento sintetico del FallDetector NON deve essere scambiato per un Takeover manuale");
    }

    @Test
    @DisplayName("5D.7-R3: Tasti S, A, D attivano il Takeover indipendentemente dallo Shift")
    void testPhysicalMovementKeysSADTriggerTakeover() {
        KeyMapping keyDown = mock(KeyMapping.class);
        when(keyDown.isDown()).thenReturn(true);

        // Probe con Shift non premuto
        CrouchIntentProbe probe = () -> new CrouchIntent(false, true);
        AutoWalkMotor motorWithProbe = new AutoWalkMotor(probe);

        assertTrue(motorWithProbe.isManualMovementKeyPressed(keyDown, null, null, null),
                "La pressione di S (indietro) deve attivare il Takeover");
    }

    @Test
    @DisplayName("5D.7-R3: Fallback trasparente in ambiente headless (reliable = false)")
    void testHeadlessFallbackWithoutWindow() {
        KeyMapping keyShift = mock(KeyMapping.class);
        when(keyShift.isDown()).thenReturn(true);

        // In ambiente headless/senza finestra, reliable è false -> fallback su keyShift.isDown()
        CrouchIntentProbe headlessProbe = () -> new CrouchIntent(false, false);
        AutoWalkMotor motorWithProbe = new AutoWalkMotor(headlessProbe);

        assertTrue(motorWithProbe.isManualMovementKeyPressed(null, null, null, keyShift),
                "In assenza di probe affidabile, il fallback deve consultare keyShift");
    }
}
