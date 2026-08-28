package org.mcaccess.minecraftaccess.addon.accessmenu;

import net.minecraft.client.Minecraft;

import org.mcaccess.minecraftaccess.api.AccessMenuFunction;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.gui.ManageWaypointsScreen;

public class ManageWaypoints implements AccessMenuFunction {
    @Override
    public void execute() {
        Minecraft.getInstance().gui.setScreen(new ManageWaypointsScreen());
    }
}
