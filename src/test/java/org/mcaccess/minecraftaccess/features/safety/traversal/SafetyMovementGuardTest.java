package org.mcaccess.minecraftaccess.features.safety.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SafetyMovementGuard: system token and raw crouch ownership")
class SafetyMovementGuardTest {

    private static final String LADDER_ID = "ladder:10,68,4";

    @Test
    @DisplayName("engaging protection writes the effective crouch state through the port")
    void engageFallProtectionEngagesSystemToken() {
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, true), writes::add);

        guard.engageFallProtection();

        assertTrue(guard.isSystemOverrideActive());
        assertNull(guard.getCurrentAllowedDescentId());
        assertEquals(List.of(true), writes);
    }

    @Test
    @DisplayName("validated descent releases a synthetic Shift when raw Shift is not held")
    void validatedDescentBreaksTheFormerSyntheticShiftDeadlock() {
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, true), writes::add);

        guard.engageFallProtection();
        guard.allowValidatedDescent(LADDER_ID);

        assertFalse(guard.isSystemOverrideActive());
        assertTrue(guard.isDescentAllowedFor(LADDER_ID));
        assertEquals(List.of(true, false), writes,
                "The raw probe remains false even after the guard itself wrote synthetic Shift=true.");
    }

    @Test
    @DisplayName("manual physical Shift remains effective after the system token is released")
    void validatedDescentPreservesManualCrouch() {
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(true, true), writes::add);

        guard.engageFallProtection();
        guard.allowValidatedDescent(LADDER_ID);

        assertFalse(guard.isSystemOverrideActive());
        assertTrue(guard.isDescentAllowedFor(LADDER_ID));
        assertEquals(List.of(true), writes);
    }

    @Test
    @DisplayName("unreliable raw input cannot open a validated-descent authorization")
    void unreliableRawInputFailsClosed() {
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, false), writes::add);

        guard.engageFallProtection();
        guard.allowValidatedDescent(LADDER_ID);

        assertTrue(guard.isSystemOverrideActive());
        assertNull(guard.getCurrentAllowedDescentId());
        assertEquals(List.of(true), writes);
    }

    @Test
    @DisplayName("revoking a descent removes its authorization immediately")
    void revokeValidatedDescentClearsOnlyTheAuthorization() {
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, true), writes::add);

        guard.engageFallProtection();
        guard.allowValidatedDescent(LADDER_ID);
        guard.revokeValidatedDescent();

        assertFalse(guard.isSystemOverrideActive());
        assertNull(guard.getCurrentAllowedDescentId());
        assertEquals(List.of(true, false), writes);
    }

    @Test
    @DisplayName("a later manual Shift change is reconciled without restoring the system token")
    void reconciliationFollowsPhysicalInputAfterDescent() {
        AtomicReference<CrouchIntent> intent = new AtomicReference<>(new CrouchIntent(false, true));
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(intent::get, writes::add);

        guard.engageFallProtection();
        guard.allowValidatedDescent(LADDER_ID);
        intent.set(new CrouchIntent(true, true));
        guard.reconcileCrouchState();
        intent.set(new CrouchIntent(false, true));
        guard.reconcileCrouchState();

        assertFalse(guard.isSystemOverrideActive());
        assertEquals(List.of(true, false, true, false), writes);
    }
}
