package org.mcaccess.minecraftaccess.features;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.directional_path_scanner.PathNarrationFormatter;
import org.mcaccess.minecraftaccess.features.directional_path_scanner.PathScanReport;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Directional Path Scanner Test Suite")
class DirectionalPathScannerTest {

    @Nested
    @DisplayName("1. I18N Dictionary Completeness & Strict Alphabetical Sort")
    class I18nCompletenessTest {

        @Test
        @DisplayName("Verify that it_it.json contains all required path scanner keys in strict alphabetical order")
        void testItalianLanguageKeys() {
            var stream = getClass().getResourceAsStream("/assets/minecraft_access/lang/it_it.json");
            assertNotNull(stream, "it_it.json must exist in classpath");

            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> keys = new ArrayList<>(json.keySet());
            List<String> sortedKeys = new ArrayList<>(keys);
            Collections.sort(sortedKeys);

            assertEquals(sortedKeys, keys, "it_it.json keys MUST be sorted alphabetically");

            assertTrue(json.has("key.category.minecraft_access.path_scanner"));
            assertTrue(json.has("key.minecraft_access.path_scanner.numpad_8"));
            assertTrue(json.has("key.minecraft_access.path_scanner.numpad_5"));
            assertTrue(json.has("key.minecraft_access.path_scanner.extended_up"));
            assertTrue(json.has("minecraft_access.gui.quick_help.cat_scanner"));
            assertTrue(json.has("minecraft_access.gui.quick_help.desc_scanner"));
            assertTrue(json.has("text.autoconfig.minecraft-access.category.directionalPathScanner"));
            assertTrue(json.has("text.autoconfig.minecraft-access.option.directionalPathScanner.enabled"));
            assertTrue(json.has("text.autoconfig.minecraft-access.option.directionalPathScanner.scanRange"));
        }

        @Test
        @DisplayName("Verify that en_us.json contains all required path scanner keys in strict alphabetical order")
        void testEnglishLanguageKeys() {
            var stream = getClass().getResourceAsStream("/assets/minecraft_access/lang/en_us.json");
            assertNotNull(stream, "en_us.json must exist in classpath");

            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> keys = new ArrayList<>(json.keySet());
            List<String> sortedKeys = new ArrayList<>(keys);
            Collections.sort(sortedKeys);

            assertEquals(sortedKeys, keys, "en_us.json keys MUST be sorted alphabetically");

            assertTrue(json.has("key.category.minecraft_access.path_scanner"));
            assertTrue(json.has("key.minecraft_access.path_scanner.numpad_8"));
            assertTrue(json.has("key.minecraft_access.path_scanner.numpad_5"));
            assertTrue(json.has("key.minecraft_access.path_scanner.extended_up"));
            assertTrue(json.has("minecraft_access.gui.quick_help.cat_scanner"));
            assertTrue(json.has("minecraft_access.gui.quick_help.desc_scanner"));
            assertTrue(json.has("text.autoconfig.minecraft-access.category.directionalPathScanner"));
            assertTrue(json.has("text.autoconfig.minecraft-access.option.directionalPathScanner.enabled"));
            assertTrue(json.has("text.autoconfig.minecraft-access.option.directionalPathScanner.scanRange"));
        }
    }

    @Nested
    @DisplayName("2. Path Scan Report & Data Model Integrity")
    class DataModelTest {

        @Test
        @DisplayName("Verify immutability and event encapsulation of PathScanReport")
        void testReportImmutability() {
            List<PathScanReport.PathScanEvent> events = new ArrayList<>();
            events.add(new PathScanReport.PathScanEvent(
                    PathScanReport.EventType.ITEM_RESOURCE,
                    "Apple",
                    3,
                    64,
                    0
            ));

            PathScanReport report = new PathScanReport("forward", "Grass Block", 2, 12, events, true);
            assertEquals("forward", report.directionKey());
            assertEquals("Grass Block", report.primaryGroundName());
            assertEquals(2, report.freeDistance());
            assertEquals(12, report.totalRange());
            assertEquals(1, report.events().size());
            assertTrue(report.reachedEnd());

            assertThrows(UnsupportedOperationException.class, () -> report.events().add(
                    new PathScanReport.PathScanEvent(PathScanReport.EventType.OBSTACLE_BLOCK, "Stone", 4, 64, 0)
            ));

        }
    }

    @Nested
    @DisplayName("3. Configuration Defaults & Boundaries")
    class ConfigIntegrityTest {

        @Test
        @DisplayName("Verify default values for DirectionalPathScanner configuration")
        void testConfigDefaults() {
            Config.DirectionalPathScanner config = new Config.DirectionalPathScanner();
            assertTrue(config.enabled);
            assertEquals(12, config.scanRange);
            assertEquals(Config.DirectionalPathScanner.ExtendedKeysMode.RELATIVE_TO_LOOK, config.extendedKeysMode);
            assertEquals(Config.DirectionalPathScanner.NumpadCardinalsMode.CARDINAL_FIXED, config.numpadCardinalsMode);
            assertTrue(config.detectObstacles);
            assertTrue(config.detectDrops);
            assertEquals(3, config.dropWarningDepth);
            assertTrue(config.detectItems);
            assertTrue(config.detectPassiveMobs);
            assertTrue(config.detectHostileMobs);
            assertTrue(config.detectFluids);
            assertEquals(Config.DirectionalPathScanner.VerbosityMode.COMPACT, config.verbosityMode);
            assertEquals(Config.DirectionalPathScanner.AudioFeedbackMode.SOUND_AND_VOICE, config.audioFeedback);
        }
    }
}
