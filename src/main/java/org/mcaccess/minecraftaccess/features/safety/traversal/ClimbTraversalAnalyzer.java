package org.mcaccess.minecraftaccess.features.safety.traversal;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder;
import org.mcaccess.minecraftaccess.features.door.DoorInteractionHelper;

/**
 * Analizzatore geometrico puro e privo di effetti collaterali per colonne arrampicabili (Contratto D0).
 */
public final class ClimbTraversalAnalyzer {

    private static final int MAX_COLUMN_HEIGHT = 64;

    private ClimbTraversalAnalyzer() {
    }

    /**
     * Analizza una colonna arrampicabile a partire da una posizione data (all'interno della colonna o adiacente).
     */
    public static @Nullable ClimbTraversal analyze(
            @NotNull BlockGetter level,
            @NotNull BlockPos originPos,
            @NotNull Direction.AxisDirection direction,
            @Nullable Direction preferredFacing,
            boolean allowClosedDoors
    ) {
        BlockPos climbEntry = findClimbableCell(level, originPos);
        if (climbEntry == null) {
            return null;
        }

        BlockState entryState = level.getBlockState(climbEntry);
        ClimbableGeometry.ClimbType type = ClimbableGeometry.getClimbType(entryState);
        if (type == null) {
            return null;
        }

        // 1. Traccia l'estensione verticale completa della colonna
        BlockPos bottom = climbEntry;
        while (bottom.getY() > level.getMinY() && ClimbableGeometry.isClimbable(level.getBlockState(bottom.below()))) {
            bottom = bottom.below();
        }

        BlockPos top = climbEntry;
        while (top.getY() < level.getMaxY() && ClimbableGeometry.isClimbable(level.getBlockState(top.above()))) {
            top = top.above();
            if (top.getY() - bottom.getY() > MAX_COLUMN_HEIGHT) {
                break;
            }
        }

        // Verifica presenza eventuale botola in cima alla scala
        BlockPos trapdoorPos = null;
        BlockState aboveTopState = level.getBlockState(top.above());
        if (aboveTopState.getBlock() instanceof TrapDoorBlock) {
            if (DoorInteractionHelper.isIronDoorOrTrapdoor(aboveTopState)) {
                // Botola di ferro: non superabile automaticamente
                return null;
            }
            trapdoorPos = top.above();
        }

        Direction wallFacing = ClimbableGeometry.getBestWallSupportFacing(entryState, preferredFacing);

        if (direction == Direction.AxisDirection.POSITIVE) {
            // Salita: ricerca del Top Landing
            BlockPos topLanding = resolveTopLanding(level, top, trapdoorPos, allowClosedDoors);
            if (topLanding == null) {
                return null;
            }
            return ClimbTraversal.of(direction, type, bottom, top, bottom, topLanding, wallFacing, trapdoorPos);
        } else {
            // Discesa: ricerca del Bottom Landing
            BlockPos bottomLanding = resolveBottomLanding(level, bottom, allowClosedDoors);
            if (bottomLanding == null) {
                return null;
            }
            return ClimbTraversal.of(direction, type, top, top, bottom, bottomLanding, wallFacing, trapdoorPos);
        }
    }

    /**
     * Identifica la cella arrampicabile di partenza (diretta o immediatamente adiacente).
     */
    public static @Nullable BlockPos findClimbableCell(@NotNull BlockGetter level, @NotNull BlockPos pos) {
        if (ClimbableGeometry.isClimbable(level.getBlockState(pos))) {
            return pos;
        }
        if (ClimbableGeometry.isClimbable(level.getBlockState(pos.below()))) {
            return pos.below();
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adj = pos.relative(dir);
            if (ClimbableGeometry.isClimbable(level.getBlockState(adj))) {
                return adj;
            }
            if (ClimbableGeometry.isClimbable(level.getBlockState(adj.below()))) {
                return adj.below();
            }
        }
        return null;
    }

    /**
     * Risolve il pianerottolo superiore sicuro (Top Landing).
     */
    public static @Nullable BlockPos resolveTopLanding(
            @NotNull BlockGetter level,
            @NotNull BlockPos topPos,
            @Nullable BlockPos trapdoorPos,
            boolean allowClosedDoors
    ) {
        // Se c'è una botola in cima, il pianerottolo può trovarsi sopra la botola (botola aperta/apribile)
        if (trapdoorPos != null) {
            BlockPos aboveTrapdoor = trapdoorPos.above();
            if (isLevelStandable(level, aboveTrapdoor, allowClosedDoors)) {
                return aboveTrapdoor;
            }
            // Oppure un blocco calpestabile adiacente alla botola
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = trapdoorPos.relative(dir);
                if (isLevelStandable(level, adj, allowClosedDoors)) {
                    return adj;
                }
            }
        }

        // Blocco direttamente sopra la sommità della scala
        BlockPos aboveTop = topPos.above();
        if (isLevelStandable(level, aboveTop, allowClosedDoors)) {
            return aboveTop;
        }

        // Blocchi calpestabili orizzontalmente adiacenti alla sommità o a quota sommità + 1
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adjSame = topPos.relative(dir);
            if (isLevelStandable(level, adjSame, allowClosedDoors)) {
                return adjSame;
            }
            BlockPos adjAbove = adjSame.above();
            if (isLevelStandable(level, adjAbove, allowClosedDoors)) {
                return adjAbove;
            }
        }

        return null;
    }

    /**
     * Risolve il pianerottolo inferiore sicuro (Bottom Landing).
     */
    public static @Nullable BlockPos resolveBottomLanding(
            @NotNull BlockGetter level,
            @NotNull BlockPos bottomPos,
            boolean allowClosedDoors
    ) {
        BlockPos below = bottomPos.below();
        if (isLevelStandable(level, below, allowClosedDoors)) {
            return below;
        }
        if (isLevelStandable(level, bottomPos, allowClosedDoors)) {
            return bottomPos;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adj = bottomPos.relative(dir);
            if (isLevelStandable(level, adj, allowClosedDoors)) {
                return adj;
            }
            BlockPos adjBelow = adj.below();
            if (isLevelStandable(level, adjBelow, allowClosedDoors)) {
                return adjBelow;
            }
        }
        return null;
    }

    private static boolean isLevelStandable(@NotNull BlockGetter level, @NotNull BlockPos pos, boolean allowClosedDoors) {
        if (level instanceof Level lvl) {
            return AutoWalkPathfinder.isStandable(lvl, pos, allowClosedDoors);
        }
        // Fallback per BlockGetter headless in test unitari
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && !ground.getCollisionShape(level, pos.below()).isEmpty();
    }

    /**
     * Risolve l'ingresso in discesa dall'alto (Top Descent Mount) da una superficie stabile (Contratto D1).
     *
     * @param level Livello del mondo.
     * @param surfacePos Posizione calpestabile da cui il giocatore approccia.
     * @param approachDirection Direzione orizzontale cardinale di approccio verso l'apertura.
     * @param allowClosedDoors Se true, autorizza botole lignee chiuse (Pass 2).
     * @return ClimbEntryTransition validata, oppure null se la transizione non e' percorribile in sicurezza.
     */
    public static @Nullable ClimbEntryTransition resolveTopDescentMount(
            @NotNull Level level,
            @NotNull BlockPos surfacePos,
            @NotNull Direction approachDirection,
            boolean allowClosedDoors
    ) {
        if (!approachDirection.getAxis().isHorizontal()) {
            return null;
        }

        // 0. Verifica che surfacePos sia calpestabile e sicuro (Contratto D1)
        if (!AutoWalkPathfinder.isStandable(level, surfacePos, allowClosedDoors)) {
            return null;
        }

        // 1. Apertura e Cima della colonna
        BlockPos aperturePos = surfacePos.relative(approachDirection);
        BlockPos floorLevelPos = aperturePos.below();

        // Limiti del mondo (resiliente a mock non configurati)
        if (level.getMaxY() > level.getMinY()) {
            if (floorLevelPos.getY() - 1 < level.getMinY() || aperturePos.getY() > level.getMaxY()) {
                return null;
            }
        }

        // Verifica assenza pericoli nell'apertura
        if (AutoWalkPathfinder.isHazard(level, aperturePos) || AutoWalkPathfinder.isHazard(level, aperturePos.above())) {
            return null;
        }

        // Verifica clearance di testa sopra l'apertura (aperturePos.above())
        if (!AutoWalkPathfinder.isPassable(level, aperturePos.above(), allowClosedDoors)) {
            return null;
        }

        BlockPos entryPos = floorLevelPos;
        BlockPos trapdoorPos = null;
        ClimbEntryTransition.PassageRequirement requirement = ClimbEntryTransition.PassageRequirement.CLEAR;

        BlockState apertureState = level.getBlockState(aperturePos);
        BlockState floorState = level.getBlockState(floorLevelPos);

        // Verifica apertura e risoluzione entry climbable:
        // Caso A: Botola a quota piedi sopra l'orlo (aperturePos)
        if (apertureState.getBlock() instanceof TrapDoorBlock) {
            trapdoorPos = aperturePos;
            if (DoorInteractionHelper.isIronDoorOrTrapdoor(apertureState)) {
                return null; // Botola di ferro: impercorribile automaticamente
            }
            if (apertureState.getValue(TrapDoorBlock.OPEN)) {
                requirement = ClimbEntryTransition.PassageRequirement.OPEN_TRAPDOOR;
            } else {
                if (!allowClosedDoors) {
                    return null; // Botola chiusa non ammessa in Pass 1
                }
                requirement = ClimbEntryTransition.PassageRequirement.OPENABLE_WOODEN_TRAPDOOR;
            }
        } else {
            // Se non e' botola, aperturePos deve essere passabile normalmente
            if (!AutoWalkPathfinder.isPassable(level, aperturePos, allowClosedDoors)) {
                return null;
            }
        }

        // Caso B: Botola incassata a quota pavimento (floorLevelPos), con scala sottostante
        if (floorState.getBlock() instanceof TrapDoorBlock) {
            trapdoorPos = floorLevelPos;
            if (DoorInteractionHelper.isIronDoorOrTrapdoor(floorState)) {
                return null; // Botola di ferro: impercorribile automaticamente
            }
            if (floorState.getValue(TrapDoorBlock.OPEN)) {
                requirement = ClimbEntryTransition.PassageRequirement.OPEN_TRAPDOOR;
            } else {
                if (!allowClosedDoors) {
                    return null; // Botola chiusa non ammessa in Pass 1
                }
                requirement = ClimbEntryTransition.PassageRequirement.OPENABLE_WOODEN_TRAPDOOR;
            }
            entryPos = floorLevelPos.below();
        } else {
            entryPos = floorLevelPos;
        }

        // 2. La cella entryPos deve essere arrampicabile
        BlockState entryState = level.getBlockState(entryPos);
        ClimbableGeometry.ClimbType climbType = ClimbableGeometry.getClimbType(entryState);
        if (climbType == null) {
            return null;
        }

        // Deve essere la sommità della colonna arrampicabile (non un piolo intermedio con sopra un'altra scala)
        if (ClimbableGeometry.isClimbable(level.getBlockState(entryPos.above()))) {
            return null;
        }

        // 3. La cella della scala (entryPos) deve essere passabile per il corpo del giocatore
        if (!AutoWalkPathfinder.isPassable(level, entryPos, allowClosedDoors)) {
            return null;
        }

        // 4. Analizza la colonna completa in discesa a partire da entryPos
        ClimbTraversal traversal = analyze(level, entryPos, Direction.AxisDirection.NEGATIVE, approachDirection, allowClosedDoors);
        if (traversal == null) {
            return null;
        }

        // Verifica che la colonna trovata abbia la cima esattamente a entryPos
        if (!traversal.columnTopPos().equals(entryPos)) {
            return null;
        }

        return ClimbEntryTransition.ofTopDescent(
                surfacePos,
                aperturePos,
                entryPos,
                approachDirection,
                traversal,
                requirement,
                trapdoorPos
        );
    }
}
