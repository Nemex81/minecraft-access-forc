package org.mcaccess.minecraftaccess.features.point_of_interest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.blay09.mods.balm.client.platform.event.callback.ClientLifecycleCallback;
import net.blay09.mods.balm.client.platform.module.BalmClientModule;
import net.blay09.mods.kuma.api.InputBinding;
import net.blay09.mods.kuma.api.KeyModifier;
import net.blay09.mods.kuma.api.KeyModifiers;
import net.blay09.mods.kuma.api.Kuma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.mcaccess.minecraftaccess.Config;
import org.mcaccess.minecraftaccess.MainClass;
import org.mcaccess.minecraftaccess.utils.KeyMappingCategories;
import org.mcaccess.minecraftaccess.utils.ModifierUtils;
import org.mcaccess.minecraftaccess.utils.condition.Interval;
import org.mcaccess.minecraftaccess.utils.events.ClientPlayingTick;

/**
 * Scans the area for entities, groups them and plays a sound at their location.
 */
@Slf4j
public class POIEntities implements BalmClientModule {
    private static final Config.POI.Entities CONFIG = Config.getInstance().poi.entities;
    private final Interval interval = Interval.defaultDelay();

    private @Nullable Entity markedEntity = null;

    private final POIGroup<Entity> markedGroup = new POIGroup<>(
            "minecraft_access.point_of_interest.group.markedEntity",
            new POIGroup.Sound(SoundEvents.ITEM_PICKUP, -5.0f),
            e -> markedEntity != null && markedEntity.getClass().isInstance(e)
    );

    private final POIGroup<Entity> otherEntitiesGroup = new POIGroup<>(
            "minecraft_access.point_of_interest.group.otherEntities",
            _ -> true
    );

    @SuppressWarnings("unchecked")
    final POIGroup<Entity>[] groups = Stream.of(List.of(markedGroup), BuiltinEntityPOIGroups.ALL, List.of(otherEntitiesGroup))
            .flatMap(Collection::stream).toArray(POIGroup[]::new);

    @Getter
    private List<Entity> lastScanResults = new ArrayList<>();

    POIEntities() {
        interval.setDelay(CONFIG.delay, Interval.Unit.MILLISECOND);
    }

    @Override
    public @NotNull Identifier getId() {
        return Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "poi/entities");
    }

    @Override
    public void initialize() {
        ClientPlayingTick.AFTER.register(this::tick);
        ClientLifecycleCallback.ConnectedToServer.EVENT.register(_ -> {
            markedEntity = null;
            lastScanResults = new ArrayList<>();
        });

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "poi.toggle_hostile_radar"))
                .withDefault(InputBinding.key(InputConstants.KEY_F6, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasControlAndAlt()) return false;
                    toggleHostileRadar();
                    return true;
                })
                .build();

        // Interruttori suono radar POI Entità (Ctrl+Alt+H e P) — Rev MC-26.22
        // Ctrl+Alt+H: silenzia il radar periodico 3s per HOSTILE (DISTINTO da F6 sentinella ravvicinata)
        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "poi.sound_toggle.entities.hostile"))
                .withDefault(InputBinding.key(InputConstants.KEY_H, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasControlAndAlt()) return false;
                    toggleSoundHostile();
                    return true;
                })
                .build();

        Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "poi.sound_toggle.entities.passive"))
                .withDefault(InputBinding.key(InputConstants.KEY_P, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)))
                .overrideCategory(KeyMappingCategories.OTHER)
                .handleWorldInput(_ -> {
                    if (!ModifierUtils.hasControlAndAlt()) return false;
                    toggleSoundPassive();
                    return true;
                })
                .build();
    }

    public void toggleHostileRadar() {
        CONFIG.hostileThreatAlerts = !CONFIG.hostileThreatAlerts;
        Config.getInstance().save();
        if (CONFIG.hostileThreatAlerts) {
            MainClass.narrate(net.minecraft.client.resources.language.I18n.get("minecraft_access.threat.radar_on"), true);
        } else {
            MainClass.narrate(net.minecraft.client.resources.language.I18n.get("minecraft_access.threat.radar_off"), true);
        }
    }

    public void toggleSoundHostile() {
        CONFIG.soundEnabledHostile = !CONFIG.soundEnabledHostile;
        Config.getInstance().save();
        MainClass.narrate(net.minecraft.client.resources.language.I18n.get(CONFIG.soundEnabledHostile
                ? "minecraft_access.poi.sound_toggle.entities.hostile.on"
                : "minecraft_access.poi.sound_toggle.entities.hostile.off"), true);
    }

    public void toggleSoundPassive() {
        CONFIG.soundEnabledPassive = !CONFIG.soundEnabledPassive;
        Config.getInstance().save();
        MainClass.narrate(net.minecraft.client.resources.language.I18n.get(CONFIG.soundEnabledPassive
                ? "minecraft_access.poi.sound_toggle.entities.passive.on"
                : "minecraft_access.poi.sound_toggle.entities.passive.off"), true);
    }

    private void tick(Minecraft client, Player player, Level level) {
        Object currentMarkedObject = MainClass.poiManager.poiMarking.getMarkedObject().value;
        if (currentMarkedObject instanceof Entity entity) {
            markedEntity = entity;
        } else {
            markedEntity = null;
        }

        interval.setDelay(CONFIG.delay, Interval.Unit.MILLISECOND);

        if (!CONFIG.enabled) return;
        if (!interval.isReady()) return;

        if (client.gui.screen() != null) return; //Prevent running if any screen is opened

        log.trace("POIEntities started");
        scanEntitiesAroundPlayer();
        playerSoundAtFoundPOI(MainClass.poiManager.poiMarking.getMarkedObject().value != null);
        log.trace("POIEntities ended");
    }

    private long lastThreatAlertTime = 0;

    private void scanEntitiesAroundPlayer() {
        // initialize
        List<Entity> currentScanResults = new ArrayList<>();
        for (POIGroup<Entity> group : groups) {
            group.clear();
        }

        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        AABB scanBox = player.getBoundingBox().inflate(CONFIG.range, CONFIG.range, CONFIG.range);
        assert Minecraft.getInstance().level != null;
        List<Entity> entities = Minecraft.getInstance().level.getEntities(player, scanBox);

        for (Entity entity : entities) {
            for (POIGroup<Entity> group : groups) {
                if (group.addIfQualified(entity)) {
                    currentScanResults.add(entity);
                    break;
                }
            }
        }

        lastScanResults = currentScanResults;

        // Hostile Threat Sentinel: check for Enemy / Hostile entities within 6 blocks
        if (CONFIG.hostileThreatAlerts) {
            long now = System.currentTimeMillis();
            if (now - lastThreatAlertTime >= 3500) {
                Entity closestHostile = null;
                double minThreatDistSq = 36.0; // 6 blocks
                for (Entity entity : currentScanResults) {
                    if (entity instanceof net.minecraft.world.entity.monster.Enemy && entity.isAlive()) {
                        double distSq = player.distanceToSqr(entity);
                        if (distSq <= minThreatDistSq) {
                            minThreatDistSq = distSq;
                            closestHostile = entity;
                        }
                    }
                }

                if (closestHostile != null) {
                    lastThreatAlertTime = now;
                    if (player.level() != null) {
                        player.level().playLocalSound(closestHostile.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), net.minecraft.sounds.SoundSource.HOSTILE, 0.75f, 0.6f, true);
                    }
                    String entityName = closestHostile.getName().getString();
                    String relPos = org.mcaccess.minecraftaccess.utils.NarrationUtils.narrateRelativePositionOfPlayerAnd(closestHostile.blockPosition());
                    String alertMsg = net.minecraft.client.resources.language.I18n.get("minecraft_access.threat.hostile_nearby", entityName, relPos);
                    MainClass.narrate(alertMsg, true);
                }
            }
        }
    }

    private void playerSoundAtFoundPOI(boolean isMarking) {
        if (CONFIG.volume == 0.0f) return;
        Function<Entity, Vec3> mapper = e -> Vec3.atCenterOf(e.blockPosition());
        if (isMarking && Config.getInstance().poi.marking.suppressOtherWhenEnabled) {
            markedGroup.playSoundForGroupItems(mapper, CONFIG.volume);
        } else if (CONFIG.playSound) {
            for (POIGroup<Entity> group : groups) {
                group.playSoundForGroupItems(mapper, CONFIG.volume);
            }
        }
    }
}
