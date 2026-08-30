package org.mcaccess.minecraftaccess.features.mentor;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.academy.AcademyManager;
import org.mcaccess.minecraftaccess.features.context.PlayerContextEngine;
import org.mcaccess.minecraftaccess.features.context.PlayerContextSnapshot;
import org.mcaccess.minecraftaccess.features.help.HelpNarrator;

@Slf4j
public class ContextualMentor implements BalmClientModule {
    private static ContextualMentor instance;
    private final Map<String, Long> lastDeliveredTime = new HashMap<>();

    public static ContextualMentor getInstance() {
        return instance;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "contextual_mentor");
    }

    @Override
    public void initialize() {
        instance = this;
        // Listen to PlayerContextEngine snapshots
        if (PlayerContextEngine.getInstance() != null) {
            PlayerContextEngine.getInstance().addListener(this::onContextUpdate);
        }
    }

    public void onContextUpdate(PlayerContextSnapshot snapshot) {
        if (snapshot == null) return;
        if (!Config.getInstance().helpSettings.mentorEnabled) return;

        // If an academy mission is active and actively giving instructions, avoid interrupting
        if (AcademyManager.getInstance() != null && AcademyManager.getInstance().isMissionActive()) {
            return;
        }

        long now = System.currentTimeMillis();

        for (MentorRule rule : MentorRuleRegistry.getRules()) {
            String ruleId = rule.id();

            // If non-repeatable and already delivered
            if (!rule.repeatable() && Config.getInstance().helpSettings.deliveredHints.contains(ruleId)) {
                continue;
            }

            // Check cooldown for repeatable rules
            if (rule.repeatable() && lastDeliveredTime.containsKey(ruleId)) {
                long lastTime = lastDeliveredTime.get(ruleId);
                if (now - lastTime < rule.cooldownMillis()) {
                    continue;
                }
            }

            // Evaluate condition
            if (rule.evaluate(snapshot)) {
                deliverHint(rule, now);
                break; // Deliver at most one hint per context cycle
            }
        }
    }

    private void deliverHint(MentorRule rule, long now) {
        lastDeliveredTime.put(rule.id(), now);

        if (!rule.repeatable()) {
            if (!Config.getInstance().helpSettings.deliveredHints.contains(rule.id())) {
                Config.getInstance().helpSettings.deliveredHints.add(rule.id());
                Config.saveConfig();
            }
        }

        String translatedMessage = I18n.get(rule.messageKey());
        HelpNarrator.playHintChime();
        HelpNarrator.narrateHelp(translatedMessage, false);
        log.info("Delivered contextual mentor hint: {}", rule.id());
    }

    public void resetDeliveredHints() {
        lastDeliveredTime.clear();
        Config.getInstance().helpSettings.deliveredHints.clear();
        Config.saveConfig();
    }
}
