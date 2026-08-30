package org.mcaccess.minecraftaccess.features.mentor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MentorRuleRegistry {
    private static final List<MentorRule> RULES = new ArrayList<>();

    static {
        // 1. Sunset warning (Survival only)
        RULES.add(new MentorRule(
                "HINT_SUNSET_WARN",
                false,
                0,
                s -> s.isSurvivalOrAdventure() && s.timeOfDay() >= 11500 && s.timeOfDay() <= 12500,
                "minecraft_access.mentor.sunset_warning"
        ));

        // 2. Deep night warning (Survival only)
        RULES.add(new MentorRule(
                "HINT_DEEP_NIGHT",
                false,
                0,
                s -> s.isSurvivalOrAdventure() && s.timeOfDay() >= 14000 && s.timeOfDay() <= 18000 && s.blockLight() < 7,
                "minecraft_access.mentor.deep_night"
        ));

        // 3. Sunrise encouragement
        RULES.add(new MentorRule(
                "HINT_SUNRISE",
                false,
                0,
                s -> s.isSurvivalOrAdventure() && s.timeOfDay() >= 0 && s.timeOfDay() <= 1000,
                "minecraft_access.mentor.sunrise"
        ));

        // 4. Idle / Disorientation assistance (Repeatable with 3-minute cooldown)
        RULES.add(new MentorRule(
                "HINT_IDLE_STUCK",
                true,
                180_000L,
                s -> s.idleTicks() >= 500, // 25 seconds of total inactivity
                "minecraft_access.mentor.idle_help"
        ));

        // 5. Stuck against a wall (Repeatable with 2-minute cooldown)
        RULES.add(new MentorRule(
                "HINT_WALL_STUCK",
                true,
                120_000L,
                s -> s.isStuckAgainstWall(),
                "minecraft_access.mentor.wall_stuck"
        ));

        // 6. First wood collected -> advise crafting planks
        RULES.add(new MentorRule(
                "HINT_FIRST_WOOD",
                false,
                0,
                s -> s.isSurvivalOrAdventure() && s.woodLogsCount() > 0 && s.planksCount() == 0,
                "minecraft_access.mentor.first_wood"
        ));

        // 7. Low hunger warning and eating instruction
        RULES.add(new MentorRule(
                "HINT_LOW_HUNGER",
                true,
                300_000L,
                s -> s.isSurvivalOrAdventure() && s.foodLevel() <= 6 && s.foodCount() > 0,
                "minecraft_access.mentor.low_hunger"
        ));

        // 8. Total darkness and has torches
        RULES.add(new MentorRule(
                "HINT_TOTAL_DARKNESS",
                true,
                180_000L,
                s -> s.isSurvivalOrAdventure() && s.blockLight() == 0 && s.torchesCount() > 0,
                "minecraft_access.mentor.total_darkness"
        ));

        // 9. Creative mode welcome
        RULES.add(new MentorRule(
                "HINT_CREATIVE_WELCOME",
                false,
                0,
                s -> s.isCreative() && s.idleTicks() >= 60,
                "minecraft_access.mentor.creative_welcome"
        ));

        // 10. Creative mode flight tips
        RULES.add(new MentorRule(
                "HINT_CREATIVE_FLIGHT",
                false,
                0,
                s -> s.isCreative() && s.isFlying(),
                "minecraft_access.mentor.creative_flight"
        ));
    }

    private MentorRuleRegistry() {
    }

    public static List<MentorRule> getRules() {
        return Collections.unmodifiableList(RULES);
    }
}
