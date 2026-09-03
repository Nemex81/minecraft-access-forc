package org.mcaccess.minecraftaccess.features;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mcaccess.minecraftaccess.features.crosshair.ObstacleNarrationContext;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Obstacle Narration Composer Pure Unit Tests (Fase 3B)")
class ObstacleNarrationComposerTest {

    @Test
    @DisplayName("1. Blank or empty message returns empty string")
    void testBlankMessageReturnsEmpty() {
        assertEquals("", ObstacleNarrationComposer.composeFinalNarration("", 1, null));
        assertEquals("", ObstacleNarrationComposer.composeFinalNarration("   ", 1, null));
        assertEquals("", ObstacleNarrationComposer.composeFinalNarration("", true, 1, null));
    }

    @Test
    @DisplayName("2. Frontal obstacle with distance 1 block produces singular distance string")
    void testFrontalObstacleSingleBlock() {
        String raw = "Davanti: gradino";
        String composed = ObstacleNarrationComposer.composeFinalNarration(raw, true, 1, ObstacleNarrationContext.EMPTY);
        assertEquals("Davanti: gradino, a 1 blocco", composed);
    }

    @Test
    @DisplayName("3. Frontal obstacle with distance > 1 blocks produces plural distance string")
    void testFrontalObstacleMultipleBlocks() {
        String raw = "Davanti: muro";
        String composed = ObstacleNarrationComposer.composeFinalNarration(raw, true, 3, ObstacleNarrationContext.EMPTY);
        assertEquals("Davanti: muro, a 3 blocchi", composed);
    }

    @Test
    @DisplayName("4. Frontal obstacle with distance <= 0 is safely clamped to 1 blocco")
    void testFrontalObstacleClampedDistance() {
        String raw = "Davanti: ostacolo";
        String composed0 = ObstacleNarrationComposer.composeFinalNarration(raw, true, 0, ObstacleNarrationContext.EMPTY);
        assertEquals("Davanti: ostacolo, a 1 blocco", composed0);

        String composedNeg = ObstacleNarrationComposer.composeFinalNarration(raw, true, -2, ObstacleNarrationContext.EMPTY);
        assertEquals("Davanti: ostacolo, a 1 blocco", composedNeg);
    }

    @Test
    @DisplayName("5. Lateral obstacle with crosshair context appends front target and distance")
    void testLateralObstacleWithCrosshairContext() {
        String raw = "Sinistra: muretto";
        ObstacleNarrationContext ctx = new ObstacleNarrationContext("Pietra", 4);
        String composed = ObstacleNarrationComposer.composeFinalNarration(raw, false, 2, ctx);
        assertEquals("Sinistra: muretto. Davanti: Pietra, a 4 blocchi", composed);
    }

    @Test
    @DisplayName("6. Lateral obstacle with crosshair context at 1 block uses singular distance")
    void testLateralObstacleWithSingleBlockCrosshair() {
        String raw = "Destra: tronco";
        ObstacleNarrationContext ctx = new ObstacleNarrationContext("Cassa", 1);
        String composed = ObstacleNarrationComposer.composeFinalNarration(raw, false, 1, ctx);
        assertEquals("Destra: tronco. Davanti: Cassa, a 1 blocco", composed);
    }

    @Test
    @DisplayName("7. Lateral obstacle with null or empty crosshair context falls back to raw message")
    void testLateralObstacleWithoutCrosshairContext() {
        String raw = "Dietro: gradino";
        assertEquals("Dietro: gradino", ObstacleNarrationComposer.composeFinalNarration(raw, false, 1, null));
        assertEquals("Dietro: gradino", ObstacleNarrationComposer.composeFinalNarration(raw, false, 1, ObstacleNarrationContext.EMPTY));
        assertEquals("Dietro: gradino", ObstacleNarrationComposer.composeFinalNarration(raw, false, 1, new ObstacleNarrationContext("   ", 2)));
    }

    @Test
    @DisplayName("8. Overload without explicit isFrontal detects front prefix automatically")
    void testOverloadAutoDetection() {
        String frontal = "Davanti: gradino";
        assertEquals("Davanti: gradino, a 2 blocchi", ObstacleNarrationComposer.composeFinalNarration(frontal, 2, ObstacleNarrationContext.EMPTY));

        String lateral = "Sinistra: gradino";
        ObstacleNarrationContext ctx = new ObstacleNarrationContext("Terra", 3);
        assertEquals("Sinistra: gradino. Davanti: Terra, a 3 blocchi", ObstacleNarrationComposer.composeFinalNarration(lateral, 1, ctx));
    }
}
