package org.mcaccess.minecraftaccess.features.safety.fall;

import java.time.Clock;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafeDescentCandidate;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafetyMovementGuard;
import org.mcaccess.minecraftaccess.features.safety.traversal.TraversalSafetyAnalyzer;
import org.mcaccess.minecraftaccess.features.safety.traversal.TraversalSafetyContext;
import org.mcaccess.minecraftaccess.features.safety.traversal.TraversalSafetyResult;
import org.mcaccess.minecraftaccess.features.safety.traversal.TraversalSafetyStatus;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;

@Slf4j
public class ProximityFallDetector {

    public enum ProximityStatus {
        CLEAR,
        SAFE_DESCENT,
        WARNING_ZONE_1,
        PRE_BRAKE_ZONE_2A,
        MECHANICAL_BRAKE_ZONE_2B;

        public boolean isAlertOrActiveDescent() {
            return this != CLEAR;
        }
    }

    public record DangerInfo(BlockPos pos, int depth, double distance) {
    }

    private final Clock clock;
    private final Config.FallDetector config;
    private final SafetyMovementGuard movementGuard;

    private boolean safetyInterventionActive = false;
    private boolean wasSprintingBeforeIntervention = false;
    private static boolean autoSneakActive = false;
    private @Nullable BlockPos lastWarnedDangerPos = null;
    private ProximityStatus lastWarnedStatus = ProximityStatus.CLEAR;
    private @Nullable String lastNotifiedDescentId = null;
    private long lastEdgeBumpTime = 0;

    // Test seams package-private per determinismo headless a 0 ms senza runtime Minecraft
    static java.util.function.BiConsumer<String, Boolean> legacyNarrationConsumer = MainClass::narrate;
    static java.util.function.Consumer<SoundCue> legacyAudioConsumer = cue -> {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && cue.soundEvent() != null) {
            BlockPos pos = cue.position() != null ? cue.position() : (client.player != null ? client.player.blockPosition() : BlockPos.ZERO);
            client.level.playLocalSound(pos, cue.soundEvent(), cue.soundSource(), cue.volume(), cue.pitch(), true);
        }
    };
    static java.util.function.Consumer<CognitiveEvent> cognitiveEventConsumer = CognitiveCoordinator::submitEvent;
    static java.util.function.Supplier<SoundEvent> xylophoneSoundSupplier = () -> SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value();
    static java.util.function.Supplier<SoundEvent> anvilSoundSupplier = () -> SoundEvents.ANVIL_LAND;
    static SafetyMovementGuard testMovementGuard = null;

    public static void resetTestSeams() {
        legacyNarrationConsumer = MainClass::narrate;
        legacyAudioConsumer = cue -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && cue.soundEvent() != null) {
                BlockPos pos = cue.position() != null ? cue.position() : (client.player != null ? client.player.blockPosition() : BlockPos.ZERO);
                client.level.playLocalSound(pos, cue.soundEvent(), cue.soundSource(), cue.volume(), cue.pitch(), true);
            }
        };
        cognitiveEventConsumer = CognitiveCoordinator::submitEvent;
        xylophoneSoundSupplier = () -> SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value();
        anvilSoundSupplier = () -> SoundEvents.ANVIL_LAND;
        testMovementGuard = null;
        autoSneakActive = false;
    }

    public static void setAutoSneakActive(boolean active) {
        autoSneakActive = active;
    }

    public static void setLegacyNarrationConsumer(java.util.function.BiConsumer<String, Boolean> consumer) {
        legacyNarrationConsumer = consumer;
    }

    public static void setLegacyAudioConsumer(java.util.function.Consumer<SoundCue> consumer) {
        legacyAudioConsumer = consumer;
    }

    public static void setCognitiveEventConsumer(java.util.function.Consumer<CognitiveEvent> consumer) {
        cognitiveEventConsumer = consumer;
    }

    public static void setXylophoneSoundSupplier(java.util.function.Supplier<SoundEvent> supplier) {
        xylophoneSoundSupplier = supplier;
    }

    public static void setAnvilSoundSupplier(java.util.function.Supplier<SoundEvent> supplier) {
        anvilSoundSupplier = supplier;
    }

    public static void setMovementGuard(@Nullable SafetyMovementGuard guard) {
        testMovementGuard = guard;
    }

    public SafetyMovementGuard getMovementGuard() {
        return testMovementGuard != null ? testMovementGuard : movementGuard;
    }

    public static boolean isAutoSneakActive() {
        return autoSneakActive;
    }

    public static boolean shouldSilenceFallAudioInAutoWalk(boolean autoWalkActive, boolean silenceFallWarnings) {
        return autoWalkActive && silenceFallWarnings;
    }

    public ProximityFallDetector() {
        this(Clock.systemDefaultZone(), Config.getInstance() != null && Config.getInstance().fallDetector != null
                ? Config.getInstance().fallDetector : new Config.FallDetector(), SafetyMovementGuard.createDefault());
    }

    public ProximityFallDetector(Clock clock, Config.FallDetector config, SafetyMovementGuard movementGuard) {
        this.clock = clock;
        this.config = config;
        this.movementGuard = movementGuard;
    }

    /**
     * Tick cinetico di deambulazione ad alta frequenza (tick a tick).
     */
    public ProximityStatus tick(
            Minecraft client,
            Player player,
            Level level,
            boolean autoWalkActive,
            boolean silenceFallVoiceWarnings
    ) {
        if (!config.enabled || !config.proximityEnabled) {
            resetSafetyState();
            return ProximityStatus.CLEAR;
        }

        boolean forward = client.options != null && client.options.keyUp != null && client.options.keyUp.isDown();
        boolean backward = client.options != null && client.options.keyDown != null && client.options.keyDown.isDown();
        boolean left = client.options != null && client.options.keyLeft != null && client.options.keyLeft.isDown();
        boolean right = client.options != null && client.options.keyRight != null && client.options.keyRight.isDown();

        float forwardAmount = (forward ? 1.0f : 0.0f) - (backward ? 1.0f : 0.0f);
        float strafeAmount = (left ? 1.0f : 0.0f) - (right ? 1.0f : 0.0f);

        Vec3 delta = player.getDeltaMovement();
        double speedSq = delta.x * delta.x + delta.z * delta.z;

        Vec3 moveDir = null;
        if (forwardAmount != 0 || strafeAmount != 0) {
            float intendedAngle = (float) Math.toDegrees(Math.atan2(-strafeAmount, forwardAmount));
            float finalYaw = player.getYRot() + intendedAngle;
            float f = -finalYaw * ((float) Math.PI / 180F);
            moveDir = new Vec3(Math.sin(f), 0, Math.cos(f)).normalize();
        } else if (speedSq > 0.0001) {
            moveDir = new Vec3(delta.x, 0, delta.z).normalize();
        }

        if (moveDir == null) {
            getMovementGuard().revokeValidatedDescent();
            // Presidio Fisico del Ciglio da Fermo (Sticky Sneak on Edge)
            if (config.autoSneakOnEdge && isStandingOnDangerousEdge(player, level)) {
                autoSneakActive = true;
                safetyInterventionActive = true;
                getMovementGuard().engageFallProtection();
                return ProximityStatus.MECHANICAL_BRAKE_ZONE_2B;
            }
            handleDangerCleared(client, player);
            return ProximityStatus.CLEAR;
        }

        int playerBaseY = (int) Math.floor(player.getY());
        int effWarningDepth = config.getEffectiveWarningDepth();
        int effAutoSneakDepth = config.autoSneakDepth;

        TraversalSafetyContext traversalContext = new TraversalSafetyContext(
                player.position(),
                player.getBoundingBox(),
                playerBaseY,
                moveDir,
                true,
                effWarningDepth,
                effAutoSneakDepth,
                level
        );
        TraversalSafetyResult traversalResult = TraversalSafetyAnalyzer.analyzeTraversal(traversalContext);

        if (traversalResult.status() == TraversalSafetyStatus.SAFE_DESCENT_AVAILABLE && traversalResult.candidate() != null) {
            getMovementGuard().allowValidatedDescent(traversalResult.candidate().columnId());
            handleSafeDescentAvailable(traversalResult.candidate(), client, player, autoWalkActive, silenceFallVoiceWarnings);
            return ProximityStatus.SAFE_DESCENT;
        } else if (traversalResult.status() == TraversalSafetyStatus.DANGEROUS_DROP && traversalResult.dangerPos() != null) {
            getMovementGuard().engageFallProtection();
            return handleDangerDetected(player, traversalResult.dangerPos(), traversalResult.dangerDepth(), 0.5, autoWalkActive, silenceFallVoiceWarnings);
        } else if (traversalResult.status() == TraversalSafetyStatus.AMBIGUOUS_OR_UNSAFE_DESCENT) {
            getMovementGuard().engageFallProtection();
        }

        getMovementGuard().revokeValidatedDescent();
        DangerInfo danger = findDangerAhead(player, level, moveDir);
        if (danger != null) {
            return handleDangerDetected(player, danger.pos(), danger.depth(), danger.distance(), autoWalkActive, silenceFallVoiceWarnings);
        } else {
            handleDangerCleared(client, player);
            lastWarnedStatus = ProximityStatus.CLEAR;
            lastWarnedDangerPos = null;
            return ProximityStatus.CLEAR;
        }
    }

    public @Nullable DangerInfo findDangerAhead(Player player, Level level, Vec3 moveDir) {
        int maxLookAhead = config.getEffectiveProxMax();
        int minLookAhead = config.getEffectiveProxMin();
        int playerBaseY = (int) Math.floor(player.getY());
        Set<BlockPos> checkedPositions = new HashSet<>();

        int stepCount = (int) Math.ceil((double) maxLookAhead / 0.25);
        BlockPos prevPos = BlockPos.containing(player.getX(), playerBaseY, player.getZ());

        for (int i = 1; i <= stepCount; i++) {
            double dist = i * 0.25;
            double targetX = player.getX() + moveDir.x * dist;
            double targetZ = player.getZ() + moveDir.z * dist;
            BlockPos stepPos = BlockPos.containing(targetX, playerBaseY, targetZ);

            if (!checkedPositions.add(stepPos)) {
                continue;
            }

            // Diagonal corner pinching check
            if (stepPos.getX() != prevPos.getX() && stepPos.getZ() != prevPos.getZ()) {
                BlockPos ortho1 = new BlockPos(stepPos.getX(), playerBaseY, prevPos.getZ());
                BlockPos ortho2 = new BlockPos(prevPos.getX(), playerBaseY, stepPos.getZ());
                if (isInsurmountableBarrier(level, ortho1) || isInsurmountableBarrier(level, ortho2)) {
                    break;
                }
            }

            if (isInsurmountableBarrier(level, stepPos)) {
                break;
            }

            if (!level.getFluidState(stepPos).isEmpty()) {
                prevPos = stepPos;
                continue;
            }

            BlockState stepState = level.getBlockState(stepPos);
            VoxelShape stepShape = stepState.getCollisionShape(level, stepPos);
            if (!stepShape.isEmpty()) {
                BlockPos headPos = stepPos.above();
                BlockState headState = level.getBlockState(headPos);
                VoxelShape headShape = headState.getCollisionShape(level, headPos);
                if (stepShape.max(Direction.Axis.Y) >= 1.0 || !headShape.isEmpty() || headState.getBlock() instanceof IronBarsBlock) {
                    break;
                }
                prevPos = stepPos;
                continue;
            }

            BlockPos headPos = stepPos.above();
            BlockState headState = level.getBlockState(headPos);
            VoxelShape headShape = headState.getCollisionShape(level, headPos);
            if (!headShape.isEmpty() || isInsurmountableBarrier(level, headPos)) {
                break;
            }

            BlockPos groundUnderStep = stepPos.below();
            int drop = calculateDangerousDrop(level, groundUnderStep, playerBaseY);
            if (drop >= config.getEffectiveWarningDepth()) {
                if (dist >= minLookAhead - 0.25) {
                    return new DangerInfo(groundUnderStep, drop, dist);
                }
            }

            prevPos = stepPos;
        }
        return null;
    }

    public boolean isStandingOnDangerousEdge(Player player, Level level) {
        int playerBaseY = (int) Math.floor(player.getY());
        double px = player.getX();
        double pz = player.getZ();

        double[][] sampleOffsets = {
                {0.55, 0.0},
                {-0.55, 0.0},
                {0.0, 0.55},
                {0.0, -0.55},
                {0.45, 0.45},
                {-0.45, 0.45},
                {0.45, -0.45},
                {-0.45, -0.45}
        };

        for (double[] offset : sampleOffsets) {
            BlockPos stepPos = BlockPos.containing(px + offset[0], playerBaseY, pz + offset[1]);

            if (isInsurmountableBarrier(level, stepPos)) {
                continue;
            }

            if (!level.getFluidState(stepPos).isEmpty()) {
                continue;
            }

            BlockState stepState = level.getBlockState(stepPos);
            VoxelShape stepShape = stepState.getCollisionShape(level, stepPos);
            if (!stepShape.isEmpty()) {
                continue;
            }

            BlockPos headPos = stepPos.above();
            BlockState headState = level.getBlockState(headPos);
            VoxelShape headShape = headState.getCollisionShape(level, headPos);
            if (!headShape.isEmpty() || isInsurmountableBarrier(level, headPos)) {
                continue;
            }

            BlockPos groundUnderStep = stepPos.below();
            int drop = calculateDangerousDrop(level, groundUnderStep, playerBaseY);
            if (drop >= config.autoSneakDepth) {
                lastWarnedDangerPos = groundUnderStep;
                return true;
            }
        }
        return false;
    }

    ProximityStatus handleDangerDetected(
            Player player,
            BlockPos dangerPos,
            int depth,
            double distance,
            boolean autoWalkActive,
            boolean silenceFallVoiceWarnings
    ) {
        final double MECHANICAL_BRAKE_THRESHOLD = 0.85;
        final double PRE_BRAKE_THRESHOLD = 1.5;

        // AutoSlowdown: attivo su tutta la fascia di pericolo se abilitato
        if (config.autoSlowdown && !autoWalkActive) {
            if (player.isSprinting()) {
                wasSprintingBeforeIntervention = true;
                player.setSprinting(false);
            }
            safetyInterventionActive = true;
        }

        ProximityStatus status;
        SoundEvent chosenSound;
        CognitivePriority priority;

        if (distance <= MECHANICAL_BRAKE_THRESHOLD) {
            // ZONA 2B — Intervento Meccanico di Salvataggio sul Ciglio Fisico
            status = ProximityStatus.MECHANICAL_BRAKE_ZONE_2B;
            if (config.autoSneakOnEdge && depth >= config.autoSneakDepth) {
                autoSneakActive = true;
                safetyInterventionActive = true;
                getMovementGuard().engageFallProtection();
            }
            // In Zona 2B suona l'incudine (MUTUA ESCLUSIONE: zero xilofono)
            chosenSound = anvilSoundSupplier != null ? anvilSoundSupplier.get() : SoundEvents.ANVIL_LAND;
            priority = CognitivePriority.CRITICAL;
        } else if (distance <= PRE_BRAKE_THRESHOLD) {
            // ZONA 2A — Pre-Freno Acustico d'Emergenza (1.0m - 1.5m)
            status = ProximityStatus.PRE_BRAKE_ZONE_2A;
            // NIENTE auto-sneak qui: il giocatore può fermarsi volontariamente!
            // MUTUA ESCLUSIONE ACUSTICA: l'incudine spegne all'istante lo xilofono
            chosenSound = anvilSoundSupplier != null ? anvilSoundSupplier.get() : SoundEvents.ANVIL_LAND;
            priority = CognitivePriority.CRITICAL;
        } else {
            // ZONA 1 — Pre-Allerta Informativa di Marcia (2.0m - 6.0m)
            status = ProximityStatus.WARNING_ZONE_1;
            // Suona lo xilofono chiaro e non traumatico
            chosenSound = xylophoneSoundSupplier != null ? xylophoneSoundSupplier.get() : SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value();
            priority = CognitivePriority.OPERATIONAL;
        }

        boolean isNewDanger = (lastWarnedDangerPos == null || !lastWarnedDangerPos.equals(dangerPos));
        boolean isStatusEscalation = (status != lastWarnedStatus);
        long now = clock.millis();

        // Durante AutoWalk la voce e tutti i cue sonori (xilofono ed emergenza incudine) sono zittiti a monte
        boolean fallVoiceSilenced = autoWalkActive && silenceFallVoiceWarnings;
        boolean audioSilencedInAutoWalk = shouldSilenceFallAudioInAutoWalk(autoWalkActive, silenceFallVoiceWarnings);

        if (isNewDanger || isStatusEscalation) {
            lastWarnedDangerPos = dangerPos;
            lastWarnedStatus = status;
            lastEdgeBumpTime = now;

            boolean voiceWanted = config.voiceWarning && !fallVoiceSilenced;
            boolean soundWanted = config.playAudioCues && !audioSilencedInAutoWalk;

            if (voiceWanted || soundWanted) {
                String relPos = NarrationUtils.narrateRelativePositionOf(player, dangerPos);
                String msg = I18n.get("minecraft_access.fall_detector.warning", relPos, NarrationUtils.narrateNumber(depth));
                CognitiveEvent event = buildFallEvent(dangerPos, depth, distance, false, voiceWanted, soundWanted, config.volume, msg, chosenSound, priority, now);
                dispatchFallAlert(event, CognitiveCoordinator.isCoordinatorEnabled(), voiceWanted, soundWanted, msg, dangerPos, chosenSound, config.volume);
            }
        } else if (autoSneakActive && now - lastEdgeBumpTime >= 1500) {
            // Edge Bump debounced su collisione ciglio
            lastEdgeBumpTime = now;
            Config.FallDetector.EdgeBumpFeedbackMode bumpMode = config.edgeBumpFeedbackMode;
            boolean soundWanted = (bumpMode == Config.FallDetector.EdgeBumpFeedbackMode.SOUND_AND_VOICE || bumpMode == Config.FallDetector.EdgeBumpFeedbackMode.SOUND_ONLY) && !audioSilencedInAutoWalk;
            boolean voiceWanted = (bumpMode == Config.FallDetector.EdgeBumpFeedbackMode.SOUND_AND_VOICE || bumpMode == Config.FallDetector.EdgeBumpFeedbackMode.VOICE_ONLY) && !fallVoiceSilenced;

            if (voiceWanted || soundWanted) {
                String relPos = NarrationUtils.narrateRelativePositionOf(player, dangerPos);
                String msg = I18n.get("minecraft_access.fall_detector.edge_bump", relPos, NarrationUtils.narrateNumber(depth));
                SoundEvent bumpSound = anvilSoundSupplier != null ? anvilSoundSupplier.get() : SoundEvents.ANVIL_LAND;
                CognitiveEvent event = buildFallEvent(dangerPos, depth, distance, true, voiceWanted, soundWanted, config.volume, msg, bumpSound, CognitivePriority.CRITICAL, now);
                dispatchFallAlert(event, CognitiveCoordinator.isCoordinatorEnabled(), voiceWanted, soundWanted, msg, dangerPos, bumpSound, config.volume);
            }
        }

        return status;
    }

    private void handleSafeDescentAvailable(
            SafeDescentCandidate candidate,
            Minecraft client,
            Player player,
            boolean autoWalkActive,
            boolean silenceFallVoiceWarnings
    ) {
        safetyInterventionActive = false;
        autoSneakActive = false;
        lastWarnedDangerPos = null;

        if (lastNotifiedDescentId == null || !lastNotifiedDescentId.equals(candidate.columnId())) {
            lastNotifiedDescentId = candidate.columnId();
            String msg = I18n.get("minecraft_access.fall_detector.safe_descent");
            boolean fallVoiceSilenced = autoWalkActive && silenceFallVoiceWarnings;

            SoundCue cue = SoundCue.of(SoundEvents.LADDER_STEP, SoundSource.PLAYERS, candidate.entryPos(), 0.7f, 1.2f);
            CognitiveEvent event = new CognitiveEvent(
                    SourceDomain.SAFETY,
                    CognitivePriority.OPERATIONAL,
                    "safety.traversal.safe_descent",
                    StateSignature.of(0, 0, candidate.columnId()),
                    fallVoiceSilenced ? "" : msg,
                    candidate.entryPos(),
                    0.5,
                    SpatialDirection.FORWARD,
                    fallVoiceSilenced ? CognitiveEvent.OutputType.SOUND_ONLY : CognitiveEvent.OutputType.VOICE_AND_SOUND,
                    cue,
                    1500L,
                    true,
                    clock.millis()
            );

            if (CognitiveCoordinator.isCoordinatorEnabled()) {
                cognitiveEventConsumer.accept(event);
            } else {
                if (!fallVoiceSilenced) {
                    legacyNarrationConsumer.accept(msg, true);
                }
                legacyAudioConsumer.accept(cue);
            }
        }
    }

    public void handleDangerCleared(Minecraft client, Player player) {
        if (safetyInterventionActive) {
            if (config.autoRestoreSprint && wasSprintingBeforeIntervention) {
                if (client.options != null && client.options.keyUp != null && client.options.keyUp.isDown() && player.getFoodData().getFoodLevel() > 6) {
                    player.setSprinting(true);
                }
            }
            resetSafetyState();
        }
    }

    public void resetLocalSafetyState() {
        safetyInterventionActive = false;
        wasSprintingBeforeIntervention = false;
        autoSneakActive = false;
        lastWarnedDangerPos = null;
        lastWarnedStatus = ProximityStatus.CLEAR;
        lastNotifiedDescentId = null;
    }

    public void resetSafetyState() {
        getMovementGuard().clearSystemOverride();
        resetLocalSafetyState();
    }

    public void resetSafetyStateForGui() {
        getMovementGuard().suspendForGui();
        resetLocalSafetyState();
    }

    public void inspectNearbyFalls() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        BlockPos center = client.player.blockPosition();
        int range = config.getEffectiveProxMax();
        int playerBaseY = center.getY();

        BlockPos closestPit = null;
        double closestDistSq = Double.MAX_VALUE;
        int closestDepth = 0;

        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                BlockPos checkFeet = center.offset(dx, 0, dz);

                if (!client.level.getFluidState(checkFeet).isEmpty()) {
                    continue;
                }

                BlockState feetState = client.level.getBlockState(checkFeet);
                VoxelShape feetShape = feetState.getCollisionShape(client.level, checkFeet);
                if (!feetShape.isEmpty() || isInsurmountableBarrier(client.level, checkFeet)) {
                    continue;
                }

                BlockPos checkGround = checkFeet.below();
                if (!client.level.getFluidState(checkGround).isEmpty()) {
                    continue;
                }

                int depth = calculateDangerousDrop(client.level, checkGround, playerBaseY);
                if (depth >= config.getEffectiveWarningDepth()) {
                    if (!isLineOfSightBlocked(client.level, center, checkFeet)) {
                        double distSq = center.distSqr(checkGround);
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            closestPit = checkGround;
                            closestDepth = depth;
                        }
                    }
                }
            }
        }

        if (closestPit != null) {
            String relPos = NarrationUtils.narrateRelativePositionOfPlayerAnd(closestPit);
            String msg = I18n.get("minecraft_access.fall_detector.pit_found", relPos, NarrationUtils.narrateNumber(closestDepth));
            legacyNarrationConsumer.accept(msg, true);
            client.level.playLocalSound(closestPit, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, config.volume, 1.0f, true);
        } else {
            legacyNarrationConsumer.accept(I18n.get("minecraft_access.fall_detector.no_pit_nearby"), true);
        }
    }

    public void toggleAutoSneak() {
        config.autoSneakOnEdge = !config.autoSneakOnEdge;
        Config.saveConfig();

        Minecraft client = Minecraft.getInstance();
        if (!config.autoSneakOnEdge) {
            resetSafetyState();
        } else if (client.player != null && client.level != null) {
            tick(client, client.player, client.level, false, false);
        }

        String stateMsg = config.autoSneakOnEdge
                ? I18n.get("minecraft_access.fall_detector.auto_sneak_enabled")
                : I18n.get("minecraft_access.fall_detector.auto_sneak_disabled");
        legacyNarrationConsumer.accept(stateMsg, true);

        if (client.level != null && client.player != null) {
            client.level.playLocalSound(
                    client.player.blockPosition(),
                    config.autoSneakOnEdge ? SoundEvents.NOTE_BLOCK_PLING.value() : SoundEvents.NOTE_BLOCK_BASS.value(),
                    SoundSource.PLAYERS,
                    0.8f,
                    config.autoSneakOnEdge ? 1.2f : 0.6f,
                    true
            );
        }
    }

    public static boolean isSafeWalkableStaircase(Level level, BlockPos landingPos, int playerBaseY) {
        BlockState landingState = level.getBlockState(landingPos);
        if (landingState.getBlock() instanceof StairBlock || landingState.getBlock() instanceof SlabBlock) {
            return true;
        }

        for (int y = landingPos.getY() + 1; y <= playerBaseY + 1; y++) {
            BlockPos abovePos = new BlockPos(landingPos.getX(), y, landingPos.getZ());
            BlockState aboveState = level.getBlockState(abovePos);
            if (aboveState.getBlock() instanceof StairBlock || aboveState.getBlock() instanceof SlabBlock) {
                return true;
            }
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adjacentPos = landingPos.relative(dir).above(1);
            BlockState adjState = level.getBlockState(adjacentPos);
            if (adjState.getBlock() instanceof StairBlock) {
                Direction stairFacing = adjState.getValue(StairBlock.FACING);
                if (stairFacing == dir) {
                    return true;
                }
            }
        }

        return false;
    }

    public int calculateDangerousDrop(Level level, BlockPos checkGround, int playerBaseY) {
        BlockPos current = checkGround;

        int depth = 0;
        while (depth < 64) {
            FluidState fluid = level.getFluidState(current);
            if (TraversalSafetyAnalyzer.isSafeWater(fluid)) {
                return 0;
            }
            if (fluid.is(FluidTags.LAVA)) {
                return 999;
            }

            BlockState state = level.getBlockState(current);
            if (TraversalSafetyAnalyzer.isClimbable(state)) {
                return 0;
            }

            if (TraversalSafetyAnalyzer.isSafeLanding(level, current)) {
                if (isSafeWalkableStaircase(level, current, playerBaseY)) {
                    return 0;
                }
                return depth;
            }

            depth++;
            current = current.below();
        }
        return depth;
    }

    public boolean isInsurmountableBarrier(Level level, BlockPos feetPos) {
        BlockState feetState = level.getBlockState(feetPos);
        VoxelShape feetShape = feetState.getCollisionShape(level, feetPos);

        if (!feetShape.isEmpty() && (feetShape.max(Direction.Axis.Y) >= 1.25 || feetState.getBlock() instanceof IronBarsBlock)) {
            return true;
        }

        BlockPos headPos = feetPos.above();
        BlockState headState = level.getBlockState(headPos);
        VoxelShape headShape = headState.getCollisionShape(level, headPos);

        if (!feetShape.isEmpty() && (!headShape.isEmpty() || headState.getBlock() instanceof IronBarsBlock)) {
            return true;
        }

        if (!feetShape.isEmpty() && feetShape.max(Direction.Axis.Y) >= 0.9) {
            BlockPos ceilingPos = feetPos.above(2);
            BlockState ceilingState = level.getBlockState(ceilingPos);
            VoxelShape ceilingShape = ceilingState.getCollisionShape(level, ceilingPos);
            if (!ceilingShape.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public boolean isLineOfSightBlocked(Level level, BlockPos origin, BlockPos target) {
        int x0 = origin.getX();
        int z0 = origin.getZ();
        int y0 = origin.getY();
        int x1 = target.getX();
        int z1 = target.getZ();

        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx - dz;

        int currX = x0;
        int currZ = z0;

        while (true) {
            BlockPos checkPos = new BlockPos(currX, y0, currZ);
            if (isInsurmountableBarrier(level, checkPos)) {
                return true;
            }

            if (currX == x1 && currZ == z1) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                currX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currZ += sz;
            }
        }

        return false;
    }

    public static @Nullable CognitiveEvent buildFallEvent(
            BlockPos dangerPos,
            int depth,
            double distance,
            boolean isEdgeBump,
            boolean voiceEnabled,
            boolean soundEnabled,
            float volume,
            @NotNull String msg,
            @Nullable SoundEvent soundEvent,
            @NotNull CognitivePriority priority,
            long now
    ) {
        if (!voiceEnabled && !soundEnabled) {
            return null;
        }

        Objects.requireNonNull(msg, "narrationText cannot be null");
        if (msg.isBlank()) {
            throw new IllegalArgumentException("narrationText cannot be blank");
        }

        CognitiveEvent.OutputType outputType;
        if (voiceEnabled && soundEnabled) {
            outputType = CognitiveEvent.OutputType.VOICE_AND_SOUND;
        } else if (voiceEnabled) {
            outputType = CognitiveEvent.OutputType.VOICE_ONLY;
        } else {
            outputType = CognitiveEvent.OutputType.SOUND_ONLY;
        }

        String semanticKey = isEdgeBump ? "safety.fall.edge_bump" : "safety.fall.warning";
        StateSignature signature = isEdgeBump
                ? StateSignature.of(0, depth, "fall:edge_bump")
                : StateSignature.of((int) Math.round(distance), depth, "fall:warning");

        SoundCue cue = soundEnabled && soundEvent != null
                ? SoundCue.of(soundEvent, SoundSource.PLAYERS, dangerPos, volume, 1.0f)
                : null;

        return CognitiveEvent.createSafetyAlert(
                semanticKey,
                priority,
                signature,
                msg,
                dangerPos,
                distance,
                SpatialDirection.FORWARD,
                outputType,
                cue,
                2000,
                now
        );
    }

    public static void dispatchFallAlert(
            @Nullable CognitiveEvent event,
            boolean coordinatorEnabled,
            boolean voiceEnabled,
            boolean soundEnabled,
            String legacyMsg,
            BlockPos dangerPos,
            @Nullable SoundEvent soundEvent,
            float volume
    ) {
        if (coordinatorEnabled && event != null) {
            cognitiveEventConsumer.accept(event);
        } else {
            if (voiceEnabled && legacyMsg != null && !legacyMsg.isBlank()) {
                legacyNarrationConsumer.accept(legacyMsg, true);
            }
            if (soundEnabled && soundEvent != null) {
                SoundCue cue = SoundCue.of(soundEvent, SoundSource.PLAYERS, dangerPos, volume, 1.0f);
                legacyAudioConsumer.accept(cue);
            }
        }
    }

    public static boolean shouldSilenceFallVoiceWarnings(boolean isAutoWalkActive, boolean silenceConfig) {
        return isAutoWalkActive && silenceConfig;
    }
}
