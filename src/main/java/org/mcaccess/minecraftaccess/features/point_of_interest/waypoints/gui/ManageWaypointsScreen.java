package org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.gui;

import java.util.List;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointManager;

public class ManageWaypointsScreen extends Screen {
    public ManageWaypointsScreen() {
        super(Component.translatable("minecraft_access.gui.manage_waypoints.title"));
    }

    @Override
    protected void init() {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(this.title, this.font);

        GridLayout grid = new GridLayout().spacing(5);
        GridLayout.RowHelper rowHelper = grid.createRowHelper(2);

        List<Waypoint> waypoints = WaypointManager.getInstance().getWaypoints();
        if (waypoints.isEmpty()) {
            Button emptyButton = Button.builder(Component.translatable("minecraft_access.gui.manage_waypoints.no_waypoints"), _ -> {})
                    .width(Button.BIG_WIDTH)
                    .build();
            emptyButton.active = false;
            rowHelper.addChild(emptyButton, 2);
        } else {
            for (Waypoint waypoint : waypoints) {
                // Button to select and set as tracked
                Button selectBtn = Button.builder(Component.literal(waypoint.name()), _ -> {
                    MainClass.poiManager.poiWaypoints.selectWaypoint(waypoint);
                    this.onClose();
                    MainClass.narrate(I18n.get("minecraft_access.gui.manage_waypoints.selected", waypoint.name()), true);
                }).width(140).build();
                rowHelper.addChild(selectBtn);

                // Button to delete
                Button deleteBtn = Button.builder(Component.translatable("selectWorld.delete"), _ -> {
                    WaypointManager.getInstance().removeWaypoint(waypoint.id());
                    MainClass.narrate(I18n.get("minecraft_access.gui.manage_waypoints.deleted", waypoint.name()), true);
                    this.rebuildWidgets();
                }).width(80).build();
                rowHelper.addChild(deleteBtn);
            }
        }

        ScrollableLayout scroll = layout.addToContents(new ScrollableLayout(this.minecraft, grid, layout.getContentHeight()));
        scroll.setMaxHeight(layout.getContentHeight());

        layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, _ -> this.onClose())
                .width(Button.BIG_WIDTH)
                .build());

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();
    }
}
