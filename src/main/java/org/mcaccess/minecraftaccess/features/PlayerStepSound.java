package org.mcaccess.minecraftaccess.features;

import com.mojang.blaze3d.platform.InputConstants;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.ModifierUtils;

/**
 * Adds hotkeys to increase/decrease the player's footstep sound volume on-the-fly.<br>
 * - Alt + Page Up: Increase footstep volume by 10%<br>
 * - Alt + Page Down: Decrease footstep volume by 10%<br>
 */
public class PlayerStepSound implements BalmClientModule {
    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "player_step_sound");
    }

    @Override
    public void initialize() {
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "player_status.increase_step_volume"))
                .withDefault(InputBinding.key(InputConstants.KEY_PAGEUP, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.PLAYER_STATUS)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasAltOnly()) return false;
                    adjustStepVolume(10);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "player_status.decrease_step_volume"))
                .withDefault(InputBinding.key(InputConstants.KEY_PAGEDOWN, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.PLAYER_STATUS)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasAltOnly()) return false;
                    adjustStepVolume(-10);
                    return true;
                })
                .build();
    }

    private static void adjustStepVolume(int delta) {
        int current = Config.getInstance().features.playerStepSoundVolume;
        int next = Math.clamp(current + delta, 0, 300);
        Config.getInstance().features.playerStepSoundVolume = next;
        Config.saveConfig();
        MainClass.narrate(I18n.get("minecraft_access.player_step_volume.narrate", next), true);
    }
}
