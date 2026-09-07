package org.mcaccess.minecraftaccess.features.door;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

/**
 * Gestore FSM a Doppio Flusso per l'interazione automatica con porte, cancelli e botole (Rev MC-26.13).
 * Gestisce l'AutoOpen e l'AutoClose nel movimento manuale (WASD) a Zero Disorientamento della Visuale
 * e fornisce i servizi di interazione per la navigazione guidata (AutoWalk).
 */
public class DoorInteractionManager implements BalmClientModule {

    public record DoorPassageSession(
            BlockPos doorPos,
            Vec3 entryPos,
            @Nullable Direction entryDirection,
            long openedTime
    ) {
        public DoorPassageSession withOpenedTime(long newTime) {
            return new DoorPassageSession(doorPos, entryPos, entryDirection, newTime);
        }
    }

    public record TrapdoorPassageSession(
            BlockPos trapdoorPos,
            long openedTime
    ) {
        public TrapdoorPassageSession withOpenedTime(long newTime) {
            return new TrapdoorPassageSession(trapdoorPos, newTime);
        }
    }

    private static final Map<BlockPos, DoorPassageSession> activeDoorSessions = new ConcurrentHashMap<>();
    private static final Map<BlockPos, TrapdoorPassageSession> activeTrapdoorSessions = new ConcurrentHashMap<>();

    private static LongSupplier clock = System::currentTimeMillis;
    public static final long WATCHDOG_TIMEOUT_MS = 6000L;

    // Cooldown per prevenire oscillazioni o doppi click ravvicinati sullo stesso blocco
    private static final Map<BlockPos, Long> recentInteractions = new ConcurrentHashMap<>();
    private static final long INTERACTION_COOLDOWN_MS = 500L;

    public static void setClockForTest(LongSupplier testClock) {
        clock = testClock;
    }

    public static void resetClock() {
        clock = System::currentTimeMillis;
    }

    public static void clearSessions() {
        activeDoorSessions.clear();
        activeTrapdoorSessions.clear();
        recentInteractions.clear();
    }

    public static Map<BlockPos, DoorPassageSession> getActiveDoorSessions() {
        return activeDoorSessions;
    }

    public static void registerSession(BlockPos doorPos, Vec3 entryPos, @Nullable Direction entryDirection, long openedTime) {
        activeDoorSessions.put(doorPos, new DoorPassageSession(doorPos, entryPos, entryDirection, openedTime));
    }

    public static Map<BlockPos, TrapdoorPassageSession> getActiveTrapdoorSessions() {
        return activeTrapdoorSessions;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "door_interaction_manager");
    }

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register((client, player, level) -> {
            if (player instanceof LocalPlayer localPlayer) {
                tick(client, localPlayer, level);
            }
        });
    }

    public void tick(@Nullable Minecraft client, @Nullable LocalPlayer player, @Nullable Level level) {
        if (client == null || player == null || level == null) {
            return;
        }

        Config config = Config.getInstance();
        if (config == null || config.doorInteraction == null) {
            return;
        }

        boolean autoOpen = config.doorInteraction.autoOpenDoors;
        boolean autoClose = config.doorInteraction.autoCloseDoors;
        boolean includeGatesAndTrapdoors = config.doorInteraction.includeGatesAndTrapdoors;
        boolean narration = config.doorInteraction.doorNarration;

        long now = clock.getAsLong();

        // 1. Elaborazione sessioni attive di chiusura automatica
        if (autoClose) {
            processActiveDoorSessions(client, player, level, now, narration);
            if (includeGatesAndTrapdoors) {
                processActiveTrapdoorSessions(client, player, level, now, narration);
            }
        } else {
            activeDoorSessions.clear();
            activeTrapdoorSessions.clear();
        }

        // Pulizia interazioni recenti più vecchie di 5 secondi
        recentInteractions.entrySet().removeIf(entry -> now - entry.getValue() > 5000L);

        // 2. Auto-apertura di prossimità per movimento manuale (quando AutoWalk non è attivo)
        boolean isAutoWalkActive = MainClass.autoWalkManager != null
                && MainClass.autoWalkManager.getMovementCoordinator() != null
                && MainClass.autoWalkManager.getMovementCoordinator().isActive();

        if (autoOpen && !isAutoWalkActive) {
            processManualAutoOpen(client, player, level, now, autoClose, includeGatesAndTrapdoors, narration);
        }
    }

    private void processActiveDoorSessions(Minecraft client, LocalPlayer player, Level level, long now, boolean narration) {
        Iterator<Map.Entry<BlockPos, DoorPassageSession>> it = activeDoorSessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, DoorPassageSession> entry = it.next();
            BlockPos doorPos = entry.getKey();
            DoorPassageSession session = entry.getValue();

            // Passage Renewal: se il giocatore si trova attivamente nel vano della porta (raggio <= 0.65m),
            // il timer viene rinnovato per evitare scadenze premature durante il transito naturale
            if (DoorInteractionHelper.isPlayerInsideDoorWay(player.position(), doorPos)) {
                entry.setValue(session.withOpenedTime(now));
                continue;
            }

            // Watchdog Timeout (Fail-safe per esitazione prolungata o retromarcia dell'utente)
            if (now - session.openedTime() > WATCHDOG_TIMEOUT_MS) {
                it.remove();
                continue;
            }

            // Verifica se il giocatore ha completato l'attraversamento entrando nel blocco adiacente (dist >= 0.90m)
            if (DoorInteractionHelper.isPlayerAcrossDoor(session.entryPos(), player.position(), doorPos, session.entryDirection())) {
                BlockState state = level.getBlockState(doorPos);
                if (DoorInteractionHelper.isInteractableOpenDoorOrGate(state)) {
                    interactWithDoor(client, doorPos);
                    DoorInteractionHelper.playDoorCloseSound(level, doorPos, state);
                    if (narration) {
                        MainClass.narrate(I18n.get("minecraft_access.door.auto_closed"), true);
                    }
                }
                it.remove();
            }
        }
    }

    private void processActiveTrapdoorSessions(Minecraft client, LocalPlayer player, Level level, long now, boolean narration) {
        Iterator<Map.Entry<BlockPos, TrapdoorPassageSession>> it = activeTrapdoorSessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, TrapdoorPassageSession> entry = it.next();
            BlockPos trapdoorPos = entry.getKey();
            TrapdoorPassageSession session = entry.getValue();

            // Passage Renewal: se il giocatore è ancora nella tromba della scala sotto la botola, rinnova il timer
            if (player.getY() < trapdoorPos.getY() + 0.2) {
                entry.setValue(session.withOpenedTime(now));
                continue;
            }

            // Watchdog Timeout
            if (now - session.openedTime() > WATCHDOG_TIMEOUT_MS) {
                it.remove();
                continue;
            }

            // Verifica se il giocatore è emerso sul pavimento solido fuori dal foro 1x1
            if (DoorInteractionHelper.isPlayerSafelyOutsideTrapdoorShaft(player.position(), trapdoorPos)) {
                BlockState state = level.getBlockState(trapdoorPos);
                if (DoorInteractionHelper.isInteractableOpenDoorOrGate(state)) {
                    interactWithDoor(client, trapdoorPos);
                    DoorInteractionHelper.playDoorCloseSound(level, trapdoorPos, state);
                    if (narration) {
                        MainClass.narrate(I18n.get("minecraft_access.door.auto_closed_trapdoor"), true);
                    }
                }
                it.remove();
            }
        }
    }

    private void processManualAutoOpen(
            Minecraft client,
            LocalPlayer player,
            Level level,
            long now,
            boolean autoClose,
            boolean includeGatesAndTrapdoors,
            boolean narration
    ) {
        Direction facing = player.getDirection();

        // 1. Controllo botola a soffitto (arrampicata su scala a pioli o salita rampe)
        if (includeGatesAndTrapdoors) {
            boolean isAscending = player.onClimbable()
                    || player.getDeltaMovement().y > 0.05
                    || (client.options != null && (client.options.keyJump.isDown() || client.options.keyUp.isDown()));

            if (isAscending) {
                BlockPos ceilingTrapdoor = DoorInteractionHelper.findCeilingTrapdoorAboveHead(level, player.blockPosition(), facing);
                if (ceilingTrapdoor != null && canInteract(ceilingTrapdoor, now)) {
                    interactWithDoor(client, ceilingTrapdoor);
                    recentInteractions.put(ceilingTrapdoor, now);
                    if (narration) {
                        MainClass.narrate(I18n.get("minecraft_access.door.auto_opening"), true);
                    }
                    if (autoClose) {
                        activeTrapdoorSessions.put(ceilingTrapdoor, new TrapdoorPassageSession(ceilingTrapdoor, now));
                    }
                    return;
                }
            }
        }

        // 2. Controllo porta o cancello orizzontale davanti al giocatore
        boolean isMovingForward = (client.options != null && client.options.keyUp.isDown())
                || player.getDeltaMovement().horizontalDistanceSqr() > 0.001;

        if (isMovingForward) {
            BlockPos doorPos = findClosedDoorInFront(level, player, facing, includeGatesAndTrapdoors);
            if (doorPos != null && canInteract(doorPos, now)) {
                interactWithDoor(client, doorPos);
                recentInteractions.put(doorPos, now);
                if (narration) {
                    MainClass.narrate(I18n.get("minecraft_access.door.auto_opening"), true);
                }
                if (autoClose) {
                    activeDoorSessions.put(doorPos, new DoorPassageSession(doorPos, player.position(), facing, now));
                }
            }
        }
    }

    public static @Nullable BlockPos findClosedDoorInFront(
            @Nullable Level level,
            @Nullable Player player,
            @Nullable Direction facing,
            boolean includeGatesAndTrapdoors
    ) {
        if (level == null || player == null || facing == null) {
            return null;
        }

        BlockPos feet = player.blockPosition();
        BlockPos inFrontFeet = feet.relative(facing);
        BlockPos inFrontEye = inFrontFeet.above();

        BlockPos[] rawCandidates = new BlockPos[] {
                inFrontFeet,
                inFrontEye,
                feet
        };

        for (BlockPos rawCandidate : rawCandidates) {
            // Normalizzazione canonica: porta sempre riferita al blocco inferiore LOWER
            BlockPos candidate = AutoWalkPathfinder.getCanonicalDoorPos(level, rawCandidate);
            BlockState state = level.getBlockState(candidate);
            if (DoorInteractionHelper.isInteractableClosedDoorOrGate(state, includeGatesAndTrapdoors)) {
                Vec3 center = Vec3.atCenterOf(candidate);
                double dx = center.x - player.getX();
                double dz = center.z - player.getZ();
                if (dx * dx + dz * dz <= 2.25 * 2.25) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private static boolean canInteract(BlockPos pos, long now) {
        Long lastTime = recentInteractions.get(pos);
        return lastTime == null || (now - lastTime >= INTERACTION_COOLDOWN_MS);
    }

    public static void interactWithDoor(@Nullable Minecraft client, @Nullable BlockPos pos) {
        if (client == null || client.gameMode == null || client.player == null || client.level == null || pos == null) {
            return;
        }
        BlockHitResult hitResult = DoorInteractionHelper.createBlockHit(pos, Direction.UP);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hitResult);
        client.player.swing(InteractionHand.MAIN_HAND);
    }
}
