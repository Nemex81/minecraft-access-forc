package org.mcaccess.minecraftaccess.features.help;

import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

@Slf4j
public class FirstRunHandler implements BalmClientModule {
    private static FirstRunHandler instance;
    private int ticksInWorld = 0;
    private boolean greetingNarrated = false;

    public static FirstRunHandler getInstance() {
        return instance;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "first_run_handler");
    }

    @Override
    public void initialize() {
        instance = this;
        ClientPlayingTick.AFTER.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client, LocalPlayer player, ClientLevel level) {
        ticksInWorld++;

        // 1.5 seconds (30 ticks) delay post-spawn for first run wizard
        if (ticksInWorld == 30 && !Config.getInstance().helpSettings.firstRunCompleted) {
            if (client.gui.screen() == null) {
                client.gui.setScreen(new FirstRunWizardScreen());
                log.info("Opened FirstRunWizardScreen after 30-tick spawn delay");
            }
        }

        // Brief context announcement for subsequent world loads
        if (ticksInWorld == 50 && Config.getInstance().helpSettings.firstRunCompleted && !greetingNarrated) {
            greetingNarrated = true;
            if (Config.getInstance().helpSettings.mentorEnabled && client.gameMode != null) {
                String gameModeName = client.gameMode.getPlayerMode().getName();
                String translatedGameMode = I18n.get("selectWorld.gameMode." + gameModeName);
                MainClass.narrate(I18n.get("minecraft_access.gui.world_join.mentor_active", translatedGameMode), false);
            }
        }
    }
}
