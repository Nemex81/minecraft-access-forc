package org.mcaccess.minecraftaccess.features.academy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class MissionRegistry {
    private static final List<Mission> MISSIONS = new ArrayList<>();

    static {
        // --- PERCORSO SOPRAVVIVENZA (PIONEER TRACK) ---

        // Mission 1: Movement & Look
        List<MissionStep> m1Steps = new ArrayList<>();
        m1Steps.add(new MissionStep(
                1,
                "minecraft_access.academy.m1.step1_instruction",
                s -> s.isMoving(),
                "minecraft_access.academy.m1.step1_success"
        ));
        m1Steps.add(new MissionStep(
                2,
                "minecraft_access.academy.m1.step2_instruction",
                s -> s.idleTicks() == 0,
                "minecraft_access.academy.m1.step2_success"
        ));
        MISSIONS.add(new Mission(
                "SURVIVAL_1_MOVEMENT",
                "minecraft_access.academy.m1.title",
                "minecraft_access.academy.m1.desc",
                false,
                m1Steps
        ));

        // Mission 2: POI Radar & Approach
        List<MissionStep> m2Steps = new ArrayList<>();
        m2Steps.add(new MissionStep(
                1,
                "minecraft_access.academy.m2.step1_instruction",
                s -> s.crosshairTarget() != null && s.crosshairTarget().getType() == HitResult.Type.BLOCK && s.crosshairDistance() < 12.0,
                "minecraft_access.academy.m2.step1_success"
        ));
        m2Steps.add(new MissionStep(
                2,
                "minecraft_access.academy.m2.step2_instruction",
                s -> s.crosshairDistance() <= 3.5,
                "minecraft_access.academy.m2.step2_success"
        ));
        MISSIONS.add(new Mission(
                "SURVIVAL_2_POI_RADAR",
                "minecraft_access.academy.m2.title",
                "minecraft_access.academy.m2.desc",
                false,
                m2Steps
        ));

        // Mission 3: Mine & Collect Wood
        List<MissionStep> m3Steps = new ArrayList<>();
        m3Steps.add(new MissionStep(
                1,
                "minecraft_access.academy.m3.step1_instruction",
                s -> s.woodLogsCount() >= 1,
                "minecraft_access.academy.m3.step1_success"
        ));
        MISSIONS.add(new Mission(
                "SURVIVAL_3_MINE_WOOD",
                "minecraft_access.academy.m3.title",
                "minecraft_access.academy.m3.desc",
                false,
                m3Steps
        ));

        // Mission 4: Crafting Table & Planks
        List<MissionStep> m4Steps = new ArrayList<>();
        m4Steps.add(new MissionStep(
                1,
                "minecraft_access.academy.m4.step1_instruction",
                s -> s.planksCount() >= 4,
                "minecraft_access.academy.m4.step1_success"
        ));
        m4Steps.add(new MissionStep(
                2,
                "minecraft_access.academy.m4.step2_instruction",
                s -> s.craftingTableCount() >= 1,
                "minecraft_access.academy.m4.step2_success"
        ));
        MISSIONS.add(new Mission(
                "SURVIVAL_4_CRAFTING",
                "minecraft_access.academy.m4.title",
                "minecraft_access.academy.m4.desc",
                false,
                m4Steps
        ));

        // Mission 5: Shelter & Safety
        List<MissionStep> m5Steps = new ArrayList<>();
        m5Steps.add(new MissionStep(
                1,
                "minecraft_access.academy.m5.step1_instruction",
                s -> s.torchesCount() > 0 || s.blockLight() > 0,
                "minecraft_access.academy.m5.step1_success"
        ));
        MISSIONS.add(new Mission(
                "SURVIVAL_5_SHELTER",
                "minecraft_access.academy.m5.title",
                "minecraft_access.academy.m5.desc",
                false,
                m5Steps
        ));

        // --- PERCORSO CREATIVA (BUILDER TRACK) ---

        // Creative Mission 1: Flight & Altitude
        List<MissionStep> c1Steps = new ArrayList<>();
        c1Steps.add(new MissionStep(
                1,
                "minecraft_access.academy.c1.step1_instruction",
                s -> s.isFlying(),
                "minecraft_access.academy.c1.step1_success"
        ));
        MISSIONS.add(new Mission(
                "CREATIVE_1_FLIGHT",
                "minecraft_access.academy.c1.title",
                "minecraft_access.academy.c1.desc",
                true,
                c1Steps
        ));

        // Creative Mission 2: Block Placement
        List<MissionStep> c2Steps = new ArrayList<>();
        c2Steps.add(new MissionStep(
                1,
                "minecraft_access.academy.c2.step1_instruction",
                s -> s.crosshairTarget() != null && s.crosshairTarget().getType() == HitResult.Type.BLOCK,
                "minecraft_access.academy.c2.step1_success"
        ));
        MISSIONS.add(new Mission(
                "CREATIVE_2_BUILD",
                "minecraft_access.academy.c2.title",
                "minecraft_access.academy.c2.desc",
                true,
                c2Steps
        ));
    }

    private MissionRegistry() {
    }

    public static List<Mission> getMissions() {
        return Collections.unmodifiableList(MISSIONS);
    }

    public static Optional<Mission> getMissionById(String id) {
        return MISSIONS.stream().filter(m -> m.id().equals(id)).findFirst();
    }
}
