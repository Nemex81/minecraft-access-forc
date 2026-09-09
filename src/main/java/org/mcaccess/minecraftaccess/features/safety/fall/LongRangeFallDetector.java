package org.mcaccess.minecraftaccess.features.safety.fall;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;

@Slf4j
public class LongRangeFallDetector {

    public record LongRangePit(BlockPos pos, int depth, double distance) {
    }

    private final Clock clock;
    private final Config.FallDetector config;
    private long previousScanTimeMs = 0;

    // Test seams package-private per testabilità headless a 0 ms
    static Consumer<SoundCue> legacyAudioConsumer = cue -> {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && cue.soundEvent() != null) {
            BlockPos pos = cue.position() != null ? cue.position() : (client.player != null ? client.player.blockPosition() : BlockPos.ZERO);
            client.level.playLocalSound(pos, cue.soundEvent(), cue.soundSource(), cue.volume(), cue.pitch(), true);
        }
    };
    static Consumer<CognitiveEvent> cognitiveEventConsumer = CognitiveCoordinator::submitEvent;
    static Supplier<SoundEvent> didgeridooSoundSupplier = () -> SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();

    public static void resetTestSeams() {
        legacyAudioConsumer = cue -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && cue.soundEvent() != null) {
                BlockPos pos = cue.position() != null ? cue.position() : (client.player != null ? client.player.blockPosition() : BlockPos.ZERO);
                client.level.playLocalSound(pos, cue.soundEvent(), cue.soundSource(), cue.volume(), cue.pitch(), true);
            }
        };
        cognitiveEventConsumer = CognitiveCoordinator::submitEvent;
        didgeridooSoundSupplier = () -> SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();
    }

    public static void setLegacyAudioConsumer(Consumer<SoundCue> consumer) {
        legacyAudioConsumer = consumer;
    }

    public static void setCognitiveEventConsumer(Consumer<CognitiveEvent> consumer) {
        cognitiveEventConsumer = consumer;
    }

    public static void setDidgeridooSoundSupplier(Supplier<SoundEvent> supplier) {
        didgeridooSoundSupplier = supplier;
    }

    public LongRangeFallDetector() {
        this(Clock.systemDefaultZone(), Config.getInstance() != null && Config.getInstance().fallDetector != null
                ? Config.getInstance().fallDetector : new Config.FallDetector());
    }

    public LongRangeFallDetector(Clock clock, Config.FallDetector config) {
        this.clock = clock;
        this.config = config;
    }

    /**
     * Tick periodico a bassa frequenza per il radar orografico a lungo raggio.
     */
    public void tick(
            Minecraft client,
            Player player,
            Level level,
            boolean isSuppressedByProximity,
            boolean isAutoWalkActive
    ) {
        // Regola 7: se AutoWalk è attivo, 100% quiete sensoriale (nessuna campanella distante)
        if (isAutoWalkActive) {
            return;
        }

        if (!config.enabled || !config.longRangeEnabled) {
            return;
        }

        // Se il rilevatore di prossimità è in allerta o in safe descent attiva, quiete reciproca
        if (isSuppressedByProximity) {
            return;
        }

        if (client.gui != null && client.gui.screen() != null) {
            return;
        }

        if (!player.onGround() || player.isInWater() || player.isSwimming()) {
            return;
        }

        long now = clock.millis();
        int interval = Math.max(1000, config.longRangeScanInterval);
        if (now - previousScanTimeMs < interval) {
            return;
        }
        previousScanTimeMs = now;

        scanOrography(player, level, now);
    }

    /**
     * Esegue la scansione polare a 16 settori radiali attorno al giocatore.
     * Campiona da minRange a maxRange a passi di 2 blocchi, arrestando il raggio
     * all'istante se intercetta ostacoli solidi continui (occlusione visiva/acustica).
     */
    public List<LongRangePit> scanOrography(Player player, Level level, long now) {
        List<LongRangePit> detectedPits = new ArrayList<>();
        BlockPos center = player.blockPosition();
        int playerBaseY = center.getY();

        int minR = config.getEffectiveLongMin();
        int maxR = config.getEffectiveLongMax();
        int threshold = Math.max(2, config.longRangeDepthThreshold);

        // 16 direzioni radiali (ogni 22.5 gradi)
        final int SECTOR_COUNT = 16;
        for (int i = 0; i < SECTOR_COUNT; i++) {
            double angle = i * (2.0 * Math.PI / SECTOR_COUNT);
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);

            // Campiona ogni blocco lungo il raggio tra minR e maxR (nessun punto cieco di parità)
            for (int r = minR; r <= maxR; r++) {
                int targetX = center.getX() + (int) Math.round(dirX * r);
                int targetZ = center.getZ() + (int) Math.round(dirZ * r);
                BlockPos probeFeet = new BlockPos(targetX, playerBaseY, targetZ);
                BlockPos probeHead = probeFeet.above();

                // 1. Verifica Occlusione: se incontriamo un blocco solido impenetrabile a quota testa, il raggio si interrompe
                BlockState headState = level.getBlockState(probeHead);
                VoxelShape headShape = headState.getCollisionShape(level, probeHead);
                if (!headShape.isEmpty()) {
                    break;
                }

                // 2. Verifica Pervietà del Calpestio: se è aria a quota piedi
                BlockState feetState = level.getBlockState(probeFeet);
                if (!feetState.isAir() || !level.getFluidState(probeFeet).isEmpty()) {
                    continue;
                }

                // 3. Calcolo profondità baratro
                int dropDepth = measureDropDepth(level, probeFeet, threshold);
                if (dropDepth >= threshold) {
                    double dist = Math.sqrt((targetX - center.getX()) * (targetX - center.getX())
                            + (targetZ - center.getZ()) * (targetZ - center.getZ()));
                    detectedPits.add(new LongRangePit(probeFeet, dropDepth, dist));
                    // Un solo candidato per ciascun raggio per non saturare l'udito
                    break;
                }
            }
        }

        // Emette il cue acustico posizionale per la buca più vicina/rilevante (massimo 1 evento per scansione)
        if (!detectedPits.isEmpty()) {
            LongRangePit primaryPit = detectedPits.stream()
                    .min((p1, p2) -> Double.compare(p1.distance(), p2.distance()))
                    .orElse(detectedPits.get(0));

            emitLongRangeFeedback(player, primaryPit, now);
        }

        return detectedPits;
    }

    /**
     * Misura la profondità della colonna d'aria rispettando la Regola 9 (Pervietà Continua / Contract D2.1).
     * Se incontra un blocco solido con collision shape, la scansione si arresta senza conteggiare oltre.
     */
    private int measureDropDepth(Level level, BlockPos startFeet, int maxCheck) {
        int depth = 0;
        BlockPos current = startFeet.below();

        while (depth <= Math.max(maxCheck + 2, 16)) {
            BlockState state = level.getBlockState(current);

            // Regola 9: arresto immediato se la colonna incontra solidi pieni
            if (!state.getCollisionShape(level, current).isEmpty()) {
                return depth;
            }

            // Se incontra fluidi (acqua o lava), restituisce la profondità raggiunta fino al pelo dell'acqua
            if (!level.getFluidState(current).isEmpty()) {
                return depth;
            }

            if (!state.isAir()) {
                return depth;
            }

            depth++;
            current = current.below();
        }

        return depth;
    }

    private void emitLongRangeFeedback(Player player, LongRangePit pit, long now) {
        int minR = config.getEffectiveLongMin();
        int maxR = config.getEffectiveLongMax();
        double realDist = pit.distance();

        // 1. Curva di Decadimento Lento: volume calcolato su distanza reale
        float progress = (float) ((realDist - minR) / (double) Math.max(1, maxR - minR));
        progress = Math.clamp(progress, 0.0f, 1.0f);

        float maxVol = config.volume * config.longRangeVolumeMultiplier;
        float minVol = maxVol * 0.5f;
        float computedVolume = maxVol - progress * (maxVol - minVol);

        SoundEvent didgeridooSound = didgeridooSoundSupplier != null ? didgeridooSoundSupplier.get() : SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();

        // 2. Proiezione Vettoriale Sicura: azzera il cutoff a 16 blocchi di OpenAL
        // preservando al 100% orientamento azimutale ed elevazione 3D
        Vec3 eyePos = (player != null && player.getEyePosition() != null) ? player.getEyePosition() : Vec3.ZERO;
        Vec3 pitCenter = Vec3.atCenterOf(pit.pos());
        Vec3 direction = pitCenter.subtract(eyePos);
        double dirLength = direction.length();

        BlockPos soundPos;
        if (dirLength > 0.001) {
            double acousticDist = Math.clamp(dirLength, 2.5, 12.0);
            Vec3 soundVec = eyePos.add(direction.normalize().scale(acousticDist));
            soundPos = BlockPos.containing(soundVec);
        } else {
            soundPos = pit.pos();
        }

        SoundCue cue = SoundCue.of(didgeridooSound, SoundSource.BLOCKS, soundPos, computedVolume, 0.8f);

        // 3. Canale Audio Diretto: il radar di lungo raggio è un feedback acustico puro non-verbale.
        // Emette direttamente all'audioConsumer per non subire lo scarto verbale del CognitiveCoordinator
        legacyAudioConsumer.accept(cue);

        CognitiveEvent event = CognitiveEvent.createSafetyAlert(
                "safety.fall.long_range",
                CognitivePriority.PASSIVE,
                StateSignature.of((int) Math.round(pit.distance()), pit.depth(), "fall:long_range"),
                "", // Zero voce a lungo raggio per non distrarre
                soundPos,
                pit.distance(),
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.SILENT, // SILENT per non duplicare l'audio a fine tick
                cue,
                3000L,
                now
        );

        if (CognitiveCoordinator.isCoordinatorEnabled()) {
            cognitiveEventConsumer.accept(event);
        }
    }
}
