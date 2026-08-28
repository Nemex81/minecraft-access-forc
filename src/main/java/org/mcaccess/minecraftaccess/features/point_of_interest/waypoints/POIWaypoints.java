package org.mcaccess.minecraftaccess.features.point_of_interest.waypoints;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.features.point_of_interest.POIGroup;
import org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.gui.SaveWaypointScreen;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.condition.Interval;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

@Slf4j
public class POIWaypoints implements BalmClientModule {
    private final Interval beaconInterval = Interval.defaultDelay();

    public final POIGroup<Waypoint> group = new POIGroup<>(
            "minecraft_access.point_of_interest.group.waypoints",
            new POIGroup.Sound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f),
            _ -> true
    );

    @SuppressWarnings("unchecked")
    public final POIGroup<Waypoint>[] groups = new POIGroup[]{group};

    public POIWaypoints() {
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "poi/waypoints");
    }

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register(this::tick);

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "other.save_waypoint_dialog"))
                .withDefault(InputBinding.key(InputConstants.KEY_D, KeyModifiers.of(KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    openSaveWaypointDialog();
                    return true;
                })
                .build();
    }

    public void openSaveWaypointDialog() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        BlockPos pos = client.player.blockPosition();
        Identifier dimension = client.level.dimension().identifier();
        String defaultName = WaypointManager.getInstance().generateDefaultCustomName();

        client.gui.setScreen(new SaveWaypointScreen(pos, dimension, defaultName));
    }

    public void selectWaypoint(Waypoint waypoint) {
        updateGroup();
        MainClass.poiManager.objectTracker.setCurrentObject(waypoint);
    }

    public void updateGroup() {
        group.clear();
        if (!Config.getInstance().poi.waypoints.enabled) return;

        List<Waypoint> waypoints = WaypointManager.getInstance().getWaypoints();
        for (Waypoint wp : waypoints) {
            group.addIfQualified(wp);
        }
    }

    private void tick(Minecraft client, Player player, Level level) {
        if (!Config.getInstance().poi.waypoints.enabled) return;
        if (client.gui.screen() != null) return;

        Config.POI.Waypoints config = Config.getInstance().poi.waypoints;
        beaconInterval.setDelay(config.beaconInterval, Interval.Unit.MILLISECOND);

        if (!config.playAudioBeacon) return;
        if (!beaconInterval.isReady()) return;

        Object currentObject = MainClass.poiManager.objectTracker.getCurrentObject();
        if (currentObject instanceof Waypoint waypoint && player instanceof LocalPlayer localPlayer) {
            Vec3 targetVec = WaypointUtils.getTargetVectorForPlayer(waypoint, localPlayer, config.crossDimensionConversion);
            if (targetVec != null && config.beaconVolume > 0.0f) {
                Vec3 soundPos = WaypointUtils.getAudioBeaconPosition(targetVec, localPlayer);
                if (soundPos != null && client.level != null) {
                    client.level.playLocalSound(
                            soundPos.x,
                            soundPos.y,
                            soundPos.z,
                            SoundEvents.NOTE_BLOCK_BELL.value(),
                            SoundSource.BLOCKS,
                            config.beaconVolume,
                            1.0f,
                            true
                    );
                }
            }
        }
    }
}
