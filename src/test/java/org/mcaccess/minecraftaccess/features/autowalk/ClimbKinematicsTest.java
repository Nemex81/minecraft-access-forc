package org.mcaccess.minecraftaccess.features.autowalk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbContactProbe;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbEntryTransition;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbLandingProbe;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbTraversal;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbableGeometry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test deterministici headless a 0 ms per ClimbKinematics (Contratto D8).
 */
class ClimbKinematicsTest {

    private ClimbTraversal createTestLadderTraversal(int bottomY, int topY) {
        BlockPos bottom = new BlockPos(-59, bottomY, -41);
        BlockPos top = new BlockPos(-59, topY, -41);
        BlockPos landing = new BlockPos(-59, topY + 1, -40);
        return ClimbTraversal.of(
                Direction.AxisDirection.POSITIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                bottom,
                top,
                bottom,
                landing,
                Direction.NORTH,
                null
        );
    }

    private ClimbTraversal createTestScaffoldingDescent(int bottomY, int topY) {
        BlockPos bottom = new BlockPos(10, bottomY, 10);
        BlockPos top = new BlockPos(10, topY, 10);
        BlockPos landing = new BlockPos(10, bottomY, 10);
        return ClimbTraversal.of(
                Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.SCAFFOLDING,
                top,
                top,
                bottom,
                landing,
                null,
                null
        );
    }

    @Test
    @DisplayName("Riproduttore Belvedere: a Y=81.0 su colonna 81..85, il tick 1 in TRANSIT NON entra in DISMOUNT")
    void testBelvedereTransitTick1DoesNotPrematurelyDismount() {
        ClimbTraversal traversal = createTestLadderTraversal(81, 85);
        BlockPos firstRung = new BlockPos(-59, 81, -41);

        // Simulazione tick 1 in TRANSIT: il giocatore e' ancora alla base (Y=81.0)
        ClimbKinematics.ClimbSnapshot snapshot = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                true,
                new Vec3(-59.5, 81.0, -41.5),
                firstRung,
                true,
                true,
                true,
                firstRung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                0,
                81.0,
                0,
                0,
                false, // Non e' l'ultimo rung (la scala continua fino a 85)
                false
        );

        ClimbKinematics.ClimbDecision decision = ClimbKinematics.evaluate(snapshot);

        // Verifica invariante: NON deve entrare in DISMOUNT
        assertNotEquals(AutoWalkMotor.ClimbSubPhase.DISMOUNT, decision.nextSubPhase(),
                "Il primo tick utile in TRANSIT non deve collassare in DISMOUNT!");
        assertEquals(AutoWalkMotor.ClimbSubPhase.TRANSIT, decision.nextSubPhase());
        assertTrue(decision.keyUp(), "In salita keyUp deve rimanere attivo!");
        assertFalse(decision.keyJump());
        assertEquals(ClimbKinematics.Outcome.CONTINUE, decision.outcome());
    }

    @Test
    @DisplayName("Mount su ladder a terra: impulso di salto limitato a 3 tick e keyUp attivo")
    void testMountOnGroundEmitsJumpPulse() {
        ClimbTraversal traversal = createTestLadderTraversal(81, 85);
        BlockPos entry = traversal.entryPos();

        // Giocatore a terra, non ancora agganciato fisicamente
        ClimbKinematics.ClimbSnapshot snapshotPulse3 = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.MOUNT,
                true,
                new Vec3(-59.5, 81.0, -41.2),
                entry,
                true,  // onGround
                false, // NOT onClimbable yet
                false,
                entry,
                RouteSegment.ClimbLeg.MOUNT,
                traversal,
                0,
                81.0,
                0,
                3, // 3 tick rimanenti
                false,
                false
        );

        ClimbKinematics.ClimbDecision decision1 = ClimbKinematics.evaluate(snapshotPulse3);
        assertEquals(AutoWalkMotor.ClimbSubPhase.MOUNT, decision1.nextSubPhase());
        assertTrue(decision1.keyUp());
        assertTrue(decision1.keyJump(), "A terra deve emettere keyJump per agganciare la ladder");
        assertEquals(2, decision1.nextJumpPulseTicksRemaining());

        // Se onClimbable diventa true, passa immediatamente a TRANSIT e azzera il pulse
        ClimbKinematics.ClimbSnapshot snapshotAttached = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.MOUNT,
                true,
                new Vec3(-59.5, 81.1, -41.2),
                entry,
                false,
                true, // onClimbable = true!
                true,
                entry,
                RouteSegment.ClimbLeg.MOUNT,
                traversal,
                1,
                81.0,
                0,
                2,
                false,
                false
        );

        ClimbKinematics.ClimbDecision decisionAttached = ClimbKinematics.evaluate(snapshotAttached);
        assertEquals(AutoWalkMotor.ClimbSubPhase.TRANSIT, decisionAttached.nextSubPhase(),
                "All'aggancio fisico deve transitare a TRANSIT!");
        assertFalse(decisionAttached.keyJump(), "Il salto deve essere rilasciato all'aggancio");
        assertTrue(decisionAttached.keyUp());
        assertEquals(0, decisionAttached.nextJumpPulseTicksRemaining());
    }

    @Test
    @DisplayName("Doppio cancello fine colonna: DISMOUNT solo con ultimo rung E quota compatibile")
    void testDualGateDismount() {
        ClimbTraversal traversal = createTestLadderTraversal(81, 85);
        BlockPos topRung = new BlockPos(-59, 85, -41);

        // Caso A: quota raggiunta (85.0) ma non e' marcato ultimo rung -> resta TRANSIT
        ClimbKinematics.ClimbSnapshot notLastRung = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                true,
                new Vec3(-59.5, 85.0, -41.5),
                topRung,
                false,
                true,
                true,
                topRung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                0,
                84.5,
                0,
                0,
                false, // NOT last rung
                false
        );
        ClimbKinematics.ClimbDecision decA = ClimbKinematics.evaluate(notLastRung);
        assertEquals(AutoWalkMotor.ClimbSubPhase.TRANSIT, decA.nextSubPhase());

        // Caso B: e' l'ultimo rung ma la quota e' ancora a 83.0 -> resta TRANSIT
        ClimbKinematics.ClimbSnapshot heightNotReached = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                true,
                new Vec3(-59.5, 83.0, -41.5),
                topRung,
                false,
                true,
                true,
                topRung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                0,
                82.5,
                0,
                0,
                true, // IS last rung
                false
        );
        ClimbKinematics.ClimbDecision decB = ClimbKinematics.evaluate(heightNotReached);
        assertEquals(AutoWalkMotor.ClimbSubPhase.TRANSIT, decB.nextSubPhase());

        // Caso C: e' l'ultimo rung E quota superata (85.0 >= 85 - 0.15) -> DISMOUNT!
        ClimbKinematics.ClimbSnapshot bothGatesOpen = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                true,
                new Vec3(-59.5, 85.0, -41.5),
                topRung,
                false,
                true,
                true,
                topRung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                0,
                84.5,
                0,
                0,
                true, // IS last rung
                false
        );
        ClimbKinematics.ClimbDecision decC = ClimbKinematics.evaluate(bothGatesOpen);
        assertEquals(AutoWalkMotor.ClimbSubPhase.DISMOUNT, decC.nextSubPhase());
        assertTrue(decC.shouldAdvanceWaypoint());
    }

    @Test
    @DisplayName("Dismount: completamento solo su landing orizzontale stabile e onGround")
    void testDismountCompletionRequiresGroundAndProximity() {
        ClimbTraversal traversal = createTestLadderTraversal(81, 85);
        BlockPos landing = traversal.landingPos(); // -59, 86, -40

        // In volo sopra il landing: non completato
        ClimbKinematics.ClimbSnapshot inAir = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.DISMOUNT,
                true,
                new Vec3(-59.5, 86.5, -40.5),
                landing,
                false, // NOT onGround
                false,
                false,
                landing,
                RouteSegment.ClimbLeg.DISMOUNT,
                traversal,
                0,
                86.0,
                0,
                0,
                false,
                false
        );
        ClimbKinematics.ClimbDecision decAir = ClimbKinematics.evaluate(inAir);
        assertEquals(ClimbKinematics.Outcome.CONTINUE, decAir.outcome());
        assertFalse(decAir.keyUp(), "Senza trasferimento certificato il dismount non deve spingere");

        // A terra e centrato sul landing: COMPLETED
        Vec3 center = Vec3.atBottomCenterOf(landing);
        ClimbKinematics.ClimbSnapshot onGroundLanding = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.DISMOUNT,
                true,
                center,
                landing,
                true, // onGround = true
                false,
                false,
                landing,
                RouteSegment.ClimbLeg.DISMOUNT,
                traversal,
                0,
                86.0,
                0,
                0,
                false,
                true,
                null,
                0.0f,
                ClimbContactProbe.Result.outside(),
                new ClimbLandingProbe.Result(true, true, true, true),
                0.0,
                0,
                7L,
                7L
        );
        ClimbKinematics.ClimbDecision decLanding = ClimbKinematics.evaluate(onGroundLanding);
        assertEquals(ClimbKinematics.Outcome.CONTINUE, decLanding.outcome(),
                "Il primo tick stabile deve soltanto armare la stabilizzazione");
        assertFalse(decLanding.keyUp());
        assertEquals(1, decLanding.nextLandingStableTicks());

        ClimbKinematics.ClimbSnapshot secondStableTick = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.DISMOUNT, true, center, landing,
                true, false, false, landing, RouteSegment.ClimbLeg.DISMOUNT,
                traversal, 0, 86.0, 0, 0, false, true,
                null, 0.0f, ClimbContactProbe.Result.outside(),
                new ClimbLandingProbe.Result(true, true, true, true),
                0.0, 1, 7L, 7L
        );
        ClimbKinematics.ClimbDecision completed = ClimbKinematics.evaluate(secondStableTick);
        assertEquals(ClimbKinematics.Outcome.COMPLETED, completed.outcome());
        assertFalse(completed.keyUp());
        assertEquals(ClimbKinematics.LeaseAction.RELEASE, completed.leaseAction());
    }

    @Test
    @DisplayName("Discesa su scaffolding: lease mantenuta attiva durante Transit e Dismount")
    void testScaffoldingDescentLeaseMaintained() {
        ClimbTraversal traversal = createTestScaffoldingDescent(64, 70);
        BlockPos currentRung = new BlockPos(10, 68, 10);

        ClimbKinematics.ClimbSnapshot descentSnapshot = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                false,
                new Vec3(10.5, 68.0, 10.5),
                currentRung,
                false,
                true,
                false,
                currentRung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                0,
                69.0,
                0,
                0,
                false,
                true
        );

        ClimbKinematics.ClimbDecision decision = ClimbKinematics.evaluate(descentSnapshot);
        assertEquals(ClimbKinematics.LeaseAction.ACQUIRE_RENEW, decision.leaseAction(),
                "La lease deve essere rinnovata durante la discesa in scaffolding");
    }

    @Test
    @DisplayName("Watchdog: 15 tick senza progresso fa REMOUNT_RETRY al primo giro, STUCK_ABORT al secondo")
    void testWatchdogStuckDetection() {
        ClimbTraversal traversal = createTestLadderTraversal(81, 85);
        BlockPos rung = new BlockPos(-59, 82, -41);

        // 10 tick senza progresso -> continua
        ClimbKinematics.ClimbSnapshot tick10 = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                true,
                new Vec3(-59.5, 82.0, -41.5),
                rung,
                false,
                true,
                true,
                rung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                10,
                82.0,
                0,
                0,
                false,
                false
        );
        ClimbKinematics.ClimbDecision dec10 = ClimbKinematics.evaluate(tick10);
        assertEquals(ClimbKinematics.Outcome.CONTINUE, dec10.outcome());

        // 14 tick che avanzano a 15 senza progresso, tentativo 0 -> REMOUNT_RETRY
        ClimbKinematics.ClimbSnapshot tick14 = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                true,
                new Vec3(-59.5, 82.0, -41.5),
                rung,
                false,
                true,
                true,
                rung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                14,
                82.0,
                0,
                0,
                false,
                false
        );
        ClimbKinematics.ClimbDecision dec14 = ClimbKinematics.evaluate(tick14);
        assertEquals(ClimbKinematics.Outcome.REMOUNT_RETRY, dec14.outcome());
        assertEquals(AutoWalkMotor.ClimbSubPhase.MOUNT, dec14.nextSubPhase());
        assertEquals(1, dec14.nextRecoveryAttempts());

        // 14 tick che avanzano a 15 senza progresso, tentativo 1 -> STUCK_ABORT
        ClimbKinematics.ClimbSnapshot tick14Second = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                true,
                new Vec3(-59.5, 82.0, -41.5),
                rung,
                false,
                true,
                true,
                rung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                14,
                82.0,
                1,
                0,
                false,
                false
        );
        ClimbKinematics.ClimbDecision decStuck = ClimbKinematics.evaluate(tick14Second);
        assertEquals(ClimbKinematics.Outcome.STUCK_ABORT, decStuck.outcome());
    }

    @Test
    @DisplayName("D14: Discesa su ladder emette GRAVITY_DESCENT (zero keyUp/keyJump) e rinnova lease")
    void testLadderDescentEmitsGravityDescentAndRenewsLease() {
        ClimbTraversal traversal = ClimbTraversal.of(
                Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                new BlockPos(-59, 81, -41),
                new BlockPos(-59, 85, -41),
                new BlockPos(-59, 85, -41),
                new BlockPos(-59, 81, -40),
                Direction.NORTH,
                null
        );
        BlockPos currentRung = new BlockPos(-59, 84, -41);

        ClimbKinematics.ClimbSnapshot descentSnapshot = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                false,
                new Vec3(-59.5, 84.5, -41.2),
                currentRung,
                false,
                true,
                false,
                currentRung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                0,
                85.0,
                0,
                0,
                false,
                true
        );

        ClimbKinematics.ClimbDecision decision = ClimbKinematics.evaluate(descentSnapshot);
        assertEquals(ClimbKinematics.LeaseAction.ACQUIRE_RENEW, decision.leaseAction(),
                "La lease deve essere rinnovata durante la discesa su ladder");
        assertFalse(decision.keyUp(), "Durante la discesa gravitazionale su ladder keyUp deve essere false");
        assertFalse(decision.keyJump(), "Durante la discesa gravitazionale su ladder keyJump deve essere false");
        assertNotNull(decision.desiredYaw(), "Lo yaw deve puntare verso il supporto");
    }

    @Test
    @DisplayName("D14: Watchdog direzionale con segno: delta Y negativo azzera il watchdog in discesa, delta positivo no")
    void testSignedDirectionalWatchdogProgressInDescent() {
        ClimbTraversal traversal = ClimbTraversal.of(
                Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                new BlockPos(-59, 81, -41),
                new BlockPos(-59, 85, -41),
                new BlockPos(-59, 85, -41),
                new BlockPos(-59, 81, -40),
                Direction.NORTH,
                null
        );
        BlockPos currentRung = new BlockPos(-59, 84, -41);

        // Caso A: il giocatore scende da 84.5 a 84.4 (delta = -0.1, discesa reale >= 0.05) -> watchdog azzerato!
        ClimbKinematics.ClimbSnapshot downwardProgress = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                false,
                new Vec3(-59.5, 84.4, -41.2),
                currentRung,
                false,
                true,
                false,
                currentRung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                5,
                84.5,
                0,
                0,
                false,
                true
        );
        ClimbKinematics.ClimbDecision decDown = ClimbKinematics.evaluate(downwardProgress);
        assertEquals(0, decDown.nextWatchdogTicks(), "Progresso Y negativo deve azzerare il watchdog in discesa");
        assertEquals(84.4, decDown.nextLastObservedY(), 0.001);

        // Caso B: il giocatore sale da 84.5 a 84.6 durante discesa (movimento anomalo/drift verso l'alto) -> watchdog NON azzerato
        ClimbKinematics.ClimbSnapshot upwardDrift = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                false,
                new Vec3(-59.5, 84.6, -41.2),
                currentRung,
                false,
                true,
                false,
                currentRung,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                5,
                84.5,
                0,
                0,
                false,
                true
        );
        ClimbKinematics.ClimbDecision decUp = ClimbKinematics.evaluate(upwardDrift);
        assertEquals(6, decUp.nextWatchdogTicks(), "Deriva Y positiva in discesa NON deve azzerare il watchdog");
    }

    @Test
    @DisplayName("D19: Mount superiore allinea all'apertura prima di W e non usa il solo centraggio")
    void topMountAlignsToApproachBeforeAdvancing() {
        BlockPos surface = new BlockPos(0, 85, 0);
        BlockPos aperture = surface.north();
        BlockPos entry = aperture.below();
        ClimbTraversal traversal = ClimbTraversal.of(
                Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                entry, entry, entry.below(3), entry.below(3).south(),
                Direction.SOUTH, null
        );
        ClimbEntryTransition transition = ClimbEntryTransition.ofTopDescent(
                surface, aperture, entry, Direction.NORTH, traversal,
                ClimbEntryTransition.PassageRequirement.CLEAR, null
        );

        ClimbKinematics.ClimbSnapshot misaligned = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.MOUNT, false,
                Vec3.atBottomCenterOf(surface), surface, true, false, false,
                entry, RouteSegment.ClimbLeg.MOUNT, traversal,
                0, 85.0, 0, 0, false, false,
                transition, Direction.SOUTH.toYRot()
        );
        ClimbKinematics.ClimbDecision align = ClimbKinematics.evaluate(misaligned);
        assertFalse(align.keyUp(), "W deve restare rilasciato durante l'allineamento");
        assertEquals(Direction.NORTH.toYRot(), align.desiredYaw());

        ClimbKinematics.ClimbSnapshot centeredButNotAttached = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.MOUNT, false,
                new Vec3(entry.getX() + 0.5, 85.0, entry.getZ() + 0.5), entry.above(), true, false, false,
                entry, RouteSegment.ClimbLeg.MOUNT, traversal,
                1, 85.0, 0, 0, false, true,
                transition, Direction.NORTH.toYRot()
        );
        ClimbKinematics.ClimbDecision approach = ClimbKinematics.evaluate(centeredButNotAttached);
        assertEquals(AutoWalkMotor.ClimbSubPhase.APPROACH, approach.nextSubPhase(),
                "Il solo centro X/Z non dimostra l'aggancio, ma abilita l'approccio controllato");
        assertTrue(approach.keyUp(), "Dopo l'allineamento W accompagna l'ingresso finché l'aggancio è reale");

        ClimbKinematics.ClimbSnapshot captureWait = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.APPROACH, false,
                new Vec3(aperture.getX() + 0.5, 85.0, aperture.getZ() + 0.55), aperture,
                true, false, false, entry, RouteSegment.ClimbLeg.MOUNT, traversal,
                2, 85.0, 0, 0, false, true, transition, Direction.NORTH.toYRot(),
                new ClimbContactProbe.Result(ClimbContactProbe.ContactState.APPROACHING,
                        0.0, false, false, true, false),
                new ClimbLandingProbe.Result(false, true, false, false),
                0.0, 0, 11L, 11L
        );
        ClimbKinematics.ClimbDecision wait = ClimbKinematics.evaluate(captureWait);
        assertEquals(AutoWalkMotor.ClimbSubPhase.CAPTURE_WAIT, wait.nextSubPhase());
        assertFalse(wait.keyUp(), "CAPTURE_WAIT deve rilasciare W prima del progresso verticale");
    }

    @Test
    @DisplayName("D23 & D24: Riproduzione Belvedere - contatto lamina con onClimbable=false transita subito a TRANSIT e rilascia W")
    void testBelvedereDescentReleasesWOnGeometricContact() {
        // Scala a pioli a X=-60, Z=-42, quota Y=82..84, facing=NORTH (parete di supporto a SUD: wallFacing=SOUTH)
        BlockPos topEntry = new BlockPos(-60, 84, -42);
        BlockPos bottom = new BlockPos(-60, 82, -42);
        BlockPos landing = new BlockPos(-60, 81, -42);
        ClimbTraversal traversal = ClimbTraversal.of(
                Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                topEntry,
                topEntry,
                bottom,
                landing,
                Direction.SOUTH,
                null
        );

        // Simulazione: centro del giocatore a Z=-40.95 (nel blocco Z=-41, quindi onClimbable() vanilla è FALSE!),
        // ma la sua hitbox tocca la lamina della ladder (che si trova a Z=-41.1875..-41.0). Quota Y=84.8
        ClimbEntryTransition transition = ClimbEntryTransition.ofTopDescent(
                new BlockPos(-60, 85, -41), new BlockPos(-60, 85, -42),
                topEntry, Direction.NORTH, traversal,
                ClimbEntryTransition.PassageRequirement.CLEAR, null);
        ClimbKinematics.ClimbSnapshot snapshot = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.CAPTURE_WAIT,
                false,
                new Vec3(-59.5, 84.8, -40.95),
                new BlockPos(-60, 84, -41), // blockPosition non è il blocco scala!
                false,
                false, // onClimbable è false!
                false,
                topEntry,
                RouteSegment.ClimbLeg.MOUNT,
                traversal,
                1,
                85.0,
                0,
                0,
                false,
                true,
                transition,
                Direction.NORTH.toYRot(),
                new ClimbContactProbe.Result(ClimbContactProbe.ContactState.CONTACT,
                        -0.2, true, true, true, false),
                new ClimbLandingProbe.Result(false, true, false, false),
                -0.08,
                0,
                12L,
                12L
        );

        ClimbKinematics.ClimbDecision decision = ClimbKinematics.evaluate(snapshot);
        assertEquals(AutoWalkMotor.ClimbSubPhase.TRANSIT, decision.nextSubPhase(),
                "Contratto D23/D24: l'intersezione con la lamina deve scatenare il commit a TRANSIT anche con onClimbable=false");
        assertFalse(decision.keyUp(),
                "Contratto D24: W DEVE essere rilasciato istantaneamente al passaggio a TRANSIT!");
        assertEquals(Direction.SOUTH.toYRot(), decision.desiredYaw(),
                "Yaw deve allinearsi verso la parete di supporto");
        assertEquals(ClimbKinematics.LeaseAction.ACQUIRE_RENEW, decision.leaseAction());
    }

    @Test
    @DisplayName("D25: Watchdog in TRANSIT discesa non esegue mai REMOUNT_RETRY con spinta verso il vuoto")
    void testDescentTransitWatchdogNeverRemountRetriesWithKeyUp() {
        BlockPos topEntry = new BlockPos(-60, 84, -42);
        BlockPos bottom = new BlockPos(-60, 82, -42);
        BlockPos landing = new BlockPos(-60, 81, -42);
        ClimbTraversal traversal = ClimbTraversal.of(
                Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                topEntry, topEntry, bottom, landing, Direction.SOUTH, null
        );

        // Snapshot in TRANSIT con 15 tick di watchdog accumulati
        ClimbKinematics.ClimbSnapshot stalledTransit = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                false, // discesa
                new Vec3(-59.5, 83.5, -41.0),
                new BlockPos(-60, 83, -42),
                false,
                true,
                false,
                new BlockPos(-60, 83, -42),
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                15, // watchdog scaduto
                83.5,
                0,
                0,
                false,
                true
        );

        ClimbKinematics.ClimbDecision decision = ClimbKinematics.evaluate(stalledTransit);
        assertEquals(ClimbKinematics.Outcome.STUCK_ABORT, decision.outcome(),
                "In discesa lo stallo deve produrre un aborto sicuro");
        assertFalse(decision.keyUp(), "Non deve mai emettere W in avanti durante il recupero da stallo in discesa");
        assertEquals(ClimbKinematics.LeaseAction.RELEASE, decision.leaseAction());
    }

    @Test
    @DisplayName("D26: Passaggio a DISMOUNT alla base scala calcola lo yaw direttamente verso landingPos")
    void testDescentDismountVectorsDirectlyToLanding() {
        BlockPos topEntry = new BlockPos(-60, 84, -42);
        BlockPos bottom = new BlockPos(-60, 82, -42);
        BlockPos landing = new BlockPos(-60, 81, -42);
        ClimbTraversal traversal = ClimbTraversal.of(
                Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                topEntry, topEntry, bottom, landing, Direction.SOUTH, null
        );

        // Giocatore a fondo scala (Y=82.0) all'ultimo piolo
        ClimbKinematics.ClimbSnapshot bottomRung = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                false,
                new Vec3(-59.5, 82.0, -41.2),
                bottom,
                false,
                true,
                false,
                bottom,
                RouteSegment.ClimbLeg.TRANSIT,
                traversal,
                0,
                82.2,
                0,
                0,
                true, // ultimo piolo di transito!
                true,
                null,
                Direction.SOUTH.toYRot(),
                new ClimbContactProbe.Result(ClimbContactProbe.ContactState.CONTACT,
                        -3.0, true, true, false, true),
                new ClimbLandingProbe.Result(false, true, false, false),
                -0.1,
                0,
                15L,
                15L
        );

        ClimbKinematics.ClimbDecision decision = ClimbKinematics.evaluate(bottomRung);
        assertEquals(AutoWalkMotor.ClimbSubPhase.DISMOUNT, decision.nextSubPhase());
        assertNotNull(decision.desiredYaw());

        // Landing reale nella cella della colonna: (-59.5, 81.0, -41.5).
        double dx = -59.5 - (-59.5);
        double dz = -41.5 - (-41.2);
        float expectedYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        assertEquals(expectedYaw, decision.desiredYaw(), 1.0f);
    }

    @Test
    @DisplayName("D32: Salto a vuoto senza aderenza ladder non promuove a TRANSIT e non riarma recovery")
    void testMountJumpWithoutContactDoesNotPromoteToTransit() {
        ClimbTraversal traversal = createTestLadderTraversal(81, 85);
        BlockPos entry = traversal.entryPos();

        // Giocatore che salta a mezz'aria (Y=81.25 > 81.0 + 0.05), ma fuori dal corridoio e senza contatto
        ClimbKinematics.ClimbSnapshot jumpInAirNoContact = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.MOUNT,
                true,
                new Vec3(-59.5, 81.25, -41.2),
                entry,
                false, // in aria
                false, // NOT onClimbable
                false,
                entry,
                RouteSegment.ClimbLeg.MOUNT,
                traversal,
                1,
                81.0,
                1, // già 1 recovery attempt consumato
                2,
                false,
                false,
                null,
                Direction.NORTH.toYRot(),
                ClimbContactProbe.Result.outside(), // fuori corridoio, nessun contatto
                new ClimbLandingProbe.Result(false, true, false, false, false),
                0.3,
                0,
                20L,
                20L
        );

        ClimbKinematics.ClimbDecision decision = ClimbKinematics.evaluate(jumpInAirNoContact);
        assertEquals(AutoWalkMotor.ClimbSubPhase.MOUNT, decision.nextSubPhase(),
                "Senza aderenza fisica, il semplice aumento di quota dal salto non deve promuovere a TRANSIT!");
        assertEquals(1, decision.nextRecoveryAttempts(),
                "Il conteggio dei recuperi non deve riarmarsi per un semplice salto");
    }

    @Test
    @DisplayName("D33: Sbarco in salita - Sollevamento residuo mantiene keyUp verso il supporto finché playerY < landingY - 0.20")
    void testAscentDismountMaintainsKeyUpDuringResidualLift() {
        ClimbTraversal traversal = createTestLadderTraversal(81, 85);
        BlockPos topRung = new BlockPos(-59, 85, -41);
        BlockPos landing = traversal.landingPos(); // -59, 86, -40 (landingY = 86.0)

        // Giocatore a Y=85.1 (consumato ultimo rung ma playerY < 86.0 - 0.20 = 85.80)
        ClimbKinematics.ClimbSnapshot residualLiftSnapshot = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.DISMOUNT,
                true,
                new Vec3(-59.5, 85.1, -41.2),
                topRung,
                false,
                true,
                false,
                landing,
                RouteSegment.ClimbLeg.DISMOUNT,
                traversal,
                0,
                85.0,
                0,
                0,
                false,
                false,
                null,
                Direction.NORTH.toYRot(),
                new ClimbContactProbe.Result(ClimbContactProbe.ContactState.CONTACT, 0.0, true, true, false, false, true),
                new ClimbLandingProbe.Result(false, true, false, false, true), // landing praticabile alla sua quota
                0.15,
                0,
                25L,
                25L
        );

        ClimbKinematics.ClimbDecision decision = ClimbKinematics.evaluate(residualLiftSnapshot);
        assertEquals(AutoWalkMotor.ClimbSubPhase.DISMOUNT, decision.nextSubPhase());
        assertTrue(decision.keyUp(), "Nel sollevamento residuo keyUp deve rimanere attivo!");
        assertFalse(decision.keyJump());
        assertEquals(Direction.NORTH.toYRot(), decision.desiredYaw(),
                "Durante il sollevamento residuo lo yaw deve rimanere rivolto verso il supporto della ladder");
        assertEquals(ClimbKinematics.Outcome.CONTINUE, decision.outcome());
    }

    @Test
    @DisplayName("D33: Sbarco in salita - Trasferimento a Y >= landingY - 0.20 orienta verso landing e spinge W se praticabile")
    void testAscentDismountTransfersTowardsLandingWhenClear() {
        ClimbTraversal traversal = createTestLadderTraversal(81, 85);
        BlockPos landing = traversal.landingPos(); // -59, 86, -40 (landingY = 86.0)

        // Giocatore a Y=85.85 (>= 86.0 - 0.20), testa e corpo oltre il bordo
        ClimbKinematics.ClimbSnapshot transferSnapshot = new ClimbKinematics.ClimbSnapshot(
                AutoWalkMotor.ClimbSubPhase.DISMOUNT,
                true,
                new Vec3(-59.5, 85.85, -41.2),
                landing,
                false,
                false,
                false,
                landing,
                RouteSegment.ClimbLeg.DISMOUNT,
                traversal,
                0,
                85.75,
                0,
                0,
                false,
                false,
                null,
                Direction.NORTH.toYRot(),
                ClimbContactProbe.Result.outside(),
                new ClimbLandingProbe.Result(false, true, false, false, true), // destinationPracticable = true
                0.05,
                0,
                25L,
                25L
        );

        ClimbKinematics.ClimbDecision decision = ClimbKinematics.evaluate(transferSnapshot);
        assertEquals(AutoWalkMotor.ClimbSubPhase.DISMOUNT, decision.nextSubPhase());
        assertTrue(decision.keyUp(), "Nel trasferimento con landing praticabile keyUp deve accompagnare il passo sul tetto!");
        assertNotNull(decision.desiredYaw());
        Vec3 landingCenter = Vec3.atBottomCenterOf(landing);
        float expectedYaw = (float) Math.toDegrees(Math.atan2(-(landingCenter.x - (-59.5)), landingCenter.z - (-41.2)));
        assertEquals(expectedYaw, decision.desiredYaw(), 1.0f);
    }
}

