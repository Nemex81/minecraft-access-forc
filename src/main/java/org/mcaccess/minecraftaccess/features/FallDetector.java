package org.mcaccess.minecraftaccess.features;

import java.time.Clock;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.ModifierUtils;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;

@Slf4j
public class FallDetector implements BalmClientModule {
    private final Clock clock;
    private long previousTimeInMillis;
    private int count;
    private final Config.FallDetector config;

    private boolean safetyInterventionActive = false;
    private boolean wasSprintingBeforeIntervention = false;
    private static boolean autoSneakActive = false;
    private @Nullable BlockPos lastWarnedDangerPos = null;
    private long lastEdgeBumpTime = 0;

    // Package-private test seams for deterministic headless testing without Minecraft runtime
    static java.util.function.BiConsumer<String, Boolean> legacyNarrationConsumer = MainClass::narrate;
    static java.util.function.Consumer<SoundCue> legacyAudioConsumer = cue -> {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && cue.soundEvent() != null) {
            BlockPos pos = cue.position() != null ? cue.position() : (client.player != null ? client.player.blockPosition() : BlockPos.ZERO);
            client.level.playLocalSound(pos, cue.soundEvent(), cue.soundSource(), cue.volume(), cue.pitch(), true);
        }
    };
    static java.util.function.Consumer<CognitiveEvent> cognitiveEventConsumer = CognitiveCoordinator::submitEvent;
    static java.util.function.Supplier<net.minecraft.sounds.SoundEvent> fallSoundSupplier = () -> SoundEvents.ANVIL_HIT;

    static void resetTestSeams() {
        legacyNarrationConsumer = MainClass::narrate;
        legacyAudioConsumer = cue -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && cue.soundEvent() != null) {
                BlockPos pos = cue.position() != null ? cue.position() : (client.player != null ? client.player.blockPosition() : BlockPos.ZERO);
                client.level.playLocalSound(pos, cue.soundEvent(), cue.soundSource(), cue.volume(), cue.pitch(), true);
            }
        };
        cognitiveEventConsumer = CognitiveCoordinator::submitEvent;
        fallSoundSupplier = () -> SoundEvents.ANVIL_HIT;
    }

    static void setLegacyNarrationConsumer(java.util.function.BiConsumer<String, Boolean> consumer) {
        legacyNarrationConsumer = consumer;
    }

    static void setLegacyAudioConsumer(java.util.function.Consumer<SoundCue> consumer) {
        legacyAudioConsumer = consumer;
    }

    static void setCognitiveEventConsumer(java.util.function.Consumer<CognitiveEvent> consumer) {
        cognitiveEventConsumer = consumer;
    }

    static void setFallSoundSupplier(java.util.function.Supplier<net.minecraft.sounds.SoundEvent> supplier) {
        fallSoundSupplier = supplier;
    }

    static @Nullable CognitiveEvent buildFallEvent(
            BlockPos dangerPos,
            int depth,
            double distance,
            boolean isEdgeBump,
            boolean voiceEnabled,
            boolean soundEnabled,
            float volume,
            @NotNull String msg,
            long now
    ) {
        if (!voiceEnabled && !soundEnabled) {
            return null;
        }

        java.util.Objects.requireNonNull(msg, "narrationText cannot be null");
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

        SoundCue cue = soundEnabled
                ? SoundCue.of(fallSoundSupplier != null ? fallSoundSupplier.get() : null, SoundSource.BLOCKS, dangerPos, volume, 1.0f)
                : null;

        return CognitiveEvent.createSafetyAlert(
                semanticKey,
                CognitivePriority.CRITICAL,
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

    static void dispatchFallAlert(
            @Nullable CognitiveEvent event,
            boolean coordinatorEnabled,
            boolean voiceEnabled,
            boolean soundEnabled,
            String legacyMsg,
            BlockPos dangerPos,
            float volume
    ) {
        if (coordinatorEnabled && event != null) {
            cognitiveEventConsumer.accept(event);
        } else {
            if (voiceEnabled && legacyMsg != null && !legacyMsg.isBlank()) {
                legacyNarrationConsumer.accept(legacyMsg, true);
            }
            if (soundEnabled) {
                SoundCue cue = SoundCue.of(fallSoundSupplier != null ? fallSoundSupplier.get() : null, SoundSource.BLOCKS, dangerPos, volume, 1.0f);
                legacyAudioConsumer.accept(cue);
            }
        }
    }

    public static boolean isAutoSneakActive() {
        return autoSneakActive;
    }

    public FallDetector() {
        clock = Clock.systemDefaultZone();
        previousTimeInMillis = clock.millis();
        config = Config.getInstance().fallDetector;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "fall_detector");
    }

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register(this::tick);

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "fall_detector.inspect_fall"))
                .withDefault(InputBinding.key(InputConstants.KEY_F, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!org.mcaccess.minecraftaccess.utils.ModifierUtils.hasAltOnly()) return false;
                    inspectNearbyFalls();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "fall_detector.toggle_auto_sneak"))
                .withDefault(InputBinding.key(InputConstants.KEY_F, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasControlAndAlt()) return false;
                    toggleAutoSneak();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "other.repeat_last_narration"))
                .withDefault(InputBinding.key(InputConstants.KEY_G, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasAltOnly()) return false;
                    MainClass.repeatLastNarration();
                    return true;
                })
                .build();
    }

    private void tick(Minecraft client, Player player, Level level) {
        if (!config.enabled) {
            resetSafetyState();
            return;
        }

        if (client.gui.screen() != null || player.isUnderWater() || player.isInWater() || player.isInWaterOrRain() || player.isSwimming() || player.isVisuallySwimming() || player.isEyeInFluid(FluidTags.WATER)) {
            resetSafetyState();
            return;
        }

        // 1. High-frequency Directional Look-Ahead Safety Check (Runs every tick)
        checkLookAheadSafety(client, player, level);

        if (autoSneakActive) {
            client.options.keyShift.setDown(true);
            player.setShiftKeyDown(true);
        }

        // 2. Periodic Ambient 3D Audio Area Scan
        long currentTimeInMillis = clock.millis();
        if (currentTimeInMillis - previousTimeInMillis >= config.delay) {
            previousTimeInMillis = currentTimeInMillis;
            if (player.onGround() && !player.isInWater() && !player.isInLiquid()) {
                searchNearbyPositions();
            }
        }
    }

    private void checkLookAheadSafety(Minecraft client, Player player, Level level) {
        boolean forward = client.options.keyUp.isDown();
        boolean backward = client.options.keyDown.isDown();
        boolean left = client.options.keyLeft.isDown();
        boolean right = client.options.keyRight.isDown();

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
            // Presidio Fisico del Ciglio da Fermo (Sticky Sneak on Edge)
            if (config.autoSneakOnEdge && isStandingOnDangerousEdge(player, level)) {
                autoSneakActive = true;
                safetyInterventionActive = true;
                client.options.keyShift.setDown(true);
                player.setShiftKeyDown(true);
                return;
            }
            handleDangerCleared(client, player);
            return;
        }

        DangerInfo danger = findDangerAhead(player, level, moveDir);
        if (danger != null) {
            handleDangerDetected(player, danger.pos, danger.depth, danger.distance);
        } else {
            handleDangerCleared(client, player);
        }
    }

    private boolean isStandingOnDangerousEdge(Player player, Level level) {
        int playerBaseY = (int) Math.floor(player.getY());
        double px = player.getX();
        double pz = player.getZ();

        // Campiona 8 punti radiali perimetrali attorno alla hitbox del giocatore (raggio 0.45m - 0.70m)
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

            BlockPos groundUnderStep = stepPos.below();
            int drop = calculateDangerousDrop(level, groundUnderStep, playerBaseY);
            if (drop >= config.depth) {
                lastWarnedDangerPos = groundUnderStep;
                return true;
            }
        }
        return false;
    }

    private void toggleAutoSneak() {
        config.autoSneakOnEdge = !config.autoSneakOnEdge;
        Config.saveConfig();

        Minecraft client = Minecraft.getInstance();
        if (!config.autoSneakOnEdge) {
            resetSafetyState();
        } else if (client.player != null && client.level != null) {
            checkLookAheadSafety(client, client.player, client.level);
        }

        String stateMsg = config.autoSneakOnEdge
                ? I18n.get("minecraft_access.fall_detector.auto_sneak_enabled")
                : I18n.get("minecraft_access.fall_detector.auto_sneak_disabled");
        MainClass.narrate(stateMsg, true);

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

    private @Nullable DangerInfo findDangerAhead(Player player, Level level, Vec3 moveDir) {
        double maxLookAhead = Math.max(1.0, (double) config.slowdownDistance);
        int playerBaseY = (int) Math.floor(player.getY());
        Set<BlockPos> checkedPositions = new HashSet<>();

        int stepCount = (int) Math.ceil(maxLookAhead / 0.25);
        BlockPos prevPos = BlockPos.containing(player.getX(), playerBaseY, player.getZ());

        for (int i = 1; i <= stepCount; i++) {
            double dist = i * 0.25;
            double targetX = player.getX() + moveDir.x * dist;
            double targetZ = player.getZ() + moveDir.z * dist;
            BlockPos stepPos = BlockPos.containing(targetX, playerBaseY, targetZ);

            if (!checkedPositions.add(stepPos)) {
                continue;
            }

            // Diagonal corner pinching check: if moving diagonally (both X and Z changed from prevPos)
            if (stepPos.getX() != prevPos.getX() && stepPos.getZ() != prevPos.getZ()) {
                BlockPos ortho1 = new BlockPos(stepPos.getX(), playerBaseY, prevPos.getZ());
                BlockPos ortho2 = new BlockPos(prevPos.getX(), playerBaseY, stepPos.getZ());
                if (isInsurmountableBarrier(level, ortho1) || isInsurmountableBarrier(level, ortho2)) {
                    // Corner is pinched by adjacent barrier! Stop lookahead immediately.
                    break;
                }
            }

            if (isInsurmountableBarrier(level, stepPos)) {
                // Physical barrier (wall, fence railing, window pane, closed door, low ceiling) blocks the player!
                break;
            }

            // If step position contains water/fluid, moving into water is completely safe
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
                    // Cannot walk horizontally through a full solid block / sill with window above!
                    break;
                }
                prevPos = stepPos;
                continue;
            }

            BlockPos groundUnderStep = stepPos.below();
            int drop = calculateDangerousDrop(level, groundUnderStep, playerBaseY);
            if (drop >= config.depth) {
                return new DangerInfo(groundUnderStep, drop, dist);
            }

            prevPos = stepPos;
        }
        return null;
    }

    private boolean isSafeWalkableStaircase(Level level, BlockPos landingPos, int playerBaseY) {
        BlockState landingState = level.getBlockState(landingPos);
        if (landingState.getBlock() instanceof StairBlock || landingState.getBlock() instanceof SlabBlock) {
            return true;
        }

        // Check if there is a stair step overhead along the column between landingPos and playerBaseY + 1
        for (int y = landingPos.getY() + 1; y <= playerBaseY + 1; y++) {
            BlockPos abovePos = new BlockPos(landingPos.getX(), y, landingPos.getZ());
            BlockState aboveState = level.getBlockState(abovePos);
            if (aboveState.getBlock() instanceof StairBlock || aboveState.getBlock() instanceof SlabBlock) {
                return true;
            }
        }

        return false;
    }

    private boolean isClimbableBlock(BlockState state) {
        return state.is(BlockTags.CLIMBABLE) || state.getBlock() instanceof LadderBlock || state.getBlock() instanceof VineBlock || state.getBlock() instanceof ScaffoldingBlock;
    }

    private boolean isSafeLandingBlock(BlockState state) {
        return state.is(Blocks.COBWEB) || state.getBlock() instanceof HayBlock || state.getBlock() instanceof HoneyBlock || state.getBlock() instanceof SlimeBlock || state.getBlock() instanceof PowderSnowBlock;
    }

    private boolean isSafeClimbableDescender(Level level, BlockPos startPos) {
        BlockPos cur = startPos;
        int scanned = 0;
        while (scanned < 64) {
            BlockState st = level.getBlockState(cur);
            if (isClimbableBlock(st) || !level.getFluidState(cur).isEmpty()) {
                cur = cur.below();
                scanned++;
                continue;
            }
            int fallBelowClimbable = 0;
            while (fallBelowClimbable <= 3) {
                if (!level.getFluidState(cur).isEmpty() || isSafeLandingBlock(level.getBlockState(cur))) {
                    return true;
                }
                if (!level.getBlockState(cur).isAir() && !level.getBlockState(cur).getCollisionShape(level, cur).isEmpty()) {
                    return true;
                }
                fallBelowClimbable++;
                cur = cur.below();
            }
            return false;
        }
        return true;
    }

    private int calculateDangerousDrop(Level level, BlockPos checkGround, int playerBaseY) {
        BlockPos current = checkGround;

        // Check if current step position or immediately above is a climbable (e.g. ladder to descend from roof)
        if (isClimbableBlock(level.getBlockState(current)) || isClimbableBlock(level.getBlockState(current.above()))) {
            if (isSafeClimbableDescender(level, current)) {
                return 0; // Safe ladder/climbable descent!
            }
        }

        int depth = 0;
        while (depth < 64) {
            FluidState fluid = level.getFluidState(current);
            if (!fluid.isEmpty()) {
                // Water or other fluid completely negates fall damage, safe landing!
                return 0;
            }

            BlockState state = level.getBlockState(current);
            if (isClimbableBlock(state)) {
                if (isSafeClimbableDescender(level, current)) {
                    return 0;
                }
            }

            if (isSafeLandingBlock(state)) {
                return 0; // Soft landing (hay, cobweb, honey, slime, powder snow)
            }

            if (!state.isAir() && !state.getCollisionShape(level, current).isEmpty()) {
                // Landed on a solid block at 'current'
                if (isSafeWalkableStaircase(level, current, playerBaseY)) {
                    return 0; // Safe staircase, not a dangerous fall
                }
                return depth;
            }
            depth++;
            current = current.below();
        }
        return depth;
    }

    private boolean isInsurmountableBarrier(Level level, BlockPos feetPos) {
        BlockState feetState = level.getBlockState(feetPos);
        VoxelShape feetShape = feetState.getCollisionShape(level, feetPos);

        // 1. High barriers like fences, cobblestone walls (height >= 1.25m) or window panes / iron bars
        if (!feetShape.isEmpty() && (feetShape.max(Direction.Axis.Y) >= 1.25 || feetState.getBlock() instanceof IronBarsBlock)) {
            return true;
        }

        // 2. Solid feet block (1.0m sill/wall) with solid or window head block
        BlockPos headPos = feetPos.above();
        BlockState headState = level.getBlockState(headPos);
        VoxelShape headShape = headState.getCollisionShape(level, headPos);

        if (!feetShape.isEmpty() && (!headShape.isEmpty() || headState.getBlock() instanceof IronBarsBlock)) {
            return true;
        }

        // 3. Step with low ceiling (1.0m step at feet, and ceiling at Y+2 is solid, blocking jumping)
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

    private boolean isLineOfSightBlocked(Level level, BlockPos origin, BlockPos target) {
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

    private record DangerInfo(BlockPos pos, int depth, double distance) {
    }

    private void handleDangerDetected(Player player, BlockPos dangerPos, int depth, double distance) {
        // ─────────────────────────────────────────────────────────────────
        // ZONA 1 — Pre-Allerta informativa & Rallentamento corsa
        // Distanza dal bordo > 0.85 m  →  avviso + slowdown, NIENTE sneak
        // ─────────────────────────────────────────────────────────────────
        final double EDGE_SNEAK_THRESHOLD = 0.85;

        if (config.autoSlowdown) {
            if (player.isSprinting()) {
                wasSprintingBeforeIntervention = true;
                player.setSprinting(false);
            }
            safetyInterventionActive = true;
        }

        // ─────────────────────────────────────────────────────────────────
        // ZONA 2 — Bordo fisico immediato / Ciglio  (d ≤ 0.85 m)
        // Solo qui si attiva l'accovacciamento forzato anti-caduta
        // ─────────────────────────────────────────────────────────────────
        if (config.autoSneakOnEdge && distance <= EDGE_SNEAK_THRESHOLD) {
            autoSneakActive = true;
            safetyInterventionActive = true;
        }

        boolean isNewDanger = (lastWarnedDangerPos == null || !lastWarnedDangerPos.equals(dangerPos));
        long now = clock.millis();
        if (isNewDanger) {
            lastWarnedDangerPos = dangerPos;
            lastEdgeBumpTime = now;

            boolean voiceWanted = config.voiceWarning;
            boolean soundWanted = config.playAudioCues;
            if (voiceWanted || soundWanted) {
                String relPos = NarrationUtils.narrateRelativePositionOfPlayerAnd(dangerPos);
                String msg = I18n.get("minecraft_access.fall_detector.warning", relPos, NarrationUtils.narrateNumber(depth));
                CognitiveEvent event = buildFallEvent(dangerPos, depth, distance, false, voiceWanted, soundWanted, config.volume, msg, now);
                dispatchFallAlert(event, CognitiveCoordinator.isCoordinatorEnabled(), voiceWanted, soundWanted, msg, dangerPos, config.volume);
            }
        } else if (autoSneakActive && now - lastEdgeBumpTime >= 1500) {
            // Edge Bump debounced — solo quando in Zona 2 e si insiste verso il vuoto
            lastEdgeBumpTime = now;
            Config.FallDetector.EdgeBumpFeedbackMode bumpMode = config.edgeBumpFeedbackMode;
            boolean soundWanted = bumpMode == Config.FallDetector.EdgeBumpFeedbackMode.SOUND_AND_VOICE || bumpMode == Config.FallDetector.EdgeBumpFeedbackMode.SOUND_ONLY;
            boolean voiceWanted = bumpMode == Config.FallDetector.EdgeBumpFeedbackMode.SOUND_AND_VOICE || bumpMode == Config.FallDetector.EdgeBumpFeedbackMode.VOICE_ONLY;

            if (voiceWanted || soundWanted) {
                String relPos = NarrationUtils.narrateRelativePositionOfPlayerAnd(dangerPos);
                String msg = I18n.get("minecraft_access.fall_detector.edge_bump", relPos, NarrationUtils.narrateNumber(depth));
                CognitiveEvent event = buildFallEvent(dangerPos, depth, distance, true, voiceWanted, soundWanted, config.volume, msg, now);
                dispatchFallAlert(event, CognitiveCoordinator.isCoordinatorEnabled(), voiceWanted, soundWanted, msg, dangerPos, config.volume);
            }
        }
    }

    private void handleDangerCleared(Minecraft client, Player player) {
        if (safetyInterventionActive) {
            if (config.autoRestoreSprint && wasSprintingBeforeIntervention) {
                if (client.options.keyUp.isDown() && player.getFoodData().getFoodLevel() > 6) {
                    player.setSprinting(true);
                }
            }
            resetSafetyState();
        }
    }

    private void resetSafetyState() {
        if (autoSneakActive) {
            Minecraft client = Minecraft.getInstance();
            if (client.options != null && client.player != null) {
                client.options.keyShift.setDown(false);
                client.player.setShiftKeyDown(false);
            }
        }
        safetyInterventionActive = false;
        wasSprintingBeforeIntervention = false;
        autoSneakActive = false;
        lastWarnedDangerPos = null;
    }

    private void inspectNearbyFalls() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        BlockPos center = client.player.blockPosition();
        int range = config.range;
        int playerBaseY = center.getY();

        BlockPos closestPit = null;
        double closestDistSq = Double.MAX_VALUE;
        int closestDepth = 0;

        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                BlockPos checkFeet = center.offset(dx, 0, dz);

                // Skip positions that are inside water or fluid
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
                if (depth >= config.depth) {
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
            MainClass.narrate(msg, true);
            client.level.playLocalSound(closestPit, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, config.volume, 1.0f, true);
        } else {
            MainClass.narrate(I18n.get("minecraft_access.fall_detector.no_pit_nearby"), true);
        }
    }

    private void searchNearbyPositions() {
        assert Minecraft.getInstance().player != null;
        assert Minecraft.getInstance().level != null;
        BlockPos center = Minecraft.getInstance().player.blockPosition();
        Level level = Minecraft.getInstance().level;

        Queue<BlockPos> toSearch = new LinkedList<>();
        Set<BlockPos> searched = new HashSet<>();
        int[] dirX = new int[]{-1, 0, 1, 0};
        int[] dirZ = new int[]{0, 1, 0, -1};
        count = 0;

        toSearch.add(center);
        searched.add(center);

        while (!toSearch.isEmpty()) {
            BlockPos item = toSearch.poll();
            playOnFall(item);

            for (int i = 0; i < 4; i++) {
                BlockPos dir = new BlockPos(item.getX() + dirX[i], item.getY(), item.getZ() + dirZ[i]);

                if (isValid(level, dir, center, searched)) {
                    toSearch.add(dir);
                    searched.add(dir);
                }
            }
        }
    }

    private boolean isValid(Level level, BlockPos dir, BlockPos center, Set<BlockPos> searched) {
        if (Math.abs(dir.getX() - center.getX()) > config.range) {
            return false;
        }

        if (Math.abs(dir.getZ() - center.getZ()) > config.range) {
            return false;
        }

        if (searched.contains(dir)) {
            return false;
        }

        if (isInsurmountableBarrier(level, dir)) {
            return false;
        }

        return true;
    }

    private void playOnFall(BlockPos toCheck) {
        assert Minecraft.getInstance().level != null;
        if (!Minecraft.getInstance().level.getFluidState(toCheck).isEmpty()) return;
        if (!Minecraft.getInstance().level.getBlockState(toCheck).isAir()) return;

        if (getDepth(toCheck, config.depth) < config.depth) return;

        ++count;
        log.debug("{}) Found qualified fall position: x:{} y:{} z:{}", count, toCheck.getX(), toCheck.getY(), toCheck.getZ());
        Minecraft.getInstance().level.playLocalSound(toCheck, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, config.volume, 1.0f, true);
    }

    private int getDepth(BlockPos blockPos, int maxDepth) {
        if (maxDepth <= 0) {
            return 0;
        }

        assert Minecraft.getInstance().level != null;
        if (!Minecraft.getInstance().level.getFluidState(blockPos).isEmpty()) return 0;
        if (!(Minecraft.getInstance().level.getBlockState(blockPos).isAir())) return 0;

        return 1 + getDepth(blockPos.below(), maxDepth - 1);
    }
}

