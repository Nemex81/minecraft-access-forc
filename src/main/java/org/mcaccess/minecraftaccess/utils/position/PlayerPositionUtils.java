package org.mcaccess.minecraftaccess.utils.position;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import org.mcaccess.minecraftaccess.utils.NarrationUtils;

/**
 * Functions about getting player entity's position, facing direction etc.
 */
public final class PlayerPositionUtils {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final String POSITION_FORMAT = "{x}, {y}, {z}";

    private PlayerPositionUtils() {
    }

    public static String getNarratableXYZPosition() {
        return POSITION_FORMAT.replace("{x}", getNarratableXPos()).replace("{y}", getNarratableYPos()).replace("{z}", getNarratableZPos());
    }

    public static String getNarratableXPos() {
        assert CLIENT.player != null;
        return NarrationUtils.narrateNumber(CLIENT.player.position().x) + 'x';
    }

    public static String getNarratableYPos() {
        assert CLIENT.player != null;
        return NarrationUtils.narrateNumber(CLIENT.player.position().y) + 'y';
    }

    public static String getNarratableZPos() {
        assert CLIENT.player != null;
        return NarrationUtils.narrateNumber(CLIENT.player.position().z) + 'z';
    }

    /**
     * @return -90 (head up) ~ 90 (head down)
     */
    public static int getVerticalFacingDirection() {
        assert CLIENT.player != null;
        return (int) CLIENT.player.getRotationVector().x;
    }

    /**
     * Get the vertical direction in words.
     *
     * @return the vertical direction in words. null on error.
     */
    public static String getVerticalFacingDirectionInWords() {
        int angle = getVerticalFacingDirection();
        if (isBetween(angle, -90, -88)) {
            return I18n.get("minecraft_access.direction.up");
        } else if (isBetween(angle, -87, -3)) {
            return I18n.get("minecraft_access.direction.degrees", NarrationUtils.narrateNumber(-angle)) + ' ' + I18n.get("minecraft_access.direction.up");
        } else if (isBetween(angle, -2, 2)) {
            return I18n.get("minecraft_access.direction.straight");
        } else if (isBetween(angle, 3, 87)) {
            return I18n.get("minecraft_access.direction.degrees", NarrationUtils.narrateNumber(angle)) + ' ' + I18n.get("minecraft_access.direction.down");
        } else if (isBetween(angle, 88, 90)) {
            return I18n.get("minecraft_access.direction.down");
        } else {
            return null;
        }
    }

    public static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    public static int getHorizontalFacingDirectionInDegrees() {
        assert CLIENT.player != null;
        int angle = (int) CLIENT.player.getRotationVector().y;
        return angle % 360;
    }

    public static Orientation getHorizontalFacing() {
        int angle = getHorizontalFacingDirectionInDegrees();
        return Orientation.ofHorizontal(angle);
    }

    public static String getHorizontalFacingDirectionInWords() {
        return I18n.get("minecraft_access.direction." + getHorizontalFacing());
    }

    /**
     * Standard geographic 360-degree compass heading (0 = North, 90 = East, 180 = South, 270 = West).
     */
    public static int getCompassDegrees() {
        assert CLIENT.player != null;
        float rawYaw = (CLIENT.player.getYRot() % 360.0f + 360.0f) % 360.0f;
        int degrees = Math.round((rawYaw + 180.0f) % 360.0f);
        return degrees % 360;
    }

    /**
     * Returns the horizontal direction and compass degrees (e.g. "Nord, 0 gradi", "Nord-Est, 45 gradi").
     */
    public static String getHorizontalFacingAndDegreesInWords() {
        String direction = getHorizontalFacingDirectionInWords();
        int deg = getCompassDegrees();
        return direction + ", " + I18n.get("minecraft_access.direction.degrees", NarrationUtils.narrateNumber(deg));
    }

    /**
     * Returns full facing direction, optionally including horizontal degrees and vertical pitch.
     */
    public static String getFullFacingInWords(boolean includeDegrees) {
        String h = includeDegrees ? getHorizontalFacingAndDegreesInWords() : getHorizontalFacingDirectionInWords();
        String v = getVerticalFacingDirectionInWords();
        return I18n.get("minecraft_access.other.facing_direction", h + (v != null ? ", " + v : ""));
    }
}
