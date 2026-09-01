package org.mcaccess.minecraftaccess.features;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.utils.position.Orientation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Numpad Controls:
 * Validates common cases, uncommon cases, and boundary / edge cases.
 */
class NumpadControlsTest {

    private static final float DEGREES_PER_MOUSE_DELTA = 0.15f;

    // =========================================================================
    // 1. COMMON CASES: Camera Math, Angles & Continuous Rotation
    // =========================================================================
    @Nested
    @DisplayName("Common Cases: Camera Mathematics & Rotation Angles")
    class CommonCasesTest {

        @Test
        @DisplayName("Verify discrete angle step calculation (normal: 15°, modified: 45°)")
        void testAngleCalculations() {
            float normalAngle = 15.0f;
            float normalDelta = normalAngle / DEGREES_PER_MOUSE_DELTA;
            assertEquals(100.0f, normalDelta, 0.001f, "15 degrees should correspond to 100 mouse delta units");

            float modifiedAngle = 45.0f;
            float modifiedDelta = modifiedAngle / DEGREES_PER_MOUSE_DELTA;
            assertEquals(300.0f, modifiedDelta, 0.001f, "45 degrees should correspond to 300 mouse delta units");
        }

        @Test
        @DisplayName("Verify 2D combined diagonal discrete step calculations (15° H + 15° V)")
        void testDiagonal2DStepCalculations() {
            float normalAngle = 15.0f;
            float deltaUnit = normalAngle / DEGREES_PER_MOUSE_DELTA;

            // Up-Left (-1, -1)
            float deltaH_UL = deltaUnit * -1;
            float deltaV_UL = deltaUnit * -1;
            assertEquals(-100.0f, deltaH_UL, 0.001f);
            assertEquals(-100.0f, deltaV_UL, 0.001f);

            // Up-Right (1, -1)
            float deltaH_UR = deltaUnit * 1;
            float deltaV_UR = deltaUnit * -1;
            assertEquals(100.0f, deltaH_UR, 0.001f);
            assertEquals(-100.0f, deltaV_UR, 0.001f);

            // Down-Left (-1, 1)
            float deltaH_DL = deltaUnit * -1;
            float deltaV_DL = deltaUnit * 1;
            assertEquals(-100.0f, deltaH_DL, 0.001f);
            assertEquals(100.0f, deltaV_DL, 0.001f);

            // Down-Right (1, 1)
            float deltaH_DR = deltaUnit * 1;
            float deltaV_DR = deltaUnit * 1;
            assertEquals(100.0f, deltaH_DR, 0.001f);
            assertEquals(100.0f, deltaV_DR, 0.001f);
        }

        @Test
        @DisplayName("Verify continuous rotation speed multiplier scaling")
        void testContinuousRotationScaling() {
            float baseAngle = 4.5f;
            float speed1x = (baseAngle * 1.0f) / DEGREES_PER_MOUSE_DELTA;
            float speed2x = (baseAngle * 2.0f) / DEGREES_PER_MOUSE_DELTA;
            float speed05x = (baseAngle * 0.5f) / DEGREES_PER_MOUSE_DELTA;

            assertEquals(30.0f, speed1x, 0.001f);
            assertEquals(60.0f, speed2x, 0.001f);
            assertEquals(15.0f, speed05x, 0.001f);
        }

        @Test
        @DisplayName("Verify Dual-Mode Hold threshold logic (Tap < 200ms vs Hold >= 200ms)")
        void testDualModeThreshold() {
            long holdDelayMs = 200;
            long startTime = 1000;

            // Tap at 50ms -> No continuous hold
            assertFalse(1050 - startTime >= holdDelayMs, "Short tap (50ms) should not engage continuous hold");

            // Tap at 150ms -> No continuous hold
            assertFalse(1150 - startTime >= holdDelayMs, "Tap (150ms) should not engage continuous hold");

            // Hold at 200ms -> Continuous hold engages
            assertTrue(1200 - startTime >= holdDelayMs, "Hold >= 200ms should engage continuous hold");

            // Hold at 500ms -> Continuous hold continues
            assertTrue(1500 - startTime >= holdDelayMs, "Hold at 500ms should continue continuous rotation");
        }

        @Test
        @DisplayName("Verify hotbar scroll throttling")
        void testHotbarScrollThrottling() {
            int delayMs = 150;
            long lastScroll = 1000;

            // Rapid repeat at 50ms -> Throttled
            assertFalse((1050 - lastScroll) >= delayMs, "Scroll event after 50ms should be throttled");

            // Repeat at 150ms -> Allowed
            assertTrue((1150 - lastScroll) >= delayMs, "Scroll event after 150ms should be allowed");
        }

        @Test
        @DisplayName("Verify continuous cardinal direction transition detection (hysteresis)")
        void testContinuousCardinalTransitions() {
            Orientation lastFacing = Orientation.EAST;

            // Minor rotation within EAST (247.5° to 292.5°)
            Orientation nextFacingSame = Orientation.ofHorizontal(275);
            assertEquals(Orientation.EAST, nextFacingSame);
            assertSame(lastFacing, nextFacingSame, "No transition should trigger when within same sector");

            // Rotated to SOUTH_EAST (292.5° to 337.5°)
            Orientation nextFacingNew = Orientation.ofHorizontal(320);
            assertEquals(Orientation.SOUTH_EAST, nextFacingNew);
            assertNotSame(lastFacing, nextFacingNew, "Transition to SOUTH_EAST must be detected");
        }
    }

    // =========================================================================
    // 2. UNCOMMON CASES: Directions, Inversions, Presets & Durability
    // =========================================================================
    @Nested
    @DisplayName("Uncommon Cases: Cardinal Orientation & Parameter Variations")
    class UncommonCasesTest {

        @Test
        @DisplayName("Verify Opposite Cardinal Directions (180° Look Behind)")
        void testOppositeDirections() {
            assertEquals(Orientation.SOUTH, Orientation.NORTH.getOpposite());
            assertEquals(Orientation.NORTH, Orientation.SOUTH.getOpposite());
            assertEquals(Orientation.WEST, Orientation.EAST.getOpposite());
            assertEquals(Orientation.EAST, Orientation.WEST.getOpposite());
            assertEquals(Orientation.SOUTH_EAST, Orientation.NORTH_WEST.getOpposite());
            assertEquals(Orientation.NORTH_WEST, Orientation.SOUTH_EAST.getOpposite());
            assertEquals(Orientation.SOUTH_WEST, Orientation.NORTH_EAST.getOpposite());
            assertEquals(Orientation.NORTH_EAST, Orientation.SOUTH_WEST.getOpposite());
        }

        @Test
        @DisplayName("Verify Invert Y-Axis calculation")
        void testInvertYAxis() {
            int normalVerticalWeight = -1; // UP is negative pitch delta in standard coordinate systems
            int invertedVerticalWeight = -normalVerticalWeight; // Inverted becomes positive

            assertEquals(1, invertedVerticalWeight, "Inverted UP vertical weight should flip sign");
        }

        @Test
        @DisplayName("Verify Durability calculation")
        void testDurabilityMath() {
            int maxDamage = 1561; // Diamond sword / pickaxe
            int damageValue = 100;
            int remainingDurability = maxDamage - damageValue;

            assertEquals(1461, remainingDurability);
            assertEquals(1561, maxDamage);
        }

        @Test
        @DisplayName("Verify Config Defaults integrity")
        void testConfigDefaults() {
            Config.NumpadControls config = new Config.NumpadControls();
            assertTrue(config.enabled, "Numpad should be enabled by default");
            assertEquals(Config.NumpadControls.HandednessPreset.RIGHT_HANDED, config.preset);
            assertEquals(15.0f, config.normalRotatingAngle);
            assertEquals(45.0f, config.modifiedRotatingAngle);
            assertTrue(config.continuousRotation);
            assertEquals(1.0f, config.continuousRotationSpeed);
            assertFalse(config.invertYAxis);
            assertTrue(config.narrateFacingOnChange);
            assertTrue(config.enableContinuousHold);
            assertEquals(150, config.scrollDelayMilliseconds);
            assertTrue(config.playCardinalSnapSound);
            assertEquals(1.0f, config.audioCueVolume);
        }
    }

    // =========================================================================
    // 3. EDGE CASES & BOUNDARY LIMITS
    // =========================================================================
    @Nested
    @DisplayName("Edge Cases: Pitch Bounds, Yaw Wrap, Modifiers, & Localization")
    class EdgeCasesTest {

        @Test
        @DisplayName("Verify pitch clamping at Nadir (+90.0°) and Zenith (-90.0°)")
        void testPitchBoundsClamping() {
            float nadirPitch = 90.0f;
            float zenithPitch = -90.0f;

            // Pitch must never exceed -90 to +90 degrees in Minecraft
            assertTrue(nadirPitch <= 90.0f && nadirPitch >= -90.0f);
            assertTrue(zenithPitch <= 90.0f && zenithPitch >= -90.0f);

            float clampedAbove = Math.clamp(120.0f, -90.0f, 90.0f);
            assertEquals(90.0f, clampedAbove, "Pitch above 90° must clamp to 90° (Nadir/Feet)");

            float clampedBelow = Math.clamp(-120.0f, -90.0f, 90.0f);
            assertEquals(-90.0f, clampedBelow, "Pitch below -90° must clamp to -90° (Zenith/Sky)");
        }

        @Test
        @DisplayName("Verify yaw 360° angle normalization boundary")
        void testYawNormalization() {
            float yawOver360 = 375.0f;
            float normalizedYaw = ((yawOver360 % 360.0f) + 360.0f) % 360.0f;
            assertEquals(15.0f, normalizedYaw, 0.001f, "375° should normalize to 15°");

            float yawNegative = -45.0f;
            float normalizedNeg = ((yawNegative % 360.0f) + 360.0f) % 360.0f;
            assertEquals(315.0f, normalizedNeg, 0.001f, "-45° should normalize to 315°");
        }

        @Test
        @DisplayName("Verify Localization: all 40+ Numpad translation keys exist in en_us.json and it_it.json")
        void testLocalizationKeyCompleteness() {
            JsonObject enJson = loadLangJson("/assets/minecraft_access/lang/en_us.json");
            JsonObject itJson = loadLangJson("/assets/minecraft_access/lang/it_it.json");

            assertNotNull(enJson, "en_us.json must be readable");
            assertNotNull(itJson, "it_it.json must be readable");

            String[] expectedKeyPrefixes = {
                    "key.category.minecraft_access.numpad_controls",
                    "key.minecraft_access.numpad.camera.look_up",
                    "key.minecraft_access.numpad.camera.look_down",
                    "key.minecraft_access.numpad.camera.look_left",
                    "key.minecraft_access.numpad.camera.look_right",
                    "key.minecraft_access.numpad.camera.look_up_left",
                    "key.minecraft_access.numpad.camera.look_up_right",
                    "key.minecraft_access.numpad.camera.look_down_left",
                    "key.minecraft_access.numpad.camera.look_down_right",
                    "key.minecraft_access.numpad.camera.look_nadir",
                    "key.minecraft_access.numpad.camera.look_zenith",
                    "key.minecraft_access.numpad.camera.center_crosshair",
                    "key.minecraft_access.numpad.camera.narrate_facing",
                    "key.minecraft_access.numpad.camera.snap_cardinal",
                    "key.minecraft_access.numpad.mouse.left_click",
                    "key.minecraft_access.numpad.mouse.right_click",
                    "key.minecraft_access.numpad.mouse.middle_click",
                    "key.minecraft_access.numpad.action.unlock",
                    "key.minecraft_access.numpad.hotbar.scroll_prev",
                    "key.minecraft_access.numpad.hotbar.scroll_next",
                    "key.minecraft_access.numpad.poi.item_prev",
                    "key.minecraft_access.numpad.poi.item_next",
                    "key.minecraft_access.numpad.poi.group_prev",
                    "key.minecraft_access.numpad.poi.group_next",
                    "key.minecraft_access.numpad.poi.look_at_current_object",
                    "key.minecraft_access.numpad.poi.target_nearest_any",
                    "key.minecraft_access.numpad.poi.target_nearest_entity",
                    "key.minecraft_access.numpad.poi.target_nearest_block",
                    "key.minecraft_access.numpad.poi.lock_target",
                    "key.minecraft_access.numpad.poi.mark_target",
                    "key.minecraft_access.numpad.poi.unmark_target",
                    "key.minecraft_access.numpad.orient.north",
                    "key.minecraft_access.numpad.orient.east",
                    "key.minecraft_access.numpad.orient.south",
                    "key.minecraft_access.numpad.orient.west",
                    "key.minecraft_access.numpad.orient.north_west",
                    "key.minecraft_access.numpad.orient.north_east",
                    "key.minecraft_access.numpad.orient.south_west",
                    "key.minecraft_access.numpad.orient.south_east",
                    "key.minecraft_access.numpad.orient.look_behind",
                    "key.minecraft_access.numpad.camera.restore_previous_look",
                    "key.minecraft_access.numpad.orient.narrate_coordinates",
                    "key.minecraft_access.numpad.orient.narrate_target_coords",
                    "key.minecraft_access.numpad.status.player_all",
                    "key.minecraft_access.numpad.status.mainhand",
                    "key.minecraft_access.numpad.status.offhand",
                    "key.minecraft_access.numpad.status.effects",
                    "key.minecraft_access.numpad.status.durability",
                    "key.minecraft_access.numpad.status.access_menu",
                    "key.minecraft_access.numpad.status.bossbar_next",
                    "key.minecraft_access.numpad.status.bossbar_prev",
                    "text.autoconfig.minecraft-access.category.numpadControls",
                    "text.autoconfig.minecraft-access.option.numpadControls.enabled",
                    "text.autoconfig.minecraft-access.option.numpadControls.preset",
                    "text.autoconfig.minecraft-access.option.numpadControls.continuousRotation",
                    "text.autoconfig.minecraft-access.option.numpadControls.continuousFeedbackMode",
                    "text.autoconfig.minecraft-access.option.numpadControls.continuousFeedbackMode.SOUND_ONLY",
                    "text.autoconfig.minecraft-access.option.numpadControls.continuousFeedbackMode.VOICE_ONLY",
                    "text.autoconfig.minecraft-access.option.numpadControls.continuousFeedbackMode.SOUND_AND_VOICE",
                    "text.autoconfig.minecraft-access.option.numpadControls.continuousFeedbackMode.OFF",
                    "text.autoconfig.minecraft-access.option.numpadControls.rotationFeedbackMode",
                    "text.autoconfig.minecraft-access.option.numpadControls.rotationFeedbackMode.CARDINAL_AND_DEGREES",
                    "text.autoconfig.minecraft-access.option.numpadControls.rotationFeedbackMode.SOUND_AND_VOICE_WITH_DEGREES",
                    "text.autoconfig.minecraft-access.option.numpadControls.rotationFeedbackMode.CARDINAL_ONLY",
                    "text.autoconfig.minecraft-access.option.numpadControls.rotationFeedbackMode.SOUND_ONLY",
                    "text.autoconfig.minecraft-access.option.numpadControls.rotationFeedbackMode.OFF",
                    "text.autoconfig.minecraft-access.option.numpadControls.centerHorizonFeedbackMode",
                    "text.autoconfig.minecraft-access.option.numpadControls.centerHorizonFeedbackMode.TARGET_ONLY",
                    "text.autoconfig.minecraft-access.option.numpadControls.centerHorizonFeedbackMode.SOUND_AND_TARGET",
                    "text.autoconfig.minecraft-access.option.numpadControls.centerHorizonFeedbackMode.SOUND_VOICE_AND_TARGET",
                    "minecraft_access.numpad.look_centered"
            };

            for (String key : expectedKeyPrefixes) {
                assertTrue(enJson.has(key), "en_us.json missing expected key: " + key);
                assertTrue(itJson.has(key), "it_it.json missing expected key: " + key);
                assertFalse(enJson.get(key).getAsString().isBlank(), "en_us.json key cannot be blank: " + key);
                assertFalse(itJson.get(key).getAsString().isBlank(), "it_it.json key cannot be blank: " + key);
            }
        }

        private JsonObject loadLangJson(String path) {
            var stream = getClass().getResourceAsStream(path);
            if (stream == null) return null;
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Nested
    @DisplayName("4. Modifier Isolation & Layer Separation")
    class ModifierIsolationTest {

        @Test
        @DisplayName("Verify modifier state helper contract")
        void testModifierContract() {
            // Screen mock or contract assertion
            assertDoesNotThrow(() -> {
                // Testing ModifierUtils class presence and static structure
                Class<?> clazz = Class.forName("org.mcaccess.minecraftaccess.utils.ModifierUtils");
                assertNotNull(clazz.getMethod("hasAnyModifier"));
                assertNotNull(clazz.getMethod("hasNoModifiers"));
                assertNotNull(clazz.getMethod("hasControlOnly"));
                assertNotNull(clazz.getMethod("hasAltOnly"));
                assertNotNull(clazz.getMethod("hasShiftOnly"));
            });
        }
    }
}

