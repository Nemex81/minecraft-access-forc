package org.mcaccess.minecraftaccess.features.autowalk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.NarrateCrosshair;
import org.mcaccess.minecraftaccess.features.safety.fall.ProximityFallDetector;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rev MC-26.19: AutoWalk Sensory Quieting & Progression Refinement Tests (D0..D5)")
class AutoWalkSensoryQuietingRefinementTest {

    @Test
    @DisplayName("D0: Master module enabled switches are true by default")
    void testMasterModuleSwitchesDefaultTrue() {
        Config.FallDetector fallConfig = new Config.FallDetector();
        Config.ObstacleDetector obstacleConfig = new Config.ObstacleDetector();
        Config.AutoWalk autoWalkConfig = new Config.AutoWalk();

        assertTrue(fallConfig.enabled, "FallDetector.enabled must be true by default");
        assertTrue(obstacleConfig.enabled, "ObstacleDetector.enabled must be true by default");
        assertTrue(autoWalkConfig.enabled, "AutoWalk.enabled must be true by default");
    }

    @Test
    @DisplayName("D1: Default ProgressionFeedbackMode is SOUND_AND_VOICE")
    void testDefaultProgressionFeedbackMode() {
        Config.AutoWalk config = new Config.AutoWalk();
        assertEquals(
                Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE,
                config.progressionFeedbackMode,
                "Default progression feedback mode must be SOUND_AND_VOICE (preserving Luca's experience)"
        );
    }

    @Test
    @DisplayName("D2: shouldPlayNodeSound 4-state boolean matrix")
    void testShouldPlayNodeSoundMatrix() {
        // playNodeSoundCue = true
        assertTrue(AutoWalkMotor.shouldPlayNodeSound(true, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertTrue(AutoWalkMotor.shouldPlayNodeSound(true, Config.AutoWalk.ProgressionFeedbackMode.SOUND_ONLY));
        assertFalse(AutoWalkMotor.shouldPlayNodeSound(true, Config.AutoWalk.ProgressionFeedbackMode.VOICE_ONLY));
        assertFalse(AutoWalkMotor.shouldPlayNodeSound(true, Config.AutoWalk.ProgressionFeedbackMode.OFF));

        // playNodeSoundCue = false (master audio cue disabled)
        assertFalse(AutoWalkMotor.shouldPlayNodeSound(false, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertFalse(AutoWalkMotor.shouldPlayNodeSound(false, Config.AutoWalk.ProgressionFeedbackMode.SOUND_ONLY));
        assertFalse(AutoWalkMotor.shouldPlayNodeSound(false, Config.AutoWalk.ProgressionFeedbackMode.VOICE_ONLY));
        assertFalse(AutoWalkMotor.shouldPlayNodeSound(false, Config.AutoWalk.ProgressionFeedbackMode.OFF));
    }

    @Test
    @DisplayName("D2: shouldNarrateStepProgression cadence and modes")
    void testShouldNarrateStepProgression() {
        // SOUND_AND_VOICE: steps multiple of 5 and index mutated -> true
        assertTrue(AutoWalkMotor.shouldNarrateStepProgression(25, 5, 0, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertTrue(AutoWalkMotor.shouldNarrateStepProgression(20, 10, 5, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertTrue(AutoWalkMotor.shouldNarrateStepProgression(15, 15, 10, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertTrue(AutoWalkMotor.shouldNarrateStepProgression(10, 20, 15, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertTrue(AutoWalkMotor.shouldNarrateStepProgression(5, 25, 20, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));

        // Non-multiple of 5 -> false
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(24, 6, 5, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(21, 9, 5, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(19, 11, 10, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(1, 29, 25, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));

        // 0 remaining steps -> false (arrival handles arrival announcement)
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(0, 30, 25, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));

        // Negative or already announced index -> false
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(-1, 5, 0, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(20, 10, 10, Config.AutoWalk.ProgressionFeedbackMode.SOUND_AND_VOICE));

        // VOICE_ONLY mode -> behaves like SOUND_AND_VOICE for narration
        assertTrue(AutoWalkMotor.shouldNarrateStepProgression(20, 10, 5, Config.AutoWalk.ProgressionFeedbackMode.VOICE_ONLY));
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(19, 11, 10, Config.AutoWalk.ProgressionFeedbackMode.VOICE_ONLY));

        // SOUND_ONLY mode -> always false for voice narration
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(20, 10, 5, Config.AutoWalk.ProgressionFeedbackMode.SOUND_ONLY));

        // OFF mode -> always false for voice narration
        assertFalse(AutoWalkMotor.shouldNarrateStepProgression(20, 10, 5, Config.AutoWalk.ProgressionFeedbackMode.OFF));
    }

    @Test
    @DisplayName("D3: shouldSilenceCrosshairHarp 4-state boolean matrix")
    void testShouldSilenceCrosshairHarpMatrix() {
        // Both true -> silenced
        assertTrue(NarrateCrosshair.shouldSilenceCrosshairHarp(true, true));

        // AutoWalk active but config false -> NOT silenced
        assertFalse(NarrateCrosshair.shouldSilenceCrosshairHarp(true, false));

        // AutoWalk inactive but config true -> NOT silenced
        assertFalse(NarrateCrosshair.shouldSilenceCrosshairHarp(false, true));

        // Both false -> NOT silenced
        assertFalse(NarrateCrosshair.shouldSilenceCrosshairHarp(false, false));
    }

    @Test
    @DisplayName("D4bis: shouldSilenceFallAudioInAutoWalk 4-state boolean matrix")
    void testShouldSilenceFallAudioInAutoWalkMatrix() {
        // Both true -> silenced (xylophone and emergency anvil muted during AutoWalk)
        assertTrue(ProximityFallDetector.shouldSilenceFallAudioInAutoWalk(true, true));

        // AutoWalk active but silenceFallWarnings false -> NOT silenced
        assertFalse(ProximityFallDetector.shouldSilenceFallAudioInAutoWalk(true, false));

        // AutoWalk inactive (e.g. manual walking, halt, or takeover) but config true -> NOT silenced (anvil active!)
        assertFalse(ProximityFallDetector.shouldSilenceFallAudioInAutoWalk(false, true));

        // Both false -> NOT silenced
        assertFalse(ProximityFallDetector.shouldSilenceFallAudioInAutoWalk(false, false));
    }
}
