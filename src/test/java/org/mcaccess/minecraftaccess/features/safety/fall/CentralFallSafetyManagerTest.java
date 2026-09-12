package org.mcaccess.minecraftaccess.features.safety.fall;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafetyMovementGuard;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CentralFallSafetyManager - Coordinamento e Quiete Reciproca")
class CentralFallSafetyManagerTest {

    private Minecraft client;
    private Player player;
    private Level level;
    private ProximityFallDetector mockProximity;
    private LongRangeFallDetector mockLongRange;
    private SafetyMovementGuard mockGuard;

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
        mockProximity = mock(ProximityFallDetector.class);
        mockLongRange = mock(LongRangeFallDetector.class);
        mockGuard = mock(SafetyMovementGuard.class);

        when(mockProximity.getMovementGuard()).thenReturn(mockGuard);

        testConfig = new Config.FallDetector();
        testConfig.enabled = true;
        testConfig.proximityEnabled = true;
        testConfig.longRangeEnabled = true;

        CentralFallSafetyManager.resetTestSeams();
    }

    @AfterEach
    void tearDown() {
        CentralFallSafetyManager.resetTestSeams();
    }

    @Test
    @DisplayName("1. Modulo Balm identificato con 'minecraft_access:fall_detector'")
    void testModuleId() {
        CentralFallSafetyManager manager = new CentralFallSafetyManager();
        assertEquals(Identifier.fromNamespaceAndPath("minecraft_access", "fall_detector"), manager.getId());
    }

    @Test
    @DisplayName("2. Se disabilitato da configurazione, resetta lo stato e non esegue scan")
    void testDisabledConfig() {
        testConfig.enabled = false;

        Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        CentralFallSafetyManager manager = new CentralFallSafetyManager(fixedClock, mockProximity, mockLongRange, () -> testConfig);

        manager.tick(client, player, level);

        verify(mockProximity).resetSafetyState();
        verify(mockProximity, never()).tick(any(), any(), any(), anyBoolean(), anyBoolean());
        verify(mockLongRange, never()).tick(any(), any(), any(), anyBoolean(), anyBoolean());

        testConfig.enabled = true;
    }

    @Test
    @DisplayName("3. Se il giocatore è in acqua, resetta lo stato e sospende le scansioni cadute")
    void testWaterState() {
        when(player.isInWater()).thenReturn(true);

        Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(10000), ZoneId.systemDefault());
        CentralFallSafetyManager manager = new CentralFallSafetyManager(fixedClock, mockProximity, mockLongRange, () -> testConfig);

        manager.tick(client, player, level);

        verify(mockProximity).resetSafetyState();
        verify(mockProximity, never()).tick(any(), any(), any(), anyBoolean(), anyBoolean());
        verify(mockLongRange, never()).tick(any(), any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("4. Allerta Proximity (Zona 1/2) attiva finestra di soppressione 2000ms sul Lungo Raggio")
    void testMutualQuietingWindow() {
        long t0 = 10000;
        TestClock clock = new TestClock(t0);
        CentralFallSafetyManager manager = new CentralFallSafetyManager(clock, mockProximity, mockLongRange, () -> testConfig);

        // Tick 1 a t0: Proximity rileva WARNING_ZONE_1
        when(mockProximity.tick(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(ProximityFallDetector.ProximityStatus.WARNING_ZONE_1);

        manager.tick(client, player, level);

        // Nel tick 1, la soppressione scatta per i successivi 2000ms (fino a t0 + 2000)
        verify(mockLongRange, times(1)).tick(eq(client), eq(player), eq(level), eq(true), eq(false));

        // Tick 2 a t0 + 1000ms: Proximity diventa CLEAR, ma siamo ancora dentro i 2000ms di quiete
        clock.setTime(t0 + 1000);
        when(mockProximity.tick(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(ProximityFallDetector.ProximityStatus.CLEAR);

        manager.tick(client, player, level);
        verify(mockLongRange, times(2)).tick(eq(client), eq(player), eq(level), eq(true), eq(false));

        // Tick 3 a t0 + 2500ms: Finestra scaduta, il lungo raggio non è più soppresso
        clock.setTime(t0 + 2500);
        manager.tick(client, player, level);
        verify(mockLongRange, times(1)).tick(eq(client), eq(player), eq(level), eq(false), eq(false));
    }

    @Test
    @DisplayName("5. In AutoWalk il lungo raggio è soppresso e la marcia comunica lo stato ad entrambi")
    void testAutoWalkSensoryQuieting() {
        long t0 = 10000;
        TestClock clock = new TestClock(t0);
        CentralFallSafetyManager manager = new CentralFallSafetyManager(clock, mockProximity, mockLongRange, () -> testConfig);

        CentralFallSafetyManager.setAutoWalkStateSupplier(() -> true);
        when(mockProximity.tick(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(ProximityFallDetector.ProximityStatus.CLEAR);

        manager.tick(client, player, level);

        verify(mockProximity).tick(eq(client), eq(player), eq(level), eq(true), anyBoolean());
        verify(mockLongRange).tick(eq(client), eq(player), eq(level), anyBoolean(), eq(true));
    }

    private static class TestClock extends Clock {
        private long millis;

        public TestClock(long millis) {
            this.millis = millis;
        }

        public void setTime(long millis) {
            this.millis = millis;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }
}
