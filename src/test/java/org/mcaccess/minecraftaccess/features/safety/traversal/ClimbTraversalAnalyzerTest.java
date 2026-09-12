package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClimbTraversalAnalyzerTest {

    @BeforeAll
    static void initBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("ClimbableGeometry: classificazione ladder, scaffolding e free climbable")
    void testClimbableClassification() {
        BlockState ladder = Blocks.LADDER.defaultBlockState();
        BlockState vine = Blocks.VINE.defaultBlockState();
        BlockState scaffolding = Blocks.SCAFFOLDING.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();

        assertEquals(ClimbableGeometry.ClimbType.WALL_MOUNTED, ClimbableGeometry.getClimbType(ladder));
        assertEquals(ClimbableGeometry.ClimbType.WALL_MOUNTED, ClimbableGeometry.getClimbType(vine));
        assertEquals(ClimbableGeometry.ClimbType.SCAFFOLDING, ClimbableGeometry.getClimbType(scaffolding));
        assertNull(ClimbableGeometry.getClimbType(air));
        assertNull(ClimbableGeometry.getClimbType(stone));

        assertTrue(ClimbableGeometry.isClimbable(ladder));
        assertTrue(ClimbableGeometry.isClimbable(scaffolding));
        assertFalse(ClimbableGeometry.isClimbable(stone));
    }

    @Test
    @DisplayName("ClimbableGeometry: risoluzione wall support facing opposto a ladder FACING")
    void testWallSupportFacings() {
        BlockState ladderNorth = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        List<Direction> supportNorth = ClimbableGeometry.getWallSupportFacings(ladderNorth);
        assertEquals(1, supportNorth.size());
        assertEquals(Direction.SOUTH, supportNorth.get(0));

        BlockState ladderEast = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        assertEquals(Direction.WEST, ClimbableGeometry.getBestWallSupportFacing(ladderEast, null));

        BlockState scaffolding = Blocks.SCAFFOLDING.defaultBlockState();
        assertTrue(ClimbableGeometry.getWallSupportFacings(scaffolding).isEmpty());
        assertNull(ClimbableGeometry.getBestWallSupportFacing(scaffolding, null));
    }

    @Test
    @DisplayName("ClimbTraversal: calcolo altezza e helper di ascesa/discesa")
    void testClimbTraversalRecord() {
        BlockPos bottom = new BlockPos(10, 64, 10);
        BlockPos top = new BlockPos(10, 70, 10);
        BlockPos landing = new BlockPos(11, 71, 10);

        ClimbTraversal ascent = ClimbTraversal.of(
                Direction.AxisDirection.POSITIVE,
                ClimbableGeometry.ClimbType.WALL_MOUNTED,
                bottom,
                top,
                bottom,
                landing,
                Direction.SOUTH,
                null
        );

        assertTrue(ascent.isAscent());
        assertFalse(ascent.isDescent());
        assertEquals(7, ascent.getHeight());
        assertTrue(ascent.columnId().contains("10,64..70,10"));

        ClimbTraversal descent = ClimbTraversal.of(
                Direction.AxisDirection.NEGATIVE,
                ClimbableGeometry.ClimbType.SCAFFOLDING,
                top,
                top,
                bottom,
                bottom,
                null,
                null
        );

        assertFalse(descent.isAscent());
        assertTrue(descent.isDescent());
        assertEquals(7, descent.getHeight());
    }

    @Test
    void testLadderGeometry() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState state = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, dir);
            System.out.println("LADDER FACING=" + dir + " shape=" + state.getShape(null, BlockPos.ZERO));
        }
    }
}
