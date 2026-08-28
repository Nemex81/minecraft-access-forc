package org.mcaccess.minecraftaccess.addon.accessmenu;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.AccessMenuFunction;

public class AutoWalk implements AccessMenuFunction {
    @Override
    public void execute() {
        if (MainClass.autoWalkManager != null) {
            MainClass.autoWalkManager.toggleAutoWalk();
        }
    }
}
