package org.mcaccess.minecraftaccess.addon.accessmenu;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.AccessMenuFunction;

public class SurvivalResources implements AccessMenuFunction {
    @Override
    public void execute() {
        if (MainClass.survivalResourceTracker != null) {
            MainClass.survivalResourceTracker.scanAndNarrate(true);
        }
    }
}
