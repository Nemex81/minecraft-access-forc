package org.mcaccess.minecraftaccess.features.safety.fall;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.cognitive.SoundCue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LongRangeFallDetector - Radar Orografico a Lungo Raggio (7..24m)")
class LongRangeFallDetectorTest {

    private Minecraft client;
    private Player player;
    private Level level;
    private List<CognitiveEvent> emittedEvents;
    private List<SoundCue> emittedSounds;
    private Config.FallDetector testConfig;

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        client = mock(Minecraft.class);
        player = mock(Player.class);
        level = mock(Level.class);

        emittedEvents = new ArrayList<>();
        emittedSounds = new ArrayList<>();

        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.setAudioConsumer(emittedSounds::add);

        LongRangeFallDetector.resetTestSeams();
        LongRangeFallDetector.setCognitiveEventConsumer(emittedEvents::add);
        LongRangeFallDetector.setLegacyAudioConsumer(emittedSounds::add);

        testConfig = new Config.FallDetector();
        testConfig.enabled = true;
        testConfig.longRangeEnabled = true;
        testConfig.longRangeMinRange = 7;
        testConfig.longRangeMaxRange = 24;
        testConfig.longRangeDepthThreshold = 4;
        testConfig.longRangeScanInterval = 3500;
        testConfig.volume = 0.80f;
        testConfig.longRangeVolumeMultiplier = 0.80f;
    }

    @AfterEach
    void tearDown() {
        CognitiveCoordinator.clearAllBuffers();
        CognitiveCoordinator.resetDelegates();
        LongRangeFallDetector.resetTestSeams();
    }

    @Test
    @DisplayName("1. Routine validateAndNormalize vincola il Lungo Raggio in [7, 24] con Swap Guard")
    void testConfigNormalizationAndClamping() {
        Config.FallDetector cfg = new Config.FallDetector();

        // Inversione (Swap Guard)
        cfg.longRangeMinRange = 20;
        cfg.longRangeMaxRange = 10;
        cfg.validateAndNormalize();
        assertEquals(10, cfg.longRangeMinRange, "Min deve essere scambiato con Max");
        assertEquals(20, cfg.longRangeMaxRange, "Max deve essere scambiato con Min");

        // Clamping estremi
        cfg.longRangeMinRange = 2;
        cfg.longRangeMaxRange = 50;
        cfg.validateAndNormalize();
        assertEquals(7, cfg.longRangeMinRange, "Min deve essere clampato a 7");
        assertEquals(24, cfg.longRangeMaxRange, "Max deve essere clampato a 24");
    }

    @Test
    @DisplayName("2. In AutoWalk il lungo raggio è 100% zittito e non esegue scansioni")
    void testAutoWalkSensoryQuieting() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        LongRangeFallDetector detector = new LongRangeFallDetector(clock, testConfig);

        when(player.onGround()).thenReturn(true);
        when(player.blockPosition()).thenReturn(new BlockPos(0, 64, 0));

        // Tick con isAutoWalkActive = true
        detector.tick(client, player, level, false, true);

        // Nessun raycast o lettura di blocchi deve avvenire
        verify(level, never()).getBlockState(any());
        assertTrue(emittedEvents.isEmpty());
        assertTrue(emittedSounds.isEmpty());
    }

    @Test
    @DisplayName("3. Quando soppresso da Proximity (isSuppressedByProximity = true) non esegue scansioni")
    void testSuppressedByProximity() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        LongRangeFallDetector detector = new LongRangeFallDetector(clock, testConfig);

        when(player.onGround()).thenReturn(true);
        when(player.blockPosition()).thenReturn(new BlockPos(0, 64, 0));

        // Tick con isSuppressedByProximity = true
        detector.tick(client, player, level, true, false);

        verify(level, never()).getBlockState(any());
        assertTrue(emittedEvents.isEmpty());
        assertTrue(emittedSounds.isEmpty());
    }

    @Test
    @DisplayName("4. Scansione polare a 16 settori individua baratro a 10 blocchi ed emette NOTE_BLOCK_BELL")
    void test16SectorPolarScanFindsChasm() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        LongRangeFallDetector detector = new LongRangeFallDetector(clock, testConfig);

        BlockPos playerPos = new BlockPos(0, 64, 0);
        when(player.blockPosition()).thenReturn(playerPos);
        when(player.onGround()).thenReturn(true);

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        FluidState emptyFluid = Fluids.EMPTY.defaultFluidState();

        when(level.getFluidState(any())).thenReturn(emptyFluid);

        // Simuliamo un baratro a Est (X=10, Y=64, Z=0)
        // A Est, r=10 (angolo 0 rad, dirX=1, dirZ=0)
        // Per tutte le posizioni ordinarie: pavimento di pietra a Y=63, aria a Y=64 (piedi) e Y=65 (testa)
        when(level.getBlockState(any())).thenAnswer(invocation -> {
            BlockPos p = invocation.getArgument(0);
            if (p.getX() == 10 && p.getZ() == 0) {
                // A X=10, Z=0 c'è una voragine profonda da Y=64 fino a Y=55
                if (p.getY() >= 55) {
                    return air;
                }
                return stone;
            }
            // Altrove: pavimento solido a Y=63
            if (p.getY() <= 63) {
                return stone;
            }
            return air;
        });

        List<LongRangeFallDetector.LongRangePit> pits = detector.scanOrography(player, level, 10000);

        assertFalse(pits.isEmpty(), "La scansione deve rilevare la voragine a 10 blocchi");
        assertEquals(1, emittedEvents.size());
        CognitiveEvent event = emittedEvents.get(0);
        assertEquals(CognitivePriority.PASSIVE, event.priority(), "L'avviso di lungo raggio deve essere PASSIVE");
        assertNotNull(event.soundCue());
        assertEquals(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), event.soundCue().soundEvent(), "Il suono deve essere NOTE_BLOCK_DIDGERIDOO");
        assertEquals(0.8f, event.soundCue().pitch(), 0.01f, "Il pitch deve essere 0.8f per timbro tellurico");

        // Verifica PRAPI-4: Il cue sonoro viene inviato direttamente all'audio consumer
        assertEquals(1, emittedSounds.size(), "Il cue sonoro deve essere emesso direttamente all'audio consumer");
        assertEquals(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), emittedSounds.get(0).soundEvent());
        assertEquals(0.8f, emittedSounds.get(0).pitch(), 0.01f, "Il pitch deve essere 0.8f");
        assertTrue(emittedSounds.get(0).volume() >= 0.35f, "Il volume calcolato non deve essere inferiore al floor di sicurezza");
    }

    @Test
    @DisplayName("5. Occlusione a quota occhi (Y+1) arresta il raggio polare prima della voragine")
    void testOcclusionBreakAtHeadHeight() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        LongRangeFallDetector detector = new LongRangeFallDetector(clock, testConfig);

        BlockPos playerPos = new BlockPos(0, 64, 0);
        when(player.blockPosition()).thenReturn(playerPos);
        when(player.onGround()).thenReturn(true);

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        FluidState emptyFluid = Fluids.EMPTY.defaultFluidState();

        when(level.getFluidState(any())).thenReturn(emptyFluid);

        // Simuliamo un muro impenetrabile a X=7 a quota occhi (Y=65), e una voragine a X=11
        when(level.getBlockState(any())).thenAnswer(invocation -> {
            BlockPos p = invocation.getArgument(0);
            if (p.getX() == 7 && p.getZ() == 0 && p.getY() == 65) {
                return stone; // Muro a quota occhi che occlude la vista
            }
            if (p.getX() == 11 && p.getZ() == 0) {
                return air; // Voragine oltre il muro
            }
            if (p.getY() <= 63) {
                return stone;
            }
            return air;
        });

        List<LongRangeFallDetector.LongRangePit> pits = detector.scanOrography(player, level, 10000);

        // Il raggio verso Est (X>0, Z=0) deve interrompersi a X=7 e NON individuare la voragine a X=11
        boolean foundPitAt11 = pits.stream().anyMatch(pit -> pit.pos().getX() == 11 && pit.pos().getZ() == 0);
        assertFalse(foundPitAt11, "La voragine oltre il muro occluso a quota occhi non deve essere rilevata");
    }
}
