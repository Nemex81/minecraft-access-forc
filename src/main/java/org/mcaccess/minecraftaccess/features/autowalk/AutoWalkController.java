package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.WorldNarrator;
import org.mcaccess.minecraftaccess.features.NarrateCrosshair;
import org.mcaccess.minecraftaccess.features.ObstacleDetector;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder.PathResult;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder.PathStatus;
import org.mcaccess.minecraftaccess.features.point_of_interest.BlockPos3d;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;

@Slf4j
public class AutoWalkController {
    public enum State {
        IDLE,
        WALKING,
        JUMPING,
        SWIMMING,
        ARRIVED,
        CANCELLED
    }

    @Getter
    private State state = State.IDLE;

    @Getter
    private @Nullable Object targetObject = null;
    private @Nullable BlockPos currentGoalPos = null;
    private List<BlockPos> currentPath = List.of();
    private int currentPathIndex = 0;

    private Vec3 lastTickPos = Vec3.ZERO;
    private int stuckTicks = 0;
    private boolean wasInAir = false;
    private double lastGroundY = 0.0;
    private long lastNodeSoundTime = 0;
    private int jumpHoldingTicks = 0;
    private int startupGraceTicks = 0;
    private int sprintCooldownTicks = 0;

    public boolean isActive() {
        return state == State.WALKING || state == State.JUMPING || state == State.SWIMMING;
    }

    public void start(Object target) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Config.AutoWalk config = Config.getInstance().autoWalk;
        if (!config.enabled) {
            MainClass.narrate(I18n.get("minecraft_access.autowalk.disabled"), true);
            return;
        }

        this.targetObject = target;
        PathResult result = AutoWalkPathfinder.findPath(client.level, client.player.position(), target, config.maxRange);

        String targetName = getTargetName(target);

        switch (result.status()) {
            case OUT_OF_RANGE -> {
                int distInt = (int) Math.round(result.totalDistance());
                String msg = I18n.get("minecraft_access.autowalk.out_of_range", NarrationUtils.narrateNumber(distInt));
                MainClass.narrate(msg, true);
                resetMovement(client);
                this.state = State.IDLE;
            }
            case NO_PATH -> {
                String msg = I18n.get("minecraft_access.autowalk.no_path", targetName);
                MainClass.narrate(msg, true);
                if (client.level != null) {
                    client.level.playLocalSound(client.player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, config.audioCueVolume, 0.6f, true);
                }
                resetMovement(client);
                this.state = State.IDLE;
            }
            case ALREADY_AT_TARGET -> {
                String msg = I18n.get("minecraft_access.autowalk.arrived", targetName);
                MainClass.narrate(msg, true);
                lookAtTarget(client.player, target);
                playArrivalSound(client);
                resetMovement(client);
                this.state = State.ARRIVED;
            }
            case FOUND -> {
                this.currentPath = result.path();
                this.currentGoalPos = result.targetGoalPos();
                this.currentPathIndex = 0;
                this.stuckTicks = 0;
                this.startupGraceTicks = 10;
                this.wasInAir = !client.player.onGround();
                this.lastGroundY = client.player.getY();
                this.lastTickPos = client.player.position();
                this.sprintCooldownTicks = 0;
                this.state = State.WALKING;

                int distInt = (int) Math.round(result.totalDistance());
                int stepsInt = currentPath.size();
                String msg = I18n.get("minecraft_access.autowalk.start", targetName, NarrationUtils.narrateNumber(distInt), NarrationUtils.narrateNumber(stepsInt));
                MainClass.narrate(msg, true);

                if (client.level != null) {
                    client.level.playLocalSound(client.player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, config.audioCueVolume, 1.2f, true);
                }
            }
        }
    }

    public void cancel(boolean narrate, @Nullable String reasonKey) {
        Minecraft client = Minecraft.getInstance();
        resetMovement(client);

        if (isActive() && client.level != null && client.player != null) {
            Config.AutoWalk config = Config.getInstance().autoWalk;
            client.level.playLocalSound(client.player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, config.audioCueVolume, 0.5f, true);
        }

        this.state = State.CANCELLED;
        this.targetObject = null;
        this.currentPath = List.of();
        this.currentPathIndex = 0;
        this.startupGraceTicks = 0;
        this.sprintCooldownTicks = 0;

        if (narrate) {
            String msg = reasonKey != null ? I18n.get(reasonKey) : I18n.get("minecraft_access.autowalk.cancelled");
            MainClass.narrate(msg, true);
        }
    }

    public void tick(Minecraft client, LocalPlayer player, Level level) {
        if (!isActive()) return;

        Config.AutoWalk config = Config.getInstance().autoWalk;
        if (!config.enabled) {
            cancel(true, "minecraft_access.autowalk.disabled");
            return;
        }

        // 1. Human Takeover Check: if manual movement keys are pressed, cancel immediately
        if (startupGraceTicks > 0) {
            startupGraceTicks--;
        } else if (config.stopOnManualInput && isManualMovementKeyPressed(client)) {
            cancel(true, "minecraft_access.autowalk.cancelled");
            return;
        }

        // 2. Validate Target
        if (targetObject == null || !isTargetValid(targetObject)) {
            cancel(true, "minecraft_access.autowalk.cancelled");
            return;
        }

        // 3. Dynamic Entity Tracking: if entity moved > 2.0 blocks from goal, repath
        if (targetObject instanceof Entity entity) {
            if (currentGoalPos != null && entity.blockPosition().distSqr(currentGoalPos) > 4.0) {
                repath(level, player, config.maxRange);
                if (!isActive()) return;
            }
        }

        // 4. Post-Landing / Post-Descent Checkpoint (Re-Path upon touching ground from air)
        boolean onGround = player.onGround();
        if (wasInAir && onGround) {
            // Player just landed after jumping or stepping down: trigger instant repath from exact landed coordinates
            double currentY = player.getY();
            if (Math.abs(currentY - lastGroundY) > 0.4 || state == State.JUMPING) {
                lastGroundY = currentY;
                repath(level, player, config.maxRange);
                if (!isActive()) return;
            }
        }
        wasInAir = !onGround;
        if (onGround) {
            lastGroundY = player.getY();
        }

        // 5. Waypoint & Path Progression
        if (currentPathIndex >= currentPath.size()) {
            finishArrival(client, player, targetObject);
            return;
        }

        BlockPos targetNodePos = currentPath.get(currentPathIndex);
        Vec3 nodeCenter = Vec3.atBottomCenterOf(targetNodePos);

        double dx = nodeCenter.x - player.getX();
        double dz = nodeCenter.z - player.getZ();
        double distH = Math.hypot(dx, dz);
        double deltaY = nodeCenter.y - player.getY();

        // Advance to next waypoint if close enough horizontally (larger threshold during sprint for continuous momentum)
        double advanceThreshold = (config.sprint && player.isSprinting()) ? 0.70 : 0.45;
        if (distH < advanceThreshold && Math.abs(deltaY) < 1.0) {
            currentPathIndex++;
            playNodeSoundCue(level, player, config);

            if (currentPathIndex >= currentPath.size()) {
                finishArrival(client, player, targetObject);
                return;
            }

            targetNodePos = currentPath.get(currentPathIndex);
            nodeCenter = Vec3.atBottomCenterOf(targetNodePos);
            dx = nodeCenter.x - player.getX();
            dz = nodeCenter.z - player.getZ();
            distH = Math.hypot(dx, dz);
            deltaY = nodeCenter.y - player.getY();
        }

        // 6. Direct Goal Proximity Check
        BlockPos rawTargetPos = AutoWalkPathfinder.isStandable(level, targetNodePos) ? targetNodePos : currentGoalPos;
        if (rawTargetPos != null) {
            double distToFinalGoalSq = player.blockPosition().distSqr(rawTargetPos);
            if (distToFinalGoalSq <= 2.0 && currentPathIndex >= currentPath.size() - 1) {
                finishArrival(client, player, targetObject);
                return;
            }
        }

        // 7. Steering: smooth, continuous horizontal Yaw rotation
        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float currentYaw = player.getYRot();
        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float maxTurnRate = 20.0f; // degrees per tick (~400 deg/sec, smooth & responsive, zero undershoot)
        float newYaw = currentYaw + Mth.clamp(yawDiff, -maxTurnRate, maxTurnRate);
        player.setYRot(newYaw);

        // 8. Movement Forward Injection & Sprint Hysteresis Control
        // When turning actively (|yawDiff| > 15 degrees), drop sprint and hold walking speed for at least 20 ticks (1s)
        // to prevent rapid zooming/chattering of FOV and animation jitter
        if (Math.abs(yawDiff) > 15.0f) {
            sprintCooldownTicks = 20;
        } else if (sprintCooldownTicks > 0) {
            sprintCooldownTicks--;
        }

        boolean canSprint = config.sprint && sprintCooldownTicks == 0 && !player.isShiftKeyDown() && player.getFoodData().getFoodLevel() > 6.0f;

        if (Math.abs(yawDiff) > 55.0f && distH > 0.6) {
            client.options.keyUp.setDown(false);
        } else {
            client.options.keyUp.setDown(true);
        }

        player.setSprinting(canSprint);

        // 9. Water & Auto-Swim Handling
        if (player.isInWater() || player.isInLiquid()) {
            state = State.SWIMMING;
            if (config.autoSwim) {
                // Hold jump to float and swim forward on water surface
                client.options.keyJump.setDown(true);
            }
        } else {
            // 10. Step-Up Jump Timing
            if (config.autoJump && deltaY > 0.40 && distH < 0.65 && onGround) {
                state = State.JUMPING;
                client.options.keyJump.setDown(true);
                jumpHoldingTicks = 3;
            } else {
                if (jumpHoldingTicks > 0) {
                    jumpHoldingTicks--;
                } else {
                    client.options.keyJump.setDown(false);
                    if (state == State.SWIMMING || state == State.JUMPING) {
                        state = State.WALKING;
                    }
                }
            }
        }

        // 11. Stuck Watchdog Detection
        Vec3 currentPos = player.position();
        double movedDist = currentPos.distanceTo(lastTickPos);
        lastTickPos = currentPos;

        if (movedDist < 0.04 && onGround) {
            stuckTicks++;
            if (stuckTicks == 12) {
                // First stuck threshold: attempt immediate repath
                repath(level, player, config.maxRange);
            } else if (stuckTicks >= 24) {
                // Second stuck threshold: abort safely
                cancel(true, "minecraft_access.autowalk.stuck");
            }
        } else {
            stuckTicks = 0;
        }
    }

    public void toggleSprint() {
        Config.AutoWalk config = Config.getInstance().autoWalk;
        config.sprint = !config.sprint;
        Config.saveConfig();

        Minecraft client = Minecraft.getInstance();
        if (client.player != null && !config.sprint) {
            client.player.setSprinting(false);
        }

        String msg = config.sprint
                ? I18n.get("minecraft_access.autowalk.sprint_enabled")
                : I18n.get("minecraft_access.autowalk.sprint_disabled");
        MainClass.narrate(msg, true);
    }

    private void repath(Level level, LocalPlayer player, int maxRange) {
        if (targetObject == null) return;
        PathResult result = AutoWalkPathfinder.findPath(level, player.position(), targetObject, maxRange);
        if (result.status() == PathStatus.FOUND) {
            this.currentPath = result.path();
            this.currentGoalPos = result.targetGoalPos();
            this.currentPathIndex = 0;
        } else if (result.status() == PathStatus.ALREADY_AT_TARGET) {
            finishArrival(Minecraft.getInstance(), player, targetObject);
        } else {
            cancel(true, "minecraft_access.autowalk.no_path");
        }
    }

    private void finishArrival(Minecraft client, LocalPlayer player, Object target) {
        Config.AutoWalk config = Config.getInstance().autoWalk;
        resetMovement(client);
        this.state = State.ARRIVED;

        if (config.lookAtTargetOnArrival) {
            lookAtTarget(player, target);
        }

        NarrateCrosshair.suppressNarration(1500);
        ObstacleDetector.suppressWarnings(1500);
        String targetName = getTargetName(target);
        if (config.voiceFeedback) {
            MainClass.narrate(I18n.get("minecraft_access.autowalk.arrived", targetName), true);
        }

        playArrivalSound(client);
        this.targetObject = null;
        this.currentPath = List.of();
        this.currentPathIndex = 0;
    }

    private void lookAtTarget(LocalPlayer player, Object target) {
        switch (target) {
            case Entity entity -> player.lookAt(EntityAnchorArgument.Anchor.EYES, entity.getEyePosition());
            case BlockPos3d bp3d -> player.lookAt(EntityAnchorArgument.Anchor.EYES, bp3d.getAccuratePosition());
            case BlockPos bp -> player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(bp));
            case Waypoint wp -> player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(wp.pos()));
            default -> {}
        }
    }

    private void playArrivalSound(Minecraft client) {
        if (client.level != null && client.player != null) {
            client.level.playLocalSound(client.player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.2f, true);
        }
    }

    private void playNodeSoundCue(Level level, LocalPlayer player, Config.AutoWalk config) {
        if (!config.playNodeSoundCue) return;
        long now = System.currentTimeMillis();
        if (now - lastNodeSoundTime >= 200) {
            lastNodeSoundTime = now;
            level.playLocalSound(player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, config.audioCueVolume * 0.5f, 1.8f, true);
        }
    }

    private void resetMovement(Minecraft client) {
        if (client.options != null) {
            client.options.keyUp.setDown(false);
            client.options.keyJump.setDown(false);
        }
        if (client.player != null) {
            client.player.setSprinting(false);
        }
    }

    private boolean isManualMovementKeyPressed(Minecraft client) {
        if (client.options == null) return false;
        // KeyUp is managed by the autowalk bot, so we only check manual braking/steering inputs:
        // keyDown (S - brake/backwards), keyLeft (A), keyRight (D), keyShift (Sneak/stop)
        return client.options.keyDown.isDown()
                || client.options.keyLeft.isDown()
                || client.options.keyRight.isDown()
                || client.options.keyShift.isDown();
    }

    private boolean isTargetValid(Object target) {
        return switch (target) {
            case Entity entity -> entity.isAlive();
            case BlockPos3d bp3d -> true;
            case BlockPos bp -> true;
            case Waypoint wp -> true;
            default -> false;
        };
    }

    private String getTargetName(Object target) {
        if (target == null) return "";
        try {
            return switch (target) {
                case Entity entity -> MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(entity);
                case BlockPos3d bp3d -> MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(bp3d);
                case BlockPos bp -> MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(bp);
                case Waypoint wp -> wp.name();
                default -> target.toString();
            };
        } catch (Exception e) {
            log.debug("Error narrating target name: ", e);
            return target.toString();
        }
    }
}
