package org.mcaccess.minecraftaccess.features.autowalk;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Facciata Legacy per AutoWalk (ASTRALIS Fase 5C).
 * Preserva la retrocompatibilita 1:1 per qualsiasi chiamata storica ad AutoWalkController,
 * delegando interamente le operazioni al nuovo MovementCoordinator e ai suoi sottogestori.
 */
@Slf4j
public class AutoWalkController {

    public enum State {
        IDLE,
        WALKING,
        JUMPING,
        SWIMMING,
        ARRIVED,
        CANCELLED
    }

    @Getter
    private final MovementCoordinator movementCoordinator;

    public AutoWalkController() {
        this(new MovementCoordinator());
    }

    public AutoWalkController(MovementCoordinator movementCoordinator) {
        this.movementCoordinator = movementCoordinator;
    }

    public State getState() {
        return switch (movementCoordinator.getMotor().getState()) {
            case IDLE -> State.IDLE;
            case WALKING -> State.WALKING;
            case JUMPING -> State.JUMPING;
            case SWIMMING -> State.SWIMMING;
            case ARRIVED -> State.ARRIVED;
            case CANCELLED -> State.CANCELLED;
        };
    }

    public @Nullable Object getTargetObject() {
        return movementCoordinator.getNavigator().getTargetObject();
    }

    public boolean isActive() {
        return movementCoordinator.isActive();
    }

    public void start(Object target) {
        movementCoordinator.start(target);
    }

    public void cancel(boolean narrate, @Nullable String reasonKey) {
        movementCoordinator.cancel(narrate, reasonKey);
    }

    public void tick(Minecraft client, LocalPlayer player, Level level) {
        movementCoordinator.tick(client, player, level);
    }

    public void toggleSprint() {
        movementCoordinator.toggleSprint();
    }
}
