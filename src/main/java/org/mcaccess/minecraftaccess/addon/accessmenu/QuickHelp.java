package org.mcaccess.minecraftaccess.addon.accessmenu;

import net.minecraft.client.Minecraft;

import org.mcaccess.minecraftaccess.api.AccessMenuFunction;
import org.mcaccess.minecraftaccess.features.help.QuickKeysHelpScreen;

public class QuickHelp implements AccessMenuFunction {
    @Override
    public void execute() {
        Minecraft.getInstance().gui.setScreen(new QuickKeysHelpScreen());
    }
}
