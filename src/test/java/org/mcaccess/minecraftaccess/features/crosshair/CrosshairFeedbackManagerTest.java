package org.mcaccess.minecraftaccess.features.crosshair;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrosshairFeedbackManagerTest {

    @Test
    @DisplayName("Verify formatFeedback with TARGET_FIRST and all tokens enabled")
    void testTargetFirstAllTokens() {
        String result = CrosshairFeedbackManager.formatFeedback(
                "Mattoni di pietra",
                3.0,
                "Sud",
                180,
                "Dritto",
                CrosshairReadingOrder.TARGET_FIRST,
                true,
                true,
                true,
                true,
                true
        );

        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("Mattoni di pietra"));
        assertTrue(result.contains("Sud"));
    }

    @Test
    @DisplayName("Verify formatFeedback with ORIENTATION_FIRST and all tokens enabled")
    void testOrientationFirstAllTokens() {
        String result = CrosshairFeedbackManager.formatFeedback(
                "Mattoni di pietra",
                3.0,
                "Sud",
                180,
                "Dritto",
                CrosshairReadingOrder.ORIENTATION_FIRST,
                true,
                true,
                true,
                true,
                true
        );

        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("Mattoni di pietra"));
        assertTrue(result.contains("Sud"));
    }

    @Test
    @DisplayName("Verify formatFeedback with TARGET_CARDINAL_INLINE and all tokens enabled")
    void testTargetCardinalInlineAllTokens() {
        String result = CrosshairFeedbackManager.formatFeedback(
                "Mattoni di pietra",
                3.0,
                "Sud",
                180,
                "Dritto",
                CrosshairReadingOrder.TARGET_CARDINAL_INLINE,
                true,
                true,
                true,
                true,
                true
        );

        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("Mattoni di pietra"));
        assertTrue(result.contains("Sud"));
    }

    @Test
    @DisplayName("Verify formatFeedback with null target (MISS / Air) across all reading orders")
    void testNullTargetMiss() {
        for (CrosshairReadingOrder order : CrosshairReadingOrder.values()) {
            String result = CrosshairFeedbackManager.formatFeedback(
                    null,
                    null,
                    "Nord-Ovest",
                    315,
                    "15 gradi Su",
                    order,
                    true,
                    true,
                    true,
                    true,
                    true
            );

            assertNotNull(result);
            assertFalse(result.isBlank());
            assertTrue(result.contains("Nord-Ovest"));
            assertFalse(result.startsWith(","));
            assertFalse(result.startsWith(":"));
        }
    }

    @Test
    @DisplayName("Verify formatFeedback when orientation tokens are disabled (Target only)")
    void testOrientationDisabled() {
        String result = CrosshairFeedbackManager.formatFeedback(
                "Mattoni di pietra",
                2.0,
                "Sud",
                180,
                "Dritto",
                CrosshairReadingOrder.TARGET_FIRST,
                true,
                true,
                false,
                false,
                false
        );

        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("Mattoni di pietra"));
        assertFalse(result.contains("Sud"));
    }

    @Test
    @DisplayName("Verify formatFeedback when all tokens are disabled produces empty string")
    void testAllTokensDisabled() {
        String result = CrosshairFeedbackManager.formatFeedback(
                "Mattoni di pietra",
                2.0,
                "Sud",
                180,
                "Dritto",
                CrosshairReadingOrder.TARGET_FIRST,
                false,
                false,
                false,
                false,
                false
        );

        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Verify formatFeedback with elevation token included")
    void testFormatFeedbackWithElevation() {
        String result = CrosshairFeedbackManager.formatFeedback(
                "Mattoni di pietra",
                "1 blocco sopra",
                3.0,
                "Sud",
                180,
                "Dritto",
                CrosshairReadingOrder.TARGET_FIRST,
                true,
                true,
                true,
                true,
                true
        );

        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("Mattoni di pietra"));
        assertTrue(result.contains("1 blocco sopra"));
        assertTrue(result.contains("Sud"));
    }

    @Test
    @DisplayName("Verify formatElevationText across all styles and delta values")
    void testFormatElevationTextStyles() {
        // Delta > 0 (Single and Multiple)
        assertEquals("1 blocco sopra", CrosshairFeedbackManager.formatElevationText(1, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DESCRIPTIVE, false));
        assertEquals("3 blocchi sopra", CrosshairFeedbackManager.formatElevationText(3, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DESCRIPTIVE, false));
        assertEquals("+1 blocco", CrosshairFeedbackManager.formatElevationText(1, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.COMPACT, false));
        assertEquals("+3 blocchi", CrosshairFeedbackManager.formatElevationText(3, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.COMPACT, false));
        assertEquals("+2", CrosshairFeedbackManager.formatElevationText(2, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DELTA_ONLY, false));

        // Delta < 0 (Single and Multiple)
        assertEquals("1 blocco sotto", CrosshairFeedbackManager.formatElevationText(-1, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DESCRIPTIVE, false));
        assertEquals("4 blocchi sotto", CrosshairFeedbackManager.formatElevationText(-4, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DESCRIPTIVE, false));
        assertEquals("-1 blocco", CrosshairFeedbackManager.formatElevationText(-1, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.COMPACT, false));
        assertEquals("-4 blocchi", CrosshairFeedbackManager.formatElevationText(-4, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.COMPACT, false));
        assertEquals("-3", CrosshairFeedbackManager.formatElevationText(-3, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DELTA_ONLY, false));

        // Delta == 0 with narrateSameLevel false (should return null)
        assertNull(CrosshairFeedbackManager.formatElevationText(0, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DESCRIPTIVE, false));

        // Delta == 0 with narrateSameLevel true
        assertEquals("a livello piedi", CrosshairFeedbackManager.formatElevationText(0, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DESCRIPTIVE, true));
        assertEquals("livello 0", CrosshairFeedbackManager.formatElevationText(0, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.COMPACT, true));
        assertEquals("0", CrosshairFeedbackManager.formatElevationText(0, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DELTA_ONLY, true));

        // Null delta
        assertNull(CrosshairFeedbackManager.formatElevationText(null, org.mcaccess.minecraftaccess.Config.NarrateCrosshair.ElevationNarrationStyle.DESCRIPTIVE, true));
    }

    @Test
    @DisplayName("Verify presence of all CrosshairFeedbackManager and Elevation I18N keys in Italian and English")
    void testI18NKeysPresence() {
        String[] requiredKeys = {
                "minecraft_access.crosshair_elevation.down_multiple.compact",
                "minecraft_access.crosshair_elevation.down_multiple.descriptive",
                "minecraft_access.crosshair_elevation.down_single.compact",
                "minecraft_access.crosshair_elevation.down_single.descriptive",
                "minecraft_access.crosshair_elevation.down.delta_only",
                "minecraft_access.crosshair_elevation.same_level.compact",
                "minecraft_access.crosshair_elevation.same_level.delta_only",
                "minecraft_access.crosshair_elevation.same_level.descriptive",
                "minecraft_access.crosshair_elevation.up_multiple.compact",
                "minecraft_access.crosshair_elevation.up_multiple.descriptive",
                "minecraft_access.crosshair_elevation.up_single.compact",
                "minecraft_access.crosshair_elevation.up_single.descriptive",
                "minecraft_access.crosshair_elevation.up.delta_only",
                "minecraft_access.crosshair_feedback.at_distance",
                "minecraft_access.crosshair_feedback.block_and_cardinal",
                "minecraft_access.crosshair_feedback.block_then_facing",
                "minecraft_access.crosshair_feedback.distance_blocks",
                "minecraft_access.crosshair_feedback.distance_blocks_single",
                "minecraft_access.crosshair_feedback.facing_then_block",
                "minecraft_access.direction.face_bottom",
                "minecraft_access.direction.face_east",
                "minecraft_access.direction.face_north",
                "minecraft_access.direction.face_south",
                "minecraft_access.direction.face_top",
                "minecraft_access.direction.face_west",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.blockFaceNarrationMode",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.blockFaceNarrationMode.COMPACT",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.blockFaceNarrationMode.DESCRIPTIVE",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.blockFaceNarrationMode.OFF",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.blockFaceNarrationMode.TOP_BOTTOM_ONLY",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.includeBlock",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.includeCardinal",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.includeCompassDegrees",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.includeDistance",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.includePitchAngle",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.readingOrder",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.readingOrder.ORIENTATION_FIRST",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.readingOrder.TARGET_CARDINAL_INLINE",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.readingOrder.TARGET_FIRST",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.feedbackMode",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.feedbackMode.OFF",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.feedbackMode.SOUND_AND_VOICE",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.feedbackMode.SOUND_ONLY",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.feedbackMode.VOICE_ONLY",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.narrationStyle",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.narrationStyle.COMPACT",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.narrationStyle.DELTA_ONLY",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.narrationStyle.DESCRIPTIVE",
                "text.autoconfig.minecraft-access.option.narrateCrosshair.relativePositionSoundCue.narrateSameLevel"
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

    @Test
    @DisplayName("Verify strict alphabetical ordering of lang JSON keys for CI compliance")
    void testStrictAlphabeticalJsonOrdering() {
        String[] langFiles = {
                "/assets/minecraft_access/lang/it_it.json",
                "/assets/minecraft_access/lang/en_us.json"
        };

        for (String path : langFiles) {
            var stream = getClass().getResourceAsStream(path);
            assertNotNull(stream, "Could not find resource: " + path);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();

            List<String> keys = new ArrayList<>(json.keySet());
            List<String> sortedKeys = new ArrayList<>(keys);
            Collections.sort(sortedKeys);

            for (int i = 0; i < keys.size(); i++) {
                assertEquals(sortedKeys.get(i), keys.get(i), "Unsorted key at index " + i + " in " + path);
            }
        }
    }
}
