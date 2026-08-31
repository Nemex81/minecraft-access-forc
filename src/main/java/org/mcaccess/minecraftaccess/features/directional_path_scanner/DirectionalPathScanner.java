package org.mcaccess.minecraftaccess.features.directional_path_scanner;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.ModifierUtils;
import org.mcaccess.minecraftaccess.utils.position.Orientation;
import org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils;

@Slf4j
public class DirectionalPathScanner implements BalmClientModule {
    private static DirectionalPathScanner instance;

    @Getter
    private PathScanReport lastReport = null;

    public static DirectionalPathScanner getInstance() {
        return instance;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "directional_path_scanner");
    }

    @Override
    public void initialize() {
        instance = this;

        // --- NUMPAD SCAN BINDINGS (Ctrl + Alt + Numpad) ---

        // Numpad 8 (North / Forward)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_8"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_8, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    boolean cardinal = Config.getInstance().directionalPathScanner.numpadCardinalsMode == Config.DirectionalPathScanner.NumpadCardinalsMode.CARDINAL_FIXED;
                    if (cardinal) {
                        scanCardinal(Orientation.NORTH, "north");
                    } else {
                        scanRelative(RelativeDir.FORWARD);
                    }
                    return true;
                })
                .build();

        // Numpad 2 (South / Backward)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_2"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_2, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    boolean cardinal = Config.getInstance().directionalPathScanner.numpadCardinalsMode == Config.DirectionalPathScanner.NumpadCardinalsMode.CARDINAL_FIXED;
                    if (cardinal) {
                        scanCardinal(Orientation.SOUTH, "south");
                    } else {
                        scanRelative(RelativeDir.BACKWARD);
                    }
                    return true;
                })
                .build();

        // Numpad 4 (West / Left)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_4"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_4, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    boolean cardinal = Config.getInstance().directionalPathScanner.numpadCardinalsMode == Config.DirectionalPathScanner.NumpadCardinalsMode.CARDINAL_FIXED;
                    if (cardinal) {
                        scanCardinal(Orientation.WEST, "west");
                    } else {
                        scanRelative(RelativeDir.LEFT);
                    }
                    return true;
                })
                .build();

        // Numpad 6 (East / Right)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_6"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_6, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    boolean cardinal = Config.getInstance().directionalPathScanner.numpadCardinalsMode == Config.DirectionalPathScanner.NumpadCardinalsMode.CARDINAL_FIXED;
                    if (cardinal) {
                        scanCardinal(Orientation.EAST, "east");
                    } else {
                        scanRelative(RelativeDir.RIGHT);
                    }
                    return true;
                })
                .build();

        // Numpad 7 (North-West)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_7"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_7, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    scanCardinal(Orientation.NORTH_WEST, "north_west");
                    return true;
                })
                .build();

        // Numpad 9 (North-East)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_9"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_9, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    scanCardinal(Orientation.NORTH_EAST, "north_east");
                    return true;
                })
                .build();

        // Numpad 1 (South-West)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_1"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_1, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    scanCardinal(Orientation.SOUTH_WEST, "south_west");
                    return true;
                })
                .build();

        // Numpad 3 (South-East)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_3"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_3, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    scanCardinal(Orientation.SOUTH_EAST, "south_east");
                    return true;
                })
                .build();

        // Numpad 5 (Forward / Player Current Look)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_5"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_5, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    scanRelative(RelativeDir.FORWARD);
                    return true;
                })
                .build();

        // Numpad 0 (Backward / Behind Player)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.numpad_0"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_KP_0, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.NUMPAD_CONTROLS)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    scanRelative(RelativeDir.BACKWARD);
                    return true;
                })
                .build();

        // --- ARROW KEYS BINDINGS (Ctrl + Alt + Arrows) ---

        // Up Arrow (Forward / North)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.extended_up"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_UP, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.PATH_SCANNER)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    boolean relative = Config.getInstance().directionalPathScanner.extendedKeysMode == Config.DirectionalPathScanner.ExtendedKeysMode.RELATIVE_TO_LOOK;
                    if (relative) {
                        scanRelative(RelativeDir.FORWARD);
                    } else {
                        scanCardinal(Orientation.NORTH, "north");
                    }
                    return true;
                })
                .build();

        // Down Arrow (Backward / South)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.extended_down"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_DOWN, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.PATH_SCANNER)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    boolean relative = Config.getInstance().directionalPathScanner.extendedKeysMode == Config.DirectionalPathScanner.ExtendedKeysMode.RELATIVE_TO_LOOK;
                    if (relative) {
                        scanRelative(RelativeDir.BACKWARD);
                    } else {
                        scanCardinal(Orientation.SOUTH, "south");
                    }
                    return true;
                })
                .build();

        // Left Arrow (Left / West)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.extended_left"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_LEFT, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.PATH_SCANNER)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    boolean relative = Config.getInstance().directionalPathScanner.extendedKeysMode == Config.DirectionalPathScanner.ExtendedKeysMode.RELATIVE_TO_LOOK;
                    if (relative) {
                        scanRelative(RelativeDir.LEFT);
                    } else {
                        scanCardinal(Orientation.WEST, "west");
                    }
                    return true;
                })
                .build();

        // Right Arrow (Right / East)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "path_scanner.extended_right"))
                .withDefault(InputBinding.key(GLFW.GLFW_KEY_RIGHT, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.PATH_SCANNER)
                .handleWorldInput(_ -> {
                    if (isDisabled() || !ModifierUtils.hasControlAndAlt()) return false;
                    boolean relative = Config.getInstance().directionalPathScanner.extendedKeysMode == Config.DirectionalPathScanner.ExtendedKeysMode.RELATIVE_TO_LOOK;
                    if (relative) {
                        scanRelative(RelativeDir.RIGHT);
                    } else {
                        scanCardinal(Orientation.EAST, "east");
                    }
                    return true;
                })
                .build();
    }

    private void scanCardinal(Orientation orientation, String directionKey) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().level == null) return;

        Vec3 stepVec = new Vec3(orientation.vector.getX(), 0, orientation.vector.getZ());
        executeScan(player, stepVec, directionKey);
    }

    private void scanRelative(RelativeDir relativeDir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().level == null) return;

        Orientation facing = PlayerPositionUtils.getHorizontalFacing();
        Vec3 stepVec;
        String dirKey;

        switch (relativeDir) {
            case FORWARD -> {
                stepVec = new Vec3(facing.vector.getX(), 0, facing.vector.getZ());
                dirKey = "forward";
            }
            case BACKWARD -> {
                stepVec = new Vec3(facing.getOpposite().vector.getX(), 0, facing.getOpposite().vector.getZ());
                dirKey = "backward";
            }
            case LEFT -> {
                stepVec = new Vec3(facing.vector.getZ(), 0, -facing.vector.getX());
                dirKey = "left";
            }
            case RIGHT -> {
                stepVec = new Vec3(-facing.vector.getZ(), 0, facing.vector.getX());
                dirKey = "right";
            }
            default -> {
                stepVec = new Vec3(facing.vector.getX(), 0, facing.vector.getZ());
                dirKey = "forward";
            }
        }

        executeScan(player, stepVec, dirKey);
    }

    private void executeScan(LocalPlayer player, Vec3 stepVec, String directionKey) {
        Config.DirectionalPathScanner config = Config.getInstance().directionalPathScanner;

        // Play directional audio feedback if enabled
        if (config.audioFeedback == Config.DirectionalPathScanner.AudioFeedbackMode.SOUND_AND_VOICE
                || config.audioFeedback == Config.DirectionalPathScanner.AudioFeedbackMode.SOUND_ONLY) {
            playScanSound(player, stepVec);
        }

        // Perform raycast
        lastReport = PathRaycaster.scanPath(player.level(), player, stepVec, directionKey, config);

        // Vocalize report if voice feedback is active
        if (config.audioFeedback != Config.DirectionalPathScanner.AudioFeedbackMode.SOUND_ONLY) {
            String narration = PathNarrationFormatter.formatReport(lastReport, config.verbosityMode);
            if (narration != null && !narration.isBlank()) {
                MainClass.narrate(narration, true);
            }
        }
    }

    private void playScanSound(LocalPlayer player, Vec3 stepVec) {
        if (player == null || player.level() == null) return;
        Vec3 soundPos = player.position().add(stepVec.normalize().scale(1.5));
        player.level().playLocalSound(
                soundPos.x, soundPos.y, soundPos.z,
                SoundEvents.NOTE_BLOCK_HAT.value(),
                SoundSource.PLAYERS,
                0.7f, 1.4f, false
        );
    }

    private boolean isDisabled() {
        return !Config.getInstance().directionalPathScanner.enabled;
    }

    public enum RelativeDir {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT
    }
}
