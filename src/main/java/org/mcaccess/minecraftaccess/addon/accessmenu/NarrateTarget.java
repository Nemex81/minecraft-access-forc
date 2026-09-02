package org.mcaccess.minecraftaccess.addon.accessmenu;

import org.mcaccess.minecraftaccess.api.AccessMenuFunction;
import org.mcaccess.minecraftaccess.features.crosshair.CrosshairFeedbackManager;

public class NarrateTarget implements AccessMenuFunction {
    @Override
    public void execute() {
        CrosshairFeedbackManager.onManualCrosshairRequested();
    }
}

