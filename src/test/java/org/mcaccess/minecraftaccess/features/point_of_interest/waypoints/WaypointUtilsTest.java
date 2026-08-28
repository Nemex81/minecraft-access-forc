package org.mcaccess.minecraftaccess.features.point_of_interest.waypoints;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WaypointUtilsTest {

    private static final Identifier OVERWORLD = Identifier.fromNamespaceAndPath("minecraft", "overworld");
    private static final Identifier NETHER = Identifier.fromNamespaceAndPath("minecraft", "the_nether");
    private static final Identifier THE_END = Identifier.fromNamespaceAndPath("minecraft", "the_end");
    private static final Identifier CUSTOM_DIM = Identifier.fromNamespaceAndPath("custom_mod", "custom_dimension");

    @Test
    @DisplayName("Verify isOverworldNetherPair with various dimension combinations")
    void testIsOverworldNetherPair() {
        assertTrue(WaypointUtils.isOverworldNetherPair(OVERWORLD, NETHER), "Overworld and Nether should be true");
        assertTrue(WaypointUtils.isOverworldNetherPair(NETHER, OVERWORLD), "Nether and Overworld should be true");

        assertFalse(WaypointUtils.isOverworldNetherPair(OVERWORLD, THE_END), "Overworld and The End should be false");
        assertFalse(WaypointUtils.isOverworldNetherPair(NETHER, THE_END), "Nether and The End should be false");
        assertFalse(WaypointUtils.isOverworldNetherPair(OVERWORLD, OVERWORLD), "Same dimension should be false");
        assertFalse(WaypointUtils.isOverworldNetherPair(NETHER, NETHER), "Same dimension should be false");
        assertFalse(WaypointUtils.isOverworldNetherPair(OVERWORLD, CUSTOM_DIM), "Custom dim should be false");
        assertFalse(WaypointUtils.isOverworldNetherPair(null, NETHER), "Null should be false");
        assertFalse(WaypointUtils.isOverworldNetherPair(OVERWORLD, null), "Null should be false");
    }

    @ParameterizedTest
    @MethodSource("provideConversionCases")
    @DisplayName("Verify coordinate conversion between Overworld and Nether")
    void testConvertCoordinates(BlockPos input, Identifier fromDim, Identifier toDim, BlockPos expected) {
        BlockPos actual = WaypointUtils.convertCoordinates(input, fromDim, toDim);
        assertEquals(expected.getX(), actual.getX(), "X coordinate mismatch");
        assertEquals(expected.getY(), actual.getY(), "Y coordinate mismatch (Y should remain unchanged)");
        assertEquals(expected.getZ(), actual.getZ(), "Z coordinate mismatch");
    }

    private static Stream<Arguments> provideConversionCases() {
        return Stream.of(
                // Overworld -> Nether (divide by 8)
                Arguments.of(new BlockPos(800, 64, -1600), OVERWORLD, NETHER, new BlockPos(100, 64, -200)),
                Arguments.of(new BlockPos(0, 70, 0), OVERWORLD, NETHER, new BlockPos(0, 70, 0)),
                Arguments.of(new BlockPos(-80, 100, 80), OVERWORLD, NETHER, new BlockPos(-10, 100, 10)),
                // Nether -> Overworld (multiply by 8)
                Arguments.of(new BlockPos(100, 64, -200), NETHER, OVERWORLD, new BlockPos(800, 64, -1600)),
                Arguments.of(new BlockPos(0, 70, 0), NETHER, OVERWORLD, new BlockPos(0, 70, 0)),
                Arguments.of(new BlockPos(-10, 100, 10), NETHER, OVERWORLD, new BlockPos(-80, 100, 80)),
                // Same or non-applicable dimensions -> unchanged
                Arguments.of(new BlockPos(123, 45, 678), OVERWORLD, OVERWORLD, new BlockPos(123, 45, 678)),
                Arguments.of(new BlockPos(123, 45, 678), OVERWORLD, THE_END, new BlockPos(123, 45, 678)),
                Arguments.of(new BlockPos(123, 45, 678), THE_END, OVERWORLD, new BlockPos(123, 45, 678))
        );
    }
}
