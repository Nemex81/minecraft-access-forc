package org.mcaccess.minecraftaccess.addon.accessmenu;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.AccessMenuFunction;

public class SaveWaypoint implements AccessMenuFunction {
    @Override
    public void execute() {
        if (MainClass.poiManager != null && MainClass.poiManager.poiWaypoints != null) {
            MainClass.poiManager.poiWaypoints.openSaveWaypointDialog();
        }
    }
}
