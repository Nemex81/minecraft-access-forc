package org.mcaccess.minecraftaccess.features.autowalk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbTraversal;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbEntryTransition;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbTraversalAnalyzer;
import org.mcaccess.minecraftaccess.features.safety.traversal.ClimbableGeometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modulo dedicato per la costruzione, normalizzazione e validazione topologica delle rotte di scalata verticale (Contratto D2).
 * Unifica la generazione della sequenza tipizzata (MOUNT -> sequenza TRANSIT per-rung -> DISMOUNT)
 * per ClimbAssistantController e AutoWalkPathfinder.
 */
public final class ClimbRouteAssembler {

    private ClimbRouteAssembler() {}

    /**
     * Rappresentazione immutabile di una rotta di scalata assemblata e validata.
     */
    public record ClimbRoute(
            @NotNull List<BlockPos> path,
            @NotNull List<RouteSegment> segments,
            @NotNull BlockPos landingPos,
            @NotNull ClimbTraversal traversal
    ) {}

    /**
     * Assembla una rotta di scalata deterministica a partire dalla posizione del giocatore e dal traversal validato.
     *
     * @param playerPos Posizione corrente dei piedi del giocatore.
     * @param traversal Dati geometrici validati della colonna arrampicabile.
     * @return ClimbRoute validata, oppure null se la rotta viola i vincoli topologici del Contratto D0.
     */
    public static @Nullable ClimbRoute assembleClimbRoute(@NotNull BlockPos playerPos, @NotNull ClimbTraversal traversal) {
        List<BlockPos> path = new ArrayList<>();
        List<RouteSegment> segments = new ArrayList<>();

        boolean isAscent = traversal.isAscent();
        BlockPos entryPos = traversal.entryPos();
        BlockPos colTop = traversal.columnTopPos();
        BlockPos colBottom = traversal.columnBottomPos();
        int colX = colTop.getX();
        int colZ = colTop.getZ();
        ClimbEntryTransition entryTransition = createEntryTransition(playerPos, traversal);

        // 1. Fase MOUNT: avvicinamento o aggancio iniziale alla colonna
        if (playerPos.getX() == colX && playerPos.getZ() == colZ) {
            // Giocatore gia' all'interno del prisma orizzontale della colonna
            path.add(playerPos);
        } else {
            // Giocatore adiacente: crea segmento atomico MOUNT verso entryPos
            int distH = Math.max(Math.abs(playerPos.getX() - entryPos.getX()), Math.abs(playerPos.getZ() - entryPos.getZ()));
            int distY = Math.abs(playerPos.getY() - entryPos.getY());
            if (distH <= 1 && distY <= 1) {
                path.add(playerPos);
                path.add(entryPos);
                segments.add(RouteSegment.climb(playerPos, entryPos, RouteSegment.ClimbLeg.MOUNT, traversal, entryTransition));
            } else {
                // Distanza eccessiva per un MOUNT atomico diretto
                return null;
            }
        }

        // 2. Fase TRANSIT: sequenza ordinata di nodi verticali per ogni piolo (deltaY = 1 o -1)
        int startY = isAscent ? colBottom.getY() : colTop.getY();
        int endY = isAscent ? colTop.getY() : colBottom.getY();
        int stepY = isAscent ? 1 : -1;

        // Se il giocatore era gia' nella colonna a una quota diversa da startY, sincronizza l'inizio transit
        BlockPos currentLast = path.get(path.size() - 1);
        if (currentLast.getX() == colX && currentLast.getZ() == colZ) {
            int currentY = currentLast.getY();
            if (isAscent && currentY >= startY && currentY <= endY) {
                startY = currentY;
            } else if (!isAscent && currentY <= startY && currentY >= endY) {
                startY = currentY;
            }
        }

        for (int y = startY; isAscent ? (y <= endY) : (y >= endY); y += stepY) {
            BlockPos rungPos = new BlockPos(colX, y, colZ);
            BlockPos lastNode = path.get(path.size() - 1);
            if (!lastNode.equals(rungPos)) {
                // Verifica adiacenza verticale atomica (|deltaY| == 1, deltaX == 0, deltaZ == 0)
                if (Math.abs(rungPos.getY() - lastNode.getY()) == 1 && rungPos.getX() == lastNode.getX() && rungPos.getZ() == lastNode.getZ()) {
                    path.add(rungPos);
                    segments.add(RouteSegment.climb(lastNode, rungPos, RouteSegment.ClimbLeg.TRANSIT, traversal));
                } else if (lastNode.equals(playerPos) && segments.isEmpty()) {
                    // Primo aggancio mount interno
                    path.add(rungPos);
                    segments.add(RouteSegment.climb(lastNode, rungPos, RouteSegment.ClimbLeg.MOUNT, traversal));
                } else {
                    // Salto non atomico nella colonna
                    return null;
                }
            }
        }

        // 3. Fase DISMOUNT: uscita continua verso landingPos
        BlockPos landingPos = traversal.landingPos();
        BlockPos lastTransitNode = path.get(path.size() - 1);
        if (!lastTransitNode.equals(landingPos)) {
            path.add(landingPos);
            segments.add(RouteSegment.climb(lastTransitNode, landingPos, RouteSegment.ClimbLeg.DISMOUNT, traversal));
        }

        // 4. Validazione topologica della rotta assemblata
        if (!validateClimbRoute(path, segments, traversal)) {
            return null;
        }

        return new ClimbRoute(
                Collections.unmodifiableList(path),
                Collections.unmodifiableList(segments),
                landingPos,
                traversal
        );
    }

    private static @Nullable ClimbEntryTransition createEntryTransition(
            @NotNull BlockPos playerPos,
            @NotNull ClimbTraversal traversal
    ) {
        if (!traversal.isDescent()) {
            return null;
        }
        int dx = traversal.entryPos().getX() - playerPos.getX();
        int dz = traversal.entryPos().getZ() - playerPos.getZ();
        if (Math.abs(dx) + Math.abs(dz) != 1) {
            return null;
        }
        Direction approach = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST
                : dz > 0 ? Direction.SOUTH : Direction.NORTH;
        BlockPos aperture = playerPos.relative(approach);
        return ClimbEntryTransition.ofTopDescent(
                playerPos,
                aperture,
                traversal.entryPos(),
                approach,
                traversal,
                ClimbEntryTransition.PassageRequirement.CLEAR,
                traversal.trapdoorPos()
        );
    }

    /**
     * Valida la conformita' topologica della rotta di scalata rispetto ai contratti D0 e D2.
     */
    public static boolean validateClimbRoute(
            @NotNull List<BlockPos> path,
            @NotNull List<RouteSegment> segments,
            @NotNull ClimbTraversal traversal
    ) {
        if (path.size() < 2 || segments.size() != path.size() - 1) {
            return false;
        }

        boolean hasTransit = false;
        boolean sawDismount = false;

        for (int i = 0; i < segments.size(); i++) {
            RouteSegment seg = segments.get(i);
            if (!seg.isClimb() || seg.climbLeg() == null || seg.climbData() == null) {
                return false;
            }
            if (!seg.climbData().columnId().equals(traversal.columnId())) {
                return false;
            }

            // Verifica coerenza tra segment e nodi del path
            if (!seg.from().equals(path.get(i)) || !seg.to().equals(path.get(i + 1))) {
                return false;
            }

            switch (seg.climbLeg()) {
                case MOUNT -> {
                    if (hasTransit || sawDismount) {
                        return false; // Mount puo' avvenire solo prima di Transit e Dismount
                    }
                }
                case TRANSIT -> {
                    if (sawDismount) {
                        return false; // Transit non puo' seguire Dismount
                    }
                    hasTransit = true;
                    // Transit deve essere rigorosamente verticale atomico lungo la colonna
                    if (seg.from().getX() != seg.to().getX() || seg.from().getZ() != seg.to().getZ()) {
                        return false;
                    }
                    if (Math.abs(seg.to().getY() - seg.from().getY()) != 1) {
                        return false;
                    }
                }
                case DISMOUNT -> {
                    sawDismount = true;
                }
            }
        }

        // Una scalata valida deve contenere almeno un segmento TRANSIT
        return hasTransit;
    }

    /**
     * Normalizza i segmenti prodotti da AutoWalkPathfinder (Contratto D1).
     * Assicura che:
     * 1. Attraversamenti orizzontali senza transito verticale siano riclassificati come WALK.
     * 2. Sequenze verticali su arrampicabili ricevano metadati completi (ClimbTraversal) e ruoli MOUNT/TRANSIT/DISMOUNT.
     * 3. Non esistano segmenti CLIMB con metadati nulli.
     */
    public static List<RouteSegment> normalizeRouteSegments(
            @NotNull Level level,
            @NotNull List<BlockPos> path,
            @NotNull List<RouteSegment> rawSegments,
            boolean allowClosedDoors
    ) {
        if (rawSegments.isEmpty() || path.size() < 2) {
            return rawSegments;
        }

        List<RouteSegment> normalized = new ArrayList<>(rawSegments.size());

        for (int i = 0; i < rawSegments.size(); i++) {
            RouteSegment seg = rawSegments.get(i);

            if (seg.isClimb()) {
                // Controlla se questo segmento o i suoi vicini formano una run verticale reale
                boolean isVertical = seg.from().getY() != seg.to().getY();
                boolean nextIsVerticalClimb = (i + 1 < rawSegments.size())
                        && rawSegments.get(i + 1).isClimb()
                        && rawSegments.get(i + 1).from().getY() != rawSegments.get(i + 1).to().getY();
                boolean prevIsVerticalClimb = (i > 0)
                        && rawSegments.get(i - 1).isClimb()
                        && rawSegments.get(i - 1).from().getY() != rawSegments.get(i - 1).to().getY();

                if (!isVertical && !nextIsVerticalClimb && !prevIsVerticalClimb) {
                    // Puro attraversamento orizzontale: declassa a WALK (Contratto D1)
                    normalized.add(RouteSegment.walk(seg.from(), seg.to()));
                    continue;
                }

                // Se mancano i climbData, risolvi il traversal geometrico
                ClimbTraversal climbData = seg.climbData();
                if (climbData == null) {
                    Direction.AxisDirection dir = (seg.to().getY() >= seg.from().getY())
                            ? Direction.AxisDirection.POSITIVE
                            : Direction.AxisDirection.NEGATIVE;
                    BlockPos probePos = ClimbableGeometry.isClimbable(level.getBlockState(seg.to())) ? seg.to() : seg.from();
                    climbData = ClimbTraversalAnalyzer.analyze(level, probePos, dir, null, allowClosedDoors);
                }

                if (climbData != null) {
                    RouteSegment.ClimbLeg leg = seg.climbLeg();
                    if (leg == null) {
                        if (!isVertical && nextIsVerticalClimb) {
                            leg = RouteSegment.ClimbLeg.MOUNT;
                        } else if (!isVertical && prevIsVerticalClimb) {
                            leg = RouteSegment.ClimbLeg.DISMOUNT;
                        } else {
                            leg = RouteSegment.ClimbLeg.TRANSIT;
                        }
                    }
                    normalized.add(RouteSegment.climb(seg.from(), seg.to(), leg, climbData, seg.climbEntryTransition()));
                } else {
                    // Fallback di sicurezza: se non e' risolvibile come climb geometrico, usa walk
                    normalized.add(RouteSegment.walk(seg.from(), seg.to()));
                }
            } else {
                normalized.add(seg);
            }
        }

        return Collections.unmodifiableList(normalized);
    }
}
