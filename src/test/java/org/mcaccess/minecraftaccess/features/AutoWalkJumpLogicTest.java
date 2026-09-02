package org.mcaccess.minecraftaccess.features;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoWalkJumpLogicTest {

    private boolean evaluateJumpCondition(
            boolean configAutoJump,
            double distH,
            boolean horizontalCollision,
            double deltaY,
            boolean onGround
    ) {
        boolean isApproachingStep = (distH <= 1.25 || horizontalCollision) && deltaY > 0.30 && deltaY <= 1.25;
        return configAutoJump && isApproachingStep && onGround;
    }

    @Test
    @DisplayName("Scenario 1: Happy Path - Approccio a blocco solido a 0.80m con autoJump attivo")
    void testStandardStepUpApproach() {
        assertTrue(evaluateJumpCondition(true, 0.80, false, 1.0, true));
    }

    @Test
    @DisplayName("Scenario 2: Approccio anticipato a 1.20m in corsa")
    void testEarlyApproachWhileSprinting() {
        assertTrue(evaluateJumpCondition(true, 1.20, false, 1.0, true));
    }

    @Test
    @DisplayName("Scenario 3: Contatto fisico per collisione diagonale con distH a 1.40m")
    void testDiagonalCollisionContact() {
        assertTrue(evaluateJumpCondition(true, 1.40, true, 1.0, true));
    }

    @Test
    @DisplayName("Scenario 4: Guardia Cloth Config - autoJump disattivato dall'utente")
    void testAutoJumpDisabled() {
        assertFalse(evaluateJumpCondition(false, 0.80, true, 1.0, true));
    }

    @Test
    @DisplayName("Scenario 5: Ostacolo troppo alto - muro di 2 blocchi (deltaY = 2.0m)")
    void testWallTooHigh() {
        assertFalse(evaluateJumpCondition(true, 0.80, true, 2.0, true));
    }

    @Test
    @DisplayName("Scenario 6: Terreno piano o dislivello nullo (deltaY = 0.0m)")
    void testFlatTerrain() {
        assertFalse(evaluateJumpCondition(true, 0.80, true, 0.0, true));
    }

    @Test
    @DisplayName("Scenario 7: In volo o a mezz'aria (onGround = false)")
    void testMidAir() {
        assertFalse(evaluateJumpCondition(true, 0.80, true, 1.0, false));
    }
}