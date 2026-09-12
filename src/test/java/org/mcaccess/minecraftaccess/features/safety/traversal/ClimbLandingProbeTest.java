package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClimbLandingProbeTest {

    @Test
    @DisplayName("D27: quattro campioni inset richiedono supporto per l'intero footprint")
    void requiresAllFourFootprintSamples() {
        Level level = mock(Level.class);
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(invocation -> {
            BlockPos pos = invocation.getArgument(0);
            return pos.equals(new BlockPos(0, 63, 0))
                    ? Blocks.STONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
        });
        BlockPos landing = new BlockPos(0, 64, 0);

        ClimbLandingProbe.Result centered = ClimbLandingProbe.evaluate(
                level, new AABB(0.2, 64.0, 0.2, 0.8, 65.8, 0.8),
                landing, true, 0.0);
        assertTrue(centered.stable());

        ClimbLandingProbe.Result overEdge = ClimbLandingProbe.evaluate(
                level, new AABB(0.65, 64.0, 0.2, 1.25, 65.8, 0.8),
                landing, true, 0.0);
        assertFalse(overEdge.supported(),
                "Un solo centro sostenuto non basta se un lato del footprint è sul vuoto");
    }

    @Test
    @DisplayName("D27: velocità verticale non stabilizzata impedisce il completamento")
    void rejectsVerticalMotion() {
        Level level = mock(Level.class);
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(invocation -> {
            BlockPos pos = invocation.getArgument(0);
            return pos.equals(new BlockPos(0, 63, 0))
                    ? Blocks.STONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
        });

        ClimbLandingProbe.Result result = ClimbLandingProbe.evaluate(
                level, new AABB(0.2, 64.0, 0.2, 0.8, 65.8, 0.8),
                new BlockPos(0, 64, 0), true, -0.12);
        assertTrue(result.supported());
        assertFalse(result.stable());
    }
}
