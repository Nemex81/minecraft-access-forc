package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbTraversal;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbTraversalAnalyzer;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbableGeometry;

/**
 * Climb Assistant Controller (Contratto D4 & D7).
 * Gestore tattico leggero per l'intercettazione dell'interazione, la selezione contestuale
 * della direzione di arrampicata (salita vs discesa) e l'installazione della rotta deterministica.
 */
@Slf4j
public final class ClimbAssistantController {

    private ClimbAssistantController() {
    }

    /**
     * Intercetta la pressione del tasto interazione (es. tasto destro o tasto di apertura porte).
     * @return true se l'interazione è stata intercettata per avviare il Climb Assistant.
     */
    public static boolean tryIntercept(@Nullable Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return false;
        }

        Config config = Config.getInstance();
        if (config == null || config.autoWalk == null
                || !config.autoWalk.climbAssistant.enabled
                || !config.autoWalk.climbAssistant.interactionKeyTrigger) {
            return false;
        }

        // Se il giocatore preme Shift (sneak), l'intercettazione è bypassata per consentire il piazzamento blocchi
        if (client.player.isShiftKeyDown()) {
            return false;
        }

        // Controllo mirino/crosshair raycast
        HitResult hit = client.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = blockHit.getBlockPos();
            double distSq = client.player.blockPosition().distSqr(hitPos);
            if (distSq <= 6.25) { // raggio max 2.5m
                if (ClimbableGeometry.isClimbable(client.level.getBlockState(hitPos))) {
                    return trigger(client, hitPos, blockHit.getDirection());
                }
            }
        }

        // Controllo vicini immediati nel raggio di 2.5m
        BlockPos feetPos = client.player.blockPosition();
        if (ClimbableGeometry.isClimbable(client.level.getBlockState(feetPos))) {
            return trigger(client, feetPos, null);
        }
        if (ClimbableGeometry.isClimbable(client.level.getBlockState(feetPos.above()))) {
            return trigger(client, feetPos.above(), null);
        }
        Direction facing = client.player.getDirection();
        BlockPos inFront = feetPos.relative(facing);
        if (ClimbableGeometry.isClimbable(client.level.getBlockState(inFront))) {
            return trigger(client, inFront, facing);
        }

        return false;
    }

    /**
     * Trigger manuale da tasto rapido (es. Alt+S o Access Menu).
     */
    public static boolean triggerFromKey() {
        Minecraft client = null;
        try {
            client = Minecraft.getInstance();
        } catch (Throwable ignored) {}
        return trigger(client, null, null);
    }

    /**
     * Risolve il contesto di arrampicata, calcola la rotta tattica e avvia la sessione.
     */
    public static boolean trigger(
            @Nullable Minecraft client,
            @Nullable BlockPos candidatePos,
            @Nullable Direction preferredFacing
    ) {
        if (client == null || client.player == null || client.level == null) {
            return false;
        }

        LocalPlayer player = client.player;
        Level level = client.level;

        // Se AutoWalk è già attivo, un nuovo trigger arresta la marcia
        MovementCoordinator coordinator = MovementCoordinator.getActiveInstance();
        if (coordinator != null && coordinator.isActive()) {
            coordinator.cancel(true, null);
            return true;
        }

        // 1. Individua il blocco arrampicabile di partenza
        BlockPos climbPos = candidatePos;
        if (climbPos == null) {
            climbPos = findNearestClimbable(level, player);
        }

        if (climbPos == null) {
            MainClass.narrate(I18n.get("minecraft_access.climb.not_facing_climbable"), true);
            return false;
        }

        // 2. Risoluzione euristica della direzione naturale (Salita vs Discesa)
        Direction.AxisDirection direction = resolveNaturalDirection(level, player, climbPos);

        // 3. Analisi geometrica pura con ClimbTraversalAnalyzer
        ClimbTraversal traversal = ClimbTraversalAnalyzer.analyze(level, climbPos, direction, preferredFacing, true);

        // Se la direzione naturale non offre un landing valido, tenta la direzione opposta
        if (traversal == null) {
            Direction.AxisDirection opposite = (direction == Direction.AxisDirection.POSITIVE)
                    ? Direction.AxisDirection.NEGATIVE
                    : Direction.AxisDirection.POSITIVE;
            traversal = ClimbTraversalAnalyzer.analyze(level, climbPos, opposite, preferredFacing, true);
        }

        if (traversal == null) {
            MainClass.narrate(I18n.get("minecraft_access.climb.no_safe_landing"), true);
            return false;
        }

        // 4. Costruzione della rotta deterministica con ClimbRouteAssembler (Contratti D0, D2)
        ClimbRouteAssembler.ClimbRoute climbRoute = ClimbRouteAssembler.assembleClimbRoute(player.blockPosition(), traversal);
        if (climbRoute == null || climbRoute.path().size() < 2) {
            MainClass.narrate(I18n.get("minecraft_access.climb.no_safe_landing"), true);
            return false;
        }

        // 5. Avvio della sessione tattica nel MovementCoordinator
        if (coordinator == null) {
            coordinator = new MovementCoordinator();
        }

        String targetDesc = traversal.isAscent()
                ? I18n.get("minecraft_access.climb.ascending")
                : I18n.get("minecraft_access.climb.descending");

        coordinator.startTacticalRoute(client, player, climbRoute.path(), climbRoute.segments(), climbRoute.landingPos(), targetDesc);
        return true;
    }

    private static @Nullable BlockPos findNearestClimbable(Level level, LocalPlayer player) {
        BlockPos feet = player.blockPosition();
        if (ClimbableGeometry.isClimbable(level.getBlockState(feet))) return feet;
        if (ClimbableGeometry.isClimbable(level.getBlockState(feet.above()))) return feet.above();
        if (ClimbableGeometry.isClimbable(level.getBlockState(feet.below()))) return feet.below();

        Direction facing = player.getDirection();
        BlockPos inFront = feet.relative(facing);
        if (ClimbableGeometry.isClimbable(level.getBlockState(inFront))) return inFront;
        if (ClimbableGeometry.isClimbable(level.getBlockState(inFront.above()))) return inFront.above();

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adj = feet.relative(dir);
            if (ClimbableGeometry.isClimbable(level.getBlockState(adj))) return adj;
        }
        return null;
    }

    private static Direction.AxisDirection resolveNaturalDirection(Level level, LocalPlayer player, BlockPos climbPos) {
        BlockPos feet = player.blockPosition();
        boolean onGround = player.onGround();
        boolean hasClimbAbove = ClimbableGeometry.isClimbable(level.getBlockState(climbPos.above()));
        boolean hasClimbBelow = ClimbableGeometry.isClimbable(level.getBlockState(climbPos.below()));

        // Regola 1: se il giocatore è alla base (onGround e climbable sopra): salita naturale
        if (onGround && hasClimbAbove && !hasClimbBelow) {
            return Direction.AxisDirection.POSITIVE;
        }

        // Regola 2: se il giocatore è in cima (onGround e climbable sotto): discesa naturale
        if (onGround && hasClimbBelow && !hasClimbAbove) {
            return Direction.AxisDirection.NEGATIVE;
        }

        // Regola 3: a metà colonna o in volo, risoluzione basata sul pitch dello sguardo
        float pitch = player.getXRot();
        return (pitch < 0) ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE;
    }
}
