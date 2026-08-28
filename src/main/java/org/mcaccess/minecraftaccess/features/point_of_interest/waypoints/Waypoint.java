package org.mcaccess.minecraftaccess.features.point_of_interest.waypoints;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record Waypoint(
        String id,
        String name,
        BlockPos pos,
        Identifier dimension,
        WaypointType type,
        long timestamp
) {
    public Waypoint withName(String newName) {
        return new Waypoint(id, newName, pos, dimension, type, timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Waypoint waypoint = (Waypoint) o;
        return Objects.equals(id, waypoint.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
