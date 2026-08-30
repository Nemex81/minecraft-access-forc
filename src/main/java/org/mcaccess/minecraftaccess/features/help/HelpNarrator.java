package org.mcaccess.minecraftaccess.features.help;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.NarrationPriority;

/**
 * Dedicated narrator for educational instructions and contextual hints.
 * Applies a dynamic time-based shield to prevent background scanners (crosshair, obstacle warnings)
 * from truncating spoken help messages.
 */
@Slf4j
public final class HelpNarrator {

    private HelpNarrator() {
    }

    /**
     * Calculate dynamic shield duration based on word count.
     * Formula: (words * 280ms) + 600ms buffer.
     */
    public static long calculateShieldDuration(String text) {
        if (text == null || text.isBlank()) {
            return 500L;
        }
        String[] words = text.trim().split("\\s+");
        return (words.length * 280L) + 600L;
    }

    /**
     * Narrates a help message with maximum priority and anti-truncation shield.
     *
     * @param text      the instruction or hint to speak
     * @param isMission true if this is an active Academy mission step, false for contextual hints
     */
    public static void narrateHelp(String text, boolean isMission) {
        if (text == null || text.isBlank()) {
            return;
        }

        boolean priorityOverride = Config.getInstance().helpSettings.helpPriorityOverride;
        long shieldDuration = calculateShieldDuration(text);

        if (priorityOverride) {
            NarrationPriority.narrateSalient(text, shieldDuration);
        } else {
            MainClass.narrate(text, true);
        }
    }

    /**
     * Plays a pleasant chime sound when a mission or milestone is completed.
     */
    public static void playSuccessChime() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.getSoundManager() != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2f, 0.6f));
        }
    }

    /**
     * Plays a subtle reminder chime when a new contextual hint is delivered.
     */
    public static void playHintChime() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.getSoundManager() != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 0.4f));
        }
    }
}
