package org.mcaccess.minecraftaccess.features.point_of_interest;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointType;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rev MC-26.20: ObjectTracker Null Safety & Validation Tests")
class ObjectTrackerTest {

    @Test
    @DisplayName("D0: isObjectValid returns false for null without NullPointerException")
    void testIsObjectValidWithNull() {
        ObjectTracker tracker = new ObjectTracker();
        assertFalse(tracker.isObjectValid(null), "isObjectValid(null) must return false safely");
    }

    @Test
    @DisplayName("D1: tracker initial currentObject is null and isObjectValid evaluates to false")
    void testIsObjectValidWithInitialState() {
        ObjectTracker tracker = new ObjectTracker();
        assertNull(tracker.getCurrentObject(), "Initial currentObject must be null");
        assertFalse(tracker.isObjectValid(tracker.getCurrentObject()), "isObjectValid(currentObject) must be false when currentObject is null");
    }

    @Test
    @DisplayName("D0: isObjectValid returns true for Waypoint")
    void testIsObjectValidWithWaypoint() {
        ObjectTracker tracker = new ObjectTracker();
        Waypoint wp = new Waypoint("wp1", "Casa", new BlockPos(0, 64, 0), Identifier.fromNamespaceAndPath("minecraft", "overworld"), WaypointType.CUSTOM, 0L);
        assertTrue(tracker.isObjectValid(wp), "isObjectValid must return true for valid Waypoint");
    }

    @Test
    @DisplayName("D0: isObjectValid returns false for unknown object types")
    void testIsObjectValidWithInvalidType() {
        ObjectTracker tracker = new ObjectTracker();
        assertFalse(tracker.isObjectValid("some_arbitrary_string"), "isObjectValid must return false for unknown types");
        assertFalse(tracker.isObjectValid(12345), "isObjectValid must return false for integers");
    }
}
