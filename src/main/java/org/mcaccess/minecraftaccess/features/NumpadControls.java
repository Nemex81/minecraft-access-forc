package org.mcaccess.minecraftaccess.features;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyConflictContext;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.blay09.mods.kuma.api.ManagedKeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.WorldNarrator;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.NarrationPriority;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;
import org.mcaccess.minecraftaccess.utils.PlayerUtils;
import org.mcaccess.minecraftaccess.utils.position.Orientation;
import org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils;
import org.mcaccess.minecraftaccess.utils.system.MouseUtils;

/**
 * Numpad Controls: Provides a tactile, single-hand 4-layer control console
 * using the numeric keypad for blind players in Minecraft Access.
 */
@Slf4j
public class NumpadControls implements BalmClientModule {
    private static final float DEGREES_PER_MOUSE_DELTA = 0.15f;
    private static final long CONTINUOUS_HOLD_DELAY_MS = 200; // ms before smooth hold rotation engages

    // Look key mappings for continuous hold
    private ManagedKeyMapping keyLookUp;
    private ManagedKeyMapping keyLookDown;
    private ManagedKeyMapping keyLookLeft;
    private ManagedKeyMapping keyLookRight;
    private ManagedKeyMapping keyLookUpLeft;
    private ManagedKeyMapping keyLookUpRight;
    private ManagedKeyMapping keyLookDownLeft;
    private ManagedKeyMapping keyLookDownRight;

    private long holdStartLookUp = 0;
    private long holdStartLookDown = 0;
    private long holdStartLookLeft = 0;
    private long holdStartLookRight = 0;
    private long holdStartLookUpLeft = 0;
    private long holdStartLookUpRight = 0;
    private long holdStartLookDownLeft = 0;
    private long holdStartLookDownRight = 0;
    private Orientation lastContinuousFacing = null;
    private boolean wasContinuouslyRotating = false;

    // Mouse button key mappings for tick-based hold detection
    private ManagedKeyMapping keyLeftClick;
    private ManagedKeyMapping keyRightClick;
    private ManagedKeyMapping keyMiddleClick;

    private long lastScrollTime = 0;

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad_controls");
    }

    @Override
    public void initialize() {
        ClientTickCallback.AFTER.register(this::tick);

        registerLayer0Direct();
        registerLayer1Shift();
        registerLayer2Control();
        registerLayer3Alt();
    }

    // =========================================================================
    // LAYER 0: DIRECT NUMPAD (Camera, Mouse, Hotbar & Basic Actions)
    // =========================================================================
    private void registerLayer0Direct() {
        // Camera Look Keys
        keyLookUp = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_up"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_8))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    if (holdStartLookUp == 0) {
                        holdStartLookUp = System.currentTimeMillis();
                        rotateCameraBy(0, -1, false);
                    }
                    return true;
                })
                .build();

        keyLookDown = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_down"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_2))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    if (holdStartLookDown == 0) {
                        holdStartLookDown = System.currentTimeMillis();
                        rotateCameraBy(0, 1, false);
                    }
                    return true;
                })
                .build();

        keyLookLeft = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_left"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_4))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    if (holdStartLookLeft == 0) {
                        holdStartLookLeft = System.currentTimeMillis();
                        rotateCameraBy(-1, 0, false);
                    }
                    return true;
                })
                .build();

        keyLookRight = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_right"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_6))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    if (holdStartLookRight == 0) {
                        holdStartLookRight = System.currentTimeMillis();
                        rotateCameraBy(1, 0, false);
                    }
                    return true;
                })
                .build();

        // 2D Diagonals
        keyLookUpLeft = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_up_left"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_7))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    if (holdStartLookUpLeft == 0) {
                        holdStartLookUpLeft = System.currentTimeMillis();
                        rotateCameraBy(-1, -1, false);
                    }
                    return true;
                })
                .build();

        keyLookUpRight = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_up_right"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_9))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    if (holdStartLookUpRight == 0) {
                        holdStartLookUpRight = System.currentTimeMillis();
                        rotateCameraBy(1, -1, false);
                    }
                    return true;
                })
                .build();

        keyLookDownLeft = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_down_left"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_1))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    if (holdStartLookDownLeft == 0) {
                        holdStartLookDownLeft = System.currentTimeMillis();
                        rotateCameraBy(-1, 1, false);
                    }
                    return true;
                })
                .build();

        keyLookDownRight = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_down_right"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_3))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    if (holdStartLookDownRight == 0) {
                        holdStartLookDownRight = System.currentTimeMillis();
                        rotateCameraBy(1, 1, false);
                    }
                    return true;
                })
                .build();

        // Center / Narrate Crosshair
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.center_crosshair"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_5))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    centerCameraHorizon();
                    narrateCrosshairTarget();
                    return true;
                })
                .build();

        // Narrate Facing
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.narrate_facing"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_0))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    String h = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
                    String v = PlayerPositionUtils.getVerticalFacingDirectionInWords();
                    MainClass.narrate(I18n.get("minecraft_access.other.facing_direction", h + (v != null ? ", " + v : "")), true);
                    return true;
                })
                .build();

        // Snap to nearest horizontal cardinal direction
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.snap_cardinal"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_DECIMAL))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    snapToNearestCardinal();
                    return true;
                })
                .build();

        // Mouse Action Keys
        keyLeftClick = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.mouse.left_click"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ADD))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .withContext(KeyConflictContext.UNIVERSAL)
                .build();

        keyRightClick = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.mouse.right_click"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ENTER))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .withContext(KeyConflictContext.UNIVERSAL)
                .build();

        // Unlock
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.action.unlock"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_SUBTRACT))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    if (MainClass.poiManager != null && MainClass.poiManager.lockingHandler != null) {
                        if (MainClass.poiManager.lockingHandler.isPlayerLocked()) {
                            MainClass.poiManager.lockingHandler.unlock(true, true);
                        }
                    }
                    return true;
                })
                .build();

        // Hotbar Scroll
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.hotbar.scroll_prev"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_DIVIDE))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    scrollHotbar(true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.hotbar.scroll_next"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_MULTIPLY))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    scrollHotbar(false);
                    return true;
                })
                .build();
    }

    // =========================================================================
    // LAYER 1: SHIFT + NUMPAD (POI Radar, Object Tracker & Scanning)
    // =========================================================================
    private void registerLayer1Shift() {
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.item_prev"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_8, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.moveObject(-1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.item_next"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_2, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.moveObject(1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.group_prev"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_4, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.moveGroup(-1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.group_next"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_6, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.moveGroup(1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.look_at_current_object"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_5, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.lookAtCurrentObject();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.target_nearest_any"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_0, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.targetNearestAny();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.target_nearest_entity"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_1, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.targetNearestEntity();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.target_nearest_block"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_3, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.targetNearestBlock();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.lock_target"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ENTER, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null || MainClass.poiManager.lockingHandler == null) return false;
                    MainClass.poiManager.lockingHandler.relock();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.mark_target"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_DECIMAL, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null || MainClass.poiManager.poiMarking == null) return false;
                    MainClass.poiManager.poiMarking.mark();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.unmark_target"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_SUBTRACT, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.poiManager == null || MainClass.poiManager.poiMarking == null) return false;
                    MainClass.poiManager.poiMarking.unmark();
                    return true;
                })
                .build();
    }

    // =========================================================================
    // LAYER 2: CTRL + NUMPAD (Absolute Orientation & Compass Snap)
    // =========================================================================
    private void registerLayer2Control() {
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.north"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_8, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraTo(Orientation.NORTH, true);
                    playSnapSound(1.0f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.east"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_6, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraTo(Orientation.EAST, true);
                    playSnapSound(1.2f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.south"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_2, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraTo(Orientation.SOUTH, true);
                    playSnapSound(0.8f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.west"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_4, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraTo(Orientation.WEST, true);
                    playSnapSound(0.6f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.north_west"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_7, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraTo(Orientation.NORTH_WEST, true);
                    playSnapSound(0.9f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.north_east"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_9, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraTo(Orientation.NORTH_EAST, true);
                    playSnapSound(1.1f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.south_west"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_1, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraTo(Orientation.SOUTH_WEST, true);
                    playSnapSound(0.7f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.south_east"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_3, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraTo(Orientation.SOUTH_EAST, true);
                    playSnapSound(0.95f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.look_behind"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_5, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    Orientation current = PlayerPositionUtils.getHorizontalFacing();
                    rotateCameraTo(current.getOpposite(), true);
                    playSnapSound(0.5f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.narrate_coordinates"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_0, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    MainClass.narrate(PlayerPositionUtils.getNarratableXYZPosition(), true);
                    return true;
                })
                .build();

        keyMiddleClick = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.mouse.middle_click"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ENTER, KeyModifiers.of(KeyModifier.CONTROL)))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .withContext(KeyConflictContext.UNIVERSAL)
                .build();
    }

    // =========================================================================
    // LAYER 3: ALT + NUMPAD (HUD, Status & Environment)
    // =========================================================================
    private void registerLayer3Alt() {
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.player_all"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_5, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    PlayerStatus.narratePlayerStatus(false);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.mainhand"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_8, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    narrateHandItem(false);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.offhand"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_2, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    narrateHandItem(true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.effects"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_4, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    narrateActiveEffects();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.durability"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_6, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    narrateHeldItemDurability();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.access_menu"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ENTER, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    Minecraft client = Minecraft.getInstance();
                    client.gui.setScreen(new AccessMenu.GUI());
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.bossbar_next"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ADD, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    HUDStatus.narrateBossBars(false);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.bossbar_prev"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_SUBTRACT, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    HUDStatus.narrateBossBars(true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_nadir"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_1, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraToPitch(90.0f, true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_zenith"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_3, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled()) return false;
                    rotateCameraToPitch(-90.0f, true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.action.auto_walk"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_0, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.autoWalkManager == null) return false;
                    MainClass.autoWalkManager.toggleAutoWalk();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.action.auto_walk_sprint"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_DECIMAL, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || MainClass.autoWalkManager == null) return false;
                    MainClass.autoWalkManager.toggleSprint();
                    return true;
                })
                .build();
    }

    // =========================================================================
    // TICK & CONTINUOUS HOLD HANDLING
    // =========================================================================
    private void tick(Minecraft client) {
        if (isDisabled() || client.gui.screen() != null) {
            resetHoldTimers();
            return;
        }

        long now = System.currentTimeMillis();
        Config.NumpadControls config = Config.getInstance().numpadControls;

        // Continuous Camera Rotation when holding keys (without Shift/Ctrl/Alt)
        if (config.continuousRotation) {
            boolean hasModifiers = hasAnyModifierDown(client);
            if (!hasModifiers) {
                // Look Up (Numpad 8)
                if (keyLookUp != null && keyLookUp.isDown()) {
                    if (holdStartLookUp == 0) holdStartLookUp = now;
                    if (now - holdStartLookUp >= CONTINUOUS_HOLD_DELAY_MS) {
                        rotateCameraContinuous(0, -1, false, config.continuousRotationSpeed);
                    }
                } else {
                    holdStartLookUp = 0;
                }

                // Look Down (Numpad 2)
                if (keyLookDown != null && keyLookDown.isDown()) {
                    if (holdStartLookDown == 0) holdStartLookDown = now;
                    if (now - holdStartLookDown >= CONTINUOUS_HOLD_DELAY_MS) {
                        rotateCameraContinuous(0, 1, false, config.continuousRotationSpeed);
                    }
                } else {
                    holdStartLookDown = 0;
                }

                // Look Left (Numpad 4)
                if (keyLookLeft != null && keyLookLeft.isDown()) {
                    if (holdStartLookLeft == 0) holdStartLookLeft = now;
                    if (now - holdStartLookLeft >= CONTINUOUS_HOLD_DELAY_MS) {
                        rotateCameraContinuous(-1, 0, false, config.continuousRotationSpeed);
                    }
                } else {
                    holdStartLookLeft = 0;
                }

                // Look Right (Numpad 6)
                if (keyLookRight != null && keyLookRight.isDown()) {
                    if (holdStartLookRight == 0) holdStartLookRight = now;
                    if (now - holdStartLookRight >= CONTINUOUS_HOLD_DELAY_MS) {
                        rotateCameraContinuous(1, 0, false, config.continuousRotationSpeed);
                    }
                } else {
                    holdStartLookRight = 0;
                }

                // Look Up-Left (Numpad 7)
                if (keyLookUpLeft != null && keyLookUpLeft.isDown()) {
                    if (holdStartLookUpLeft == 0) holdStartLookUpLeft = now;
                    if (now - holdStartLookUpLeft >= CONTINUOUS_HOLD_DELAY_MS) {
                        rotateCameraContinuous(-1, -1, false, config.continuousRotationSpeed);
                    }
                } else {
                    holdStartLookUpLeft = 0;
                }

                // Look Up-Right (Numpad 9)
                if (keyLookUpRight != null && keyLookUpRight.isDown()) {
                    if (holdStartLookUpRight == 0) holdStartLookUpRight = now;
                    if (now - holdStartLookUpRight >= CONTINUOUS_HOLD_DELAY_MS) {
                        rotateCameraContinuous(1, -1, false, config.continuousRotationSpeed);
                    }
                } else {
                    holdStartLookUpRight = 0;
                }

                // Look Down-Left (Numpad 1)
                if (keyLookDownLeft != null && keyLookDownLeft.isDown()) {
                    if (holdStartLookDownLeft == 0) holdStartLookDownLeft = now;
                    if (now - holdStartLookDownLeft >= CONTINUOUS_HOLD_DELAY_MS) {
                        rotateCameraContinuous(-1, 1, false, config.continuousRotationSpeed);
                    }
                } else {
                    holdStartLookDownLeft = 0;
                }

                // Look Down-Right (Numpad 3)
                if (keyLookDownRight != null && keyLookDownRight.isDown()) {
                    if (holdStartLookDownRight == 0) holdStartLookDownRight = now;
                    if (now - holdStartLookDownRight >= CONTINUOUS_HOLD_DELAY_MS) {
                        rotateCameraContinuous(1, 1, false, config.continuousRotationSpeed);
                    }
                } else {
                    holdStartLookDownRight = 0;
                }

                if (holdStartLookUp == 0 && holdStartLookDown == 0 && holdStartLookLeft == 0 && holdStartLookRight == 0
                        && holdStartLookUpLeft == 0 && holdStartLookUpRight == 0 && holdStartLookDownLeft == 0 && holdStartLookDownRight == 0) {
                    checkContinuousRotationRelease();
                }
            } else {
                resetHoldTimers();
            }
        }

        // Continuous Mouse Hold Handling
        if (config.enableContinuousHold) {
            if (keyLeftClick != null) {
                if (keyLeftClick.isDown() && !keyLeftClick.wasDown()) {
                    MouseUtils.Key.LEFT.press();
                } else if (!keyLeftClick.isDown() && keyLeftClick.wasDown()) {
                    MouseUtils.Key.LEFT.release();
                }
            }

            if (keyRightClick != null) {
                if (keyRightClick.isDown() && !keyRightClick.wasDown()) {
                    MouseUtils.Key.RIGHT.press();
                } else if (!keyRightClick.isDown() && keyRightClick.wasDown()) {
                    MouseUtils.Key.RIGHT.release();
                }
            }

            if (keyMiddleClick != null) {
                if (keyMiddleClick.isDown() && !keyMiddleClick.wasDown()) {
                    MouseUtils.Key.MIDDLE.press();
                } else if (!keyMiddleClick.isDown() && keyMiddleClick.wasDown()) {
                    MouseUtils.Key.MIDDLE.release();
                }
            }
        }
    }

    private void resetHoldTimers() {
        holdStartLookUp = 0;
        holdStartLookDown = 0;
        holdStartLookLeft = 0;
        holdStartLookRight = 0;
        holdStartLookUpLeft = 0;
        holdStartLookUpRight = 0;
        holdStartLookDownLeft = 0;
        holdStartLookDownRight = 0;
        checkContinuousRotationRelease();
    }

    private void checkContinuousRotationRelease() {
        if (wasContinuouslyRotating) {
            wasContinuouslyRotating = false;
            lastContinuousFacing = null;
            if (Config.getInstance().numpadControls.narrateFacingOnChange) {
                NarrationPriority.suppressBackgroundScanners(350);
                MainClass.narrate(PlayerPositionUtils.getHorizontalFacingDirectionInWords(), true);
            }
        }
    }

    private boolean hasAnyModifierDown(Minecraft client) {
        long window = client.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    // =========================================================================
    // HELPER & UTILITY METHODS
    // =========================================================================
    private boolean isDisabled() {
        return !Config.getInstance().numpadControls.enabled;
    }

    private void rotateCameraBy(int horizontalWeight, int verticalWeight, boolean isModified) {
        if (handleLocking()) return;
        Config.NumpadControls config = Config.getInstance().numpadControls;
        float baseAngle = isModified ? config.modifiedRotatingAngle : config.normalRotatingAngle;
        float angle = baseAngle / DEGREES_PER_MOUSE_DELTA;

        if (config.invertYAxis) {
            verticalWeight = -verticalWeight;
        }

        float deltaH = angle * horizontalWeight;
        float deltaV = angle * verticalWeight;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (!isModified && Math.signum(player.getXRot()) * Math.signum(player.getXRot() + deltaV * DEGREES_PER_MOUSE_DELTA) < 0) {
            player.turn(deltaH, 0);
            player.setXRot(0.0f);
            player.xRotO = 0.0f;
        } else {
            player.turn(deltaH, deltaV);
        }

        if (config.narrateFacingOnChange) {
            if (horizontalWeight != 0 && verticalWeight != 0) {
                String h = PlayerPositionUtils.getHorizontalFacingDirectionInWords();
                String v = PlayerPositionUtils.getVerticalFacingDirectionInWords();
                MainClass.narrate(h + (v != null ? ", " + v : ""), true);
            } else if (horizontalWeight != 0) {
                MainClass.narrate(PlayerPositionUtils.getHorizontalFacingDirectionInWords(), true);
            } else if (verticalWeight != 0) {
                String v = PlayerPositionUtils.getVerticalFacingDirectionInWords();
                if (v != null) MainClass.narrate(v, true);
            }
        }
    }

    private void rotateCameraContinuous(int horizontalWeight, int verticalWeight, boolean isModified, float speedMultiplier) {
        if (handleLocking()) return;
        Config.NumpadControls config = Config.getInstance().numpadControls;
        float baseContinuousAngle = (isModified ? 2.5f : 4.5f) * speedMultiplier;
        float angle = baseContinuousAngle / DEGREES_PER_MOUSE_DELTA;

        if (config.invertYAxis) {
            verticalWeight = -verticalWeight;
        }

        float deltaH = angle * horizontalWeight;
        float deltaV = angle * verticalWeight;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        wasContinuouslyRotating = true;

        // Suppress background ambient scanners (crosshair & obstacles) during continuous rotation
        NarrationPriority.suppressBackgroundScanners(350);

        player.turn(deltaH, deltaV);

        if (horizontalWeight != 0 && config.continuousFeedbackMode != Config.NumpadControls.ContinuousFeedbackMode.OFF) {
            Orientation currentFacing = PlayerPositionUtils.getHorizontalFacing();
            if (currentFacing != lastContinuousFacing) {
                lastContinuousFacing = currentFacing;
                boolean isCardinal = currentFacing == Orientation.NORTH
                        || currentFacing == Orientation.EAST
                        || currentFacing == Orientation.SOUTH
                        || currentFacing == Orientation.WEST;

                // Sound feedback (hat sound at 45° sectors, higher pitch on 4 cardinal points)
                if (config.continuousFeedbackMode == Config.NumpadControls.ContinuousFeedbackMode.SOUND_ONLY
                        || config.continuousFeedbackMode == Config.NumpadControls.ContinuousFeedbackMode.SOUND_AND_VOICE) {
                    float pitch = isCardinal ? 1.2f : 0.9f;
                    player.level().playLocalSound(player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.35f * config.audioCueVolume, pitch, false);
                }

                // Voice feedback during rotation (only on major 4 cardinal points to avoid speech truncation)
                if ((config.continuousFeedbackMode == Config.NumpadControls.ContinuousFeedbackMode.VOICE_ONLY
                        || config.continuousFeedbackMode == Config.NumpadControls.ContinuousFeedbackMode.SOUND_AND_VOICE)
                        && isCardinal) {
                    MainClass.narrate(PlayerPositionUtils.getHorizontalFacingDirectionInWords(), true);
                }
            }
        }
    }

    private void rotateCameraTo(Orientation direction, boolean narrateChange) {
        if (handleLocking()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Vec3 playerBlockPosition = player.position();
        Vec3 targetBlockPosition = playerBlockPosition.add(Vec3.atLowerCornerOf(direction.vector));
        player.lookAt(EntityAnchorArgument.Anchor.FEET, targetBlockPosition);

        if (narrateChange && Config.getInstance().numpadControls.narrateFacingOnChange) {
            if (direction.in(Orientation.Layer.MIDDLE)) {
                MainClass.narrate(PlayerPositionUtils.getHorizontalFacingDirectionInWords(), true);
            } else {
                MainClass.narrate(PlayerPositionUtils.getVerticalFacingDirectionInWords(), true);
            }
        }
    }

    private void rotateCameraToPitch(float pitchDegrees, boolean narrateChange) {
        if (handleLocking()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        player.setXRot(pitchDegrees);
        player.xRotO = pitchDegrees;

        if (narrateChange && Config.getInstance().numpadControls.narrateFacingOnChange) {
            String v = PlayerPositionUtils.getVerticalFacingDirectionInWords();
            if (v != null) MainClass.narrate(v, true);
        }
    }

    private void centerCameraHorizon() {
        if (handleLocking()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        player.setXRot(0.0f);
        player.xRotO = 0.0f;
    }

    private void snapToNearestCardinal() {
        if (handleLocking()) return;
        Orientation o = PlayerPositionUtils.getHorizontalFacing();
        rotateCameraTo(o, true);
        playSnapSound(1.0f);
    }

    private void scrollHotbar(boolean scrollUp) {
        long now = System.currentTimeMillis();
        int delay = Config.getInstance().numpadControls.scrollDelayMilliseconds;
        if (now - lastScrollTime >= delay) {
            if (scrollUp) {
                MouseUtils.Wheel.UP.scroll();
            } else {
                MouseUtils.Wheel.DOWN.scroll();
            }
            lastScrollTime = now;
        }
    }

    private void narrateCrosshairTarget() {
        HitResult hit = PlayerUtils.crosshairTarget(20.0);
        if (hit == null) return;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos blockPos = blockHit.getBlockPos();
            String narration = MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(blockPos)
                    + I18n.get("minecraft_access.other.words_connection")
                    + NarrationUtils.narrateRelativePositionOfPlayerAnd(blockPos);
            MainClass.narrate(narration, false);
        }
    }

    private void narrateHandItem(boolean isOffhand) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) return;

        String hand = isOffhand ? I18n.get("minecraft_access.other.offhand") : I18n.get("options.mainHand");
        ItemStack heldItem = isOffhand ? player.getOffhandItem() : player.getMainHandItem();

        String name = getItemName(heldItem);
        int count = heldItem.getCount();
        String fullName = (count > 1 && !heldItem.isEmpty()) ? count + " " + name : name;
        MainClass.narrate("%s: %s".formatted(hand, fullName), true);
    }

    private void narrateActiveEffects() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            MainClass.narrate(I18n.get("minecraft_access.effect_narration.no_effects"), true);
            return;
        }
        String narration = effects.stream().map(NarrationUtils::narrateEffect)
                .collect(Collectors.joining(I18n.get("minecraft_access.other.words_connection")));
        MainClass.narrate(narration, true);
    }

    private void narrateHeldItemDurability() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack item = player.getMainHandItem();
        if (item.isEmpty() || !item.isDamageableItem()) {
            MainClass.narrate(I18n.get("minecraft_access.status.durability.no_item"), true);
            return;
        }
        int max = item.getMaxDamage();
        int current = max - item.getDamageValue();
        MainClass.narrate(I18n.get("minecraft_access.status.durability", current, max), true);
    }

    private String getItemName(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return I18n.get("minecraft_access.inventory_controls.empty_slot", "");
        }
        StringBuilder itemName = new StringBuilder();
        itemName.append(itemStack.getHoverName().getString());
        Optional.ofNullable(itemStack.get(DataComponents.JUKEBOX_PLAYABLE))
                .flatMap(jukeboxPlayable -> jukeboxPlayable.song().unwrapKey())
                .ifPresent(discNumber -> itemName.append(' ').append(I18n.get("jukebox_song.minecraft." + discNumber.identifier().getPath())));
        return itemName.toString();
    }

    private void playSnapSound(float pitch) {
        if (!Config.getInstance().numpadControls.playCardinalSnapSound) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().level != null) {
            float volume = Config.getInstance().numpadControls.audioCueVolume;
            Minecraft.getInstance().level.playLocalSound(
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_HAT.value(),
                    SoundSource.PLAYERS,
                    volume, pitch, false
            );
        }
    }

    private boolean handleLocking() {
        if (Minecraft.getInstance().getCameraEntity() == null || Minecraft.getInstance().player == null) {
            return false;
        }
        if (MainClass.poiManager != null && MainClass.poiManager.lockingHandler != null) {
            if (MainClass.poiManager.lockingHandler.isPlayerLocked() || !Minecraft.getInstance().getCameraEntity().is(Minecraft.getInstance().player)) {
                MainClass.narrate(I18n.get("minecraft_access.other.camera_locked"), true);
                return true;
            }
        }
        return false;
    }
}
