package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClimbContactProbeTest {

    private static final BlockPos ENTRY = new BlockPos(-60, 84, -42);
    private static final BlockPos SURFACE = new BlockPos(-60, 85, -41);
    private static final BlockPos APERTURE = new BlockPos(-60, 85, -42);

    private static ClimbTraversal traversal() {
        return ClimbTraversal.of(Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                ENTRY, ENTRY, new BlockPos(-60, 82, -42),
                new BlockPos(-60, 81, -42), Direction.SOUTH, null);
    }

    private static ClimbEntryTransition transition(ClimbTraversal traversal) {
        return ClimbEntryTransition.ofTopDescent(SURFACE, APERTURE, ENTRY,
                Direction.NORTH, traversal,
                ClimbEntryTransition.PassageRequirement.CLEAR, null);
    }

    @Test
    @DisplayName("D23: usa la shape reale orientata senza doppia espansione")
    void currentAabbIntersectsActualNorthFacingLadderShape() {
        ClimbTraversal traversal = traversal();
        VoxelShape northFacingLadder = Shapes.box(0.0, 0.0, 0.8125, 1.0, 1.0, 1.0);
        AABB previous = new AABB(-59.8, 85.0, -40.8, -59.2, 86.8, -40.2);
        AABB current = new AABB(-59.8, 84.80, -41.25, -59.2, 86.60, -40.65);

        ClimbContactProbe.Result result = ClimbContactProbe.evaluate(
                current, previous, northFacingLadder, ENTRY,
                transition(traversal), traversal);

        assertEquals(ClimbContactProbe.ContactState.COMMITTED_INSIDE, result.state());
        assertTrue(result.currentIntersection());
        assertTrue(result.sweptIntersection());
        assertTrue(result.signedEntryPlaneDistance() < 0.0);
    }

    @Test
    @DisplayName("D23/D24: ingresso orizzontale arma CAPTURE_WAIT senza certificare contatto")
    void captureBandIsNotClimbContact() {
        ClimbTraversal traversal = traversal();
        VoxelShape northFacingLadder = Shapes.box(0.0, 0.0, 0.8125, 1.0, 1.0, 1.0);
        AABB previous = new AABB(-59.8, 85.0, -41.0, -59.2, 86.8, -40.4);
        AABB current = new AABB(-59.8, 85.0, -41.85, -59.2, 86.8, -41.25);

        ClimbContactProbe.Result result = ClimbContactProbe.evaluate(
                current, previous, northFacingLadder, ENTRY,
                transition(traversal), traversal);

        assertEquals(ClimbContactProbe.ContactState.APPROACHING, result.state());
        assertTrue(result.apertureCapture());
        assertFalse(result.currentIntersection());
    }

    @Test
    @DisplayName("D23/D26: attraversamento swept del piano inferiore è rilevato tra due tick")
    void detectsSweptBottomCrossing() {
        ClimbTraversal traversal = traversal();
        VoxelShape ladder = Shapes.box(0.0, 0.0, 0.8125, 1.0, 1.0, 1.0);
        AABB previous = new AABB(-59.8, 82.20, -41.80, -59.2, 84.0, -41.20);
        AABB current = new AABB(-59.8, 81.95, -41.80, -59.2, 83.75, -41.20);

        ClimbContactProbe.Result result = ClimbContactProbe.evaluate(
                current, previous, ladder, new BlockPos(-60, 82, -42),
                transition(traversal), traversal);

        assertTrue(result.bottomCrossing());
        assertEquals(ClimbContactProbe.ContactState.BELOW_COLUMN, result.state());
    }
}
