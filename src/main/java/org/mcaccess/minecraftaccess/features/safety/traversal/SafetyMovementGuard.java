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
public final class SafetyMovementGuard implements ControlledDescentPort {

    private boolean systemOverrideActive;
    private @Nullable String currentAllowedDescentId;
    private @Nullable String activeLeaseColumnId;
    private boolean leaseRequiresSneak;
    private @Nullable Boolean lastAppliedCrouch;
    private final CrouchIntentProbe intentProbe;
    private final SneakOverridePort sneakPort;

    private static volatile SafetyMovementGuard defaultInstance;

    public SafetyMovementGuard(@NotNull CrouchIntentProbe intentProbe, @NotNull SneakOverridePort sneakPort) {
        this.intentProbe = intentProbe;
        this.sneakPort = sneakPort;
    }

    public static SafetyMovementGuard createDefault() {
        return new SafetyMovementGuard(new RawCrouchIntentProvider(), new MinecraftSneakOverridePort());
    }

    public static SafetyMovementGuard getDefaultInstance() {
        if (defaultInstance == null) {
            synchronized (SafetyMovementGuard.class) {
                if (defaultInstance == null) {
                    defaultInstance = createDefault();
                }
            }
        }
        return defaultInstance;
    }

    public static void setDefaultInstance(@Nullable SafetyMovementGuard instance) {
        defaultInstance = instance;
    }

    /**
     * Engages the generic fail-safe crouch token. A validated climb lease is
     * authoritative for the expected edge and cannot be cancelled by a
     * proximity tick that has no horizontal movement vector (D16).
     */
    public void engageFallProtection() {
        if (activeLeaseColumnId != null && activeLeaseColumnId.equals(currentAllowedDescentId)) {
            reconcileCrouchState();
            return;
        }
        currentAllowedDescentId = null;
        systemOverrideActive = true;
        reconcileCrouchState();
    }

    /**
     * Releases the system token only after a validated descent and a reliable
     * raw-input read. Physical Shift remains authoritative.
     */
    public void allowValidatedDescent(@NotNull String descentColumnId) {
        if (activeLeaseColumnId != null && !activeLeaseColumnId.equals(descentColumnId)) {
            reconcileCrouchState();
            return;
        }
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
        if (activeLeaseColumnId != null && activeLeaseColumnId.equals(currentAllowedDescentId)) {
            reconcileCrouchState();
            return;
        }
        currentAllowedDescentId = null;
        reconcileCrouchState();
    }

    @Override
    public void acquireDescentLease(@NotNull String columnId, boolean requiresSneak) {
        this.activeLeaseColumnId = columnId;
        this.leaseRequiresSneak = requiresSneak;
        allowValidatedDescent(columnId);
        if (!columnId.equals(currentAllowedDescentId)) {
            this.activeLeaseColumnId = null;
            this.leaseRequiresSneak = false;
            systemOverrideActive = true;
            reconcileCrouchState();
        }
    }

    @Override
    public void renewDescentLease(@NotNull String columnId) {
        if (columnId.equals(activeLeaseColumnId)) {
            allowValidatedDescent(columnId);
        }
    }

    @Override
    public void releaseDescentLease(@NotNull String columnId) {
        if (columnId.equals(activeLeaseColumnId)) {
            this.activeLeaseColumnId = null;
            this.leaseRequiresSneak = false;
            revokeValidatedDescent();
        }
    }

    @Override
    public boolean hasActiveDescentLease() {
        return activeLeaseColumnId != null;
    }

    @Override
    public boolean isDescentLeaseActiveFor(@NotNull String columnId) {
        return columnId.equals(activeLeaseColumnId) && columnId.equals(currentAllowedDescentId);
    }

    /** Hard fail-closed revocation reserved for lifecycle or validated hazards. */
    public void revokeActiveDescentLeaseForHazard() {
        activeLeaseColumnId = null;
        leaseRequiresSneak = false;
        currentAllowedDescentId = null;
        systemOverrideActive = true;
        reconcileCrouchState();
    }

    /** Clears the system token when fall protection no longer owns crouch. */
    public void clearSystemOverride() {
        currentAllowedDescentId = null;
        activeLeaseColumnId = null;
        leaseRequiresSneak = false;
        systemOverrideActive = false;
        reconcileCrouchState();
    }

    /**
     * Suspends traversal safety while a GUI owns keyboard input.
     * It never reads raw input and releases only a crouch previously owned by
     * the system safety token.
     */
    public void suspendForGui() {
        boolean releaseSystemCrouch = systemOverrideActive || leaseRequiresSneak;
        currentAllowedDescentId = null;
        activeLeaseColumnId = null;
        leaseRequiresSneak = false;
        systemOverrideActive = false;

        if (releaseSystemCrouch) {
            applyIfChanged(false);
        }
    }

    /** Reconciles the only permitted writer with the current token and raw input. */
    public void reconcileCrouchState() {
        reconcileCrouchState(intentProbe.readIntent());
    }

    private void reconcileCrouchState(@NotNull CrouchIntent intent) {
        if (!intent.reliable()) {
            // Unknown input must never open a descent or release an active safety token.
            if (systemOverrideActive || leaseRequiresSneak) {
                applyIfChanged(true);
            }
            return;
        }

        applyIfChanged(systemOverrideActive || intent.pressed() || leaseRequiresSneak);
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
