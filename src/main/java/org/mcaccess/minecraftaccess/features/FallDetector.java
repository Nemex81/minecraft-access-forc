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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

@Slf4j
public class FallDetector implements BalmClientModule {
    private final Clock clock;
    private long previousTimeInMillis;
    private int count;
    private final Config.FallDetector config;

    private boolean safetyInterventionActive = false;
    private boolean wasSprintingBeforeIntervention = false;
    private @Nullable BlockPos lastWarnedDangerPos = null;

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
                    inspectNearbyFalls();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "other.repeat_last_narration"))
                .withDefault(InputBinding.key(InputConstants.KEY_G, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
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

        if (client.gui.screen() != null || player.isUnderWater() || player.isSwimming() || player.isVisuallySwimming()) {
            resetSafetyState();
            return;
        }

        // 1. High-frequency Directional Look-Ahead Safety Check (Runs every tick)
        checkLookAheadSafety(client, player, level);

        // 2. Periodic Ambient 3D Audio Area Scan
        long currentTimeInMillis = clock.millis();
        if (currentTimeInMillis - previousTimeInMillis >= config.delay) {
            previousTimeInMillis = currentTimeInMillis;
            if (player.onGround()) {
                searchNearbyPositions();
            }
        }
    }

    private void checkLookAheadSafety(Minecraft client, Player player, Level level) {
        Vec3 delta = player.getDeltaMovement();
        double speedSq = delta.x * delta.x + delta.z * delta.z;

        Vec3 moveDir;
        if (speedSq > 0.0001) {
            moveDir = new Vec3(delta.x, 0, delta.z).normalize();
        } else {
            float yRot = player.getYRot();
            float f = -yRot * ((float) Math.PI / 180F);
            moveDir = new Vec3(Math.sin(f), 0, Math.cos(f)).normalize();
        }

        DangerInfo danger = findDangerAhead(player, level, moveDir);
        if (danger != null) {
            handleDangerDetected(player, danger.pos, danger.depth);
        } else {
            handleDangerCleared(client, player);
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
                return new DangerInfo(groundUnderStep, drop);
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

    private int calculateDangerousDrop(Level level, BlockPos checkGround, int playerBaseY) {
        BlockPos current = checkGround;
        int depth = 0;
        while (depth < 64) {
            BlockState state = level.getBlockState(current);
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

    private record DangerInfo(BlockPos pos, int depth) {
    }

    private void handleDangerDetected(Player player, BlockPos dangerPos, int depth) {
        if (config.autoSlowdown) {
            if (player.isSprinting()) {
                wasSprintingBeforeIntervention = true;
                player.setSprinting(false);
            }
            safetyInterventionActive = true;
        }

        if (config.voiceWarning) {
            if (lastWarnedDangerPos == null || !lastWarnedDangerPos.equals(dangerPos)) {
                lastWarnedDangerPos = dangerPos;
                String relPos = NarrationUtils.narrateRelativePositionOfPlayerAnd(dangerPos);
                String msg = I18n.get("minecraft_access.fall_detector.warning", relPos, NarrationUtils.narrateNumber(depth));
                MainClass.narrate(msg, true);
                assert Minecraft.getInstance().level != null;
                Minecraft.getInstance().level.playLocalSound(dangerPos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, config.volume, 1.0f, true);
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
        safetyInterventionActive = false;
        wasSprintingBeforeIntervention = false;
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

                BlockState feetState = client.level.getBlockState(checkFeet);
                VoxelShape feetShape = feetState.getCollisionShape(client.level, checkFeet);
                if (!feetShape.isEmpty() || isInsurmountableBarrier(client.level, checkFeet)) {
                    continue;
                }

                BlockPos checkGround = checkFeet.below();
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
        if (!(Minecraft.getInstance().level.getBlockState(blockPos).isAir())) return 0;

        return 1 + getDepth(blockPos.below(), maxDepth - 1);
    }
}

