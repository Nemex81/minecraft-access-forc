package org.mcaccess.minecraftaccess.features.autowalk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder.PathResult;
import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder.PathStatus;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator;
import org.mcaccess.minecraftaccess.features.cognitive.CognitiveEvent;
import org.mcaccess.minecraftaccess.features.cognitive.CognitivePriority;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AutoWalk Pathfinder & 5D Headroom/Two-Pass Tests (ASTRALIS Fase 5D)")
class AutoWalkPathfinderTest {

    private final Map<BlockPos, BlockState> blockWorld = new HashMap<>();
    private ClientLevel level;

    @BeforeAll
    static void initBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        blockWorld.clear();
        level = mock(ClientLevel.class);
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(invocation -> {
            BlockPos pos = invocation.getArgument(0);
            BlockState state = blockWorld.get(pos);
            if (state != null) {
                return state;
            }
            if (pos.getY() < 64) {
                return Blocks.STONE.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        });
    }

    @AfterEach
    void tearDown() {
        blockWorld.clear();
        MovementCoordinator.resetTestSeams();
        CognitiveCoordinator.clearAllBuffers();
    }

    private void setBlock(BlockPos pos, BlockState state) {
        blockWorld.put(pos.immutable(), state);
    }

    private void setSolid(BlockPos pos) {
        setBlock(pos, Blocks.STONE.defaultBlockState());
    }

    private void setClosedDoor(BlockPos lowerPos) {
        setClosedDoor(lowerPos, Direction.NORTH);
    }

    private void setClosedDoor(BlockPos lowerPos, Direction facing) {
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.FACING, facing);
        BlockState upper = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(DoorBlock.FACING, facing);
        setBlock(lowerPos, lower);
        setBlock(lowerPos.above(), upper);
    }

    // =========================================================================
    // Scenario 1: Strict Pass (Via esterna libera bypassa porte chiuse vicine)
    // =========================================================================
    @Test
    @DisplayName("Scenario 1: Strict Pass aggira porta chiusa se esiste un percorso libero all'esterno")
    void testStrictPassBypassesClosedDoorWhenOpenRouteExists() {
        // Posizione giocatore (0, 64, 0), Destinazione (0, 64, 4)
        // La linea retta (0, 64, 2) ha una porta chiusa
        setClosedDoor(new BlockPos(0, 64, 2));

        // Il corridoio a lato (X = 1 o X = -1) è completamente aperto
        Vec3 start = new Vec3(0.5, 64.0, 0.5);
        BlockPos goal = new BlockPos(0, 64, 4);

        PathResult result = AutoWalkPathfinder.findPath(level, start, goal, 64);

        assertEquals(PathStatus.FOUND, result.status(), "Deve trovare una rotta libera");
        assertNotNull(result.path());
        assertFalse(result.path().isEmpty());

        // La rotta trovata al Passaggio 1 NON deve passare per la porta chiusa a (0, 64, 2)
        assertFalse(result.path().contains(new BlockPos(0, 64, 2)),
                "La rotta rigorosa non deve attraversare la porta chiusa se esiste alternativa libera");
    }

    // =========================================================================
    // Scenario 2: Fallback Pass (Porta chiusa usata con penalità 30.0 se unica via)
    // =========================================================================
    @Test
    @DisplayName("Scenario 2: Fallback Pass attraversa porta chiusa con penalita solo se unica via")
    void testFallbackPassSelectsClosedDoorWhenNoOpenRouteExists() {
        // Stanza completamente sigillata: perimetri chiusi da X = -2 a X = 2 e Z da -1 a 5
        for (int x = -2; x <= 2; x++) {
            // Parete sud a Z = -1
            setSolid(new BlockPos(x, 64, -1));
            setSolid(new BlockPos(x, 65, -1));
            // Parete nord a Z = 5
            setSolid(new BlockPos(x, 64, 5));
            setSolid(new BlockPos(x, 65, 5));
            // Parete divisoria interna a Z = 2 (con varco solo a X = 0)
            if (x != 0) {
                setSolid(new BlockPos(x, 64, 2));
                setSolid(new BlockPos(x, 65, 2));
            }
        }
        for (int z = -1; z <= 5; z++) {
            // Parete ovest a X = -2
            setSolid(new BlockPos(-2, 64, z));
            setSolid(new BlockPos(-2, 65, z));
            // Parete est a X = 2
            setSolid(new BlockPos(2, 64, z));
            setSolid(new BlockPos(2, 65, z));
        }

        // Porta chiusa a (0, 64, 2) come UNICA via per passare dalla stanza sud (Z = 0) alla stanza nord (Z = 4)
        setClosedDoor(new BlockPos(0, 64, 2));

        Vec3 start = new Vec3(0.5, 64.0, 0.5);
        BlockPos goal = new BlockPos(0, 64, 4);

        PathResult result = AutoWalkPathfinder.findPath(level, start, goal, 64);

        assertEquals(PathStatus.FOUND, result.status(), "Il Fallback Pass deve trovare la rotta attraverso la porta");
        assertNotNull(result.path());
        assertTrue(result.path().contains(new BlockPos(0, 64, 2)),
                "La rotta di fallback deve passare per la porta chiusa quando non vi sono alternative");
        assertTrue(result.totalDistance() >= AutoWalkPathfinder.CLOSED_DOOR_PENALTY,
                "Il costo totale deve includere la penalità di 30.0 per la porta chiusa");
    }

    // =========================================================================
    // Scenario 3: Test Budget Esaurito (SEARCH_BUDGET_EXHAUSTED distinto da NO_PATH)
    // =========================================================================
    @Test
    @DisplayName("Scenario 3: Esaurimento budget restituisce SEARCH_BUDGET_EXHAUSTED senza falso fallback")
    void testSearchBudgetExhaustedReturnsDistinctStatus() {
        Vec3 start = new Vec3(0.5, 64.0, 0.5);
        BlockPos distantGoal = new BlockPos(0, 64, 20);

        // Con un budget restrittivo di soli 3 nodi esplorati
        PathResult result = AutoWalkPathfinder.findPath(level, start, distantGoal, 64, 3);

        assertEquals(PathStatus.SEARCH_BUDGET_EXHAUSTED, result.status(),
                "Deve restituire SEARCH_BUDGET_EXHAUSTED e non NO_PATH");
        assertNotEquals(PathStatus.NO_PATH, result.status(),
                "SEARCH_BUDGET_EXHAUSTED deve essere distinto da NO_PATH");
        assertTrue(result.path().isEmpty(), "Nessuna rotta deve essere fornita su budget esaurito");
    }

    // =========================================================================
    // Scenario 4: Copertura Varchi & Penalità Singola Porta
    // =========================================================================
    @Test
    @DisplayName("Scenario 4: Rilevamento univoco varchi chiusi/aperti e singola penalita per varco a 2 blocchi")
    void testDoorGateTrapdoorCoverageAndSinglePenalty() {
        BlockPos testPos = new BlockPos(10, 64, 10);

        // 1. Porta in legno inferiore
        BlockState doorLowerClosed = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState doorLowerOpen = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.OPEN, true)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);

        setBlock(testPos, doorLowerClosed);
        assertTrue(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));
        setBlock(testPos, doorLowerOpen);
        assertFalse(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));

        // 2. Porta in legno superiore
        BlockState doorUpperClosed = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        BlockState doorUpperOpen = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.OPEN, true)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);

        setBlock(testPos, doorUpperClosed);
        assertTrue(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));
        setBlock(testPos, doorUpperOpen);
        assertFalse(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));

        // 3. Cancello
        BlockState gateClosed = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN, false);
        BlockState gateOpen = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN, true);

        setBlock(testPos, gateClosed);
        assertTrue(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));
        setBlock(testPos, gateOpen);
        assertFalse(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));

        // 4. Botola
        BlockState trapClosed = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false);
        BlockState trapOpen = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);

        setBlock(testPos, trapClosed);
        assertTrue(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));
        setBlock(testPos, trapOpen);
        assertFalse(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));

        // 5. Blocchi ordinari
        setBlock(testPos, Blocks.STONE.defaultBlockState());
        assertFalse(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));
        setBlock(testPos, Blocks.AIR.defaultBlockState());
        assertFalse(AutoWalkPathfinder.isDoorOrGateClosed(level, testPos));
    }

    // =========================================================================
    // Scenario 5: Headroom Geometrico & Clearance Salto a 3 Volumi
    // =========================================================================
    @Test
    @DisplayName("Scenario 5: Headroom a 3 volumi per gradini/scale e controllo corridoi diagonali")
    void testHeadroomClearanceAndJumpArc() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos stepFoot = new BlockPos(0, 64, 1);
        BlockPos targetStep = new BlockPos(0, 65, 1);

        // Configurazione gradino saltabile: altezza 1.0 (roccia a stepFoot)
        setSolid(stepFoot);

        // Caso 5A: Spazio libero a 3 volumi sopra landing e takeoff -> salita valida
        assertTrue(AutoWalkPathfinder.hasJumpArcClearance(level, from, targetStep),
                "Con tutti i volumi d'aria liberi, hasJumpArcClearance deve essere true");
        assertTrue(AutoWalkPathfinder.isClimbableStep(level, from, stepFoot, targetStep, true),
                "Gradino con headroom sufficiente deve essere scalabile");

        // Caso 5B: Soffitto basso al culmine della parabola di atterraggio (targetStep.above(2) solido)
        setSolid(targetStep.above(2)); // (0, 67, 1)
        assertFalse(AutoWalkPathfinder.hasJumpArcClearance(level, from, targetStep),
                "Soffitto solido a targetStep.above(2) deve far fallire hasJumpArcClearance");
        assertFalse(AutoWalkPathfinder.isClimbableStep(level, from, stepFoot, targetStep, true),
                "Gradino sotto soffitto basso deve essere rigettato");
        setBlock(targetStep.above(2), Blocks.AIR.defaultBlockState()); // Ripristino

        // Caso 5C: Soffitto basso sopra il punto di stacco del giocatore (from.above(2) solido)
        setSolid(from.above(2)); // (0, 66, 0)
        assertFalse(AutoWalkPathfinder.hasJumpArcClearance(level, from, targetStep),
                "Soffitto solido a from.above(2) deve far fallire hasJumpArcClearance");
        assertFalse(AutoWalkPathfinder.isClimbableStep(level, from, stepFoot, targetStep, true));
        setBlock(from.above(2), Blocks.AIR.defaultBlockState()); // Ripristino

        // Caso 5D: Pericolo (Lava) nel volume di atterraggio
        setBlock(targetStep, Blocks.LAVA.defaultBlockState());
        assertFalse(AutoWalkPathfinder.hasJumpArcClearance(level, from, targetStep),
                "Presenza di lava deve invalidare hasJumpArcClearance");
        setBlock(targetStep, Blocks.AIR.defaultBlockState()); // Ripristino

        // Caso 5E: Salita diagonale con ostacolo nei corridoi ortogonali intermedi
        BlockPos diagFrom = new BlockPos(0, 64, 0);
        BlockPos diagTargetStep = new BlockPos(1, 65, 1);
        BlockPos ortho1 = new BlockPos(1, 64, 0);
        BlockPos ortho2 = new BlockPos(0, 64, 1);

        // Senza soffitto sui vicini ortogonali
        assertTrue(AutoWalkPathfinder.isClearHeadroom(level, ortho1.above(2)));
        assertTrue(AutoWalkPathfinder.isClearHeadroom(level, ortho2.above(2)));

        // Con soffitto basso su ortho1.above(2)
        setSolid(ortho1.above(2));
        assertFalse(AutoWalkPathfinder.isClearHeadroom(level, ortho1.above(2)),
                "Corridoio ortogonale intermedio con soffitto basso deve essere rilevato");
    }

    // =========================================================================
    // Scenario 6: Flusso Terminale Budget nei Componenti (Navigator, Motor, Coordinator)
    // =========================================================================
    @Test
    @DisplayName("Scenario 6A: RouteNavigator gestisce SEARCH_BUDGET_EXHAUSTED pulendo la rotta")
    void testRouteNavigatorHandlesBudgetExhaustion() {
        RouteNavigator navigator = new RouteNavigator();
        BlockPos dummyGoal = new BlockPos(5, 64, 5);
        navigator.setTestRoute(List.of(dummyGoal), dummyGoal, "MetaTest");
        assertTrue(navigator.hasActiveRoute());

        // 1. In startRoute con budget restrittivo su meta distante
        PathResult startRes = navigator.startRoute(level, new Vec3(0, 64, 0), new BlockPos(0, 64, 20), 10);
        assertNotEquals(PathStatus.FOUND, startRes.status());
        assertFalse(navigator.hasActiveRoute());
        assertTrue(navigator.isRouteCompleted());

        // 2. In repath
        navigator.setTestRoute(List.of(dummyGoal), dummyGoal, "MetaTest");
        assertTrue(navigator.hasActiveRoute());
        PathResult repathRes = navigator.repath(level, new Vec3(0, 64, 0), 10);
        assertNotEquals(PathStatus.FOUND, repathRes.status());
        assertFalse(navigator.hasActiveRoute());
    }

    @Test
    @DisplayName("Scenario 6B: AutoWalkMotor gestisce SEARCH_BUDGET_EXHAUSTED come terminale CANCELLED")
    void testAutoWalkMotorHandlesBudgetExhaustion() {
        AutoWalkMotor motor = new AutoWalkMotor();
        RouteNavigator navigator = new RouteNavigator();
        BlockPos goal = new BlockPos(10, 64, 10);
        navigator.setTestRoute(List.of(goal), goal, "TargetTest");

        Config.AutoWalk config = new Config.AutoWalk();
        List<String> callbackEvents = new ArrayList<>();
        AutoWalkMotor.MotorCallback callback = new AutoWalkMotor.MotorCallback() {
            @Override public void onArrival(Object target) { callbackEvents.add("ARRIVAL"); }
            @Override public void onStepNode() { callbackEvents.add("STEP_NODE"); }
            @Override public void onProgression(int remainingSteps) { callbackEvents.add("PROGRESSION"); }
            @Override public void onDoorClosed() { callbackEvents.add("DOOR_CLOSED"); }
            @Override public void onDoorOpened(Object target) { callbackEvents.add("DOOR_OPENED"); }
            @Override public void onTakeover() { callbackEvents.add("TAKEOVER"); }
            @Override public void onNoPath(Object target) { callbackEvents.add("NO_PATH"); }
            @Override public void onStuck() { callbackEvents.add("STUCK"); }
            @Override public void onRepathRequested() { callbackEvents.add("REPATH_REQUESTED"); }
        };

        motor.setState(AutoWalkMotor.State.WALKING);
        PathResult exhausted = PathResult.searchBudgetExhausted();

        boolean isTerminal = motor.handleRepathResult(exhausted, null, null, navigator, config, "TargetTest", callback);

        assertTrue(isTerminal, "SEARCH_BUDGET_EXHAUSTED deve essere terminale");
        assertEquals(AutoWalkMotor.State.CANCELLED, motor.getState(), "Stato deve passare a CANCELLED");
        assertFalse(navigator.hasActiveRoute(), "La rotta deve essere pulita");
        assertFalse(callbackEvents.contains("DOOR_CLOSED"), "Non deve attivare attesa porta su budget esaurito");
        assertTrue(callbackEvents.contains("NO_PATH"), "Deve notificare onNoPath per consentire la voce");
    }

    @Test
    @DisplayName("Scenario 6C: MovementCoordinator gestisce SEARCH_BUDGET_EXHAUSTED con feedback no_path storico")
    void testMovementCoordinatorHandlesBudgetExhaustion() {
        RouteNavigator navigator = new RouteNavigator();
        AutoWalkMotor motor = new AutoWalkMotor();
        MovementCoordinator coordinator = new MovementCoordinator(navigator, motor);

        List<CognitiveEvent> events = new ArrayList<>();
        MovementCoordinator.setCognitiveEventConsumer(events::add);
        MovementCoordinator.setMessageResolver((key, args) -> "[" + key + "]");

        Config.AutoWalk testConfig = new Config.AutoWalk();
        testConfig.enabled = true;
        testConfig.maxRange = 10;
        testConfig.audioCueVolume = 0.8f;
        MovementCoordinator.setTestAutoWalkConfig(testConfig);

        LocalPlayer player = mock(LocalPlayer.class);
        when(player.position()).thenReturn(new Vec3(0, 64, 0));
        when(player.blockPosition()).thenReturn(new BlockPos(0, 64, 0));

        // Bersaglio irraggiungibile circondato da mura
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    setSolid(new BlockPos(x, 64, z));
                    setSolid(new BlockPos(x, 65, z));
                }
            }
        }
        BlockPos isolatedGoal = new BlockPos(10, 64, 10);

        coordinator.start(null, player, level, isolatedGoal);

        assertFalse(motor.isActive());
        assertEquals(AutoWalkMotor.State.IDLE, motor.getState());
        assertFalse(events.isEmpty());

        CognitiveEvent event = events.get(0);
        assertTrue(
                MovementCoordinator.SEMANTIC_NO_PATH.equals(event.semanticKey())
                        || MovementCoordinator.SEMANTIC_OUT_OF_RANGE.equals(event.semanticKey()),
                "L'evento emesso deve essere di sicurezza/mancata rotta"
        );
        assertEquals(CognitivePriority.OPERATIONAL, event.priority());
    }

    private void setOpenDoor(BlockPos lowerPos) {
        setOpenDoor(lowerPos, Direction.NORTH);
    }

    private void setOpenDoor(BlockPos lowerPos, Direction facing) {
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.OPEN, true)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.FACING, facing);
        BlockState upper = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.OPEN, true)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(DoorBlock.FACING, facing);
        setBlock(lowerPos, lower);
        setBlock(lowerPos.above(), upper);
    }

    // =========================================================================
    // Revisione Correttiva 5D.3: Collisione Varco Radice, Specularità & Diagonali Rigorose
    // =========================================================================

    @Test
    @DisplayName("5D.3 - Test 1: Radice, movimento verso pannello interseca VoxelShape (escluso in Pass 1, 31.0 in Pass 2)")
    void testRootMoveTowardsDoorPanelIntersectsCollisionShape() {
        // Oak door facing WEST ha il pannello chiuso posizionato a X in [0.8125, 1.0] dentro il blocco (0, 64, 0).
        BlockPos doorPos = new BlockPos(0, 64, 0);
        setClosedDoor(doorPos, Direction.WEST);

        Vec3 startVec = new Vec3(0.5, 64.0, 0.5);
        BlockPos targetPos = new BlockPos(1, 64, 0);
        AutoWalkPathfinder.PathNode rootNode = new AutoWalkPathfinder.PathNode(
                doorPos, 0.0, 10.0, 10.0, null, null, false, 0
        );
        AutoWalkPathfinder.NeighborMove moveEast = new AutoWalkPathfinder.NeighborMove(
                targetPos, Direction.EAST, 0, false, false
        );

        // L'helper continuo deve rilevare l'intersezione fisica tra l'AABB del giocatore e la VoxelShape del pannello
        BlockPos hitDoor = AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveEast);
        assertEquals(doorPos, hitDoor, "Il movimento verso il pannello a Est deve rilevare la collisione con la porta chiusa");

        // Nel Passaggio 1 (allowClosedDoors = false): la mossa verso Est deve essere esclusa dai vicini validi
        List<AutoWalkPathfinder.NeighborMove> pass1Moves = AutoWalkPathfinder.getValidNeighbors(
                level, startVec, doorPos, 32, doorPos, false, true
        );
        boolean moveEastInPass1 = pass1Moves.stream().anyMatch(m -> m.targetPos().equals(targetPos));
        assertFalse(moveEastInPass1, "Nel Passaggio 1 la mossa che attraversa il pannello chiuso deve essere esclusa");

        // Nel Passaggio 2 (allowClosedDoors = true): la mossa riceve la penalità di 30.0 una sola volta
        double costRoot = AutoWalkPathfinder.calculateStepCost(level, startVec, rootNode, moveEast, true);
        assertEquals(1.0 + AutoWalkPathfinder.CLOSED_DOOR_PENALTY, costRoot, 0.001,
                "Nel Passaggio 2 l'attraversamento iniziale del pannello deve pagare 31.0");
    }

    @Test
    @DisplayName("5D.3 - Test 2: Radice, movimento parallelo al pannello privo di intersezione (valido in Pass 1, 1.0 in Pass 2)")
    void testRootMoveParallelToDoorPanelHasNoIntersection() {
        // Oak door facing WEST ha il pannello a X in [0.8125, 1.0].
        BlockPos doorPos = new BlockPos(0, 64, 0);
        setClosedDoor(doorPos, Direction.WEST);

        Vec3 startVec = new Vec3(0.5, 64.0, 0.5);
        BlockPos targetNorth = new BlockPos(0, 64, -1);
        AutoWalkPathfinder.PathNode rootNode = new AutoWalkPathfinder.PathNode(
                doorPos, 0.0, 10.0, 10.0, null, null, false, 0
        );
        AutoWalkPathfinder.NeighborMove moveNorth = new AutoWalkPathfinder.NeighborMove(
                targetNorth, Direction.NORTH, 0, false, false
        );

        // Il movimento verso Nord lungo Z non tocca la coordinata X=0.8125 (playerBox.maxX = 0.5 + 0.3 = 0.80 < 0.8125)
        BlockPos hitDoor = AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveNorth);
        assertNull(hitDoor, "Il movimento parallelo al pannello senza intersezione non deve rilevare collisione");

        // Nel Passaggio 1 (allowClosedDoors = false): la mossa verso Nord deve essere valida e inclusa
        List<AutoWalkPathfinder.NeighborMove> pass1Moves = AutoWalkPathfinder.getValidNeighbors(
                level, startVec, doorPos, 32, doorPos, false, true
        );
        boolean moveNorthInPass1 = pass1Moves.stream().anyMatch(m -> m.targetPos().equals(targetNorth));
        assertTrue(moveNorthInPass1, "Nel Passaggio 1 la mossa parallela non collidente deve restare valida");

        // Nel Passaggio 2 (allowClosedDoors = true): costo base 1.0 senza alcuna penalità di uscita
        double costNorth = AutoWalkPathfinder.calculateStepCost(level, startVec, rootNode, moveNorth, true);
        assertEquals(1.0, costNorth, 0.001,
                "Il movimento parallelo senza intersezione non deve scontare la penalità di porta chiusa");
    }

    @Test
    @DisplayName("5D.3 - Test 3: Specularità fisica verificata su tutti e 4 gli orientamenti cardinali (NORTH, SOUTH, EAST, WEST)")
    void testPhysicalSymmetryAcrossAllFacings() {
        BlockPos doorPos = new BlockPos(0, 64, 0);
        Vec3 startVec = new Vec3(0.5, 64.0, 0.5);

        // 1. EAST: pannello a X in [0.0, 0.1875] -> movimento verso WEST (-X) collide, verso NORTH (Z) no
        setClosedDoor(doorPos, Direction.EAST);
        AutoWalkPathfinder.NeighborMove moveWest = new AutoWalkPathfinder.NeighborMove(
                new BlockPos(-1, 64, 0), Direction.WEST, 0, false, false
        );
        AutoWalkPathfinder.NeighborMove moveNorth = new AutoWalkPathfinder.NeighborMove(
                new BlockPos(0, 64, -1), Direction.NORTH, 0, false, false
        );
        assertEquals(doorPos, AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveWest),
                "EAST: movimento verso West deve collidere con il pannello");
        assertNull(AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveNorth),
                "EAST: movimento verso North non deve collidere");

        // 2. WEST: pannello a X in [0.8125, 1.0] -> movimento verso EAST (+X) collide, verso SOUTH (Z) no
        setClosedDoor(doorPos, Direction.WEST);
        AutoWalkPathfinder.NeighborMove moveEast = new AutoWalkPathfinder.NeighborMove(
                new BlockPos(1, 64, 0), Direction.EAST, 0, false, false
        );
        AutoWalkPathfinder.NeighborMove moveSouth = new AutoWalkPathfinder.NeighborMove(
                new BlockPos(0, 64, 1), Direction.SOUTH, 0, false, false
        );
        assertEquals(doorPos, AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveEast),
                "WEST: movimento verso East deve collidere con il pannello");
        assertNull(AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveSouth),
                "WEST: movimento verso South non deve collidere");

        // 3. SOUTH: pannello a Z in [0.0, 0.1875] -> movimento verso NORTH (-Z) collide, verso EAST (X) no
        setClosedDoor(doorPos, Direction.SOUTH);
        assertEquals(doorPos, AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveNorth),
                "SOUTH: movimento verso North deve collidere con il pannello");
        assertNull(AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveEast),
                "SOUTH: movimento verso East non deve collidere");

        // 4. NORTH: pannello a Z in [0.8125, 1.0] -> movimento verso SOUTH (+Z) collide, verso WEST (X) no
        setClosedDoor(doorPos, Direction.NORTH);
        assertEquals(doorPos, AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveSouth),
                "NORTH: movimento verso South deve collidere con il pannello");
        assertNull(AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, doorPos, moveWest),
                "NORTH: movimento verso West non deve collidere");
    }

    @Test
    @DisplayName("5D.3 - Test 4: Normalizzazione canonica a due blocchi: metà inferiore e superiore condividono identità e singola penalità")
    void testCanonicalTwoBlockDoorNormalization() {
        BlockPos lowerPos = new BlockPos(5, 64, 5);
        setClosedDoor(lowerPos, Direction.NORTH);
        BlockPos upperPos = lowerPos.above();

        BlockPos canonLower = AutoWalkPathfinder.getCanonicalDoorPos(level, lowerPos);
        BlockPos canonUpper = AutoWalkPathfinder.getCanonicalDoorPos(level, upperPos);

        assertEquals(lowerPos, canonLower, "La metà inferiore deve mappare a se stessa");
        assertEquals(lowerPos, canonUpper, "La metà superiore deve mappare alla metà inferiore canonica");

        // Se un raggio parte dalla radice e collide con la metà superiore (headDoorPos),
        // getRootMoveIntersectedClosedDoor restituisce comunque la posizione canonica lowerPos
        Vec3 startVec = new Vec3(5.5, 64.0, 5.5);
        AutoWalkPathfinder.NeighborMove moveSouth = new AutoWalkPathfinder.NeighborMove(
                new BlockPos(5, 64, 6), Direction.SOUTH, 0, false, false
        );
        BlockPos hitDoor = AutoWalkPathfinder.getRootMoveIntersectedClosedDoor(level, startVec, lowerPos, moveSouth);
        assertEquals(lowerPos, hitDoor, "La collisione rilevata deve restituire l'identificatore canonico inferiore");

        // Transizione tra metà della stessa porta: la penalità deve essere applicata una sola volta
        AutoWalkPathfinder.PathNode rootLower = new AutoWalkPathfinder.PathNode(
                lowerPos, 0.0, 5.0, 5.0, null, null, false, 0
        );
        AutoWalkPathfinder.NeighborMove moveUpper = new AutoWalkPathfinder.NeighborMove(
                upperPos, Direction.UP, 1, false, false
        );

        double costSameDoor = AutoWalkPathfinder.calculateStepCost(level, startVec, rootLower, moveUpper, true);
        assertEquals(1.0 + 0.50 + AutoWalkPathfinder.CLOSED_DOOR_PENALTY, costSameDoor, 0.001,
                "La transizione interna alla stessa porta non deve duplicare la penalità");
    }

    @Test
    @DisplayName("5D.3 - Test 5: Fixture Tenuta: partenza da cella porta chiusa con alternativa aperta preferisce la porta aperta al tick 0")
    void testFixtureTenutaPrefersOpenDoubleDoorAtTickZero() {
        // Pavimento solido a Y=63
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                setSolid(new BlockPos(x, 63, z));
            }
        }

        // Muri perimetrali esterni per sigillare l'ambiente (X=-4..4, Z=-5..8)
        for (int x = -4; x <= 4; x++) {
            setSolid(new BlockPos(x, 64, -5));
            setSolid(new BlockPos(x, 65, -5));
            setSolid(new BlockPos(x, 64, 8));
            setSolid(new BlockPos(x, 65, 8));
        }
        for (int z = -5; z <= 8; z++) {
            setSolid(new BlockPos(-4, 64, z));
            setSolid(new BlockPos(-4, 65, z));
            setSolid(new BlockPos(4, 64, z));
            setSolid(new BlockPos(4, 65, z));
        }

        // Parete divisoria a X=0 con due varchi:
        // A Z=0: porta singola CHIUSA a (0, 64, 0) con facing WEST (pannello su confine Est X=0.8125..1.0)
        // A Z=4 e Z=5: porta doppia APERTA a (0, 64, 4) e (0, 64, 5) con facing WEST
        for (int z = -5; z <= 8; z++) {
            if (z != 0 && z != 4 && z != 5) {
                setSolid(new BlockPos(0, 64, z));
                setSolid(new BlockPos(0, 65, z));
            }
        }
        setClosedDoor(new BlockPos(0, 64, 0), Direction.WEST);
        setOpenDoor(new BlockPos(0, 64, 4), Direction.WEST);
        setOpenDoor(new BlockPos(0, 64, 5), Direction.WEST);

        // Destinazione nella stanza Est a (3, 64, 4)
        BlockPos goal = new BlockPos(3, 64, 4);

        // Giocatore parte dentro la cella della porta chiusa a (0, 64, 0) a X=0.5
        // Uscire direttamente verso Est (1, 64, 0) attraversa il pannello chiuso a X=0.8125!
        Vec3 startVec = new Vec3(0.5, 64.0, 0.5);

        PathResult result = AutoWalkPathfinder.findPath(level, startVec, goal, 32);

        assertEquals(PathStatus.FOUND, result.status(), "Il percorso deve essere trovato");
        assertFalse(result.path().isEmpty());

        // La rotta deve passare attraverso il varco aperto a Z=4 o Z=5 (trovato già nel Passaggio 1!)
        // e non uscire direttamente attraverso la porta chiusa a (0, 64, 0)
        boolean usedOpenGate = result.path().stream().anyMatch(p -> p.getX() == 0 && (p.getZ() == 4 || p.getZ() == 5));
        assertTrue(usedOpenGate, "La rotta deve preferire il varco aperto rispetto alla porta chiusa di partenza");
    }

    @Test
    @DisplayName("5D.3 - Test 6: Varco inevitabile (Dominio Pathfinder): Passaggio 2 trova rotta con nodo ortogonale esplicito")
    void testInevitableClosedDoorProducesOrthogonalDoorNodeInPass2() {
        for (int x = -5; x <= 10; x++) {
            for (int z = -5; z <= 5; z++) {
                setSolid(new BlockPos(x, 63, z));
            }
        }

        // Muri perimetrali esterni per sigillare l'ambiente e rendere la porta a X=3 l'unico passaggio possibile
        for (int x = -5; x <= 10; x++) {
            setSolid(new BlockPos(x, 64, -5));
            setSolid(new BlockPos(x, 65, -5));
            setSolid(new BlockPos(x, 64, 5));
            setSolid(new BlockPos(x, 65, 5));
        }
        for (int z = -5; z <= 5; z++) {
            setSolid(new BlockPos(-5, 64, z));
            setSolid(new BlockPos(-5, 65, z));
            setSolid(new BlockPos(10, 64, z));
            setSolid(new BlockPos(10, 65, z));
        }

        // Muro divisorio continuo a X=3 con unica porta chiusa a (3, 64, 0)
        for (int z = -5; z <= 5; z++) {
            if (z != 0) {
                setSolid(new BlockPos(3, 64, z));
                setSolid(new BlockPos(3, 65, z));
            }
        }
        setClosedDoor(new BlockPos(3, 64, 0), Direction.WEST);

        Vec3 startVec = new Vec3(0.5, 64.0, 0.5);
        BlockPos goal = new BlockPos(6, 64, 0);

        PathResult result = AutoWalkPathfinder.findPath(level, startVec, goal, 32);

        assertEquals(PathStatus.FOUND, result.status(), "In presenza di porta inevitabile, il fallback Passaggio 2 deve trovare la rotta");
        assertTrue(result.path().contains(new BlockPos(3, 64, 0)), "La rotta deve contenere la porta chiusa come nodo ortogonale esplicito");
    }

    @Test
    @DisplayName("5D.3 - Test 7: Diagonale, barriera intermedia (porta, cancelletto o botola chiusa) invalida sempre la diagonale")
    void testDiagonalMoveRejectedWhenIntermediateCorridorHasClosedInteractiveBarrier() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos target = new BlockPos(1, 64, 1);
        BlockPos ortho1 = new BlockPos(1, 64, 0);
        BlockPos ortho2 = new BlockPos(0, 64, 1);

        // Caso A: Porta chiusa nel corridoio ortogonale 1
        setClosedDoor(ortho1, Direction.NORTH);
        assertFalse(AutoWalkPathfinder.hasStrictDiagonalIntermediateClearance(level, ortho1, ortho2, false),
                "La presenza di una porta chiusa in ortho1 deve invalidare la diagonale");

        // Caso B: Cancelletto chiuso nel corridoio ortogonale 2 (dopo aver liberato ortho1)
        blockWorld.remove(ortho1);
        blockWorld.remove(ortho1.above());
        BlockState closedGate = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN, false);
        setBlock(ortho2, closedGate);
        assertFalse(AutoWalkPathfinder.hasStrictDiagonalIntermediateClearance(level, ortho1, ortho2, false),
                "La presenza di un cancelletto chiuso in ortho2 deve invalidare la diagonale");

        // Caso C: Botola chiusa nel corridoio ortogonale 1 a quota piedi o testa (dopo aver liberato ortho2)
        blockWorld.remove(ortho2);
        BlockState closedTrapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false);
        setBlock(ortho1, closedTrapdoor);
        assertFalse(AutoWalkPathfinder.hasStrictDiagonalIntermediateClearance(level, ortho1, ortho2, false),
                "La presenza di una botola chiusa in ortho1 deve invalidare la diagonale");

        // Verifica che la mossa diagonale sia esclusa da getValidNeighbors sia con allowClosedDoors=false sia con allowClosedDoors=true
        Vec3 startVec = new Vec3(0.5, 64.0, 0.5);
        List<AutoWalkPathfinder.NeighborMove> pass1Moves = AutoWalkPathfinder.getValidNeighbors(
                level, startVec, from, 32, from, false, false
        );
        List<AutoWalkPathfinder.NeighborMove> pass2Moves = AutoWalkPathfinder.getValidNeighbors(
                level, startVec, from, 32, from, true, false
        );
        assertFalse(pass1Moves.stream().anyMatch(m -> m.targetPos().equals(target)),
                "La diagonale con barriera intermedia non deve essere generata in Passaggio 1");
        assertFalse(pass2Moves.stream().anyMatch(m -> m.targetPos().equals(target)),
                "La diagonale con barriera intermedia non deve essere generata in Passaggio 2");
    }

    @Test
    @DisplayName("5D.3 - Test 8: Diagonale, corridoi liberi (valida in piano; in salita richiede clearance al culmine del salto)")
    void testDiagonalMoveWithClearCorridorsAndJumpArcHeadroom() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos ortho1 = new BlockPos(1, 64, 0);
        BlockPos ortho2 = new BlockPos(0, 64, 1);

        // Corridoi ortogonali completamente liberi (aria)
        blockWorld.remove(ortho1);
        blockWorld.remove(ortho1.above());
        blockWorld.remove(ortho2);
        blockWorld.remove(ortho2.above());

        // Caso A: Terreno pianeggiante (isStepUp = false)
        assertTrue(AutoWalkPathfinder.hasStrictDiagonalIntermediateClearance(level, ortho1, ortho2, false),
                "Con corridoi liberi, la diagonale pianeggiante deve essere valida");

        // Caso B: Salita (isStepUp = true): richiede clearance anche a quota Y=66 (above(2))
        // B.1: Soffitto basso a Y=66 sopra ortho1
        setSolid(ortho1.above(2));
        assertFalse(AutoWalkPathfinder.hasStrictDiagonalIntermediateClearance(level, ortho1, ortho2, true),
                "Soffitto basso a quota culmine salto (above(2)) deve invalidare la salita diagonale");

        // B.2: Rimuovendo l'ostacolo dal soffitto, la salita diagonale è valida
        blockWorld.remove(ortho1.above(2));
        assertTrue(AutoWalkPathfinder.hasStrictDiagonalIntermediateClearance(level, ortho1, ortho2, true),
                "Con volume al culmine del salto libero, la salita diagonale deve essere valida");
    }

    // =========================================================================
    // 5D.4: Disimpegno Nicchia Porta, Semantica Nodo Iniziale e Clearance (C1-C7)
    // =========================================================================

    @Test
    @DisplayName("5D.4 - Test 1: hasDirectClearPath rileva aria libera, porta chiusa e blocco solido")
    void testHasDirectClearPath() {
        Vec3 start = new Vec3(0.5, 64.0, 0.5);
        BlockPos targetPos = new BlockPos(0, 64, 2);

        // Caso A: Corridoio libero (aria)
        assertTrue(AutoWalkPathfinder.hasDirectClearPath(level, start, targetPos),
                "Un percorso diretto in aria libera deve restituire true");

        // Caso B: Porta chiusa intermedia a (0, 64, 1)
        BlockPos doorPos = new BlockPos(0, 64, 1);
        setClosedDoor(doorPos);
        assertFalse(AutoWalkPathfinder.hasDirectClearPath(level, start, targetPos),
                "Una porta chiusa lungo la linea diretta deve invalidare hasDirectClearPath");

        // Caso C: Blocco solido intermedio a (0, 64, 1)
        blockWorld.remove(doorPos);
        blockWorld.remove(doorPos.above());
        setSolid(doorPos);
        assertFalse(AutoWalkPathfinder.hasDirectClearPath(level, start, targetPos),
                "Un blocco solido lungo la linea diretta deve invalidare hasDirectClearPath");
    }

    @Test
    @DisplayName("5D.4 - Test 2: ALREADY_AT_TARGET condizionato alla traversabilità diretta (Addendum 10.1)")
    void testAlreadyAtTargetConditionedOnDirectClearance() {
        Vec3 start = new Vec3(0.5, 64.0, 0.5);
        BlockPos targetPos = new BlockPos(0, 64, 1); // directDist = 1.0 < 1.25

        // Caso A: Bersaglio vicino ma separato da porta chiusa
        setClosedDoor(targetPos);
        PathResult resultWithDoor = AutoWalkPathfinder.findPath(level, start, targetPos, 32);
        assertNotEquals(PathStatus.ALREADY_AT_TARGET, resultWithDoor.status(),
                "Un bersaglio vicino separato da porta chiusa NON deve restituire prematuramente ALREADY_AT_TARGET");
        assertEquals(PathStatus.FOUND, resultWithDoor.status(),
                "Deve trovare rotta con attesa porta per consentire l'apertura");

        // Caso B: Bersaglio vicino senza ostacoli (aria libera)
        blockWorld.remove(targetPos);
        blockWorld.remove(targetPos.above());
        PathResult resultClear = AutoWalkPathfinder.findPath(level, start, targetPos, 32);
        assertEquals(PathStatus.ALREADY_AT_TARGET, resultClear.status(),
                "In assenza di ostacoli a distanza < 1.25m deve restituire ALREADY_AT_TARGET");
    }

    @Test
    @DisplayName("5D.4 - Test 3: checkLocalClearance distingue varco chiuso, stipite solido e via libera (Addendum 10.3)")
    void testCheckLocalClearanceDistinguishesJambDoorAndClear() {
        AABB playerBox = new AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);

        // Caso A: Via completamente libera
        Vec3 fromClear = new Vec3(10.5, 64.0, 10.5);
        Vec3 toClear = new Vec3(10.5, 64.0, 11.5);
        AutoWalkPathfinder.ClearanceResult clearRes = AutoWalkPathfinder.checkLocalClearance(level, fromClear, toClear, playerBox);
        assertEquals(AutoWalkPathfinder.ClearanceResult.ClearanceStatus.CLEAR, clearRes.status());
        assertNull(clearRes.blockingDoorPos());

        // Caso B: Varco chiuso a (10, 64, 11) con pannello a Z in [11.0, 11.1875]
        BlockPos doorPos = new BlockPos(10, 64, 11);
        setClosedDoor(doorPos, Direction.SOUTH);
        AutoWalkPathfinder.ClearanceResult doorRes = AutoWalkPathfinder.checkLocalClearance(level, fromClear, toClear, playerBox);
        assertEquals(AutoWalkPathfinder.ClearanceResult.ClearanceStatus.BLOCKED_BY_CLOSED_DOOR, doorRes.status());
        assertEquals(doorPos, doorRes.blockingDoorPos(), "blockingDoorPos deve riportare le coordinate canoniche della porta");

        // Caso C: Stipite solido laterale a (11, 64, 10) con giocatore sfalsato a contatto
        blockWorld.remove(doorPos);
        blockWorld.remove(doorPos.above());
        BlockPos jambPos = new BlockPos(11, 64, 10);
        setSolid(jambPos);
        // Giocatore parte leggermente decentrato a X = 10.8 (hitbox tocca il blocco solido a X = 11.0)
        Vec3 fromJamb = new Vec3(10.8, 64.0, 10.5);
        Vec3 toJamb = new Vec3(10.8, 64.0, 11.5);
        AutoWalkPathfinder.ClearanceResult jambRes = AutoWalkPathfinder.checkLocalClearance(level, fromJamb, toJamb, playerBox);
        assertEquals(AutoWalkPathfinder.ClearanceResult.ClearanceStatus.BLOCKED_BY_SOLID_JAMB, jambRes.status());
        assertNull(jambRes.blockingDoorPos(), "Uno stipite solido non deve avere blockingDoorPos");
    }

    // =========================================================================
    // Revisione 5D.5: Integrità Discesa Scale, Headroom Step-Off e Vincolo Rampa
    // =========================================================================

    @Test
    @DisplayName("5D.5 - Contratto S1: isSafeDescent richiede headroom a quota testa (columnAir.above())")
    void testSafeDescentRequiresHeadroomClearance() {
        BlockPos from = new BlockPos(5, 68, 5);
        BlockPos to = new BlockPos(5, 68, 6);
        BlockPos dropLanding = new BlockPos(5, 66, 6);
        int drop = 2;

        // Suolo d'atterraggio a Y = 65 solido
        setSolid(new BlockPos(5, 65, 6));

        // Colonna di caduta (Y = 68, Y = 67, Y = 66) è aria libera
        // Ma soffitto sopra to a Y = 69 è solido (mancanza headroom)
        BlockPos ceilingAbove = to.above(); // (5, 69, 6)
        setSolid(ceilingAbove);

        assertFalse(AutoWalkPathfinder.isSafeDescent(level, from, to, dropLanding, drop, true),
                "Se columnAir.above() è un blocco solido, isSafeDescent deve scartare la discesa per collisione della testa");

        // Rimuoviamo il soffitto rendendolo aria libera
        blockWorld.remove(ceilingAbove);
        assertTrue(AutoWalkPathfinder.isSafeDescent(level, from, to, dropLanding, drop, true),
                "Con columnAir.above() passabile, isSafeDescent deve autorizzare la discesa");
    }

    @Test
    @DisplayName("5D.5 - Contratto S2: isLateralStairDrop vieta salti/discese laterali su rampa di scale")
    void testStairFlightDisallowsLateralDrop() {
        BlockPos from = new BlockPos(0, 68, 0);
        BlockPos belowFrom = from.below(); // (0, 67, 0)

        // Scala orientata a WEST (sale verso West, scende verso East / +X)
        BlockState stairWest = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST);
        setBlock(belowFrom, stairWest);

        // 1. Discesa longitudinale verso East (+X, dx = +1, dz = 0): consentita
        BlockPos toEast = new BlockPos(1, 68, 0);
        assertFalse(AutoWalkPathfinder.isLateralStairDrop(level, from, toEast, 1),
                "La discesa longitudinale lungo la direzione di discesa della scala deve essere consentita");

        // 2. Discesa laterale verso North (dz = -1, dx = 0): vietata
        BlockPos toNorth = new BlockPos(0, 68, -1);
        assertTrue(AutoWalkPathfinder.isLateralStairDrop(level, from, toNorth, 1),
                "La discesa laterale perpendicolare all'asse della scala deve essere vietata");

        // 3. Discesa laterale verso South (dz = +1, dx = 0): vietata
        BlockPos toSouth = new BlockPos(0, 68, 1);
        assertTrue(AutoWalkPathfinder.isLateralStairDrop(level, from, toSouth, 1),
                "La discesa laterale verso l'altro lato della scala deve essere vietata");

        // 4. Discesa diagonale verso North-East (dx = +1, dz = -1): vietata
        BlockPos toNorthEast = new BlockPos(1, 68, -1);
        assertTrue(AutoWalkPathfinder.isLateralStairDrop(level, from, toNorthEast, 1),
                "La discesa diagonale che scavalca lo spigolo laterale della scala deve essere vietata");

        // 5. Spostamento orizzontale piatto (drop = 0): consentito (cammino su larghezza scala)
        assertFalse(AutoWalkPathfinder.isLateralStairDrop(level, from, toNorth, 0),
                "Un movimento flat (drop = 0) non è una caduta dal bordo scala");
    }

    @Test
    @DisplayName("5D.5 - Integrazione: la discesa della rampa completa fino al pianerottolo evita scorciatoia laterale")
    void testStaircaseDescentFollowsFlightToLanding() {
        // Costruiamo una rampa di 3 gradini che scende verso East (+X):
        // X = 0: gradino a Y = 68 (walkable a Y = 69)
        // X = 1: gradino a Y = 67 (walkable a Y = 68)
        // X = 2: gradino a Y = 66 (walkable a Y = 67)
        // X = 3: pavimento pianerottolo solido a Y = 65 (walkable a Y = 66)
        // X = 4: corridoio aperto a Y = 65
        BlockState stairWest = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST);

        setBlock(new BlockPos(0, 68, 0), stairWest);
        setBlock(new BlockPos(1, 67, 0), stairWest);
        setBlock(new BlockPos(2, 66, 0), stairWest);
        setSolid(new BlockPos(3, 65, 0)); // Pianerottolo
        setSolid(new BlockPos(4, 65, 0));
        setSolid(new BlockPos(4, 65, 1)); // Destinazione a lato del corridoio dopo la scala

        // A X = 1, Z = 1 c'è un dislivello verso un piano inferiore a Y = 65, ma con soffitto solido a Y = 69
        setSolid(new BlockPos(1, 65, 1));
        setSolid(new BlockPos(1, 69, 1)); // Soffitto che ostruisce l'headroom

        Vec3 start = new Vec3(0.5, 69.0, 0.5);
        BlockPos target = new BlockPos(4, 66, 1);

        PathResult result = AutoWalkPathfinder.findPath(level, start, target, 32);

        assertEquals(PathStatus.FOUND, result.status(), "Deve trovare un percorso valido");
        assertNotNull(result.path());

        // Verifichiamo che il percorso scenda ordinatamente lungo l'asse X della scala
        boolean reachedLanding = false;
        for (BlockPos pos : result.path()) {
            if (pos.getX() == 3 && pos.getY() == 66) {
                reachedLanding = true;
            }
            // Non deve MAI saltare a Z = 1 mentre si trova a X <= 2
            if (pos.getX() <= 2) {
                assertEquals(0, pos.getZ(), "Non deve deviare lateralmente a Z = 1 durante la rampa di scale");
            }
        }
        assertTrue(reachedLanding, "Il percorso deve scendere fino al pianerottolo a X = 3");
    }

    @Test
    @DisplayName("Contratto B1: Budget esaurito nel Passaggio 1 autorizza il Fallback su Passaggio 2 con porte chiuse")
    void testBudgetExhaustionAllowsFallbackWhenDoorBlocked() {
        // Pavimento a Y = 69
        setSolid(new BlockPos(0, 69, 0));
        // Pavimento sotto la porta a X = 1
        setSolid(new BlockPos(1, 69, 0));
        // Porta chiusa a X = 1
        setClosedDoor(new BlockPos(1, 70, 0), Direction.EAST);
        setSolid(new BlockPos(2, 69, 0));

        // Corridoio aperto verso -X (vicolo cieco di 30 nodi)
        for (int x = -1; x >= -30; x--) {
            setSolid(new BlockPos(x, 69, 0));
        }

        Vec3 start = new Vec3(0.5, 70.0, 0.5);
        BlockPos target = new BlockPos(2, 70, 0);

        // Verifica diretta della logica di gating del fallback
        assertTrue(AutoWalkPathfinder.isCandidateForClosedDoorFallback(level, new BlockPos(0, 70, 0), target, 2.0, 32),
                "La meta a distanza 2m con porta chiusa adiacente deve essere candidata al fallback");

        // Con budget = 20:
        // Passaggio 1 esplora il corridoio verso -X ed esaurisce i 20 nodi senza poter attraversare la porta.
        // Contratto B1 rileva la porta chiusa e autorizza il Passaggio 2.
        // Il Passaggio 2 esplora 15 nodi del corridoio prima che il costo f superi la porta, quindi attraversa
        // la porta a X = 1 e trova il target entro i 20 nodi.
        PathResult result = AutoWalkPathfinder.findPath(level, start, target, 32, 20);

        assertEquals(PathStatus.FOUND, result.status(), "Il Passaggio 2 deve trovare il percorso attraverso la porta chiusa anche se il Passaggio 1 ha esaurito il budget");
        assertNotNull(result.path());
        assertTrue(result.path().contains(new BlockPos(1, 70, 0)), "Il percorso deve transitare per il varco della porta");

        // Caso di esclusione (aperta campagna senza porte con distanza > 64m sia da start che da target):
        // Non deve procedere al Passaggio 2 e deve restituire SEARCH_BUDGET_EXHAUSTED
        BlockPos openFieldStart = new BlockPos(100, 70, 0);
        BlockPos farOpenTarget = new BlockPos(180, 70, 0);
        assertFalse(AutoWalkPathfinder.isCandidateForClosedDoorFallback(level, openFieldStart, farOpenTarget, 80.0, 100),
                "In aperta campagna a 80m senza alcuna porta nei dintorni, non deve essere candidato al fallback");

        // Corridoio aperto verso +X a partire da openFieldStart
        for (int x = 100; x <= 180; x++) {
            setSolid(new BlockPos(x, 69, 0));
        }
        PathResult farResult = AutoWalkPathfinder.findPath(level, new Vec3(100.5, 70.0, 0.5), farOpenTarget, 100, 20);
        assertEquals(PathStatus.SEARCH_BUDGET_EXHAUSTED, farResult.status(), "In aperta campagna oltre 64m senza porte, deve mantenere SEARCH_BUDGET_EXHAUSTED");
    }

    @Test
    @DisplayName("Contratto B2: Scala a L con pianerottolo d'angolo e scala a pioli (ladder) a parete")
    void testLShapedStaircaseWithLadderLanding() {
        // Piattaforma iniziale a Y = 76 (floor Y = 75)
        setSolid(new BlockPos(0, 75, 0));

        // Rampa 1: scende verso Sud (+Z)
        BlockState stairNorth = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH);
        setBlock(new BlockPos(0, 75, 1), stairNorth);
        setBlock(new BlockPos(0, 74, 2), stairNorth);

        // Pilastro d'angolo interno per impedire tagli diagonali nel vuoto
        setSolid(new BlockPos(1, 72, 2));
        setSolid(new BlockPos(1, 73, 2));
        setSolid(new BlockPos(1, 74, 2));
        setSolid(new BlockPos(1, 75, 2));
        setSolid(new BlockPos(1, 76, 2));

        // Pianerottolo d'angolo a (0, 73, 3): pavimento a Y = 72, ladder su parete a Y = 73 e 74
        setSolid(new BlockPos(0, 72, 3));
        BlockState ladderEast = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, Direction.EAST);
        setBlock(new BlockPos(0, 73, 3), ladderEast);
        setBlock(new BlockPos(0, 74, 3), ladderEast);

        // Rampa 2: gira a Est (+X) e scende verso Est
        BlockState stairWest = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST);
        setBlock(new BlockPos(1, 72, 3), stairWest);
        setBlock(new BlockPos(2, 71, 3), stairWest);

        // Pianerottolo finale a Y = 70 (floor Y = 69)
        setSolid(new BlockPos(3, 69, 3));
        setSolid(new BlockPos(4, 69, 3));

        Vec3 start = new Vec3(0.5, 76.0, 0.5);
        BlockPos target = new BlockPos(4, 70, 3);

        PathResult result = AutoWalkPathfinder.findPath(level, start, target, 32);

        assertEquals(PathStatus.FOUND, result.status(), "Deve trovare il percorso lungo la scala a L");
        assertNotNull(result.path());

        // Deve transitare per la colonna del pianerottolo d'angolo (X=0, Z=3) provvisto di ladder
        boolean visitedCorner = result.path().stream()
                .anyMatch(p -> p.getX() == 0 && p.getZ() == 3);
        assertTrue(visitedCorner, "Il percorso deve transitare per il pianerottolo d'angolo con la ladder");
    }

    @Test
    @DisplayName("Contratto B3 & B4: Rotta reale Torre Belvedere -> cas ingresso solaio con rampe a L e porta")
    void testBelvedereToSolaioMansionRoute() {
        // Pavimento stanza torre a Y = 80
        setSolid(new BlockPos(-62, 80, -39));
        setSolid(new BlockPos(-62, 80, -38));

        // Rampa 1: scende verso Sud (+Z)
        BlockState stairNorth = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH);
        setBlock(new BlockPos(-62, 80, -37), stairNorth);
        setBlock(new BlockPos(-62, 79, -36), stairNorth);

        // Pianerottolo d'angolo a (-62, 79, -35): floor Y = 78, ladder su parete a Y = 79 e 80
        setSolid(new BlockPos(-62, 78, -35));
        BlockState ladderEast = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, Direction.EAST);
        setBlock(new BlockPos(-62, 79, -35), ladderEast);
        setBlock(new BlockPos(-62, 80, -35), ladderEast);

        // Rampa 2: scende verso Est (+X)
        BlockState stairWest = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST);
        setBlock(new BlockPos(-61, 78, -35), stairWest);
        setBlock(new BlockPos(-60, 77, -35), stairWest);
        setBlock(new BlockPos(-59, 76, -35), stairWest);
        setBlock(new BlockPos(-58, 75, -35), stairWest);

        // Pianerottolo a fine Rampa 2 a Y = 75 (floor Y = 74)
        setSolid(new BlockPos(-57, 74, -35));

        // Corridoio verso la porta del solaio
        for (int x = -57; x >= -63; x--) {
            setSolid(new BlockPos(x, 74, -36));
        }

        // Porta del solaio a (-64, 75, -36)
        setClosedDoor(new BlockPos(-64, 75, -36), Direction.EAST);

        // Pavimento interno solaio
        setSolid(new BlockPos(-65, 74, -36));

        // Simulazione terrazza aperta complanare alla torre a Y = 81 (distrazione per l'euristica)
        for (int x = -61; x >= -65; x--) {
            for (int z = -40; z >= -42; z--) {
                setSolid(new BlockPos(x, 80, z));
            }
        }

        Vec3 start = new Vec3(-61.5, 81.0, -38.6);
        Waypoint solaioWaypoint = new Waypoint(
                "solaio-1",
                "cas ingresso solaio",
                new BlockPos(-64, 75, -36),
                Identifier.parse("minecraft:overworld"),
                WaypointType.CUSTOM,
                0L
        );

        PathResult result = AutoWalkPathfinder.findPath(level, start, solaioWaypoint, 32);

        assertEquals(PathStatus.FOUND, result.status(), "Deve trovare il percorso dalla torre al solaio");
        assertNotNull(result.path());

        // Il punto di arrivo deve essere antistante la porta a (-63, 75, -36) o all'interno
        BlockPos finalPos = result.path().get(result.path().size() - 1);
        assertTrue(finalPos.equals(new BlockPos(-63, 75, -36)) || finalPos.equals(new BlockPos(-64, 75, -36)),
                "La destinazione finale deve essere sulla soglia del solaio a quota Y = 75");
    }

    @Test
    @DisplayName("Contratto B3: Bilanciamento euristico per dislivelli multi-piano")
    void testCalculateHeuristicVerticalConvergence() {
        BlockPos goal = new BlockPos(-64, 75, -36);

        // Nodo complanare (dy = 0): distanza puramente orizzontale = 4.0
        BlockPos sameFloorNode = new BlockPos(-60, 75, -36);
        assertEquals(4.0, AutoWalkPathfinder.calculateHeuristic(sameFloorNode, goal), 1e-6);

        // Nodo a dislivello lieve (dy = 2 < 4): moltiplicatore 1.5 -> dy_eff = 3.0
        BlockPos slightVerticalNode = new BlockPos(-64, 77, -36);
        assertEquals(3.0, AutoWalkPathfinder.calculateHeuristic(slightVerticalNode, goal), 1e-6);

        // Nodo multi-piano (dy = 6 >= 4): moltiplicatore 2.5 -> dy_eff = 15.0
        BlockPos multiFloorRoofNode = new BlockPos(-64, 81, -36);
        assertEquals(15.0, AutoWalkPathfinder.calculateHeuristic(multiFloorRoofNode, goal), 1e-6);

        // Nodo sulla scala intermedia a Y = 78 (dy = 3): h ~ 6.10
        BlockPos stairNode = new BlockPos(-60, 78, -35);
        double stairH = AutoWalkPathfinder.calculateHeuristic(stairNode, goal);

        // Nodo sulla terrazza a Y = 81 (dy = 6): h ~ 15.42
        BlockPos terraceNode = new BlockPos(-62, 81, -39);
        double terraceH = AutoWalkPathfinder.calculateHeuristic(terraceNode, goal);

        // Il nodo sulla scala DEVE avere un'euristica significativamente più vantaggiosa rispetto alla terrazza
        assertTrue(stairH < terraceH, "Il nodo sulla scala (" + stairH + ") deve essere fortemente preferito rispetto alla terrazza (" + terraceH + ")");
    }

    // =========================================================================
    // Scenario 10: Revisione 5D.7 (Budget 5000, Door Penalty 5.0, L-Stair Turn, Anti-Roof Waypoint)
    // =========================================================================

    @Test
    @DisplayName("Contratto D3: MAX_EXPLORED_NODES è 5000 come budget globale")
    void testMaxExploredNodesIs5000() {
        assertEquals(5000, AutoWalkPathfinder.MAX_EXPLORED_NODES, "Il budget nodi globale deve essere elevato a 5000");
    }

    @Test
    @DisplayName("Contratto D2: CLOSED_DOOR_PENALTY è ricalibrato a 5.0")
    void testClosedDoorPenaltyIs5() {
        assertEquals(5.0, AutoWalkPathfinder.CLOSED_DOOR_PENALTY, 1e-6, "La penalità per porta chiusa deve essere 5.0");
    }

    @Test
    @DisplayName("Contratto D2: Una botola chiusa NON è attraversabile neanche in fallback allowClosedDoors=true")
    void testClosedTrapdoorNotPassableInFallback() {
        BlockPos trapPos = new BlockPos(10, 64, 10);
        BlockState closedTrap = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false);
        setBlock(trapPos, closedTrap);

        assertFalse(AutoWalkPathfinder.isPassable(level, trapPos, false),
                "Botola chiusa non deve essere passabile in Pass 1");
        assertFalse(AutoWalkPathfinder.isPassable(level, trapPos, true),
                "Botola chiusa NON deve essere passabile in Pass 2 (fallback)");

        BlockState openTrap = Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);
        setBlock(trapPos, openTrap);
        assertTrue(AutoWalkPathfinder.isPassable(level, trapPos, true),
                "Botola aperta deve essere passabile");
    }

    @Test
    @DisplayName("Contratto D4: Sanificazione Waypoint esclude rawTargetPos.above() dai goal")
    void testWaypointGoalExcludesAboveTarget() {
        BlockPos wpPos = new BlockPos(-64, 75, -36);
        Waypoint wp = new Waypoint("wp_1", "test_wp", wpPos, Identifier.parse("minecraft:overworld"), WaypointType.CUSTOM, 0L);

        setBlock(wpPos, Blocks.AIR.defaultBlockState());
        setBlock(wpPos.below(), Blocks.STONE.defaultBlockState());
        setBlock(wpPos.above(), Blocks.AIR.defaultBlockState());
        setBlock(wpPos.above(2), Blocks.AIR.defaultBlockState());

        Set<BlockPos> goals = AutoWalkPathfinder.resolveValidGoalPositions(level, wp, wpPos, true);

        assertTrue(goals.contains(wpPos), "I goal del waypoint devono contenere la posizione esatta");
        assertFalse(goals.contains(wpPos.above()), "I goal del waypoint NON devono contenere rawTargetPos.above()");
    }

    @Test
    @DisplayName("Contratto D1: Curva su scala a L riconosciuta in salita e discesa con corridoio esterno libero")
    void testLStairTurnTransition() {
        // Rampa 1 che sale verso ovest: gradino in (-61, 78, -35) facing WEST
        BlockPos stair1 = new BlockPos(-61, 78, -35);
        BlockPos feet1 = new BlockPos(-61, 79, -35);
        BlockState stairState1 = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM);
        setBlock(stair1, stairState1);

        // Rampa 2 che sale verso nord: gradino in (-62, 79, -36) facing NORTH
        BlockPos stair2 = new BlockPos(-62, 79, -36);
        BlockPos feet2 = new BlockPos(-62, 80, -36);
        BlockState stairState2 = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH)
                .setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM);
        setBlock(stair2, stairState2);

        // Corridoio interno: (-61, 79, -36) è un muro di pietra (solido)
        BlockPos innerWall = new BlockPos(-61, 79, -36);
        setSolid(innerWall);
        setSolid(innerWall.above());

        // Corridoio esterno: (-62, 79, -35) è aperto (aria)
        BlockPos outerClear = new BlockPos(-62, 79, -35);
        setBlock(outerClear, Blocks.AIR.defaultBlockState());
        setBlock(outerClear.above(), Blocks.AIR.defaultBlockState());

        // Test 1: Step-Up da feet1 a feet2 (salita a 90° con dislivello +1)
        assertTrue(AutoWalkPathfinder.isLStairTurnTransition(level, feet1, feet2, 1, innerWall, outerClear),
                "La transizione di salita su scala a L deve essere considerata lecita");

        // Test 2: Descent da feet2 a feet1 (discesa a 90° con dislivello -1)
        assertTrue(AutoWalkPathfinder.isLStairTurnTransition(level, feet2, feet1, -1, innerWall, outerClear),
                "La transizione di discesa su scala a L deve essere considerata lecita");

        // Test 3: Rampa rettilinea (stesso asse) non deve essere permessa
        BlockState straightStair = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM);
        setBlock(stair2, straightStair);
        assertFalse(AutoWalkPathfinder.isLStairTurnTransition(level, feet1, feet2, 1, innerWall, outerClear),
                "Due gradini con lo stesso asse (rampa rettilinea) NON devono consentire tagli diagonali");

        // Test 4: Se entrambi i corridoi sono bloccati, la diagonale deve essere rifiutata
        setBlock(stair2, stairState2);
        setSolid(outerClear);
        assertFalse(AutoWalkPathfinder.isLStairTurnTransition(level, feet1, feet2, 1, innerWall, outerClear),
                "Se entrambi i corridoi intermedi sono bloccati, la curva a L deve essere rifiutata");
    }

    @Test
    @DisplayName("Contratto D6.1: LadderBlock è considerato passabile orizzontalmente dal giocatore")
    void testLadderBlockIsPassable() {
        BlockPos ladderPos = new BlockPos(20, 64, 20);
        BlockState ladderState = Blocks.LADDER.defaultBlockState();
        setBlock(ladderPos, ladderState);

        assertTrue(AutoWalkPathfinder.isPassable(level, ladderPos, false),
                "LadderBlock deve essere passabile orizzontalmente in Pass 1 (senza porte)");
        assertTrue(AutoWalkPathfinder.isPassable(level, ladderPos, true),
                "LadderBlock deve essere passabile orizzontalmente in Pass 2 (fallback)");
    }

    @Test
    @DisplayName("Contratto D6.2: LadderBlock non ostruisce lo spazio per la testa del giocatore (isClearHeadroom)")
    void testLadderBlockIsClearHeadroom() {
        BlockPos headPos = new BlockPos(20, 65, 20);
        BlockState ladderState = Blocks.LADDER.defaultBlockState();
        setBlock(headPos, ladderState);

        assertTrue(AutoWalkPathfinder.isClearHeadroom(level, headPos),
                "LadderBlock a quota testa non deve essere considerato un ostacolo di collisione");
    }

    @Test
    @DisplayName("Contratto D6.3: Vietato stazionare sopra un LadderBlock (isStandable = false)")
    void testCannotStandOnLadderBlock() {
        BlockPos feetPos = new BlockPos(20, 64, 20);
        BlockPos belowPos = feetPos.below();

        // Blocco sotto è una scala a pioli
        setBlock(belowPos, Blocks.LADDER.defaultBlockState());
        // Spazio piedi e testa sono aria
        setBlock(feetPos, Blocks.AIR.defaultBlockState());
        setBlock(feetPos.above(), Blocks.AIR.defaultBlockState());

        assertFalse(AutoWalkPathfinder.isStandable(level, feetPos, false),
                "Non deve essere consentito stazionare sopra una scala a pioli (non è calpestabile come pavimento)");
        assertFalse(AutoWalkPathfinder.isStandable(level, feetPos, true),
                "Non deve essere consentito stazionare sopra una scala a pioli neanche in modalità fallback");
    }

    @Test
    @DisplayName("Contratto D6: Curva a L ammessa con scala a pioli (LadderBlock) sull'angolo ortogonale esterno")
    void testLStairTurnTransitionAllowedWithLadderAtCorner() {
        // Rampa 1: gradino (-61, 78, -35) facing WEST
        BlockPos stair1 = new BlockPos(-61, 78, -35);
        BlockPos feet1 = new BlockPos(-61, 79, -35);
        BlockState stairState1 = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM);
        setBlock(stair1, stairState1);

        // Rampa 2: gradino (-62, 79, -36) facing NORTH
        BlockPos stair2 = new BlockPos(-62, 79, -36);
        BlockPos feet2 = new BlockPos(-62, 80, -36);
        BlockState stairState2 = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH)
                .setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM);
        setBlock(stair2, stairState2);

        // Corridoio interno: (-61, 79, -36) è solido (muro di pietra)
        BlockPos innerWall = new BlockPos(-61, 79, -36);
        setSolid(innerWall);
        setSolid(innerWall.above());

        // Corridoio esterno: (-62, 79, -35) presenta una scala a pioli a parete (LadderBlock)
        BlockPos outerWithLadder = new BlockPos(-62, 79, -35);
        setBlock(outerWithLadder, Blocks.LADDER.defaultBlockState());
        setBlock(outerWithLadder.above(), Blocks.AIR.defaultBlockState());

        // La transizione deve ora avere successo sia in salita che in discesa
        assertTrue(AutoWalkPathfinder.isLStairTurnTransition(level, feet1, feet2, 1, innerWall, outerWithLadder),
                "La curva a L in salita deve essere permessa con LadderBlock sull'angolo esterno");
        assertTrue(AutoWalkPathfinder.isLStairTurnTransition(level, feet2, feet1, -1, innerWall, outerWithLadder),
                "La curva a L in discesa deve essere permessa con LadderBlock sull'angolo esterno");
    }
}

