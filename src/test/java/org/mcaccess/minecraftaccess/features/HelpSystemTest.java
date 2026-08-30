package org.mcaccess.minecraftaccess.features;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.features.academy.Mission;
import org.mcaccess.minecraftaccess.features.academy.MissionRegistry;
import org.mcaccess.minecraftaccess.features.academy.MissionStep;
import org.mcaccess.minecraftaccess.features.context.PlayerContextSnapshot;
import org.mcaccess.minecraftaccess.features.help.HelpNarrator;
import org.mcaccess.minecraftaccess.features.mentor.MentorRule;
import org.mcaccess.minecraftaccess.features.mentor.MentorRuleRegistry;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Comprehensive Diagnostics & Validation for Help, Mentor and Academy Systems")
class HelpSystemTest {

    private PlayerContextSnapshot createSnapshot(
            boolean isMoving,
            boolean isStuck,
            boolean isFlying,
            int idleTicks,
            HitResult hit,
            double hitDist,
            int logs,
            int planks,
            int torches,
            int food,
            int craftingTables,
            int blockLight,
            int foodLevel,
            long timeOfDay,
            GameType gameMode
    ) {
        return new PlayerContextSnapshot(
                Vec3.ZERO,
                BlockPos.ZERO,
                "plains",
                blockLight,
                isStuck,
                isMoving,
                false,
                false,
                isFlying,
                false,
                hit,
                hitDist,
                logs,
                planks,
                0,
                torches,
                food,
                craftingTables,
                20.0f,
                20.0f,
                foodLevel,
                timeOfDay,
                0,
                gameMode,
                idleTicks
        );
    }

    @Nested
    @DisplayName("1. HelpNarrator Dynamic Shield & Voice Priority Calculations")
    class HelpNarratorShieldTests {

        @Test
        @DisplayName("Verify shield calculation with null and empty inputs")
        void testNullAndEmptyShield() {
            assertEquals(500L, HelpNarrator.calculateShieldDuration(null));
            assertEquals(500L, HelpNarrator.calculateShieldDuration(""));
            assertEquals(500L, HelpNarrator.calculateShieldDuration("   "));
        }

        @Test
        @DisplayName("Verify dynamic shield formula: (words * 280ms) + 600ms buffer")
        void testShieldDurationFormula() {
            // 1 word -> (1 * 280) + 600 = 880 ms
            assertEquals(880L, HelpNarrator.calculateShieldDuration("Attenzione"));

            // 5 words -> (5 * 280) + 600 = 2000 ms
            assertEquals(2000L, HelpNarrator.calculateShieldDuration("Il sole sta tramontando presto"));

            // 10 words -> (10 * 280) + 600 = 3400 ms
            assertEquals(3400L, HelpNarrator.calculateShieldDuration("uno due tre quattro cinque sei sette otto nove dieci"));

            // 20 words -> (20 * 280) + 600 = 6200 ms
            String twentyWords = "uno due tre quattro cinque sei sette otto nove dieci undici dodici tredici quattordici quindici sedici diciassette diciotto diciannove venti";
            assertEquals(6200L, HelpNarrator.calculateShieldDuration(twentyWords));
        }
    }

    @Nested
    @DisplayName("2. PlayerContextSnapshot & GameMode Invariance")
    class ContextSnapshotTests {

        @Test
        @DisplayName("Verify GameMode predicates across all game modes")
        void testGameModePredicates() {
            PlayerContextSnapshot survival = createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 1000, GameType.SURVIVAL);
            assertTrue(survival.isSurvivalOrAdventure());
            assertFalse(survival.isCreative());
            assertFalse(survival.isSpectator());

            PlayerContextSnapshot adventure = createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 1000, GameType.ADVENTURE);
            assertTrue(adventure.isSurvivalOrAdventure());
            assertFalse(adventure.isCreative());
            assertFalse(adventure.isSpectator());

            PlayerContextSnapshot creative = createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 1000, GameType.CREATIVE);
            assertFalse(creative.isSurvivalOrAdventure());
            assertTrue(creative.isCreative());
            assertFalse(creative.isSpectator());

            PlayerContextSnapshot spectator = createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 1000, GameType.SPECTATOR);
            assertFalse(spectator.isSurvivalOrAdventure());
            assertFalse(spectator.isCreative());
            assertTrue(spectator.isSpectator());
        }
    }

    @Nested
    @DisplayName("3. Contextual Mentor Rules Simulation & Edge Cases")
    class MentorRulesSimulationTests {

        @Test
        @DisplayName("Rule HINT_SUNSET_WARN: Boundary conditions between 11500 and 12500 ticks in Survival")
        void testSunsetWarningRule() {
            MentorRule rule = MentorRuleRegistry.getRules().stream()
                    .filter(r -> r.id().equals("HINT_SUNSET_WARN"))
                    .findFirst().orElseThrow();

            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 11499, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 11500, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 12000, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 12500, GameType.SURVIVAL)));
            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 12501, GameType.SURVIVAL)));

            // Ignored in Creative
            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 12000, GameType.CREATIVE)));
        }

        @Test
        @DisplayName("Rule HINT_DEEP_NIGHT: Between 14000 and 18000 ticks with low block light (< 7)")
        void testDeepNightRule() {
            MentorRule rule = MentorRuleRegistry.getRules().stream()
                    .filter(r -> r.id().equals("HINT_DEEP_NIGHT"))
                    .findFirst().orElseThrow();

            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 0, 20, 13999, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 6, 20, 14000, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 0, 20, 16000, GameType.SURVIVAL)));
            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 8, 20, 16000, GameType.SURVIVAL))); // Torches lit (light >= 7)
            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 0, 20, 18001, GameType.SURVIVAL)));
        }

        @Test
        @DisplayName("Rule HINT_IDLE_STUCK: Inactivity >= 500 ticks (25 seconds)")
        void testIdleStuckRule() {
            MentorRule rule = MentorRuleRegistry.getRules().stream()
                    .filter(r -> r.id().equals("HINT_IDLE_STUCK"))
                    .findFirst().orElseThrow();

            assertFalse(rule.evaluate(createSnapshot(false, false, false, 499, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 500, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 1200, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.CREATIVE)));
        }

        @Test
        @DisplayName("Rule HINT_WALL_STUCK: Wall collision state")
        void testWallStuckRule() {
            MentorRule rule = MentorRuleRegistry.getRules().stream()
                    .filter(r -> r.id().equals("HINT_WALL_STUCK"))
                    .findFirst().orElseThrow();

            assertFalse(rule.evaluate(createSnapshot(true, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(true, true, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
        }

        @Test
        @DisplayName("Rule HINT_FIRST_WOOD: Triggered when logs >= 1 and planks == 0 in Survival")
        void testFirstWoodRule() {
            MentorRule rule = MentorRuleRegistry.getRules().stream()
                    .filter(r -> r.id().equals("HINT_FIRST_WOOD"))
                    .findFirst().orElseThrow();

            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 1, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 4, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 4, 4, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL))); // Already crafted planks
        }

        @Test
        @DisplayName("Rule HINT_LOW_HUNGER: Triggered when foodLevel <= 6 and has food in inventory")
        void testLowHungerRule() {
            MentorRule rule = MentorRuleRegistry.getRules().stream()
                    .filter(r -> r.id().equals("HINT_LOW_HUNGER"))
                    .findFirst().orElseThrow();

            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 2, 0, 15, 7, 6000, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 2, 0, 15, 6, 6000, GameType.SURVIVAL)));
            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 4, 6000, GameType.SURVIVAL))); // No food to eat
        }

        @Test
        @DisplayName("Rule HINT_TOTAL_DARKNESS: Triggered when block light == 0 and player holds torches")
        void testTotalDarknessRule() {
            MentorRule rule = MentorRuleRegistry.getRules().stream()
                    .filter(r -> r.id().equals("HINT_TOTAL_DARKNESS"))
                    .findFirst().orElseThrow();

            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 5, 0, 0, 1, 20, 16000, GameType.SURVIVAL)));
            assertTrue(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 5, 0, 0, 0, 20, 16000, GameType.SURVIVAL)));
            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 0, 20, 16000, GameType.SURVIVAL))); // No torches in bag
        }

        @Test
        @DisplayName("Rule HINT_CREATIVE_FLIGHT: Triggered when flying in Creative mode")
        void testCreativeFlightRule() {
            MentorRule rule = MentorRuleRegistry.getRules().stream()
                    .filter(r -> r.id().equals("HINT_CREATIVE_FLIGHT"))
                    .findFirst().orElseThrow();

            assertFalse(rule.evaluate(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.CREATIVE)));
            assertTrue(rule.evaluate(createSnapshot(false, false, true, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.CREATIVE)));
            assertFalse(rule.evaluate(createSnapshot(false, false, true, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL))); // Survival elytra/flying doesn't trigger creative tutorial
        }
    }

    @Nested
    @DisplayName("4. Academy Missions FSM & Interactive Step Predicates")
    class AcademyMissionsTests {

        @Test
        @DisplayName("Survival Mission 1: Movement & Look Step Predicates")
        void testMission1Predicates() {
            Mission m1 = MissionRegistry.getMissionById("SURVIVAL_1_MOVEMENT").orElseThrow();
            assertEquals(2, m1.steps().size());

            MissionStep step1 = m1.steps().get(0);
            assertFalse(step1.isCompleted(createSnapshot(false, false, false, 10, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(step1.isCompleted(createSnapshot(true, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));

            MissionStep step2 = m1.steps().get(1);
            assertFalse(step2.isCompleted(createSnapshot(false, false, false, 5, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(step2.isCompleted(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
        }

        @Test
        @DisplayName("Survival Mission 2: POI Radar & Approach Step Predicates")
        void testMission2Predicates() {
            Mission m2 = MissionRegistry.getMissionById("SURVIVAL_2_POI_RADAR").orElseThrow();
            BlockHitResult blockHit = new BlockHitResult(new Vec3(0, 1, 5), Direction.NORTH, new BlockPos(0, 1, 5), false);

            MissionStep step1 = m2.steps().get(0);
            assertFalse(step1.isCompleted(createSnapshot(false, false, false, 0, null, 999.0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(step1.isCompleted(createSnapshot(false, false, false, 0, blockHit, 5.0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));

            MissionStep step2 = m2.steps().get(1);
            assertFalse(step2.isCompleted(createSnapshot(false, false, false, 0, blockHit, 4.0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(step2.isCompleted(createSnapshot(false, false, false, 0, blockHit, 3.2, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
        }

        @Test
        @DisplayName("Survival Mission 3 & 4: Wood Mining and Crafting Table Predicates")
        void testMission3And4Predicates() {
            Mission m3 = MissionRegistry.getMissionById("SURVIVAL_3_MINE_WOOD").orElseThrow();
            assertFalse(m3.steps().get(0).isCompleted(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(m3.steps().get(0).isCompleted(createSnapshot(false, false, false, 0, null, 0, 1, 0, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));

            Mission m4 = MissionRegistry.getMissionById("SURVIVAL_4_CRAFTING").orElseThrow();
            assertFalse(m4.steps().get(0).isCompleted(createSnapshot(false, false, false, 0, null, 0, 1, 3, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(m4.steps().get(0).isCompleted(createSnapshot(false, false, false, 0, null, 0, 0, 4, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));

            assertFalse(m4.steps().get(1).isCompleted(createSnapshot(false, false, false, 0, null, 0, 0, 4, 0, 0, 0, 15, 20, 6000, GameType.SURVIVAL)));
            assertTrue(m4.steps().get(1).isCompleted(createSnapshot(false, false, false, 0, null, 0, 0, 0, 0, 0, 1, 15, 20, 6000, GameType.SURVIVAL)));
        }
    }

    @Nested
    @DisplayName("5. I18N Verification & Spoken Command Accuracy")
    class I18NAndKeyAccuracyTests {

        private JsonObject loadJson(String path) {
            InputStream stream = getClass().getResourceAsStream(path);
            assertNotNull(stream, "Resource not found: " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }

        @Test
        @DisplayName("Ensure all registered Mentor rules have valid I18N keys in both IT and EN")
        void testMentorRuleKeysInI18N() {
            JsonObject it = loadJson("/assets/minecraft_access/lang/it_it.json");
            JsonObject en = loadJson("/assets/minecraft_access/lang/en_us.json");

            for (MentorRule rule : MentorRuleRegistry.getRules()) {
                assertTrue(it.has(rule.messageKey()), "Missing IT key for mentor rule: " + rule.messageKey());
                assertTrue(en.has(rule.messageKey()), "Missing EN key for mentor rule: " + rule.messageKey());
            }
        }

        @Test
        @DisplayName("Ensure all registered Academy missions and steps have valid I18N keys in both IT and EN")
        void testAcademyMissionKeysInI18N() {
            JsonObject it = loadJson("/assets/minecraft_access/lang/it_it.json");
            JsonObject en = loadJson("/assets/minecraft_access/lang/en_us.json");

            for (Mission mission : MissionRegistry.getMissions()) {
                assertTrue(it.has(mission.titleKey()), "Missing IT title: " + mission.titleKey());
                assertTrue(en.has(mission.titleKey()), "Missing EN title: " + mission.titleKey());
                assertTrue(it.has(mission.descriptionKey()), "Missing IT desc: " + mission.descriptionKey());
                assertTrue(en.has(mission.descriptionKey()), "Missing EN desc: " + mission.descriptionKey());

                for (MissionStep step : mission.steps()) {
                    assertTrue(it.has(step.instructionKey()), "Missing IT instruction: " + step.instructionKey());
                    assertTrue(en.has(step.instructionKey()), "Missing EN instruction: " + step.instructionKey());
                    assertTrue(it.has(step.successKey()), "Missing IT success: " + step.successKey());
                    assertTrue(en.has(step.successKey()), "Missing EN success: " + step.successKey());
                }
            }
        }

        @Test
        @DisplayName("Ensure spoken commands in strings accurately mention correct keys for both Desktop Numpad and Laptop")
        void testSpokenCommandsAccuracy() {
            JsonObject it = loadJson("/assets/minecraft_access/lang/it_it.json");

            // Look guidance must mention M for North/horizon and 5 for numpad
            String lookDesc = it.get("minecraft_access.gui.quick_help.desc_look").getAsString();
            assertTrue(lookDesc.contains("M"), "Look guide must mention M for forward look");
            assertTrue(lookDesc.contains("5 del tastierino"), "Look guide must mention 5 on numpad");
            assertTrue(lookDesc.contains("Numpad 2 4 6 8"), "Look guide must mention Numpad 2 4 6 8");
            assertTrue(lookDesc.contains("I J K L"), "Look guide must mention I J K L for laptop users");

            // Action guidance must mention è / 0 for attack and + / Invio for interaction
            String actionDesc = it.get("minecraft_access.gui.quick_help.desc_action").getAsString();
            assertTrue(actionDesc.contains("è") && actionDesc.contains("0 del tastierino"), "Action guide must mention è on keyboard and 0 on numpad for attack");
            assertTrue(actionDesc.contains("+ della tastiera") && actionDesc.contains("Invio del tastierino"), "Action guide must mention + on keyboard and Invio on numpad for interaction");
            assertTrue(actionDesc.contains("Y"), "Action guide must mention Y for target locking");

            // POI guidance must mention End and PageUp/PageDown
            String poiDesc = it.get("minecraft_access.gui.quick_help.desc_poi").getAsString();
            assertTrue(poiDesc.contains("End"), "POI guide must mention End key");
            assertTrue(poiDesc.contains("PageUp") && poiDesc.contains("PageDown"), "POI guide must mention PageUp/PageDown");
        }

        @Test
        @DisplayName("Ensure GameMode guard rail & auto-advance keys exist in both IT and EN")
        void testGameModeGuardRailKeysInI18N() {
            JsonObject it = loadJson("/assets/minecraft_access/lang/it_it.json");
            JsonObject en = loadJson("/assets/minecraft_access/lang/en_us.json");

            String[] keys = new String[]{
                    "minecraft_access.academy.error_requires_creative",
                    "minecraft_access.academy.error_requires_survival",
                    "minecraft_access.gui.academy_hub.requires_creative_tag",
                    "minecraft_access.gui.academy_hub.requires_survival_tag",
                    "minecraft_access.academy.all_completed",
                    "minecraft_access.gui.academy_hub.auto_advance_on",
                    "minecraft_access.gui.academy_hub.auto_advance_off"
            };

            for (String key : keys) {
                assertTrue(it.has(key), "Missing IT key: " + key);
                assertTrue(en.has(key), "Missing EN key: " + key);
            }
        }
    }
}
