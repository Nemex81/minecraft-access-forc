package org.mcaccess.minecraftaccess.features.door;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Rev MC-26.12 Permissive Door & Gate Interaction Tests")
class DoorInteractionHelperTest {

    @BeforeAll
    static void initBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("1. isInteractableOpenDoorOrGate correctly recognizes open vs closed wooden doors")
    void testWoodenDoorRecognition() {
        var openDoor = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, true);
        var closedDoor = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, false);

        assertTrue(DoorInteractionHelper.isInteractableOpenDoorOrGate(openDoor), "Open oak door must be interactable");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(closedDoor), "Closed oak door must return false");
    }

    @Test
    @DisplayName("2. Iron doors and iron trapdoors are strictly excluded (cannot be opened by hand)")
    void testIronDoorsAndTrapdoorsExcluded() {
        var openIronDoor = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, true);
        var closedIronDoor = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, false);
        var openIronTrapdoor = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);
        var closedIronTrapdoor = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false);

        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(openIronDoor), "Open iron door must NOT be hand-interactable");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(closedIronDoor), "Closed iron door must NOT be hand-interactable");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(openIronTrapdoor), "Open iron trapdoor must NOT be hand-interactable");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(closedIronTrapdoor), "Closed iron trapdoor must NOT be hand-interactable");
    }

    @Test
    @DisplayName("3. Fence gates and wooden trapdoors correctly recognized when open")
    void testFenceGatesAndTrapdoors() {
        var openGate = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN, true);
        var closedGate = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN, false);
        var openTrapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);
        var closedTrapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false);

        assertTrue(DoorInteractionHelper.isInteractableOpenDoorOrGate(openGate), "Open fence gate must be recognized");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(closedGate), "Closed fence gate must return false");
        assertTrue(DoorInteractionHelper.isInteractableOpenDoorOrGate(openTrapdoor), "Open oak trapdoor must be recognized");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(closedTrapdoor), "Closed oak trapdoor must return false");
    }

    @Test
    @DisplayName("4. Non-door blocks, air, and null return false")
    void testNonDoorBlocks() {
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(null), "Null state must return false");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(Blocks.AIR.defaultBlockState()), "Air must return false");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(Blocks.STONE.defaultBlockState()), "Stone must return false");
        assertFalse(DoorInteractionHelper.isInteractableOpenDoorOrGate(Blocks.DIRT.defaultBlockState()), "Dirt must return false");
    }

    @Test
    @DisplayName("5. resolvePermissiveDoorHit returns null on null client or components")
    void testNullGuards() {
        assertNull(DoorInteractionHelper.resolvePermissiveDoorHit(null));

        Minecraft client = mock(Minecraft.class);
        client.player = null;
        client.level = null;
        assertNull(DoorInteractionHelper.resolvePermissiveDoorHit(client));
    }

    @Test
    @DisplayName("6. Never overrides if vanilla hitResult is an entity")
    void testEntityHitBypass() {
        Minecraft client = mock(Minecraft.class);
        LocalPlayer player = mock(LocalPlayer.class);
        ClientLevel level = mock(ClientLevel.class);
        Entity entity = mock(Entity.class);

        client.player = player;
        client.level = level;
        client.hitResult = new EntityHitResult(entity);

        assertNull(DoorInteractionHelper.resolvePermissiveDoorHit(client),
                "Should never override if player is targeting an entity");
    }

    @Test
    @DisplayName("7. Never overrides if vanilla hitResult already directly hits an open door")
    void testAlreadyTargetingDoorBypass() {
        Minecraft client = mock(Minecraft.class);
        LocalPlayer player = mock(LocalPlayer.class);
        ClientLevel level = mock(ClientLevel.class);

        BlockPos doorPos = new BlockPos(10, 64, 10);
        var openDoor = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, true);
        when(level.getBlockState(doorPos)).thenReturn(openDoor);

        client.player = player;
        client.level = level;
        client.hitResult = new BlockHitResult(new Vec3(10.5, 64.5, 10.5), Direction.NORTH, doorPos, false);

        assertNull(DoorInteractionHelper.resolvePermissiveDoorHit(client),
                "Should return null if vanilla hitResult already directly hit the open door");
    }
}
