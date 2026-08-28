# Minecraft Access — Architecture & Technical Design

This document details the high-level software architecture, component relationships, and lifecycle design of the `minecraft-access` mod for Minecraft 26.2 (Java 25, Fabric & NeoForge).

---

## 1. System Overview & Technology Stack

The mod is engineered as a unified multi-loader accessibility bridge:
- **Build System**: Architectury Loom + Gradle (`shadowJar`).
- **Mod Abstraction Layer**: Balm (multi-platform abstraction layer for Fabric & NeoForge).
- **Bytecode Instrumentation**: SpongePowered Mixin.
- **Screen Reader Interfacing**: Tolk native library bridge (NVDA / SAPI proxy) wrapped in `MainClass.narrate`.
- **Positional Audio Engine**: Minecraft OpenAL 3D Sound Engine.

---

## 2. Core Architectural Subsystems

### A. Screen Reader Proxy & Narration Pipeline (`MainClass.java`)
- Acts as the central gateway for all vocal output.
- Dispatches speech events to Tolk / NVDA with strict 3-tier prioritization:
  1. `Level 1 (Critical Hazard)`: `interrupt = true` (lava, cliffs, combat).
  2. `Level 2 (Navigation / GUI)`: `interrupt = false` (arrival chimes, crafting slots, waypoint screen).
  3. `Level 3 (Ambient Exploration)`: Debounced (150–300ms crosshair hovering).

### B. AutoWalk & Pathfinder Subsystem (`features/autowalk/`)
- Computes real-time vector navigation towards 3D Waypoints.
- Enforces smooth directional panning ($20^\circ$/tick) to avoid FOV jitter.
- Integrates anti-chattering sprint hysteresis and cliff edge safety checks.

### C. Points of Interest & Waypoints (`features/point_of_interest/`)
- Manages persistent spatial anchors saved in JSON format under `config/minecraft-access/waypoints/`.
- Provides 3D positional audio beacons with logarithmic distance attenuation.

### D. Keyboard Navigation & Numpad Controller (`features/NumpadControls.java`)
- Provides an 8-direction radial scanner (`8, 2, 4, 6, 7, 9, 1, 3`).
- Altimetric telemetry: `+` (eye level), `-` (jump/head level), `5` (feet level).
- Coordinate telemetry: `0` (XYZ readout), `.` (reset).

### E. Obstacle & Fall Detector (`features/ObstacleDetector.java` & `FallDetector.java`)
- Raycasts $0.1\text{m} \le d \le 5.0\text{m}$ in front of the player.
- Step model:
  - $\Delta Y \le 0.60\text{m}$: Continuous path / auto-step (silent).
  - $0.60\text{m} < \Delta Y \le 1.20\text{m}$: Jumpable step (`STEP_CLIMBABLE`).
  - $\Delta Y > 1.20\text{m}$: Wall / barrier (`WALL`).
- Corner pinching: $45^\circ$ rays check orthogonal neighbours to respect the player's $0.6\text{m}$ hitbox.

### F. Inventory & Recipe Book Mixins (`mixin/RecipeButtonMixin.java`)
- Mixin injections into vanilla screens (`AbstractContainerScreen`, `RecipeBookComponent`).
- Prevents recipe button focus locking on the `X` key inspection.

---

## 3. Data Flow & Event Lifecycle

```
[Player Input / Keyboard]
       │
       ▼
[KeyBinding Handler] ────► [Feature Subsystem (AutoWalk, Numpad, POI)]
                                   │
                                   ├──► [Raycasting & Voxel World Query]
                                   │
                                   ▼
                       [MainClass.narrate / Audio 3D]
                                   │
                                   ├──► [Tolk DLL / NVDA Screen Reader]
                                   └──► [OpenAL Sound Engine]
```