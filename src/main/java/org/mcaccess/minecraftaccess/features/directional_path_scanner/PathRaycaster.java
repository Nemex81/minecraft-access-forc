package org.mcaccess.minecraftaccess.features.directional_path_scanner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.mcaccess.minecraftaccess.Config;

/**
 * Geometric and voxel engine for directional path scanning with ground-following probe.
 */
public final class PathRaycaster {

    public static final double MAX_STEP_CLIMB = 1.20;

    private PathRaycaster() {
    }

    public static PathScanReport scanPath(
            Level level,
            LocalPlayer player,
            Vec3 stepVector,
            String directionKey,
            Config.DirectionalPathScanner config
    ) {
        if (level == null || player == null || stepVector == null) {
            return new PathScanReport(directionKey, "", 0, 0, List.of(), false);
        }

        int maxRange = Math.max(1, config.scanRange);
        boolean isDetailed = (config.verbosityMode == Config.DirectionalPathScanner.VerbosityMode.DETAILED);

        Vec3 playerPos = player.position();
        int originX = (int) Math.floor(playerPos.x);
        int originZ = (int) Math.floor(playerPos.z);
        double feetY = playerPos.y;
        int currentGroundY = (int) Math.floor(feetY - 0.05);

        // Get initial ground block under player
        BlockPos initialGroundPos = new BlockPos(originX, currentGroundY, originZ);
        String primaryGroundName = getBlockDisplayName(level, initialGroundPos);

        List<PathScanReport.PathScanEvent> events = new ArrayList<>();
        int initialFreeDistance = 0;
        boolean firstObstacleEncountered = false;
        boolean reachedEnd = true;

        int prevX = originX;
        int prevZ = originZ;
        int lastNotedElevationY = currentGroundY;

        for (int d = 1; d <= maxRange; d++) {
            int targetX = (int) Math.round(originX + stepVector.x * d);
            int targetZ = (int) Math.round(originZ + stepVector.z * d);

            // Check corner pinching if moving diagonally
            boolean isDiagonal = (stepVector.x != 0 && stepVector.z != 0);
            if (isDiagonal && d > 0) {
                int cornerX = targetX;
                int cornerZ = prevZ;
                int cornerAltX = prevX;
                int cornerAltZ = targetZ;

                BlockPos c1Foot = new BlockPos(cornerX, currentGroundY + 1, cornerZ);
                BlockPos c1Head = c1Foot.above();
                BlockPos c2Foot = new BlockPos(cornerAltX, currentGroundY + 1, cornerAltZ);
                BlockPos c2Head = c2Foot.above();

                boolean c1Blocked = isSolidBlocking(level, c1Foot) || isSolidBlocking(level, c1Head);
                boolean c2Blocked = isSolidBlocking(level, c2Foot) || isSolidBlocking(level, c2Head);

                if (c1Blocked && c2Blocked) {
                    events.add(new PathScanReport.PathScanEvent(
                            PathScanReport.EventType.OBSTACLE_BLOCK,
                            "pinch_gap",
                            d,
                            currentGroundY + 1,
                            0
                    ));
                    firstObstacleEncountered = true;
                    reachedEnd = false;
                    if (!isDetailed) {
                        break;
                    }
                }
            }

            // Probe vertical ground elevation
            BlockPos groundCandidate = new BlockPos(targetX, currentGroundY, targetZ);
            BlockPos stepUpCandidate = groundCandidate.above(); // +1 step
            BlockPos stepDownCandidate = groundCandidate.below(); // -1 step

            boolean stepUpSolid = isWalkableGround(level, stepUpCandidate);
            boolean groundSolid = isWalkableGround(level, groundCandidate);
            boolean stepDownSolid = isWalkableGround(level, stepDownCandidate);

            int targetGroundY = currentGroundY;
            boolean validStep = false;

            if (stepUpSolid) {
                BlockPos head1 = stepUpCandidate.above(1);
                BlockPos head2 = stepUpCandidate.above(2);
                if (!isSolidBlocking(level, head1) && !isSolidBlocking(level, head2)) {
                    targetGroundY = currentGroundY + 1;
                    validStep = true;
                }
            } else if (groundSolid) {
                BlockPos head1 = groundCandidate.above(1);
                BlockPos head2 = groundCandidate.above(2);
                if (!isSolidBlocking(level, head1) && !isSolidBlocking(level, head2)) {
                    targetGroundY = currentGroundY;
                    validStep = true;
                }
            } else if (stepDownSolid) {
                BlockPos head1 = stepDownCandidate.above(1);
                BlockPos head2 = stepDownCandidate.above(2);
                if (!isSolidBlocking(level, head1) && !isSolidBlocking(level, head2)) {
                    targetGroundY = currentGroundY - 1;
                    validStep = true;
                }
            }

            if (!validStep) {
                // Solid obstacle at foot or head level
                BlockPos feetPos = new BlockPos(targetX, currentGroundY + 1, targetZ);
                BlockPos headPos = new BlockPos(targetX, currentGroundY + 2, targetZ);

                if (isSolidBlocking(level, feetPos) || isSolidBlocking(level, headPos)) {
                    BlockState obstacleState = isSolidBlocking(level, feetPos)
                            ? level.getBlockState(feetPos)
                            : level.getBlockState(headPos);

                    if (config.detectObstacles) {
                        events.add(new PathScanReport.PathScanEvent(
                                PathScanReport.EventType.OBSTACLE_BLOCK,
                                obstacleState.getBlock().getName().getString(),
                                d,
                                isSolidBlocking(level, feetPos) ? currentGroundY + 1 : currentGroundY + 2,
                                0
                        ));
                    }
                    firstObstacleEncountered = true;
                    reachedEnd = false;
                    if (!isDetailed) {
                        break;
                    }
                } else if (!stepUpSolid && !groundSolid && !stepDownSolid) {
                    // Drop / pit detection
                    int dropDepth = 2;
                    int findGroundY = currentGroundY - 2;
                    while (findGroundY >= level.getMinY()) {
                        BlockPos check = new BlockPos(targetX, findGroundY, targetZ);
                        if (isWalkableGround(level, check)) {
                            break;
                        }
                        dropDepth++;
                        findGroundY--;
                    }

                    if (config.detectDrops && dropDepth >= config.dropWarningDepth) {
                        events.add(new PathScanReport.PathScanEvent(
                                PathScanReport.EventType.DROP_HAZARD,
                                "drop",
                                d,
                                findGroundY,
                                dropDepth
                        ));
                    }
                    firstObstacleEncountered = true;
                    reachedEnd = false;
                    if (!isDetailed) {
                        break;
                    }
                }
            } else {
                // Elevation change event in detailed mode
                if (targetGroundY != lastNotedElevationY) {
                    String stepGroundName = getBlockDisplayName(level, new BlockPos(targetX, targetGroundY, targetZ));
                    events.add(new PathScanReport.PathScanEvent(
                            PathScanReport.EventType.ELEVATION_CHANGE,
                            stepGroundName,
                            d,
                            targetGroundY + 1,
                            targetGroundY - currentGroundY
                    ));
                    lastNotedElevationY = targetGroundY;
                }
                currentGroundY = targetGroundY;
            }

            // Update primary ground name if it was air/empty initially
            if (primaryGroundName.isEmpty() || primaryGroundName.equalsIgnoreCase("Aria")) {
                primaryGroundName = getBlockDisplayName(level, new BlockPos(targetX, currentGroundY, targetZ));
            }

            BlockPos currentFeetPos = new BlockPos(targetX, currentGroundY + 1, targetZ);

            // Check crops and food plants at feet level
            if (config.detectItems) {
                BlockState feetState = level.getBlockState(currentFeetPos);
                if (!feetState.isAir()) {
                    String foodName = getCropOrFoodDisplayName(feetState);
                    if (foodName != null && !foodName.isBlank()) {
                        events.add(new PathScanReport.PathScanEvent(
                                PathScanReport.EventType.ITEM_RESOURCE,
                                foodName,
                                d,
                                currentGroundY + 1,
                                0
                        ));
                    }
                }
            }

            // Check fluids

            if (config.detectFluids) {
                FluidState fluidState = level.getFluidState(currentFeetPos);
                if (!fluidState.isEmpty()) {
                    String fluidName = fluidState.is(Fluids.LAVA) || fluidState.is(Fluids.FLOWING_LAVA)
                            ? "lava"
                            : "water";
                    events.add(new PathScanReport.PathScanEvent(
                            PathScanReport.EventType.FLUID,
                            fluidName,
                            d,
                            currentGroundY + 1,
                            0
                    ));
                }
            }

            // Check entities with downward-expanded AABB to catch items on slabs/paths
            AABB blockBox = new AABB(
                    targetX - 0.1, currentGroundY - 0.2, targetZ - 0.1,
                    targetX + 1.1, currentGroundY + 2.8, targetZ + 1.1
            );

            List<Entity> entities = level.getEntitiesOfClass(Entity.class, blockBox, e -> e != player && e.isAlive());
            for (Entity entity : entities) {
                if (entity instanceof ItemEntity itemEntity && config.detectItems) {
                    String itemName = itemEntity.getItem().getHoverName().getString();
                    events.add(new PathScanReport.PathScanEvent(
                            PathScanReport.EventType.ITEM_RESOURCE,
                            itemName,
                            d,
                            currentGroundY + 1,
                            0
                    ));
                } else if (entity instanceof Enemy && config.detectHostileMobs) {
                    String mobName = entity.getType().getDescription().getString();
                    events.add(new PathScanReport.PathScanEvent(
                            PathScanReport.EventType.HOSTILE_MOB,
                            mobName,
                            d,
                            currentGroundY + 1,
                            0
                    ));
                } else if (entity instanceof LivingEntity && !(entity instanceof Player) && config.detectPassiveMobs) {
                    String mobName = entity.getType().getDescription().getString();
                    events.add(new PathScanReport.PathScanEvent(
                            PathScanReport.EventType.PASSIVE_MOB,
                            mobName,
                            d,
                            currentGroundY + 1,
                            0
                    ));
                }
            }

            if (!firstObstacleEncountered) {
                initialFreeDistance = d;
            }

            prevX = targetX;
            prevZ = targetZ;
        }

        return new PathScanReport(directionKey, primaryGroundName, initialFreeDistance, maxRange, events, reachedEnd);
    }

    public static String getBlockDisplayName(Level level, BlockPos pos) {
        if (level == null || pos == null) return "";
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return "";
        return state.getBlock().getName().getString();
    }

    public static boolean isWalkableGround(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        if (collisionShape.isEmpty()) return false;
        double height = collisionShape.max(Direction.Axis.Y);
        return height > 0.0;
    }

    public static boolean isSolidBlocking(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        // Allow open doors and trapdoors
        if (state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.OPEN) && state.getValue(DoorBlock.OPEN)) {
            return false;
        }
        if (state.getBlock() instanceof TrapDoorBlock && state.hasProperty(TrapDoorBlock.OPEN) && state.getValue(TrapDoorBlock.OPEN)) {
            return false;
        }

        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        return !collisionShape.isEmpty();
    }

    public static String getCropOrFoodDisplayName(BlockState state) {
        if (state == null || state.isAir()) return null;
        var block = state.getBlock();
        if (block instanceof CropBlock cropBlock) {
            String name = block.getName().getString();
            return cropBlock.isMaxAge(state) ? name : name + " (in crescita)";
        }
        if (block instanceof SweetBerryBushBlock) {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            String name = block.getName().getString();
            return age >= 2 ? name : name + " (senza bacche)";
        }
        if (block instanceof CocoaBlock) {
            String name = block.getName().getString();
            int age = state.getValue(CocoaBlock.AGE);
            return age >= 2 ? name : name + " (in crescita)";
        }
        if (block == Blocks.MELON || block == Blocks.PUMPKIN || block == Blocks.CARVED_PUMPKIN
                || block instanceof SugarCaneBlock || block == Blocks.CACTUS
                || block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM) {
            return block.getName().getString();
        }
        return null;
    }
}

