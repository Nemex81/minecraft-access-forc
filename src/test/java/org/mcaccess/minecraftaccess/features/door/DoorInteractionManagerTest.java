package org.mcaccess.minecraftaccess.features.door;

import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Rev MC-26.13 Door & Gateway Interaction Manager Tests (Headless 0 ms)")
class DoorInteractionManagerTest {

    @BeforeAll
    static void initBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void tearDown() {
        DoorInteractionManager.resetClock();
        DoorInteractionManager.clearSessions();
    }

    @Test
    @DisplayName("1. isInteractableClosedDoorOrGate recognizes closed vs open wooden doors")
    void testWoodenDoorRecognition() {
        var closedDoor = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, false);
        var openDoor = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, true);

        assertTrue(DoorInteractionHelper.isInteractableClosedDoorOrGate(closedDoor, true), "Closed oak door must be recognized as interactable");
        assertFalse(DoorInteractionHelper.isInteractableClosedDoorOrGate(openDoor, true), "Open oak door must not be marked closed");

        assertTrue(DoorInteractionHelper.isInteractableOpenDoorOrGate(openDoor), "Open oak door must be interactable");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(closedDoor), "Closed oak door must not be marked open");
    }

    @Test
    @DisplayName("2. Iron doors and iron trapdoors are strictly excluded from hand interaction")
    void testIronDoorsAndTrapdoorsExcluded() {
        var closedIronDoor = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, false);
        var openIronDoor = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, true);
        var closedIronTrapdoor = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false);
        var openIronTrapdoor = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);

        assertTrue(DoorInteractionHelper.isIronDoorOrTrapdoor(closedIronDoor));
        assertTrue(DoorInteractionHelper.isIronDoorOrTrapdoor(openIronDoor));
        assertTrue(DoorInteractionHelper.isIronDoorOrTrapdoor(closedIronTrapdoor));
        assertTrue(DoorInteractionHelper.isIronDoorOrTrapdoor(openIronTrapdoor));

        assertFalse(DoorInteractionHelper.isInteractableClosedDoorOrGate(closedIronDoor, true), "Closed iron door must NOT be hand-interactable");
        assertFalse(DoorInteractionHelper.isInteractableClosedDoorOrGate(closedIronTrapdoor, true), "Closed iron trapdoor must NOT be hand-interactable");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(openIronDoor), "Open iron door must NOT be hand-interactable");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(openIronTrapdoor), "Open iron trapdoor must NOT be hand-interactable");
    }

    @Test
    @DisplayName("3. includeGatesAndTrapdoors flag respects user configuration")
    void testIncludeGatesAndTrapdoorsFlag() {
        var closedGate = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN, false);
        var closedTrapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false);

        assertTrue(DoorInteractionHelper.isInteractableClosedDoorOrGate(closedGate, true), "Fence gate must be recognized when enabled");
        assertFalse(DoorInteractionHelper.isInteractableClosedDoorOrGate(closedGate, false), "Fence gate must be ignored when disabled");

        assertTrue(DoorInteractionHelper.isInteractableClosedDoorOrGate(closedTrapdoor, true), "Trapdoor must be recognized when enabled");
        assertFalse(DoorInteractionHelper.isInteractableClosedDoorOrGate(closedTrapdoor, false), "Trapdoor must be ignored when disabled");
    }

    @Test
    @DisplayName("4. isPlayerAcrossDoor: Crossing threshold into opposite half-space at >= 0.90m")
    void testPlayerAcrossDoorGeometricDetection() {
        BlockPos doorPos = new BlockPos(10, 64, 10);
        Vec3 entryPos = new Vec3(10.5, 64.0, 8.5); // Approached from North, walking South

        // Case A: Still approaching in front of door
        Vec3 posApproaching = new Vec3(10.5, 64.0, 9.5);
        assertFalse(DoorInteractionHelper.isPlayerAcrossDoor(entryPos, posApproaching, doorPos, Direction.SOUTH),
                "Player in front of door has not crossed");

        // Case B: Directly on the door threshold (inside doorway <= 0.65m)
        Vec3 posThreshold = new Vec3(10.5, 64.0, 10.5);
        assertFalse(DoorInteractionHelper.isPlayerAcrossDoor(entryPos, posThreshold, doorPos, Direction.SOUTH),
                "Player on the threshold is inside swing arc");
        assertTrue(DoorInteractionHelper.isPlayerInsideDoorWay(posThreshold, doorPos),
                "Player on threshold must be recognized inside doorway for passage renewal");

        // Case C: Safely crossed to the opposite side (South side, distance >= 0.90m)
        Vec3 posOppositeSafe = new Vec3(10.5, 64.0, 11.5); // dist = 1.0m, across
        assertTrue(DoorInteractionHelper.isPlayerAcrossDoor(entryPos, posOppositeSafe, doorPos, Direction.SOUTH),
                "Player at 1.0m on opposite side must be recognized as across");

        // Case D: Retreated backwards
        Vec3 posRetreated = new Vec3(10.5, 64.0, 7.0); // dist = 3.5m, but backwards
        assertFalse(DoorInteractionHelper.isPlayerAcrossDoor(entryPos, posRetreated, doorPos, Direction.SOUTH),
                "Player retreating backwards must not trigger auto-close");

        // Case E: Vertical out of reach
        Vec3 posHighAbove = new Vec3(10.5, 70.0, 11.5);
        assertFalse(DoorInteractionHelper.isPlayerAcrossDoor(entryPos, posHighAbove, doorPos, Direction.SOUTH),
                "Player vertically out of bounds must return false");
    }

    @Test
    @DisplayName("5. findCeilingTrapdoorAboveHead detects overhead trapdoors on ladders and stairs")
    void testFindCeilingTrapdoorAboveHead() {
        BlockGetter level = mock(BlockGetter.class);
        when(level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());

        BlockPos feetPos = new BlockPos(5, 64, 5);
        BlockPos trapdoorPos = feetPos.above(2); // at Y = 66

        var closedTrapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false);
        when(level.getBlockState(trapdoorPos)).thenReturn(closedTrapdoor);
        when(level.getBlockState(feetPos.above(1))).thenReturn(Blocks.AIR.defaultBlockState());

        BlockPos detected = DoorInteractionHelper.findCeilingTrapdoorAboveHead(level, feetPos, Direction.NORTH);
        assertEquals(trapdoorPos, detected, "Ceiling trapdoor 2 blocks above must be detected");

        // Iron trapdoor must be ignored
        var ironTrapdoor = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false);
        when(level.getBlockState(trapdoorPos)).thenReturn(ironTrapdoor);
        assertNull(DoorInteractionHelper.findCeilingTrapdoorAboveHead(level, feetPos, Direction.NORTH),
                "Iron trapdoor above head must not be auto-opened");
    }

    @Test
    @DisplayName("6. isPlayerSafelyOutsideTrapdoorShaft requires being above hole and XZ distance >= 0.85m")
    void testPlayerSafelyOutsideTrapdoorShaft() {
        BlockPos trapdoorPos = new BlockPos(5, 70, 5);

        // Climbing inside the shaft
        Vec3 insideShaft = new Vec3(5.5, 68.5, 5.5);
        assertFalse(DoorInteractionHelper.isPlayerSafelyOutsideTrapdoorShaft(insideShaft, trapdoorPos),
                "Inside shaft must return false");

        // Directly above the 1x1 hole on upper floor (stepping on air / open hole)
        Vec3 aboveHole = new Vec3(5.5, 70.5, 5.5);
        assertFalse(DoorInteractionHelper.isPlayerSafelyOutsideTrapdoorShaft(aboveHole, trapdoorPos),
                "Standing directly above the open hole must not close the trapdoor under feet");

        // Safely stepped onto adjacent solid floor
        Vec3 safeOnFloor = new Vec3(6.5, 70.5, 5.5); // dx = 1.0 >= 0.85m
        assertTrue(DoorInteractionHelper.isPlayerSafelyOutsideTrapdoorShaft(safeOnFloor, trapdoorPos),
                "Stepping 1.0m away on upper floor must be recognized as safely outside shaft");
    }

    @Test
    @DisplayName("7. Watchdog timeout fail-safe at 0 ms virtual time seam (6000 ms)")
    void testWatchdogTimeoutFailsafe() {
        AtomicLong virtualTime = new AtomicLong(1000L);
        DoorInteractionManager.setClockForTest(virtualTime::get);

        BlockPos doorPos = new BlockPos(20, 64, 20);
        Vec3 entryPos = new Vec3(20.5, 64.0, 18.5);

        DoorInteractionManager.registerSession(doorPos, entryPos, Direction.SOUTH, virtualTime.get());
        assertEquals(1, DoorInteractionManager.getActiveDoorSessions().size(), "Session must be registered");

        // Advance 3000 ms (less than 6000 ms timeout)
        virtualTime.addAndGet(3000L);
        assertEquals(1, DoorInteractionManager.getActiveDoorSessions().size(), "Session must stay active within timeout");

        // Advance past 6000 ms (total dt = 7000 ms)
        virtualTime.addAndGet(4000L); // now at 8000L
        long now = virtualTime.get();

        // Check timeout evaluation logic
        var session = DoorInteractionManager.getActiveDoorSessions().get(doorPos);
        assertNotNull(session);
        assertTrue((now - session.openedTime()) > DoorInteractionManager.WATCHDOG_TIMEOUT_MS, "Elapsed time must exceed watchdog threshold");
    }

    @Test
    @DisplayName("8. createBlockHit produces centered, deterministic BlockHitResult")
    void testCreateBlockHit() {
        BlockPos pos = new BlockPos(3, 64, -7);
        BlockHitResult hit = DoorInteractionHelper.createBlockHit(pos, Direction.UP);

        assertNotNull(hit);
        assertEquals(pos, hit.getBlockPos());
        assertEquals(Direction.UP, hit.getDirection());
        assertEquals(new Vec3(3.5, 64.5, -6.5), hit.getLocation());
        assertFalse(hit.isInside());
    }
}
