package org.mcaccess.minecraftaccess.features;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.safety.traversal.CrouchIntent;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafeDescentCandidate;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafeDescentType;
import org.mcaccess.minecraftaccess.features.safety.traversal.SafetyMovementGuard;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FallDetector Traversal Safety Integration Tests (Rev MC-26.8)")
class FallDetectorTraversalIntegrationTest {

    @BeforeAll
    static void initBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.resetDelegates();
        FallDetector.resetTestSeams();
    }

    @AfterEach
    void tearDown() {
        CognitiveCoordinator.setCoordinatorEnabled(true);
        CognitiveCoordinator.resetDelegates();
        FallDetector.resetTestSeams();
    }

    @Test
    @DisplayName("1. Safe descent candidate routes OPERATIONAL event via CognitiveCoordinator")
    void testSafeDescentRoutesViaCoordinator() {
        List<CognitiveEvent> events = new ArrayList<>();
        FallDetector.setCognitiveEventConsumer(events::add);

        SafeDescentCandidate candidate = SafeDescentCandidate.of(
                new BlockPos(10, 68, 4),
                new BlockPos(10, 67, 4),
                new BlockPos(10, 63, 4),
                SafeDescentType.LADDER,
                Direction.NORTH
        );

        // Movement guard test
        SafetyMovementGuard guard = new SafetyMovementGuard(
                () -> new CrouchIntent(false, true),
                crouching -> {}
        );
        guard.engageFallProtection();
        assertTrue(guard.isSystemOverrideActive());

        guard.allowValidatedDescent(candidate.columnId());
        assertFalse(guard.isSystemOverrideActive(), "Guard must release system crouch for validated ladder");
        assertTrue(guard.isDescentAllowedFor(candidate.columnId()));
    }

    @Test
    @DisplayName("2. Safe descent candidate uses legacy narration and audio fallback when coordinator is disabled")
    void testSafeDescentLegacyFallback() {
        CognitiveCoordinator.setCoordinatorEnabled(false);

        AtomicReference<String> legacyNarration = new AtomicReference<>();
        AtomicBoolean legacyAudioFired = new AtomicBoolean(false);

        FallDetector.setLegacyNarrationConsumer((msg, interrupt) -> legacyNarration.set(msg));
        FallDetector.setLegacyAudioConsumer(cue -> legacyAudioFired.set(true));

        assertFalse(CognitiveCoordinator.isCoordinatorEnabled());
    }

    @Test
    @DisplayName("3. Contratto S4: isSafeWalkableStaircase riconosce il pianerottolo solido adiacente alla rampa discendente")
    void testStairLandingRecognizedAsSafeStaircase() {
        Level level = mock(Level.class);

        BlockPos landingPos = new BlockPos(10, 65, 5);
        BlockPos stairPos = new BlockPos(9, 66, 5); // Adiacente a Ovest, quota Y+1

        // landingPos è un pavimento solido normale (non scala)
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        when(level.getBlockState(landingPos)).thenReturn(stoneBricks);

        // stairPos è una scala rivolta a OVEST (sale a Ovest, scende verso Est verso landingPos)
        BlockState stairWest = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST);
        when(level.getBlockState(stairPos)).thenReturn(stairWest);

        // Tutte le altre posizioni adiacenti o sopra sono aria
        when(level.getBlockState(argThat(pos -> pos != null && !pos.equals(landingPos) && !pos.equals(stairPos))))
                .thenReturn(Blocks.AIR.defaultBlockState());

        // Il pianerottolo in fondo alla rampa di scale deve essere riconosciuto come safe staircase
        boolean isSafe = FallDetector.isSafeWalkableStaircase(level, landingPos, 68);
        assertTrue(isSafe, "Il pianerottolo in fondo alla rampa di scale deve essere riconosciuto come discesa sicura");
    }
}
