package org.mcaccess.minecraftaccess.features.safety.traversal;

@FunctionalInterface
public interface CrouchIntentProbe {
    /**
     * Reads physical crouch intent independently from state written by the safety system.
     */
    CrouchIntent readIntent();
}
