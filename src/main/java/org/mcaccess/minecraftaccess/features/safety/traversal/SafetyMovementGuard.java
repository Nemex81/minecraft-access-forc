package org.mcaccess.minecraftaccess.features.safety.traversal;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SafetyMovementGuard {

    private boolean systemOverrideActive = false;
    private @Nullable String currentAllowedDescentId = null;
    private final CrouchIntentProbe intentProbe;
    private Consumer<Boolean> shiftStateApplier;

    public SafetyMovementGuard(@NotNull CrouchIntentProbe intentProbe) {
        this.intentProbe = intentProbe;
        this.shiftStateApplier = state -> {
            try {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.options != null && client.options.keyShift != null) {
                    client.options.keyShift.setDown(state);
                }
                if (client != null && client.player != null) {
                    client.player.setShiftKeyDown(state);
                }
            } catch (Exception ignored) {
                // Headless resilience
            }
        };
    }

    public static SafetyMovementGuard createDefault() {
        return new SafetyMovementGuard(() -> {
            try {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.options != null && client.options.keyShift != null) {
                    return client.options.keyShift.isDown();
                }
            } catch (Exception ignored) {
            }
            return false;
        });
    }

    /**
     * Engages system crouch override to protect player from falling over dangerous edge.
     */
    public void engageFallProtection() {
        currentAllowedDescentId = null;
        systemOverrideActive = true;
        shiftStateApplier.accept(true);
    }

    /**
     * Temporarily releases system crouch override ONLY if a validated descent is active,
     * allowing player's bounding box to cross the edge and latch onto ladder rungs.
     * Preserves physical crouch if user is holding the key manually.
     */
    public void allowValidatedDescent(@NotNull String descentColumnId) {
        currentAllowedDescentId = descentColumnId;
        if (systemOverrideActive) {
            systemOverrideActive = false;
            // Only release logical shift if user is NOT physically pressing the shift key
            if (!intentProbe.isPhysicalCrouchHeld()) {
                shiftStateApplier.accept(false);
            }
        }
    }

    /**
     * Completely clears system crouch override (e.g. player stepped back onto solid ground).
     */
    public void clearSystemOverride() {
        currentAllowedDescentId = null;
        if (systemOverrideActive) {
            systemOverrideActive = false;
            if (!intentProbe.isPhysicalCrouchHeld()) {
                shiftStateApplier.accept(false);
            }
        }
    }

    public boolean isSystemOverrideActive() {
        return systemOverrideActive;
    }

    public boolean isDescentAllowedFor(@NotNull String descentColumnId) {
        return descentColumnId.equals(currentAllowedDescentId);
    }

    public @Nullable String getCurrentAllowedDescentId() {
        return currentAllowedDescentId;
    }

    // Package-private test seam for unit testing
    void setShiftStateApplier(@NotNull Consumer<Boolean> applier) {
        this.shiftStateApplier = applier;
    }
}
