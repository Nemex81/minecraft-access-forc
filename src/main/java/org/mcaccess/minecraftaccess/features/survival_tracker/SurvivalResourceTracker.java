package org.mcaccess.minecraftaccess.features.survival_tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

@Slf4j
public class SurvivalResourceTracker implements BalmClientModule {
    private static SurvivalResourceTracker instance;

    @Getter
    private final SurvivalScanner scanner = new SurvivalScanner();

    private int periodicTickCounter = 0;
    private Vec3 lastScanPlayerPos = Vec3.ZERO;
    private BlockPos lastWoodPos = null;
    private BlockPos lastStonePos = null;
    private Object lastFoodTarget = null; // BlockPos or Entity UUID/ID

    public static SurvivalResourceTracker getInstance() {
        return instance;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "survival_tracker");
    }

    @Override
    public void initialize() {
        instance = this;
        ClientPlayingTick.AFTER.register(this::tick);

        // Shortcut 1: Numpad (Alt + Numpad 7)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "survival_tracker.scan_numpad"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_7, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    if (!org.mcaccess.minecraftaccess.utils.ModifierUtils.hasAltOnly()) return false;
                    scanAndNarrate(true);
                    return true;
                })
                .build();

        // Shortcut 2: Extended Keyboard (Alt + B)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "survival_tracker.scan_extended"))
                .withDefault(InputBinding.key(com.mojang.blaze3d.platform.InputConstants.KEY_B, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    if (!org.mcaccess.minecraftaccess.utils.ModifierUtils.hasAltOnly()) return false;
                    scanAndNarrate(true);
                    return true;
                })
                .build();
    }

    private void tick(Minecraft client, Player player, Level level) {
        if (client.gui.screen() != null) return;
        if (!(player instanceof LocalPlayer localPlayer)) return;

        Config.SurvivalTracker config = Config.getInstance().survivalTracker;
        if (!config.enabled || !config.periodicScanEnabled) {
            periodicTickCounter = 0;
            return;
        }

        periodicTickCounter++;
        int intervalTicks = Math.max(20, config.periodicIntervalSeconds * 20);

        if (periodicTickCounter >= intervalTicks) {
            periodicTickCounter = 0;
            performPeriodicScan(level, localPlayer, config);
        }
    }

    private void performPeriodicScan(Level level, LocalPlayer player, Config.SurvivalTracker config) {
        Map<SurvivalResourceType, SurvivalResourceTarget> targets = scanner.scan(level, player, config.range);

        SurvivalResourceTarget wood = targets.get(SurvivalResourceType.WOOD);
        SurvivalResourceTarget stone = targets.get(SurvivalResourceType.STONE);
        SurvivalResourceTarget food = targets.get(SurvivalResourceType.FOOD);

        BlockPos currentWoodPos = wood != null ? wood.blockPos() : null;
        BlockPos currentStonePos = stone != null ? stone.blockPos() : null;
        Object currentFoodTarget = food != null ? (food.blockPos() != null ? food.blockPos() : (food.entity() != null ? food.entity().getUUID() : null)) : null;

        Vec3 currentPos = player.position();
        boolean playerMoved = currentPos.distanceToSqr(lastScanPlayerPos) > 4.0; // Moved > 2 blocks
        boolean targetsChanged = !Objects.equals(currentWoodPos, lastWoodPos)
                || !Objects.equals(currentStonePos, lastStonePos)
                || !Objects.equals(currentFoodTarget, lastFoodTarget);

        lastScanPlayerPos = currentPos;
        lastWoodPos = currentWoodPos;
        lastStonePos = currentStonePos;
        lastFoodTarget = currentFoodTarget;

        // Smart Debounce: only narrate periodically if player moved or detected targets changed
        if (playerMoved || targetsChanged) {
            String narration = buildNarrationString(targets, config);
            MainClass.narrate(narration, false);
        }
    }

    public void scanAndNarrate(boolean manual) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Config.SurvivalTracker config = Config.getInstance().survivalTracker;
        Map<SurvivalResourceType, SurvivalResourceTarget> targets = scanner.scan(client.level, client.player, config.range);

        String narration = buildNarrationString(targets, config);
        MainClass.narrate(narration, manual);

        if (manual && client.level != null) {
            client.level.playLocalSound(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.PLAYERS,
                    0.5f,
                    1.2f,
                    false
            );
        }
    }

    public static String buildNarrationString(Map<SurvivalResourceType, SurvivalResourceTarget> targets, Config.SurvivalTracker config) {
        List<String> parts = new ArrayList<>();

        // Wood
        if (config.trackWood) {
            SurvivalResourceTarget wood = targets.get(SurvivalResourceType.WOOD);
            if (wood != null) {
                parts.add(wood.name() + " " + wood.relativeDirection() + ", " + wood.compassDirection() + ", " + wood.altitudeDirection());
            } else {
                parts.add(I18n.get("minecraft_access.survival_tracker.wood") + " " + I18n.get("minecraft_access.survival_tracker.not_found_masc"));
            }
        }

        // Stone
        if (config.trackStone) {
            SurvivalResourceTarget stone = targets.get(SurvivalResourceType.STONE);
            if (stone != null) {
                parts.add(stone.name() + " " + stone.relativeDirection() + ", " + stone.compassDirection() + ", " + stone.altitudeDirection());
            } else {
                parts.add(I18n.get("minecraft_access.survival_tracker.stone") + " " + I18n.get("minecraft_access.survival_tracker.not_found_fem"));
            }
        }

        // Food
        if (config.trackFood) {
            SurvivalResourceTarget food = targets.get(SurvivalResourceType.FOOD);
            if (food != null) {
                parts.add(food.name() + " " + food.relativeDirection() + ", " + food.compassDirection() + ", " + food.altitudeDirection());
            } else {
                parts.add(I18n.get("minecraft_access.survival_tracker.food") + " " + I18n.get("minecraft_access.survival_tracker.not_found_masc"));
            }
        }

        if (parts.isEmpty()) {
            return I18n.get("minecraft_access.survival_tracker.prefix") + ": " + I18n.get("minecraft_access.survival_tracker.none_enabled");
        }

        return I18n.get("minecraft_access.survival_tracker.prefix") + ": " + String.join("; ", parts);
    }
}
