package org.mcaccess.minecraftaccess.features.help;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.academy.AcademyManager;
import org.mcaccess.minecraftaccess.features.academy.MissionRegistry;

public class FirstRunWizardScreen extends Screen {
    private boolean choosingPreset = false;

    public FirstRunWizardScreen() {
        super(Component.translatable("minecraft_access.gui.first_run.title"));
    }

    @Override
    protected void init() {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(this.title, this.font);

        GridLayout grid = new GridLayout().spacing(8);
        GridLayout.RowHelper rowHelper = grid.createRowHelper(1);

        if (!choosingPreset) {
            // Button 1: Start Academy
            rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.first_run.start_academy"), _ -> {
                choosingPreset = true;
                this.rebuildWidgets();
            }).width(Button.BIG_WIDTH).build());

            // Button 2: Enable Mentor only
            rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.first_run.enable_mentor_only"), _ -> {
                Config.getInstance().helpSettings.firstRunCompleted = true;
                Config.getInstance().helpSettings.mentorEnabled = true;
                Config.saveConfig();
                this.onClose();
                MainClass.narrate(I18n.get("minecraft_access.gui.first_run.mentor_enabled_feedback"), true);
            }).width(Button.BIG_WIDTH).build());

            // Button 3: Disable Help
            rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.first_run.disable_help"), _ -> {
                Config.getInstance().helpSettings.firstRunCompleted = true;
                Config.getInstance().helpSettings.mentorEnabled = false;
                Config.saveConfig();
                this.onClose();
                MainClass.narrate(I18n.get("minecraft_access.gui.first_run.help_disabled_feedback"), true);
            }).width(Button.BIG_WIDTH).build());
        } else {
            // Preset Desktop
            rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.first_run.preset_desktop"), _ -> {
                Config.getInstance().helpSettings.hardwarePreset = Config.HardwarePreset.DESKTOP_NUMPAD;
                finishWizardAndStartAcademy();
            }).width(Button.BIG_WIDTH).build());

            // Preset Laptop
            rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.first_run.preset_laptop"), _ -> {
                Config.getInstance().helpSettings.hardwarePreset = Config.HardwarePreset.LAPTOP_KEYS;
                finishWizardAndStartAcademy();
            }).width(Button.BIG_WIDTH).build());
        }

        layout.addToContents(grid);
        layout.addToFooter(Button.builder(CommonComponents.GUI_CANCEL, _ -> {
            Config.getInstance().helpSettings.firstRunCompleted = true;
            Config.saveConfig();
            this.onClose();
        }).width(Button.DEFAULT_WIDTH).build());

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();

        if (!choosingPreset) {
            MainClass.narrate(I18n.get("minecraft_access.gui.first_run.welcome_narration"), true);
        } else {
            MainClass.narrate(I18n.get("minecraft_access.gui.first_run.choose_preset_narration"), true);
        }
    }

    private void finishWizardAndStartAcademy() {
        Config.getInstance().helpSettings.firstRunCompleted = true;
        Config.getInstance().helpSettings.mentorEnabled = true;
        Config.saveConfig();
        this.onClose();

        if (AcademyManager.getInstance() != null && !MissionRegistry.getMissions().isEmpty()) {
            AcademyManager.getInstance().startMission(MissionRegistry.getMissions().get(0));
        }
    }
}
