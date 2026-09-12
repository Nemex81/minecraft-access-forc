package org.mcaccess.minecraftaccess.features.door;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

    /**
     * Determines whether the given blockstate is an iron door or iron trapdoor.
     */
    public static boolean isIronDoorOrTrapdoor(@Nullable BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        return state.is(Blocks.IRON_DOOR) || state.is(Blocks.IRON_TRAPDOOR);
    }

    /**
     * Determines whether the given blockstate is an interactable, currently closed door, fence gate, or trapdoor.
     * Excludes non-hand-operable blocks like iron doors and iron trapdoors.
     *
     * @param state The BlockState to examine.
     * @param includeGatesAndTrapdoors whether to include fence gates and trapdoors.
     * @return true if closed and interactable, false otherwise.
     */
    public static boolean isInteractableClosedDoorOrGate(@Nullable BlockState state, boolean includeGatesAndTrapdoors) {
        if (state == null || state.isAir()) {
            return false;
        }

        if (isIronDoorOrTrapdoor(state)) {
            return false;
        }

        if (state.getBlock() instanceof DoorBlock doorBlock) {
            return !doorBlock.isOpen(state);
        }

        if (includeGatesAndTrapdoors) {
            if (state.getBlock() instanceof FenceGateBlock) {
                return state.hasProperty(BlockStateProperties.OPEN) && Boolean.FALSE.equals(state.getValue(BlockStateProperties.OPEN));
            }

            if (state.getBlock() instanceof TrapDoorBlock) {
                return state.hasProperty(BlockStateProperties.OPEN) && Boolean.FALSE.equals(state.getValue(BlockStateProperties.OPEN));
            }
        }

        return false;
    }

    /**
     * Volume di sicurezza diegetico congelato per sonificazione 3D conforme ad ASTRALIS (0.7f - 0.8f).
     */
    public static final float DOOR_SOUND_VOLUME = 0.75f;

    /**
     * Verifica se il giocatore si trova nel vano della porta (raggio <= 0.65m in XZ dal centro del blocco canonico).
     * Utilizzato per il rinnovo dinamico del watchdog (Passage Renewal).
     */
    public static boolean isPlayerInsideDoorWay(@Nullable Vec3 currentPos, @Nullable BlockPos doorPos) {
        if (currentPos == null || doorPos == null) {
            return false;
        }
        if (Math.abs(currentPos.y - doorPos.getY()) > 2.5) {
            return false;
        }
        Vec3 doorCenter = Vec3.atCenterOf(doorPos);
        double dx = currentPos.x - doorCenter.x;
        double dz = currentPos.z - doorCenter.z;
        return (dx * dx + dz * dz) <= 0.65 * 0.65;
    }

    /**
     * Checks if the player has crossed the door threshold into the opposite half-space
     * and has entered the adjacent block (distance >= 0.90m from the canonical door center in XZ plane).
     *
     * @param entryPos The player position when the door was approached/opened.
     * @param currentPos The current player position.
     * @param doorPos The block position of the door.
     * @param entryDirection Optional movement direction of the player when approaching.
     * @return true if the player has safely crossed into the adjacent space beyond the door.
     */
    public static boolean isPlayerAcrossDoor(
            @Nullable Vec3 entryPos,
            @Nullable Vec3 currentPos,
            @Nullable BlockPos doorPos,
            @Nullable Direction entryDirection
    ) {
        if (entryPos == null || currentPos == null || doorPos == null) {
            return false;
        }
        if (Math.abs(currentPos.y - doorPos.getY()) > 2.5) {
            return false;
        }
        Vec3 doorCenter = Vec3.atCenterOf(doorPos);
        double currX = currentPos.x - doorCenter.x;
        double currZ = currentPos.z - doorCenter.z;

        double distSq = currX * currX + currZ * currZ;
        // La porta in Minecraft non esce dal proprio blocco 1x1.
        // A distanza >= 0.90m il giocatore ha liberato completamente il blocco porta (raggio 0.5 + 0.3 hitbox = 0.8m)
        // ed è stabilmente nel blocco adiacente (interasse 1.0m).
        if (distSq < 0.90 * 0.90) {
            return false;
        }

        // 1. Proiezione sul vettore di ingresso effettivo del giocatore (doorCenter - entryPos)
        double inX = doorCenter.x - entryPos.x;
        double inZ = doorCenter.z - entryPos.z;
        double inLen = Math.sqrt(inX * inX + inZ * inZ);
        if (inLen > 0.15) {
            double dot = inX * currX + inZ * currZ;
            // Se dot > 0 il giocatore si trova nel semispazio oltre la porta rispetto al punto di ingresso
            if (dot > 0) {
                return true;
            }
        }

        // 2. Proiezione sulla direzione di marcia del giocatore all'approccio
        if (entryDirection != null && entryDirection.getAxis().isHorizontal()) {
            double projMove = currX * entryDirection.getStepX() + currZ * entryDirection.getStepZ();
            if (projMove >= 0.70) {
                return true;
            }
        }

        return false;
    }

    /**
     * Riproduce il suono diegetico 3D nativo di chiusura porta/cancello/botola
     * alle coordinate esatte del blocco (OpenAL HRTF posizionale alle spalle del giocatore).
     */
    public static void playDoorCloseSound(@Nullable Level level, @Nullable BlockPos pos, @Nullable BlockState state) {
        if (level == null || pos == null || state == null) {
            return;
        }
        SoundEvent sound = null;
        if (state.getBlock() instanceof DoorBlock doorBlock) {
            BlockSetType type = doorBlock.type();
            sound = type != null ? type.doorClose() : SoundEvents.WOODEN_DOOR_CLOSE;
        } else if (state.getBlock() instanceof TrapDoorBlock) {
            sound = SoundEvents.WOODEN_TRAPDOOR_CLOSE;
        } else if (state.getBlock() instanceof FenceGateBlock) {
            sound = SoundEvents.FENCE_GATE_CLOSE;
        }
        if (sound != null) {
            level.playLocalSound(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    sound,
                    SoundSource.BLOCKS,
                    DOOR_SOUND_VOLUME,
                    level.getRandom().nextFloat() * 0.1F + 0.9F,
                    false
            );
        }
    }

    /**
     * Searches for a closed trapdoor directly above or diagonally above the player's head
     * along ladders, vines, or stairs.
     */
    public static @Nullable BlockPos findCeilingTrapdoorAboveHead(@Nullable BlockGetter level, @Nullable BlockPos feetPos, @Nullable Direction moveDirection) {
        if (level == null || feetPos == null) {
            return null;
        }
        // Direct overhead check (ladder shafts, scaffolding, or straight-up ascent)
        for (int dy = 1; dy <= 2; dy++) {
            BlockPos checkPos = feetPos.above(dy);
            BlockState state = level.getBlockState(checkPos);
            if (state != null && state.getBlock() instanceof TrapDoorBlock && isInteractableClosedDoorOrGate(state, true)) {
                return checkPos;
            }
        }

        // Forward overhead check (stairs leading up through a trapdoor opening)
        if (moveDirection != null && moveDirection.getAxis().isHorizontal()) {
            for (int dy = 1; dy <= 2; dy++) {
                BlockPos checkPos = feetPos.relative(moveDirection).above(dy);
                BlockState state = level.getBlockState(checkPos);
                if (state != null && state.getBlock() instanceof TrapDoorBlock && isInteractableClosedDoorOrGate(state, true)) {
                    return checkPos;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the player has fully climbed through a trapdoor shaft and is safely standing
     * on the solid floor adjacent to the 1x1 opening (XZ distance >= 0.85m and Y >= trapdoor Y).
     */
    public static boolean isPlayerSafelyOutsideTrapdoorShaft(@Nullable Vec3 currentPos, @Nullable BlockPos trapdoorPos) {
        if (currentPos == null || trapdoorPos == null) {
            return false;
        }
        if (currentPos.y < trapdoorPos.getY()) {
            return false;
        }
        double dx = currentPos.x - (trapdoorPos.getX() + 0.5);
        double dz = currentPos.z - (trapdoorPos.getZ() + 0.5);
        double distSq = dx * dx + dz * dz;
        return distSq >= 0.85 * 0.85;
    }

    /**
     * Creates a deterministic BlockHitResult aimed at the center of the target block.
     */
    public static BlockHitResult createBlockHit(BlockPos pos, @Nullable Direction side) {
        Vec3 center = Vec3.atCenterOf(pos);
        return new BlockHitResult(center, side != null ? side : Direction.UP, pos, false);
    }
}

