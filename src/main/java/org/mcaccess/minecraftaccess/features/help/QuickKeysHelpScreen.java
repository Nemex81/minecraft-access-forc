package org.mcaccess.minecraftaccess.features.help;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.mcaccess.minecraftaccess.MainClass;

public class QuickKeysHelpScreen extends Screen {

    public QuickKeysHelpScreen() {
        super(Component.translatable("minecraft_access.gui.quick_help.title"));
    }

    @Override
    protected void init() {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(this.title, this.font);

        GridLayout grid = new GridLayout().spacing(6);
        GridLayout.RowHelper rowHelper = grid.createRowHelper(1);

        // Category 1: Movement & Jump
        rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.quick_help.cat_movement"), _ -> {
            MainClass.narrate(I18n.get("minecraft_access.gui.quick_help.desc_movement"), true);
        }).width(Button.BIG_WIDTH).build());

        // Category 2: Looking & Alignment
        rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.quick_help.cat_look"), _ -> {
            MainClass.narrate(I18n.get("minecraft_access.gui.quick_help.desc_look"), true);
        }).width(Button.BIG_WIDTH).build());

        // Category 3: POI Radar & Target
        rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.quick_help.cat_poi"), _ -> {
            MainClass.narrate(I18n.get("minecraft_access.gui.quick_help.desc_poi"), true);
        }).width(Button.BIG_WIDTH).build());

        // Category 4: Interaction & Actions
        rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.quick_help.cat_action"), _ -> {
            MainClass.narrate(I18n.get("minecraft_access.gui.quick_help.desc_action"), true);
        }).width(Button.BIG_WIDTH).build());

        // Category 5: Access Menu & Status
        rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.quick_help.cat_menu"), _ -> {
            MainClass.narrate(I18n.get("minecraft_access.gui.quick_help.desc_menu"), true);
        }).width(Button.BIG_WIDTH).build());

        layout.addToContents(grid);
        layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, _ -> this.onClose())
                .width(Button.DEFAULT_WIDTH)
                .build());

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();

        MainClass.narrate(I18n.get("minecraft_access.gui.quick_help.intro_narration"), true);
    }
}
