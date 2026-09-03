package org.mcaccess.minecraftaccess.features.safety.traversal;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Safety Movement Guard Ownership & Crouch Control Unit Tests (Rev MC-26.8)")
class SafetyMovementGuardTest {

    @Test
    @DisplayName("1. engageFallProtection engages system override and applies shift true")
    void testEngageFallProtection() {
        AtomicBoolean appliedState = new AtomicBoolean(false);
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> false);
        guard.setShiftStateApplier(appliedState::set);

        guard.engageFallProtection();

        assertTrue(guard.isSystemOverrideActive());
        assertTrue(appliedState.get());
        assertNull(guard.getCurrentAllowedDescentId());
    }

    @Test
    @DisplayName("2. allowValidatedDescent releases shift when user is NOT physically holding shift")
    void testAllowValidatedDescentReleasesShift() {
        AtomicBoolean appliedState = new AtomicBoolean(false);
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> false); // User is not pressing shift
        guard.setShiftStateApplier(appliedState::set);

        guard.engageFallProtection();
        assertTrue(appliedState.get());

        guard.allowValidatedDescent("ladder:10,68,4");

        assertFalse(guard.isSystemOverrideActive(), "System override must be deactivated for validated descent");
        assertFalse(appliedState.get(), "Shift must be released so player bounding box can enter ladder");
        assertTrue(guard.isDescentAllowedFor("ladder:10,68,4"));
    }

    @Test
    @DisplayName("3. allowValidatedDescent PRESERVES shift if user is physically holding shift key")
    void testAllowValidatedDescentPreservesManualShift() {
        AtomicBoolean appliedState = new AtomicBoolean(false);
        // User IS physically pressing shift with their finger!
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> true);
        guard.setShiftStateApplier(appliedState::set);

        guard.engageFallProtection();
        assertTrue(guard.isSystemOverrideActive());

        appliedState.set(true);
        guard.allowValidatedDescent("ladder:10,68,4");

        assertFalse(guard.isSystemOverrideActive(), "System override is cleared");
        assertTrue(appliedState.get(), "Physical shift MUST NOT be released when user is holding the key manually!");
        assertTrue(guard.isDescentAllowedFor("ladder:10,68,4"));
    }

    @Test
    @DisplayName("4. clearSystemOverride restores normal state and clears allowed descent")
    void testClearSystemOverride() {
        AtomicBoolean appliedState = new AtomicBoolean(false);
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> false);
        guard.setShiftStateApplier(appliedState::set);

        guard.engageFallProtection();
        guard.allowValidatedDescent("ladder:10,68,4");
        guard.clearSystemOverride();

        assertFalse(guard.isSystemOverrideActive());
        assertNull(guard.getCurrentAllowedDescentId());
        assertFalse(appliedState.get());
    }
}
