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
import org.mcaccess.minecraftaccess.features.crosshair.CrosshairFeedbackManager;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.ModifierUtils;
import org.mcaccess.minecraftaccess.utils.NarrationPriority;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;
import org.mcaccess.minecraftaccess.utils.PlayerUtils;
import org.mcaccess.minecraftaccess.utils.position.Orientation;
import org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils;
import org.mcaccess.minecraftaccess.utils.system.MouseUtils;

/**
 * Numpad Controls: Provides a tactile, single-hand 3-layer control console
 * using the numeric keypad for blind players in Minecraft Access (Zero Shift modifier).
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
        registerLayer2Control();
        registerLayer3Alt();
    }

    // =========================================================================
    // LAYER 0: DIRECT NUMPAD (Camera, Actions, Player Status & Hotbar)
    // =========================================================================
    private void registerLayer0Direct() {
        // Camera Look Keys
        keyLookUp = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_up"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_8))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
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
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
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
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
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
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
                    if (holdStartLookRight == 0) {
                        holdStartLookRight = System.currentTimeMillis();
                        rotateCameraBy(1, 0, false);
                    }
                    return true;
                })
                .build();

        keyLookUpLeft = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_up_left"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_7))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
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
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
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
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
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
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
                    if (holdStartLookDownRight == 0) {
                        holdStartLookDownRight = System.currentTimeMillis();
                        rotateCameraBy(1, 1, false);
                    }
                    return true;
                })
                .build();

        // Center / Narrate Crosshair (with multimodal sound and optional voice feedback)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.center_crosshair"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_5))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
                    centerCameraHorizon();
                    Config.NumpadControls.CenterHorizonFeedbackMode mode = Config.getInstance().numpadControls.centerHorizonFeedbackMode;
                    if (mode == Config.NumpadControls.CenterHorizonFeedbackMode.SOUND_AND_TARGET
                            || mode == Config.NumpadControls.CenterHorizonFeedbackMode.SOUND_VOICE_AND_TARGET) {
                        playSnapSound(1.0f);
                    }
                    if (mode == Config.NumpadControls.CenterHorizonFeedbackMode.SOUND_VOICE_AND_TARGET) {
                        MainClass.narrate(I18n.get("minecraft_access.numpad.look_centered"), true);
                    }
                    CrosshairFeedbackManager.onLookCentered();
                    return true;
                })
                .build();

        // Primary Action (Attack / Mine with simulated left click) on Numpad 0
        keyLeftClick = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.mouse.left_click"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_0))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .withContext(KeyConflictContext.UNIVERSAL)
                .build();

        // Secondary Action (Use / Place / Eat with simulated right click) on Numpad Enter
        keyRightClick = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.mouse.right_click"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ENTER))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .withContext(KeyConflictContext.UNIVERSAL)
                .build();

        // Player Status (Health, Hunger, Level - Instant 1-touch read) on Numpad Decimal (.)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.player_all"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_DECIMAL))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
                    PlayerStatus.narratePlayerStatus(false);
                    return true;
                })
                .build();

        // Pick Block (Middle click) on Numpad +
        keyMiddleClick = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.mouse.middle_click"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ADD))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .withContext(KeyConflictContext.UNIVERSAL)
                .build();

        // Unlock on Numpad -
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.action.unlock"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_SUBTRACT))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
                    if (MainClass.poiManager != null && MainClass.poiManager.lockingHandler != null) {
                        if (MainClass.poiManager.lockingHandler.isPlayerLocked()) {
                            MainClass.poiManager.lockingHandler.unlock(true, true);
                        }
                    }
                    return true;
                })
                .build();

        // Hotbar Scroll on Numpad / and Numpad *
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.hotbar.scroll_prev"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_DIVIDE))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
                    scrollHotbar(true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.hotbar.scroll_next"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_MULTIPLY))
                .enableKeyRepeat()
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || ModifierUtils.hasAnyModifier()) return false;
                    scrollHotbar(false);
                    return true;
                })
                .build();
    }

    // =========================================================================
    // LAYER 1: CTRL + NUMPAD (Compass, Cardinals, Snap & POI Radar)
    // =========================================================================
    private void registerLayer2Control() {
        // Cardinals (Ctrl + 8, 6, 2, 4)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.north"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_8, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    rotateCameraTo(Orientation.NORTH, true);
                    playSnapSound(1.0f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.east"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_6, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    rotateCameraTo(Orientation.EAST, true);
                    playSnapSound(1.2f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.south"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_2, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    rotateCameraTo(Orientation.SOUTH, true);
                    playSnapSound(0.8f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.west"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_4, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    rotateCameraTo(Orientation.WEST, true);
                    playSnapSound(0.6f);
                    return true;
                })
                .build();

        // Intercardinals (Ctrl + 7, 9, 1, 3)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.north_west"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_7, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    rotateCameraTo(Orientation.NORTH_WEST, true);
                    playSnapSound(0.9f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.north_east"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_9, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    rotateCameraTo(Orientation.NORTH_EAST, true);
                    playSnapSound(1.1f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.south_west"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_1, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    rotateCameraTo(Orientation.SOUTH_WEST, true);
                    playSnapSound(0.7f);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.south_east"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_3, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    rotateCameraTo(Orientation.SOUTH_EAST, true);
                    playSnapSound(0.95f);
                    return true;
                })
                .build();

        // Look Behind (Ctrl + 0)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.look_behind"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_0, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    Orientation current = PlayerPositionUtils.getHorizontalFacing();
                    rotateCameraTo(current.getOpposite(), true);
                    playSnapSound(0.5f);
                    return true;
                })
                .build();

        // Restore Previous Look (Ctrl + 5)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.restore_previous_look"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_5, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    return LookHistoryManager.restorePreviousLook(Minecraft.getInstance());
                })
                .build();

        // POI Radar Controls on Ctrl + Numpad
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.group_prev"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_DIVIDE, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.moveGroup(-1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.group_next"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_MULTIPLY, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.moveGroup(1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.item_prev"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_SUBTRACT, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.moveObject(-1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.item_next"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ADD, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.moveObject(1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.poi.look_at_current_object"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ENTER, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly() || MainClass.poiManager == null) return false;
                    MainClass.poiManager.objectTracker.lookAtCurrentObject();
                    return true;
                })
                .build();

        // Narrate Targeted Block Coordinates (Ctrl + Numpad Decimal)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.orient.narrate_target_coords"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_DECIMAL, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlOnly()) return false;
                    narrateTargetCoordinates();
                    return true;
                })
                .build();
    }

    // =========================================================================
    // LAYER 2: ALT + NUMPAD (Diagnostics, Inventory, Vertices & Auto-Walk)
    // =========================================================================
    private void registerLayer3Alt() {
        // Equipment & Durability
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.mainhand"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_8, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly()) return false;
                    narrateHandItem(false);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.offhand"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_2, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly()) return false;
                    narrateHandItem(true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.effects"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_4, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly()) return false;
                    narrateActiveEffects();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.durability"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_6, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly()) return false;
                    narrateHeldItemDurability();
                    return true;
                })
                .build();

        // Narrate Facing (Direction & Pitch) on Alt + Numpad 5
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.narrate_facing"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_5, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly()) return false;
                    MainClass.narrate(PlayerPositionUtils.getFullFacingInWords(true), true);
                    return true;
                })
                .build();

        // Vertices: Nadir (Look at feet +90°) and Zenith (Look at sky -90°)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_nadir"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_1, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly()) return false;
                    rotateCameraToPitch(90.0f, true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.camera.look_zenith"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_3, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly()) return false;
                    rotateCameraToPitch(-90.0f, true);
                    return true;
                })
                .build();

        // Auto-Walk Toggle on Alt + Numpad 0
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.action.auto_walk"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_0, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly() || MainClass.autoWalkManager == null) return false;
                    MainClass.autoWalkManager.toggleAutoWalk();
                    return true;
                })
                .build();

        // Auto-Walk Sprint Toggle on Alt + Numpad Decimal
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.action.toggle_sprint"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_DECIMAL, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly() || MainClass.autoWalkManager == null) return false;
                    MainClass.autoWalkManager.toggleSprint();
                    return true;
                })
                .build();

        // Access Menu on Alt + Numpad Enter
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "numpad.status.access_menu"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_ENTER, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasAltOnly()) return false;
                    Minecraft client = Minecraft.getInstance();
                    client.gui.setScreen(new AccessMenu.GUI());
                    return true;
                })
                .build();
    }

    // =========================================================================
    // TICK UPDATE & CONTINUOUS HOLD CAMERA ENGINE
    // =========================================================================
    private void tick(Minecraft client) {
        if (isDisabled()) return;
        LocalPlayer player = client.player;
        if (player == null) return;

        // If any modifier (Ctrl, Alt, Shift) is held, suppress mouse button simulation and continuous rotation
        if (ModifierUtils.hasAnyModifier()) {
            if (keyLeftClick != null && keyLeftClick.wasDown()) {
                MouseUtils.Key.LEFT.release();
            }
            if (keyRightClick != null && keyRightClick.wasDown()) {
                MouseUtils.Key.RIGHT.release();
            }
            if (keyMiddleClick != null && keyMiddleClick.wasDown()) {
                MouseUtils.Key.MIDDLE.release();
            }
            holdStartLookUp = 0;
            holdStartLookDown = 0;
            holdStartLookLeft = 0;
            holdStartLookRight = 0;
            holdStartLookUpLeft = 0;
            holdStartLookUpRight = 0;
            holdStartLookDownLeft = 0;
            holdStartLookDownRight = 0;
            return;
        }

        // Mouse button hold simulation with proper press/release state transitions
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

        // Camera continuous hold rotation engine
        Config.NumpadControls config = Config.getInstance().numpadControls;
        if (!config.enableContinuousHold) return;

        long now = System.currentTimeMillis();
        int h = 0;
        int v = 0;

        if (keyLookUp != null && keyLookUp.isDown() && now - holdStartLookUp > CONTINUOUS_HOLD_DELAY_MS) v -= 1;
        if (keyLookDown != null && keyLookDown.isDown() && now - holdStartLookDown > CONTINUOUS_HOLD_DELAY_MS) v += 1;
        if (keyLookLeft != null && keyLookLeft.isDown() && now - holdStartLookLeft > CONTINUOUS_HOLD_DELAY_MS) h -= 1;
        if (keyLookRight != null && keyLookRight.isDown() && now - holdStartLookRight > CONTINUOUS_HOLD_DELAY_MS) h += 1;
        if (keyLookUpLeft != null && keyLookUpLeft.isDown() && now - holdStartLookUpLeft > CONTINUOUS_HOLD_DELAY_MS) { h -= 1; v -= 1; }
        if (keyLookUpRight != null && keyLookUpRight.isDown() && now - holdStartLookUpRight > CONTINUOUS_HOLD_DELAY_MS) { h += 1; v -= 1; }
        if (keyLookDownLeft != null && keyLookDownLeft.isDown() && now - holdStartLookDownLeft > CONTINUOUS_HOLD_DELAY_MS) { h -= 1; v += 1; }
        if (keyLookDownRight != null && keyLookDownRight.isDown() && now - holdStartLookDownRight > CONTINUOUS_HOLD_DELAY_MS) { h += 1; v += 1; }

        // Reset start times when keys are released
        if (keyLookUp != null && !keyLookUp.isDown()) holdStartLookUp = 0;
        if (keyLookDown != null && !keyLookDown.isDown()) holdStartLookDown = 0;
        if (keyLookLeft != null && !keyLookLeft.isDown()) holdStartLookLeft = 0;
        if (keyLookRight != null && !keyLookRight.isDown()) holdStartLookRight = 0;
        if (keyLookUpLeft != null && !keyLookUpLeft.isDown()) holdStartLookUpLeft = 0;
        if (keyLookUpRight != null && !keyLookUpRight.isDown()) holdStartLookUpRight = 0;
        if (keyLookDownLeft != null && !keyLookDownLeft.isDown()) holdStartLookDownLeft = 0;
        if (keyLookDownRight != null && !keyLookDownRight.isDown()) holdStartLookDownRight = 0;

        if (h != 0 || v != 0) {
            wasContinuouslyRotating = true;
            rotateCameraContinuously(h, v, config.continuousRotationSpeed);
        } else if (wasContinuouslyRotating) {
            wasContinuouslyRotating = false;
            lastContinuousFacing = null;
            if (config.narrateFacingOnChange && config.rotationFeedbackMode != Config.NumpadControls.RotationFeedbackMode.OFF) {
                CrosshairFeedbackManager.onCameraRotated(true);
            }
        }
    }

    private void rotateCameraBy(int horizontalWeight, int verticalWeight, boolean isContinuous) {
        if (handleLocking()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        LookHistoryManager.recordManualRotation(player.getYRot(), player.getXRot());

        Config.NumpadControls config = Config.getInstance().numpadControls;
        float angle = config.normalRotatingAngle;
        if (config.invertYAxis) verticalWeight = -verticalWeight;

        float deltaYaw = horizontalWeight * (angle / DEGREES_PER_MOUSE_DELTA);
        float deltaPitch = verticalWeight * (angle / DEGREES_PER_MOUSE_DELTA);

        player.turn(deltaYaw, deltaPitch);

        if (!isContinuous && config.narrateFacingOnChange) {
            Config.NumpadControls.RotationFeedbackMode mode = config.rotationFeedbackMode;
            if (mode == Config.NumpadControls.RotationFeedbackMode.OFF) return;

            // Audio Cue handling with pitch hierarchy
            if (mode == Config.NumpadControls.RotationFeedbackMode.SOUND_ONLY
                    || mode == Config.NumpadControls.RotationFeedbackMode.SOUND_AND_VOICE_WITH_DEGREES) {
                int deg = PlayerPositionUtils.getCompassDegrees();
                float pitch = 0.85f;
                if (deg % 90 == 0) {
                    pitch = 1.2f; // Cardinal (0, 90, 180, 270)
                } else if (deg % 45 == 0) {
                    pitch = 1.0f; // Intercardinal (45, 135, 225, 315)
                }
                player.level().playLocalSound(player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.35f * config.audioCueVolume, pitch, false);
            }

            // Voice narration handling
            if (mode != Config.NumpadControls.RotationFeedbackMode.SOUND_ONLY) {
                CrosshairFeedbackManager.onCameraRotated(true);
            }
        }
    }

    private void rotateCameraContinuously(int horizontalWeight, int verticalWeight, float speedMultiplier) {
        if (handleLocking()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        LookHistoryManager.recordManualRotation(player.getYRot(), player.getXRot());

        Config.NumpadControls config = Config.getInstance().numpadControls;
        if (config.invertYAxis) verticalWeight = -verticalWeight;

        float baseStep = 3.0f * speedMultiplier;
        float deltaYaw = horizontalWeight * (baseStep / DEGREES_PER_MOUSE_DELTA);
        float deltaPitch = verticalWeight * (baseStep / DEGREES_PER_MOUSE_DELTA);

        player.turn(deltaYaw, deltaPitch);

        if (horizontalWeight != 0 && config.continuousFeedbackMode != Config.NumpadControls.ContinuousFeedbackMode.OFF) {
            Orientation currentFacing = PlayerPositionUtils.getHorizontalFacing();
            if (currentFacing != lastContinuousFacing) {
                lastContinuousFacing = currentFacing;
                boolean isCardinal = currentFacing == Orientation.NORTH
                        || currentFacing == Orientation.EAST
                        || currentFacing == Orientation.SOUTH
                        || currentFacing == Orientation.WEST;

                if (config.continuousFeedbackMode == Config.NumpadControls.ContinuousFeedbackMode.SOUND_ONLY
                        || config.continuousFeedbackMode == Config.NumpadControls.ContinuousFeedbackMode.SOUND_AND_VOICE) {
                    float pitch = isCardinal ? 1.2f : 0.9f;
                    player.level().playLocalSound(player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.35f * config.audioCueVolume, pitch, false);
                }

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

        LookHistoryManager.saveCurrentLook(player.getYRot(), player.getXRot());
        Vec3 playerBlockPosition = player.position();
        Vec3 targetBlockPosition = playerBlockPosition.add(Vec3.atLowerCornerOf(direction.vector));
        player.lookAt(EntityAnchorArgument.Anchor.FEET, targetBlockPosition);

        if (narrateChange && Config.getInstance().numpadControls.narrateFacingOnChange) {
            CrosshairFeedbackManager.onCameraRotated(true);
        }
    }

    private void rotateCameraToPitch(float pitchDegrees, boolean narrateChange) {
        if (handleLocking()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        LookHistoryManager.saveCurrentLook(player.getYRot(), player.getXRot());
        player.setXRot(pitchDegrees);
        player.xRotO = pitchDegrees;

        if (narrateChange && Config.getInstance().numpadControls.narrateFacingOnChange) {
            CrosshairFeedbackManager.onCameraRotated(true);
        }
    }

    private void centerCameraHorizon() {
        if (handleLocking()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        LookHistoryManager.saveCurrentLook(player.getYRot(), player.getXRot());
        player.setXRot(0.0f);
        player.xRotO = 0.0f;
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

    private void narrateTargetCoordinates() {
        HitResult hit = PlayerUtils.crosshairTarget(20.0);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            MainClass.narrate(I18n.get("minecraft_access.access_menu.target_missed"), true);
            return;
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos blockPos = blockHit.getBlockPos();
            String blockName = MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(blockPos);
            String coords = blockPos.getX() + "x, " + blockPos.getY() + "y, " + blockPos.getZ() + "z";
            MainClass.narrate(blockName + ": " + coords, true);
        } else if (hit.getType() == HitResult.Type.ENTITY) {
            Vec3 pos = hit.getLocation();
            String coords = (int) pos.x + "x, " + (int) pos.y + "y, " + (int) pos.z + "z";
            MainClass.narrate(coords, true);
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
        if (item.isEmpty()) {
            MainClass.narrate(I18n.get("minecraft_access.other.no_item_held"), true);
            return;
        }

        if (!item.isDamageableItem()) {
            MainClass.narrate(I18n.get("minecraft_access.other.item_not_damageable", getItemName(item)), true);
            return;
        }

        int max = item.getMaxDamage();
        int current = max - item.getDamageValue();
        MainClass.narrate(I18n.get("minecraft_access.other.item_durability", getItemName(item), current, max), true);
    }

    private String getItemName(ItemStack item) {
        if (item.isEmpty()) {
            return I18n.get("minecraft_access.other.empty_hand");
        }
        StringBuilder itemName = new StringBuilder(item.getHoverName().getString());
        Optional.ofNullable(item.get(DataComponents.CUSTOM_NAME))
                .ifPresent(_ -> itemName.append(I18n.get("minecraft_access.other.has_custom_name")));
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

    private boolean isDisabled() {
        return !Config.getInstance().numpadControls.enabled;
    }
}
