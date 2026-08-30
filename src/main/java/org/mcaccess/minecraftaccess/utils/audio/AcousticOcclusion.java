package org.mcaccess.minecraftaccess.utils.audio;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Utility for calculating progressive material-based acoustic occlusion in 3D voxel space.
 */
@Slf4j
public final class AcousticOcclusion {
    public static final float MIN_VOLUME_FLOOR = 0.01f;
    public static final float OCCLUSION_THRESHOLD = 0.20f;

    private AcousticOcclusion() {
    }

    /**
     * Calculates the acoustic absorption factor for a given block state.
     *
     * @param state The block state to evaluate.
     * @return Attenuation coefficient between 0.0f (transparent) and 0.50f (ultra-dense).
     */
    public static float calculateBlockAttenuation(BlockState state) {
        if (state == null || state.isAir()) {
            return 0.0f;
        }

        // Tier 5: Ultra-Dense Heavy Rock & Metals (-50%)
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.BEDROCK) || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return 0.50f;
        }

        // Tier 4: Stone, Bricks, Cobblestone, Metals (-38%)
        if (state.is(BlockTags.STONE_BRICKS) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BEACON_BASE_BLOCKS) || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.BASE_STONE_NETHER)) {
            return 0.38f;
        }

        // Tier 3: Solid Wood Logs, Stems, Bark blocks (-28%)
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.OVERWORLD_NATURAL_LOGS)) {
            return 0.28f;
        }

        // Tier 1: Light Wood, Doors, Trapdoors, Slabs, Fences, Glass, Leaves (-10%)
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.ALL_SIGNS) || state.getBlock() instanceof IronBarsBlock
                || state.is(BlockTags.IMPERMEABLE)) {
            return 0.10f;
        }

        // Tier 2: Processed Wood (Planks, Stairs, Chests, Dirt, Wool) (-18%)
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.DIRT) || state.is(BlockTags.WOOL)
                || state.is(BlockTags.SHULKER_BOXES)) {
            return 0.18f;
        }

        // Default fallback for any other solid/collidable block
        if (state.blocksMotion()) {
            return 0.35f;
        }

        return 0.0f;
    }

    /**
     * Calculates the total acoustic occlusion between two positions in the level.
     *
     * @param from  Starting position (e.g. player eye position).
     * @param to    Target position (e.g. entity or block position).
     * @param level The active level/world.
     * @return Accumulated occlusion factor (0.0f = clear line of sight, >= 1.0f = fully occluded).
     */
    public static float calculateTotalOcclusion(Vec3 from, Vec3 to, Level level) {
        if (level == null || from == null || to == null) {
            return 0.0f;
        }

        double distance = from.distanceTo(to);
        if (distance < 0.8) {
            return 0.0f;
        }

        BlockPos startPos = BlockPos.containing(from);
        BlockPos endPos = BlockPos.containing(to);

        Vec3 dir = to.subtract(from).normalize();
        double stepSize = 0.5;
        int steps = (int) Math.ceil(distance / stepSize);

        Set<BlockPos> checkedPositions = new HashSet<>();
        float totalOcclusion = 0.0f;

        for (int i = 1; i < steps; i++) {
            Vec3 samplePoint = from.add(dir.scale(i * stepSize));
            BlockPos pos = BlockPos.containing(samplePoint);

            if (pos.equals(startPos) || pos.equals(endPos)) {
                continue;
            }

            if (checkedPositions.add(pos)) {
                BlockState state = level.getBlockState(pos);
                float attenuation = calculateBlockAttenuation(state);
                totalOcclusion += attenuation;
                if (totalOcclusion >= 1.0f) {
                    break;
                }
            }
        }

        return totalOcclusion;
    }

    /**
     * Calculates the scaled volume multiplier considering material-based occlusion and the minimum floor.
     *
     * @param from  Player eye position.
     * @param to    Target position.
     * @param level The active level.
     * @return Volume multiplier between MIN_VOLUME_FLOOR (0.01f) and 1.0f.
     */
    public static float getVolumeMultiplier(Vec3 from, Vec3 to, Level level) {
        float totalOcclusion = calculateTotalOcclusion(from, to, level);
        if (totalOcclusion <= 0.0f) {
            return 1.0f;
        }
        return Math.max(MIN_VOLUME_FLOOR, 1.0f - totalOcclusion);
    }

    /**
     * Determines whether the line of sight is significantly occluded by solid obstacles.
     *
     * @param from  Player eye position.
     * @param to    Target position.
     * @param level The active level.
     * @return true if total occlusion exceeds OCCLUSION_THRESHOLD (0.20f).
     */
    public static boolean isOccluded(Vec3 from, Vec3 to, Level level) {
        return calculateTotalOcclusion(from, to, level) >= OCCLUSION_THRESHOLD;
    }
}
