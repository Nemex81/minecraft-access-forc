package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

/**
 * Provides raw physical crouch (Shift) intent from the Minecraft client.
 * Implements {@link CrouchIntentProbe} so it can be passed to {@link SafetyMovementGuard}.
 */
public class RawCrouchIntentProvider implements CrouchIntentProbe {
    @Override
    public boolean isPhysicalCrouchHeld() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.options != null && client.options.keyShift != null) {
                return client.options.keyShift.isDown();
            }
        } catch (Exception ignored) {
            // headless or test environment
        }
        return false;
    }

    /**
     * Reads the raw crouch intent together with a reliability flag.
     * The intent is considered reliable when we can successfully query the client.
     */
    public @NotNull CrouchIntent readIntent() {
        boolean pressed = isPhysicalCrouchHeld();
        // If we reached this point we were able to query the client, so we consider it reliable.
        return new CrouchIntent(pressed, true);
    }
}
