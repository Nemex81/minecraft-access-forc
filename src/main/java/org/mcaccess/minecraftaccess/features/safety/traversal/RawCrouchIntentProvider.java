package org.mcaccess.minecraftaccess.features.safety.traversal;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * Reads the physical Shift keys through GLFW. It deliberately never consults
 * {@code KeyMapping.isDown()}, because that value also contains the synthetic
 * state written by {@link MinecraftSneakOverridePort}.
 */
public final class RawCrouchIntentProvider implements CrouchIntentProbe {

    @Override
    public @NotNull CrouchIntent readIntent() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                return new CrouchIntent(false, false);
            }

            Window window = client.getWindow();
            if (window == null) {
                return new CrouchIntent(false, false);
            }

            boolean pressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            return new CrouchIntent(pressed, true);
        } catch (RuntimeException ignored) {
            // There is no trustworthy raw state during client start-up or headless tests.
            return new CrouchIntent(false, false);
        }
    }
}
