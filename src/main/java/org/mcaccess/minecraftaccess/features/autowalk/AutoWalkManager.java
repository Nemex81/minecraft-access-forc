package org.mcaccess.minecraftaccess.features.autowalk;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

public class AutoWalkManager implements BalmClientModule {
    @Getter
    private final MovementCoordinator movementCoordinator = new MovementCoordinator();

    @Getter
    private final AutoWalkController controller = new AutoWalkController(movementCoordinator);

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "autowalk_manager");
    }

    @Override
    public void initialize() {
        MovementCoordinator.registerLifecycleHooks(movementCoordinator.getMotor(), movementCoordinator.getNavigator());

        ClientPlayingTick.AFTER.register((client, player, level) -> {
            if (player instanceof LocalPlayer localPlayer) {
                movementCoordinator.tick(client, localPlayer, level);
            }
        });

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "other.auto_walk"))
                .withDefault(InputBinding.key(InputConstants.KEY_W, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    toggleAutoWalk();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "other.auto_walk_toggle_sprint"))
                .withDefault(InputBinding.key(InputConstants.KEY_W, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    toggleSprint();
                    return true;
                })
                .build();
    }

    public void toggleSprint() {
        movementCoordinator.toggleSprint();
    }

    public void toggleAutoWalk() {
        if (movementCoordinator.isActive()) {
            movementCoordinator.cancel(true, null);
        } else {
            if (!Config.getInstance().autoWalk.enabled) {
                MainClass.narrate(I18n.get("minecraft_access.autowalk.disabled"), true);
                return;
            }

            if (MainClass.poiManager == null || MainClass.poiManager.objectTracker == null) {
                MainClass.narrate(I18n.get("minecraft_access.point_of_interest.not_selected"), true);
                return;
            }

            Object target = MainClass.poiManager.objectTracker.getCurrentObject();
            if (target == null) {
                MainClass.narrate(I18n.get("minecraft_access.point_of_interest.not_selected"), true);
                return;
            }

            movementCoordinator.start(target);
        }
    }
}
