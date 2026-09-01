package org.mcaccess.minecraftaccess.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils;

/**
 * Universal Look History Manager.
 * Stores previous camera look orientation (yaw and pitch) before automatic rotations
 * (e.g. Obstacle Inspection, Aim Assist, Survival Tracker, POI pointing)
 * and allows instant restoration via keyboard or numpad shortcuts.
 */
public final class LookHistoryManager {
    // Tier 1: Dynamic Look Undo (Short-term / Volatile)
    private static float savedYaw = 0.0f;
    private static float savedPitch = 0.0f;
    private static boolean hasSavedLook = false;
    private static long lastManualRotationTime = 0L;
    private static final long MANUAL_ROTATION_WINDOW_MS = 1500L;

    // Tier 2: Persistent Reference Bookmark (Long-term / Explicit)
    private static float bookmarkYaw = 0.0f;
    private static float bookmarkPitch = 0.0f;
    private static boolean hasBookmark = false;

    private LookHistoryManager() {
    }

    /**
     * Saves the current look orientation before an automatic rotation.
     *
     * @param yaw   The horizontal rotation in degrees.
     * @param pitch The vertical pitch in degrees.
     */
    public static void saveCurrentLook(float yaw, float pitch) {
        savedYaw = yaw;
        savedPitch = pitch;
        hasSavedLook = true;
    }

    /**
     * Records a manual step rotation (e.g. from I, J, K, L or numpad arrows).
     * Saves the initial starting look anchor if this is the start of a new rotation sequence
     * (i.e. more than MANUAL_ROTATION_WINDOW_MS has passed since the last step rotation).
     *
     * @param currentYaw   The horizontal rotation before this step.
     * @param currentPitch The vertical pitch before this step.
     */
    public static void recordManualRotation(float currentYaw, float currentPitch) {
        long now = System.currentTimeMillis();
        if (now - lastManualRotationTime > MANUAL_ROTATION_WINDOW_MS) {
            saveCurrentLook(currentYaw, currentPitch);
        }
        lastManualRotationTime = now;
    }

    /**
     * Manually syncs and locks the current look orientation as the persistent Tier 2 reference bookmark,
     * while also updating the Tier 1 dynamic undo buffer and providing spoken confirmation.
     *
     * @param client The Minecraft client instance.
     * @return true if successfully synced.
     */
    public static boolean syncReferenceLook(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }
        bookmarkYaw = client.player.getYRot();
        bookmarkPitch = client.player.getXRot();
        hasBookmark = true;

        saveCurrentLook(bookmarkYaw, bookmarkPitch);
        lastManualRotationTime = System.currentTimeMillis();

        String facingDescription = PlayerPositionUtils.getFullFacingInWords(true);
        MainClass.narrate(I18n.get("minecraft_access.camera_controls.reference_look_set", facingDescription), true);
        return true;
    }

    /**
     * Aligns the player's camera to the Tier 2 persistent reference bookmark.
     * Saves the current look orientation into Tier 1 before aligning, enabling instant return with Backspace.
     *
     * @param client The Minecraft client instance.
     * @return true if alignment succeeded, false if no bookmark is set.
     */
    public static boolean alignToReferenceLook(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }

        if (!hasBookmark) {
            MainClass.narrate(I18n.get("minecraft_access.camera_controls.no_reference_look"), true);
            return false;
        }

        LocalPlayer player = client.player;
        saveCurrentLook(player.getYRot(), player.getXRot());

        player.setYRot(bookmarkYaw);
        player.setXRot(bookmarkPitch);

        String facingDescription = PlayerPositionUtils.getFullFacingInWords(true);
        MainClass.narrate(I18n.get("minecraft_access.camera_controls.aligned_to_reference_look", facingDescription), true);
        return true;
    }

    /**
     * Checks if a previous look orientation has been saved in Tier 1.
     *
     * @return true if a saved look exists.
     */
    public static boolean hasSavedLook() {
        return hasSavedLook;
    }

    /**
     * Gets the saved yaw in Tier 1.
     */
    public static float getSavedYaw() {
        return savedYaw;
    }

    /**
     * Gets the saved pitch in Tier 1.
     */
    public static float getSavedPitch() {
        return savedPitch;
    }

    /**
     * Checks if a Tier 2 persistent bookmark is set.
     */
    public static boolean hasBookmark() {
        return hasBookmark;
    }

    /**
     * Gets the Tier 2 bookmark yaw.
     */
    public static float getBookmarkYaw() {
        return bookmarkYaw;
    }

    /**
     * Gets the Tier 2 bookmark pitch.
     */
    public static float getBookmarkPitch() {
        return bookmarkPitch;
    }

    /**
     * Clears all saved look states (Tier 1 dynamic undo and Tier 2 bookmark).
     */
    public static void clear() {
        savedYaw = 0.0f;
        savedPitch = 0.0f;
        hasSavedLook = false;
        bookmarkYaw = 0.0f;
        bookmarkPitch = 0.0f;
        hasBookmark = false;
        lastManualRotationTime = 0L;
    }

    /**
     * Restores the previous look orientation on the player's camera.
     * Swaps the current look orientation with the saved one, enabling toggle back and forth.
     *
     * @param client The Minecraft client instance.
     * @return true if look was successfully restored, false if no saved look existed.
     */
    public static boolean restorePreviousLook(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }

        if (!hasSavedLook) {
            MainClass.narrate(I18n.get("minecraft_access.camera_controls.no_previous_look"), true);
            return false;
        }

        LocalPlayer player = client.player;
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        // Apply saved rotation
        player.setYRot(savedYaw);
        player.setXRot(savedPitch);

        // Update saved look with the previous orientation before restore (enabling toggle back)
        savedYaw = currentYaw;
        savedPitch = currentPitch;

        // Feedback
        String facingDescription = PlayerPositionUtils.getFullFacingInWords(true);
        MainClass.narrate(I18n.get("minecraft_access.camera_controls.look_restored", facingDescription), true);

        return true;
    }
}
