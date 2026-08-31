package org.mcaccess.minecraftaccess.utils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Utility for strict, exclusive evaluation of keyboard modifier keys (Ctrl, Alt, Shift).
 * Prevents overlapping / double-event triggers when modified key combinations are pressed.
 */
public final class ModifierUtils {

    private ModifierUtils() {
    }

    /**
     * @return true if Control key (Left or Right) is held down.
     */
    public static boolean hasControl() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        try {
            return client.hasControlDown();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return true if Alt key (Left or Right) is held down.
     */
    public static boolean hasAlt() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        try {
            return client.hasAltDown();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return true if Shift key (Left or Right) is held down.
     */
    public static boolean hasShift() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        try {
            Window window = client.getWindow();
            if (window == null) return false;
            return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return true if ANY modifier (Ctrl, Alt, Shift) is currently held down.
     */
    public static boolean hasAnyModifier() {
        return hasControl() || hasAlt() || hasShift();
    }

    /**
     * @return true if NO modifier is held down (clean Layer 0 / direct key press).
     */
    public static boolean hasNoModifiers() {
        return !hasControl() && !hasAlt() && !hasShift();
    }

    /**
     * @return true if ONLY Control is held down (and neither Alt nor Shift).
     */
    public static boolean hasControlOnly() {
        return hasControl() && !hasAlt() && !hasShift();
    }

    /**
     * @return true if ONLY Alt is held down (and neither Control nor Shift).
     */
    public static boolean hasAltOnly() {
        return hasAlt() && !hasControl() && !hasShift();
    }

    /**
     * @return true if ONLY Shift is held down (and neither Control nor Alt).
     */
    public static boolean hasShiftOnly() {
        return hasShift() && !hasControl() && !hasAlt();
    }
}
