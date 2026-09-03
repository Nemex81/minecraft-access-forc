package org.mcaccess.minecraftaccess.features.cognitive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

/**
 * Central Cognitive Coordinator for Minecraft Access.
 * Arbitrates events from domain managers (Safety, Exploration, Movement, Status, Guidance),
 * enforces priority hierarchy, provides immediate 0ms Fast-Path for critical danger,
 * flushes and concatenates compatible events at client tick boundary,
 * and maintains short-term attention memory to eliminate speech truncation and auditory chatter.
 */
@Slf4j
public class CognitiveCoordinator implements BalmClientModule {
    private static CognitiveCoordinator instance;

    @Getter
    @Setter
    private static boolean coordinatorEnabled = true;

    @Getter
    @Setter
    private static boolean chainedNarrationEnabled = true;

    @Getter
    @Setter
    private static long deduplicationWindowMs = 1500;

    @Getter
    @Setter
    private static boolean criticalModAudioDucking = true;

    // Output delegates (customizable for JUnit testing without Minecraft environment)
    @Setter
    private static BiConsumer<String, Boolean> narrationConsumer = MainClass::narrate;

    @Setter
    private static Consumer<SoundCue> audioConsumer = CognitiveCoordinator::defaultPlaySound;

    public interface TemplateResolver {
        @Nullable String resolve(String templateKey, String first, String second);
    }

    @Setter
    private static TemplateResolver templateResolver = CognitiveCoordinator::defaultResolveTemplate;

    // Fast-path and tick state
    private static int criticalCountInTick = 0;
    private static long criticalShieldUntil = 0;

    // Tick buffer and short-term queues
    private static final List<CognitiveEvent> tickBuffer = new ArrayList<>();
    private static final Queue<CognitiveEvent> shortQueue = new LinkedList<>();

    // High-fidelity deduplication cache (LRU up to 32 entries)
    private static final int MAX_DEDUPLICATION_CACHE = 32;
    private static final Map<DeduplicationKey, Long> recentEvents = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<DeduplicationKey, Long> eldest) {
            return size() > MAX_DEDUPLICATION_CACHE;
        }
    };

    // Attention memory (max 8 records)
    private static final AttentionMemory attentionMemory = new AttentionMemory();

    public record DeduplicationKey(
            SourceDomain domain,
            String semanticKey,
            @Nullable BlockPos targetPos,
            StateSignature stateSignature,
            CognitivePriority priority
    ) {}

    public static class AttentionMemory {
        private @Nullable CognitiveEvent lastDanger = null;
        private @Nullable CognitiveEvent lastTarget = null;
        private @Nullable String lastReportedSafeState = null;
        private long lastDangerClearedTimestamp = 0;

        public synchronized void setLastDanger(@Nullable CognitiveEvent danger) {
            this.lastDanger = danger;
        }

        public synchronized @Nullable CognitiveEvent getLastDanger() {
            return lastDanger;
        }

        public synchronized void setLastTarget(@Nullable CognitiveEvent target) {
            this.lastTarget = target;
        }

        public synchronized @Nullable CognitiveEvent getLastTarget() {
            return lastTarget;
        }

        public synchronized void notifySafetyClear(String message, long now) {
            this.lastReportedSafeState = message;
            this.lastDangerClearedTimestamp = now;
            this.lastDanger = null;
        }

        public synchronized boolean isRecentSafetyClear(long now, long windowMs) {
            return (now - lastDangerClearedTimestamp) < windowMs;
        }

        public synchronized void clear() {
            lastDanger = null;
            lastTarget = null;
            lastReportedSafeState = null;
            lastDangerClearedTimestamp = 0;
        }
    }

    public static CognitiveCoordinator getInstance() {
        return instance;
    }

    public static AttentionMemory getAttentionMemory() {
        return attentionMemory;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "cognitive_coordinator");
    }

    @Override
    public void initialize() {
        instance = this;
        ClientPlayingTick.AFTER.register(this::onClientTick);
    }

    /**
     * Submit an event from any domain manager to the Cognitive Coordinator.
     * CRITICAL events are handled immediately via Fast-Path (0ms latency).
     * Lower priority events are collected into the tick buffer for end-of-tick arbitration.
     */
    public static void submitEvent(@NotNull CognitiveEvent event) {
        submitEvent(event, System.currentTimeMillis());
    }

    /**
     * Submit an event with an explicit timestamp (supports testing and deterministic clocks).
     */
    public static synchronized void submitEvent(@NotNull CognitiveEvent event, long now) {
        if (!coordinatorEnabled) {
            // Direct legacy bypass
            if (event.isVoiceEnabled() && !event.narrationText().isBlank()) {
                narrationConsumer.accept(event.narrationText(), event.priority() == CognitivePriority.CRITICAL);
            }
            if (event.isSoundEnabled() && event.soundCue() != null) {
                audioConsumer.accept(event.soundCue());
            }
            return;
        }

        // 1. FAST-PATH for CRITICAL safety emergencies (0ms latency)
        if (event.priority() == CognitivePriority.CRITICAL) {
            handleCriticalFastPath(event, now);
            return;
        }

        // 2. Buffer collection for OPERATIONAL, CONTEXTUAL, PASSIVE
        tickBuffer.add(event);
    }

    private static void handleCriticalFastPath(CognitiveEvent event, long now) {
        criticalShieldUntil = now + 1500;
        attentionMemory.setLastDanger(event);

        DeduplicationKey key = new DeduplicationKey(
                event.domain(),
                event.semanticKey(),
                event.targetPos(),
                event.stateSignature(),
                event.priority()
        );

        Long lastEmitted = recentEvents.get(key);
        boolean isDuplicate = lastEmitted != null && (now - lastEmitted) < deduplicationWindowMs;

        // S1: Audio debounce for critical duplicates on edge
        if (event.isSoundEnabled() && event.soundCue() != null) {
            if (!isDuplicate) {
                audioConsumer.accept(event.soundCue());
            }
        }

        if (event.isVoiceEnabled() && !event.narrationText().isBlank()) {
            if (!isDuplicate) {
                recentEvents.put(key, now);
                if (criticalCountInTick == 0) {
                    // First critical in tick: interrupt background noise immediately
                    narrationConsumer.accept(event.narrationText(), true);
                } else {
                    // Concurrent second critical in same tick: enqueue without truncating the first
                    narrationConsumer.accept(event.narrationText(), false);
                }
                criticalCountInTick++;
            }
        }
    }

    private void onClientTick(Minecraft client, LocalPlayer player, ClientLevel level) {
        flushTick(System.currentTimeMillis());
    }

    /**
     * Flush and arbitrate collected events at the end of the client tick.
     * Exposed for testing with arbitrary timestamps.
     */
    public static synchronized void flushTick(long now) {
        criticalCountInTick = 0;

        if (tickBuffer.isEmpty() && shortQueue.isEmpty()) {
            return;
        }

        // Step 1: Collect candidates from shortQueue and current tick buffer (skipping SILENT events)
        List<CognitiveEvent> candidates = new ArrayList<>();

        while (!shortQueue.isEmpty()) {
            CognitiveEvent queued = shortQueue.poll();
            if (!queued.isExpired(now) && queued.outputType() != CognitiveEvent.OutputType.SILENT) {
                candidates.add(queued);
            }
        }

        for (CognitiveEvent event : tickBuffer) {
            if (!event.isExpired(now) && event.outputType() != CognitiveEvent.OutputType.SILENT) {
                candidates.add(event);
            }
        }
        tickBuffer.clear();

        if (candidates.isEmpty()) {
            return;
        }

        // Step 2: Critical shield check (CRITICAL silences ALL non-critical events)
        if (now < criticalShieldUntil) {
            for (CognitiveEvent event : candidates) {
                // If OPERATIONAL, preserve in shortQueue with TTL to be spoken once shield expires
                if (event.priority() == CognitivePriority.OPERATIONAL && !event.isExpired(now)) {
                    if (shortQueue.size() < 4) {
                        shortQueue.offer(event);
                    }
                }
                // PASSIVE and CONTEXTUAL are dropped during critical safety emergency
            }
            return;
        }

        // Step 3: Direct interaction shield check (S2: OPERATIONAL and max 1 CONTEXTUAL deferred, PASSIVE dropped)
        boolean directShieldActive = DirectInteractionShield.isActive();
        if (directShieldActive) {
            boolean contextualStored = false;
            for (CognitiveEvent event : candidates) {
                if (event.priority() == CognitivePriority.OPERATIONAL && !event.isExpired(now)) {
                    if (shortQueue.size() < 4) {
                        shortQueue.offer(event);
                    }
                } else if (event.priority() == CognitivePriority.CONTEXTUAL && !event.isExpired(now) && !contextualStored) {
                    if (shortQueue.size() < 4) {
                        shortQueue.offer(event);
                        contextualStored = true;
                    }
                }
                // PASSIVE is dropped as transient/obsolete
            }
            return;
        }

        // Step 4: High-fidelity deduplication filter
        List<CognitiveEvent> filtered = new ArrayList<>();
        for (CognitiveEvent event : candidates) {
            DeduplicationKey key = new DeduplicationKey(
                    event.domain(),
                    event.semanticKey(),
                    event.targetPos(),
                    event.stateSignature(),
                    event.priority()
            );

            Long lastEmitted = recentEvents.get(key);
            if (lastEmitted != null && (now - lastEmitted) < deduplicationWindowMs) {
                continue;
            }

            filtered.add(event);
        }

        if (filtered.isEmpty()) {
            return;
        }

        // Step 5: Sort candidates by priority descending, then timestamp
        filtered.sort(Comparator
                .comparing((CognitiveEvent e) -> e.priority().getRank()).reversed()
                .thenComparing(CognitiveEvent::timestamp));

        CognitiveEvent primary = filtered.get(0);
        CognitiveEvent secondary = filtered.size() > 1 ? filtered.get(1) : null;

        // Step 6: Evaluate chained narration (Voice checks, I18N Template & Spatial Compatibility)
        if (chainedNarrationEnabled && secondary != null && canChainEvents(primary, secondary)) {
            String templateKey = getJoinTemplateKey(primary.domain(), secondary.domain());

            if (templateKey != null) {
                boolean pVoice = primary.isVoiceEnabled() && !primary.narrationText().isBlank();
                boolean sVoice = secondary.isVoiceEnabled() && !secondary.narrationText().isBlank();

                String combinedText = null;
                if (pVoice && sVoice) {
                    combinedText = templateResolver.resolve(templateKey, primary.narrationText(), secondary.narrationText());
                }

                // F1-1: If both had voice but combinedText could not be resolved by template, DO NOT fuse!
                if (pVoice && sVoice && (combinedText == null || combinedText.isBlank())) {
                    // Chaining rejected due to missing template resolution -> proceed to unchained path
                } else {
                    // Execute authorized chaining
                    if (combinedText != null && !combinedText.isBlank()) {
                        boolean interrupt = primary.priority() == CognitivePriority.OPERATIONAL;
                        narrationConsumer.accept(combinedText, interrupt);
                    } else if (pVoice) {
                        boolean interrupt = primary.priority() == CognitivePriority.OPERATIONAL;
                        narrationConsumer.accept(primary.narrationText(), interrupt);
                    } else if (sVoice) {
                        narrationConsumer.accept(secondary.narrationText(), false);
                    }

                    if (primary.isSoundEnabled() && primary.soundCue() != null) {
                        audioConsumer.accept(primary.soundCue());
                    }
                    if (secondary.isSoundEnabled() && secondary.soundCue() != null) {
                        audioConsumer.accept(secondary.soundCue());
                    }

                    registerEventEmitted(primary, now);
                    registerEventEmitted(secondary, now);

                    updateAttentionMemory(primary);
                    updateAttentionMemory(secondary);
                    return;
                }
            }
        }

        // Unchained emission: emit primary
        boolean interrupt = primary.priority() == CognitivePriority.OPERATIONAL;

        if (primary.isVoiceEnabled() && !primary.narrationText().isBlank()) {
            narrationConsumer.accept(primary.narrationText(), interrupt);
        }

        if (primary.isSoundEnabled() && primary.soundCue() != null) {
            audioConsumer.accept(primary.soundCue());
        }

        registerEventEmitted(primary, now);
        updateAttentionMemory(primary);

        // If secondary is valid: preserve in shortQueue for next tick
        if (secondary != null && !secondary.isExpired(now) && secondary.outputType() != CognitiveEvent.OutputType.SILENT) {
            boolean shouldQueue = (secondary.priority() == CognitivePriority.OPERATIONAL || secondary.priority() == CognitivePriority.CONTEXTUAL)
                    || (chainedNarrationEnabled && canChainEvents(primary, secondary)); // F1-1: preserve compatible PASSIVE secondary on chaining fallback
            if (shouldQueue && shortQueue.size() < 4) {
                shortQueue.offer(secondary);
            }
        }
    }

    /**
     * Verifies chaining compatibility: flags, spatial compatibility, and template availability.
     */
    private static boolean canChainEvents(CognitiveEvent primary, CognitiveEvent secondary) {
        if (!primary.canChain() || !secondary.canChain()) {
            return false;
        }

        // Verify spatial compatibility
        if (!primary.isSpatiallyCompatible(secondary)) {
            return false;
        }

        // Verify that at least one output channel is active
        boolean anyOutput = (primary.isVoiceEnabled() || primary.isSoundEnabled())
                && (secondary.isVoiceEnabled() || secondary.isSoundEnabled());
        if (!anyOutput) {
            return false;
        }

        // Must have an explicit I18N join template key
        return getJoinTemplateKey(primary.domain(), secondary.domain()) != null;
    }

    /**
     * Map domain pairs to explicit I18N template keys.
     * Returns null if no localized pairing rule exists (preventing arbitrary concatenation).
     */
    public static @Nullable String getJoinTemplateKey(SourceDomain primaryDomain, SourceDomain secondaryDomain) {
        if (primaryDomain == SourceDomain.SAFETY && secondaryDomain == SourceDomain.EXPLORATION) {
            return "minecraft_access.cognitive.join_safety_exploration";
        }
        if (primaryDomain == SourceDomain.MOVEMENT && secondaryDomain == SourceDomain.SAFETY) {
            return "minecraft_access.cognitive.join_movement_safety";
        }
        if (primaryDomain == SourceDomain.MOVEMENT && secondaryDomain == SourceDomain.EXPLORATION) {
            return "minecraft_access.cognitive.join_movement_exploration";
        }
        return null;
    }

    private static @Nullable String defaultResolveTemplate(String templateKey, String first, String second) {
        try {
            String translated = I18n.get(templateKey, first.trim(), second.trim());
            if (!translated.equals(templateKey)) {
                return translated;
            }
        } catch (Exception ignored) {
        }
        // F1-1: Return null if template is not loaded or missing. NO hardcoded fusion!
        return null;
    }

    private static void registerEventEmitted(CognitiveEvent event, long now) {
        DeduplicationKey key = new DeduplicationKey(
                event.domain(),
                event.semanticKey(),
                event.targetPos(),
                event.stateSignature(),
                event.priority()
        );
        recentEvents.put(key, now);
    }

    private static void updateAttentionMemory(CognitiveEvent event) {
        if (event.domain() == SourceDomain.SAFETY && event.priority() == CognitivePriority.CRITICAL) {
            attentionMemory.setLastDanger(event);
        } else if (event.domain() == SourceDomain.EXPLORATION) {
            attentionMemory.setLastTarget(event);
        }
    }

    /**
     * Clear all pending buffers, short queues, attention memory and shields.
     * Invoked on world change, dimension change, death, respawn or server disconnect.
     */
    public static synchronized void clearAllBuffers() {
        tickBuffer.clear();
        shortQueue.clear();
        recentEvents.clear();
        attentionMemory.clear();
        DirectInteractionShield.reset();
        criticalShieldUntil = 0;
        criticalCountInTick = 0;
    }

    /**
     * Reset output delegates to production defaults.
     */
    public static void resetDelegates() {
        narrationConsumer = MainClass::narrate;
        audioConsumer = CognitiveCoordinator::defaultPlaySound;
        templateResolver = CognitiveCoordinator::defaultResolveTemplate;
    }

    private static void defaultPlaySound(SoundCue cue) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && cue.soundEvent() != null) {
                BlockPos pos = cue.position() != null ? cue.position() : (client.player != null ? client.player.blockPosition() : BlockPos.ZERO);
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
            log.warn("Error playing sound cue in CognitiveCoordinator: {}", e.getMessage());
        }
    }
}
