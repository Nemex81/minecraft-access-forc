package org.mcaccess.minecraftaccess.features.directional_path_scanner;

import java.util.Collections;
import java.util.List;

/**
 * Immutable data structure representing the outcome of a directional path scan.
 */
public record PathScanReport(
        String directionKey,
        String primaryGroundName,
        int freeDistance,
        int totalRange,
        List<PathScanEvent> events,
        boolean reachedEnd
) {
    public PathScanReport {
        events = Collections.unmodifiableList(events);
    }

    public enum EventType {
        PATH_SEGMENT,
        OBSTACLE_BLOCK,
        DROP_HAZARD,
        ITEM_RESOURCE,
        PASSIVE_MOB,
        HOSTILE_MOB,
        FLUID,
        ELEVATION_CHANGE
    }

    public record PathScanEvent(
            EventType type,
            String name,
            int distance,
            int y,
            int dropDepth
    ) {
    }
}
