package org.mcaccess.minecraftaccess.features.autowalk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbTraversal;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbableGeometry;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbEntryTransition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test deterministici per ClimbRouteAssembler (Contratti D0, D1, D2).
 */
class ClimbRouteAssemblerTest {

    @Test
    @DisplayName("Assemblaggio salita: sequenza MOUNT -> TRANSIT per-rung -> DISMOUNT")
    void testAssembleAscentRoute() {
        BlockPos bottom = new BlockPos(-59, 81, -41);
        BlockPos top = new BlockPos(-59, 85, -41);
        BlockPos landing = new BlockPos(-59, 86, -40);

        ClimbTraversal traversal = ClimbTraversal.of(
                Direction.AxisDirection.POSITIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                bottom,
                top,
                bottom,
                landing,
                Direction.NORTH,
                null
        );

        // Giocatore adiacente a quota 81 (davanti alla scala)
        BlockPos playerPos = new BlockPos(-59, 81, -42);

        ClimbRouteAssembler.ClimbRoute route = ClimbRouteAssembler.assembleClimbRoute(playerPos, traversal);
        assertNotNull(route, "La rotta deve essere assemblata con successo");

        List<BlockPos> path = route.path();
        List<RouteSegment> segments = route.segments();

        assertEquals(path.size() - 1, segments.size());

        // Primo segmento: MOUNT (da playerPos a entryPos)
        RouteSegment firstSeg = segments.get(0);
        assertTrue(firstSeg.isMount());
        assertEquals(playerPos, firstSeg.from());
        assertEquals(bottom, firstSeg.to());

        // Segmenti intermedi: TRANSIT per-rung (81->82, 82->83, 83->84, 84->85)
        for (int i = 1; i < segments.size() - 1; i++) {
            RouteSegment transitSeg = segments.get(i);
            assertTrue(transitSeg.isTransit());
            assertEquals(1, Math.abs(transitSeg.to().getY() - transitSeg.from().getY()));
            assertEquals(transitSeg.from().getX(), transitSeg.to().getX());
            assertEquals(transitSeg.from().getZ(), transitSeg.to().getZ());
        }

        // Ultimo segmento: DISMOUNT (da top a landing)
        RouteSegment lastSeg = segments.get(segments.size() - 1);
        assertTrue(lastSeg.isDismount());
        assertEquals(top, lastSeg.from());
        assertEquals(landing, lastSeg.to());
    }

    @Test
    @DisplayName("Assemblaggio discesa: sequenza da top a bottom")
    void testAssembleDescentRoute() {
        BlockPos bottom = new BlockPos(10, 64, 10);
        BlockPos top = new BlockPos(10, 70, 10);
        BlockPos landing = new BlockPos(10, 64, 11);

        ClimbTraversal traversal = ClimbTraversal.of(
                Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                top,
                top,
                bottom,
                landing,
                Direction.SOUTH,
                null
        );

        BlockPos playerPos = new BlockPos(10, 70, 9); // Adiacente alla sommita'

        ClimbRouteAssembler.ClimbRoute route = ClimbRouteAssembler.assembleClimbRoute(playerPos, traversal);
        assertNotNull(route);

        List<RouteSegment> segments = route.segments();
        assertTrue(segments.get(0).isMount());
        ClimbEntryTransition transition = segments.get(0).climbEntryTransition();
        assertNotNull(transition, "Il Mount superiore deve conservare la transizione completa");
        assertEquals(Direction.SOUTH, transition.approachDirection());
        assertEquals(playerPos, transition.surfacePos());
        assertEquals(top, transition.entryPos());
        assertTrue(segments.get(segments.size() - 1).isDismount());

        // Verifica che i transit vadano verso il basso (deltaY = -1)
        for (int i = 1; i < segments.size() - 1; i++) {
            RouteSegment transit = segments.get(i);
            assertTrue(transit.isTransit());
            assertEquals(-1, transit.to().getY() - transit.from().getY());
        }
    }

    @Test
    @DisplayName("Validazione topologica: rifiuta rotte non conformi")
    void testValidationRejectsInvalidTopology() {
        BlockPos bottom = new BlockPos(0, 64, 0);
        BlockPos top = new BlockPos(0, 66, 0);
        BlockPos landing = new BlockPos(0, 67, 1);

        ClimbTraversal traversal = ClimbTraversal.of(
                Direction.AxisDirection.POSITIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                bottom,
                top,
                bottom,
                landing,
                Direction.NORTH,
                null
        );

        // Path con salto verticale > 1 blocco
        List<BlockPos> brokenPath = List.of(
                bottom,
                new BlockPos(0, 66, 0), // Salto da 64 a 66 senza 65!
                landing
        );
        List<RouteSegment> brokenSegments = List.of(
                RouteSegment.climb(bottom, new BlockPos(0, 66, 0), RouteSegment.ClimbLeg.TRANSIT, traversal),
                RouteSegment.climb(new BlockPos(0, 66, 0), landing, RouteSegment.ClimbLeg.DISMOUNT, traversal)
        );

        assertFalse(ClimbRouteAssembler.validateClimbRoute(brokenPath, brokenSegments, traversal));
    }
}
