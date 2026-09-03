package org.mcaccess.minecraftaccess.features.safety.traversal;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the system crouch token and reconciles it with raw user intent. The
 * guard never reads the effective {@code KeyMapping} state: that state can
 * have been written by this class itself.
 */
@Slf4j
public final class SafetyMovementGuard {

    private boolean systemOverrideActive;
    private @Nullable String currentAllowedDescentId;
    private @Nullable Boolean lastAppliedCrouch;
    private final CrouchIntentProbe intentProbe;
    private final SneakOverridePort sneakPort;

    public SafetyMovementGuard(@NotNull CrouchIntentProbe intentProbe, @NotNull SneakOverridePort sneakPort) {
        this.intentProbe = intentProbe;
        this.sneakPort = sneakPort;
    }

    public static SafetyMovementGuard createDefault() {
        return new SafetyMovementGuard(new RawCrouchIntentProvider(), new MinecraftSneakOverridePort());
    }

    /** Engages the fail-safe crouch token and invalidates every previous descent. */
    public void engageFallProtection() {
        currentAllowedDescentId = null;
        systemOverrideActive = true;
        reconcileCrouchState();
    }

    /**
     * Releases the system token only after a validated descent and a reliable
     * raw-input read. Physical Shift remains authoritative.
     */
    public void allowValidatedDescent(@NotNull String descentColumnId) {
        CrouchIntent intent = intentProbe.readIntent();
        if (!intent.reliable()) {
            log.debug("Keeping fall protection active because raw crouch intent is unavailable");
            reconcileCrouchState(intent);
            return;
        }

        currentAllowedDescentId = descentColumnId;
        systemOverrideActive = false;
        reconcileCrouchState(intent);
    }

    /** Revokes a previously validated descent without altering manual crouch intent. */
    public void revokeValidatedDescent() {
        currentAllowedDescentId = null;
        reconcileCrouchState();
    }

    /** Clears the system token when fall protection no longer owns crouch. */
    public void clearSystemOverride() {
        currentAllowedDescentId = null;
        systemOverrideActive = false;
        reconcileCrouchState();
    }

    /** Reconciles the only permitted writer with the current token and raw input. */
    public void reconcileCrouchState() {
        reconcileCrouchState(intentProbe.readIntent());
    }

    private void reconcileCrouchState(@NotNull CrouchIntent intent) {
        if (!intent.reliable()) {
            // Unknown input must never open a descent or release an active safety token.
            if (systemOverrideActive) {
                applyIfChanged(true);
            }
            return;
        }

        applyIfChanged(systemOverrideActive || intent.pressed());
    }

    private void applyIfChanged(boolean crouching) {
        if (Boolean.valueOf(crouching).equals(lastAppliedCrouch)) {
            return;
        }
        sneakPort.applyEffectiveCrouch(crouching);
        lastAppliedCrouch = crouching;
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
}
