package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.WorldNarrator;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder.PathResult;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder.PathStatus;
import org.mcaccess.minecraftaccess.features.point_of_interest.BlockPos3d;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;

/**
 * Route Navigator (Level 2 - Route Domain Mind).
 * Detentore esclusivo dello stato della rotta, dell'avanzamento nodi,
 * dei ricalcoli dinamici su bersagli mobili e della geometria nello spazio voxel.
 */
@Slf4j
public class RouteNavigator {
    @Getter
    private @Nullable Object targetObject = null;

    @Getter
    private @Nullable BlockPos currentGoalPos = null;

    private List<BlockPos> currentPath = List.of();

    @Getter
    private int currentPathIndex = 0;

    @Getter
    private Vec3 startPosContinuous = Vec3.ZERO;

    @Getter
    private @Nullable BlockPos rootBlockPos = null;

    @Getter
    private boolean firstSegmentPending = false;

    @Getter
    private long routeRevisionId = 0L;

    /**
     * Segnala il completamento del primo segmento e del disimpegno iniziale.
     */
    public void completeFirstSegment() {
        this.firstSegmentPending = false;
    }

    /**
     * Restituisce la sequenza di nodi della rotta in forma strutturalmente immutabile.
     */
    public List<BlockPos> getCurrentPath() {
        return currentPath;
    }

    /**
     * Installa una nuova rotta conservando la radice e posizionando il cursore operativo su path.get(1).
     */
    void installRoute(PathResult result, Vec3 playerPos) {
        this.currentPath = List.copyOf(result.path());
        this.currentGoalPos = result.targetGoalPos();
        this.startPosContinuous = playerPos;
        this.rootBlockPos = !this.currentPath.isEmpty() ? this.currentPath.get(0) : null;
        this.routeRevisionId++;

        if (result.status() == PathStatus.FOUND && this.currentPath.size() >= 2) {
            this.currentPathIndex = 1;
            this.firstSegmentPending = true;
        } else {
            this.currentPathIndex = 0;
            this.firstSegmentPending = false;
        }
    }

    /**
     * Avvia una nuova rotta calcolando il percorso A* verso il bersaglio.
     */
    public PathResult startRoute(Level level, Vec3 playerPos, Object target, int maxRange) {
        this.targetObject = target;
        PathResult result = AutoWalkPathfinder.findPath(level, playerPos, target, maxRange);

        switch (result.status()) {
            case FOUND, ALREADY_AT_TARGET -> {
                installRoute(result, playerPos);
            }
            case OUT_OF_RANGE, NO_PATH, SEARCH_BUDGET_EXHAUSTED -> {
                clearRoute();
            }
        }
        return result;
    }

    /**
     * Ricalcola la rotta verso il bersaglio corrente dalla posizione specificata.
     */
    public PathResult repath(Level level, Vec3 playerPos, int maxRange) {
        if (targetObject == null || !isTargetValid(targetObject)) {
            clearRoute();
            return PathResult.noPath();
        }
        PathResult result = AutoWalkPathfinder.findPath(level, playerPos, targetObject, maxRange);
        switch (result.status()) {
            case FOUND, ALREADY_AT_TARGET -> {
                installRoute(result, playerPos);
            }
            case NO_PATH, OUT_OF_RANGE, SEARCH_BUDGET_EXHAUSTED -> {
                clearRoute();
            }
        }
        return result;
    }

    /**
     * Verifica pura: determina se l'entità bersaglio si è spostata
     * di oltre 2.0 blocchi quadratici (distSqr > 4.0) rispetto alla meta corrente.
     */
    public boolean shouldRepathForEntity() {
        if (targetObject instanceof Entity entity) {
            return currentGoalPos != null && entity.blockPosition().distSqr(currentGoalPos) > 4.0;
        }
        return false;
    }

    /**
     * Avanza l'indice al prossimo nodo della rotta se non è già completata.
     * @return true se l'indice è avanzato con successo.
     */
    public boolean advanceWaypoint() {
        if (firstSegmentPending) {
            firstSegmentPending = false;
        }
        if (currentPathIndex < currentPath.size()) {
            currentPathIndex++;
            return true;
        }
        return false;
    }

    /**
     * Verifica se la rotta è attiva (ha nodi e non è ancora terminata).
     */
    public boolean hasActiveRoute() {
        return !currentPath.isEmpty() && currentPathIndex < currentPath.size();
    }

    /**
     * Verifica se tutti i nodi sono stati superati o se la rotta è vuota.
     */
    public boolean isRouteCompleted() {
        return currentPath.isEmpty() || currentPathIndex >= currentPath.size();
    }

    /**
     * Restituisce la posizione del nodo attualmente da raggiungere, oppure null se la rotta è vuota/terminata.
     */
    public @Nullable BlockPos getCurrentNodePos() {
        if (currentPathIndex < currentPath.size()) {
            return currentPath.get(currentPathIndex);
        }
        return null;
    }

    /**
     * Restituisce il numero di passi/nodi rimanenti fino al traguardo.
     */
    public int getRemainingSteps() {
        return Math.max(0, currentPath.size() - currentPathIndex);
    }

    /**
     * Calcola la distanza euclidea rimanente lungo la rotta a partire dalla posizione del giocatore.
     */
    public double getRemainingDistance(Vec3 playerPos) {
        if (!hasActiveRoute()) return 0.0;
        double total = 0.0;
        Vec3 prev = playerPos;
        for (int i = currentPathIndex; i < currentPath.size(); i++) {
            Vec3 nodeCenter = Vec3.atBottomCenterOf(currentPath.get(i));
            total += prev.distanceTo(nodeCenter);
            prev = nodeCenter;
        }
        return total;
    }

    /**
     * Verifica se il giocatore si trova a meno di 2 blocchi quadratici dal traguardo finale (distSqr <= 2.0).
     */
    public boolean isAtFinalGoal(Vec3 playerPos, Level level) {
        if (currentPath.isEmpty() || firstSegmentPending) return false;
        BlockPos targetNodePos = getCurrentNodePos();
        BlockPos rawTargetPos = (targetNodePos != null && level != null && AutoWalkPathfinder.isStandable(level, targetNodePos)) ? targetNodePos : currentGoalPos;
        if (rawTargetPos != null) {
            BlockPos playerBlockPos = BlockPos.containing(playerPos);
            double distSq = playerBlockPos.distSqr(rawTargetPos);
            return distSq <= 2.0 && currentPathIndex >= currentPath.size() - 1;
        }
        return false;
    }

    /**
     * Svuota completamente la rotta e azzera tutti gli indici e lo stato del segmento.
     */
    public void clearRoute() {
        this.targetObject = null;
        this.currentGoalPos = null;
        this.currentPath = List.of();
        this.currentPathIndex = 0;
        this.startPosContinuous = Vec3.ZERO;
        this.rootBlockPos = null;
        this.firstSegmentPending = false;
        this.routeRevisionId++;
    }

    /**
     * Imposta manualmente un percorso immutabile (ad uso test unitari headless o coordinate fisse).
     */
    public void setTestRoute(List<BlockPos> path, @Nullable BlockPos goalPos, @Nullable Object target) {
        this.currentPath = List.copyOf(path);
        this.currentGoalPos = goalPos;
        this.targetObject = target;
        this.startPosContinuous = path.isEmpty() ? Vec3.ZERO : Vec3.atBottomCenterOf(path.get(0));
        this.rootBlockPos = path.isEmpty() ? null : path.get(0);
        this.routeRevisionId++;
        if (path.size() >= 2) {
            this.currentPathIndex = 1;
            this.firstSegmentPending = true;
        } else {
            this.currentPathIndex = 0;
            this.firstSegmentPending = false;
        }
    }

    /**
     * Validatore dell'integrità del bersaglio (es. se entità è ancora viva).
     */
    public boolean isTargetValid(@Nullable Object target) {
        if (target == null) return false;
        return switch (target) {
            case Entity entity -> entity.isAlive();
            case BlockPos3d bp3d -> true;
            case BlockPos bp -> true;
            case Waypoint wp -> true;
            default -> false;
        };
    }

    /**
     * Restituisce il nome descrittivo del bersaglio.
     */
    public String getTargetName(@Nullable Object target) {
        if (target == null) return "";
        try {
            return switch (target) {
                case Entity entity -> {
                    WorldNarrator narrator = MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator);
                    yield narrator != null ? narrator.narrate(entity) : entity.getName().getString();
                }
                case BlockPos3d bp3d -> {
                    WorldNarrator narrator = MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator);
                    yield narrator != null ? narrator.narrate(bp3d) : bp3d.toString();
                }
                case BlockPos bp -> {
                    WorldNarrator narrator = MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator);
                    yield narrator != null ? narrator.narrate(bp) : bp.toShortString();
                }
                case Waypoint wp -> wp.name();
                default -> target.toString();
            };
        } catch (Exception e) {
            log.debug("Error narrating target name: ", e);
            return target.toString();
        }
    }
}
