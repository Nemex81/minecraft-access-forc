package org.mcaccess.minecraftaccess.features;

import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcaccess.minecraftaccess.features.crosshair.ObstacleNarrationContext;

/**
 * Pure composer for obstacle narration messages.
 * Single source of truth for formatting obstacle warnings with distance and crosshair context,
 * shared identically between legacy direct bypass and cognitive event pipeline.
 * Fully decoupled from Minecraft client/player/level state for deterministic testing.
 */
public final class ObstacleNarrationComposer {

    private ObstacleNarrationComposer() {
    }

    public static String getFrontPrefix() {
        String frontPrefix = I18n.get("minecraft_access.obstacle_detector.dir_forward");
        if (frontPrefix.equals("minecraft_access.obstacle_detector.dir_forward") || frontPrefix.startsWith("minecraft_access.")) {
            return "Davanti";
        }
        return frontPrefix;
    }

    public static String composeFinalNarration(
            @NotNull String obstacleMsg,
            int obstacleDistance,
            @Nullable ObstacleNarrationContext crosshairContext
    ) {
        if (obstacleMsg.isBlank()) {
            return "";
        }
        boolean isFrontal = obstacleMsg.startsWith(getFrontPrefix());
        return composeFinalNarration(obstacleMsg, isFrontal, obstacleDistance, crosshairContext);
    }

    public static String composeFinalNarration(
            @NotNull String obstacleMsg,
            boolean isFrontal,
            int obstacleDistance,
            @Nullable ObstacleNarrationContext crosshairContext
    ) {
        if (obstacleMsg.isBlank()) {
            return "";
        }

        String frontPrefix = getFrontPrefix();

        if (isFrontal) {
            int distance = Math.max(1, obstacleDistance);
            String distStr = (distance <= 1)
                    ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco")
                    : getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", distance);
            return getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", obstacleMsg, distStr);
        } else {
            if (crosshairContext != null && crosshairContext.targetNarration() != null && !crosshairContext.targetNarration().isBlank()) {
                int targetDist = (crosshairContext.targetDistance() != null) ? crosshairContext.targetDistance() : 1;
                String distStr = (targetDist > 1)
                        ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", targetDist)
                        : getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco");
                String frontTarget = getI18nString("minecraft_access.crosshair_feedback.at_distance", "%s, a %s", crosshairContext.targetNarration(), distStr);
                return obstacleMsg + ". " + frontPrefix + ": " + frontTarget;
            } else {
                return obstacleMsg;
            }
        }
    }

    private static String getI18nString(String key, String fallbackFormat, Object... args) {
        String result = I18n.get(key, args);
        if (result.equals(key) || result.startsWith("minecraft_access.")) {
            return String.format(fallbackFormat, args);
        }
        return result;
    }
}
