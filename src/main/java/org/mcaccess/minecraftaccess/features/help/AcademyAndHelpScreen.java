package org.mcaccess.minecraftaccess.features.help;

import java.util.List;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.academy.AcademyManager;
import org.mcaccess.minecraftaccess.features.academy.Mission;
import org.mcaccess.minecraftaccess.features.academy.MissionRegistry;

public class AcademyAndHelpScreen extends Screen {

    public AcademyAndHelpScreen() {
        super(Component.translatable("minecraft_access.gui.academy_hub.title"));
    }

    @Override
    protected void init() {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(this.title, this.font);

        GridLayout grid = new GridLayout().spacing(5);
        GridLayout.RowHelper rowHelper = grid.createRowHelper(1);

        // 1. Toggle Mentor
        boolean mentorActive = Config.getInstance().helpSettings.mentorEnabled;
        String mentorStatusText = mentorActive
                ? I18n.get("minecraft_access.gui.academy_hub.mentor_status_on")
                : I18n.get("minecraft_access.gui.academy_hub.mentor_status_off");

        rowHelper.addChild(Button.builder(Component.literal(mentorStatusText), _ -> {
            Config.getInstance().helpSettings.mentorEnabled = !Config.getInstance().helpSettings.mentorEnabled;
            Config.saveConfig();
            this.rebuildWidgets();
            String newStatus = Config.getInstance().helpSettings.mentorEnabled
                    ? I18n.get("minecraft_access.gui.academy_hub.mentor_status_on")
                    : I18n.get("minecraft_access.gui.academy_hub.mentor_status_off");
            MainClass.narrate(newStatus, true);
        }).width(Button.BIG_WIDTH).build());

        // 2. Toggle Auto-Advance
        boolean autoAdvance = Config.getInstance().helpSettings.autoAdvanceMissions;
        String autoAdvanceText = autoAdvance
                ? I18n.get("minecraft_access.gui.academy_hub.auto_advance_on")
                : I18n.get("minecraft_access.gui.academy_hub.auto_advance_off");

        rowHelper.addChild(Button.builder(Component.literal(autoAdvanceText), _ -> {
            Config.getInstance().helpSettings.autoAdvanceMissions = !Config.getInstance().helpSettings.autoAdvanceMissions;
            Config.saveConfig();
            this.rebuildWidgets();
            String newStatus = Config.getInstance().helpSettings.autoAdvanceMissions
                    ? I18n.get("minecraft_access.gui.academy_hub.auto_advance_on")
                    : I18n.get("minecraft_access.gui.academy_hub.auto_advance_off");
            MainClass.narrate(newStatus, true);
        }).width(Button.BIG_WIDTH).build());

        // 3. Stop Active Mission (if active)
        if (AcademyManager.getInstance() != null && AcademyManager.getInstance().isMissionActive()) {
            rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.academy_hub.stop_mission"), _ -> {
                AcademyManager.getInstance().stopMission();
                this.rebuildWidgets();
            }).width(Button.BIG_WIDTH).build());
        }

        // 4. Missions List with GameMode Adaptive Compatibility
        GameType currentMode = (this.minecraft != null && this.minecraft.gameMode != null)
                ? this.minecraft.gameMode.getPlayerMode()
                : null;

        List<Mission> missions = MissionRegistry.getMissions();
        for (Mission m : missions) {
            boolean isCompleted = Config.getInstance().helpSettings.completedMissions.contains(m.id());
            String statusSuffix = isCompleted
                    ? I18n.get("minecraft_access.gui.academy_hub.mission_completed_tag")
                    : I18n.get("minecraft_access.gui.academy_hub.mission_not_started_tag");

            boolean isCompatible = true;
            String incompatibilitySuffix = "";

            if (currentMode != null) {
                if (m.id().startsWith("CREATIVE_") && currentMode != GameType.CREATIVE) {
                    isCompatible = false;
                    incompatibilitySuffix = " - [" + I18n.get("minecraft_access.gui.academy_hub.requires_creative_tag") + "]";
                } else if (m.id().startsWith("SURVIVAL_") && currentMode == GameType.CREATIVE) {
                    isCompatible = false;
                    incompatibilitySuffix = " - [" + I18n.get("minecraft_access.gui.academy_hub.requires_survival_tag") + "]";
                }
            }

            String btnText = I18n.get(m.titleKey()) + " - " + statusSuffix + incompatibilitySuffix;

            Button btn = Button.builder(Component.literal(btnText), _ -> {
                this.onClose();
                if (AcademyManager.getInstance() != null) {
                    AcademyManager.getInstance().startMission(m);
                }
            }).width(Button.BIG_WIDTH).build();

            btn.active = isCompatible;
            rowHelper.addChild(btn);
        }

        // 5. Quick Keys Help Screen
        rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.quick_help.title"), _ -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(new QuickKeysHelpScreen());
            }
        }).width(Button.BIG_WIDTH).build());

        // 6. Restart Wizard
        rowHelper.addChild(Button.builder(Component.translatable("minecraft_access.gui.academy_hub.restart_wizard"), _ -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(new FirstRunWizardScreen());
            }
        }).width(Button.BIG_WIDTH).build());

        ScrollableLayout scroll = layout.addToContents(new ScrollableLayout(this.minecraft, grid, layout.getContentHeight()));
        scroll.setMaxHeight(layout.getContentHeight());

        layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, _ -> this.onClose())
                .width(Button.DEFAULT_WIDTH)
                .build());

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();
    }
}
