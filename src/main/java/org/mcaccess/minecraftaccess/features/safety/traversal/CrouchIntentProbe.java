package org.mcaccess.minecraftaccess.features.safety.traversal;

@FunctionalInterface
public interface CrouchIntentProbe {
    /**
     * Returns true if the user is physically pressing the crouch / sneak input on keyboard/controller.
     */
    boolean isPhysicalCrouchHeld();
}
