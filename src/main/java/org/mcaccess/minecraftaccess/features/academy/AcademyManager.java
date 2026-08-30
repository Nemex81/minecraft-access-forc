package org.mcaccess.minecraftaccess.features.academy;

import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.context.PlayerContextEngine;
import org.mcaccess.minecraftaccess.features.context.PlayerContextSnapshot;
import org.mcaccess.minecraftaccess.features.help.HelpNarrator;

@Slf4j
public class AcademyManager implements BalmClientModule {
    private static AcademyManager instance;

    @Getter
    private Mission activeMission = null;
    @Getter
    private int currentStepIndex = 0;
    private long lastStepCompletedTime = 0;

    private Mission pendingAutoAdvanceMission = null;
    private long pendingAutoAdvanceTime = 0;

    public static AcademyManager getInstance() {
        return instance;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "academy_manager");
    }

    @Override
    public void initialize() {
        instance = this;
        if (PlayerContextEngine.getInstance() != null) {
            PlayerContextEngine.getInstance().addListener(this::onContextUpdate);
        }
    }

    public boolean isMissionActive() {
        return activeMission != null || pendingAutoAdvanceMission != null;
    }

    public void startMission(Mission mission) {
        // GameMode Guard Rail Validation
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null) {
            GameType currentGameType = client.gameMode.getPlayerMode();
            if (mission.id().startsWith("CREATIVE_") && currentGameType != GameType.CREATIVE) {
                HelpNarrator.narrateHelp(I18n.get("minecraft_access.academy.error_requires_creative"), true);
                log.warn("Blocked starting Creative mission {} while in {}", mission.id(), currentGameType);
                return;
            }
            if (mission.id().startsWith("SURVIVAL_") && currentGameType == GameType.CREATIVE) {
                HelpNarrator.narrateHelp(I18n.get("minecraft_access.academy.error_requires_survival"), true);
                log.warn("Blocked starting Survival mission {} while in Creative mode", mission.id());
                return;
            }
        }

        this.pendingAutoAdvanceMission = null;
        this.activeMission = mission;
        this.currentStepIndex = 0;
        this.lastStepCompletedTime = System.currentTimeMillis();

        String missionTitle = I18n.get(mission.titleKey());
        MissionStep firstStep = mission.steps().get(0);
        String instruction = I18n.get(firstStep.instructionKey());

        HelpNarrator.narrateHelp(I18n.get("minecraft_access.academy.starting_mission", missionTitle, instruction), true);
        log.info("Started Academy Mission: {}", mission.id());
    }

    public void stopMission() {
        pendingAutoAdvanceMission = null;
        if (activeMission != null) {
            String title = I18n.get(activeMission.titleKey());
            activeMission = null;
            currentStepIndex = 0;
            HelpNarrator.narrateHelp(I18n.get("minecraft_access.academy.mission_stopped", title), true);
        }
    }

    public Mission findNextMission(PlayerContextSnapshot snapshot) {
        List<Mission> allMissions = MissionRegistry.getMissions();
        for (Mission m : allMissions) {
            if (Config.getInstance().helpSettings.completedMissions.contains(m.id())) {
                continue;
            }
            if (snapshot != null && snapshot.isCreative()) {
                if (m.id().startsWith("CREATIVE_")) {
                    return m;
                }
            } else {
                if (m.id().startsWith("SURVIVAL_")) {
                    return m;
                }
            }
        }
        return null;
    }

    private void onContextUpdate(PlayerContextSnapshot snapshot) {
        if (snapshot == null) return;

        // Check if there is an automatic mission advancement pending
        if (pendingAutoAdvanceMission != null) {
            if (System.currentTimeMillis() >= pendingAutoAdvanceTime) {
                Mission toStart = pendingAutoAdvanceMission;
                pendingAutoAdvanceMission = null;
                startMission(toStart);
            }
            return;
        }

        if (activeMission == null) return;

        // Prevent instant multi-step skipping in the same tick
        if (System.currentTimeMillis() - lastStepCompletedTime < 1500L) {
            return;
        }

        if (currentStepIndex >= activeMission.steps().size()) {
            return;
        }

        MissionStep currentStep = activeMission.steps().get(currentStepIndex);
        if (currentStep.isCompleted(snapshot)) {
            lastStepCompletedTime = System.currentTimeMillis();
            HelpNarrator.playSuccessChime();

            String successMsg = I18n.get(currentStep.successKey());

            if (currentStepIndex + 1 < activeMission.steps().size()) {
                currentStepIndex++;
                MissionStep nextStep = activeMission.steps().get(currentStepIndex);
                String nextInstruction = I18n.get(nextStep.instructionKey());
                HelpNarrator.narrateHelp(successMsg + " " + nextInstruction, true);
            } else {
                // Entire mission completed!
                String completedId = activeMission.id();
                String missionTitle = I18n.get(activeMission.titleKey());

                if (!Config.getInstance().helpSettings.completedMissions.contains(completedId)) {
                    Config.getInstance().helpSettings.completedMissions.add(completedId);
                    Config.saveConfig();
                }

                activeMission = null;
                currentStepIndex = 0;
                String completionText = successMsg + " " + I18n.get("minecraft_access.academy.mission_completed", missionTitle);
                HelpNarrator.narrateHelp(completionText, true);
                log.info("Completed Academy Mission: {}", missionTitle);

                // Auto-advance if enabled
                if (Config.getInstance().helpSettings.autoAdvanceMissions) {
                    Mission nextMission = findNextMission(snapshot);
                    if (nextMission != null) {
                        pendingAutoAdvanceMission = nextMission;
                        pendingAutoAdvanceTime = System.currentTimeMillis() + 4500L;
                        log.info("Scheduled auto-advance to next mission: {} in 4.5s", nextMission.id());
                    } else {
                        // All completed for this game mode
                        HelpNarrator.narrateHelp(I18n.get("minecraft_access.academy.all_completed"), true);
                    }
                }
            }
        }
    }
}
