# Changelog

All notable changes to the **Minecraft Access** project (Fork & Community Enhancements) will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

### 🌟 Added

#### Movement, Navigation & Waypoints
- **AutoWalk & Pathfinding Engine**: Integrated assisted auto-walking system with real-time pathfinding calculations (`AutoWalkController`, `AutoWalkPathfinder`, `AutoWalkManager`).
- **POI & Custom Waypoint Management**: Complete waypoint creation and tracking system with dedicated accessible GUIs (`ManageWaypointsScreen`, `SaveWaypointScreen`, `POIWaypoints`, `WaypointUtils`).

#### Interactive Onboarding, Novice Academy & Contextual Mentor
- **PlayerContextEngine**: Real-time snapshot engine continuously sampling player environment, vital stats, game mode, and environmental threats.
- **HelpNarrator & Speech Priority Shield**: Priority-based shield protecting pedagogical instructions from being cut off by regular scanner chatter, with distinctive auditory feedback.
- **Novice Academy**: Guided interactive tutorial missions covering basic movement, spatial orientation, wood harvesting, crafting, and basic combat with mode guardrails.
- **Contextual Mentor**: Proactive mentor delivering gentle audio cues during idle situations, wall collisions, darkness, and low hunger.
- **QuickHelp**: Tabbed instant key reference accessible from the Access Menu or `F1`.

#### Ergonomic 4-Layer Numpad Controls (Zero-Shift)
- Completely eliminated `Shift` key combinations to prevent accidental crouching and movement slowdown.
- **Layer 0 (Direct)**: Attack/Mine (`0`), Use/Place/Eat (`Enter`), Quick Status (`.`), Center Horizon (`5`), Pick Block (`+`), Unlock (`-`).
- **Layer 1 (`Ctrl + Numpad`)**: 8-way compass snap, Player XYZ coordinates (`Ctrl + 5`), Target block/entity coordinates (`Ctrl + .`), 180° look behind (`Ctrl + 0`), POI tracker navigation.
- **Layer 2 (`Alt + Numpad`)**: Full compass heading & pitch (`Alt + 5`), equipment inspection, Nadir/Zenith vertical look, AutoWalk toggle.
- **Layer 3 (Directional Path Scanner)**: Step-by-step path probing with farmland/crop stage identification.

#### Spatial Audio, Acoustic Occlusion & Compass
- **360° Continuous Acoustic Compass**: Dynamic heading calculation ($0^\circ \dots 359^\circ$) with modulated frequency/pitch audio feedback for cardinal, intercardinal, and intermediate angles.
- **5-Tier Material-Based Acoustic Occlusion**: Real-time volumetric raycast sound damping through materials (Wood, Stone, Glass, Metal, Foliage) and directional "behind wall" voice notices.
- **Dynamic Footstep Scaling & Proprioception**: Local player footstep sound level scaling and on-the-fly adjustment hotkeys (`Alt+PageUp` / `Alt+PageDown`).

#### Tactical Awareness & Crosshair Coordination
- **Survival Resource Tracker (`Alt+B` / `Alt+Numpad 7`)**: Instant auditory tally of critical survival inventory (food, weapons, tools, solid building blocks, ammunition) with modifier key isolation.
- **Fluid Fall Safety & Threat Sentinel**: Enhanced `FallDetector` supporting fluid/lava/water boundary safety, proximity warnings for approaching hostile mobs, and door proximity navigation cues.
- **Crosshair Feedback Manager (`CrosshairFeedbackManager`)**: Modular token-based coordinator providing atomic announcements of voxel coordinates, cardinal orientation, block properties, and light levels.

#### Advanced Container & Recipe Book GUI Navigation
- **Universal 4-Arrow Keys Grid Navigation**: Native arrow-key navigation across all container screens (`AbstractContainerScreen`) with automatic text-box (`EditBox`) input decoupling.
- **Recipe Book Category Navigation (`V` / `Shift+V`)**: Audible button click, localized category speech (*"Construction"*, *"Equipment"*, *"Miscellaneous"*, *"Mechanisms and Redstone"*), and auto-focusing the first recipe.
- **Recipe Book Page Turning (`Shift+I` / `Shift+K`)**: Page boundary detection with contextual feedback (*"First page"*, *"Last page"*, *"Single page"*).
- **Crafting Statistics & Grammar Concordance**: Page summary tally (`[T] recipes: [R] craftable, [N] uncraftable`) with dynamic singular/plural grammatical agreement in Italian and English.
- **Specialized Container Event Listeners & Auto-Focus**: Added dynamic feedback and auto-focus for Stonecutter (`StonecutterScreen` — auto-focus on recipe grid), Loom (`LoomScreen` — pattern count announcement and auto-focus on patterns), Furnaces (`AbstractFurnaceMenu` — discrete smelting completion notice), and Brewing Stand (`BrewingStandMenu` — brewing completion notice).

---

### 🐞 Fixed
- **Recipe Book ClassCastException**: Resolved crash when switching tabs with `V` / `Shift+V` in Minecraft 26.2.
- **AutoConfig Reflection Warning**: Excluded static singleton `Config.instance` from AutoConfig GUI provider via `@ConfigEntry.Gui.Excluded`.
- **Inventory Defensive Null Checks**: Added null guards on slot groups to eliminate spurious NullPointerExceptions.
- **Mouse Simulation State Machine**: Fixed continuous mining/placing on numpad keys via `wasDown()`.
- **CI/CD Strict JSON Sorting**: Guaranteed alphabetical key ordering in `it_it.json` and `en_us.json` for automated GitHub workflows.
