package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pure, side-effect free analyzer that evaluates whether a player's movement corridor
 * leads to a dangerous drop or an intentional, validated safe descent (ladder, vines, scaffolding, water).
 */
public final class TraversalSafetyAnalyzer {

    private TraversalSafetyAnalyzer() {
    }

    public static @NotNull TraversalSafetyResult analyzeTraversal(@NotNull TraversalSafetyContext context) {
        if (!context.hasMovementIntent() || context.movementIntent() == null) {
            return TraversalSafetyResult.notApplicable("No movement intent provided");
        }

        Vec3 intent = context.movementIntent();
        Vec3 playerPos = context.playerPos();
        int playerBaseY = context.playerBaseY();
        BlockGetter level = context.level();
        int dangerThreshold = context.dangerDropThreshold();

        BlockPos playerBlockPos = BlockPos.containing(playerPos.x, playerBaseY, playerPos.z);

        // Sample entry candidate along the normalized intent vector (0.35m to 0.75m ahead)
        double checkDist = 0.55;
        BlockPos entryFeetPos = BlockPos.containing(playerPos.x + intent.x * checkDist, playerBaseY, playerPos.z + intent.z * checkDist);

        // If target cell is at player feet and player is already standing on solid ground, check step-off
        boolean isSteppingOffEdge = !entryFeetPos.equals(playerBlockPos);
        BlockPos candidateEntry = isSteppingOffEdge ? entryFeetPos : entryFeetPos;

        // 1. Check if the entry cell or the cell immediately below is a safe descent column
        SafeDescentCandidate candidate = findDescentCandidate(level, playerBlockPos, candidateEntry, intent, dangerThreshold);
        if (candidate != null) {
            return TraversalSafetyResult.safeDescent(candidate, "Validated safe descent via " + candidate.type());
        }

        // 2. If candidate is broken or invalid, check if there is an unvalidated climbable that drops into danger
        if (hasBrokenClimbableAhead(level, candidateEntry, dangerThreshold)) {
            return TraversalSafetyResult.ambiguousOrUnsafe("Broken climbable column or unsafe drop below descent structure");
        }

        // 3. Otherwise evaluate standard drop depth in the entry cell
        BlockPos groundUnderStep = candidateEntry.below();
        int drop = calculateDrop(level, groundUnderStep, dangerThreshold);
        if (drop >= dangerThreshold) {
            return TraversalSafetyResult.dangerousDrop(groundUnderStep, drop, "Drop depth " + drop + " exceeds threshold " + dangerThreshold);
        }

        return TraversalSafetyResult.notApplicable("Corridor drop is within safe walk limits (" + drop + " < " + dangerThreshold + ")");
    }

    private static @Nullable SafeDescentCandidate findDescentCandidate(
            @NotNull BlockGetter level,
            @NotNull BlockPos playerPos,
            @NotNull BlockPos entryPos,
            @NotNull Vec3 intent,
            int dangerThreshold
    ) {
        // Candidate locations to probe:
        // A. Direct entry at player feet level (e.g. scaffolding, open trapdoor over ladder)
        // B. Stepping onto ladder attached to external wall at feet - 1 level
        // C. Direct block under entry
        BlockPos[] probePositions = {
                entryPos,
                entryPos.below(),
                playerPos.below()
        };

        for (BlockPos probePos : probePositions) {
            BlockState state = level.getBlockState(probePos);

            // Open trapdoor check over a climbable below
            if (state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN)) {
                BlockPos belowTrapdoor = probePos.below();
                BlockState belowState = level.getBlockState(belowTrapdoor);
                if (isClimbable(belowState)) {
                    SafeDescentCandidate validCol = validateClimbableColumn(level, entryPos, belowTrapdoor, belowState, intent, dangerThreshold);
                    if (validCol != null) return validCol;
                }
            }

            if (isClimbable(state)) {
                SafeDescentCandidate validCol = validateClimbableColumn(level, entryPos, probePos, state, intent, dangerThreshold);
                if (validCol != null) return validCol;
            }
        }

        // Water landing column check: probe downwards from entryPos
        BlockPos waterProbe = entryPos;
        for (int d = 0; d <= Math.max(8, dangerThreshold + 2); d++) {
            FluidState fluid = level.getFluidState(waterProbe);
            if (isSafeWater(fluid)) {
                SafeDescentCandidate waterCol = validateWaterColumn(level, entryPos, waterProbe, dangerThreshold);
                if (waterCol != null) return waterCol;
            }
            waterProbe = waterProbe.below();
        }

        return null;
    }

    private static @Nullable SafeDescentCandidate validateClimbableColumn(
            @NotNull BlockGetter level,
            @NotNull BlockPos entryPos,
            @NotNull BlockPos topPos,
            @NotNull BlockState topState,
            @NotNull Vec3 intent,
            int dangerThreshold
    ) {
        SafeDescentType type = resolveType(topState);
        Direction wallFacing = null;

        if (topState.getBlock() instanceof LadderBlock) {
            wallFacing = topState.getValue(LadderBlock.FACING);
            // Ladder requires entering towards the wall or onto the ladder rung
            // If the ladder is facing NORTH, its rungs are on the North face and attached to the South block.
        }

        // Trace column downwards until terrain or non-climbable
        BlockPos cur = topPos;
        int depthScanned = 0;
        while (depthScanned < 64) {
            BlockState curState = level.getBlockState(cur);
            FluidState curFluid = level.getFluidState(cur);

            if (isClimbable(curState) || isSafeWater(curFluid)) {
                cur = cur.below();
                depthScanned++;
                continue;
            }

            // Exited climbable: check if landing position is safe
            BlockPos landingPos = cur;
            if (isSafeLanding(level, landingPos)) {
                return SafeDescentCandidate.of(entryPos, topPos, landingPos, type, wallFacing);
            }

            // Check drop distance from end of climbable to solid ground
            int dropBelowClimbable = calculateDrop(level, landingPos, dangerThreshold);
            if (dropBelowClimbable < dangerThreshold) {
                return SafeDescentCandidate.of(entryPos, topPos, landingPos.below(dropBelowClimbable), type, wallFacing);
            } else {
                // Broken ladder dropping into a deep abyss!
                return null;
            }
        }

        return null;
    }

    private static @Nullable SafeDescentCandidate validateWaterColumn(
            @NotNull BlockGetter level,
            @NotNull BlockPos entryPos,
            @NotNull BlockPos waterTop,
            int dangerThreshold
    ) {
        // Water drops require at least 1 full water block and non-lava
        BlockPos cur = waterTop;
        int depth = 0;
        while (depth < 64) {
            FluidState fs = level.getFluidState(cur);
            if (!isSafeWater(fs)) {
                break;
            }
            cur = cur.below();
            depth++;
        }
        if (depth >= 1) {
            return SafeDescentCandidate.of(entryPos, waterTop, cur, SafeDescentType.WATER_DESCENT, null);
        }
        return null;
    }

    private static boolean hasBrokenClimbableAhead(BlockGetter level, BlockPos entryPos, int dangerThreshold) {
        BlockPos probe = entryPos.below();
        BlockState state = level.getBlockState(probe);
        if (isClimbable(state)) {
            // Trace down
            BlockPos cur = probe;
            int scanned = 0;
            while (scanned < 64 && isClimbable(level.getBlockState(cur))) {
                cur = cur.below();
                scanned++;
            }
            int drop = calculateDrop(level, cur, dangerThreshold);
            return drop >= dangerThreshold;
        }
        return false;
    }

    public static boolean isClimbable(@NotNull BlockState state) {
        return state.is(BlockTags.CLIMBABLE)
                || state.getBlock() instanceof LadderBlock
                || state.getBlock() instanceof VineBlock
                || state.getBlock() instanceof ScaffoldingBlock;
    }

    public static boolean isSafeWater(@NotNull FluidState fluid) {
        if (fluid.isEmpty()) return false;
        boolean isWater = fluid.is(FluidTags.WATER) || fluid.getType() == net.minecraft.world.level.material.Fluids.WATER || fluid.getType() == net.minecraft.world.level.material.Fluids.FLOWING_WATER;
        boolean isLava = fluid.is(FluidTags.LAVA) || fluid.getType() == net.minecraft.world.level.material.Fluids.LAVA || fluid.getType() == net.minecraft.world.level.material.Fluids.FLOWING_LAVA;
        return isWater && !isLava;
    }

    public static boolean isSafeLanding(@NotNull BlockGetter level, @NotNull BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        if (isSafeWater(fluid)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.COBWEB)
                || state.getBlock() instanceof HayBlock
                || state.getBlock() instanceof HoneyBlock
                || state.getBlock() instanceof SlimeBlock
                || state.getBlock() instanceof PowderSnowBlock) {
            return true;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !state.isAir() && !shape.isEmpty();
    }

    private static int calculateDrop(@NotNull BlockGetter level, @NotNull BlockPos startPos, int maxScan) {
        BlockPos cur = startPos;
        int depth = 0;
        int limit = Math.max(64, maxScan + 5);

        while (depth < limit) {
            FluidState fluid = level.getFluidState(cur);
            if (isSafeWater(fluid)) {
                return 0; // Safe water landing
            }
            if (fluid.is(FluidTags.LAVA)) {
                // Lava is NEVER safe: treat as lethal depth
                return 999;
            }

            BlockState state = level.getBlockState(cur);
            if (isSafeLanding(level, cur)) {
                return depth;
            }
            depth++;
            cur = cur.below();
        }
        return depth;
    }

    private static SafeDescentType resolveType(BlockState state) {
        if (state.getBlock() instanceof LadderBlock) return SafeDescentType.LADDER;
        if (state.getBlock() instanceof VineBlock) return SafeDescentType.VINE;
        if (state.getBlock() instanceof ScaffoldingBlock) return SafeDescentType.SCAFFOLDING;
        return SafeDescentType.TAGGED_CLIMBABLE;
    }
}
