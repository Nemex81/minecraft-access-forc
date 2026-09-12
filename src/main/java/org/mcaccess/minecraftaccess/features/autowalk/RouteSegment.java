package org.mcaccess.minecraftaccess.features.autowalk;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbTraversal;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbEntryTransition;

/**
 * Record immutabile che descrive un segmento atomico tipizzato del percorso calcolato (Contratto D2).
 */
public record RouteSegment(
        @NotNull BlockPos from,
        @NotNull BlockPos to,
        @NotNull SegmentType type,
        @Nullable ClimbLeg climbLeg,
        @Nullable ClimbTraversal climbData,
        @Nullable ClimbEntryTransition climbEntryTransition
) {
    public enum SegmentType {
        WALK,
        STEP_UP,
        DROP_DOWN,
        SWIM,
        CLIMB
    }

    public enum ClimbLeg {
        MOUNT,
        TRANSIT,
        DISMOUNT
    }

    public RouteSegment(
            @NotNull BlockPos from,
            @NotNull BlockPos to,
            @NotNull SegmentType type,
            @Nullable ClimbTraversal climbData
    ) {
        this(from, to, type, type == SegmentType.CLIMB ? ClimbLeg.TRANSIT : null, climbData, null);
    }

    public RouteSegment(
            @NotNull BlockPos from,
            @NotNull BlockPos to,
            @NotNull SegmentType type,
            @Nullable ClimbLeg climbLeg,
            @Nullable ClimbTraversal climbData
    ) {
        this(from, to, type, climbLeg, climbData, null);
    }

    public boolean isClimb() {
        return type == SegmentType.CLIMB;
    }

    public boolean isMount() {
        return type == SegmentType.CLIMB && climbLeg == ClimbLeg.MOUNT;
    }

    public boolean isTransit() {
        return type == SegmentType.CLIMB && climbLeg == ClimbLeg.TRANSIT;
    }

    public boolean isDismount() {
        return type == SegmentType.CLIMB && climbLeg == ClimbLeg.DISMOUNT;
    }

    public static RouteSegment walk(@NotNull BlockPos from, @NotNull BlockPos to) {
        return new RouteSegment(from, to, SegmentType.WALK, null, null, null);
    }

    public static RouteSegment stepUp(@NotNull BlockPos from, @NotNull BlockPos to) {
        return new RouteSegment(from, to, SegmentType.STEP_UP, null, null, null);
    }

    public static RouteSegment dropDown(@NotNull BlockPos from, @NotNull BlockPos to) {
        return new RouteSegment(from, to, SegmentType.DROP_DOWN, null, null, null);
    }

    public static RouteSegment swim(@NotNull BlockPos from, @NotNull BlockPos to) {
        return new RouteSegment(from, to, SegmentType.SWIM, null, null, null);
    }

    public static RouteSegment climb(
            @NotNull BlockPos from,
            @NotNull BlockPos to,
            @NotNull ClimbLeg leg,
            @Nullable ClimbTraversal climbData
    ) {
        return new RouteSegment(from, to, SegmentType.CLIMB, leg, climbData, null);
    }

    public static RouteSegment climb(
            @NotNull BlockPos from,
            @NotNull BlockPos to,
            @NotNull ClimbLeg leg,
            @Nullable ClimbTraversal climbData,
            @Nullable ClimbEntryTransition climbEntryTransition
    ) {
        return new RouteSegment(from, to, SegmentType.CLIMB, leg, climbData, climbEntryTransition);
    }

    public static RouteSegment climb(@NotNull BlockPos from, @NotNull BlockPos to, @Nullable ClimbTraversal climbData) {
        return new RouteSegment(from, to, SegmentType.CLIMB, ClimbLeg.TRANSIT, climbData, null);
    }
}
