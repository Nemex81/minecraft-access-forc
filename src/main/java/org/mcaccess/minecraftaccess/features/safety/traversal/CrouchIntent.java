package org.mcaccess.minecraftaccess.features.safety.traversal;

/**
 * Immutable data class representing the crouch intent.
 */
public record CrouchIntent(boolean pressed, boolean reliable) {}
