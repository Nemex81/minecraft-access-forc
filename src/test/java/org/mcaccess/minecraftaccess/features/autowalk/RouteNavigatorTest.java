package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.features.point_of_interest.BlockPos3d;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointType;

import static org.junit.jupiter.api.Assertions.*;

class RouteNavigatorTest {

    private RouteNavigator navigator;

    @BeforeEach
    void setUp() {
        navigator = new RouteNavigator();
    }

    @Test
    @DisplayName("Inizializzazione e query su rotta vuota")
    void testInitialRouteStateAndEmptyQuery() {
        assertFalse(navigator.hasActiveRoute());
        assertTrue(navigator.isRouteCompleted());
        assertNull(navigator.getCurrentNodePos());
        assertEquals(0, navigator.getRemainingSteps());
        assertEquals(0.0, navigator.getRemainingDistance(Vec3.ZERO));
    }

    @Test
    @DisplayName("Avanzamento nodi e decremento passi rimanenti")
    void testRouteProgressionAndSteps() {
        BlockPos root = new BlockPos(9, 64, 10);
        BlockPos n1 = new BlockPos(10, 64, 10);
        BlockPos n2 = new BlockPos(11, 64, 10);
        BlockPos n3 = new BlockPos(12, 64, 10);
        navigator.setTestRoute(List.of(root, n1, n2, n3), n3, n3);

        assertTrue(navigator.hasActiveRoute());
        assertFalse(navigator.isRouteCompleted());
        assertEquals(3, navigator.getRemainingSteps());
        assertEquals(n1, navigator.getCurrentNodePos());

        // Avanzamento al nodo 2
        assertTrue(navigator.advanceWaypoint());
        assertEquals(2, navigator.getRemainingSteps());
        assertEquals(n2, navigator.getCurrentNodePos());

        // Avanzamento al nodo 3 (ultimo nodo)
        assertTrue(navigator.advanceWaypoint());
        assertEquals(1, navigator.getRemainingSteps());
        assertEquals(n3, navigator.getCurrentNodePos());

        // Avanzamento oltre l'ultimo nodo -> rotta completata
        assertTrue(navigator.advanceWaypoint());
        assertEquals(0, navigator.getRemainingSteps());
        assertNull(navigator.getCurrentNodePos());
        assertTrue(navigator.isRouteCompleted());
        assertFalse(navigator.hasActiveRoute());

        // Ulteriore chiamata non avanza
        assertFalse(navigator.advanceWaypoint());
    }

    @Test
    @DisplayName("Svuotamento rotta tramite clearRoute()")
    void testClearRoute() {
        BlockPos n1 = new BlockPos(5, 64, 5);
        navigator.setTestRoute(List.of(n1), n1, n1);
        assertTrue(navigator.hasActiveRoute());

        navigator.clearRoute();
        assertFalse(navigator.hasActiveRoute());
        assertTrue(navigator.isRouteCompleted());
        assertNull(navigator.getTargetObject());
        assertNull(navigator.getCurrentGoalPos());
        assertEquals(0, navigator.getCurrentPathIndex());
    }

    @Test
    @DisplayName("Validazione tipi di bersaglio supportati")
    void testTargetValidation() {
        assertFalse(navigator.isTargetValid(null));
        assertTrue(navigator.isTargetValid(new BlockPos(10, 64, 10)));
        assertTrue(navigator.isTargetValid(new BlockPos3d(new BlockPos(10, 64, 10))));
        assertTrue(navigator.isTargetValid(new Waypoint("wp1", "Base", new BlockPos(10, 64, 10), Identifier.fromNamespaceAndPath("minecraft", "overworld"), WaypointType.CUSTOM, 0L)));
        assertFalse(navigator.isTargetValid("StringaNonSupportata"));
    }

    @Test
    @DisplayName("Calcolo distanza euclidea lungo i nodi rimanenti")
    void testRemainingDistanceCalculation() {
        BlockPos n1 = new BlockPos(0, 64, 0);
        BlockPos n2 = new BlockPos(3, 64, 0); // dist = 3.0
        BlockPos n3 = new BlockPos(3, 64, 4); // dist = 4.0
        navigator.setTestRoute(List.of(n1, n2, n3), n3, n3);

        Vec3 playerPos = Vec3.atBottomCenterOf(n1);
        double dist = navigator.getRemainingDistance(playerPos);
        assertEquals(7.0, dist, 0.001);

        navigator.advanceWaypoint();
        Vec3 playerPosAtN2 = Vec3.atBottomCenterOf(n2);
        assertEquals(4.0, navigator.getRemainingDistance(playerPosAtN2), 0.001);
    }

    @Test
    @DisplayName("Immutabilità strutturale della rotta restituita da getCurrentPath()")
    void testStructuralImmutability() {
        BlockPos n1 = new BlockPos(1, 64, 1);
        navigator.setTestRoute(List.of(n1), n1, n1);

        List<BlockPos> path = navigator.getCurrentPath();
        assertThrows(UnsupportedOperationException.class, () -> path.add(new BlockPos(2, 64, 2)));
        assertThrows(UnsupportedOperationException.class, () -> path.remove(0));
    }

    @Test
    @DisplayName("Verifica pura shouldRepathForEntity su bersagli non-entità")
    void testShouldRepathForEntityWithNonEntity() {
        BlockPos n1 = new BlockPos(10, 64, 10);
        navigator.setTestRoute(List.of(n1), n1, n1);
        // Target è BlockPos, non Entity -> non richiede repath dinamico entità
        assertFalse(navigator.shouldRepathForEntity());
    }
}
