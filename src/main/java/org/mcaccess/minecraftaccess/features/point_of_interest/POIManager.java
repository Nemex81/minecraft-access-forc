package org.mcaccess.minecraftaccess.features.point_of_interest;

import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.POIWaypoints;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointManager;

public class POIManager {
    public LockingHandler lockingHandler;
    public ObjectTracker objectTracker;
    public POIBlocks poiBlocks;
    public POIEntities poiEntities;
    public POIMarking poiMarking;
    public WaypointManager waypointManager;
    public POIWaypoints poiWaypoints;

    public POIManager() {
        lockingHandler = new LockingHandler();
        objectTracker = new ObjectTracker();
        poiBlocks = new POIBlocks();
        poiEntities = new POIEntities();
        poiMarking = new POIMarking();
        waypointManager = new WaypointManager();
        poiWaypoints = new POIWaypoints();
    }
}
