package org.mcaccess.minecraftaccess.features.safety.traversal;

/**
 * Port interface for applying effective crouch state.
 * Abstracts the write operation for shift key, enabling a single writer.
 */
public interface SneakOverridePort {
    /**
     * Apply the effective crouch state.
     * @param crouching true to engage crouch (sneak), false to release.
     */
    void applyEffectiveCrouch(boolean crouching);
}
