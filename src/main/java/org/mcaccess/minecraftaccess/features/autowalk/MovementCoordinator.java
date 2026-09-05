package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.event.callback.ClientLifecycleCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.NarrateCrosshair;
import org.mcaccess.minecraftaccess.features.ObstacleDetector;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder.PathResult;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;
import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;

/**
 * Movement Coordinator (Level 2 - Movement Domain Subsystem & Cognitive Event Factory).
 * Gestisce l'arbitraggio degli eventi di marcia verso il CognitiveCoordinator,
 * preserva la parità acustica e vocale storica 1:1, supporta il fallback legacy (Invariante 3),
 * implementa il debouncing rigoroso a 200ms per i suoni dei nodi, e vigila sul ciclo di vita client
 * (morte, respawn, cambio livello/dimensione, connessione e disconnessione).
 */
@Slf4j
public class MovementCoordinator {

    public static final String SEMANTIC_START = "autowalk:start";
    public static final String SEMANTIC_ARRIVED = "autowalk:arrived";
    public static final String SEMANTIC_NO_PATH = "autowalk:no_path";
    public static final String SEMANTIC_OUT_OF_RANGE = "autowalk:out_of_range";
    public static final String SEMANTIC_CANCELLED = "autowalk:cancelled";
    public static final String SEMANTIC_STUCK = "autowalk:stuck";
    public static final String SEMANTIC_DOOR_WAIT = "autowalk:door_wait";
    public static final String SEMANTIC_DOOR_OPENED = "autowalk:door_opened";
    public static final String SEMANTIC_PROGRESS = "autowalk:progress";
    public static final String SEMANTIC_STEP_NODE = "autowalk:step_node";

    @Getter
    private final RouteNavigator navigator;

    @Getter
    private final AutoWalkMotor motor;

    public MovementCoordinator() {
        this(new RouteNavigator(), new AutoWalkMotor());
    }

    public MovementCoordinator(RouteNavigator navigator, AutoWalkMotor motor) {
        this.navigator = navigator;
        this.motor = motor;
    }

    public boolean isActive() {
        return motor.isActive();
    }

    private static @Nullable ClientLevel lastLevel = null;
    private static int lastPlayerId = -1;

    @Getter
    @Setter
    private static long lastNodeSoundTime = 0;

    // Test Seams per test headless deterministici a 0ms
    public interface MessageResolver {
        String get(String key, Object... args);
    }

    private static @Nullable Consumer<CognitiveEvent> cognitiveEventConsumer = null;
    private static @Nullable BiConsumer<String, Boolean> legacyVoiceConsumer = null;
    private static @Nullable Consumer<SoundCue> legacyAudioConsumer = null;
    private static MessageResolver messageResolver = I18n::get;

    public static void setCognitiveEventConsumer(@Nullable Consumer<CognitiveEvent> consumer) {
        cognitiveEventConsumer = consumer;
    }

    public static void setLegacyVoiceConsumer(@Nullable BiConsumer<String, Boolean> consumer) {
        legacyVoiceConsumer = consumer;
    }

    public static void setLegacyAudioConsumer(@Nullable Consumer<SoundCue> consumer) {
        legacyAudioConsumer = consumer;
    }

    public static void setMessageResolver(MessageResolver resolver) {
        messageResolver = resolver;
    }

    private static @Nullable Config.AutoWalk testAutoWalkConfig = null;
    private static @Nullable Boolean testNarrateHints = null;

    public static void setTestAutoWalkConfig(@Nullable Config.AutoWalk config) {
        testAutoWalkConfig = config;
    }

    public static void setTestNarrateHints(@Nullable Boolean narrateHints) {
        testNarrateHints = narrateHints;
    }

    public static void resetTestSeams() {
        cognitiveEventConsumer = null;
        legacyVoiceConsumer = null;
        legacyAudioConsumer = null;
        messageResolver = I18n::get;
        lastLevel = null;
        lastPlayerId = -1;
        lastNodeSoundTime = 0;
        testAutoWalkConfig = null;
        testNarrateHints = null;
    }

    // ==========================================
    // Factory Pura dei 10 Eventi di Movimento
    // ==========================================

    /**
     * Evento 1: AUTOWALK_START (OPERATIONAL)
     */
    public static CognitiveEvent createStartEvent(
            String targetName,
            int distInt,
            int stepsInt,
            @Nullable BlockPos playerPos,
            float audioVolume,
            long now
    ) {
        String text = messageResolver.get(
                "minecraft_access.autowalk.start",
                targetName,
                NarrationUtils.narrateNumber(distInt),
                NarrationUtils.narrateNumber(stepsInt)
        );
        SoundCue cue = audioVolume > 0 && playerPos != null
                ? SoundCue.of(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, playerPos, audioVolume, 1.2f)
                : null;
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                SEMANTIC_START,
                StateSignature.of(distInt, stepsInt, targetName),
                text,
                playerPos,
                distInt,
                SpatialDirection.FORWARD,
                cue != null ? CognitiveEvent.OutputType.VOICE_AND_SOUND : CognitiveEvent.OutputType.VOICE_ONLY,
                cue,
                3000L,
                false,
                now
        );
    }

    /**
     * Evento 2: AUTOWALK_ARRIVED (OPERATIONAL, suona NOTE_BLOCK_BELL a pitch 1.2f)
     */
    public static CognitiveEvent createArrivedEvent(
            String targetName,
            @Nullable BlockPos playerPos,
            boolean voiceFeedback,
            float audioVolume,
            long now
    ) {
        String text = voiceFeedback ? messageResolver.get("minecraft_access.autowalk.arrived", targetName) : "";
        SoundCue cue = SoundCue.of(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, playerPos, 0.8f, 1.2f);
        CognitiveEvent.OutputType outputType = voiceFeedback
                ? CognitiveEvent.OutputType.VOICE_AND_SOUND
                : CognitiveEvent.OutputType.SOUND_ONLY;
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                SEMANTIC_ARRIVED,
                StateSignature.of(0, 0, targetName),
                text,
                playerPos,
                0.0,
                SpatialDirection.FORWARD,
                outputType,
                cue,
                3000L,
                false,
                now
        );
    }

    /**
     * Evento 3: AUTOWALK_NO_PATH (OPERATIONAL, suona NOTE_BLOCK_BASS a pitch 0.6f)
     */
    public static CognitiveEvent createNoPathEvent(
            String targetName,
            @Nullable BlockPos playerPos,
            float audioVolume,
            long now
    ) {
        String text = messageResolver.get("minecraft_access.autowalk.no_path", targetName);
        SoundCue cue = audioVolume > 0 && playerPos != null
                ? SoundCue.of(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, playerPos, audioVolume, 0.6f)
                : null;
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                SEMANTIC_NO_PATH,
                StateSignature.of(0, 0, targetName),
                text,
                playerPos,
                0.0,
                SpatialDirection.FORWARD,
                cue != null ? CognitiveEvent.OutputType.VOICE_AND_SOUND : CognitiveEvent.OutputType.VOICE_ONLY,
                cue,
                3000L,
                false,
                now
        );
    }

    /**
     * Evento 4: AUTOWALK_OUT_OF_RANGE (OPERATIONAL, VOICE_ONLY)
     */
    public static CognitiveEvent createOutOfRangeEvent(
            int distInt,
            @Nullable BlockPos playerPos,
            long now
    ) {
        String text = messageResolver.get("minecraft_access.autowalk.out_of_range", NarrationUtils.narrateNumber(distInt));
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                SEMANTIC_OUT_OF_RANGE,
                StateSignature.of(distInt, 0, null),
                text,
                playerPos,
                distInt,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000L,
                false,
                now
        );
    }

    /**
     * Evento 5: AUTOWALK_CANCELLED (OPERATIONAL, suona NOTE_BLOCK_HAT a pitch 0.5f)
     */
    public static CognitiveEvent createCancelledEvent(
            @Nullable BlockPos playerPos,
            float audioVolume,
            long now
    ) {
        String text = messageResolver.get("minecraft_access.autowalk.cancelled");
        SoundCue cue = audioVolume > 0 && playerPos != null
                ? SoundCue.of(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, playerPos, audioVolume, 0.5f)
                : null;
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                SEMANTIC_CANCELLED,
                StateSignature.EMPTY,
                text,
                playerPos,
                0.0,
                SpatialDirection.FORWARD,
                cue != null ? CognitiveEvent.OutputType.VOICE_AND_SOUND : CognitiveEvent.OutputType.VOICE_ONLY,
                cue,
                3000L,
                false,
                now
        );
    }

    /**
     * Evento 6: AUTOWALK_STUCK (OPERATIONAL, suona NOTE_BLOCK_BASS a pitch 0.5f)
     */
    public static CognitiveEvent createStuckEvent(
            @Nullable BlockPos playerPos,
            float audioVolume,
            long now
    ) {
        String text = messageResolver.get("minecraft_access.autowalk.stuck");
        SoundCue cue = audioVolume > 0 && playerPos != null
                ? SoundCue.of(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, playerPos, audioVolume, 0.5f)
                : null;
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                SEMANTIC_STUCK,
                StateSignature.EMPTY,
                text,
                playerPos,
                0.0,
                SpatialDirection.FORWARD,
                cue != null ? CognitiveEvent.OutputType.VOICE_AND_SOUND : CognitiveEvent.OutputType.VOICE_ONLY,
                cue,
                3000L,
                false,
                now
        );
    }

    /**
     * Evento 7: AUTOWALK_DOOR_WAIT (OPERATIONAL, VOICE_ONLY)
     */
    public static CognitiveEvent createDoorWaitEvent(
            @Nullable BlockPos doorPos,
            long now
    ) {
        String text = messageResolver.get("minecraft_access.autowalk.step_door_closed");
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                SEMANTIC_DOOR_WAIT,
                StateSignature.of(0, 0, doorPos != null ? doorPos.toShortString() : null),
                text,
                doorPos,
                0.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000L,
                false,
                now
        );
    }

    /**
     * Evento 8: AUTOWALK_DOOR_OPENED (OPERATIONAL, VOICE_ONLY)
     */
    public static CognitiveEvent createDoorOpenedEvent(
            String targetName,
            @Nullable BlockPos doorPos,
            long now
    ) {
        String text = messageResolver.get("minecraft_access.autowalk.step_door_opened", targetName);
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.OPERATIONAL,
                SEMANTIC_DOOR_OPENED,
                StateSignature.of(0, 0, targetName),
                text,
                doorPos,
                0.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                3000L,
                false,
                now
        );
    }

    /**
     * Evento 9: AUTOWALK_PROGRESS (CONTEXTUAL, VOICE_ONLY)
     */
    public static CognitiveEvent createProgressEvent(
            int remainingSteps,
            @Nullable BlockPos playerPos,
            long now
    ) {
        String text = messageResolver.get("minecraft_access.autowalk.step_progression", NarrationUtils.narrateNumber(remainingSteps));
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.CONTEXTUAL,
                SEMANTIC_PROGRESS,
                StateSignature.of(remainingSteps, 0, null),
                text,
                playerPos,
                0.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.VOICE_ONLY,
                null,
                2000L,
                false,
                now
        );
    }

    /**
     * Evento 10: AUTOWALK_STEP_NODE (PASSIVE, SOUND_ONLY, suona NOTE_BLOCK_HAT a pitch 1.8f)
     */
    public static CognitiveEvent createStepNodeEvent(
            @Nullable BlockPos playerPos,
            float audioVolume,
            long now
    ) {
        SoundCue cue = SoundCue.of(
                SoundEvents.NOTE_BLOCK_HAT.value(),
                SoundSource.BLOCKS,
                playerPos,
                audioVolume * 0.5f,
                1.8f
        );
        return new CognitiveEvent(
                SourceDomain.MOVEMENT,
                CognitivePriority.PASSIVE,
                SEMANTIC_STEP_NODE,
                StateSignature.EMPTY,
                "",
                playerPos,
                0.0,
                SpatialDirection.FORWARD,
                CognitiveEvent.OutputType.SOUND_ONLY,
                cue,
                500L,
                false,
                now
        );
    }

    // ==========================================
    // Dispacciamento Eventi e Fallback Legacy
    // ==========================================

    /**
     * Invia l'evento al CognitiveCoordinator se attivo, oppure applica il fallback legacy.
     * Rispetta l'Invariante 3: gli eventi OPERATIONAL usano sempre interrupt = true in modalità legacy.
     */
    public static void postEvent(CognitiveEvent event, boolean legacyInterrupt) {
        if (cognitiveEventConsumer != null) {
            cognitiveEventConsumer.accept(event);
            return;
        }

        if (CognitiveCoordinator.isCoordinatorEnabled()) {
            CognitiveCoordinator.submitEvent(event);
        } else {
            // Invariante 3: Trasparenza del fallback legacy con interrupt vocale
            if (event.isVoiceEnabled() && !event.narrationText().isBlank()) {
                if (legacyVoiceConsumer != null) {
                    legacyVoiceConsumer.accept(event.narrationText(), legacyInterrupt);
                } else {
                    MainClass.narrate(event.narrationText(), legacyInterrupt);
                }
            }
            if (event.isSoundEnabled() && event.soundCue() != null) {
                emitLegacySound(event.soundCue());
            }
        }
    }

    private static void emitLegacySound(SoundCue cue) {
        if (legacyAudioConsumer != null) {
            legacyAudioConsumer.accept(cue);
            return;
        }
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.player != null && cue.soundEvent() != null) {
                BlockPos pos = cue.position() != null ? cue.position() : client.player.blockPosition();
                client.level.playLocalSound(
                        pos,
                        cue.soundEvent(),
                        cue.soundSource(),
                        cue.volume(),
                        cue.pitch(),
                        true
                );
            }
        } catch (Exception e) {
            log.warn("Errore riproduzione suono legacy in MovementCoordinator: {}", e.getMessage());
        }
    }


    // ==========================================
    // Ciclo di Vita, Tick Monitor e Reset
    // ==========================================

    /**
     * Registra gli hook di ciclo di vita Balm per disconnessione e connessione al server.
     */
    public static void registerLifecycleHooks(AutoWalkMotor motor, RouteNavigator navigator) {
        try {
            ClientLifecycleCallback.ConnectedToServer.EVENT.register(_ -> resetSession(Minecraft.getInstance(), motor, navigator));
            ClientLifecycleCallback.DisconnectedFromServer.EVENT.register(_ -> resetSession(Minecraft.getInstance(), motor, navigator));
        } catch (Throwable t) {
            log.debug("Balm ClientLifecycleCallback non disponibile: {}", t.getMessage());
        }
    }

    /**
     * Avvia una nuova sessione di marcia verso il bersaglio specificato.
     */
    public void start(Object target) {
        if (target == null) return;
        Minecraft client = null;
        try {
            client = Minecraft.getInstance();
        } catch (Throwable ignored) {}
        if (client == null || client.player == null || client.level == null) return;
        start(client, client.player, client.level, target);
    }

    /**
     * Sovraccarico testabile headless per avviare una sessione di marcia.
     */
    public void start(
            @Nullable Minecraft client,
            @Nullable LocalPlayer player,
            @Nullable Level level,
            @Nullable Object target
    ) {
        if (player == null || level == null || target == null) return;

        Config.AutoWalk config = getAutoWalkConfig();
        if (!config.enabled) {
            String msg = messageResolver.get("minecraft_access.autowalk.disabled");
            postDirectVoice(msg, true);
            return;
        }

        PathResult result = navigator.startRoute(level, player.position(), target, config.maxRange);
        String targetName = navigator.getTargetName(target);
        BlockPos playerPos = player.blockPosition();
        long now = System.currentTimeMillis();

        switch (result.status()) {
            case OUT_OF_RANGE -> {
                motor.stop(client);
                navigator.clearRoute();
                int distInt = (int) Math.round(result.totalDistance());
                CognitiveEvent event = createOutOfRangeEvent(distInt, playerPos, now);
                postEvent(event, true);
            }
            case NO_PATH, SEARCH_BUDGET_EXHAUSTED -> {
                motor.stop(client);
                navigator.clearRoute();
                CognitiveEvent event = createNoPathEvent(targetName, playerPos, config.audioCueVolume, now);
                postEvent(event, true);
            }
            case ALREADY_AT_TARGET -> {
                boolean narrateHints = isNarrateHintsEnabled();
                AutoWalkMotor.MotorCallback callback = createMotorCallback(
                        navigator,
                        config,
                        narrateHints,
                        player::blockPosition
                );
                motor.finishArrival(client, player, navigator, config, callback);
            }
            case FOUND -> {
                motor.start(player.position(), player.onGround(), player.getY());
                int distInt = (int) Math.round(result.totalDistance());
                int stepsInt = navigator.getRemainingSteps();
                CognitiveEvent event = createStartEvent(targetName, distInt, stepsInt, playerPos, config.audioCueVolume, now);
                postEvent(event, true);
            }
        }
    }

    /**
     * Annulla la marcia corrente, rilascia i comandi fisici ed emette l'evento o notifica di cancellazione.
     */
    public void cancel(boolean narrate, @Nullable String reasonKey) {
        Minecraft client = null;
        try {
            client = Minecraft.getInstance();
        } catch (Throwable ignored) {}
        cancel(client, narrate, reasonKey);
    }

    /**
     * Sovraccarico testabile headless per annullare la marcia.
     */
    public void cancel(@Nullable Minecraft client, boolean narrate, @Nullable String reasonKey) {
        boolean wasActive = motor.isActive();
        motor.stop(client);
        motor.setState(AutoWalkMotor.State.CANCELLED);
        navigator.clearRoute();
        CognitiveCoordinator.clearDomainEvents(SourceDomain.MOVEMENT);

        BlockPos playerPos = (client != null && client.player != null) ? client.player.blockPosition() : null;
        Config.AutoWalk config = getAutoWalkConfig();

        if (wasActive) {
            // Se la marcia era attiva ed è una cancellazione generica con narrazione, usa il canale cognitivo unificato
            if (narrate && (reasonKey == null || reasonKey.equals("minecraft_access.autowalk.cancelled"))) {
                CognitiveEvent event = createCancelledEvent(playerPos, config.audioCueVolume, System.currentTimeMillis());
                postEvent(event, true);
                return;
            }

            // Se la marcia era attiva e c'è una motivazione specifica (es. autowalk.disabled) o narrate = false:
            // riproduce sempre il tradizionale suono di arresto HAT a volume config e pitch 0.5f
            SoundCue cue = SoundCue.of(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, playerPos, config.audioCueVolume, 0.5f);
            emitLegacySound(cue);

            if (narrate && reasonKey != null) {
                String msg = messageResolver.get(reasonKey);
                postDirectVoice(msg, true);
            }
        } else {
            // Se la marcia non era attiva, emette solo la voce se richiesta (senza suono di marcia interrotta)
            if (narrate) {
                String effectiveKey = (reasonKey != null) ? reasonKey : "minecraft_access.autowalk.cancelled";
                String msg = messageResolver.get(effectiveKey);
                postDirectVoice(msg, true);
            }
        }
    }

    /**
     * Esegue il tick di dominio del MovementCoordinator: verifica il ciclo di vita (morte/dimensione)
     * e fa avanzare il motore cinematico AutoWalkMotor.
     */
    public void tick(Minecraft client, LocalPlayer player, Level level) {
        if (level instanceof ClientLevel clientLevel) {
            handleClientTick(player, clientLevel, client, motor, navigator);
        }

        if (!motor.isActive()) return;

        Config.AutoWalk config = getAutoWalkConfig();
        if (!config.enabled) {
            cancel(client, true, "minecraft_access.autowalk.disabled");
            return;
        }

        boolean narrateHints = isNarrateHintsEnabled();
        AutoWalkMotor.MotorCallback callback = createMotorCallback(
                navigator,
                config,
                narrateHints,
                player::blockPosition
        );

        motor.tick(client, navigator, config, narrateHints, callback);
    }

    /**
     * Commuta l'abilitazione dello sprint per AutoWalk e notifica l'utente con interrupt = true.
     */
    public void toggleSprint() {
        Minecraft client = null;
        try {
            client = Minecraft.getInstance();
        } catch (Throwable ignored) {}
        toggleSprint(client);
    }

    /**
     * Sovraccarico testabile headless per il toggle sprint.
     */
    public void toggleSprint(@Nullable Minecraft client) {
        Config.AutoWalk config = getAutoWalkConfig();
        config.sprint = !config.sprint;
        try {
            Config.saveConfig();
        } catch (Throwable ignored) {}

        if (client != null && client.player != null && !config.sprint) {
            client.player.setSprinting(false);
        }

        String msg = config.sprint
                ? messageResolver.get("minecraft_access.autowalk.sprint_enabled")
                : messageResolver.get("minecraft_access.autowalk.sprint_disabled");
        postDirectVoice(msg, true);
    }

    private static Config.AutoWalk getAutoWalkConfig() {
        if (testAutoWalkConfig != null) {
            return testAutoWalkConfig;
        }
        Config inst = Config.getInstance();
        return (inst != null && inst.autoWalk != null) ? inst.autoWalk : new Config.AutoWalk();
    }

    private static boolean isNarrateHintsEnabled() {
        if (testNarrateHints != null) {
            return testNarrateHints;
        }
        Config inst = Config.getInstance();
        return inst != null && inst.speechSettings != null && inst.speechSettings.narrateHints;
    }

    /**
     * Emissione diretta sul canale vocale con interrupt per messaggi operativi e notifiche dirette.
     */
    public static void postDirectVoice(String message, boolean interrupt) {
        if (legacyVoiceConsumer != null) {
            legacyVoiceConsumer.accept(message, interrupt);
        } else {
            MainClass.narrate(message, interrupt);
        }
    }

    /**
     * Monitoraggio tick client per morte, cambio livello o cambio dimensione.
     */
    public static void handleClientTick(
            @Nullable LocalPlayer player,
            @Nullable ClientLevel level,
            @Nullable Minecraft client,
            AutoWalkMotor motor,
            RouteNavigator navigator
    ) {
        if (player == null || level == null) {
            return;
        }

        if (player.isDeadOrDying() || player.getHealth() <= 0) {
            resetSession(client, motor, navigator);
            return;
        }

        if (lastLevel == null) {
            lastLevel = level;
            lastPlayerId = player.getId();
        } else if (lastLevel != level || lastPlayerId != player.getId()) {
            lastLevel = level;
            lastPlayerId = player.getId();
            resetSession(client, motor, navigator);
        }
    }

    /**
     * Resetta completamente la sessione di movimento: ferma il motore, svuota la rotta
     * e cancella tutti gli eventi di dominio MOVEMENT nel CognitiveCoordinator.
     */
    public static void resetSession(
            @Nullable Minecraft client,
            AutoWalkMotor motor,
            RouteNavigator navigator
    ) {
        motor.stop(client);
        navigator.clearRoute();
        CognitiveCoordinator.clearDomainEvents(SourceDomain.MOVEMENT);
    }

    // ==========================================
    // Bridge Callback per AutoWalkMotor
    // ==========================================

    /**
     * Applica la soppressione temporanea passiva di sicurezza (1500ms) a NarrateCrosshair
     * e ObstacleDetector all'arrivo alla meta, con degradazione controllata in ambiente di test headless.
     */
    public static void suppressEnvironmentalNarration(long durationMillis) {
        try {
            NarrateCrosshair.suppressNarration(durationMillis);
        } catch (Throwable ignored) {
            // Degradazione controllata per test unitari headless senza istanza Minecraft/ClothConfig
        }
        try {
            ObstacleDetector.suppressWarnings(durationMillis);
        } catch (Throwable ignored) {
            // Degradazione controllata per test unitari headless senza istanza Minecraft/ClothConfig
        }
    }

    /**
     * Costruisce un'istanza di MotorCallback che collega gli eventi del motore cinematico
     * alla generazione e invio degli eventi cognitivi del MovementCoordinator.
     */
    public static AutoWalkMotor.MotorCallback createMotorCallback(
            RouteNavigator navigator,
            Config.AutoWalk config,
            boolean narrateHints,
            Supplier<BlockPos> playerPosSupplier
    ) {
        return new AutoWalkMotor.MotorCallback() {
            @Override
            public void onArrival(Object target) {
                String targetName = navigator.getTargetName(target);
                BlockPos pos = playerPosSupplier.get();
                CognitiveEvent event = createArrivedEvent(targetName, pos, config.voiceFeedback, config.audioCueVolume, System.currentTimeMillis());
                postEvent(event, true);
                suppressEnvironmentalNarration(1500);
            }

            @Override
            public void onStepNode() {
                if (config.playNodeSoundCue) {
                    long now = System.currentTimeMillis();
                    if (now - lastNodeSoundTime >= 200) {
                        lastNodeSoundTime = now;
                        CognitiveEvent event = createStepNodeEvent(playerPosSupplier.get(), config.audioCueVolume, now);
                        postEvent(event, false);
                    }
                }
            }

            @Override
            public void onProgression(int remainingSteps) {
                if (narrateHints) {
                    CognitiveEvent event = createProgressEvent(remainingSteps, playerPosSupplier.get(), System.currentTimeMillis());
                    postEvent(event, false);
                }
            }

            @Override
            public void onDoorClosed() {
                if (narrateHints) {
                    CognitiveEvent event = createDoorWaitEvent(playerPosSupplier.get(), System.currentTimeMillis());
                    postEvent(event, true);
                }
            }

            @Override
            public void onDoorOpened(Object target) {
                if (narrateHints) {
                    String targetName = navigator.getTargetName(target);
                    CognitiveEvent event = createDoorOpenedEvent(targetName, playerPosSupplier.get(), System.currentTimeMillis());
                    postEvent(event, true);
                }
            }

            @Override
            public void onTakeover() {
                CognitiveCoordinator.clearDomainEvents(SourceDomain.MOVEMENT);
                CognitiveEvent event = createCancelledEvent(playerPosSupplier.get(), config.audioCueVolume, System.currentTimeMillis());
                postEvent(event, true);
            }

            @Override
            public void onNoPath(Object target) {
                CognitiveCoordinator.clearDomainEvents(SourceDomain.MOVEMENT);
                String targetName = navigator.getTargetName(target);
                CognitiveEvent event = createNoPathEvent(targetName, playerPosSupplier.get(), config.audioCueVolume, System.currentTimeMillis());
                postEvent(event, true);
            }

            @Override
            public void onStuck() {
                CognitiveCoordinator.clearDomainEvents(SourceDomain.MOVEMENT);
                CognitiveEvent event = createStuckEvent(playerPosSupplier.get(), config.audioCueVolume, System.currentTimeMillis());
                postEvent(event, true);
            }

            @Override
            public void onRepathRequested() {
                // Silenzioso: ricalcolo dinamico interno in corso
            }
        };
    }
}
