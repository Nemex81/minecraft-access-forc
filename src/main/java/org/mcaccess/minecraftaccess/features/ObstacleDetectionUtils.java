package org.mcaccess.minecraftaccess.features;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;

public final class ObstacleDetectionUtils {

    public static final double AUTO_STEP_THRESHOLD = 0.60;
    public static final double MAX_JUMPABLE_STEP = 1.20;

    private ObstacleDetectionUtils() {
    }

    public enum ObstacleState {
        CLEAR,
        STEP_CLIMBABLE,
        WALL,
        LOW_CEILING,
        HEAD_OBSTACLE
    }

    public enum NarrationStyle {
        BLOCK,
        ELEVATION,
        DIRECT,
        SLOPE
    }

    public record ObstacleScanResult(
            ObstacleState state,
            BlockPos targetFootPos,
            BlockPos targetHeadPos,
            BlockPos targetHeadroomPos,
            BlockPos playerHeadroomPos,
            @Nullable BlockPos lookAtPos,
            @Nullable BlockState primaryBlockState
    ) {
    }

    public static boolean isSolid(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public static double getBlockCollisionHeight(Level level, BlockPos pos) {
        if (level == null || pos == null) return 0.0;
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) return 0.0;
        return shape.max(Direction.Axis.Y);
    }

    public static ObstacleScanResult scan(Level level, Vec3 playerPos, Vec3 moveDir, int range) {
        if (level == null || playerPos == null) {
            return new ObstacleScanResult(ObstacleState.CLEAR, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, null, null);
        }

        int scanRange = Math.max(1, range);
        double playerFeetY = playerPos.y;
        int baseGroundY = (int) Math.floor(playerFeetY - 0.05);
        int playerBaseY = baseGroundY + 1;

        BlockPos playerHeadroomPos = BlockPos.containing(playerPos.x, baseGroundY + 3, playerPos.z);

        List<BlockPos> targetFootPositions = new ArrayList<>();
        double stepSize = 0.25;
        for (double d = 0.1; d <= scanRange + 0.01; d += stepSize) {
            double tx = playerPos.x + moveDir.x * d;
            double tz = playerPos.z + moveDir.z * d;
            BlockPos p = BlockPos.containing(tx, playerBaseY, tz);
            if (!targetFootPositions.contains(p)) {
                targetFootPositions.add(p);
            }
        }

        if (targetFootPositions.isEmpty()) {
            double tx = playerPos.x + moveDir.x * scanRange;
            double tz = playerPos.z + moveDir.z * scanRange;
            targetFootPositions.add(BlockPos.containing(tx, playerBaseY, tz));
        }

        for (BlockPos targetFootPos : targetFootPositions) {
            BlockPos posBelow = targetFootPos.below(1);
            BlockPos targetHeadPos = targetFootPos.above(1);
            BlockPos targetHeadroomPos = targetFootPos.above(2);

            double topYBelow = baseGroundY;
            if (level != null) {
                BlockState stateBelow = level.getBlockState(posBelow);
                VoxelShape shapeBelow = stateBelow.getCollisionShape(level, posBelow);
                if (!shapeBelow.isEmpty()) {
                    topYBelow = baseGroundY + shapeBelow.max(Direction.Axis.Y);
                }
            }

            double topYFoot = topYBelow;
            boolean footSolid = false;
            if (level != null) {
                BlockState stateFoot = level.getBlockState(targetFootPos);
                VoxelShape shapeFoot = stateFoot.getCollisionShape(level, targetFootPos);
                if (!shapeFoot.isEmpty()) {
                    footSolid = true;
                    topYFoot = playerBaseY + shapeFoot.max(Direction.Axis.Y);
                }
            }

            double deltaY = topYFoot - playerFeetY;
            boolean headSolid = isSolid(level, targetHeadPos);
            boolean targetHeadroomSolid = isSolid(level, targetHeadroomPos);
            boolean playerHeadroomSolid = isSolid(level, playerHeadroomPos);

            ObstacleState state = evaluateState(deltaY, headSolid, targetHeadroomSolid, playerHeadroomSolid);

            if (state != ObstacleState.CLEAR) {
                BlockPos lookAtPos = determineLookAtBlock(deltaY, headSolid, targetHeadroomSolid, playerHeadroomSolid,
                        targetFootPos, targetHeadPos, targetHeadroomPos, playerHeadroomPos);

                BlockState primaryBlockState = null;
                if (state == ObstacleState.STEP_CLIMBABLE || state == ObstacleState.LOW_CEILING) {
                    primaryBlockState = level.getBlockState(targetFootPos);
                } else if (state == ObstacleState.WALL) {
                    primaryBlockState = headSolid ? level.getBlockState(targetHeadPos) : level.getBlockState(targetFootPos);
                } else if (state == ObstacleState.HEAD_OBSTACLE) {
                    primaryBlockState = level.getBlockState(targetHeadPos);
                }

                return new ObstacleScanResult(state, targetFootPos, targetHeadPos, targetHeadroomPos, playerHeadroomPos, lookAtPos, primaryBlockState);
            }
        }

        BlockPos primaryFootPos = targetFootPositions.get(0);
        return new ObstacleScanResult(ObstacleState.CLEAR, primaryFootPos, primaryFootPos.above(1), primaryFootPos.above(2), playerHeadroomPos, null, null);
    }

    public static ObstacleState evaluateState(double deltaY, boolean headSolid, boolean targetHeadroomSolid, boolean playerHeadroomSolid) {
        // DeltaY <= 0.6 is smoothly walked over with Minecraft's native auto-step (dirt paths, flat terrain, slabs, stairs)
        if (deltaY <= AUTO_STEP_THRESHOLD && !headSolid) {
            return ObstacleState.CLEAR;
        }
        // Fences, walls, barriers (> 1.20) or solid block at eye level -> unjumpable obstacle
        if (deltaY > MAX_JUMPABLE_STEP || headSolid) {
            if (deltaY <= AUTO_STEP_THRESHOLD && headSolid) {
                return ObstacleState.HEAD_OBSTACLE;
            }
            return ObstacleState.WALL;
        }
        // Real jumpable step (0.60 < deltaY <= 1.20)
        if (deltaY > AUTO_STEP_THRESHOLD && deltaY <= MAX_JUMPABLE_STEP && !headSolid) {
            if (targetHeadroomSolid || playerHeadroomSolid) {
                return ObstacleState.LOW_CEILING;
            }
            return ObstacleState.STEP_CLIMBABLE;
        }
        return ObstacleState.CLEAR;
    }

    public static ObstacleState evaluateState(boolean footSolid, double footHeight, boolean headSolid, boolean targetHeadroomSolid, boolean playerHeadroomSolid) {
        double deltaY = footSolid ? footHeight : 0.0;
        return evaluateState(deltaY, headSolid, targetHeadroomSolid, playerHeadroomSolid);
    }

    public static ObstacleState evaluateState(boolean footSolid, boolean headSolid, boolean targetHeadroomSolid, boolean playerHeadroomSolid) {
        return evaluateState(footSolid, footSolid ? 1.0 : 0.0, headSolid, targetHeadroomSolid, playerHeadroomSolid);
    }

    public static BlockPos determineLookAtBlock(double deltaY, boolean headSolid, boolean targetHeadroomSolid, boolean playerHeadroomSolid,
                                               BlockPos targetFootPos, BlockPos targetHeadPos, BlockPos targetHeadroomPos, BlockPos playerHeadroomPos) {
        if (playerHeadroomSolid) {
            return playerHeadroomPos;
        }
        if (headSolid) {
            return targetHeadPos;
        }
        if (targetHeadroomSolid) {
            return targetHeadroomPos;
        }
        if (deltaY > AUTO_STEP_THRESHOLD) {
            return targetFootPos;
        }
        return null;
    }

    public static BlockPos determineLookAtBlock(boolean footSolid, boolean headSolid, boolean targetHeadroomSolid, boolean playerHeadroomSolid,
                                               BlockPos targetFootPos, BlockPos targetHeadPos, BlockPos targetHeadroomPos, BlockPos playerHeadroomPos) {
        return determineLookAtBlock(footSolid ? 1.0 : 0.0, headSolid, targetHeadroomSolid, playerHeadroomSolid,
                targetFootPos, targetHeadPos, targetHeadroomPos, playerHeadroomPos);
    }

    public static String getNarrationMessage(ObstacleScanResult result, NarrationStyle style) {
        String blockName = result.primaryBlockState() != null ? result.primaryBlockState().getBlock().getName().getString() : "";
        return switch (result.state()) {
            case CLEAR -> I18n.get("minecraft_access.obstacle_detector.clear");
            case WALL -> I18n.get("minecraft_access.obstacle_detector.obstacle", blockName);
            case HEAD_OBSTACLE -> I18n.get("minecraft_access.obstacle_detector.head_obstacle", blockName);
            case STEP_CLIMBABLE -> switch (style) {
                case BLOCK -> I18n.get("minecraft_access.obstacle_detector.step_climbable.block", blockName);
                case ELEVATION -> I18n.get("minecraft_access.obstacle_detector.step_climbable.elevation", blockName);
                case DIRECT -> I18n.get("minecraft_access.obstacle_detector.step_climbable.direct", blockName);
                case SLOPE -> I18n.get("minecraft_access.obstacle_detector.step_climbable.slope", blockName);
            };
            case LOW_CEILING -> switch (style) {
                case BLOCK -> I18n.get("minecraft_access.obstacle_detector.low_ceiling.block", blockName);
                case ELEVATION -> I18n.get("minecraft_access.obstacle_detector.low_ceiling.elevation", blockName);
                case DIRECT -> I18n.get("minecraft_access.obstacle_detector.low_ceiling.direct", blockName);
                case SLOPE -> I18n.get("minecraft_access.obstacle_detector.low_ceiling.slope", blockName);
            };
        };
    }

    public static double calculateRelativeAngle(Vec3 moveDir, float playerYaw) {
        if (moveDir == null || (moveDir.x == 0 && moveDir.z == 0)) {
            return 0.0;
        }
        // Geographic compass angle of movement (0 = North, 90 = East, 180 = South, 270 = West)
        double moveCompassDeg = (Math.toDegrees(Math.atan2(moveDir.x, -moveDir.z)) + 360.0) % 360.0;
        float rawYaw = (playerYaw % 360.0f + 360.0f) % 360.0f;
        double playerHeading = (rawYaw + 180.0f) % 360.0f;
        return (moveCompassDeg - playerHeading + 360.0) % 360.0;
    }

    public static @Nullable String getRelativeDirectionString(double relAngle, Config.ObstacleDetector.DirectionFeedbackMode mode) {
        if (mode == null || mode == Config.ObstacleDetector.DirectionFeedbackMode.OFF) {
            return null;
        }
        double a = ((relAngle % 360.0) + 360.0) % 360.0;
        if (mode == Config.ObstacleDetector.DirectionFeedbackMode.FOUR_DIRECTIONS || mode == Config.ObstacleDetector.DirectionFeedbackMode.OMIT_FORWARD) {
            if (a >= 315.0 || a < 45.0) {
                return mode == Config.ObstacleDetector.DirectionFeedbackMode.OMIT_FORWARD ? null : I18n.get("minecraft_access.obstacle_detector.dir_forward");
            } else if (a < 135.0) {
                return I18n.get("minecraft_access.obstacle_detector.dir_right");
            } else if (a < 225.0) {
                return I18n.get("minecraft_access.obstacle_detector.dir_back");
            } else {
                return I18n.get("minecraft_access.obstacle_detector.dir_left");
            }
        } else if (mode == Config.ObstacleDetector.DirectionFeedbackMode.EIGHT_DIRECTIONS) {
            if (a >= 337.5 || a < 22.5) {
                return I18n.get("minecraft_access.obstacle_detector.dir_forward");
            } else if (a < 67.5) {
                return I18n.get("minecraft_access.obstacle_detector.dir_forward_right");
            } else if (a < 112.5) {
                return I18n.get("minecraft_access.obstacle_detector.dir_right");
            } else if (a < 157.5) {
                return I18n.get("minecraft_access.obstacle_detector.dir_back_right");
            } else if (a < 202.5) {
                return I18n.get("minecraft_access.obstacle_detector.dir_back");
            } else if (a < 247.5) {
                return I18n.get("minecraft_access.obstacle_detector.dir_back_left");
            } else if (a < 292.5) {
                return I18n.get("minecraft_access.obstacle_detector.dir_left");
            } else {
                return I18n.get("minecraft_access.obstacle_detector.dir_forward_left");
            }
        }
        return null;
    }

    public record DirectionalObstacle(
            double relativeAngle,
            String directionName,
            ObstacleScanResult result,
            int distanceInBlocks
    ) {
    }

    public record PanoramicScanResult(
            boolean playerHeadroomSolid,
            @Nullable BlockState playerHeadroomState,
            List<DirectionalObstacle> obstacles,
            List<String> clearDirections,
            int scannedRange
    ) {
    }

    public static @Nullable Double calculateIntendedMoveAngle(boolean up, boolean down, boolean left, boolean right) {
        double inputZ = 0; // forward (+1) / back (-1)
        double inputX = 0; // strafe left (+1) / strafe right (-1)
        if (up) inputZ += 1;
        if (down) inputZ -= 1;
        if (left) inputX += 1;
        if (right) inputX -= 1;

        if (inputZ == 0 && inputX == 0) {
            return null;
        }

        // In Minecraft coordinates: W = 0, D = 90, S = 180, A = 270
        double relDeg = Math.toDegrees(Math.atan2(-inputX, inputZ));
        return (relDeg + 360.0) % 360.0;
    }

    public static Vec3 calculateWorldMoveDirFromRelativeAngle(double relAngleDeg, float playerYaw) {
        float rawYaw = (playerYaw % 360.0f + 360.0f) % 360.0f;
        double playerHeading = (rawYaw + 180.0f) % 360.0f;
        double worldCompassDeg = (playerHeading + relAngleDeg + 360.0) % 360.0;
        double rad = Math.toRadians(worldCompassDeg);
        double vx = Math.sin(rad);
        double vz = -Math.cos(rad);
        return new Vec3(vx, 0, vz).normalize();
    }

    public static PanoramicScanResult scanPanoramic(Level level, Vec3 playerPos, float playerYaw, int range,
                                                   Config.ObstacleDetector.DirectionFeedbackMode mode, boolean checkHeadroom) {
        if (level == null || playerPos == null) {
            return new PanoramicScanResult(false, null, List.of(), List.of(), range);
        }

        int scanRange = Math.max(1, range);
        double playerFeetY = playerPos.y;
        int baseGroundY = (int) Math.floor(playerFeetY - 0.05);

        boolean playerHeadroomSolid = false;
        BlockState playerHeadroomState = null;
        if (checkHeadroom) {
            BlockPos playerHeadroomPos = BlockPos.containing(playerPos.x, baseGroundY + 3, playerPos.z);
            if (isSolid(level, playerHeadroomPos)) {
                playerHeadroomSolid = true;
                playerHeadroomState = level.getBlockState(playerHeadroomPos);
            }
        }

        double[] angles;
        if (mode == Config.ObstacleDetector.DirectionFeedbackMode.EIGHT_DIRECTIONS) {
            angles = new double[]{0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0};
        } else {
            angles = new double[]{0.0, 90.0, 180.0, 270.0};
        }

        List<DirectionalObstacle> obstacles = new ArrayList<>();
        List<String> clearDirections = new ArrayList<>();

        for (double relAngle : angles) {
            Vec3 rayDir = calculateWorldMoveDirFromRelativeAngle(relAngle, playerYaw);
            ObstacleScanResult res = scan(level, playerPos, rayDir, scanRange);
            String dirName = getRelativeDirectionString(relAngle, mode != null ? mode : Config.ObstacleDetector.DirectionFeedbackMode.FOUR_DIRECTIONS);
            if (dirName == null || dirName.isBlank()) {
                dirName = I18n.get("minecraft_access.obstacle_detector.dir_forward");
            }

            if (res.state() != ObstacleState.CLEAR) {
                double dx = res.targetFootPos().getX() + 0.5 - playerPos.x;
                double dz = res.targetFootPos().getZ() + 0.5 - playerPos.z;
                int dist = Math.max(1, (int) Math.round(Math.sqrt(dx * dx + dz * dz)));
                obstacles.add(new DirectionalObstacle(relAngle, dirName, res, dist));
            } else {
                clearDirections.add(dirName);
            }
        }

        return new PanoramicScanResult(playerHeadroomSolid, playerHeadroomState, obstacles, clearDirections, scanRange);
    }

    public static String getPanoramicNarrationMessage(PanoramicScanResult pano, NarrationStyle style) {
        StringBuilder sb = new StringBuilder();

        if (pano.playerHeadroomSolid()) {
            String headBlockName = pano.playerHeadroomState() != null ? pano.playerHeadroomState().getBlock().getName().getString() : "";
            sb.append(I18n.get("minecraft_access.obstacle_detector.player_headroom_blocked", headBlockName));
        }

        if (pano.obstacles().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(". ");
            }
            sb.append(I18n.get("minecraft_access.obstacle_detector.panoramic_clear", pano.scannedRange()));
            return sb.toString();
        }

        for (DirectionalObstacle obs : pano.obstacles()) {
            if (sb.length() > 0) {
                sb.append(". ");
            }
            String baseMsg = getNarrationMessage(obs.result(), style);
            String distStr = obs.distanceInBlocks() == 1
                    ? I18n.get("minecraft_access.obstacle_detector.at_distance_single", baseMsg)
                    : I18n.get("minecraft_access.obstacle_detector.at_distance", baseMsg, obs.distanceInBlocks());
            sb.append(I18n.get("minecraft_access.obstacle_detector.with_direction", obs.directionName(), distStr));
        }

        return sb.toString();
    }

    public static String getNarrationMessage(ObstacleScanResult result, NarrationStyle style, double relAngle, Config.ObstacleDetector.DirectionFeedbackMode mode) {
        String baseMessage = getNarrationMessage(result, style);
        if (result.state() == ObstacleState.CLEAR) {
            return baseMessage;
        }
        String dirString = getRelativeDirectionString(relAngle, mode);
        if (dirString != null && !dirString.isBlank()) {
            return I18n.get("minecraft_access.obstacle_detector.with_direction", dirString, baseMessage);
        }
        return baseMessage;
    }
}
