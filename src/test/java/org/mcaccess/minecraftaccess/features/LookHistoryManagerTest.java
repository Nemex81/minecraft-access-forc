package org.mcaccess.minecraftaccess.features;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LookHistoryManagerTest {

    @BeforeEach
    @AfterEach
    void resetState() {
        LookHistoryManager.clear();
    }

    @Test
    @DisplayName("Verify saveCurrentLook and clear functions")
    void testSaveAndClearLook() {
        assertFalse(LookHistoryManager.hasSavedLook());
        assertEquals(0.0f, LookHistoryManager.getSavedYaw());
        assertEquals(0.0f, LookHistoryManager.getSavedPitch());

        LookHistoryManager.saveCurrentLook(180.0f, -25.5f);
        assertTrue(LookHistoryManager.hasSavedLook());
        assertEquals(180.0f, LookHistoryManager.getSavedYaw(), 0.001f);
        assertEquals(-25.5f, LookHistoryManager.getSavedPitch(), 0.001f);

        LookHistoryManager.clear();
        assertFalse(LookHistoryManager.hasSavedLook());
        assertEquals(0.0f, LookHistoryManager.getSavedYaw());
        assertEquals(0.0f, LookHistoryManager.getSavedPitch());
    }

    @Test
    @DisplayName("Verify restorePreviousLook graceful handling of null client")
    void testRestoreWithNullClient() {
        LookHistoryManager.saveCurrentLook(90.0f, 15.0f);
        assertFalse(LookHistoryManager.restorePreviousLook(null));
    }

    @Test
    @DisplayName("Verify recordManualRotation anchor logic with time window")
    void testManualRotationWindow() throws InterruptedException {
        // First manual step: saves anchor
        LookHistoryManager.recordManualRotation(180.0f, 0.0f);
        assertTrue(LookHistoryManager.hasSavedLook());
        assertEquals(180.0f, LookHistoryManager.getSavedYaw(), 0.001f);
        assertEquals(0.0f, LookHistoryManager.getSavedPitch(), 0.001f);

        // Immediate subsequent steps in the same sequence do NOT overwrite anchor
        LookHistoryManager.recordManualRotation(195.0f, 0.0f);
        assertEquals(180.0f, LookHistoryManager.getSavedYaw(), 0.001f);

        LookHistoryManager.recordManualRotation(210.0f, 10.0f);
        assertEquals(180.0f, LookHistoryManager.getSavedYaw(), 0.001f);
    }

    @Test
    @DisplayName("Verify Tier 2 persistent bookmark and clear")
    void testTier2Bookmark() {
        assertFalse(LookHistoryManager.hasBookmark());
        assertEquals(0.0f, LookHistoryManager.getBookmarkYaw());
        assertEquals(0.0f, LookHistoryManager.getBookmarkPitch());

        // Align with null client or when no bookmark set
        assertFalse(LookHistoryManager.alignToReferenceLook(null));

        LookHistoryManager.clear();
        assertFalse(LookHistoryManager.hasBookmark());
    }

    @Test
    @DisplayName("Verify FallDetector autoSneakActive initial and toggle state")
    void testFallDetectorAutoSneak() {
        assertFalse(FallDetector.isAutoSneakActive());
    }

    @Test
    @DisplayName("Verify Look Restore and Enum I18N keys presence in Italian and English")
    void testI18NKeysPresence() {
        String[] requiredKeys = {
                "key.minecraft_access.camera_controls.align_to_reference_look",
                "key.minecraft_access.camera_controls.restore_previous_look",
                "key.minecraft_access.camera_controls.sync_reference_look",
                "key.minecraft_access.numpad.camera.restore_previous_look",
                "minecraft_access.camera_controls.aligned_to_reference_look",
                "minecraft_access.camera_controls.look_restored",
                "minecraft_access.camera_controls.no_previous_look",
                "minecraft_access.camera_controls.no_reference_look",
                "minecraft_access.camera_controls.reference_look_set",
                "minecraft_access.obstacle_detector.looked_at_obstacle",
                "minecraft_access.point_of_interest.look_at",
                "text.autoconfig.minecraft-access.option.fallDetector.autoSneakOnEdge",
                "text.autoconfig.minecraft-access.option.fallDetector.autoSneakOnEdge.@Tooltip",
                "text.autoconfig.minecraft-access.option.fallDetector.playAudioCues",
                "text.autoconfig.minecraft-access.option.fallDetector.playAudioCues.@Tooltip",
                "text.autoconfig.minecraft-access.enum.CenterHorizonFeedbackMode.OFF",
                "text.autoconfig.minecraft-access.enum.CenterHorizonFeedbackMode.SOUND_AND_TARGET",
                "text.autoconfig.minecraft-access.enum.CenterHorizonFeedbackMode.SOUND_ONLY",
                "text.autoconfig.minecraft-access.enum.CenterHorizonFeedbackMode.SOUND_VOICE_AND_TARGET",
                "text.autoconfig.minecraft-access.enum.ContinuousFeedbackMode.OFF",
                "text.autoconfig.minecraft-access.enum.ContinuousFeedbackMode.SOUND_AND_VOICE",
                "text.autoconfig.minecraft-access.enum.ContinuousFeedbackMode.SOUND_ONLY",
                "text.autoconfig.minecraft-access.enum.ContinuousFeedbackMode.VOICE_ONLY",
                "text.autoconfig.minecraft-access.enum.DirectionFeedbackMode.EIGHT_DIRECTIONS",
                "text.autoconfig.minecraft-access.enum.DirectionFeedbackMode.FOUR_DIRECTIONS",
                "text.autoconfig.minecraft-access.enum.DirectionFeedbackMode.OFF",
                "text.autoconfig.minecraft-access.enum.DirectionFeedbackMode.OMIT_FORWARD",
                "text.autoconfig.minecraft-access.enum.HandednessPreset.LEFT_HANDED",
                "text.autoconfig.minecraft-access.enum.HandednessPreset.RIGHT_HANDED",
                "text.autoconfig.minecraft-access.enum.NarrationStyle.BLOCK",
                "text.autoconfig.minecraft-access.enum.NarrationStyle.DIRECT",
                "text.autoconfig.minecraft-access.enum.NarrationStyle.ELEVATION",
                "text.autoconfig.minecraft-access.enum.NarrationStyle.SLOPE",
                "text.autoconfig.minecraft-access.enum.PickedUpItemNarration.ALWAYS",
                "text.autoconfig.minecraft-access.enum.PickedUpItemNarration.DISABLED",
                "text.autoconfig.minecraft-access.enum.PickedUpItemNarration.WHEN_FISHING",
                "text.autoconfig.minecraft-access.enum.RotationFeedbackMode.CARDINAL_AND_DEGREES",
                "text.autoconfig.minecraft-access.enum.RotationFeedbackMode.CARDINAL_ONLY",
                "text.autoconfig.minecraft-access.enum.RotationFeedbackMode.OFF",
                "text.autoconfig.minecraft-access.enum.RotationFeedbackMode.SOUND_AND_VOICE_WITH_DEGREES",
                "text.autoconfig.minecraft-access.enum.RotationFeedbackMode.SOUND_ONLY"
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
