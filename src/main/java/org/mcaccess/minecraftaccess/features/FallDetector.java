package org.mcaccess.minecraftaccess.features;

import java.util.Objects;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.safety.fall.CentralFallSafetyManager;
import org.mcaccess.minecraftaccess.features.safety.fall.ProximityFallDetector;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafetyMovementGuard;

/**
 * Facade delegante verso {@link CentralFallSafetyManager} e {@link ProximityFallDetector}.
 * Mantiene la piena compatibilità all'indietro per API storiche, mixin e suite di test,
 * de-monolitizzando l'architettura delle cadute (Rev MC-26.18).
 */
@Slf4j
public class FallDetector implements BalmClientModule {

    public record DangerInfo(BlockPos pos, int depth, double distance) {}

    @Getter
    private final CentralFallSafetyManager delegate;

    public FallDetector() {
        this(new CentralFallSafetyManager());
    }

    public FallDetector(CentralFallSafetyManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public @NotNull Identifier getId() {
        return delegate.getId();
    }

    @Override
    public void initialize() {
        delegate.initialize();
    }

    public void tick(Minecraft client, Player player, Level level) {
        delegate.tick(client, player, level);
    }

    public static boolean isAutoSneakActive() {
        return ProximityFallDetector.isAutoSneakActive();
    }

    public static boolean shouldSilenceFallVoiceWarnings(boolean isAutoWalkActive, boolean configFallVoice) {
        return ProximityFallDetector.shouldSilenceFallVoiceWarnings(isAutoWalkActive, configFallVoice);
    }

    public static void resetTestSeams() {
        CentralFallSafetyManager.resetTestSeams();
    }

    public static void setLegacyNarrationConsumer(java.util.function.BiConsumer<String, Boolean> consumer) {
        ProximityFallDetector.setLegacyNarrationConsumer(consumer);
    }

    public static void setLegacyAudioConsumer(java.util.function.Consumer<SoundCue> consumer) {
        ProximityFallDetector.setLegacyAudioConsumer(consumer);
    }

    public static void setCognitiveEventConsumer(java.util.function.Consumer<CognitiveEvent> consumer) {
        ProximityFallDetector.setCognitiveEventConsumer(consumer);
    }

    public static void setFallSoundSupplier(java.util.function.Supplier<SoundEvent> supplier) {
        ProximityFallDetector.setAnvilSoundSupplier(supplier);
    }

    public static void setMovementGuard(@Nullable SafetyMovementGuard guard) {
        ProximityFallDetector.setMovementGuard(guard);
    }

    public SafetyMovementGuard getMovementGuard() {
        return delegate.getProximityDetector().getMovementGuard();
    }

    public static boolean isSafeWalkableStaircase(Level level, BlockPos landingPos, int playerBaseY) {
        return ProximityFallDetector.isSafeWalkableStaircase(level, landingPos, playerBaseY);
    }

    public boolean isStandingOnDangerousEdge(Player player, Level level) {
        return delegate.getProximityDetector().isStandingOnDangerousEdge(player, level);
    }

    public @Nullable DangerInfo findDangerAhead(Player player, Level level, Vec3 moveDir) {
        ProximityFallDetector.DangerInfo danger = delegate.getProximityDetector().findDangerAhead(player, level, moveDir);
        if (danger == null) return null;
        return new DangerInfo(danger.pos(), danger.depth(), danger.distance());
    }

    public void inspectNearbyFalls() {
        delegate.getProximityDetector().inspectNearbyFalls();
    }

    public void toggleAutoSneak() {
        delegate.getProximityDetector().toggleAutoSneak();
    }

    public static @Nullable CognitiveEvent buildFallEvent(
            BlockPos dangerPos,
            int depth,
            double distance,
            boolean isEdgeBump,
            boolean voiceEnabled,
            boolean soundEnabled,
            float volume,
            @NotNull String msg,
            long now
    ) {
        CognitivePriority priority = (distance <= 1.5 || isEdgeBump) ? CognitivePriority.CRITICAL : CognitivePriority.OPERATIONAL;
        SoundEvent sound = (distance <= 1.5 || isEdgeBump) ? SoundEvents.ANVIL_LAND : SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value();
        return ProximityFallDetector.buildFallEvent(dangerPos, depth, distance, isEdgeBump, voiceEnabled, soundEnabled, volume, msg, sound, priority, now);
    }

    public static void dispatchFallAlert(
            @Nullable CognitiveEvent event,
            boolean coordinatorEnabled,
            boolean voiceEnabled,
            boolean soundEnabled,
            String legacyMsg,
            BlockPos dangerPos,
            float volume
    ) {
        SoundEvent sound = SoundEvents.ANVIL_LAND;
        ProximityFallDetector.dispatchFallAlert(event, coordinatorEnabled, voiceEnabled, soundEnabled, legacyMsg, dangerPos, sound, volume);
    }
}
