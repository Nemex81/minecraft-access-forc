package org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointManager;

public class SaveWaypointScreen extends Screen {
    private EditBox nameField;
    private final BlockPos pos;
    private final Identifier dimension;
    private final String defaultName;

    public SaveWaypointScreen(BlockPos pos, Identifier dimension, String defaultName) {
        super(Component.translatable("minecraft_access.gui.save_waypoint.title"));
        this.pos = pos;
        this.dimension = dimension;
        this.defaultName = defaultName;
    }

    @Override
    protected void init() {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(this.title, this.font);

        LinearLayout contents = layout.addToContents(LinearLayout.vertical().spacing(8));

        this.nameField = new EditBox(this.font, 200, 20, Component.translatable("minecraft_access.gui.save_waypoint.name_field"));
        this.nameField.setValue(defaultName);
        this.nameField.setHighlightPos(0);
        contents.addChild(this.nameField);

        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, _ -> onClose()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, _ -> saveAndClose()).build());

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();

        this.setInitialFocus(this.nameField);

        MainClass.narrate(I18n.get("minecraft_access.gui.save_waypoint.narrate_open"), true);
    }

    private void saveAndClose() {
        String name = this.nameField.getValue().trim();
        if (name.isEmpty()) {
            name = defaultName;
        }
        WaypointManager.getInstance().addCustomWaypoint(name, pos, dimension);
        this.onClose();
        MainClass.narrate(I18n.get("minecraft_access.gui.save_waypoint.saved", name), true);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
