package org.mcaccess.minecraftaccess.features;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.balm.client.platform.util.SessionLocal;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.WorldNarrator;
import org.mcaccess.minecraftaccess.features.crosshair.CrosshairFeedbackManager;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.ModifierUtils;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

/**
 * This feature reads the name of the targeted block or entity.<br>
 * It also gives feedback when a block is powered by a redstone signal or when a door is open similar cases.
 */
@Slf4j
public class NarrateCrosshair implements BalmClientModule {
    private final SessionLocal<@Nullable Vec3> previousSoundPos = new SessionLocal<>(() -> null);
    private static long suppressUntil = 0;

    private static Config.NarrateCrosshair getConfig() {
        Config cfg = Config.getInstance();
        return (cfg != null && cfg.narrateCrosshair != null) ? cfg.narrateCrosshair : new Config.NarrateCrosshair();
    }

    public static void suppressNarration(long durationMillis) {
        suppressUntil = System.currentTimeMillis() + durationMillis;
    }

    public static void synchronizeTarget(@Nullable HitResult rayCast, @Nullable String narration) {
        CrosshairFeedbackManager.synchronizeTarget(rayCast, narration);
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "narrate_crosshair");
    }

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register(this::tick);

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "narrate_crosshair.toggle_crosshair_audio"))
                .withDefault(InputBinding.key(InputConstants.KEY_F5, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasControlAndAlt()) return false;
                    toggleCrosshairAudio();
                    return true;
                })
                .build();
    }

    public void toggleCrosshairAudio() {
        Config cfg = Config.getInstance();
        if (cfg == null || cfg.narrateCrosshair == null || cfg.narrateCrosshair.relativePositionSoundCue == null) return;
        Config.NarrateCrosshair.RelativePositionSoundCue rpc = cfg.narrateCrosshair.relativePositionSoundCue;
        boolean wasSoundEnabled = rpc.isSoundEnabled();
        if (wasSoundEnabled) {
            if (rpc.feedbackMode == Config.NarrateCrosshair.ElevationFeedbackMode.SOUND_AND_VOICE) {
                rpc.feedbackMode = Config.NarrateCrosshair.ElevationFeedbackMode.VOICE_ONLY;
            } else {
                rpc.feedbackMode = Config.NarrateCrosshair.ElevationFeedbackMode.OFF;
            }
        } else {
            if (rpc.feedbackMode == Config.NarrateCrosshair.ElevationFeedbackMode.VOICE_ONLY) {
                rpc.feedbackMode = Config.NarrateCrosshair.ElevationFeedbackMode.SOUND_AND_VOICE;
            } else {
                rpc.feedbackMode = Config.NarrateCrosshair.ElevationFeedbackMode.SOUND_AND_VOICE;
            }
        }
        cfg.save();
        if (rpc.isSoundEnabled()) {
            MainClass.narrate(I18n.get("minecraft_access.narrate_crosshair.audio_on"), true);
        } else {
            MainClass.narrate(I18n.get("minecraft_access.narrate_crosshair.audio_off"), true);
        }
    }

    private void tick(Minecraft client, Player player, Level level) {
        if (client.gui.screen() != null) return;
        Config.NarrateCrosshair config = getConfig();
        if (!config.enabled) return;
        if (System.currentTimeMillis() < suppressUntil) return;

        WorldNarrator narrator = MainClass.registry(WorldNarrator.class).get(config.narrator);
        HitResult rayCast = (narrator != null) ? narrator.rayCast() : null;
        if (rayCast == null || rayCast.getType() == HitResult.Type.MISS) {
            CrosshairFeedbackManager.onCrosshairMiss();
            return;
        }

        String narration = narrator.narrate(rayCast);
        if (narration == null || narration.isBlank()) {
            CrosshairFeedbackManager.onCrosshairMiss();
            return;
        }

        if (config.relativePositionSoundCue.isSoundEnabled()) {
            boolean autoWalkActive = org.mcaccess.minecraftaccess.features.autowalk.MovementCoordinator.isAutoWalkActive();
            Config mainConfig = Config.getInstance();
            boolean silenceCrosshair = mainConfig != null && mainConfig.autoWalk != null && mainConfig.autoWalk.silenceCrosshairDuringWalk;
            if (!shouldSilenceCrosshairHarp(autoWalkActive, silenceCrosshair)) {
                double rayCastDistance = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
                Vec3 targetPosition = switch (rayCast) {
                    case BlockHitResult blockHitResult -> Vec3.atCenterOf(blockHitResult.getBlockPos());
                    case EntityHitResult entityHitResult -> entityHitResult.getEntity().position();
                    default -> rayCast.getLocation();
                };
                if (!Objects.equals(targetPosition, previousSoundPos.value)) {
                    playRelativePositionSoundCue(targetPosition, rayCastDistance,
                            SoundEvents.NOTE_BLOCK_HARP,
                            config.relativePositionSoundCue.minSoundVolume,
                            config.relativePositionSoundCue.maxSoundVolume);
                }
                previousSoundPos.value = targetPosition;
            }
        }

        if (!(rayCast instanceof BlockHitResult || rayCast instanceof EntityHitResult)) {
            log.warn("Filtering only works on BlockHitResult and EntityHitResult. Using narrator {}", config.narrator);
        } else if (config.filter.enabled) {
            switch (rayCast) {
                case BlockHitResult blockHitResult when config.filter.targetMode.filterBlocks() -> {
                    Identifier key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(blockHitResult.getBlockPos()).getBlock());
                    if (isIgnored(key, config)) {
                        return;
                    }
                }
                case EntityHitResult entityHitResult when config.filter.targetMode.filterEntities() -> {
                    Identifier key = EntityType.getKey(entityHitResult.getEntity().getType());
                    if (isIgnored(key, config)) {
                        return;
                    }
                }
                default -> {
                }
            }
        }

        Object target = switch (rayCast) {
            case BlockHitResult blockHitResult -> config.disableNarratingConsecutiveBlocks ? null : blockHitResult.getBlockPos();
            case EntityHitResult entityHitResult -> entityHitResult.getEntity();
            default -> rayCast;
        };

        double distance = player.getEyePosition().distanceTo(rayCast.getLocation());

        Vec3 delta = player.getDeltaMovement();
        double speedSq = delta.x * delta.x + delta.z * delta.z;
        boolean isMoving = speedSq > 0.0001;
        boolean hasMoveInput = client.options.keyUp.isDown() || client.options.keyDown.isDown() || client.options.keyLeft.isDown() || client.options.keyRight.isDown();
        boolean inActiveMovement = isMoving || hasMoveInput;

        String canonicalId = org.mcaccess.minecraftaccess.features.crosshair.CrosshairExplorationEventFactory.extractCanonicalId(rayCast, level);
        CrosshairFeedbackManager.processCrosshairTick(rayCast, target, narration, distance, inActiveMovement, canonicalId);
    }

    private boolean isIgnored(Identifier identifier, Config.NarrateCrosshair config) {
        if (identifier == null) return false;
        String name = identifier.getPath();
        Predicate<String> p = config.filter.fuzzy ? name::contains : name::equals;
        return config.filter.whitelist
                ? Arrays.stream(config.filter.targets).noneMatch(p)
                : Arrays.stream(config.filter.targets).anyMatch(p);
    }

    // To indicate relative location between player and target.
    private static void playRelativePositionSoundCue(Vec3 targetPosition, double maxDistance, Holder.Reference<SoundEvent> sound, double minVolume, double maxVolume) {
        assert Minecraft.getInstance().player != null;
        Vec3 playerPos = Minecraft.getInstance().player.position();

        // Use pitch to represent relative elevation, the higher the sound the higher the target.
        // The range of pitch is [0.5, 2.0], calculated as: 2 ^ (x / 12), where x is [-12, 12].
        // ref: https://minecraft.wiki/w/Note_Block#Notes
        //
        // Since we have a custom distance,
        // the range of (targetY - playerY) is [-maxDistance, maxDistance],
        // so let the maxDistance be the denominator to map to the original range.
        float pitch = (float) Math.pow(2, (targetPosition.y() - playerPos.y) / maxDistance);

        // Use volume to represent distance, the louder the sound the closer the distance.
        double distance = Math.sqrt(targetPosition.distanceToSqr(playerPos.x, playerPos.y, playerPos.z));
        // = base volume (minVolume) + the volume delta per block ((maxVolume - minVolume) / maxDistance)
        double volumeDeltaPerBlock = (maxVolume - minVolume) / maxDistance;
        float volume = (float) (minVolume + (maxDistance - distance) * volumeDeltaPerBlock);

        assert Minecraft.getInstance().level != null;
        Minecraft.getInstance().level.playLocalSound(
                targetPosition.x,
                targetPosition.y,
                targetPosition.z,
                sound.value(),
                SoundSource.BLOCKS,
                volume,
                pitch,
                true
        );
    }

    public static boolean shouldSilenceCrosshairHarp(boolean isAutoWalkActive, boolean silenceConfig) {
        return isAutoWalkActive && silenceConfig;
    }
}
