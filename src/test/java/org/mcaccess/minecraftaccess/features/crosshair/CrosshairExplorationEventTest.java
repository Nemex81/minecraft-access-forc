package org.mcaccess.minecraftaccess.features.crosshair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;
import org.mcaccess.minecraftaccess.features.cognitive.SpatialDirection;
import org.mcaccess.minecraftaccess.features.cognitive.StateSignature;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Crosshair Exploration Event Factory, Deduplication & Routing Tests (Sotto-fase 4B)")
class CrosshairExplorationEventTest {

    private final List<String> capturedDirectNarration = new ArrayList<>();
    private final List<CognitiveEvent> capturedCognitiveEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CrosshairFeedbackManager.resetTestSeams();
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.setExplorationRoutingEnabled(true);
        CognitiveCoordinator.setDeduplicationWindowMs(1500);

        capturedDirectNarration.clear();
        capturedCognitiveEvents.clear();

        CrosshairFeedbackManager.setNarrationConsumer((text, interrupt) -> capturedDirectNarration.add(text));
        CrosshairFeedbackManager.setCognitiveEventConsumer(capturedCognitiveEvents::add);
    }

    @AfterEach
    void tearDown() {
        CrosshairFeedbackManager.resetTestSeams();
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
    }

    @Test
    @DisplayName("1. Factory produces well-formed CognitiveEvent with PASSIVE priority and canChain=false")
    void testFactoryEventContract() {
        BlockPos pos = new BlockPos(10, 64, 20);
        long now = 5000L;

        CognitiveEvent event = CrosshairExplorationEventFactory.createEvent(
                CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET,
                "minecraft:oak_log",
                pos,
                3.5,
                4,
                "Tronco di quercia",
                now
        );

        assertNotNull(event);
        assertEquals(SourceDomain.EXPLORATION, event.domain());
        assertEquals(CognitivePriority.PASSIVE, event.priority());
        assertEquals(CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET, event.semanticKey());
        assertEquals(SpatialDirection.FORWARD, event.direction());
        assertEquals("Tronco di quercia", event.narrationText());
        assertEquals(pos, event.targetPos());
        assertEquals(3.5, event.distance(), 0.001);
        assertEquals(4, event.stateSignature().distanceBucket());
        assertEquals("minecraft:oak_log", event.stateSignature().targetId());
        assertFalse(event.canChain(), "Crosshair automatic event must have canChain=false");
        assertEquals(CognitiveEvent.OutputType.VOICE_ONLY, event.outputType());
        assertFalse(event.isExpired(now + 1999));
        assertTrue(event.isExpired(now + 2001));
    }

    @Test
    @DisplayName("2. Factory returns null for blank or empty narration text")
    void testFactoryReturnsNullOnEmptyText() {
        CognitiveEvent event1 = CrosshairExplorationEventFactory.createEvent(
                CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET,
                "minecraft:air",
                null,
                0,
                0,
                "",
                1000L
        );
        assertNull(event1);

        CognitiveEvent event2 = CrosshairExplorationEventFactory.createEvent(
                CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET,
                "minecraft:air",
                null,
                0,
                0,
                "   ",
                1000L
        );
        assertNull(event2);
    }

    @Test
    @DisplayName("3. Distinct canonical IDs prevent false deduplication even with identical text")
    void testCanonicalIdPreventsFalseDeduplication() {
        BlockPos pos = new BlockPos(5, 64, 5);
        List<String> spoken = new ArrayList<>();
        CognitiveCoordinator.setNarrationConsumer((text, interrupt) -> spoken.add(text));

        long t0 = 1000L;
        // Event 1: block target (e.g. wall)
        CognitiveEvent blockEvent = CrosshairExplorationEventFactory.createEvent(
                CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET,
                "minecraft:cobblestone_wall",
                pos,
                2.0,
                2,
                "Muretto",
                t0
        );
        assertNotNull(blockEvent);
        CognitiveCoordinator.submitEvent(blockEvent, t0);
        CognitiveCoordinator.flushTick(t0);

        assertEquals(1, spoken.size());
        assertEquals("Muretto", spoken.get(0));

        // Event 2: entity target that produces same translated text (or different canonical ID) at same position
        CognitiveEvent entityEvent = CrosshairExplorationEventFactory.createEvent(
                CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET,
                "minecraft:armor_stand",
                pos,
                2.0,
                2,
                "Muretto",
                t0 + 100
        );
        assertNotNull(entityEvent);
        CognitiveCoordinator.submitEvent(entityEvent, t0 + 100);
        CognitiveCoordinator.flushTick(t0 + 100);

        assertEquals(2, spoken.size(), "Different canonical ID must NOT be deduplicated as identical");
    }

    @Test
    @DisplayName("4. Identical canonical ID and bucket are deduplicated within window")
    void testIdenticalCanonicalIdDeduplicated() {
        BlockPos pos = new BlockPos(5, 64, 5);
        List<String> spoken = new ArrayList<>();
        CognitiveCoordinator.setNarrationConsumer((text, interrupt) -> spoken.add(text));

        long t0 = 1000L;
        CognitiveEvent e1 = CrosshairExplorationEventFactory.createEvent(
                CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET,
                "minecraft:stone",
                pos,
                3.0,
                3,
                "Pietra",
                t0
        );
        assertNotNull(e1);
        CognitiveCoordinator.submitEvent(e1, t0);
        CognitiveCoordinator.flushTick(t0);
        assertEquals(1, spoken.size());

        // Identical within 1500ms deduplication window
        CognitiveEvent e2 = CrosshairExplorationEventFactory.createEvent(
                CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET,
                "minecraft:stone",
                pos,
                3.2,
                3,
                "Pietra",
                t0 + 500
        );
        assertNotNull(e2);
        CognitiveCoordinator.submitEvent(e2, t0 + 500);
        CognitiveCoordinator.flushTick(t0 + 500);
        assertEquals(1, spoken.size(), "Identical canonical ID and distance bucket must be deduplicated");

        // Distance bucket changed (from 3 to 1) -> must NOT be deduplicated
        CognitiveEvent e3 = CrosshairExplorationEventFactory.createEvent(
                CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET,
                "minecraft:stone",
                pos,
                1.1,
                1,
                "Pietra",
                t0 + 600
        );
        assertNotNull(e3);
        CognitiveCoordinator.submitEvent(e3, t0 + 600);
        CognitiveCoordinator.flushTick(t0 + 600);
        assertEquals(2, spoken.size(), "Distance progression bucket change must trigger new narration");
    }

    @Test
    @DisplayName("5. Exploration routing active: events routed through coordinator seam")
    void testExplorationRoutingActiveDispatchesToCoordinator() {
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.setExplorationRoutingEnabled(true);

        long now = 1000L;
        CrosshairFeedbackManager.setClock(() -> now);

        // Simulate target mutation event creation through factory
        CognitiveEvent event = CrosshairExplorationEventFactory.createEvent(
                CrosshairExplorationEventFactory.SEMANTIC_KEY_TARGET,
                "minecraft:iron_ore",
                new BlockPos(1, 2, 3),
                4.0,
                4,
                "Minerale di ferro",
                now
        );
        assertNotNull(event);
        CognitiveCoordinator.submitEvent(event, now);

        // Verify that coordinator received event
        assertEquals(0, capturedDirectNarration.size(), "Direct narration must NOT be called when routing active");

        // When coordinator flushes, speech is emitted cleanly
        List<String> output = new ArrayList<>();
        CognitiveCoordinator.setNarrationConsumer((text, interrupt) -> output.add(text));
        CognitiveCoordinator.flushTick(now);

        assertEquals(1, output.size());
        assertEquals("Minerale di ferro", output.get(0));
    }

    @Test
    @DisplayName("6. Exploration routing disabled: fallback to legacy direct narration")
    void testExplorationRoutingDisabledFallback() {
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.setExplorationRoutingEnabled(false);

        assertFalse(CognitiveCoordinator.isExplorationRoutingActive());
    }
}
