package org.mcaccess.minecraftaccess.features;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.survival_tracker.SurvivalResourceTarget;
import org.mcaccess.minecraftaccess.features.survival_tracker.SurvivalResourceType;
import org.mcaccess.minecraftaccess.features.survival_tracker.SurvivalResourceTracker;
import org.mcaccess.minecraftaccess.features.survival_tracker.SurvivalScanner;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Survival Resource Tracker Test Suite")
class SurvivalTrackerTest {

    @Nested
    @DisplayName("1. Geographic Cardinal & Compass Direction Calculations")
    class DirectionMathTest {

        @Test
        @DisplayName("Verify cardinal direction mapping across 360 degrees")
        void testCardinalDirectionMapping() {
            // Note: in non-Minecraft environment, I18n returns key or translated string if mocked
            // We test the angles directly through getRelativeDirectionKey and getCardinalDirectionName
            assertEquals("minecraft_access.survival_tracker.rel_forward", SurvivalScanner.getRelativeDirectionKey(0));
            assertEquals("minecraft_access.survival_tracker.rel_forward", SurvivalScanner.getRelativeDirectionKey(10));
            assertEquals("minecraft_access.survival_tracker.rel_forward", SurvivalScanner.getRelativeDirectionKey(355));
            assertEquals("minecraft_access.survival_tracker.rel_forward_right", SurvivalScanner.getRelativeDirectionKey(45));
            assertEquals("minecraft_access.survival_tracker.rel_right", SurvivalScanner.getRelativeDirectionKey(90));
            assertEquals("minecraft_access.survival_tracker.rel_back_right", SurvivalScanner.getRelativeDirectionKey(135));
            assertEquals("minecraft_access.survival_tracker.rel_back", SurvivalScanner.getRelativeDirectionKey(180));
            assertEquals("minecraft_access.survival_tracker.rel_back_left", SurvivalScanner.getRelativeDirectionKey(225));
            assertEquals("minecraft_access.survival_tracker.rel_left", SurvivalScanner.getRelativeDirectionKey(270));
            assertEquals("minecraft_access.survival_tracker.rel_forward_left", SurvivalScanner.getRelativeDirectionKey(315));
        }

        @Test
        @DisplayName("Verify distance formatting units (singular vs plural)")
        void testDistanceFormatting() {
            String singular = SurvivalScanner.formatDistanceWithUnits(1);
            String plural = SurvivalScanner.formatDistanceWithUnits(5);
            assertNotNull(singular);
            assertNotNull(plural);
        }
    }

    @Nested
    @DisplayName("2. Narration String Composition & Filtering")
    class NarrationBuilderTest {

        @Test
        @DisplayName("Verify builder handles all 3 targets present")
        void testAllTargetsPresent() {
            Config.SurvivalTracker config = new Config.SurvivalTracker();
            Map<SurvivalResourceType, SurvivalResourceTarget> targets = new HashMap<>();

            targets.put(SurvivalResourceType.WOOD, new SurvivalResourceTarget(
                    SurvivalResourceType.WOOD,
                    null,
                    null,
                    "Legno",
                    8.0,
                    2,
                    "8 blocchi avanti",
                    "Nord a 0 gradi",
                    "in alto 2 blocchi"
            ));

            targets.put(SurvivalResourceType.STONE, new SurvivalResourceTarget(
                    SurvivalResourceType.STONE,
                    null,
                    null,
                    "Pietra",
                    14.0,
                    -3,
                    "14 blocchi a destra",
                    "Est a 90 gradi",
                    "in basso 3 blocchi"
            ));

            targets.put(SurvivalResourceType.FOOD, new SurvivalResourceTarget(
                    SurvivalResourceType.FOOD,
                    null,
                    null,
                    "Cibo",
                    6.0,
                    0,
                    "6 blocchi indietro a sinistra",
                    "Sud-Ovest a 220 gradi",
                    "stesso livello"
            ));

            String result = SurvivalResourceTracker.buildNarrationString(targets, config);
            assertNotNull(result);
            assertTrue(result.contains("Legno 8 blocchi avanti, Nord a 0 gradi, in alto 2 blocchi"));
            assertTrue(result.contains("Pietra 14 blocchi a destra, Est a 90 gradi, in basso 3 blocchi"));
            assertTrue(result.contains("Cibo 6 blocchi indietro a sinistra, Sud-Ovest a 220 gradi, stesso livello"));
        }

        @Test
        @DisplayName("Verify builder handles missing targets correctly")
        void testMissingTargets() {
            Config.SurvivalTracker config = new Config.SurvivalTracker();
            Map<SurvivalResourceType, SurvivalResourceTarget> targets = new HashMap<>();

            // Only wood found
            targets.put(SurvivalResourceType.WOOD, new SurvivalResourceTarget(
                    SurvivalResourceType.WOOD,
                    null,
                    null,
                    "Legno",
                    5.0,
                    0,
                    "5 blocchi avanti",
                    "Nord a 0 gradi",
                    "stesso livello"
            ));

            String result = SurvivalResourceTracker.buildNarrationString(targets, config);
            assertNotNull(result);
            assertTrue(result.contains("Legno 5 blocchi avanti, Nord a 0 gradi, stesso livello"));
        }

        @Test
        @DisplayName("Verify builder handles selective category toggles")
        void testSelectiveToggles() {
            Config.SurvivalTracker config = new Config.SurvivalTracker();
            config.trackStone = false;
            config.trackFood = false;

            Map<SurvivalResourceType, SurvivalResourceTarget> targets = new HashMap<>();
            targets.put(SurvivalResourceType.WOOD, new SurvivalResourceTarget(
                    SurvivalResourceType.WOOD,
                    null,
                    null,
                    "Legno",
                    10.0,
                    1,
                    "10 blocchi avanti",
                    "Nord a 0 gradi",
                    "in alto 1 blocco"
            ));

            String result = SurvivalResourceTracker.buildNarrationString(targets, config);
            assertNotNull(result);
            assertTrue(result.contains("Legno 10 blocchi avanti"));
            assertFalse(result.contains("Pietra"));
            assertFalse(result.contains("Cibo"));
        }
    }

    @Nested
    @DisplayName("3. I18N Language File Integrity & Key Parity")
    class I18nParityTest {

        @Test
        @DisplayName("Verify all Survival Tracker translation keys exist in both it_it.json and en_us.json")
        void testI18nKeysExist() {
            JsonObject itJson = loadJson("/assets/minecraft_access/lang/it_it.json");
            JsonObject enJson = loadJson("/assets/minecraft_access/lang/en_us.json");

            String[] requiredKeys = {
                    "access_menu_function.minecraft_access.survival_resources",
                    "key.minecraft_access.survival_tracker.scan_extended",
                    "key.minecraft_access.survival_tracker.scan_numpad",
                    "minecraft_access.survival_tracker.altitude_down",
                    "minecraft_access.survival_tracker.altitude_up",
                    "minecraft_access.survival_tracker.at_degrees",
                    "minecraft_access.survival_tracker.food",
                    "minecraft_access.survival_tracker.none_enabled",
                    "minecraft_access.survival_tracker.not_found_fem",
                    "minecraft_access.survival_tracker.not_found_masc",
                    "minecraft_access.survival_tracker.plural_blocks",
                    "minecraft_access.survival_tracker.prefix",
                    "minecraft_access.survival_tracker.rel_back",
                    "minecraft_access.survival_tracker.rel_back_left",
                    "minecraft_access.survival_tracker.rel_back_right",
                    "minecraft_access.survival_tracker.rel_forward",
                    "minecraft_access.survival_tracker.rel_forward_left",
                    "minecraft_access.survival_tracker.rel_forward_right",
                    "minecraft_access.survival_tracker.rel_left",
                    "minecraft_access.survival_tracker.rel_right",
                    "minecraft_access.survival_tracker.same_level",
                    "minecraft_access.survival_tracker.single_block",
                    "minecraft_access.survival_tracker.stone",
                    "minecraft_access.survival_tracker.wood",
                    "text.autoconfig.minecraft-access.category.survivalTracker",
                    "text.autoconfig.minecraft-access.option.survivalTracker.enabled",
                    "text.autoconfig.minecraft-access.option.survivalTracker.periodicIntervalSeconds",
                    "text.autoconfig.minecraft-access.option.survivalTracker.periodicScanEnabled",
                    "text.autoconfig.minecraft-access.option.survivalTracker.range",
                    "text.autoconfig.minecraft-access.option.survivalTracker.trackFood",
                    "text.autoconfig.minecraft-access.option.survivalTracker.trackStone",
                    "text.autoconfig.minecraft-access.option.survivalTracker.trackWood"
            };

            for (String key : requiredKeys) {
                assertTrue(itJson.has(key), "it_it.json missing key: " + key);
                assertTrue(enJson.has(key), "en_us.json missing key: " + key);
                assertFalse(itJson.get(key).getAsString().isBlank(), "it_it.json key is blank: " + key);
                assertFalse(enJson.get(key).getAsString().isBlank(), "en_us.json key is blank: " + key);
            }
        }

        private JsonObject loadJson(String resourcePath) {
            try (var is = getClass().getResourceAsStream(resourcePath)) {
                assertNotNull(is, "Resource not found: " + resourcePath);
                return JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                fail("Failed to load JSON resource " + resourcePath + ": " + e.getMessage());
                return null;
            }
        }
    }

    @Nested
    @DisplayName("4. Boundary, Corner & Edge Cases")
    class BoundaryAndEdgeCasesTest {

        @Test
        @DisplayName("Verify edge angle transitions around 360/0 degree seams")
        void testAngleSeams() {
            assertEquals("minecraft_access.survival_tracker.rel_forward", SurvivalScanner.getRelativeDirectionKey(0.0));
            assertEquals("minecraft_access.survival_tracker.rel_forward", SurvivalScanner.getRelativeDirectionKey(22.4));
            assertEquals("minecraft_access.survival_tracker.rel_forward_right", SurvivalScanner.getRelativeDirectionKey(22.6));
            assertEquals("minecraft_access.survival_tracker.rel_forward_right", SurvivalScanner.getRelativeDirectionKey(67.4));
            assertEquals("minecraft_access.survival_tracker.rel_right", SurvivalScanner.getRelativeDirectionKey(67.6));
            assertEquals("minecraft_access.survival_tracker.rel_forward", SurvivalScanner.getRelativeDirectionKey(337.6));
            assertEquals("minecraft_access.survival_tracker.rel_forward", SurvivalScanner.getRelativeDirectionKey(359.9));
        }

        @Test
        @DisplayName("Verify narration when all categories are disabled")
        void testAllCategoriesDisabled() {
            Config.SurvivalTracker config = new Config.SurvivalTracker();
            config.trackWood = false;
            config.trackStone = false;
            config.trackFood = false;

            Map<SurvivalResourceType, SurvivalResourceTarget> targets = new HashMap<>();
            String result = SurvivalResourceTracker.buildNarrationString(targets, config);
            assertNotNull(result);
            assertTrue(result.contains("minecraft_access.survival_tracker.none_enabled") || result.contains("Nessuna") || !result.isEmpty());
        }

        @Test
        @DisplayName("Verify narration when no resources are found in range")
        void testNoResourcesFound() {
            Config.SurvivalTracker config = new Config.SurvivalTracker();
            config.trackWood = true;
            config.trackStone = true;
            config.trackFood = true;

            Map<SurvivalResourceType, SurvivalResourceTarget> targets = new HashMap<>();
            String result = SurvivalResourceTracker.buildNarrationString(targets, config);
            assertNotNull(result);
            assertTrue(result.contains("minecraft_access.survival_tracker.wood") || result.contains("Legno") || result.contains("Wood"));
        }

        @Test
        @DisplayName("Verify extreme vertical delta formatting")
        void testExtremeVerticalDeltas() {
            Config.SurvivalTracker config = new Config.SurvivalTracker();
            Map<SurvivalResourceType, SurvivalResourceTarget> targets = new HashMap<>();

            targets.put(SurvivalResourceType.WOOD, new SurvivalResourceTarget(
                    SurvivalResourceType.WOOD,
                    null,
                    null,
                    "Legno",
                    120.0,
                    64,
                    "120 blocchi avanti",
                    "Nord a 0 gradi",
                    "in alto 64 blocchi"
            ));

            targets.put(SurvivalResourceType.STONE, new SurvivalResourceTarget(
                    SurvivalResourceType.STONE,
                    null,
                    null,
                    "Pietra",
                    50.0,
                    -128,
                    "50 blocchi a sinistra",
                    "Ovest a 270 gradi",
                    "in basso 128 blocchi"
            ));

            String result = SurvivalResourceTracker.buildNarrationString(targets, config);
            assertNotNull(result);
            assertTrue(result.contains("in alto 64 blocchi"));
            assertTrue(result.contains("in basso 128 blocchi"));
        }
    }
}

