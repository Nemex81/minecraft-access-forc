package org.mcaccess.minecraftaccess.features;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObstacleDetectorTest {

    @Test
    @DisplayName("Verify state evaluation: Clear passage on flat ground and auto-step surfaces (deltaY <= 0.60)")
    void testFlatTerrainAndAutoStep() {
        // Flat terrain (deltaY = 0.0)
        assertEquals(ObstacleDetectionUtils.ObstacleState.CLEAR, ObstacleDetectionUtils.evaluateState(0.0, false, false, false));

        // Dirt path to grass block (deltaY = 0.0625) -> auto-stepped smoothly without jump
        assertEquals(ObstacleDetectionUtils.ObstacleState.CLEAR, ObstacleDetectionUtils.evaluateState(0.0625, false, false, false));

        // Bottom slab or stair step (deltaY = 0.50) -> auto-stepped smoothly
        assertEquals(ObstacleDetectionUtils.ObstacleState.CLEAR, ObstacleDetectionUtils.evaluateState(0.50, false, false, false));

        // Auto-step maximum boundary (deltaY = 0.60)
        assertEquals(ObstacleDetectionUtils.ObstacleState.CLEAR, ObstacleDetectionUtils.evaluateState(0.60, false, false, false));
    }

    @Test
    @DisplayName("Verify state evaluation: Real climbable steps requiring jump (0.60 < deltaY <= 1.20)")
    void testClimbableSteps() {
        // Standard full block from flat grass (deltaY = 1.0)
        assertEquals(ObstacleDetectionUtils.ObstacleState.STEP_CLIMBABLE, ObstacleDetectionUtils.evaluateState(1.0, false, false, false));

        // Standard full block from dirt path (deltaY = 1.0625)
        assertEquals(ObstacleDetectionUtils.ObstacleState.STEP_CLIMBABLE, ObstacleDetectionUtils.evaluateState(1.0625, false, false, false));

        // Chest from flat ground (deltaY = 0.875)
        assertEquals(ObstacleDetectionUtils.ObstacleState.STEP_CLIMBABLE, ObstacleDetectionUtils.evaluateState(0.875, false, false, false));

        // Maximum jumpable elevation (deltaY = 1.20)
        assertEquals(ObstacleDetectionUtils.ObstacleState.STEP_CLIMBABLE, ObstacleDetectionUtils.evaluateState(1.20, false, false, false));
    }

    @Test
    @DisplayName("Verify state evaluation: Fence, Wall, Closed Gate (deltaY > 1.20) classified as WALL")
    void testFenceAndWallObstacles() {
        // Fence or Wall from flat grass (deltaY = 1.5)
        assertEquals(ObstacleDetectionUtils.ObstacleState.WALL, ObstacleDetectionUtils.evaluateState(1.5, false, false, false));

        // Fence from dirt path (deltaY = 1.5625)
        assertEquals(ObstacleDetectionUtils.ObstacleState.WALL, ObstacleDetectionUtils.evaluateState(1.5625, false, false, false));

        // 2-block vertical wall (deltaY = 2.0 or headSolid = true)
        assertEquals(ObstacleDetectionUtils.ObstacleState.WALL, ObstacleDetectionUtils.evaluateState(1.0, true, false, false));
        assertEquals(ObstacleDetectionUtils.ObstacleState.WALL, ObstacleDetectionUtils.evaluateState(2.0, false, false, false));
    }

    @Test
    @DisplayName("Verify state evaluation: Low ceiling above step or player")
    void testLowCeiling() {
        // Low ceiling above target step
        assertEquals(ObstacleDetectionUtils.ObstacleState.LOW_CEILING, ObstacleDetectionUtils.evaluateState(1.0, false, true, false));

        // Low ceiling above player head
        assertEquals(ObstacleDetectionUtils.ObstacleState.LOW_CEILING, ObstacleDetectionUtils.evaluateState(1.0, false, false, true));

        // Low ceiling on both
        assertEquals(ObstacleDetectionUtils.ObstacleState.LOW_CEILING, ObstacleDetectionUtils.evaluateState(1.0, false, true, true));
    }

    @Test
    @DisplayName("Verify state evaluation: Head-level obstacle (suspended block/branch)")
    void testHeadLevelObstacle() {
        // Flat ground under feet, solid block at eye level
        assertEquals(ObstacleDetectionUtils.ObstacleState.HEAD_OBSTACLE, ObstacleDetectionUtils.evaluateState(0.0, true, false, false));
        assertEquals(ObstacleDetectionUtils.ObstacleState.HEAD_OBSTACLE, ObstacleDetectionUtils.evaluateState(0.5, true, false, false));
    }

    @Test
    @DisplayName("Verify Look-At priority hierarchy: P_headroom > B_head > B_headroom > B_foot")
    void testLookAtPriorities() {
        BlockPos foot = new BlockPos(10, 64, 20);
        BlockPos head = foot.above(1);
        BlockPos targetCeiling = foot.above(2);
        BlockPos playerCeiling = new BlockPos(9, 66, 20);

        // Priority 1: Player ceiling solid
        BlockPos look1 = ObstacleDetectionUtils.determineLookAtBlock(1.0, true, true, true, foot, head, targetCeiling, playerCeiling);
        assertEquals(playerCeiling, look1);

        // Priority 2: Head solid (wall / head obstacle, player ceiling clear)
        BlockPos look2 = ObstacleDetectionUtils.determineLookAtBlock(1.0, true, true, false, foot, head, targetCeiling, playerCeiling);
        assertEquals(head, look2);

        // Priority 3: Target ceiling solid (step with low ceiling, head clear, player ceiling clear)
        BlockPos look3 = ObstacleDetectionUtils.determineLookAtBlock(1.0, false, true, false, foot, head, targetCeiling, playerCeiling);
        assertEquals(targetCeiling, look3);

        // Priority 4: Climbable step (deltaY = 1.0)
        BlockPos look4 = ObstacleDetectionUtils.determineLookAtBlock(1.0, false, false, false, foot, head, targetCeiling, playerCeiling);
        assertEquals(foot, look4);

        // Flat ground (deltaY = 0.0)
        BlockPos lookNone = ObstacleDetectionUtils.determineLookAtBlock(0.0, false, false, false, foot, head, targetCeiling, playerCeiling);
        assertNull(lookNone);
    }

    @Test
    @DisplayName("Verify directional coordinate calculations")
    void testDirectionalCoordinates() {
        Vec3 playerPos = new Vec3(10.5, 64.0, 20.5);

        // Facing North (Z-1)
        Vec3 dirNorth = new Vec3(0, 0, -1);
        int baseGroundY = (int) Math.floor(playerPos.y - 0.05);
        int playerBaseY = baseGroundY + 1;
        BlockPos playerHeadroom = BlockPos.containing(playerPos.x, baseGroundY + 3, playerPos.z);
        assertEquals(new BlockPos(10, 66, 20), playerHeadroom);

        BlockPos targetFootNorth = BlockPos.containing(playerPos.x + dirNorth.x, playerBaseY, playerPos.z + dirNorth.z);
        assertEquals(new BlockPos(10, 64, 19), targetFootNorth);
        assertEquals(new BlockPos(10, 65, 19), targetFootNorth.above(1));
        assertEquals(new BlockPos(10, 66, 19), targetFootNorth.above(2));

        // Facing East (X+1)
        Vec3 dirEast = new Vec3(1, 0, 0);
        BlockPos targetFootEast = BlockPos.containing(playerPos.x + dirEast.x, playerBaseY, playerPos.z + dirEast.z);
        assertEquals(new BlockPos(11, 64, 20), targetFootEast);
    }

    @Test
    @DisplayName("Verify progressive proximity raycast steps correctly from near to far")
    void testProximityRaycastOrder() {
        // Player standing near block boundary: player at (-90.2, 65.0, -32.9), facing East (+X)
        Vec3 playerPos = new Vec3(-90.2, 65.0, -32.9);
        Vec3 dirEast = new Vec3(1, 0, 0);

        int baseGroundY = (int) Math.floor(playerPos.y - 0.05);
        int playerBaseY = baseGroundY + 1;
        BlockPos playerBlock = BlockPos.containing(playerPos.x, playerBaseY, playerPos.z);
        assertEquals(new BlockPos(-91, 65, -33), playerBlock);

        // Immediate adjacent step at distance 0.35m
        BlockPos nearPos = BlockPos.containing(playerPos.x + dirEast.x * 0.35, playerBaseY, playerPos.z + dirEast.z * 0.35);
        assertEquals(new BlockPos(-90, 65, -33), nearPos); // Nearest block (e.g. Fence)

        // Far step at distance 1.5m
        BlockPos farPos = BlockPos.containing(playerPos.x + dirEast.x * 1.5, playerBaseY, playerPos.z + dirEast.z * 1.5);
        assertEquals(new BlockPos(-89, 65, -33), farPos); // Furthest block (e.g. Hay Bale)

        // Player touching the fence: player position center is at (-89.85, 65.0, -32.9) inside block (-90, 65, -33)
        Vec3 touchingPos = new Vec3(-89.85, 65.0, -32.9);
        BlockPos touchingBlock = BlockPos.containing(touchingPos.x, playerBaseY, touchingPos.z);
        assertEquals(new BlockPos(-90, 65, -33), touchingBlock);

        // Step at 0.1m still samples inside the fence block
        BlockPos step01 = BlockPos.containing(touchingPos.x + dirEast.x * 0.1, playerBaseY, touchingPos.z + dirEast.z * 0.1);
        assertEquals(new BlockPos(-90, 65, -33), step01);
    }

    @Test
    @DisplayName("Verify NarrationStyle enum values")
    void testNarrationStyles() {
        assertEquals(4, ObstacleDetectionUtils.NarrationStyle.values().length);
        assertNotNull(ObstacleDetectionUtils.NarrationStyle.valueOf("BLOCK"));
        assertNotNull(ObstacleDetectionUtils.NarrationStyle.valueOf("ELEVATION"));
        assertNotNull(ObstacleDetectionUtils.NarrationStyle.valueOf("DIRECT"));
        assertNotNull(ObstacleDetectionUtils.NarrationStyle.valueOf("SLOPE"));
    }

    @Test
    @DisplayName("Verify I18N keys presence in Italian and English language files")
    void testI18NKeysPresence() {
        String[] requiredKeys = {
                "key.minecraft_access.obstacle_detector.inspect_obstacle",
                "minecraft_access.obstacle_detector.clear",
                "minecraft_access.obstacle_detector.head_obstacle",
                "minecraft_access.obstacle_detector.obstacle",
                "minecraft_access.obstacle_detector.low_ceiling.block",
                "minecraft_access.obstacle_detector.low_ceiling.direct",
                "minecraft_access.obstacle_detector.low_ceiling.elevation",
                "minecraft_access.obstacle_detector.low_ceiling.slope",
                "minecraft_access.obstacle_detector.step_climbable.block",
                "minecraft_access.obstacle_detector.step_climbable.direct",
                "minecraft_access.obstacle_detector.step_climbable.elevation",
                "minecraft_access.obstacle_detector.step_climbable.slope",
                "text.autoconfig.minecraft-access.category.obstacleDetector",
                "text.autoconfig.minecraft-access.option.obstacleDetector.delay",
                "text.autoconfig.minecraft-access.option.obstacleDetector.detectionRange",
                "text.autoconfig.minecraft-access.option.obstacleDetector.enabled",
                "text.autoconfig.minecraft-access.option.obstacleDetector.lookAtObstacleOnInspection",
                "text.autoconfig.minecraft-access.option.obstacleDetector.narrationStyle",
                "text.autoconfig.minecraft-access.option.obstacleDetector.playAudioCues",
                "text.autoconfig.minecraft-access.option.obstacleDetector.voiceWarning",
                "text.autoconfig.minecraft-access.option.obstacleDetector.volume"
        };

        String[] langFiles = {
                "/assets/minecraft_access/lang/it_it.json",
                "/assets/minecraft_access/lang/en_us.json"
        };

        for (String path : langFiles) {
            var stream = getClass().getResourceAsStream(path);
            assertNotNull(stream, "Could not find resource: " + path);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();

            for (String key : requiredKeys) {
                assertTrue(json.has(key), "Missing key '" + key + "' in " + path);
                assertFalse(json.get(key).getAsString().isBlank(), "Empty translation for key '" + key + "' in " + path);
            }
        }
    }
}
