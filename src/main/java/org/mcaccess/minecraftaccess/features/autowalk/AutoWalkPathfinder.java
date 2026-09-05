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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils;
import org.mcaccess.minecraftaccess.features.point_of_interest.BlockPos3d;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;

public final class AutoWalkPathfinder {
    public static final int MAX_EXPLORED_NODES = 2500;
    private static final double TURN_PENALTY = 0.35;
    private static final double WATER_PENALTY = 1.50;
    private static final double STEP_UP_PENALTY = 0.50;
    private static final double DROP_DOWN_PENALTY = 0.25;
    public static final double CLOSED_DOOR_PENALTY = 30.0;

    private AutoWalkPathfinder() {
    }

    public enum PathStatus {
        FOUND,
        NO_PATH,
        OUT_OF_RANGE,
        ALREADY_AT_TARGET,
        SEARCH_BUDGET_EXHAUSTED
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

        public static PathResult searchBudgetExhausted() {
            return new PathResult(PathStatus.SEARCH_BUDGET_EXHAUSTED, List.of(), 0, null);
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
        return findPath(level, startVec, rawTarget, maxRange, MAX_EXPLORED_NODES);
    }

    // Package-private per consentire ai test unitari di verificare l'esaurimento del budget in modo deterministico
    static PathResult findPath(Level level, Vec3 startVec, Object rawTarget, int maxRange, int maxExploredNodes) {
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

        // Two-Pass Pathfinding:
        // Passaggio 1: Strict Path (allowClosedDoors = false)
        Set<BlockPos> validGoalsStrict = resolveValidGoalPositions(level, rawTarget, rawTargetPos, false);
        if ((validGoalsStrict.contains(startFeet) || directDist < 1.25) && hasDirectClearPath(level, startVec, rawTargetPos)) {
            return PathResult.alreadyAtTarget(rawTargetPos);
        }

        if (!validGoalsStrict.isEmpty()) {
            PathResult strictResult = computeAStar(level, startVec, startFeet, validGoalsStrict, rawTargetPos, maxRange, false, maxExploredNodes);
            if (strictResult.status() == PathStatus.FOUND || strictResult.status() == PathStatus.ALREADY_AT_TARGET) {
                return strictResult;
            }
            if (strictResult.status() == PathStatus.SEARCH_BUDGET_EXHAUSTED) {
                // Politica di protezione: non dichiarare la porta inevitabile su budget esaurito
                return strictResult;
            }
        }

        // Passaggio 2: Fallback Path (allowClosedDoors = true con CLOSED_DOOR_PENALTY = 30.0)
        // Eseguito ESCLUSIVAMENTE se il Passaggio 1 restituisce un reale NO_PATH
        Set<BlockPos> validGoalsFallback = resolveValidGoalPositions(level, rawTarget, rawTargetPos, true);
        if (validGoalsFallback.isEmpty()) {
            return PathResult.noPath();
        }

        if (validGoalsFallback.contains(startFeet) && hasDirectClearPath(level, startVec, rawTargetPos)) {
            return PathResult.alreadyAtTarget(rawTargetPos);
        }

        return computeAStar(level, startVec, startFeet, validGoalsFallback, rawTargetPos, maxRange, true, maxExploredNodes);
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

    public static Set<BlockPos> resolveValidGoalPositions(Level level, Object rawTarget, BlockPos rawTargetPos) {
        return resolveValidGoalPositions(level, rawTarget, rawTargetPos, true);
    }

    public static Set<BlockPos> resolveValidGoalPositions(Level level, Object rawTarget, BlockPos rawTargetPos, boolean allowClosedDoors) {
        Set<BlockPos> goals = new HashSet<>();

        if (rawTarget instanceof Entity) {
            // For entities, goal can be entity feet or immediately adjacent walkable blocks
            goals.add(rawTargetPos);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = rawTargetPos.relative(dir);
                if (isStandable(level, adj, allowClosedDoors)) {
                    goals.add(adj);
                }
            }
            return goals;
        }

        if (rawTarget instanceof Waypoint) {
            // For waypoints, add direct position and all surrounding standable spots (including sloped approaches)
            if (isStandable(level, rawTargetPos, allowClosedDoors)) {
                goals.add(rawTargetPos);
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = rawTargetPos.relative(dir);
                if (isStandable(level, adj, allowClosedDoors)) {
                    goals.add(adj);
                }
                BlockPos aboveAdj = adj.above();
                if (isStandable(level, aboveAdj, allowClosedDoors)) {
                    goals.add(aboveAdj);
                }
                BlockPos belowAdj = adj.below();
                if (isStandable(level, belowAdj, allowClosedDoors)) {
                    goals.add(belowAdj);
                }
            }
            if (isStandable(level, rawTargetPos.above(), allowClosedDoors)) {
                goals.add(rawTargetPos.above());
            }
            return goals;
        }

        // For blocks: check if the block itself is non-solid / passable (e.g. flower, fluid, air)
        if (!isSolid(level, rawTargetPos) && isStandable(level, rawTargetPos, allowClosedDoors)) {
            goals.add(rawTargetPos);
            return goals;
        }

        // If target block is solid (chest, furnace, door, ores), check 4 horizontal neighbors + top/bottom
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adj = rawTargetPos.relative(dir);
            if (isStandable(level, adj, allowClosedDoors)) {
                goals.add(adj);
            }
            // Check 1 block above/below adjacent for sloped approach
            BlockPos aboveAdj = adj.above();
            if (isStandable(level, aboveAdj, allowClosedDoors)) {
                goals.add(aboveAdj);
            }
            BlockPos belowAdj = adj.below();
            if (isStandable(level, belowAdj, allowClosedDoors)) {
                goals.add(belowAdj);
            }
        }

        // Check standing directly above the block
        BlockPos above = rawTargetPos.above();
        if (isStandable(level, above, allowClosedDoors)) {
            goals.add(above);
        }

        return goals;
    }

    private static PathResult computeAStar(
            Level level,
            Vec3 startVec,
            BlockPos startPos,
            Set<BlockPos> validGoals,
            BlockPos primaryTargetPos,
            int maxRange,
            boolean allowClosedDoors,
            int maxExploredNodes
    ) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Map<BlockPos, Double> bestGCost = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        double startH = calculateHeuristic(startPos, primaryTargetPos);
        PathNode startNode = new PathNode(startPos, 0.0, startH, startH, null, null, isInWater(level, startPos), 0);

        openSet.add(startNode);
        bestGCost.put(startPos, 0.0);

        int exploredCount = 0;

        while (!openSet.isEmpty() && exploredCount < maxExploredNodes) {
            PathNode current = openSet.poll();
            exploredCount++;

            if (validGoals.contains(current.pos)) {
                List<BlockPos> path = reconstructPath(current);
                return PathResult.found(path, current.gCost, current.pos);
            }

            closedSet.add(current.pos);

            boolean isRootNode = (current.parent == null);

            for (NeighborMove move : getValidNeighbors(level, startVec, current.pos, maxRange, startPos, allowClosedDoors, isRootNode)) {
                if (closedSet.contains(move.targetPos)) continue;

                double moveCost = calculateStepCost(level, startVec, current, move, allowClosedDoors);
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

        if (!openSet.isEmpty() && exploredCount >= maxExploredNodes) {
            return PathResult.searchBudgetExhausted();
        }

        return PathResult.noPath();
    }

    record NeighborMove(
            BlockPos targetPos,
            Direction direction,
            int verticalDelta,
            boolean isWater,
            boolean isDiagonal
    ) {
    }

    static List<NeighborMove> getValidNeighbors(
            Level level,
            Vec3 startVec,
            BlockPos pos,
            int maxRange,
            BlockPos origin,
            boolean allowClosedDoors,
            boolean isRootNode
    ) {
        List<NeighborMove> neighbors = new ArrayList<>(12);

        // 1. Orthogonal Horizontal Steps (North, South, East, West)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos stepPos = pos.relative(dir);
            if (isWithinBounds(stepPos, origin, maxRange)) {
                checkAndAddMoves(level, startVec, pos, stepPos, dir, false, neighbors, allowClosedDoors, isRootNode, null, null);
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
                // Inviolabilità delle diagonali: entrambi i corridoi ortogonali intermedi devono essere
                // rigorosamente passabili senza varchi chiusi a quota piedi e testa (allowClosedDoors = false)
                if (hasStrictDiagonalIntermediateClearance(level, ortho1, ortho2, false)) {
                    checkAndAddMoves(level, startVec, pos, diagPos, null, true, neighbors, allowClosedDoors, isRootNode, ortho1, ortho2);
                }
            }
        }

        return neighbors;
    }

    private static void checkAndAddMoves(
            Level level,
            Vec3 startVec,
            BlockPos from,
            BlockPos to,
            @Nullable Direction dir,
            boolean isDiag,
            List<NeighborMove> moves,
            boolean allowClosedDoors,
            boolean isRootNode,
            @Nullable BlockPos ortho1,
            @Nullable BlockPos ortho2
    ) {
        // A. Flat Move (Same Y)
        if (isStandable(level, to, allowClosedDoors)) {
            NeighborMove move = new NeighborMove(to, dir, 0, isInWater(level, to), isDiag);
            if (!isRootNode || allowClosedDoors || getRootMoveIntersectedClosedDoor(level, startVec, from, move) == null) {
                moves.add(move);
            }
            return;
        }

        // B. Step-Up Move (+1 Y)
        BlockPos upPos = to.above(1);
        if (isClimbableStep(level, from, to, upPos, allowClosedDoors)) {
            // Per salita diagonale, verificare che i due corridoi intermedi ortogonali abbiano clearance al culmine del salto
            if (!isDiag || (ortho1 != null && ortho2 != null && hasStrictDiagonalIntermediateClearance(level, ortho1, ortho2, true))) {
                NeighborMove move = new NeighborMove(upPos, dir, 1, isInWater(level, upPos), isDiag);
                if (!isRootNode || allowClosedDoors || getRootMoveIntersectedClosedDoor(level, startVec, from, move) == null) {
                    moves.add(move);
                }
                return;
            }
        }

        // C. Descent Moves (-1 Y, -2 Y, -3 Y)
        for (int drop = 1; drop <= 3; drop++) {
            BlockPos dropPos = to.below(drop);
            if (!isLateralStairDrop(level, from, to, drop) && isSafeDescent(level, from, to, dropPos, drop, allowClosedDoors)) {
                NeighborMove move = new NeighborMove(dropPos, dir, -drop, isInWater(level, dropPos), isDiag);
                if (!isRootNode || allowClosedDoors || getRootMoveIntersectedClosedDoor(level, startVec, from, move) == null) {
                    moves.add(move);
                }
                return;
            }
        }
    }

    public static boolean isStandable(Level level, BlockPos pos) {
        return isStandable(level, pos, true);
    }

    public static boolean isStandable(Level level, BlockPos pos, boolean allowClosedDoors) {
        if (level == null || isHazard(level, pos) || isHazard(level, pos.above())) {
            return false;
        }

        // Check if current position is water surface with air above (swimmable)
        if (isInWater(level, pos)) {
            return !isSolid(level, pos.above()) && !isHazard(level, pos.above());
        }

        // Regular ground: feet and head must be passable under current door policy, block below must be solid
        if (!isPassable(level, pos, allowClosedDoors) || !isPassable(level, pos.above(), allowClosedDoors)) {
            return false;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (isHazard(level, below)) return false;

        VoxelShape collision = belowState.getCollisionShape(level, below);
        return !collision.isEmpty();
    }

    public static boolean isPassable(Level level, BlockPos pos) {
        return isPassable(level, pos, true);
    }

    public static boolean isPassable(Level level, BlockPos pos, boolean allowClosedDoors) {
        if (level == null || isHazard(level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (isDoorOrGate(state)) {
            if (allowClosedDoors) {
                return true;
            }
            return !isDoorOrGateClosed(level, pos);
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isDoorOrGate(BlockState state) {
        Block block = state.getBlock();
        return block instanceof DoorBlock
                || block instanceof FenceGateBlock
                || block instanceof TrapDoorBlock;
    }

    public static boolean isDoorOrGateClosed(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        if (block instanceof DoorBlock) {
            return !blockState.getValue(DoorBlock.OPEN);
        }
        if (block instanceof FenceGateBlock) {
            return !blockState.getValue(FenceGateBlock.OPEN);
        }
        if (block instanceof TrapDoorBlock) {
            return !blockState.getValue(TrapDoorBlock.OPEN);
        }
        return false;
    }

    /**
     * Restituisce la posizione canonica del varco interattivo.
     * Per le porte a due blocchi (DoorBlock), la coordinata viene normalizzata
     * alla metà inferiore (LOWER). Per cancelli e botole, restituisce la posizione stessa.
     */
    public static BlockPos getCanonicalDoorPos(Level level, BlockPos pos) {
        if (level == null || pos == null) return pos;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock) {
            if (state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                return pos.below();
            }
        }
        return pos;
    }

    /**
     * Determina se entrambi i corridoi ortogonali intermedi di una mossa diagonale
     * sono rigorosamente liberi (senza varchi chiusi a quota piedi e testa),
     * e per salite verifica anche la clearance al culmine del salto.
     * È indipendente dalla politica allowClosedDoors: varchi chiusi laterali invalidano sempre la diagonale.
     */
    public static boolean hasStrictDiagonalIntermediateClearance(Level level, BlockPos ortho1, BlockPos ortho2, boolean isStepUp) {
        if (level == null || ortho1 == null || ortho2 == null) return false;

        // Entrambi i corridoi ortogonali intermedi devono essere rigorosamente passabili (allowClosedDoors = false)
        // a quota piedi e testa
        if (!isPassable(level, ortho1, false) || !isPassable(level, ortho1.above(), false)) {
            return false;
        }
        if (!isPassable(level, ortho2, false) || !isPassable(level, ortho2.above(), false)) {
            return false;
        }

        // Se la mossa comporta un dislivello in salita, verificare anche la clearance al culmine del salto
        if (isStepUp) {
            if (!isClearHeadroom(level, ortho1.above(2)) || !isClearHeadroom(level, ortho2.above(2))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica la traversabilità diretta in linea retta tra startVec e targetPos,
     * assicurando che non vi siano varchi chiusi o blocchi solidi che ostruiscano il passaggio.
     */
    public static boolean hasDirectClearPath(Level level, Vec3 from, BlockPos targetPos) {
        if (level == null || from == null || targetPos == null) return false;
        BlockPos fromPos = BlockPos.containing(from.x, from.y, from.z);
        if (fromPos.equals(targetPos)) {
            return true;
        }

        Vec3 targetCenter = Vec3.atBottomCenterOf(targetPos);
        Vec3 dir = targetCenter.subtract(from);
        double dist = dir.length();
        if (dist < 1e-4) return true;

        int samples = Math.max(2, (int) Math.ceil(dist / 0.20));
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            Vec3 sample = from.add(dir.scale(t));
            BlockPos sampleFeet = BlockPos.containing(sample.x, sample.y, sample.z);
            BlockPos sampleHead = sampleFeet.above();

            if (isDoorOrGateClosed(level, sampleFeet) || isDoorOrGateClosed(level, sampleHead)) {
                return false;
            }
            if (isSolid(level, sampleFeet) || isSolid(level, sampleHead)) {
                return false;
            }
        }
        return true;
    }

    public record ClearanceResult(
            ClearanceStatus status,
            @Nullable BlockPos blockingDoorPos
    ) {
        public enum ClearanceStatus {
            CLEAR,
            BLOCKED_BY_SOLID_JAMB,
            BLOCKED_BY_CLOSED_DOOR
        }

        public static ClearanceResult clear() {
            return new ClearanceResult(ClearanceStatus.CLEAR, null);
        }

        public static ClearanceResult blockedByJamb() {
            return new ClearanceResult(ClearanceStatus.BLOCKED_BY_SOLID_JAMB, null);
        }

        public static ClearanceResult blockedByDoor(BlockPos canonicalDoorPos) {
            return new ClearanceResult(ClearanceStatus.BLOCKED_BY_CLOSED_DOOR, canonicalDoorPos);
        }
    }

    /**
     * Esegue il controllo geometrico puro del volume spazzato dalla bounding box del giocatore (0.6 x 1.8 m)
     * lungo il segmento tra from e to.
     * Restituisce CLEAR se il volume è libero, BLOCKED_BY_CLOSED_DOOR con la posizione canonica se interseca
     * un varco chiuso (C3), oppure BLOCKED_BY_SOLID_JAMB se ostruito da uno stipite o blocco solido (C4).
     */
    public static ClearanceResult checkLocalClearance(Level level, Vec3 from, Vec3 to, @Nullable AABB playerBox) {
        if (level == null || from == null || to == null) {
            return ClearanceResult.clear();
        }

        Vec3 dir = to.subtract(from);
        double dist = dir.length();
        if (dist < 1e-4) {
            return ClearanceResult.clear();
        }

        double width = playerBox != null ? (playerBox.getXsize() / 2.0) : 0.3;
        double height = playerBox != null ? playerBox.getYsize() : 1.8;

        double stepSize = 0.08;
        int samples = Math.max(1, (int) Math.ceil(dist / stepSize));

        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            Vec3 sample = from.add(dir.scale(t));

            AABB boxAtSample = new AABB(
                    sample.x - width, sample.y, sample.z - width,
                    sample.x + width, sample.y + height, sample.z + width
            );

            int minX = Mth.floor(boxAtSample.minX);
            int maxX = Mth.floor(boxAtSample.maxX);
            int minY = Mth.floor(boxAtSample.minY);
            int maxY = Mth.floor(boxAtSample.maxY);
            int minZ = Mth.floor(boxAtSample.minZ);
            int maxZ = Mth.floor(boxAtSample.maxZ);

            // Prima passata: priorità varchi chiusi (Contratto C3)
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos checkPos = new BlockPos(x, y, z);
                        if (isDoorOrGateClosed(level, checkPos)) {
                            BlockState state = level.getBlockState(checkPos);
                            VoxelShape shape = state.getCollisionShape(level, checkPos);
                            if (!shape.isEmpty()) {
                                for (AABB localBox : shape.toAabbs()) {
                                    AABB worldBox = localBox.move(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                                    if (boxAtSample.intersects(worldBox)) {
                                        return ClearanceResult.blockedByDoor(getCanonicalDoorPos(level, checkPos));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Seconda passata: blocchi solidi impassabili (stipiti/muri - Contratto C4)
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos checkPos = new BlockPos(x, y, z);
                        if (!isDoorOrGateClosed(level, checkPos) && isSolid(level, checkPos)) {
                            BlockState state = level.getBlockState(checkPos);
                            VoxelShape shape = state.getCollisionShape(level, checkPos);
                            if (!shape.isEmpty()) {
                                for (AABB localBox : shape.toAabbs()) {
                                    AABB worldBox = localBox.move(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                                    if (boxAtSample.intersects(worldBox)) {
                                        return ClearanceResult.blockedByJamb();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return ClearanceResult.clear();
    }

    public static @Nullable BlockPos getRootMoveIntersectedClosedDoor(
            Level level,
            Vec3 startVec,
            BlockPos rootFeet,
            BlockPos targetPos
    ) {
        if (targetPos == null) return null;
        Vec3 targetCenter = new Vec3(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        return getRootMoveIntersectedClosedDoor(level, startVec, rootFeet, targetCenter);
    }

    public static @Nullable BlockPos getRootMoveIntersectedClosedDoor(
            Level level,
            Vec3 startVec,
            BlockPos rootFeet,
            NeighborMove move
    ) {
        if (move == null) return null;
        Vec3 targetCenter = new Vec3(
                move.targetPos().getX() + 0.5,
                move.targetPos().getY(),
                move.targetPos().getZ() + 0.5
        );
        return getRootMoveIntersectedClosedDoor(level, startVec, rootFeet, targetCenter);
    }

    /**
     * Valuta se la traiettoria fisica continua dal punto reale di avvio (startVec)
     * verso il centro del nodo candidato interseca la forma di collisione (VoxelShape)
     * di un varco chiuso (porta, cancello o botola) presente alla radice (a quota piedi o testa).
     *
     * Se viene rilevata un'intersezione fisica tra l'hitbox del giocatore (0.6 x 1.8 m)
     * e il varco chiuso, restituisce la BlockPos canonica (getCanonicalDoorPos) del varco intersecato.
     * In assenza di collisione (es. allontanamento, movimento parallelo, spazio libero), restituisce null.
     */
    public static @Nullable BlockPos getRootMoveIntersectedClosedDoor(
            Level level,
            Vec3 startVec,
            BlockPos rootFeet,
            Vec3 targetCenter
    ) {
        if (level == null || startVec == null || rootFeet == null || targetCenter == null) {
            return null;
        }

        BlockPos feetDoorPos = isDoorOrGateClosed(level, rootFeet) ? rootFeet : null;
        BlockPos headDoorPos = isDoorOrGateClosed(level, rootFeet.above()) ? rootFeet.above() : null;

        if (feetDoorPos == null && headDoorPos == null) {
            return null;
        }

        Vec3 dirVec = targetCenter.subtract(startVec);
        double length = dirVec.length();
        if (length < 1e-4) {
            return null;
        }

        double stepSize = 0.08;
        int samples = Math.max(1, (int) Math.ceil(length / stepSize));

        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            Vec3 samplePos = startVec.add(dirVec.scale(t));

            AABB playerBox = new AABB(
                    samplePos.x - 0.3, samplePos.y, samplePos.z - 0.3,
                    samplePos.x + 0.3, samplePos.y + 1.8, samplePos.z + 0.3
            );

            if (feetDoorPos != null) {
                BlockState state = level.getBlockState(feetDoorPos);
                VoxelShape shape = state.getCollisionShape(level, feetDoorPos);
                if (!shape.isEmpty()) {
                    for (AABB localBox : shape.toAabbs()) {
                        AABB worldBox = localBox.move(feetDoorPos.getX(), feetDoorPos.getY(), feetDoorPos.getZ());
                        if (playerBox.intersects(worldBox)) {
                            return getCanonicalDoorPos(level, feetDoorPos);
                        }
                    }
                }
            }

            if (headDoorPos != null) {
                BlockState state = level.getBlockState(headDoorPos);
                VoxelShape shape = state.getCollisionShape(level, headDoorPos);
                if (!shape.isEmpty()) {
                    for (AABB localBox : shape.toAabbs()) {
                        AABB worldBox = localBox.move(headDoorPos.getX(), headDoorPos.getY(), headDoorPos.getZ());
                        if (playerBox.intersects(worldBox)) {
                            return getCanonicalDoorPos(level, headDoorPos);
                        }
                    }
                }
            }
        }

        return null;
    }

    public static boolean isClimbableStep(Level level, BlockPos from, BlockPos stepFoot, BlockPos targetStep) {
        return isClimbableStep(level, from, stepFoot, targetStep, true);
    }

    public static boolean isClimbableStep(Level level, BlockPos from, BlockPos stepFoot, BlockPos targetStep, boolean allowClosedDoors) {
        if (level == null || isHazard(level, stepFoot) || isHazard(level, targetStep) || isHazard(level, targetStep.above())) {
            return false;
        }

        // 3-Volume Jump Arc Clearance
        if (!hasJumpArcClearance(level, from, targetStep)) {
            return false;
        }

        // Clearance at step landing: targetStep (feet) and targetStep.above() (head) under current door policy
        if (!isPassable(level, targetStep, allowClosedDoors) || !isPassable(level, targetStep.above(), allowClosedDoors)) {
            return false;
        }

        // Step block collision height check
        double height = ObstacleDetectionUtils.getBlockCollisionHeight(level, stepFoot);
        return height > 0.0 && height <= ObstacleDetectionUtils.MAX_JUMPABLE_STEP;
    }

    public static boolean hasJumpArcClearance(Level level, BlockPos from, BlockPos targetStep) {
        if (level == null) return false;

        // Headroom directly above player at takeoff point (from.above(2))
        if (!isClearHeadroom(level, from.above(2))) {
            return false;
        }

        // 3 consecutive vertical volumes starting from landing step foot:
        // 1. targetStep (feet)
        // 2. targetStep.above() (head/body)
        // 3. targetStep.above(2) (jump peak headroom)
        if (!isClearHeadroom(level, targetStep)
                || !isClearHeadroom(level, targetStep.above())
                || !isClearHeadroom(level, targetStep.above(2))) {
            return false;
        }

        return true;
    }

    public static boolean isClearHeadroom(Level level, BlockPos pos) {
        if (level == null || isHazard(level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isSafeDescent(Level level, BlockPos from, BlockPos columnAir, BlockPos dropLanding, int dropDepth) {
        return isSafeDescent(level, from, columnAir, dropLanding, dropDepth, true);
    }

    public static boolean isSafeDescent(Level level, BlockPos from, BlockPos columnAir, BlockPos dropLanding, int dropDepth, boolean allowClosedDoors) {
        if (level == null || isHazard(level, dropLanding) || isHazard(level, dropLanding.above())) {
            return false;
        }

        // Contratto S1: Headroom check at step-off: player's head passes through columnAir.above()
        if (!isPassable(level, columnAir.above(), allowClosedDoors)) {
            return false;
        }

        // The vertical descent column must be clear
        for (int y = 0; y < dropDepth; y++) {
            BlockPos check = columnAir.below(y);
            if (isSolid(level, check) || (!allowClosedDoors && isDoorOrGateClosed(level, check))) {
                return false;
            }
        }

        // Landing block space must be standable
        return isStandable(level, dropLanding, allowClosedDoors);
    }

    /**
     * Contratto S2 (Stair Flight Constraint):
     * Determina se una mossa con dislivello verso il basso (drop >= 1) da una cella
     * su gradino di scale devia lateralmente o diagonalmente rispetto alla direzione naturale
     * di discesa della scala.
     */
    public static boolean isLateralStairDrop(Level level, BlockPos from, BlockPos to, int drop) {
        if (level == null || from == null || to == null || drop <= 0) return false;
        BlockState belowFrom = level.getBlockState(from.below());
        BlockState stairState = null;
        if (belowFrom.getBlock() instanceof StairBlock) {
            stairState = belowFrom;
        } else {
            BlockState fromState = level.getBlockState(from);
            if (fromState.getBlock() instanceof StairBlock) {
                stairState = fromState;
            }
        }
        if (stairState == null) return false;

        Direction facing = stairState.getValue(StairBlock.FACING);
        Direction descDir = facing.getOpposite();

        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();

        boolean isLongitudinal = (dx == descDir.getStepX() && dz == descDir.getStepZ());
        return !isLongitudinal;
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

    static double calculateStepCost(Level level, PathNode current, NeighborMove move, boolean allowClosedDoors) {
        Vec3 defaultStart = new Vec3(current.pos.getX() + 0.5, current.pos.getY(), current.pos.getZ() + 0.5);
        return calculateStepCost(level, defaultStart, current, move, allowClosedDoors);
    }

    static double calculateStepCost(Level level, Vec3 startVec, PathNode current, NeighborMove move, boolean allowClosedDoors) {
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

        // Penalità per passaggio attraverso blocco interattivo chiuso (porta, cancello o botola)
        if (allowClosedDoors) {
            BlockPos enteredDoor = null;
            if (isDoorOrGateClosed(level, move.targetPos)) {
                enteredDoor = getCanonicalDoorPos(level, move.targetPos);
            } else if (isDoorOrGateClosed(level, move.targetPos.above())) {
                enteredDoor = getCanonicalDoorPos(level, move.targetPos.above());
            }

            if (enteredDoor != null) {
                dist += CLOSED_DOOR_PENALTY;
            }

            // Per il solo nodo radice di partenza: se il raggio fisico continuo dal punto reale
            // collide con un varco chiuso presente alla radice, applica la penalità iniziale.
            // Se exitedDoor coincide con enteredDoor (stesso varco canonico), la penalità non viene duplicata.
            if (current.parent == null) {
                BlockPos exitedDoor = getRootMoveIntersectedClosedDoor(level, startVec, current.pos, move);
                if (exitedDoor != null && !exitedDoor.equals(enteredDoor)) {
                    dist += CLOSED_DOOR_PENALTY;
                }
            }
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
