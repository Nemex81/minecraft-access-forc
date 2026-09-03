package org.mcaccess.minecraftaccess.features.safety.traversal;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Traversal Safety Analyzer Pure Geometric Unit Tests (Rev MC-26.8)")
class TraversalSafetyAnalyzerTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static class TestBlockGetter implements BlockGetter {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final Map<BlockPos, FluidState> fluids = new HashMap<>();

        void set(BlockPos pos, BlockState state) {
            blocks.put(pos.immutable(), state);
        }

        void setFluid(BlockPos pos, FluidState fluid) {
            fluids.put(pos.immutable(), fluid);
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos blockPos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos blockPos) {
            return blocks.getOrDefault(blockPos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos blockPos) {
            return fluids.getOrDefault(blockPos, Fluids.EMPTY.defaultFluidState());
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    }

    @Test
    @DisplayName("1. Real roof regression: roof at Y=68, wall ladder Y=67..64, ground at Y=63 -> SAFE_DESCENT_AVAILABLE")
    void testRealRoofRegressionSafeDescent() {
        TestBlockGetter level = new TestBlockGetter();

        // Player standing on stone roof at (10, 68, 5)
        level.set(new BlockPos(10, 67, 5), Blocks.STONE.defaultBlockState());
        level.set(new BlockPos(10, 68, 5), Blocks.AIR.defaultBlockState());

        // Wall ladder at (10, Y, 4) attached to stone wall at (10, Y, 5) facing NORTH
        BlockState ladderState = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        for (int y = 64; y <= 67; y++) {
            level.set(new BlockPos(10, y, 5), Blocks.STONE.defaultBlockState()); // Solid wall
            level.set(new BlockPos(10, y, 4), ladderState); // Ladder rungs
        }

        // Solid landing at Y=63
        level.set(new BlockPos(10, 63, 4), Blocks.STONE.defaultBlockState());

        Vec3 playerPos = new Vec3(10.5, 68.0, 5.2);
        AABB bbox = new AABB(10.2, 68.0, 4.9, 10.8, 69.8, 5.5);
        Vec3 intentNorth = new Vec3(0.0, 0.0, -1.0); // Walking forward towards the ladder at Z=4

        TraversalSafetyContext context = new TraversalSafetyContext(
                playerPos, bbox, 68, intentNorth, true, 3, level
        );

        TraversalSafetyResult result = TraversalSafetyAnalyzer.analyzeTraversal(context);

        assertEquals(TraversalSafetyStatus.SAFE_DESCENT_AVAILABLE, result.status());
        assertNotNull(result.candidate());
        assertEquals(SafeDescentType.LADDER, result.candidate().type());
        assertEquals(Direction.NORTH, result.candidate().wallFacing());
        assertEquals(new BlockPos(10, 67, 4), result.candidate().columnTopPos());
        assertEquals(new BlockPos(10, 63, 4), result.candidate().landingPos());
    }

    @Test
    @DisplayName("2. Identical roof without ladder drops 4 blocks into danger -> DANGEROUS_DROP")
    void testRoofWithoutLadderDangerousDrop() {
        TestBlockGetter level = new TestBlockGetter();

        // Player on roof at (10, 68, 5)
        level.set(new BlockPos(10, 67, 5), Blocks.STONE.defaultBlockState());
        // Solid ground 4 blocks below at Y=63
        level.set(new BlockPos(10, 63, 4), Blocks.STONE.defaultBlockState());

        Vec3 playerPos = new Vec3(10.5, 68.0, 5.2);
        AABB bbox = new AABB(10.2, 68.0, 4.9, 10.8, 69.8, 5.5);
        Vec3 intentNorth = new Vec3(0.0, 0.0, -1.0);

        TraversalSafetyContext context = new TraversalSafetyContext(
                playerPos, bbox, 68, intentNorth, true, 3, level
        );

        TraversalSafetyResult result = TraversalSafetyAnalyzer.analyzeTraversal(context);

        assertEquals(TraversalSafetyStatus.DANGEROUS_DROP, result.status());
        assertNull(result.candidate());
        assertNotNull(result.dangerPos());
        assertTrue(result.dangerDepth() >= 3);
    }

    @Test
    @DisplayName("3. Broken ladder terminating 5 blocks above ground -> AMBIGUOUS_OR_UNSAFE_DESCENT")
    void testBrokenLadderUnsafeDescent() {
        TestBlockGetter level = new TestBlockGetter();

        // Roof at Y=68
        level.set(new BlockPos(10, 67, 5), Blocks.STONE.defaultBlockState());

        // Ladder exists ONLY at Y=67 and Y=66, then broken! Ground is at Y=60 (drop of 6 blocks!)
        BlockState ladderState = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        level.set(new BlockPos(10, 67, 4), ladderState);
        level.set(new BlockPos(10, 66, 4), ladderState);
        level.set(new BlockPos(10, 60, 4), Blocks.STONE.defaultBlockState());

        Vec3 playerPos = new Vec3(10.5, 68.0, 5.2);
        AABB bbox = new AABB(10.2, 68.0, 4.9, 10.8, 69.8, 5.5);
        Vec3 intentNorth = new Vec3(0.0, 0.0, -1.0);

        TraversalSafetyContext context = new TraversalSafetyContext(
                playerPos, bbox, 68, intentNorth, true, 3, level
        );

        TraversalSafetyResult result = TraversalSafetyAnalyzer.analyzeTraversal(context);

        assertEquals(TraversalSafetyStatus.AMBIGUOUS_OR_UNSAFE_DESCENT, result.status(),
                "Broken ladder dropping into abyss must not be authorized as safe descent");
    }

    @Test
    @DisplayName("4. No horizontal movement intent -> NOT_APPLICABLE")
    void testNoMovementIntentNotApplicable() {
        TestBlockGetter level = new TestBlockGetter();

        TraversalSafetyContext context = new TraversalSafetyContext(
                new Vec3(10.0, 68.0, 5.0), new AABB(9.7, 68.0, 4.7, 10.3, 69.8, 5.3), 68, null, false, 3, level
        );

        TraversalSafetyResult result = TraversalSafetyAnalyzer.analyzeTraversal(context);

        assertEquals(TraversalSafetyStatus.NOT_APPLICABLE, result.status());
    }

    @Test
    @DisplayName("5. Safe water pool below edge -> SAFE_DESCENT_AVAILABLE with WATER_DESCENT")
    void testWaterDescent() {
        TestBlockGetter level = new TestBlockGetter();

        level.set(new BlockPos(10, 67, 5), Blocks.STONE.defaultBlockState());
        // Water pool at Y=64
        level.setFluid(new BlockPos(10, 64, 4), Fluids.WATER.defaultFluidState());
        level.set(new BlockPos(10, 63, 4), Blocks.STONE.defaultBlockState());

        Vec3 playerPos = new Vec3(10.5, 68.0, 5.2);
        AABB bbox = new AABB(10.2, 68.0, 4.9, 10.8, 69.8, 5.5);
        Vec3 intentNorth = new Vec3(0.0, 0.0, -1.0);

        TraversalSafetyContext context = new TraversalSafetyContext(
                playerPos, bbox, 68, intentNorth, true, 3, level
        );

        TraversalSafetyResult result = TraversalSafetyAnalyzer.analyzeTraversal(context);

        assertEquals(TraversalSafetyStatus.SAFE_DESCENT_AVAILABLE, result.status());
        assertNotNull(result.candidate());
        assertEquals(SafeDescentType.WATER_DESCENT, result.candidate().type());
    }

    @Test
    @DisplayName("6. Lava pool below edge is NEVER safe -> DANGEROUS_DROP")
    void testLavaPoolIsNeverSafe() {
        TestBlockGetter level = new TestBlockGetter();

        level.set(new BlockPos(10, 67, 5), Blocks.STONE.defaultBlockState());
        // Lava pool at Y=64
        level.setFluid(new BlockPos(10, 64, 4), Fluids.LAVA.defaultFluidState());

        Vec3 playerPos = new Vec3(10.5, 68.0, 5.2);
        AABB bbox = new AABB(10.2, 68.0, 4.9, 10.8, 69.8, 5.5);
        Vec3 intentNorth = new Vec3(0.0, 0.0, -1.0);

        TraversalSafetyContext context = new TraversalSafetyContext(
                playerPos, bbox, 68, intentNorth, true, 3, level
        );

        TraversalSafetyResult result = TraversalSafetyAnalyzer.analyzeTraversal(context);

        assertEquals(TraversalSafetyStatus.DANGEROUS_DROP, result.status(),
                "Lava pool must NEVER be classified as safe landing");
    }

    @Test
    @DisplayName("7. Open trapdoor over continuous ladder -> SAFE_DESCENT_AVAILABLE")
    void testOpenTrapdoorOverLadder() {
        TestBlockGetter level = new TestBlockGetter();

        // Player at (10, 68, 5). Open trapdoor at (10, 68, 4)
        level.set(new BlockPos(10, 67, 5), Blocks.STONE.defaultBlockState());
        BlockState trapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);
        level.set(new BlockPos(10, 68, 4), trapdoor);

        // Ladder under trapdoor from Y=67 down to Y=64, ground at Y=63
        BlockState ladderState = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        for (int y = 64; y <= 67; y++) {
            level.set(new BlockPos(10, y, 4), ladderState);
        }
        level.set(new BlockPos(10, 63, 4), Blocks.STONE.defaultBlockState());

        Vec3 playerPos = new Vec3(10.5, 68.0, 5.2);
        AABB bbox = new AABB(10.2, 68.0, 4.9, 10.8, 69.8, 5.5);
        Vec3 intentNorth = new Vec3(0.0, 0.0, -1.0);

        TraversalSafetyContext context = new TraversalSafetyContext(
                playerPos, bbox, 68, intentNorth, true, 3, level
        );

        TraversalSafetyResult result = TraversalSafetyAnalyzer.analyzeTraversal(context);

        assertEquals(TraversalSafetyStatus.SAFE_DESCENT_AVAILABLE, result.status());
        assertNotNull(result.candidate());
        assertEquals(SafeDescentType.LADDER, result.candidate().type());
    }
}
