package org.mcaccess.minecraftaccess.features.point_of_interest.waypoints;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.client.platform.event.callback.ClientLifecycleCallback;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

@Slf4j
public class WaypointManager implements BalmClientModule {
    @Getter
    private static WaypointManager instance;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<Waypoint> waypoints = new ArrayList<>();
    private boolean deathRecorded = false;
    private BlockPos lastBedPos = null;

    public WaypointManager() {
        instance = this;
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "poi/waypoint_manager");
    }

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register(this::tick);
        ClientLifecycleCallback.ConnectedToServer.EVENT.register(_ -> {
            loadForCurrentWorld();
            deathRecorded = false;
            lastBedPos = null;
        });
    }

    private void tick(Minecraft client, net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level) {
        if (!Config.getInstance().poi.waypoints.enabled) return;

        // Auto save death point
        if (Config.getInstance().poi.waypoints.autoSaveDeathPoint) {
            if (player.isDeadOrDying() || player.getHealth() <= 0) {
                if (!deathRecorded) {
                    deathRecorded = true;
                    saveDeathWaypoint(player.blockPosition(), level.dimension().identifier());
                }
            } else {
                deathRecorded = false;
            }
        }

        // Auto save bed point
        if (Config.getInstance().poi.waypoints.autoSaveBedPoint && player.isSleeping()) {
            player.getSleepingPos().ifPresent(bedPos -> {
                if (!bedPos.equals(lastBedPos)) {
                    lastBedPos = bedPos;
                    saveBedWaypoint(bedPos, level.dimension().identifier());
                }
            });
        }
    }

    public List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    public void addCustomWaypoint(String name, BlockPos pos, Identifier dimension) {
        String id = UUID.randomUUID().toString();
        Waypoint waypoint = new Waypoint(id, name, pos, dimension, WaypointType.CUSTOM, System.currentTimeMillis());
        waypoints.add(waypoint);
        saveForCurrentWorld();
        if (MainClass.poiManager != null && MainClass.poiManager.poiWaypoints != null) {
            MainClass.poiManager.poiWaypoints.updateGroup();
        }
    }

    public void saveDeathWaypoint(BlockPos pos, Identifier dimension) {
        waypoints.removeIf(w -> w.type() == WaypointType.DEATH);
        String name = I18n.get("minecraft_access.point_of_interest.waypoint.death");
        Waypoint deathWp = new Waypoint("death_point", name, pos, dimension, WaypointType.DEATH, System.currentTimeMillis());
        waypoints.add(deathWp);
        saveForCurrentWorld();
        if (MainClass.poiManager != null && MainClass.poiManager.poiWaypoints != null) {
            MainClass.poiManager.poiWaypoints.updateGroup();
        }
        log.info("Auto-saved death point at {} ({})", pos, dimension);
    }

    public void saveBedWaypoint(BlockPos pos, Identifier dimension) {
        waypoints.removeIf(w -> w.type() == WaypointType.BED);
        String name = I18n.get("minecraft_access.point_of_interest.waypoint.bed");
        Waypoint bedWp = new Waypoint("bed_point", name, pos, dimension, WaypointType.BED, System.currentTimeMillis());
        waypoints.add(bedWp);
        saveForCurrentWorld();
        if (MainClass.poiManager != null && MainClass.poiManager.poiWaypoints != null) {
            MainClass.poiManager.poiWaypoints.updateGroup();
        }
        log.info("Auto-saved bed point at {} ({})", pos, dimension);
    }

    public void removeWaypoint(String id) {
        waypoints.removeIf(w -> w.id().equals(id));
        saveForCurrentWorld();
        if (MainClass.poiManager != null && MainClass.poiManager.poiWaypoints != null) {
            MainClass.poiManager.poiWaypoints.updateGroup();
        }
    }

    public String generateDefaultCustomName() {
        int customCount = (int) waypoints.stream().filter(w -> w.type() == WaypointType.CUSTOM).count() + 1;
        return I18n.get("minecraft_access.point_of_interest.waypoint.default_custom", customCount);
    }

    private Path getStoragePath() {
        String worldId = getCurrentWorldId();
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("minecraft-access").resolve("waypoints");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            log.error("Failed to create waypoints directory", e);
        }
        return dir.resolve(worldId + ".json");
    }

    public static String getCurrentWorldId() {
        Minecraft client = Minecraft.getInstance();
        if (client.getSingleplayerServer() != null) {
            String name = client.getSingleplayerServer().getWorldData().getLevelName();
            return "singleplayer_" + name.replaceAll("[^a-zA-Z0-9_-]", "_");
        } else if (client.getCurrentServer() != null) {
            return "server_" + client.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9_-]", "_");
        }
        return "default_world";
    }

    public void loadForCurrentWorld() {
        waypoints.clear();
        Path file = getStoragePath();
        if (!Files.exists(file)) {
            if (MainClass.poiManager != null && MainClass.poiManager.poiWaypoints != null) {
                MainClass.poiManager.poiWaypoints.updateGroup();
            }
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            List<WaypointDTO> dtos = GSON.fromJson(reader, new TypeToken<List<WaypointDTO>>() {}.getType());
            if (dtos != null) {
                for (WaypointDTO dto : dtos) {
                    if (dto.dimension == null) continue;
                    Identifier dim = Identifier.parse(dto.dimension);
                    WaypointType type = WaypointType.CUSTOM;
                    if (dto.type != null) {
                        try {
                            type = WaypointType.valueOf(dto.type);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    waypoints.add(new Waypoint(
                            dto.id,
                            dto.name,
                            new BlockPos(dto.x, dto.y, dto.z),
                            dim,
                            type,
                            dto.timestamp
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Failed to load waypoints from {}", file, e);
        }

        if (MainClass.poiManager != null && MainClass.poiManager.poiWaypoints != null) {
            MainClass.poiManager.poiWaypoints.updateGroup();
        }
    }

    public void saveForCurrentWorld() {
        Path file = getStoragePath();
        List<WaypointDTO> dtos = new ArrayList<>();
        for (Waypoint w : waypoints) {
            WaypointDTO dto = new WaypointDTO();
            dto.id = w.id();
            dto.name = w.name();
            dto.x = w.pos().getX();
            dto.y = w.pos().getY();
            dto.z = w.pos().getZ();
            dto.dimension = w.dimension().toString();
            dto.type = w.type().name();
            dto.timestamp = w.timestamp();
            dtos.add(dto);
        }

        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(dtos, writer);
        } catch (Exception e) {
            log.error("Failed to save waypoints to {}", file, e);
        }
    }

    static class WaypointDTO {
        String id;
        String name;
        int x;
        int y;
        int z;
        String dimension;
        String type;
        long timestamp;
    }
}
