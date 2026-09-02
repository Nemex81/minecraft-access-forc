package org.mcaccess.minecraftaccess.features.context;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.TimeIndicator;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

@Slf4j
public class PlayerContextEngine implements BalmClientModule {
    private static final int SAMPLE_INTERVAL_TICKS = 10;
    private static PlayerContextEngine instance;

    @Getter
    private PlayerContextSnapshot latestSnapshot = null;
    private final List<Consumer<PlayerContextSnapshot>> listeners = new ArrayList<>();

    private int tickCounter = 0;
    private int idleTicks = 0;
    private Vec3 lastPos = Vec3.ZERO;
    private float lastYRot = 0;
    private float lastXRot = 0;

    public static PlayerContextEngine getInstance() {
        return instance;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "player_context_engine");
    }

    @Override
    public void initialize() {
        instance = this;
        ClientPlayingTick.AFTER.register(this::onClientTick);
    }

    public void addListener(Consumer<PlayerContextSnapshot> listener) {
        listeners.add(listener);
    }

    private void onClientTick(Minecraft client, LocalPlayer player, ClientLevel level) {
        tickCounter++;

        // Track idle status
        Vec3 currentPos = player.position();
        float currentYRot = player.getYRot();
        float currentXRot = player.getXRot();

        boolean moved = currentPos.distanceToSqr(lastPos) > 0.001;
        boolean looked = Math.abs(currentYRot - lastYRot) > 0.5f || Math.abs(currentXRot - lastXRot) > 0.5f;

        if (moved || looked) {
            idleTicks = 0;
        } else {
            idleTicks++;
        }

        lastPos = currentPos;
        lastYRot = currentYRot;
        lastXRot = currentXRot;

        if (tickCounter % SAMPLE_INTERVAL_TICKS == 0) {
            latestSnapshot = captureSnapshot(client, player, level);
            for (Consumer<PlayerContextSnapshot> listener : listeners) {
                try {
                    listener.accept(latestSnapshot);
                } catch (Exception e) {
                    log.error("Error evaluating player context listener", e);
                }
            }
        }
    }

    public PlayerContextSnapshot captureSnapshot(Minecraft client, LocalPlayer player, ClientLevel level) {
        BlockPos playerPos = player.blockPosition();
        String biomeKey = level.getBiome(playerPos).unwrapKey()
                .map(k -> k.identifier().getPath())
                .orElse("unknown");

        int blockLight = level.getBrightness(LightLayer.BLOCK, playerPos);

        // Stuck detection: player is pressing movement keys but horizontal collision blocks forward motion
        boolean up = client.options.keyUp.isDown();
        boolean down = client.options.keyDown.isDown();
        boolean left = client.options.keyLeft.isDown();
        boolean right = client.options.keyRight.isDown();
        Double intendedRelAngle = org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.calculateIntendedMoveAngle(up, down, left, right);
        boolean hasMoveInput = intendedRelAngle != null;
        boolean isStuck = hasMoveInput && player.horizontalCollision;

        String collisionDirectionWord = net.minecraft.client.resources.language.I18n.get("minecraft_access.obstacle_detector.dir_forward").toLowerCase();
        if (hasMoveInput) {
            String dir = org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.getRelativeDirectionString(
                    intendedRelAngle,
                    org.mcaccess.minecraftaccess.Config.ObstacleDetector.DirectionFeedbackMode.EIGHT_DIRECTIONS
            );
            if (dir != null && !dir.isBlank()) {
                collisionDirectionWord = dir.toLowerCase();
            }
        }

        boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        boolean isSneaking = player.isCrouching();
        boolean isSprinting = player.isSprinting();
        boolean isFlying = player.getAbilities().flying;
        boolean isInWater = player.isInWater();

        HitResult hit = client.hitResult;
        double hitDist = (hit != null && hit.getType() != HitResult.Type.MISS)
                ? hit.getLocation().distanceTo(player.getEyePosition())
                : Double.MAX_VALUE;

        // Inventory counts
        int logs = 0;
        int planks = 0;
        int cobble = 0;
        int torches = 0;
        int food = 0;
        int craftingTables = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ItemTags.LOGS)) {
                logs += stack.getCount();
            } else if (stack.is(ItemTags.PLANKS)) {
                planks += stack.getCount();
            } else if (stack.is(Items.COBBLESTONE)) {
                cobble += stack.getCount();
            } else if (stack.is(Items.TORCH)) {
                torches += stack.getCount();
            } else if (stack.is(Items.CRAFTING_TABLE)) {
                craftingTables += stack.getCount();
            }

            if (stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                food += stack.getCount();
            }
        }

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int foodLevel = player.getFoodData().getFoodLevel();

        long timeOfDay = 0L;
        try {
            timeOfDay = (long) (TimeIndicator.getCurrentTime() * 1000.0);
        } catch (Exception ignored) {
        }

        int nearbyHostiles = level.getEntitiesOfClass(
                Monster.class,
                player.getBoundingBox().inflate(16.0)
        ).size();

        GameType gameMode = client.gameMode != null ? client.gameMode.getPlayerMode() : GameType.SURVIVAL;

        return new PlayerContextSnapshot(
                player.position(),
                playerPos,
                biomeKey,
                blockLight,
                isStuck,
                collisionDirectionWord,
                isMoving,
                isSneaking,
                isSprinting,
                isFlying,
                isInWater,
                hit,
                hitDist,
                logs,
                planks,
                cobble,
                torches,
                food,
                craftingTables,
                health,
                maxHealth,
                foodLevel,
                timeOfDay,
                nearbyHostiles,
                gameMode,
                idleTicks
        );
    }
}
