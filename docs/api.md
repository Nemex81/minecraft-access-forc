# Minecraft Access — Public & Internal API Documentation

This document describes the public integration points and internal APIs provided by `minecraft-access` for client-side modding and accessibility extensions.

---

## 1. Speech & Narration API

The primary entry point for speech narration is `MainClass.narrate`:

```java
package org.mcaccess.minecraft_access;

public class MainClass {
    /**
     * Sends a narration string to the active Screen Reader (NVDA / SAPI).
     *
     * @param text      The localized text string to speak.
     * @param interrupt If true, interrupts any active speech immediately (Level 1).
     *                  If false, queues or safely finishes current phrase (Level 2).
     */
    public static void narrate(String text, boolean interrupt) {
        // Implementation delegates to Tolk native proxy
    }
}
```

### Best Practices:
- Always use localized strings via `Component.translatable("minecraft_access.key")`.
- Set `interrupt = true` ONLY for immediate physical hazards (lava, pits, damage).
- Use `interrupt = false` for UI navigation, slot selection, and arrival notifications.

---

## 2. 3D Positional Audio API

To play positional audio cues that respect the player's 3D sound settings:

```java
package org.mcaccess.minecraft_access.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;

public class AudioHelper {
    /**
     * Plays a 3D sound event at specific world coordinates with safe volume clamping.
     *
     * @param sound  The sound event to play.
     * @param pos    Target block position.
     * @param volume Clamped to maximum 0.8f for screen reader protection.
     * @param pitch  Pitch modifier (0.5f = low/down, 1.0f = neutral, 1.5f = high/up).
     */
    public static void playPositionalSound(SoundEvent sound, BlockPos pos, float volume, float pitch) {
        // Implementation
    }
}
```

---

## 3. Point of Interest (POI) & Waypoints API

```java
package org.mcaccess.minecraft_access.features.point_of_interest.waypoints;

import java.util.List;

public class WaypointManager {
    public static List<Waypoint> getWaypointsForWorld(String worldId);
    public static void addWaypoint(Waypoint waypoint);
    public static void removeWaypoint(String waypointId);
    public static Waypoint getActiveTarget();
    public static void setActiveTarget(Waypoint waypoint);
}
```