package org.mcaccess.minecraftaccess.features.safety.traversal;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Osservatore geometrico puro del contatto con una colonna arrampicabile (D23).
 * Il chiamante risolve la shape nel mondo; questo componente non legge il client
 * e non possiede alcun input virtuale.
 */
public final class ClimbContactProbe {

    private static final double EPSILON = 1.0e-4;
    private static final double CAPTURE_DEPTH = 0.45;

    public enum ContactState {
        OUTSIDE,
        APPROACHING,
        CONTACT,
        COMMITTED_INSIDE,
        BELOW_COLUMN
    }

    public enum ReasonCode {
        NONE,
        AABB_CONTACT,
        SWEPT_CROSSING,
        VANILLA_CLIMBABLE,
        BOTTOM_CROSSING,
        SUPPORTED_LANDING,
        OUTSIDE_COLUMN_ABORT
    }

    public record Result(
            @NotNull ContactState state,
            double signedEntryPlaneDistance,
            boolean currentIntersection,
            boolean sweptIntersection,
            boolean apertureCapture,
            boolean bottomCrossing,
            boolean inColumnCorridor
    ) {
        public Result(
                @NotNull ContactState state,
                double signedEntryPlaneDistance,
                boolean currentIntersection,
                boolean sweptIntersection,
                boolean apertureCapture,
                boolean bottomCrossing
        ) {
            this(state, signedEntryPlaneDistance, currentIntersection, sweptIntersection, apertureCapture, bottomCrossing, true);
        }

        public static @NotNull Result outside() {
            return new Result(ContactState.OUTSIDE, Double.POSITIVE_INFINITY,
                    false, false, false, false, false);
        }
    }

    private ClimbContactProbe() {
    }

    public static @NotNull Result evaluate(
            @NotNull AABB currentPlayerBox,
            @NotNull AABB previousPlayerBox,
            @NotNull VoxelShape localClimbShape,
            @NotNull BlockPos shapePos,
            @Nullable ClimbEntryTransition entryTransition,
            @NotNull ClimbTraversal traversal
    ) {
        AABB swept = swept(previousPlayerBox, currentPlayerBox);
        boolean currentIntersection = intersectsShape(currentPlayerBox, localClimbShape, shapePos);
        boolean sweptIntersection = intersectsShape(swept, localClimbShape, shapePos);

        double surfaceY = entryTransition != null
                ? entryTransition.surfacePos().getY()
                : traversal.columnTopPos().getY() + 1.0;
        double signedEntryDistance = currentPlayerBox.minY - surfaceY;

        boolean apertureCapture = entryTransition != null
                && centerReachedCaptureBand(currentPlayerBox, previousPlayerBox, entryTransition);
        boolean committedInside = entryTransition != null
                && previousPlayerBox.minY >= surfaceY - EPSILON
                && currentPlayerBox.minY < surfaceY - EPSILON
                && sweptProjectionOverlapsAperture(swept, entryTransition.aperturePos());

        int colX = traversal.columnTopPos().getX();
        int colZ = traversal.columnTopPos().getZ();
        boolean inColumnCorridor = currentPlayerBox.maxX > colX + EPSILON
                && currentPlayerBox.minX < colX + 1.0 - EPSILON
                && currentPlayerBox.maxZ > colZ + EPSILON
                && currentPlayerBox.minZ < colZ + 1.0 - EPSILON;

        double bottomPlaneY = traversal.columnBottomPos().getY();
        boolean bottomCrossing = inColumnCorridor
                && previousPlayerBox.minY > bottomPlaneY + EPSILON
                && currentPlayerBox.minY <= bottomPlaneY + EPSILON;
        boolean belowColumn = inColumnCorridor && currentPlayerBox.minY < bottomPlaneY - EPSILON;

        ContactState state;
        if (belowColumn) {
            state = ContactState.BELOW_COLUMN;
        } else if (committedInside) {
            state = ContactState.COMMITTED_INSIDE;
        } else if (currentIntersection || sweptIntersection) {
            state = ContactState.CONTACT;
        } else if (apertureCapture) {
            state = ContactState.APPROACHING;
        } else {
            state = ContactState.OUTSIDE;
        }

        return new Result(state, signedEntryDistance, currentIntersection,
                sweptIntersection, apertureCapture, bottomCrossing, inColumnCorridor);
    }

    private static boolean intersectsShape(AABB playerBox, VoxelShape localShape, BlockPos pos) {
        List<AABB> boxes = localShape.toAabbs();
        for (AABB local : boxes) {
            AABB world = local.move(pos.getX(), pos.getY(), pos.getZ());
            if (strictlyIntersects(playerBox, world)) {
                return true;
            }
        }
        return false;
    }

    private static boolean strictlyIntersects(AABB a, AABB b) {
        return a.maxX > b.minX + EPSILON && a.minX < b.maxX - EPSILON
                && a.maxY > b.minY + EPSILON && a.minY < b.maxY - EPSILON
                && a.maxZ > b.minZ + EPSILON && a.minZ < b.maxZ - EPSILON;
    }

    private static AABB swept(AABB previous, AABB current) {
        return new AABB(
                Math.min(previous.minX, current.minX),
                Math.min(previous.minY, current.minY),
                Math.min(previous.minZ, current.minZ),
                Math.max(previous.maxX, current.maxX),
                Math.max(previous.maxY, current.maxY),
                Math.max(previous.maxZ, current.maxZ)
        );
    }

    private static boolean centerReachedCaptureBand(
            AABB current,
            AABB previous,
            ClimbEntryTransition transition
    ) {
        double currentX = (current.minX + current.maxX) * 0.5;
        double currentZ = (current.minZ + current.maxZ) * 0.5;
        double previousX = (previous.minX + previous.maxX) * 0.5;
        double previousZ = (previous.minZ + previous.maxZ) * 0.5;
        BlockPos aperture = transition.aperturePos();

        boolean withinCrossAxis = switch (transition.approachDirection().getAxis()) {
            case X -> currentZ >= aperture.getZ() + EPSILON && currentZ <= aperture.getZ() + 1.0 - EPSILON;
            case Z -> currentX >= aperture.getX() + EPSILON && currentX <= aperture.getX() + 1.0 - EPSILON;
            default -> false;
        };
        if (!withinCrossAxis) {
            return false;
        }

        double threshold = switch (transition.approachDirection()) {
            case NORTH -> aperture.getZ() + 1.0 - CAPTURE_DEPTH;
            case SOUTH -> aperture.getZ() + CAPTURE_DEPTH;
            case WEST -> aperture.getX() + 1.0 - CAPTURE_DEPTH;
            case EAST -> aperture.getX() + CAPTURE_DEPTH;
            default -> Double.NaN;
        };
        return switch (transition.approachDirection()) {
            case NORTH -> currentZ <= threshold || previousZ > threshold && currentZ <= threshold;
            case SOUTH -> currentZ >= threshold || previousZ < threshold && currentZ >= threshold;
            case WEST -> currentX <= threshold || previousX > threshold && currentX <= threshold;
            case EAST -> currentX >= threshold || previousX < threshold && currentX >= threshold;
            default -> false;
        };
    }

    private static boolean sweptProjectionOverlapsAperture(AABB swept, BlockPos aperture) {
        return swept.maxX > aperture.getX() + EPSILON
                && swept.minX < aperture.getX() + 1.0 - EPSILON
                && swept.maxZ > aperture.getZ() + EPSILON
                && swept.minZ < aperture.getZ() + 1.0 - EPSILON;
    }
}
