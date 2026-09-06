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

    @Test
    @DisplayName("manual crouch is untouched when entering GUI and does not query the raw probe")
    void manualCrouchUntouchedWhenEnteringGui() {
        java.util.concurrent.atomic.AtomicInteger probeReads = new java.util.concurrent.atomic.AtomicInteger();
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> {
            probeReads.incrementAndGet();
            return new CrouchIntent(true, true);
        }, writes::add);

        // Apply manual crouch before opening GUI
        guard.reconcileCrouchState();
        assertEquals(List.of(true), writes);
        assertEquals(1, probeReads.get());

        // Suspend for GUI
        guard.suspendForGui();

        // No new writes, probe was NOT queried by suspendForGui, system token is false
        assertEquals(List.of(true), writes);
        assertEquals(1, probeReads.get(), "suspendForGui() must never query the raw probe");
        assertFalse(guard.isSystemOverrideActive());
        assertNull(guard.getCurrentAllowedDescentId());
    }

    @Test
    @DisplayName("system safety token is released exactly once upon GUI suspension (idempotent)")
    void systemOverrideReleasedExactlyOnceUponGuiSuspension() {
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, true), writes::add);

        guard.engageFallProtection();
        assertEquals(List.of(true), writes);

        // Suspend once
        guard.suspendForGui();
        assertEquals(List.of(true, false), writes);
        assertFalse(guard.isSystemOverrideActive());
        assertNull(guard.getCurrentAllowedDescentId());

        // Suspend second time (simulating subsequent ticks while GUI remains open)
        guard.suspendForGui();
        assertEquals(List.of(true, false), writes, "Subsequent GUI ticks must be idempotent and produce zero writes");
    }

    @Test
    @DisplayName("Shift pressed solely inside GUI does not write to the port")
    void shiftPressedInsideGuiDoesNotWritePort() {
        java.util.concurrent.atomic.AtomicInteger probeReads = new java.util.concurrent.atomic.AtomicInteger();
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> {
            probeReads.incrementAndGet();
            return new CrouchIntent(true, true);
        }, writes::add);

        guard.suspendForGui();

        assertTrue(writes.isEmpty(), "Zero port writes when suspendForGui is invoked with physical Shift held");
        assertEquals(0, probeReads.get(), "Zero probe reads during suspendForGui");
        assertFalse(guard.isSystemOverrideActive());
    }

    @Test
    @DisplayName("manual crouch resumes naturally upon GUI exit if physical Shift is still held")
    void manualCrouchResumesNaturallyWhenGuiCloses() {
        AtomicReference<CrouchIntent> intent = new AtomicReference<>(new CrouchIntent(false, true));
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(intent::get, writes::add);

        guard.engageFallProtection();
        guard.suspendForGui();
        assertEquals(List.of(true, false), writes);

        // User keeps holding Shift when closing GUI
        intent.set(new CrouchIntent(true, true));
        guard.reconcileCrouchState();

        assertEquals(List.of(true, false, true), writes);
        assertFalse(guard.isSystemOverrideActive(), "System safety token must not be re-engaged by manual input");
    }

    @Test
    @DisplayName("no sticky sneak when GUI closes if physical Shift was released")
    void noStickySneakWhenGuiClosesWithoutShift() {
        AtomicReference<CrouchIntent> intent = new AtomicReference<>(new CrouchIntent(false, true));
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(intent::get, writes::add);

        guard.engageFallProtection();
        guard.suspendForGui();
        assertEquals(List.of(true, false), writes);

        // GUI closed, Shift was released
        guard.reconcileCrouchState();

        assertEquals(List.of(true, false), writes, "No additional crouch writes when Shift is not pressed");
        assertFalse(guard.isSystemOverrideActive());
    }

    @Test
    @DisplayName("descent authorization is always revoked by GUI suspension")
    void descentAuthorizationAlwaysRevokedByGuiSuspension() {
        List<Boolean> writes = new ArrayList<>();
        SafetyMovementGuard guard = new SafetyMovementGuard(() -> new CrouchIntent(false, true), writes::add);

        guard.engageFallProtection();
        guard.allowValidatedDescent(LADDER_ID);
        assertTrue(guard.isDescentAllowedFor(LADDER_ID));

        guard.suspendForGui();

        assertFalse(guard.isDescentAllowedFor(LADDER_ID));
        assertNull(guard.getCurrentAllowedDescentId());
        assertFalse(guard.isSystemOverrideActive());
    }
}
