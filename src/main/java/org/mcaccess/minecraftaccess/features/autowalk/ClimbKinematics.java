package org.mcaccess.minecraftaccess.features.autowalk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbContactProbe;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbEntryTransition;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbLandingProbe;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbTraversal;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbableGeometry;

/**
 * Modulo cinematico deterministico e testabile headless per la scalata verticale (Contratti D4..D8).
 * Isola le decisioni cinematiche di Mount, Transit per-rung e Dismount a doppio cancello.
 */
public final class ClimbKinematics {

    public static final int MOUNT_JUMP_PULSE_TICKS = 3;
    public static final double ASCENT_RUNG_TOLERANCE = 0.15;
    public static final double DESCENT_RUNG_TOLERANCE = 0.25;
    public static final double WATCHDOG_MIN_PROGRESS = 0.05;
    public static final int WATCHDOG_WINDOW_TICKS = 15;
    public static final double LANDING_HORIZONTAL_TOLERANCE = 0.35;
    public static final float MOUNT_YAW_TOLERANCE_DEGREES = 8.0f;

    public enum LeaseAction {
        NONE,
        ACQUIRE_RENEW,
        RELEASE
    }

    public enum Outcome {
        CONTINUE,
        COMPLETED,
        REMOUNT_RETRY,
        STUCK_ABORT,
        TOPOLOGICAL_ABORT
    }

    /**
     * Snapshot immutabile dello stato corrente per il seam cinematico (Contratto D8).
     */
    public record ClimbSnapshot(
            @NotNull AutoWalkMotor.ClimbSubPhase subPhase,
            boolean isAscent,
            @NotNull Vec3 playerPos,
            @NotNull BlockPos playerBlockPos,
            boolean playerOnGround,
            boolean playerOnClimbable,
            boolean horizontalCollision,
            @NotNull BlockPos currentRungPos,
            @Nullable RouteSegment.ClimbLeg currentLeg,
            @Nullable ClimbTraversal traversal,
            int watchdogTicks,
            double lastObservedY,
            int recoveryAttempts,
            int jumpPulseTicksRemaining,
            boolean isLastTransitRung,
            boolean hasActiveDescentLease,
            @Nullable ClimbEntryTransition climbEntryTransition,
            float playerYaw,
            @NotNull ClimbContactProbe.Result contactResult,
            @NotNull ClimbLandingProbe.Result landingResult,
            double verticalVelocity,
            int landingStableTicks,
            long routeRevisionId,
            long activeClimbRouteRevisionId
    ) {
        public ClimbSnapshot(
                @NotNull AutoWalkMotor.ClimbSubPhase subPhase,
                boolean isAscent,
                @NotNull Vec3 playerPos,
                @NotNull BlockPos playerBlockPos,
                boolean playerOnGround,
                boolean playerOnClimbable,
                boolean horizontalCollision,
                @NotNull BlockPos currentRungPos,
                @Nullable RouteSegment.ClimbLeg currentLeg,
                @Nullable ClimbTraversal traversal,
                int watchdogTicks,
                double lastObservedY,
                int recoveryAttempts,
                int jumpPulseTicksRemaining,
                boolean isLastTransitRung,
                boolean hasActiveDescentLease,
                @Nullable ClimbEntryTransition climbEntryTransition,
                float playerYaw
        ) {
            this(subPhase, isAscent, playerPos, playerBlockPos, playerOnGround,
                    playerOnClimbable, horizontalCollision, currentRungPos,
                    currentLeg, traversal, watchdogTicks, lastObservedY,
                    recoveryAttempts, jumpPulseTicksRemaining, isLastTransitRung,
                    hasActiveDescentLease, climbEntryTransition, playerYaw,
                    ClimbContactProbe.Result.outside(),
                    new ClimbLandingProbe.Result(false, true, false, false),
                    0.0, 0, 0L, 0L);
        }

        public ClimbSnapshot(
                @NotNull AutoWalkMotor.ClimbSubPhase subPhase,
                boolean isAscent,
                @NotNull Vec3 playerPos,
                @NotNull BlockPos playerBlockPos,
                boolean playerOnGround,
                boolean playerOnClimbable,
                boolean horizontalCollision,
                @NotNull BlockPos currentRungPos,
                @Nullable RouteSegment.ClimbLeg currentLeg,
                @Nullable ClimbTraversal traversal,
                int watchdogTicks,
                double lastObservedY,
                int recoveryAttempts,
                int jumpPulseTicksRemaining,
                boolean isLastTransitRung,
                boolean hasActiveDescentLease
        ) {
            this(subPhase, isAscent, playerPos, playerBlockPos, playerOnGround,
                    playerOnClimbable, horizontalCollision, currentRungPos,
                    currentLeg, traversal, watchdogTicks, lastObservedY,
                    recoveryAttempts, jumpPulseTicksRemaining, isLastTransitRung,
                    hasActiveDescentLease, null, 0.0f);
        }
    }

    /**
     * Decisione deterministica immutabile prodotta dal seam cinematico.
     */
    public record ClimbDecision(
            @NotNull AutoWalkMotor.ClimbSubPhase nextSubPhase,
            boolean keyUp,
            boolean keyJump,
            @Nullable Float desiredYaw,
            boolean shouldAdvanceWaypoint,
            int nextJumpPulseTicksRemaining,
            int nextWatchdogTicks,
            double nextLastObservedY,
            int nextRecoveryAttempts,
            @NotNull LeaseAction leaseAction,
            @NotNull Outcome outcome,
            int nextLandingStableTicks,
            @NotNull ClimbContactProbe.ReasonCode reasonCode
    ) {
        public ClimbDecision(
                @NotNull AutoWalkMotor.ClimbSubPhase nextSubPhase,
                boolean keyUp,
                boolean keyJump,
                @Nullable Float desiredYaw,
                boolean shouldAdvanceWaypoint,
                int nextJumpPulseTicksRemaining,
                int nextWatchdogTicks,
                double nextLastObservedY,
                int nextRecoveryAttempts,
                @NotNull LeaseAction leaseAction,
                @NotNull Outcome outcome
        ) {
            this(nextSubPhase, keyUp, keyJump, desiredYaw, shouldAdvanceWaypoint,
                    nextJumpPulseTicksRemaining, nextWatchdogTicks, nextLastObservedY,
                    nextRecoveryAttempts, leaseAction, outcome, 0,
                    ClimbContactProbe.ReasonCode.NONE);
        }
    }

    private ClimbKinematics() {
    }

    /**
     * Valuta atomicamente lo snapshot cinematico e calcola la decisione senza effetti collaterali (Contratto D8).
     */
    public static @NotNull ClimbDecision evaluate(@NotNull ClimbSnapshot snapshot) {
        if (snapshot.traversal() == null) {
            return new ClimbDecision(
                    snapshot.subPhase(),
                    false,
                    false,
                    null,
                    false,
                    0,
                    snapshot.watchdogTicks(),
                    snapshot.lastObservedY(),
                    snapshot.recoveryAttempts(),
                    LeaseAction.RELEASE,
                    Outcome.TOPOLOGICAL_ABORT
            );
        }

        return switch (snapshot.subPhase()) {
            case MOUNT, ALIGN, APPROACH, CAPTURE_WAIT -> evaluateMount(snapshot);
            case TRANSIT -> evaluateTransit(snapshot);
            case DISMOUNT -> evaluateDismount(snapshot);
        };
    }

    private static ClimbDecision evaluateMount(ClimbSnapshot snapshot) {
        ClimbTraversal traversal = snapshot.traversal();
        boolean isAscent = snapshot.isAscent();
        ClimbableGeometry.ClimbType climbType = traversal.climbType();

        Float desiredYaw = null;
        if (!isAscent && snapshot.climbEntryTransition() != null) {
            desiredYaw = snapshot.climbEntryTransition().approachDirection().toYRot();
        } else if (climbType == ClimbableGeometry.ClimbType.WALL_MOUNTED && traversal.wallFacing() != null) {
            desiredYaw = traversal.wallFacing().toYRot();
        }

        if (!isAscent) {
            return evaluateDescentMount(snapshot, desiredYaw);
        }

        // Verifica Watchdog in fase MOUNT (15 tick senza aggancio)
        int nextWatchdog = snapshot.watchdogTicks() + 1;
        if (nextWatchdog > WATCHDOG_WINDOW_TICKS) {
            if (snapshot.recoveryAttempts() == 0) {
                return new ClimbDecision(
                        AutoWalkMotor.ClimbSubPhase.MOUNT,
                        true,
                        false,
                        desiredYaw,
                        false,
                        MOUNT_JUMP_PULSE_TICKS,
                        0,
                        snapshot.playerPos().y(),
                        1,
                        LeaseAction.NONE,
                        Outcome.REMOUNT_RETRY
                );
            } else {
                return new ClimbDecision(
                        snapshot.subPhase(),
                        false,
                        false,
                        null,
                        false,
                        0,
                        0,
                        snapshot.playerPos().y(),
                        snapshot.recoveryAttempts(),
                        LeaseAction.RELEASE,
                        Outcome.STUCK_ABORT
                );
            }
        }

        if (isAscent) {
            // In salita su scala a parete (Contratto D32):
            // L'aumento di quota da solo non basta: serve contatto fisico effettivo con la scala.
            // onClimbable true, oppure contatto geometrico certificato nel corridoio della colonna.
            boolean inCorridor = snapshot.contactResult().inColumnCorridor();
            boolean contactActive = snapshot.contactResult().currentIntersection()
                    || snapshot.contactResult().state() == ClimbContactProbe.ContactState.CONTACT;
            boolean physicallyAttached = snapshot.playerOnClimbable()
                    || (inCorridor && contactActive && snapshot.playerPos().y() > snapshot.lastObservedY() + WATCHDOG_MIN_PROGRESS);

            if (physicallyAttached) {
                // Passaggio a TRANSIT: disattiva jump pulse, attiva keyUp
                return new ClimbDecision(
                        AutoWalkMotor.ClimbSubPhase.TRANSIT,
                        true,
                        false,
                        desiredYaw,
                        false,
                        0,
                        0,
                        snapshot.playerPos().y(),
                        snapshot.recoveryAttempts(), // D32: preserva il conteggio dei recuperi
                        LeaseAction.NONE,
                        Outcome.CONTINUE
                );
            }

            // Non ancora agganciato fisicamente:
            // Mantiene keyUp verso il supporto.
            // Se a terra e ci sono tick di impulso salto disponibili, emette keyJump (Contratto D4).
            boolean keyJump = false;
            int nextPulse = snapshot.jumpPulseTicksRemaining();
            if (climbType == ClimbableGeometry.ClimbType.WALL_MOUNTED && snapshot.playerOnGround() && nextPulse > 0) {
                keyJump = true;
                nextPulse--;
            }

            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.MOUNT,
                    true,
                    keyJump,
                    desiredYaw,
                    false,
                    nextPulse,
                    nextWatchdog,
                    snapshot.lastObservedY(),
                    snapshot.recoveryAttempts(),
                    LeaseAction.NONE,
                    Outcome.CONTINUE
            );
        }
        throw new IllegalStateException("Unreachable climb mount state");
    }

    private static ClimbDecision evaluateDescentMount(ClimbSnapshot snapshot, @Nullable Float desiredYaw) {
        ClimbContactProbe.Result contact = snapshot.contactResult();
        int nextWatchdog = snapshot.watchdogTicks() + 1;

        if (desiredYaw != null && !isYawAligned(snapshot.playerYaw(), desiredYaw)) {
            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.ALIGN, false, false, desiredYaw,
                    false, 0, nextWatchdog, snapshot.lastObservedY(),
                    snapshot.recoveryAttempts(), LeaseAction.ACQUIRE_RENEW,
                    Outcome.CONTINUE
            );
        }

        ClimbContactProbe.ReasonCode commitReason = commitReason(snapshot);
        if (commitReason != ClimbContactProbe.ReasonCode.NONE) {
            Float transitYaw = snapshot.traversal().wallFacing() != null
                    ? snapshot.traversal().wallFacing().toYRot()
                    : desiredYaw;
            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.TRANSIT, false, false, transitYaw,
                    false, 0, 0, snapshot.playerPos().y(), 0,
                    LeaseAction.ACQUIRE_RENEW, Outcome.CONTINUE, 0, commitReason
            );
        }

        if (snapshot.subPhase() == AutoWalkMotor.ClimbSubPhase.CAPTURE_WAIT
                || contact.apertureCapture()
                || contact.state() == ClimbContactProbe.ContactState.APPROACHING) {
            if (nextWatchdog >= WATCHDOG_WINDOW_TICKS) {
                return new ClimbDecision(
                        AutoWalkMotor.ClimbSubPhase.CAPTURE_WAIT, false, false, desiredYaw,
                        false, 0, 0, snapshot.playerPos().y(),
                        snapshot.recoveryAttempts(), LeaseAction.RELEASE,
                        Outcome.STUCK_ABORT, 0,
                        ClimbContactProbe.ReasonCode.OUTSIDE_COLUMN_ABORT
                );
            }
            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.CAPTURE_WAIT, false, false, desiredYaw,
                    false, 0, nextWatchdog, snapshot.lastObservedY(),
                    snapshot.recoveryAttempts(), LeaseAction.ACQUIRE_RENEW,
                    Outcome.CONTINUE
            );
        }

        if (nextWatchdog >= WATCHDOG_WINDOW_TICKS) {
            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.APPROACH, false, false, desiredYaw,
                    false, 0, 0, snapshot.playerPos().y(),
                    snapshot.recoveryAttempts(), LeaseAction.RELEASE,
                    Outcome.STUCK_ABORT, 0,
                    ClimbContactProbe.ReasonCode.OUTSIDE_COLUMN_ABORT
            );
        }

        return new ClimbDecision(
                AutoWalkMotor.ClimbSubPhase.APPROACH, true, false, desiredYaw,
                false, 0, nextWatchdog, snapshot.lastObservedY(),
                snapshot.recoveryAttempts(), LeaseAction.ACQUIRE_RENEW,
                Outcome.CONTINUE
        );
    }

    private static ClimbContactProbe.ReasonCode commitReason(ClimbSnapshot snapshot) {
        ClimbContactProbe.Result contact = snapshot.contactResult();
        if (contact.state() == ClimbContactProbe.ContactState.COMMITTED_INSIDE) {
            return ClimbContactProbe.ReasonCode.SWEPT_CROSSING;
        }
        if (contact.currentIntersection()) {
            return ClimbContactProbe.ReasonCode.AABB_CONTACT;
        }
        if (contact.sweptIntersection()) {
            return ClimbContactProbe.ReasonCode.SWEPT_CROSSING;
        }
        if (snapshot.playerOnClimbable()) {
            return ClimbContactProbe.ReasonCode.VANILLA_CLIMBABLE;
        }
        return ClimbContactProbe.ReasonCode.NONE;
    }

    static boolean isYawAligned(float currentYaw, float desiredYaw) {
        float delta = (desiredYaw - currentYaw) % 360.0f;
        if (delta > 180.0f) delta -= 360.0f;
        if (delta < -180.0f) delta += 360.0f;
        return Math.abs(delta) <= MOUNT_YAW_TOLERANCE_DEGREES;
    }

    private static ClimbDecision evaluateTransit(ClimbSnapshot snapshot) {
        ClimbTraversal traversal = snapshot.traversal();
        boolean isAscent = snapshot.isAscent();
        ClimbableGeometry.ClimbType climbType = traversal.climbType();

        // D26 & D34: il fondo, il landing della traversal o il contatto col suolo hanno priorita' sul watchdog.
        if (!isAscent && snapshot.isLastTransitRung()
                && (snapshot.playerOnGround()
                || snapshot.contactResult().bottomCrossing()
                || snapshot.contactResult().state() == ClimbContactProbe.ContactState.BELOW_COLUMN
                || snapshot.landingResult().supported())) {
            Float landingYaw = yawToward(snapshot.playerPos(), traversal.landingPos());
            ClimbContactProbe.ReasonCode reason = (snapshot.playerOnGround() || snapshot.landingResult().supported())
                    ? ClimbContactProbe.ReasonCode.SUPPORTED_LANDING
                    : ClimbContactProbe.ReasonCode.BOTTOM_CROSSING;
            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.DISMOUNT, false, false, landingYaw,
                    true, 0, 0, snapshot.playerPos().y(), 0,
                    LeaseAction.ACQUIRE_RENEW, Outcome.CONTINUE, 0, reason
            );
        }

        // 1. Controllo Watchdog di progresso verticale continuo con direzione firmata (Contratto D13)
        double dyProgress = isAscent
                ? (snapshot.playerPos().y() - snapshot.lastObservedY())
                : (snapshot.lastObservedY() - snapshot.playerPos().y());
        int nextWatchdog = snapshot.watchdogTicks();
        double nextLastObservedY = snapshot.lastObservedY();
        int nextRecovery = snapshot.recoveryAttempts();

        if (dyProgress >= WATCHDOG_MIN_PROGRESS) {
            nextLastObservedY = snapshot.playerPos().y();
            nextWatchdog = 0;
            // D32: il timer watchdog si azzera su progresso, ma il conteggio dei recovery
            // si riarma solo su avanzamento autentico al prossimo piolo (rungPassed).
        } else {
            nextWatchdog++;
            if (nextWatchdog >= WATCHDOG_WINDOW_TICKS) {
                if (nextRecovery == 0 && isAscent) {
                    // Un solo tentativo di riallineamento con nuovo Mount (solo per salita! Contratto D24/D25)
                    Float remountYaw = (traversal.wallFacing() != null) ? traversal.wallFacing().toYRot() : null;
                    return new ClimbDecision(
                            AutoWalkMotor.ClimbSubPhase.MOUNT,
                            true,
                            false,
                            remountYaw,
                            false,
                            MOUNT_JUMP_PULSE_TICKS,
                            0,
                            snapshot.playerPos().y(),
                            1,
                            snapshot.hasActiveDescentLease() ? LeaseAction.ACQUIRE_RENEW : LeaseAction.NONE,
                            Outcome.REMOUNT_RETRY
                    );
                } else {
                    // In discesa o dopo recovery esaurito: arresto protetto senza spinta orizzontale nel vuoto
                    return new ClimbDecision(
                            snapshot.subPhase(),
                            false,
                            false,
                            null,
                            false,
                            0,
                            0,
                            snapshot.playerPos().y(),
                            nextRecovery,
                            LeaseAction.RELEASE,
                            Outcome.STUCK_ABORT
                    );
                }
            }
        }

        // 2. Comandi di movimento per tipo cinematico
        Float desiredYaw = null;
        boolean keyUp = false;
        boolean keyJump = false;
        LeaseAction lease = LeaseAction.NONE;

        if (isAscent) {
            if (climbType == ClimbableGeometry.ClimbType.SCAFFOLDING) {
                keyJump = true;
                keyUp = false;
            } else {
                if (traversal.wallFacing() != null) {
                    desiredYaw = traversal.wallFacing().toYRot();
                }
                keyUp = true;
                keyJump = false;
            }

            // 3. Verifica superamento piolo corrente / termine colonna (Doppio Cancello - Contratto D6)
            double rungExitY = snapshot.currentRungPos().getY();
            boolean rungPassed = snapshot.playerPos().y() >= rungExitY - ASCENT_RUNG_TOLERANCE;

            if (rungPassed) {
                if (snapshot.isLastTransitRung()) {
                    // Doppio cancello: ultimo piolo consumato E quota continua compatibile con columnTopPos
                    double columnTopY = traversal.columnTopPos().getY();
                    if (snapshot.playerPos().y() >= columnTopY - ASCENT_RUNG_TOLERANCE) {
                        return new ClimbDecision(
                                AutoWalkMotor.ClimbSubPhase.DISMOUNT,
                                true,
                                false,
                                desiredYaw,
                                true, // Avanza al nodo landing
                                0,
                                0, // D37: Inizializza il watchdog all'ingresso di una nuova sotto-fase
                                snapshot.playerPos().y(),
                                0, // D32: Progresso autentico azzera i recuperi
                                LeaseAction.NONE,
                                Outcome.CONTINUE
                        );
                    }
                } else {
                    // Avanza al prossimo piolo intermedio della colonna
                    return new ClimbDecision(
                            AutoWalkMotor.ClimbSubPhase.TRANSIT,
                            keyUp,
                            keyJump,
                            desiredYaw,
                            true, // Avanza al prossimo rung
                            0,
                            nextWatchdog,
                            nextLastObservedY,
                            0, // D32: Avanzamento autentico al prossimo piolo azzera i recuperi
                            LeaseAction.NONE,
                            Outcome.CONTINUE
                    );
                }
            }
        } else {
            // Discesa (Contratti D10, D12: ladder/vines usano gravita' vanilla senza keyUp/keyDown)
            lease = LeaseAction.ACQUIRE_RENEW;
            if (climbType == ClimbableGeometry.ClimbType.SCAFFOLDING) {
                keyUp = false;
            } else {
                if (traversal.wallFacing() != null) {
                    desiredYaw = traversal.wallFacing().toYRot();
                }
                keyUp = false;
            }

            double rungExitY = snapshot.currentRungPos().getY();
            boolean rungPassed = snapshot.playerPos().y() <= rungExitY + DESCENT_RUNG_TOLERANCE;

            if (rungPassed) {
                if (!snapshot.isLastTransitRung()) {
                    return new ClimbDecision(
                            AutoWalkMotor.ClimbSubPhase.TRANSIT,
                            keyUp,
                            keyJump,
                            desiredYaw,
                            true, // Avanza al prossimo rung di discesa
                            0,
                            nextWatchdog,
                            nextLastObservedY,
                            0, // D32: Avanzamento autentico in discesa azzera i recuperi
                            lease,
                            Outcome.CONTINUE
                    );
                }
            }
        }

        // Prosegue la marcia lungo il piolo corrente
        return new ClimbDecision(
                AutoWalkMotor.ClimbSubPhase.TRANSIT,
                keyUp,
                keyJump,
                desiredYaw,
                false,
                0,
                nextWatchdog,
                nextLastObservedY,
                nextRecovery,
                lease,
                Outcome.CONTINUE
        );
    }

    private static ClimbDecision evaluateDismount(ClimbSnapshot snapshot) {
        ClimbTraversal traversal = snapshot.traversal();
        BlockPos landingPos = traversal.landingPos();
        float targetYaw = yawToward(snapshot.playerPos(), landingPos);

        // La lease resta acquisita fino al landing stabile per ogni discesa.
        LeaseAction lease = snapshot.hasActiveDescentLease() ? LeaseAction.ACQUIRE_RENEW : LeaseAction.NONE;

        boolean revisionMatches = snapshot.routeRevisionId() == snapshot.activeClimbRouteRevisionId();
        boolean stableNow = revisionMatches && snapshot.playerOnGround()
                && snapshot.landingResult().stable();
        int nextStableTicks = stableNow ? snapshot.landingStableTicks() + 1 : 0;

        if (nextStableTicks >= 2) {
            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.DISMOUNT,
                    false,
                    false,
                    targetYaw,
                    false,
                    0,
                    0,
                    snapshot.playerPos().y(),
                    0,
                    LeaseAction.RELEASE,
                    Outcome.COMPLETED,
                    nextStableTicks,
                    ClimbContactProbe.ReasonCode.SUPPORTED_LANDING
            );
        }

        // Se la AABB e' gia' sostenuta a terra, nessun passo orizzontale incondizionato (D34).
        if (snapshot.landingResult().supported()) {
            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.DISMOUNT, false, false, targetYaw,
                    false, 0, 0, snapshot.playerPos().y(), 0, lease,
                    Outcome.CONTINUE, nextStableTicks,
                    ClimbContactProbe.ReasonCode.SUPPORTED_LANDING
            );
        }

        // Biforcazione per direzione di scalata (Contratti D33 e D34)
        if (snapshot.isAscent()) {
            double landingY = landingPos.getY();
            double playerY = snapshot.playerPos().y();
            double dyProgress = playerY - snapshot.lastObservedY();

            // Progresso firmato verso il landing azzera il watchdog (D37)
            int nextWatchdog = snapshot.watchdogTicks();
            double nextLastY = snapshot.lastObservedY();
            if (dyProgress >= WATCHDOG_MIN_PROGRESS) {
                nextWatchdog = 0;
                nextLastY = playerY;
            } else {
                nextWatchdog++;
            }

            if (!revisionMatches || nextWatchdog >= WATCHDOG_WINDOW_TICKS) {
                return new ClimbDecision(
                        AutoWalkMotor.ClimbSubPhase.DISMOUNT, false, false, null,
                        false, 0, 0, playerY, 0,
                        LeaseAction.RELEASE, Outcome.STUCK_ABORT, 0,
                        ClimbContactProbe.ReasonCode.OUTSIDE_COLUMN_ABORT
                );
            }

            // Sottostato 1: Sollevamento residuo (playerY < landingY - 0.20)
            // Mantieni comandi di scalata verso il supporto ladder o jump su scaffolding
            if (playerY < landingY - 0.20) {
                Float supportYaw = traversal.wallFacing() != null ? traversal.wallFacing().toYRot() : targetYaw;
                boolean keyUp = traversal.climbType() != ClimbableGeometry.ClimbType.SCAFFOLDING;
                boolean keyJump = traversal.climbType() == ClimbableGeometry.ClimbType.SCAFFOLDING;
                return new ClimbDecision(
                        AutoWalkMotor.ClimbSubPhase.DISMOUNT,
                        keyUp,
                        keyJump,
                        supportYaw,
                        false,
                        0,
                        nextWatchdog,
                        nextLastY,
                        0,
                        LeaseAction.NONE,
                        Outcome.CONTINUE,
                        0,
                        ClimbContactProbe.ReasonCode.NONE
                );
            }

            // Sottostato 2: Trasferimento (playerY >= landingY - 0.20)
            // Il corpo ha superato il bordo del blocco di landing; avanza orizzontalmente sul pavimento
            boolean transferOk = snapshot.landingResult().destinationPracticable()
                    || snapshot.landingResult().transferSafe();
            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.DISMOUNT,
                    transferOk,
                    false,
                    targetYaw,
                    false,
                    0,
                    nextWatchdog,
                    nextLastY,
                    0,
                    LeaseAction.NONE,
                    Outcome.CONTINUE,
                    0,
                    ClimbContactProbe.ReasonCode.NONE
            );
        }

        // Ramo Discesa (Contratti D35, D36)
        int nextWatchdog = snapshot.watchdogTicks() + 1;
        if (!revisionMatches || nextWatchdog >= WATCHDOG_WINDOW_TICKS) {
            return new ClimbDecision(
                    AutoWalkMotor.ClimbSubPhase.DISMOUNT, false, false, null,
                    false, 0, 0, snapshot.playerPos().y(), 0,
                    LeaseAction.RELEASE, Outcome.STUCK_ABORT, 0,
                    ClimbContactProbe.ReasonCode.OUTSIDE_COLUMN_ABORT
            );
        }

        // Un trasferimento laterale in discesa e' permesso solo se il probe certifica l'impronta
        return new ClimbDecision(
                AutoWalkMotor.ClimbSubPhase.DISMOUNT,
                snapshot.landingResult().transferSafe(),
                false,
                targetYaw,
                false,
                0,
                nextWatchdog,
                snapshot.playerPos().y(),
                0,
                lease,
                Outcome.CONTINUE,
                0,
                ClimbContactProbe.ReasonCode.NONE
        );
    }

    private static float yawToward(Vec3 playerPos, BlockPos target) {
        Vec3 center = Vec3.atBottomCenterOf(target);
        return (float) Math.toDegrees(Math.atan2(-(center.x - playerPos.x()), center.z - playerPos.z()));
    }
}
