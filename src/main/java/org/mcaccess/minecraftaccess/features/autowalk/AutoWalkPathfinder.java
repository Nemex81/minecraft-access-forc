package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils;
import org.mcaccess.minecraftaccess.features.point_of_interest.BlockPos3d;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;

public final class AutoWalkPathfinder {
    private static final int MAX_EXPLORED_NODES = 2500;
    private static final double TURN_PENALTY = 0.35;
    private static final double WATER_PENALTY = 1.50;
    private static final double STEP_UP_PENALTY = 0.50;
    private static final double DROP_DOWN_PENALTY = 0.25;

    private AutoWalkPathfinder() {
    }

    public enum PathStatus {
        FOUND,
        NO_PATH,
        OUT_OF_RANGE,
        ALREADY_AT_TARGET
    }

    public record PathResult(
            PathStatus status,
            List<BlockPos> path,
            double totalDistance,
            @Nullable BlockPos targetGoalPos
    ) {
        public static PathResult outOfRange(double dist) {
            return new PathResult(PathStatus.OUT_OF_RANGE, List.of(), dist, null);
        }

        public static PathResult noPath() {
            return new PathResult(PathStatus.NO_PATH, List.of(), 0, null);
        }

        public static PathResult alreadyAtTarget(BlockPos pos) {
            return new PathResult(PathStatus.ALREADY_AT_TARGET, List.of(pos), 0, pos);
        }

        public static PathResult found(List<BlockPos> path, double distance, BlockPos goal) {
            return new PathResult(PathStatus.FOUND, path, distance, goal);
        }
    }

    public record PathNode(
            BlockPos pos,
            double gCost,
            double hCost,
            double fCost,
            @Nullable PathNode parent,
            @Nullable Direction fromDir,
            boolean isWater,
            int verticalDelta // 0 = flat, +1 = step up, -1/-2/-3 = drop down
    ) implements Comparable<PathNode> {
        @Override
        public int compareTo(PathNode o) {
            int cmp = Double.compare(this.fCost, o.fCost);
            if (cmp == 0) {
                return Double.compare(this.hCost, o.hCost);
            }
            return cmp;
        }
    }

    public static PathResult findPath(Level level, Vec3 startVec, Object rawTarget, int maxRange) {
        if (level == null || startVec == null || rawTarget == null) {
            return PathResult.noPath();
        }

        BlockPos startFeet = BlockPos.containing(startVec.x, startVec.y, startVec.z);
        BlockPos rawTargetPos = resolveRawTargetPosition(rawTarget);
        if (rawTargetPos == null) {
            return PathResult.noPath();
        }

        double directDist = Math.sqrt(startFeet.distSqr(rawTargetPos));
        if (directDist > maxRange) {
            return PathResult.outOfRange(directDist);
        }

        // Resolve goal landing position (e.g. adjacent block for solid blocks)
        Set<BlockPos> validGoals = resolveValidGoalPositions(level, rawTarget, rawTargetPos);
        if (validGoals.isEmpty()) {
            return PathResult.noPath();
        }

        if (validGoals.contains(startFeet) || directDist < 1.25) {
            return PathResult.alreadyAtTarget(rawTargetPos);
        }

        return computeAStar(level, startFeet, validGoals, rawTargetPos, maxRange);
    }

    private static BlockPos resolveRawTargetPosition(Object rawTarget) {
        return switch (rawTarget) {
            case BlockPos3d bp3d -> bp3d;
            case BlockPos bp -> bp;
            case Entity entity -> entity.blockPosition();
            case Waypoint wp -> wp.pos();
            default -> null;
        };
    }

    private static Set<BlockPos> resolveValidGoalPositions(Level level, Object rawTarget, BlockPos rawTargetPos) {
        Set<BlockPos> goals = new HashSet<>();

        if (rawTarget instanceof Entity) {
            // For entities, goal can be entity feet or immediately adjacent walkable blocks
            goals.add(rawTargetPos);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = rawTargetPos.relative(dir);
                if (isStandable(level, adj)) {
                    goals.add(adj);
                }
            }
            return goals;
        }

        if (rawTarget instanceof Waypoint) {
            // For waypoints, add direct position and all surrounding standable spots (including sloped approaches)
            goals.add(rawTargetPos);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = rawTargetPos.relative(dir);
                if (isStandable(level, adj)) {
                    goals.add(adj);
                }
                BlockPos aboveAdj = adj.above();
                if (isStandable(level, aboveAdj)) {
                    goals.add(aboveAdj);
                }
                BlockPos belowAdj = adj.below();
                if (isStandable(level, belowAdj)) {
                    goals.add(belowAdj);
                }
            }
            if (isStandable(level, rawTargetPos.above())) {
                goals.add(rawTargetPos.above());
            }
            return goals;
        }

        // For blocks: check if the block itself is non-solid / passable (e.g. flower, fluid, air)
        if (!isSolid(level, rawTargetPos) && isStandable(level, rawTargetPos)) {
            goals.add(rawTargetPos);
            return goals;
        }

        // If target block is solid (chest, furnace, door, ores), check 4 horizontal neighbors + top/bottom
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adj = rawTargetPos.relative(dir);
            if (isStandable(level, adj)) {
                goals.add(adj);
            }
            // Check 1 block above/below adjacent for sloped approach
            BlockPos aboveAdj = adj.above();
            if (isStandable(level, aboveAdj)) {
                goals.add(aboveAdj);
            }
            BlockPos belowAdj = adj.below();
            if (isStandable(level, belowAdj)) {
                goals.add(belowAdj);
            }
        }

        // Check standing directly above the block
        BlockPos above = rawTargetPos.above();
        if (isStandable(level, above)) {
            goals.add(above);
        }

        return goals;
    }

    private static PathResult computeAStar(Level level, BlockPos startPos, Set<BlockPos> validGoals, BlockPos primaryTargetPos, int maxRange) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Map<BlockPos, Double> bestGCost = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        double startH = calculateHeuristic(startPos, primaryTargetPos);
        PathNode startNode = new PathNode(startPos, 0.0, startH, startH, null, null, isInWater(level, startPos), 0);

        openSet.add(startNode);
        bestGCost.put(startPos, 0.0);

        PathNode closestNode = startNode;
        double minH = startH;
        int exploredCount = 0;

        while (!openSet.isEmpty() && exploredCount < MAX_EXPLORED_NODES) {
            PathNode current = openSet.poll();
            exploredCount++;

            if (validGoals.contains(current.pos)) {
                List<BlockPos> path = reconstructPath(current);
                return PathResult.found(path, current.gCost, current.pos);
            }

            closedSet.add(current.pos);

            if (current.hCost < minH) {
                minH = current.hCost;
                closestNode = current;
            }

            for (NeighborMove move : getValidNeighbors(level, current.pos, maxRange, startPos)) {
                if (closedSet.contains(move.targetPos)) continue;

                double moveCost = calculateStepCost(current, move);
                double tentativeG = current.gCost + moveCost;

                if (tentativeG < bestGCost.getOrDefault(move.targetPos, Double.MAX_VALUE)) {
                    bestGCost.put(move.targetPos, tentativeG);
                    double h = calculateHeuristic(move.targetPos, primaryTargetPos);
                    PathNode neighborNode = new PathNode(
                            move.targetPos,
                            tentativeG,
                            h,
                            tentativeG + h,
                            current,
                            move.direction,
                            move.isWater,
                            move.verticalDelta
                    );
                    openSet.add(neighborNode);
                }
            }
        }

        // If exact goal not reached but we explored towards it, return no path
        return PathResult.noPath();
    }

    private record NeighborMove(
            BlockPos targetPos,
            Direction direction,
            int verticalDelta,
            boolean isWater,
            boolean isDiagonal
    ) {
    }

    private static List<NeighborMove> getValidNeighbors(Level level, BlockPos pos, int maxRange, BlockPos origin) {
        List<NeighborMove> neighbors = new ArrayList<>(12);

        // 1. Orthogonal Horizontal Steps (North, South, East, West)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos stepPos = pos.relative(dir);
            if (isWithinBounds(stepPos, origin, maxRange)) {
                checkAndAddMoves(level, pos, stepPos, dir, false, neighbors);
            }
        }

        // 2. Diagonal Steps (NE, NW, SE, SW) with Anti-Snagging checks
        int[][] diagOffsets = {
                {1, 1, 0, 1, 1, 0},   // +X +Z (adjacent: +X+0, +0+Z)
                {1, -1, 0, -1, 1, 0},  // +X -Z (adjacent: +X+0, +0-Z)
                {-1, 1, 0, 1, -1, 0},  // -X +Z (adjacent: -X+0, +0+Z)
                {-1, -1, 0, -1, -1, 0} // -X -Z (adjacent: -X+0, +0-Z)
        };

        for (int[] d : diagOffsets) {
            BlockPos diagPos = pos.offset(d[0], 0, d[1]);
            BlockPos ortho1 = pos.offset(d[2], 0, d[3]);
            BlockPos ortho2 = pos.offset(d[4], 0, d[5]);

            if (isWithinBounds(diagPos, origin, maxRange)) {
                // Anti-snagging: both orthogonal sub-steps must be completely clear of solid blocks at feet and head
                if (isPassable(level, ortho1) && isPassable(level, ortho1.above())
                        && isPassable(level, ortho2) && isPassable(level, ortho2.above())) {
                    checkAndAddMoves(level, pos, diagPos, null, true, neighbors);
                }
            }
        }

        return neighbors;
    }

    private static void checkAndAddMoves(Level level, BlockPos from, BlockPos to, @Nullable Direction dir, boolean isDiag, List<NeighborMove> moves) {
        // A. Flat Move (Same Y)
        if (isStandable(level, to)) {
            moves.add(new NeighborMove(to, dir, 0, isInWater(level, to), isDiag));
            return;
        }

        // B. Step-Up Move (+1 Y)
        BlockPos upPos = to.above(1);
        if (isClimbableStep(level, from, to, upPos)) {
            moves.add(new NeighborMove(upPos, dir, 1, isInWater(level, upPos), isDiag));
            return;
        }

        // C. Descent Moves (-1 Y, -2 Y, -3 Y)
        for (int drop = 1; drop <= 3; drop++) {
            BlockPos dropPos = to.below(drop);
            if (isSafeDescent(level, from, to, dropPos, drop)) {
                moves.add(new NeighborMove(dropPos, dir, -drop, isInWater(level, dropPos), isDiag));
                return;
            }
        }
    }

    public static boolean isStandable(Level level, BlockPos pos) {
        if (level == null || isHazard(level, pos) || isHazard(level, pos.above())) {
            return false;
        }

        // Check if current position is water surface with air above (swimmable)
        if (isInWater(level, pos)) {
            return !isSolid(level, pos.above()) && !isHazard(level, pos.above());
        }

        // Regular ground: feet and head must be passable, block below must be solid
        if (!isPassable(level, pos) || !isPassable(level, pos.above())) {
            return false;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (isHazard(level, below)) return false;

        VoxelShape collision = belowState.getCollisionShape(level, below);
        return !collision.isEmpty();
    }

    public static boolean isPassable(Level level, BlockPos pos) {
        if (level == null || isHazard(level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (isDoorOrGate(state)) return true;
        return state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isDoorOrGate(BlockState state) {
        Block block = state.getBlock();
        return block instanceof DoorBlock
                || block instanceof FenceGateBlock
                || block instanceof TrapDoorBlock;
    }

    public static boolean isClimbableStep(Level level, BlockPos from, BlockPos stepFoot, BlockPos targetStep) {
        if (level == null || isHazard(level, stepFoot) || isHazard(level, targetStep) || isHazard(level, targetStep.above())) {
            return false;
        }

        // Headroom directly above player (from.above(2)) must be completely clear to jump
        if (isSolid(level, from.above(2))) {
            return false;
        }

        // Clearance at the step landing: targetStep (feet) and targetStep.above() (head) and targetStep.above(2) (jump peak headroom)
        if (!isPassable(level, targetStep) || !isPassable(level, targetStep.above())) {
            return false;
        }

        // Step block collision height check
        double height = ObstacleDetectionUtils.getBlockCollisionHeight(level, stepFoot);
        return height > 0.0 && height <= ObstacleDetectionUtils.MAX_JUMPABLE_STEP;
    }

    public static boolean isSafeDescent(Level level, BlockPos from, BlockPos columnAir, BlockPos dropLanding, int dropDepth) {
        if (level == null || isHazard(level, dropLanding) || isHazard(level, dropLanding.above())) {
            return false;
        }

        // The vertical descent column must be clear
        for (int y = 0; y < dropDepth; y++) {
            BlockPos check = columnAir.below(y);
            if (isSolid(level, check)) {
                return false;
            }
        }

        // Landing block space must be standable
        return isStandable(level, dropLanding);
    }

    public static boolean isInWater(Level level, BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        return fluid.is(FluidTags.WATER) || state.is(Blocks.WATER);
    }

    public static boolean isHazard(Level level, BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.LAVA)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW);
    }

    public static boolean isSolid(Level level, BlockPos pos) {
        return ObstacleDetectionUtils.isSolid(level, pos);
    }

    private static boolean isWithinBounds(BlockPos pos, BlockPos origin, int maxRange) {
        int dx = Math.abs(pos.getX() - origin.getX());
        int dz = Math.abs(pos.getZ() - origin.getZ());
        return dx <= maxRange && dz <= maxRange;
    }

    private static double calculateStepCost(PathNode current, NeighborMove move) {
        double dist = move.isDiagonal ? 1.414 : 1.0;

        if (move.verticalDelta > 0) {
            dist += STEP_UP_PENALTY;
        } else if (move.verticalDelta < 0) {
            dist += DROP_DOWN_PENALTY * Math.abs(move.verticalDelta);
        }

        if (move.isWater) {
            dist += WATER_PENALTY;
        }

        // Direction turning penalty to favor straight lines
        if (current.fromDir != null && move.direction != null && current.fromDir != move.direction) {
            dist += TURN_PENALTY;
        }

        return dist;
    }

    private static double calculateHeuristic(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = (a.getY() - b.getY()) * 1.5; // Slight vertical bias
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static List<BlockPos> reconstructPath(PathNode endNode) {
        List<BlockPos> path = new ArrayList<>();
        PathNode curr = endNode;
        while (curr != null) {
            path.add(curr.pos);
            curr = curr.parent;
        }
        Collections.reverse(path);
        return path;
    }
}
