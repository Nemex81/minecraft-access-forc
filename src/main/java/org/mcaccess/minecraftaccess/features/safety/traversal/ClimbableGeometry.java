package org.mcaccess.minecraftaccess.features.safety.traversal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Vocabolario geometrico e classificazione pura dei blocchi arrampicabili (Contratto D0).
 */
public final class ClimbableGeometry {

    public enum ClimbType {
        WALL_MOUNTED,
        SCAFFOLDING,
        FREE_CLIMBABLE
    }

    private ClimbableGeometry() {
    }

    /**
     * Determina se lo stato del blocco è arrampicabile e ne restituisce la classificazione cinematica.
     */
    public static @Nullable ClimbType getClimbType(@Nullable BlockState state) {
        if (state == null || state.isAir()) {
            return null;
        }
        if (state.getBlock() instanceof LadderBlock || state.getBlock() instanceof VineBlock) {
            return ClimbType.WALL_MOUNTED;
        }
        if (state.getBlock() instanceof ScaffoldingBlock) {
            return ClimbType.SCAFFOLDING;
        }
        if (state.is(BlockTags.CLIMBABLE)) {
            return ClimbType.FREE_CLIMBABLE;
        }
        return null;
    }

    /**
     * Verifica rapida se lo stato del blocco è arrampicabile.
     */
    public static boolean isClimbable(@Nullable BlockState state) {
        return getClimbType(state) != null;
    }

    /**
     * Risolve le direzioni della parete di supporto a cui il blocco è ancorato.
     * Per LadderBlock restituisce il facing opposto alla proprietà FACING (la parete su cui poggia).
     * Per VineBlock valuta le facce orizzontali attive.
     * Per Scaffolding o FreeClimbable restituisce una lista vuota.
     */
    public static @NotNull List<Direction> getWallSupportFacings(@Nullable BlockState state) {
        if (state == null) {
            return List.of();
        }
        if (state.getBlock() instanceof LadderBlock) {
            if (state.hasProperty(LadderBlock.FACING)) {
                Direction facing = state.getValue(LadderBlock.FACING);
                // Il blocco scala 'guarda' verso facing; la parete di supporto è nel verso opposto
                return List.of(facing.getOpposite());
            }
        } else if (state.getBlock() instanceof VineBlock) {
            List<Direction> facings = new ArrayList<>(4);
            if (state.hasProperty(VineBlock.NORTH) && state.getValue(VineBlock.NORTH)) facings.add(Direction.NORTH);
            if (state.hasProperty(VineBlock.SOUTH) && state.getValue(VineBlock.SOUTH)) facings.add(Direction.SOUTH);
            if (state.hasProperty(VineBlock.EAST) && state.getValue(VineBlock.EAST)) facings.add(Direction.EAST);
            if (state.hasProperty(VineBlock.WEST) && state.getValue(VineBlock.WEST)) facings.add(Direction.WEST);
            return facings;
        }
        return List.of();
    }

    /**
     * Seleziona la faccia di supporto più vicina alla direzione di approccio o sguardo del giocatore.
     */
    public static @Nullable Direction getBestWallSupportFacing(@Nullable BlockState state, @Nullable Direction preferredDirection) {
        List<Direction> facings = getWallSupportFacings(state);
        if (facings.isEmpty()) {
            return null;
        }
        if (preferredDirection != null && facings.contains(preferredDirection)) {
            return preferredDirection;
        }
        return facings.get(0);
    }

}
