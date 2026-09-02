package org.mcaccess.minecraftaccess.features;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.balm.client.platform.util.SessionLocal;
import net.minecraft.client.Minecraft;
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
import org.mcaccess.minecraftaccess.utils.condition.Interval;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

/**
 * This feature reads the name of the targeted block or entity.<br>
 * It also gives feedback when a block is powered by a redstone signal or when a door is open similar cases.
 */
@Slf4j
public class NarrateCrosshair implements BalmClientModule {
    private static final SessionLocal<@Nullable Object> previousTarget = new SessionLocal<>(() -> null);
    private static final SessionLocal<@Nullable String> previousNarration = new SessionLocal<>(() -> null);
    private static final SessionLocal<@Nullable Integer> previousDistance = new SessionLocal<>(() -> null);
    private final SessionLocal<@Nullable Vec3> previousSoundPos = new SessionLocal<>(() -> null);
    private final Interval repetitionInterval = Interval.defaultDelay();
    private static final Config.NarrateCrosshair CONFIG = Config.getInstance().narrateCrosshair;
    private static long suppressUntil = 0;

    public static void suppressNarration(long durationMillis) {
        suppressUntil = System.currentTimeMillis() + durationMillis;
    }

    public static void synchronizeTarget(@Nullable HitResult rayCast, @Nullable String narration) {
        if (rayCast == null) {
            previousTarget.value = null;
            previousNarration.value = null;
            previousDistance.value = null;
            return;
        }
        Object target = switch (rayCast) {
            case BlockHitResult blockHitResult -> CONFIG.disableNarratingConsecutiveBlocks ? null : blockHitResult.getBlockPos();
            case EntityHitResult entityHitResult -> entityHitResult.getEntity();
            default -> rayCast;
        };
        previousTarget.value = target;
        previousNarration.value = narration;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            previousDistance.value = (int) Math.round(client.player.getEyePosition().distanceTo(rayCast.getLocation()));
        }
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "narrate_crosshair");
    }

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register(this::tick);
    }

    private void tick(Minecraft client, Player player, Level level) {
        if (client.gui.screen() != null) return;
        if (!CONFIG.enabled) return;
        if (System.currentTimeMillis() < suppressUntil) return;
        repetitionInterval.setDelay(CONFIG.repetitionInterval, Interval.Unit.MILLISECOND);

        WorldNarrator narrator = MainClass.registry(WorldNarrator.class).get(CONFIG.narrator);
        HitResult rayCast = narrator.rayCast();
        if (rayCast == null || rayCast.getType() == HitResult.Type.MISS) {
            previousTarget.value = null;
            previousNarration.value = null;
            previousDistance.value = null;
            return;
        }

        String narration = narrator.narrate(rayCast);
        Object target = switch (rayCast) {
            case BlockHitResult blockHitResult -> CONFIG.disableNarratingConsecutiveBlocks ? null : blockHitResult.getBlockPos();
            case EntityHitResult entityHitResult -> entityHitResult.getEntity();
            default -> rayCast;
        };

        double distance = player.getEyePosition().distanceTo(rayCast.getLocation());
        int roundedDistance = (int) Math.round(distance);

        Vec3 delta = player.getDeltaMovement();
        double speedSq = delta.x * delta.x + delta.z * delta.z;
        boolean isMoving = speedSq > 0.0001;
        boolean hasMoveInput = client.options.keyUp.isDown() || client.options.keyDown.isDown() || client.options.keyLeft.isDown() || client.options.keyRight.isDown();
        boolean inActiveMovement = isMoving || hasMoveInput;

        boolean targetChanged = !Objects.equals(target, previousTarget.value) || !Objects.equals(narration, previousNarration.value);
        boolean distanceChanged = CONFIG.narrateDistanceChangeInMovement && (previousDistance.value == null || !previousDistance.value.equals(roundedDistance));

        if (!targetChanged && !distanceChanged && !repetitionInterval.isReady()) {
            previousTarget.value = target;
            previousNarration.value = narration;
            previousDistance.value = roundedDistance;
            return;
        }

        previousTarget.value = target;
        previousNarration.value = narration;
        previousDistance.value = roundedDistance;

        if (narration == null) {
            return;
        }

        if (CONFIG.relativePositionSoundCue.enabled) {
            double rayCastDistance = Math.min(player.blockInteractionRange(), player.entityInteractionRange());
            Vec3 targetPosition = switch (rayCast) {
                case BlockHitResult blockHitResult -> Vec3.atCenterOf(blockHitResult.getBlockPos());
                case EntityHitResult entityHitResult -> entityHitResult.getEntity().position();
                default -> rayCast.getLocation();
            };
            if (!Objects.equals(targetPosition, previousSoundPos.value)) {
                playRelativePositionSoundCue(targetPosition, rayCastDistance,
                        SoundEvents.NOTE_BLOCK_HARP,
                        CONFIG.relativePositionSoundCue.minSoundVolume,
                        CONFIG.relativePositionSoundCue.maxSoundVolume);
            }
            previousSoundPos.value = targetPosition;
        }

        if (!(rayCast instanceof BlockHitResult || rayCast instanceof EntityHitResult)) {
            log.warn("Filtering only works on BlockHitResult and EntityHitResult. Using narrator {}", CONFIG.narrator);
        } else if (CONFIG.filter.enabled) {
            switch (rayCast) {
                case BlockHitResult blockHitResult when CONFIG.filter.targetMode.filterBlocks() -> {
                    Identifier key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(blockHitResult.getBlockPos()).getBlock());
                    if (isIgnored(key)) {
                        return;
                    }
                }
                case EntityHitResult entityHitResult when CONFIG.filter.targetMode.filterEntities() -> {
                    Identifier key = EntityType.getKey(entityHitResult.getEntity().getType());
                    if (isIgnored(key)) {
                        return;
                    }
                }
                default -> {
                }
            }
        }

        if (inActiveMovement) {
            CrosshairFeedbackManager.onCrosshairTargetChangedInMovement(rayCast, narration, distance);
        } else {
            CrosshairFeedbackManager.onCrosshairTargetChanged(rayCast, narration);
        }
    }

    private boolean isIgnored(Identifier identifier) {
        if (identifier == null) return false;
        String name = identifier.getPath();
        Predicate<String> p = CONFIG.filter.fuzzy ? name::contains : name::equals;
        return CONFIG.filter.whitelist
                ? Arrays.stream(CONFIG.filter.targets).noneMatch(p)
                : Arrays.stream(CONFIG.filter.targets).anyMatch(p);
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
}
