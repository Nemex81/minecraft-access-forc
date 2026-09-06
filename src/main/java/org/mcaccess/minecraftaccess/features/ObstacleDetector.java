package org.mcaccess.minecraftaccess.features;

import java.time.Clock;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.blay09.mods.kuma.api.ManagedKeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.ObstacleScanResult;
import org.mcaccess.minecraftaccess.features.ObstacleDetectionUtils.ObstacleState;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.crosshair.CrosshairFeedbackManager;
import org.mcaccess.minecraftaccess.features.crosshair.ObstacleNarrationContext;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;
import org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils;

@Slf4j
public class ObstacleDetector implements BalmClientModule {
    private final Clock clock;
    private long previousTimeInMillis;
    private final Config.ObstacleDetector config;
    private @Nullable BlockPos lastWarnedObstaclePos = null;
    private ObstacleState lastWarnedState = ObstacleState.CLEAR;
    private static long suppressUntil = 0;

    public static void suppressWarnings(long durationMillis) {
        suppressUntil = System.currentTimeMillis() + durationMillis;
    }

    @FunctionalInterface
    interface LegacyObstacleVoiceSink {
        void accept(ObstacleScanResult result, String message, double relativeAngle);
    }

    @FunctionalInterface
    interface LegacyObstacleAudioSink {
        void accept(@Nullable Level level, SoundCue cue);
    }

    // Package-private test seams for deterministic headless testing without Minecraft runtime
    static java.util.function.Consumer<CognitiveEvent> cognitiveEventConsumer = CognitiveCoordinator::submitEvent;
    static LegacyObstacleVoiceSink legacyVoiceConsumer = CrosshairFeedbackManager::onObstacleDetected;
    static LegacyObstacleAudioSink legacyAudioConsumer = ObstacleDetector::playLegacySound;

    static void resetTestSeams() {
        cognitiveEventConsumer = CognitiveCoordinator::submitEvent;
        legacyVoiceConsumer = CrosshairFeedbackManager::onObstacleDetected;
        legacyAudioConsumer = ObstacleDetector::playLegacySound;
    }

    static void setCognitiveEventConsumer(java.util.function.Consumer<CognitiveEvent> consumer) {
        cognitiveEventConsumer = consumer;
    }

    static void setLegacyVoiceConsumer(LegacyObstacleVoiceSink consumer) {
        legacyVoiceConsumer = consumer;
    }

    static void setLegacyAudioConsumer(LegacyObstacleAudioSink consumer) {
        legacyAudioConsumer = consumer;
    }

    public ObstacleDetector() {
        this(Clock.systemDefaultZone(), Config.getInstance().obstacleDetector);
    }

    public ObstacleDetector(Clock clock, Config.ObstacleDetector config) {
        this.clock = clock;
        this.config = config;
        this.previousTimeInMillis = clock.millis();
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "obstacle_detector");
    }

    @Getter
    private static ManagedKeyMapping keyInspectObstacle;

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register(this::tick);

        keyInspectObstacle = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "obstacle_detector.inspect_obstacle"))
                .withDefault(InputBinding.key(InputConstants.KEY_V, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    inspectObstacle();
                    return true;
                })
                .build();
    }

    private void tick(Minecraft client, Player player, Level level) {
        if (!config.enabled) {
            resetState();
            return;
        }

        if (System.currentTimeMillis() < suppressUntil) {
            resetState();
            return;
        }

        if (client.gui.screen() != null || player.isUnderWater() || player.isSwimming() || player.isVisuallySwimming()) {
            resetState();
            return;
        }

        Vec3 delta = player.getDeltaMovement();
        double speedSq = delta.x * delta.x + delta.z * delta.z;
        boolean isMoving = speedSq > 0.0001;

        boolean up = client.options.keyUp.isDown();
        boolean down = client.options.keyDown.isDown();
        boolean left = client.options.keyLeft.isDown();
        boolean right = client.options.keyRight.isDown();
        Double intendedRelAngle = ObstacleDetectionUtils.calculateIntendedMoveAngle(up, down, left, right);
        boolean hasMoveInput = intendedRelAngle != null;

        if (!isMoving && !hasMoveInput) {
            return;
        }

        Vec3 moveDir;
        double relAngleForNarration;
        if (hasMoveInput) {
            relAngleForNarration = intendedRelAngle;
            moveDir = ObstacleDetectionUtils.calculateWorldMoveDirFromRelativeAngle(intendedRelAngle, player.getYRot());
        } else {
            moveDir = new Vec3(delta.x, 0, delta.z).normalize();
            relAngleForNarration = ObstacleDetectionUtils.calculateRelativeAngle(moveDir, player.getYRot());
        }

        ObstacleScanResult result = ObstacleDetectionUtils.scan(level, player.position(), moveDir, config.detectionRange);

        if (result.state() != ObstacleState.CLEAR) {
            long currentTime = clock.millis();
            boolean isNewPosition = lastWarnedObstaclePos == null || !lastWarnedObstaclePos.equals(result.targetFootPos());
            boolean isNewState = result.state() != lastWarnedState;
            boolean delayElapsed = (currentTime - previousTimeInMillis) >= config.delay;

            boolean shouldWarn = isNewPosition || isNewState || (hasMoveInput && delayElapsed);

            if (shouldWarn) {
                previousTimeInMillis = currentTime;
                lastWarnedObstaclePos = result.targetFootPos();
                lastWarnedState = result.state();

                int obstacleDistance = Math.max(1, (int) Math.round(Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(result.targetFootPos())))));
                dispatchObstacleAlert(
                        level,
                        result,
                        relAngleForNarration,
                        obstacleDistance,
                        config,
                        currentTime
                );
            }
        } else {
            lastWarnedState = ObstacleState.CLEAR;
            lastWarnedObstaclePos = null;
        }
    }

    static void dispatchObstacleAlert(
            ObstacleScanResult result,
            double relAngle,
            int obstacleDistance,
            Config.ObstacleDetector config,
            long now
    ) {
        dispatchObstacleAlert(
                null,
                result,
                relAngle,
                obstacleDistance,
                config.voiceWarning,
                config.playAudioCues,
                config.volume,
                config.narrationStyle,
                config.directionFeedbackMode,
                now
        );
    }

    static void dispatchObstacleAlert(
            @NotNull Level level,
            ObstacleScanResult result,
            double relAngle,
            int obstacleDistance,
            Config.ObstacleDetector config,
            long now
    ) {
        dispatchObstacleAlert(
                level,
                result,
                relAngle,
                obstacleDistance,
                config.voiceWarning,
                config.playAudioCues,
                config.volume,
                config.narrationStyle,
                config.directionFeedbackMode,
                now
        );
    }

    static void dispatchObstacleAlert(
            ObstacleScanResult result,
            double relAngle,
            int obstacleDistance,
            boolean voiceWanted,
            boolean soundWanted,
            float volume,
            ObstacleDetectionUtils.NarrationStyle narrationStyle,
            Config.ObstacleDetector.DirectionFeedbackMode directionFeedbackMode,
            long now
    ) {
        dispatchObstacleAlert(
                null,
                result,
                relAngle,
                obstacleDistance,
                voiceWanted,
                soundWanted,
                volume,
                narrationStyle,
                directionFeedbackMode,
                now
        );
    }

    static void dispatchObstacleAlert(
            @Nullable Level legacyLevel,
            ObstacleScanResult result,
            double relAngle,
            int obstacleDistance,
            boolean voiceWanted,
            boolean soundWanted,
            float volume,
            ObstacleDetectionUtils.NarrationStyle narrationStyle,
            Config.ObstacleDetector.DirectionFeedbackMode directionFeedbackMode,
            long now
    ) {
        if (!voiceWanted && !soundWanted) {
            return;
        }

        String rawMsg = ObstacleDetectionUtils.getNarrationMessage(
                result, narrationStyle, relAngle, directionFeedbackMode
        );

        boolean coordinatorActive = CognitiveCoordinator.isCoordinatorEnabled();
        if (coordinatorActive) {
            ObstacleNarrationContext crosshairContext = CrosshairFeedbackManager.getNarrationContextSnapshot();
            String finalMsg = ObstacleNarrationComposer.composeFinalNarration(rawMsg, obstacleDistance, crosshairContext);
            CognitiveEvent event = ObstacleSafetyEventFactory.createObstacleEvent(
                    result, finalMsg, obstacleDistance, relAngle, voiceWanted, soundWanted, volume, now
            );

            if (event != null) {
                if (event.isVoiceEnabled()) {
                    CrosshairFeedbackManager.suppressAutomaticMovementFeedback(100);
                }
                cognitiveEventConsumer.accept(event);
            }
        } else {
            if (voiceWanted) {
                legacyVoiceConsumer.accept(result, rawMsg, relAngle);
            }
            if (soundWanted) {
                SoundCue cue = ObstacleSafetyEventFactory.createSoundCue(result, volume);
                legacyAudioConsumer.accept(legacyLevel, cue);
            }
        }
    }

    private static void playLegacySound(@Nullable Level level, SoundCue cue) {
        if (level != null && cue.soundEvent() != null && cue.position() != null) {
            level.playLocalSound(cue.position(), cue.soundEvent(), cue.soundSource(), cue.volume(), cue.pitch(), true);
        }
    }

    public void inspectObstacle() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        ObstacleDetectionUtils.PanoramicScanResult pano = ObstacleDetectionUtils.scanPanoramic(
                client.level,
                client.player.position(),
                client.player.getYRot(),
                config.panoramicRange,
                config.directionFeedbackMode,
                config.checkHeadroomClearance
        );

        String msg = ObstacleDetectionUtils.getPanoramicNarrationMessage(pano, config.narrationStyle);

        if (!pano.obstacles().isEmpty()) {
            ObstacleDetectionUtils.DirectionalObstacle closestObs = pano.obstacles().stream()
                    .min((o1, o2) -> {
                        int distCmp = Integer.compare(o1.distanceInBlocks(), o2.distanceInBlocks());
                        if (distCmp != 0) return distCmp;
                        if (o1.relativeAngle() == 0.0) return -1;
                        if (o2.relativeAngle() == 0.0) return 1;
                        return 0;
                    })
                    .orElse(pano.obstacles().get(0));

            if (config.playAudioCues) {
                playSoundCue(client.level, closestObs.result());
            }

            if (config.lookAtObstacleOnInspection && closestObs.result().lookAtPos() != null) {
                LookHistoryManager.saveCurrentLook(client.player.getYRot(), client.player.getXRot());
                client.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(closestObs.result().lookAtPos()));
                String facing = PlayerPositionUtils.getFullFacingInWords(true);
                String lookedAtMsg = I18n.get("minecraft_access.obstacle_detector.looked_at_obstacle", facing);
                MainClass.narrate(msg + ". " + lookedAtMsg, true);
                return;
            }
        }

        MainClass.narrate(msg, true);
    }

    private void playSoundCue(Level level, ObstacleScanResult result) {
        SoundCue cue = ObstacleSafetyEventFactory.createSoundCue(result, config.volume);
        if (level != null && cue.soundEvent() != null) {
            level.playLocalSound(cue.position(), cue.soundEvent(), cue.soundSource(), cue.volume(), cue.pitch(), true);
        }
    }

    private void resetState() {
        lastWarnedObstaclePos = null;
        lastWarnedState = ObstacleState.CLEAR;
    }
}
