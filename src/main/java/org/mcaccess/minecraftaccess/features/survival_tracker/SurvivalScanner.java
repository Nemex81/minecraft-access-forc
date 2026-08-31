package org.mcaccess.minecraftaccess.features.survival_tracker;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;
import org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils;

public class SurvivalScanner {

    public static boolean hasExposedAirFace(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        return level.getBlockState(pos.above()).isAir()
                || level.getBlockState(pos.below()).isAir()
                || level.getBlockState(pos.north()).isAir()
                || level.getBlockState(pos.south()).isAir()
                || level.getBlockState(pos.east()).isAir()
                || level.getBlockState(pos.west()).isAir();
    }

    public static boolean isWoodBlock(BlockState state) {
        if (state == null || state.isAir()) return false;
        Block block = state.getBlock();
        if (block == Blocks.CRAFTING_TABLE) return true;
        if (state.is(BlockTags.LOGS)) return true;
        return state.is(BlockTags.PLANKS);
    }

    public static boolean isStoneBlock(BlockState state) {
        if (state == null || state.isAir()) return false;
        Block block = state.getBlock();
        return block == Blocks.STONE
                || block == Blocks.COBBLESTONE
                || block == Blocks.DEEPSLATE
                || block == Blocks.COBBLED_DEEPSLATE
                || block == Blocks.ANDESITE
                || block == Blocks.DIORITE
                || block == Blocks.GRANITE
                || block == Blocks.SANDSTONE
                || block == Blocks.RED_SANDSTONE
                || block == Blocks.TUFF
                || block == Blocks.CALCITE
                || block == Blocks.BLACKSTONE
                || block == Blocks.BASALT;
    }

    public static boolean isFoodBlock(BlockState state) {
        if (state == null || state.isAir()) return false;
        Block block = state.getBlock();
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        return block == Blocks.MELON;
    }

    public static boolean isFoodEntity(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;
        if (entity instanceof Player || entity instanceof Enemy) return false;

        if (entity instanceof Animal || entity instanceof AgeableMob || entity instanceof WaterAnimal) {
            return true;
        }

        if (entity instanceof ItemEntity itemEntity) {
            return itemEntity.getItem().has(DataComponents.FOOD);
        }

        return false;
    }

    public Map<SurvivalResourceType, SurvivalResourceTarget> scan(Level level, LocalPlayer player, int range) {
        Map<SurvivalResourceType, SurvivalResourceTarget> results = new HashMap<>();
        if (level == null || player == null) return results;

        BlockPos playerPos = player.blockPosition();
        Vec3 playerVec = player.position();
        float playerHeading = PlayerPositionUtils.getCompassDegrees();

        BlockPos nearestWoodPos = null;
        double minWoodDistSq = Double.MAX_VALUE;

        BlockPos nearestStonePos = null;
        double minStoneDistSq = Double.MAX_VALUE;

        BlockPos nearestFoodBlockPos = null;
        double minFoodBlockDistSq = Double.MAX_VALUE;

        int r = Math.max(1, range);
        int rSq = r * r;

        // Voxel Scan around player within range
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int distHorizSq = dx * dx + dz * dz;
                if (distHorizSq > rSq) continue;

                for (int dy = -r; dy <= r; dy++) {
                    int dist3DSq = distHorizSq + dy * dy;
                    if (dist3DSq > rSq) continue;

                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;

                    double dSq = playerVec.distanceToSqr(Vec3.atCenterOf(pos));

                    // Check Wood
                    if (isWoodBlock(state)) {
                        if (dSq < minWoodDistSq) {
                            minWoodDistSq = dSq;
                            nearestWoodPos = pos;
                        }
                    }

                    // Check Stone (only if has exposed air face)
                    if (isStoneBlock(state) && hasExposedAirFace(level, pos)) {
                        if (dSq < minStoneDistSq) {
                            minStoneDistSq = dSq;
                            nearestStonePos = pos;
                        }
                    }

                    // Check Food Blocks (Crops, berries, melons)
                    if (isFoodBlock(state)) {
                        if (dSq < minFoodBlockDistSq) {
                            minFoodBlockDistSq = dSq;
                            nearestFoodBlockPos = pos;
                        }
                    }
                }
            }
        }

        // Entity Scan for Food (Passive animals, dropped food)
        Entity nearestFoodEntity = null;
        double minFoodEntityDistSq = Double.MAX_VALUE;

        AABB box = new AABB(playerPos).inflate(r);
        for (Entity entity : level.getEntities((Entity) null, box, SurvivalScanner::isFoodEntity)) {
            double dSq = playerVec.distanceToSqr(entity.position());
            if (dSq <= rSq && dSq < minFoodEntityDistSq) {
                minFoodEntityDistSq = dSq;
                nearestFoodEntity = entity;
            }
        }

        // Process Wood
        if (nearestWoodPos != null) {
            results.put(SurvivalResourceType.WOOD, buildTarget(
                    SurvivalResourceType.WOOD,
                    nearestWoodPos,
                    null,
                    I18n.get("minecraft_access.survival_tracker.wood"),
                    playerVec,
                    playerPos,
                    playerHeading
            ));
        }

        // Process Stone
        if (nearestStonePos != null) {
            results.put(SurvivalResourceType.STONE, buildTarget(
                    SurvivalResourceType.STONE,
                    nearestStonePos,
                    null,
                    I18n.get("minecraft_access.survival_tracker.stone"),
                    playerVec,
                    playerPos,
                    playerHeading
            ));
        }

        // Process Food (compare closest food block vs closest food entity)
        if (nearestFoodEntity != null && (nearestFoodBlockPos == null || minFoodEntityDistSq <= minFoodBlockDistSq)) {
            results.put(SurvivalResourceType.FOOD, buildTarget(
                    SurvivalResourceType.FOOD,
                    null,
                    nearestFoodEntity,
                    I18n.get("minecraft_access.survival_tracker.food"),
                    playerVec,
                    playerPos,
                    playerHeading
            ));
        } else if (nearestFoodBlockPos != null) {
            results.put(SurvivalResourceType.FOOD, buildTarget(
                    SurvivalResourceType.FOOD,
                    nearestFoodBlockPos,
                    null,
                    I18n.get("minecraft_access.survival_tracker.food"),
                    playerVec,
                    playerPos,
                    playerHeading
            ));
        }

        return results;
    }

    public static SurvivalResourceTarget buildTarget(
            SurvivalResourceType type,
            @Nullable BlockPos blockPos,
            @Nullable Entity entity,
            String name,
            Vec3 playerVec,
            BlockPos playerPos,
            float playerHeading
    ) {
        Vec3 targetVec = blockPos != null ? Vec3.atCenterOf(blockPos) : entity.position();
        double distance = playerVec.distanceTo(targetVec);
        int targetY = blockPos != null ? blockPos.getY() : (int) Math.floor(entity.getY());
        int diffY = targetY - playerPos.getY();

        double dx = targetVec.x - playerVec.x;
        double dz = targetVec.z - playerVec.z;

        // Geographic compass angle to target (0 = North, 90 = East, 180 = South, 270 = West)
        double targetCompassDeg = (Math.toDegrees(Math.atan2(dx, -dz)) + 360.0) % 360.0;
        int targetCompassDegInt = (int) Math.round(targetCompassDeg) % 360;

        String cardinalName = getCardinalDirectionName(targetCompassDegInt);
        String compassString = cardinalName + " " + I18n.get("minecraft_access.survival_tracker.at_degrees", NarrationUtils.narrateNumber(targetCompassDegInt));

        // Relative direction from player view heading
        double relAngle = (targetCompassDeg - playerHeading + 360.0) % 360.0;
        String relDirectionKey = getRelativeDirectionKey(relAngle);
        int distInt = Math.max(1, (int) Math.round(distance));
        String distanceString = formatDistanceWithUnits(distInt);
        String relativeString = distanceString + " " + I18n.get(relDirectionKey);

        // Altitude difference
        String altitudeString;
        if (diffY == 0) {
            altitudeString = I18n.get("minecraft_access.survival_tracker.same_level");
        } else if (diffY > 0) {
            String units = formatDistanceWithUnits(diffY);
            altitudeString = I18n.get("minecraft_access.survival_tracker.altitude_up", units);
        } else {
            String units = formatDistanceWithUnits(Math.abs(diffY));
            altitudeString = I18n.get("minecraft_access.survival_tracker.altitude_down", units);
        }

        return new SurvivalResourceTarget(
                type,
                blockPos,
                entity,
                name,
                distance,
                diffY,
                relativeString,
                compassString,
                altitudeString
        );
    }

    public static String formatDistanceWithUnits(int dist) {
        if (dist == 1) {
            return I18n.get("minecraft_access.survival_tracker.single_block");
        } else {
            return I18n.get("minecraft_access.survival_tracker.plural_blocks", dist);
        }
    }

    public static String getCardinalDirectionName(int deg) {
        int normalised = ((deg % 360) + 360) % 360;
        if (normalised >= 338 || normalised < 23) {
            return I18n.get("minecraft_access.direction.north");
        } else if (normalised < 68) {
            return I18n.get("minecraft_access.direction.north_east");
        } else if (normalised < 113) {
            return I18n.get("minecraft_access.direction.east");
        } else if (normalised < 158) {
            return I18n.get("minecraft_access.direction.south_east");
        } else if (normalised < 203) {
            return I18n.get("minecraft_access.direction.south");
        } else if (normalised < 248) {
            return I18n.get("minecraft_access.direction.south_west");
        } else if (normalised < 293) {
            return I18n.get("minecraft_access.direction.west");
        } else {
            return I18n.get("minecraft_access.direction.north_west");
        }
    }

    public static String getRelativeDirectionKey(double relAngle) {
        double a = ((relAngle % 360.0) + 360.0) % 360.0;
        if (a >= 337.5 || a < 22.5) {
            return "minecraft_access.survival_tracker.rel_forward";
        } else if (a < 67.5) {
            return "minecraft_access.survival_tracker.rel_forward_right";
        } else if (a < 112.5) {
            return "minecraft_access.survival_tracker.rel_right";
        } else if (a < 157.5) {
            return "minecraft_access.survival_tracker.rel_back_right";
        } else if (a < 202.5) {
            return "minecraft_access.survival_tracker.rel_back";
        } else if (a < 247.5) {
            return "minecraft_access.survival_tracker.rel_back_left";
        } else if (a < 292.5) {
            return "minecraft_access.survival_tracker.rel_left";
        } else {
            return "minecraft_access.survival_tracker.rel_forward_left";
        }
    }
}
