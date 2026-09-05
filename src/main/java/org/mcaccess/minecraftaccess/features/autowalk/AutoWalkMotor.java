package org.mcaccess.minecraftaccess.features.autowalk;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder.PathResult;
import org.mcaccess.minecraftaccess.features.safety.traversal.CrouchIntent;
import org.mcaccess.minecraftaccess.features.safety.traversal.CrouchIntentProbe;
import org.mcaccess.minecraftaccess.features.safety.traversal.RawCrouchIntentProvider;

/**
 * AutoWalk Motor (Level 2 - Motor Execution Body).
 * Detentore della FSM cinematica (State), esecutore dei comandi virtuali del client,
 * gestione della sterzata progressiva, frenata in curva, salti su gradino, nuoto,
 * rilevamento porte chiuse e riaperte, stuck watchdog e human takeover.
 */
@Slf4j
public class AutoWalkMotor {

    public enum State {
        IDLE,
        WALKING,
        JUMPING,
        SWIMMING,
        ARRIVED,
        CANCELLED
    }

    public enum StuckAction {
        NONE,
        REPATH,
        ABORT
    }

    /**
     * Interfaccia di callback con cause semantiche distinte verso il coordinatore.
     */
    public interface MotorCallback {
        void onArrival(Object target);
        void onStepNode();
        void onProgression(int remainingSteps);
        void onDoorClosed();
        void onDoorOpened(Object target);
        void onTakeover();
        void onNoPath(Object target);
        void onStuck();
        void onRepathRequested();
    }

    @Getter
    @Setter
    private State state = State.IDLE;

    @Getter
    private Vec3 lastTickPos = Vec3.ZERO;

    @Getter
    private int stuckTicks = 0;

    @Getter
    private boolean wasInAir = false;

    @Getter
    private double lastGroundY = 0.0;

    @Getter
    private int jumpHoldingTicks = 0;

    @Getter
    @Setter
    private int startupGraceTicks = 0;

    @Getter
    private int sprintCooldownTicks = 0;

    @Getter
    private @Nullable BlockPos waitingClosedDoorPos = null;

    @Getter
    private int lastAnnouncedStepIndex = -1;

    /**
     * Traccia la proprietà del tasto di salto (true se premuto dal motore per step-up).
     */
    @Getter
    @Setter
    private boolean motorHoldingJump = false;

    @Getter
    private int initialAlignmentTicks = 0;

    private final CrouchIntentProbe crouchIntentProbe;

    public AutoWalkMotor() {
        this(new RawCrouchIntentProvider());
    }

    public AutoWalkMotor(CrouchIntentProbe crouchIntentProbe) {
        this.crouchIntentProbe = crouchIntentProbe != null ? crouchIntentProbe : new RawCrouchIntentProvider();
    }

    public boolean isActive() {
        return state == State.WALKING || state == State.JUMPING || state == State.SWIMMING;
    }

    /**
     * Inizializza i parametri del motore all'avvio della marcia.
     */
    public void start(Vec3 playerPos, boolean onGround, double playerY) {
        this.state = State.WALKING;
        this.stuckTicks = 0;
        this.startupGraceTicks = 10;
        this.wasInAir = !onGround;
        this.lastGroundY = playerY;
        this.lastTickPos = playerPos;
        this.sprintCooldownTicks = 0;
        this.waitingClosedDoorPos = null;
        this.lastAnnouncedStepIndex = -1;
        this.jumpHoldingTicks = 0;
        this.motorHoldingJump = false;
        this.initialAlignmentTicks = 0;
    }

    /**
     * Arresta il motore e rilascia i comandi fisici del client.
     */
    public void stop(Minecraft client) {
        resetMovement(client);
        this.state = State.IDLE;
        this.startupGraceTicks = 0;
        this.sprintCooldownTicks = 0;
        this.waitingClosedDoorPos = null;
        this.stuckTicks = 0;
        this.jumpHoldingTicks = 0;
        this.motorHoldingJump = false;
        this.initialAlignmentTicks = 0;
    }

    /**
     * Rilascia fisicamente tutti i tasti virtuali di movimento e disattiva lo sprint.
     * Preserva la pressione manuale del tasto di salto (barra spaziatrice) rilasciando keyJump
     * solo se il salto era posseduto dal motore (motorHoldingJump).
     */
    public void resetMovement(@Nullable Minecraft client) {
        if (client != null && client.options != null) {
            resetMovement(client.options.keyUp, client.options.keyJump, client.player);
        } else {
            resetMovement(null, null, client != null ? client.player : null);
        }
    }

    public void resetMovement(@Nullable KeyMapping keyUp, @Nullable KeyMapping keyJump, @Nullable LocalPlayer player) {
        if (keyUp != null) {
            keyUp.setDown(false);
        }
        if (motorHoldingJump && keyJump != null) {
            keyJump.setDown(false);
        }
        if (player != null) {
            player.setSprinting(false);
        }
        this.jumpHoldingTicks = 0;
        this.motorHoldingJump = false;
    }

    /**
     * Esecuzione del tick cinematico del client.
     */
    public void tick(Minecraft client, RouteNavigator navigator, Config.AutoWalk config, boolean narrateHints, MotorCallback callback) {
        if (client == null || client.player == null || client.level == null) return;
        if (!isActive()) return;

        LocalPlayer player = client.player;
        Level level = client.level;

        // 1. Human Takeover Check
        if (evaluateTakeover(config.stopOnManualInput, isManualMovementKeyPressed(client))) {
            resetMovement(client);
            this.state = State.CANCELLED;
            callback.onTakeover();
            return;
        }

        // Decrementa i tick di grazia iniziali
        if (startupGraceTicks > 0) {
            startupGraceTicks--;
        }

        // 2. Verifica validità bersaglio
        Object target = navigator.getTargetObject();
        if (target == null || !navigator.isTargetValid(target)) {
            resetMovement(client);
            this.state = State.CANCELLED;
            callback.onTakeover();
            return;
        }

        // 3. Dynamic Entity Tracking
        if (navigator.shouldRepathForEntity()) {
            Object targetBefore = navigator.getTargetObject();
            PathResult result = navigator.repath(level, player.position(), config.maxRange);
            if (handleRepathResult(result, client, player, navigator, config, targetBefore, callback)) {
                return; // Esito terminale gestito (ARRIVED o CANCELLED)
            }
        }

        // 4. Post-Landing Repath Checkpoint
        boolean onGround = player.onGround();
        if (wasInAir && onGround) {
            double currentY = player.getY();
            if (Math.abs(currentY - lastGroundY) > 0.4 || state == State.JUMPING) {
                lastGroundY = currentY;
                Object targetBefore = navigator.getTargetObject();
                PathResult result = navigator.repath(level, player.position(), config.maxRange);
                if (handleRepathResult(result, client, player, navigator, config, targetBefore, callback)) {
                    return; // Esito terminale gestito (ARRIVED o CANCELLED)
                }
            }
        }
        wasInAir = !onGround;
        if (onGround) {
            lastGroundY = player.getY();
        }

        // 5. Verifica se rotta è completata
        if (navigator.isRouteCompleted()) {
            finishArrival(client, player, navigator, config, callback);
            return;
        }

        BlockPos targetNodePos = navigator.getCurrentNodePos();
        if (targetNodePos == null) {
            finishArrival(client, player, navigator, config, callback);
            return;
        }

        Vec3 nodeCenter = Vec3.atBottomCenterOf(targetNodePos);
        double dx = nodeCenter.x - player.getX();
        double dz = nodeCenter.z - player.getZ();
        double distH = Math.hypot(dx, dz);
        double deltaY = nodeCenter.y - player.getY();

        // 6. Direct Goal Proximity Check
        if (navigator.isAtFinalGoal(player.position(), level)) {
            finishArrival(client, player, navigator, config, callback);
            return;
        }

        // 6.5. Closed Door and Obstacle Interactive Check (C3, C4, C5 & Addendum 10.2, 10.3)
        BlockPos relevantDoor = null;
        boolean solidJambObstacle = false;

        if (navigator.isFirstSegmentPending()) {
            // Contratto C3 & C4: Valutazione volumetrica con bounding box reale
            AutoWalkPathfinder.ClearanceResult clearance = AutoWalkPathfinder.checkLocalClearance(
                    level, player.position(), nodeCenter, player.getBoundingBox()
            );

            switch (clearance.status()) {
                case BLOCKED_BY_CLOSED_DOOR -> {
                    relevantDoor = clearance.blockingDoorPos();
                }
                case BLOCKED_BY_SOLID_JAMB -> {
                    solidJambObstacle = true;
                }
                case CLEAR -> {
                    // Se la connessione al primo nodo è libera e siamo in prossimità del nodo 1:
                    // Verifichiamo se c'è una curva verso il nodo 2 (se presente)
                    List<BlockPos> path = navigator.getCurrentPath();
                    int currentIndex = navigator.getCurrentPathIndex();
                    if (path.size() > currentIndex + 1) {
                        BlockPos nextNextPos = path.get(currentIndex + 1);
                        Vec3 nextNextCenter = Vec3.atBottomCenterOf(nextNextPos);
                        AutoWalkPathfinder.ClearanceResult nextClearance = AutoWalkPathfinder.checkLocalClearance(
                                level, nodeCenter, nextNextCenter, player.getBoundingBox()
                        );
                        if (nextClearance.status() == AutoWalkPathfinder.ClearanceResult.ClearanceStatus.BLOCKED_BY_SOLID_JAMB) {
                            // Non completare ancora il disimpegno: avanza dritto verso il centro di nodeCenter
                            solidJambObstacle = true;
                        } else if (distH < 0.45) {
                            navigator.completeFirstSegment();
                        }
                    } else if (distH < 0.45) {
                        navigator.completeFirstSegment();
                    }
                }
            }
        } else {
            // Movimento ordinario (i >= 1): rilevazione porta nel nodo obiettivo
            BlockPos doorCheckPos = targetNodePos;
            if (!isDoorOrGateClosed(level, doorCheckPos) && isDoorOrGateClosed(level, doorCheckPos.above())) {
                doorCheckPos = doorCheckPos.above();
            }
            if (isDoorOrGateClosed(level, doorCheckPos)) {
                relevantDoor = AutoWalkPathfinder.getCanonicalDoorPos(level, doorCheckPos);
            }
        }

        double distToDoorSq = relevantDoor != null ? player.blockPosition().distSqr(relevantDoor) : Double.MAX_VALUE;
        boolean isDoorClosed = relevantDoor != null && isDoorOrGateClosed(level, relevantDoor);

        boolean isWaitingDoor = processDoorWait(
                level,
                relevantDoor,
                isDoorClosed,
                distToDoorSq,
                narrateHints,
                callback,
                () -> {
                    client.options.keyUp.setDown(false);
                    player.setSprinting(false);
                },
                pos -> {
                    Vec3 doorCenter = Vec3.atCenterOf(pos);
                    player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(doorCenter.x, player.getEyeY(), doorCenter.z));
                    player.setXRot(0.0f);
                },
                target
        );
        if (isWaitingDoor) {
            return;
        }

        // 7. Steering: rotazione progressiva Yaw con clamp a 20°/tick
        float yawDiff = calculateYawDiff(player.getYRot(), dx, dz);
        float newYaw = calculateSteering(player.getYRot(), dx, dz, 20.0f);
        player.setYRot(newYaw);

        // Contratto C6: Allineamento iniziale orizzontale limitato a max 12 tick
        if (navigator.isFirstSegmentPending() && Math.abs(yawDiff) > 30.0f) {
            if (initialAlignmentTicks < 12) {
                initialAlignmentTicks++;
                client.options.keyUp.setDown(false);
                return;
            }
        }

        // 8. Sprint Hysteresis Control & Forward Injection
        sprintCooldownTicks = updateSprintCooldown(yawDiff, sprintCooldownTicks);
        boolean sprintAllowed = config.sprint && !navigator.isFirstSegmentPending() && !solidJambObstacle;
        boolean canSprint = canSprint(sprintAllowed, sprintCooldownTicks, player.isShiftKeyDown(), player.getFoodData().getFoodLevel());

        // Contratto S3: Validazione volumetrica pre-virata per evitare spinte cieche contro pareti adiacenti
        boolean turnBlockedByWall = false;
        if (Math.abs(yawDiff) > 45.0f) {
            AutoWalkPathfinder.ClearanceResult turnClearance = AutoWalkPathfinder.checkLocalClearance(
                    level, player.position(), nodeCenter, player.getBoundingBox()
            );
            if (turnClearance.status() == AutoWalkPathfinder.ClearanceResult.ClearanceStatus.BLOCKED_BY_SOLID_JAMB) {
                turnBlockedByWall = true;
            }
        }

        if (shouldBrakeForTurn(yawDiff, distH) || (navigator.isFirstSegmentPending() && Math.abs(yawDiff) > 45.0f) || turnBlockedByWall) {
            client.options.keyUp.setDown(false);
        } else {
            client.options.keyUp.setDown(true);
        }

        player.setSprinting(canSprint);

        // 9. Water & Auto-Swim
        if (player.isInWater() || player.isInLiquid()) {
            state = State.SWIMMING;
            if (shouldHoldJumpForAutoSwim(true, config.autoSwim)) {
                client.options.keyJump.setDown(true);
                jumpHoldingTicks = 0;
                motorHoldingJump = true;
            } else if (shouldReleaseMotorJump(true, config.autoSwim, motorHoldingJump, jumpHoldingTicks)) {
                // Rilascia solo se il salto era stato avviato dal motore, rispettando lo Spazio manuale
                client.options.keyJump.setDown(false);
                jumpHoldingTicks = 0;
                motorHoldingJump = false;
            }
        } else {
            // 10. Step-Up Jump Timing
            if (evaluateStepJump(config.autoJump, distH, player.horizontalCollision, deltaY, onGround)) {
                state = State.JUMPING;
                client.options.keyJump.setDown(true);
                jumpHoldingTicks = 4;
                motorHoldingJump = true;
            } else {
                if (jumpHoldingTicks > 0) {
                    jumpHoldingTicks--;
                } else {
                    if (shouldReleaseMotorJump(false, config.autoSwim, motorHoldingJump, jumpHoldingTicks)) {
                        client.options.keyJump.setDown(false);
                        motorHoldingJump = false;
                    }
                    if (state == State.SWIMMING || state == State.JUMPING) {
                        state = State.WALKING;
                    }
                }
            }
        }

        // 11. Stuck Watchdog Detection
        Vec3 currentPos = player.position();
        double movedDist = currentPos.distanceTo(lastTickPos);
        lastTickPos = currentPos;

        StuckAction stuckAction = evaluateStuck(movedDist, onGround, stuckTicks);
        if (movedDist < 0.04 && onGround) {
            stuckTicks++;
            if (stuckAction == StuckAction.REPATH) {
                Object targetBefore = navigator.getTargetObject();
                PathResult result = navigator.repath(level, player.position(), config.maxRange);
                if (handleRepathResult(result, client, player, navigator, config, targetBefore, callback)) {
                    return; // Esito terminale gestito
                }
            } else if (stuckAction == StuckAction.ABORT) {
                resetMovement(client);
                this.state = State.CANCELLED;
                callback.onStuck();
                return;
            }
        } else {
            stuckTicks = 0;
        }

        // 12. Waypoint Progression
        if (evaluateWaypointAdvance(distH, deltaY, player.isSprinting(), config.sprint)) {
            navigator.advanceWaypoint();
            callback.onStepNode();

            int remainingSteps = navigator.getRemainingSteps();
            int currentIndex = navigator.getCurrentPathIndex();
            if (narrateHints && remainingSteps > 0 && remainingSteps % 5 == 0 && currentIndex != lastAnnouncedStepIndex) {
                lastAnnouncedStepIndex = currentIndex;
                callback.onProgression(remainingSteps);
            }

            if (navigator.isRouteCompleted()) {
                finishArrival(client, player, navigator, config, callback);
            }
        }
    }

    /**
     * Gestisce gli esiti del ricalcolo preservando la parità storica 1:1.
     * @return true se l'esito è terminale (ARRIVED o CANCELLED) e impone l'uscita immediata dal tick.
     */
    public boolean handleRepathResult(
            PathResult result,
            Minecraft client,
            LocalPlayer player,
            RouteNavigator navigator,
            Config.AutoWalk config,
            @Nullable Object targetBefore,
            MotorCallback callback
    ) {
        switch (result.status()) {
            case ALREADY_AT_TARGET -> {
                finishArrival(client, player, navigator, config, callback);
                return true;
            }
            case NO_PATH, OUT_OF_RANGE, SEARCH_BUDGET_EXHAUSTED -> {
                resetMovement(client);
                this.state = State.CANCELLED;
                navigator.clearRoute();
                callback.onNoPath(targetBefore);
                return true;
            }
            case FOUND -> {
                if (client != null && client.options != null) {
                    client.options.keyUp.setDown(false);
                }
                callback.onRepathRequested();
                return true;
            }
        }
        return false;
    }

    public void finishArrival(Minecraft client, LocalPlayer player, RouteNavigator navigator, Config.AutoWalk config, MotorCallback callback) {
        resetMovement(client);
        this.state = State.ARRIVED;
        Object target = navigator.getTargetObject();

        if (config.lookAtTargetOnArrival && target != null) {
            lookAtTarget(player, target);
        }

        navigator.clearRoute();
        callback.onArrival(target);
    }

    public void lookAtTarget(LocalPlayer player, Object target) {
        if (player == null || target == null) return;
        switch (target) {
            case net.minecraft.world.entity.Entity entity -> player.lookAt(EntityAnchorArgument.Anchor.EYES, entity.getEyePosition());
            case org.mcaccess.minecraftaccess.features.point_of_interest.BlockPos3d bp3d -> {
                Vec3 pos = bp3d.getAccuratePosition();
                player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(pos.x, player.getEyeY(), pos.z));
                player.setXRot(0.0f);
            }
            case BlockPos bp -> {
                Vec3 pos = Vec3.atCenterOf(bp);
                player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(pos.x, player.getEyeY(), pos.z));
                player.setXRot(0.0f);
            }
            case org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint wp -> {
                Vec3 pos = Vec3.atCenterOf(wp.pos());
                player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(pos.x, player.getEyeY(), pos.z));
                player.setXRot(0.0f);
            }
            default -> {}
        }
    }

    // ==========================================
    // Funzioni Pure di Valutazione Algoritmica
    // ==========================================

    public boolean evaluateTakeover(boolean stopOnManualInput, boolean manualKeyPressed) {
        if (!stopOnManualInput) return false;
        if (startupGraceTicks > 0) return false;
        return manualKeyPressed;
    }

    public boolean isManualMovementKeyPressed(Minecraft client) {
        if (client == null || client.options == null) return false;
        return isManualMovementKeyPressed(
                client.options.keyDown,
                client.options.keyLeft,
                client.options.keyRight,
                client.options.keyShift
        );
    }

    public boolean isManualMovementKeyPressed(
            @Nullable KeyMapping keyDown,
            @Nullable KeyMapping keyLeft,
            @Nullable KeyMapping keyRight,
            @Nullable KeyMapping keyShift
    ) {
        CrouchIntent crouchIntent = crouchIntentProbe.readIntent();
        boolean manualShift = crouchIntent.reliable() ? crouchIntent.pressed() : (keyShift != null && keyShift.isDown());
        return (keyDown != null && keyDown.isDown())
                || (keyLeft != null && keyLeft.isDown())
                || (keyRight != null && keyRight.isDown())
                || manualShift;
    }

    public static boolean evaluateStepJump(boolean autoJump, double distH, boolean horizontalCollision, double deltaY, boolean onGround) {
        boolean isApproachingStep = (distH <= 1.25 || horizontalCollision) && deltaY > 0.30 && deltaY <= 1.25;
        return autoJump && isApproachingStep && onGround;
    }

    /**
     * Stabilisce se il motore deve assumere il possesso del tasto salto per il nuoto assistito.
     */
    public static boolean shouldHoldJumpForAutoSwim(boolean inLiquid, boolean autoSwim) {
        return inLiquid && autoSwim;
    }

    /**
     * Stabilisce se il motore deve rilasciare un salto di sua proprietà senza interferire
     * con i tempi del salto assistito ancora in corso.
     */
    public static boolean shouldReleaseMotorJump(boolean inLiquid, boolean autoSwim, boolean motorHoldingJump, int jumpHoldingTicks) {
        return motorHoldingJump && (!inLiquid ? jumpHoldingTicks <= 0 : !autoSwim);
    }

    public static boolean evaluateWaypointAdvance(double distH, double deltaY, boolean isSprinting, boolean sprintConfig) {
        double threshold = (sprintConfig && isSprinting) ? 0.70 : 0.45;
        return distH < threshold && Math.abs(deltaY) < 1.0;
    }

    public static float calculateYawDiff(float currentYaw, double dx, double dz) {
        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        return Mth.wrapDegrees(targetYaw - currentYaw);
    }

    public static float calculateSteering(float currentYaw, double dx, double dz, float maxTurnRate) {
        float yawDiff = calculateYawDiff(currentYaw, dx, dz);
        return currentYaw + Mth.clamp(yawDiff, -maxTurnRate, maxTurnRate);
    }

    public static boolean shouldBrakeForTurn(float yawDiff, double distH) {
        return Math.abs(yawDiff) > 55.0f && distH > 0.6;
    }

    public static int updateSprintCooldown(float yawDiff, int currentCooldown) {
        if (Math.abs(yawDiff) > 15.0f) {
            return 20;
        }
        return Math.max(0, currentCooldown - 1);
    }

    public static boolean canSprint(boolean sprintConfig, int sprintCooldown, boolean isShiftDown, float foodLevel) {
        return sprintConfig && sprintCooldown == 0 && !isShiftDown && foodLevel > 6.0f;
    }

    public static StuckAction evaluateStuck(double movedDist, boolean onGround, int currentStuckTicks) {
        if (movedDist < 0.04 && onGround) {
            int nextTicks = currentStuckTicks + 1;
            if (nextTicks >= 24) return StuckAction.ABORT;
            if (nextTicks == 12) return StuckAction.REPATH;
            return StuckAction.NONE;
        }
        return StuckAction.NONE;
    }

    /**
     * Riconciliazione ricca dell'attesa porta basata sulla FSM a 5 stati (Addendum 10.2).
     * 1. Stessa porta chiusa: silenzio, nessun riorientamento o avviso duplicato;
     * 2. Stessa porta aperta: emissione singola onDoorOpened;
     * 3. Porta non più pertinente: azzeramento silenzioso senza avviso;
     * 4. Porta rimossa o sostituita: azzeramento silenzioso;
     * 5. Nuova porta chiusa: apertura nuovo episodio di attesa con singolo allineamento e avviso.
     */
    public boolean processDoorWait(
            @Nullable Level level,
            @Nullable BlockPos doorPos,
            boolean isClosed,
            double distToDoorSq,
            boolean narrateHints,
            MotorCallback callback,
            Runnable stopMovement,
            Consumer<BlockPos> lookAtAction,
            Object target
    ) {
        if (isClosed && doorPos != null) {
            if (distToDoorSq <= 4.5) {
                if (stopMovement != null) {
                    stopMovement.run();
                }
                // Stato 1: Stessa porta ancora chiusa
                if (waitingClosedDoorPos != null && waitingClosedDoorPos.equals(doorPos)) {
                    return true;
                }
                // Stato 5: Nuova porta chiusa intercettata (o primo episodio)
                waitingClosedDoorPos = doorPos;
                if (lookAtAction != null) {
                    lookAtAction.accept(doorPos);
                }
                if (narrateHints && callback != null) {
                    callback.onDoorClosed();
                }
                return true;
            }
        } else if (waitingClosedDoorPos != null) {
            boolean isSameDoor = (doorPos != null && waitingClosedDoorPos.equals(doorPos));
            boolean isStillDoor = (level != null && isDoorBlock(level, waitingClosedDoorPos));
            boolean isRealOpen = isSameDoor && (!isClosed || (level != null && isStillDoor && !isDoorOrGateClosed(level, waitingClosedDoorPos)));

            if (isRealOpen) {
                // Stato 2: Stessa porta ora realmente aperta
                waitingClosedDoorPos = null;
                if (narrateHints && callback != null) {
                    callback.onDoorOpened(target);
                }
            } else {
                // Stato 3 & 4: Porta non più pertinente (rotta cambiata), o rimossa/sostituita
                waitingClosedDoorPos = null;
            }
        }
        return false;
    }

    public boolean processDoorWait(
            BlockPos doorPos,
            boolean isClosed,
            double distToDoorSq,
            boolean narrateHints,
            MotorCallback callback,
            Runnable stopMovement,
            Consumer<BlockPos> lookAtAction,
            Object target
    ) {
        return processDoorWait(null, doorPos, isClosed, distToDoorSq, narrateHints, callback, stopMovement, lookAtAction, target);
    }

    public static boolean isDoorBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.getBlock() instanceof TrapDoorBlock;
    }

    public static boolean isDoorOrGateClosed(Level level, BlockPos pos) {
        return AutoWalkPathfinder.isDoorOrGateClosed(level, pos);
    }
}
