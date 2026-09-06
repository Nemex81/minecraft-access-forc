package org.mcaccess.minecraftaccess.features.safety.traversal;

import net.minecraft.client.Minecraft;
import lombok.extern.slf4j.Slf4j;

/**
 * The only production adapter allowed to write Minecraft's effective crouch state.
 */
@Slf4j
public final class MinecraftSneakOverridePort implements SneakOverridePort {

    @Override
    public void applyEffectiveCrouch(boolean crouching) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                return;
            }
            if (client.options != null && client.options.keyShift != null) {
                client.options.keyShift.setDown(crouching);
            }
            if (client.player != null) {
                client.player.setShiftKeyDown(crouching);
            }
        } catch (RuntimeException exception) {
            log.debug("Unable to apply synthetic crouch state", exception);
        }
    }
}
