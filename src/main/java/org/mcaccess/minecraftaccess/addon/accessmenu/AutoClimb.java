package org.mcaccess.minecraftaccess.addon.accessmenu;

import org.mcaccess.minecraftaccess.api.AccessMenuFunction;
import org.mcaccess.minecraftaccess.features.autowalk.ClimbAssistantController;

public class AutoClimb implements AccessMenuFunction {
    @Override
    public void execute() {
        ClimbAssistantController.triggerFromKey();
    }
}
