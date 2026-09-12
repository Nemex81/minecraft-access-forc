package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/** Runtime collector for the volumetric landing gates required by D26/D27. */
public final class ClimbLandingProbe {

    private static final double FOOT_INSET = 0.02;
    private static final double HEIGHT_EPSILON = 0.08;
    private static final double VELOCITY_EPSILON = 0.085;

    public record Result(
            boolean supported,
            boolean bodyClear,
            boolean verticallyStable,
            boolean transferSafe,
            boolean destinationPracticable
    ) {
        public Result(boolean supported, boolean bodyClear, boolean verticallyStable, boolean transferSafe) {
            this(supported, bodyClear, verticallyStable, transferSafe, false);
        }

        public boolean stable() {
            return supported && bodyClear && verticallyStable;
        }
    }

    private ClimbLandingProbe() {
    }

    public static @NotNull Result evaluate(
            @NotNull Level level,
            @NotNull AABB playerBox,
            @NotNull BlockPos landingPos,
            boolean onGround,
            double verticalVelocity
    ) {
        double centerX = (playerBox.minX + playerBox.maxX) * 0.5;
        double centerZ = (playerBox.minZ + playerBox.maxZ) * 0.5;
        boolean footprintSupported = isFootprintSupported(level, playerBox, landingPos);
        boolean supported = onGround && footprintSupported;

        AABB bodyInterior = playerBox.inflate(-1.0e-4);
        boolean bodyClear = !intersectsCollision(level, bodyInterior, landingPos);
        boolean verticallyStable = Math.abs(verticalVelocity) <= VELOCITY_EPSILON
                && Math.abs(playerBox.minY - landingPos.getY()) <= HEIGHT_EPSILON;
        double targetX = landingPos.getX() + 0.5;
        double targetZ = landingPos.getZ() + 0.5;
        double dx = targetX - centerX;
        double dz = targetZ - centerZ;
        double distance = Math.hypot(dx, dz);
        double scale = distance > 0.08 ? 0.08 / distance : 1.0;
        AABB predicted = playerBox.move(dx * scale, 0.0, dz * scale);
        boolean predictedClear = !intersectsCollision(level, predicted.inflate(-1.0e-4), landingPos);
        boolean transferSafe = bodyClear && predictedClear
                && isFootprintSupported(level, predicted, landingPos);
        boolean destinationPracticable = hasSupportAt(level, targetX, targetZ, landingPos.getY(), landingPos);
        return new Result(supported, bodyClear, verticallyStable, transferSafe, destinationPracticable);
    }

    private static boolean isFootprintSupported(Level level, AABB playerBox, BlockPos landingPos) {
        double centerX = (playerBox.minX + playerBox.maxX) * 0.5;
        double centerZ = (playerBox.minZ + playerBox.maxZ) * 0.5;
        double insetX = Math.max(0.0, (playerBox.maxX - playerBox.minX) * 0.5 - FOOT_INSET);
        double insetZ = Math.max(0.0, (playerBox.maxZ - playerBox.minZ) * 0.5 - FOOT_INSET);
        return hasSupportAt(level, centerX - insetX, centerZ - insetZ, playerBox.minY, landingPos)
                && hasSupportAt(level, centerX - insetX, centerZ + insetZ, playerBox.minY, landingPos)
                && hasSupportAt(level, centerX + insetX, centerZ - insetZ, playerBox.minY, landingPos)
                && hasSupportAt(level, centerX + insetX, centerZ + insetZ, playerBox.minY, landingPos);
    }

    private static boolean hasSupportAt(Level level, double x, double z, double feetY, BlockPos landingPos) {
        int minX = (int) Math.floor(x);
        int minZ = (int) Math.floor(z);
        int baseY = (int) Math.floor(feetY - 1.0e-4);
        for (int y = baseY - 1; y <= baseY; y++) {
            BlockPos pos = new BlockPos(minX, y, minZ);
            if (Math.abs(pos.getX() - landingPos.getX()) > 1
                    || Math.abs(pos.getZ() - landingPos.getZ()) > 1) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            for (AABB local : shape.toAabbs()) {
                AABB world = local.move(pos.getX(), pos.getY(), pos.getZ());
                if (x > world.minX - 1.0e-4 && x < world.maxX + 1.0e-4
                        && z > world.minZ - 1.0e-4 && z < world.maxZ + 1.0e-4
                        && Math.abs(world.maxY - feetY) <= HEIGHT_EPSILON) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean intersectsCollision(Level level, AABB body, BlockPos landingPos) {
        int minX = (int) Math.floor(body.minX);
        int maxX = (int) Math.floor(body.maxX);
        int minY = (int) Math.floor(body.minY);
        int maxY = (int) Math.floor(body.maxY);
        int minZ = (int) Math.floor(body.minZ);
        int maxZ = (int) Math.floor(body.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (Math.abs(x - landingPos.getX()) > 1 || Math.abs(z - landingPos.getZ()) > 1) {
                        continue;
                    }
                    VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                    for (AABB local : shape.toAabbs()) {
                        if (body.intersects(local.move(x, y, z))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
