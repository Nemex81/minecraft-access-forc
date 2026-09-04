package org.mcaccess.minecraftaccess.features.point_of_interest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.blay09.mods.balm.client.platform.event.callback.ClientLifecycleCallback;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.api.WorldNarrator;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointUtils;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.NarrationUtils;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

public class ObjectTracker implements BalmClientModule {
    public static final String START_OF_LIST = "minecraft_access.other.start_of_list";
    public static final String END_OF_LIST = "minecraft_access.other.end_of_list";

    @Getter
    private Object currentObject = null;

    @Getter
    private POIGroup<?> currentGroup = null;

    private List<POIGroup<?>> groups = new ArrayList<>();

    ObjectTracker() {
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "poi/object_tracker");
    }

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register(this::tick);
        ClientLifecycleCallback.ConnectedToServer.EVENT.register(_ -> {
            currentObject = null;
            currentGroup = null;
            groups = new ArrayList<>();
        });

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.narrate_current_object"))
                .withDefault(InputBinding.key(InputConstants.KEY_HOME))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    narrateCurrentObject(true);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.target_nearest/any"))
                .withDefault(InputBinding.key(InputConstants.KEY_END))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    targetNearestAny();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.target_nearest/block"))
                .withDefault(InputBinding.key(InputConstants.KEY_END, KeyModifiers.of(KeyModifier.SHIFT)))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    targetNearestBlock();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.target_nearest/entity"))
                .withDefault(InputBinding.key(InputConstants.KEY_END, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    targetNearestEntity();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.move_item/previous"))
                .withDefault(InputBinding.key(InputConstants.KEY_PAGEUP))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    moveObject(-1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.move_item/next"))
                .withDefault(InputBinding.key(InputConstants.KEY_PAGEDOWN))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    moveObject(1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.move_group/previous"))
                .withDefault(InputBinding.key(InputConstants.KEY_PAGEUP, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    moveGroup(-1);
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.look_at_current_object"))
                .withDefault(InputBinding.key(InputConstants.KEY_HOME, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    lookAtCurrentObject();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.narrate_coordinates_current_object"))
                .withDefault(InputBinding.key(InputConstants.KEY_HOME, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    narrateCoordinatesOfCurrentObject();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "object_tracker.move_group/next"))
                .withDefault(InputBinding.key(InputConstants.KEY_PAGEDOWN, KeyModifiers.of(KeyModifier.CONTROL)))
                .overrideCategory(KeyMappingCategories.OBJECT_TRACKER)
                .handleWorldInput(_ -> {
                    moveGroup(1);
                    return true;
                })
                .build();
    }

    private List<POIGroup<?>> getPOIGroups() {
        List<POIGroup<?>> result = new ArrayList<>();
        result.addAll(Arrays.asList(MainClass.poiManager.poiEntities.groups));
        result.addAll(Arrays.asList(MainClass.poiManager.poiBlocks.groups));
        result.addAll(Arrays.asList(MainClass.poiManager.poiWaypoints.groups));

        return result.stream().filter(group -> !group.isEmpty()).toList();
    }

    private void tick(Minecraft client, Player player, Level level) {
        if (client.gui.screen() != null) return;

        updateGroups();
    }

    private void updateGroups() {
        groups = getPOIGroups();

        int currentGroupIndex = groups.indexOf(currentGroup);

        if (currentGroup != null && currentGroup.isEmpty()) currentGroupIndex = -1;
        if (!groups.isEmpty() && currentGroupIndex == -1) currentGroup = groups.getFirst();
        if (groups.isEmpty() && currentGroupIndex != -1) currentGroup = null;
    }

    private void narrateCurrentObject(boolean interrupt) {
        if (checkAndNarrateIfAllGroupsEmpty()) return;

        if (!isObjectValid(currentObject)) {
            clearCurrentObject();
            narrateDirect(I18n.get("minecraft_access.point_of_interest.not_selected"), true);
            return;
        }

        boolean narrateDistance = Config.getInstance().poi.narrateDistance;
        LocalPlayer player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;
        Config.POI.WallOcclusionFeedbackMode occlusionMode = Config.getInstance().poi.wallOcclusionFeedback;
        boolean applySoundAttenuation = (occlusionMode == Config.POI.WallOcclusionFeedbackMode.SOUND_AND_VOICE
                || occlusionMode == Config.POI.WallOcclusionFeedbackMode.SOUND_ONLY);
        boolean applyVoiceWarning = (occlusionMode == Config.POI.WallOcclusionFeedbackMode.SOUND_AND_VOICE
                || occlusionMode == Config.POI.WallOcclusionFeedbackMode.VOICE_ONLY);

        if (currentObject instanceof Entity entity) {
            boolean isEntityOccluded = (player != null && level != null
                    && org.mcaccess.minecraftaccess.utils.audio.AcousticOcclusion.isOccluded(player.getEyePosition(), entity.getEyePosition(), level));
            float volumeMultiplier = (applySoundAttenuation && player != null && level != null)
                    ? org.mcaccess.minecraftaccess.utils.audio.AcousticOcclusion.getVolumeMultiplier(player.getEyePosition(), entity.getEyePosition(), level)
                    : 1.0f;

            StringBuilder narration = new StringBuilder(
                    MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(entity)
            );
            if (applyVoiceWarning && isEntityOccluded) {
                narration.append(I18n.get("minecraft_access.point_of_interest.behind_wall"));
            }
            if (narrateDistance) {
                narration.append(' ')
                        .append(NarrationUtils.narrateRelativePositionOfPlayerAnd(entity.blockPosition()));
            }
            narrateDirect(narration.toString(), interrupt);
            assert Minecraft.getInstance().level != null;
            Minecraft.getInstance().level.playLocalSound(
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.BLOCKS,
                    volumeMultiplier,
                    1.0f,
                    true
            );
            return;
        }

        if (currentObject instanceof BlockPos blockPos) {
            Vec3 blockCenter = Vec3.atCenterOf(blockPos);
            boolean isBlockOccluded = (player != null && level != null
                    && org.mcaccess.minecraftaccess.utils.audio.AcousticOcclusion.isOccluded(player.getEyePosition(), blockCenter, level));
            float volumeMultiplier = (applySoundAttenuation && player != null && level != null)
                    ? org.mcaccess.minecraftaccess.utils.audio.AcousticOcclusion.getVolumeMultiplier(player.getEyePosition(), blockCenter, level)
                    : 1.0f;

            StringBuilder narration = new StringBuilder(
                    MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(blockPos)
            );
            if (applyVoiceWarning && isBlockOccluded) {
                narration.append(I18n.get("minecraft_access.point_of_interest.behind_wall"));
            }
            if (narrateDistance) {
                narration.append(' ')
                        .append(NarrationUtils.narrateRelativePositionOfPlayerAnd(blockPos));
            }
            narrateDirect(narration.toString(), interrupt);
            assert Minecraft.getInstance().level != null;
            Minecraft.getInstance().level.playLocalSound(
                    blockPos,
                    SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.BLOCKS,
                    volumeMultiplier,
                    1.0f,
                    true
            );
            return;
        }

        if (currentObject instanceof Waypoint waypoint) {
            if (player == null) return;

            Identifier currentDim = player.level().dimension().identifier();
            Identifier targetDim = waypoint.dimension();

            StringBuilder narration = new StringBuilder(waypoint.name());

            if (currentDim.equals(targetDim)) {
                if (narrateDistance) {
                    narration.append(' ')
                            .append(NarrationUtils.narrateRelativePositionOfPlayerAnd(waypoint.pos()));
                }
            } else {
                if (Config.getInstance().poi.waypoints.crossDimensionConversion && WaypointUtils.isOverworldNetherPair(currentDim, targetDim)) {
                    BlockPos convertedPos = WaypointUtils.convertCoordinates(waypoint.pos(), targetDim, currentDim);
                    String dimName = WaypointUtils.getDimensionName(currentDim);
                    narration.append(' ')
                            .append(I18n.get(
                                    "minecraft_access.point_of_interest.cross_dimension_narration",
                                    WaypointUtils.getDimensionName(targetDim),
                                    NarrationUtils.narrateRelativePositionOfPlayerAnd(convertedPos),
                                    dimName,
                                    NarrationUtils.narrateCoordinatesOf(convertedPos)
                            ));
                } else {
                    narration.append(' ')
                            .append(I18n.get("minecraft_access.point_of_interest.other_dimension", WaypointUtils.getDimensionName(targetDim)));
                }
            }

            narrateDirect(narration.toString(), interrupt);

            Vec3 targetVec = WaypointUtils.getTargetVectorForPlayer(waypoint, player, Config.getInstance().poi.waypoints.crossDimensionConversion);
            if (targetVec != null && Minecraft.getInstance().level != null) {
                Vec3 soundPos = WaypointUtils.getAudioBeaconPosition(targetVec, player);
                if (soundPos != null) {
                    Minecraft.getInstance().level.playLocalSound(
                            soundPos.x,
                            soundPos.y,
                            soundPos.z,
                            SoundEvents.NOTE_BLOCK_BELL.value(),
                            SoundSource.BLOCKS,
                            1.0f,
                            1.0f,
                            true
                    );
                }
            }
        }
    }

    public void setCurrentObject(Object object) {
        this.currentObject = object;
        for (POIGroup<?> group : groups) {
            if (group.getItems().contains(object)) {
                this.currentGroup = group;
                break;
            }
        }
    }

    public void lookAtCurrentObject() {
        if (!isObjectValid(currentObject)) {
            narrateDirect(I18n.get("minecraft_access.point_of_interest.not_selected"), true);
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        org.mcaccess.minecraftaccess.features.LookHistoryManager.saveCurrentLook(player.getYRot(), player.getXRot());

        switch (currentObject) {
            case Entity entity -> {
                player.lookAt(EntityAnchorArgument.Anchor.EYES, entity.getEyePosition());
                String name = MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(entity);
                String facing = org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils.getFullFacingInWords(true);
                narrateDirect(I18n.get("minecraft_access.point_of_interest.look_at", name, facing), true);
            }
            case BlockPos blockPos -> {
                player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(blockPos));
                String name = MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(blockPos);
                String facing = org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils.getFullFacingInWords(true);
                narrateDirect(I18n.get("minecraft_access.point_of_interest.look_at", name, facing), true);
            }
            case Waypoint waypoint -> {
                Vec3 targetVec = WaypointUtils.getTargetVectorForPlayer(waypoint, player, Config.getInstance().poi.waypoints.crossDimensionConversion);
                if (targetVec != null) {
                    player.lookAt(EntityAnchorArgument.Anchor.EYES, targetVec);
                    String facing = org.mcaccess.minecraftaccess.utils.position.PlayerPositionUtils.getFullFacingInWords(true);
                    narrateDirect(I18n.get("minecraft_access.point_of_interest.look_at", waypoint.name(), facing), true);
                } else {
                    narrateDirect(I18n.get("minecraft_access.point_of_interest.other_dimension", WaypointUtils.getDimensionName(waypoint.dimension())), true);
                }
            }
            default -> {}
        }
    }

    public void narrateCoordinatesOfCurrentObject() {
        if (!isObjectValid(currentObject)) {
            narrateDirect(I18n.get("minecraft_access.point_of_interest.not_selected"), true);
            return;
        }

        switch (currentObject) {
            case Entity entity -> {
                String name = MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(entity);
                String coords = NarrationUtils.narrateCoordinatesOf(entity.blockPosition());
                narrateDirect(I18n.get("minecraft_access.point_of_interest.coordinates", name, coords), true);
            }
            case BlockPos blockPos -> {
                String name = MainClass.registry(WorldNarrator.class).get(Config.getInstance().narrateCrosshair.narrator).narrate(blockPos);
                String coords = NarrationUtils.narrateCoordinatesOf(blockPos);
                narrateDirect(I18n.get("minecraft_access.point_of_interest.coordinates", name, coords), true);
            }
            case Waypoint waypoint -> {
                String coords = NarrationUtils.narrateCoordinatesOf(waypoint.pos());
                String dim = WaypointUtils.getDimensionName(waypoint.dimension());
                narrateDirect(I18n.get("minecraft_access.point_of_interest.coordinates_with_dimension", waypoint.name(), dim, coords), true);
            }
            default -> {}
        }
    }

    public void moveGroup(int step) {
        if (checkAndNarrateIfAllGroupsEmpty()) return;

        int currentGroupIndex = groups.indexOf(currentGroup);
        int newIndex = currentGroupIndex + step;

        boolean atBoundary = false;
        if (newIndex < 0) {
            newIndex = 0;
            atBoundary = true;
            narrateDirect(I18n.get(START_OF_LIST), true);
        } else if (newIndex >= groups.size()) {
            newIndex = groups.size() - 1;
            atBoundary = true;
            narrateDirect(I18n.get(END_OF_LIST), true);
        }

        POIGroup<?> nextGroup = groups.get(newIndex);

        while ((nextGroup.isEmpty()
                || nextGroup.sortByDistance().stream().noneMatch(this::isObjectValid))
                && newIndex + step >= 0
                && newIndex + step < groups.size()) {
            newIndex += step;
            nextGroup = groups.get(newIndex);
        }

        if (nextGroup.isEmpty()
                || nextGroup.sortByDistance().stream().noneMatch(this::isObjectValid)) {
            narrateDirect(I18n.get(step > 0 ? END_OF_LIST : START_OF_LIST), true);
            return;
        }

        currentGroup = nextGroup;

        List<?> validObjects = currentGroup.sortByDistance().stream()
                .filter(this::isObjectValid)
                .toList();

        if (validObjects.isEmpty()) {
            clearCurrentObject();
            narrateDirect(I18n.get("minecraft_access.point_of_interest.not_selected"), true);
            return;
        }

        currentObject = validObjects.getFirst();
        narrateDirect(currentGroup.getTranslatedName(), !atBoundary);
        narrateCurrentObject(false);
    }

    public boolean isObjectValid(Object object) {
        return switch (object) {
            case Entity entity -> entity.isAlive();
            case BlockPos pos -> {
                if (Minecraft.getInstance().level == null) {
                    yield false;
                } else {
                    yield !(Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof AirBlock);
                }
            }
            case Waypoint waypoint -> true;
            default -> false;
        };
    }

    public void moveObject(int step) {
        if (checkAndNarrateIfAllGroupsEmpty()) return;
        if (currentGroup != null && currentGroup.isEmpty()) clearCurrentObject();

        List<?> objects = currentGroup.sortByDistance();
        int currentObjectIndex = objects.indexOf(currentObject);

        if (currentObjectIndex == -1) {
            narrateDirect(I18n.get(START_OF_LIST), true);
            currentObject = objects.getFirst();
        } else {
            int newIndex = currentObjectIndex + step;
            if (newIndex < 0) {
                narrateDirect(I18n.get(START_OF_LIST), true);
                currentObject = objects.getFirst();
            } else if (newIndex >= objects.size()) {
                narrateDirect(I18n.get(END_OF_LIST), true);
                currentObject = objects.getLast();
            } else {
                currentObject = objects.get(newIndex);
            }
        }

        while (!isObjectValid(currentObject)) {
            int nextIndex = objects.indexOf(currentObject) + step;
            if (nextIndex < 0 || nextIndex >= objects.size()) {
                narrateDirect(I18n.get(step > 0 ? END_OF_LIST : START_OF_LIST), true);
                clearCurrentObject();
                return;
            }
            currentObject = objects.get(nextIndex);
        }

        narrateCurrentObject(false);
    }

    private boolean checkAndNarrateIfAllGroupsEmpty() {
        if (groups.isEmpty()) {
            clearCurrentObject();
            narrateDirect(I18n.get("minecraft_access.point_of_interest.not_found"), true);
            return true;
        } else {
            return false;
        }
    }

    public void targetNearestAny() {
        Minecraft client = Minecraft.getInstance();
        assert client.player != null;

        List<Entity> entities = MainClass.poiManager.poiEntities.getLastScanResults()
                .stream()
                .sorted(Comparator.comparingDouble(a -> a.distanceTo(client.player)))
                .toList();

        List<BlockPos> blocks = MainClass.poiManager.poiBlocks.getLastScanResults()
                .stream()
                .sorted(Comparator.comparingDouble(a -> client.player.getEyePosition().distanceTo(Vec3.atCenterOf(a))))
                .toList();

        if (entities.isEmpty() && blocks.isEmpty()) {
            narrateDirect(I18n.get("minecraft_access.point_of_interest.not_found"), true);
            return;
        }

        if (entities.isEmpty()) {
            currentObject = blocks.getFirst();
        } else if (blocks.isEmpty()) {
            currentObject = entities.getFirst();
        } else if (client.player.getEyePosition().distanceTo(Vec3.atCenterOf(blocks.getFirst()))
                < client.player.distanceTo(entities.getFirst())) {
            currentObject = blocks.getFirst();
        } else {
            currentObject = entities.getFirst();
        }

        narrateDirect(I18n.get("minecraft_access.point_of_interest.targeting_nearest"), true);
        narrateCurrentObject(false);
    }

    public void targetNearestBlock() {
        Minecraft client = Minecraft.getInstance();
        assert client.player != null;

        List<BlockPos> blocks = MainClass.poiManager.poiBlocks.getLastScanResults()
                .stream()
                .sorted(Comparator.comparingDouble(a -> client.player.getEyePosition().distanceTo(Vec3.atCenterOf(a))))
                .toList();

        if (blocks.isEmpty()) {
            narrateDirect(I18n.get("minecraft_access.point_of_interest.not_found.block"), true);
            return;
        }

        currentObject = blocks.getFirst();
        narrateDirect(I18n.get("minecraft_access.point_of_interest.targeting_nearest.block"), true);
        narrateCurrentObject(false);
    }

    public void targetNearestEntity() {
        Minecraft client = Minecraft.getInstance();
        assert client.player != null;

        List<Entity> entities = MainClass.poiManager.poiEntities.getLastScanResults()
                .stream()
                .sorted(Comparator.comparingDouble(a -> a.distanceTo(client.player)))
                .toList();

        if (entities.isEmpty()) {
            narrateDirect(I18n.get("minecraft_access.point_of_interest.not_found.entity"), true);
            return;
        }

        currentObject = entities.getFirst();
        narrateDirect(I18n.get("minecraft_access.point_of_interest.targeting_nearest.entity"), true);
        narrateCurrentObject(false);
    }

    public void clearCurrentObject() {
        currentObject = null;
        updateGroups();
    }

    private static void narrateDirect(String text, boolean interrupt) {
        if (text == null || text.isBlank()) return;
        org.mcaccess.minecraftaccess.features.cognitive.DirectInteractionShield.protectVoiceResponse(text);
        MainClass.narrate(text, interrupt);
    }
}
