package org.mcaccess.minecraftaccess.features.door;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.mcaccess.minecraftaccess.utils.PlayerUtils;

/**
 * Helper class for permissive door, gate, and trapdoor interaction (Rev MC-26.12).
 * Bridges the gap between the micro-voxel accessible raymarch (which snaps to the whole block volume of doors)
 * and vanilla Minecraft interaction (which checks thin 3-pixel physical collision shapes).
 */
public final class DoorInteractionHelper {
    private DoorInteractionHelper() {}

    /**
     * Determines whether the given blockstate is an interactable, currently open door, fence gate, or trapdoor.
     * Excludes non-hand-operable blocks like iron doors and iron trapdoors.
     *
     * @param state The BlockState to examine.
     * @return true if the block is an open, hand-operable door, fence gate, or trapdoor; false otherwise.
     */
    public static boolean isInteractableOpenDoorOrGate(@Nullable BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        // Iron doors and iron trapdoors cannot be operated by hand with right-click
        if (state.is(Blocks.IRON_DOOR) || state.is(Blocks.IRON_TRAPDOOR)) {
            return false;
        }

        if (state.getBlock() instanceof DoorBlock doorBlock) {
            return doorBlock.isOpen(state);
        }

        if (state.getBlock() instanceof FenceGateBlock) {
            return state.hasProperty(BlockStateProperties.OPEN) && Boolean.TRUE.equals(state.getValue(BlockStateProperties.OPEN));
        }

        if (state.getBlock() instanceof TrapDoorBlock) {
            return state.hasProperty(BlockStateProperties.OPEN) && Boolean.TRUE.equals(state.getValue(BlockStateProperties.OPEN));
        }

        return false;
    }

    /**
     * Resolves a permissive BlockHitResult if the player is looking towards an open door, gate, or trapdoor
     * whose physical collision box is thin and easy to miss with vanilla raycasting.
     *
     * @param client The Minecraft client instance.
     * @return A BlockHitResult on the open door/gate if permissive snap applies, or null if vanilla hitResult should be kept.
     */
    public static @Nullable BlockHitResult resolvePermissiveDoorHit(@Nullable Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return null;
        }

        // Never override if vanilla hitResult is already an entity (e.g. player interacting with a mob or NPC)
        HitResult vanillaHit = client.hitResult;
        if (vanillaHit != null && vanillaHit.getType() == HitResult.Type.ENTITY) {
            return null;
        }

        // If vanilla hitResult already directly hit an open interactable door/gate (e.g. precise frame hit), let vanilla handle it
        if (vanillaHit instanceof BlockHitResult blockHit) {
            BlockState currentState = client.level.getBlockState(blockHit.getBlockPos());
            if (isInteractableOpenDoorOrGate(currentState)) {
                return null;
            }
        }

        // Query the accessible crosshair target, which uses micro-voxel raymarching for thin elements
        Player player = client.player;
        double reach = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
        HitResult accessibleHit = PlayerUtils.crosshairTarget(reach);

        if (accessibleHit instanceof BlockHitResult targetBlockHit) {
            BlockState targetState = client.level.getBlockState(targetBlockHit.getBlockPos());
            if (isInteractableOpenDoorOrGate(targetState)) {
                return targetBlockHit;
            }
        }

        return null;
    }
}
