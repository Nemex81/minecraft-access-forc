package org.mcaccess.minecraftaccess.features.point_of_interest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mcaccess.minecraftaccess.MainClass;

@Slf4j
public class POIGroup<T> {
    private final String nameTranslateKey;
    private final Sound sound;

    private final Predicate<T> predicate;
    private final List<T> items = new ArrayList<>();

    public POIGroup(String nameTranslateKey, Sound sound, Predicate<T> predicate) {
        this.nameTranslateKey = nameTranslateKey;
        this.sound = sound;
        this.predicate = predicate;
    }

    public POIGroup(String nameTranslateKey, Predicate<T> predicate) {
        this(nameTranslateKey, new Sound(null, 0), predicate);
    }

    public String getTranslatedName() {
        return I18n.get(nameTranslateKey);
    }

    /**
     * @return true if item was added
     */
    public boolean addIfQualified(T item) {
        if (!MainClass.poiManager.objectTracker.isObjectValid(item)) return false;

        if (predicate.test(item)) {
            log.debug("[{}] Add POI item [{}]", getTranslatedName(), item);
            items.add(item);
            return true;
        }
        return false;
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Contract(pure = true)
    public @UnmodifiableView List<T> getItems() {
        return Collections.unmodifiableList(items);
    }

    public @UnmodifiableView List<T> sortByDistance() {
        List<T> result = new ArrayList<>(items);
        Map<T, Double> cache = result.stream().collect(Collectors.toMap(k -> k, this::distanceBetweenPlayerAnd));
        result.sort(Comparator.comparing(cache::get));
        return Collections.unmodifiableList(result);
    }

    private double distanceBetweenPlayerAnd(T item) {
        LocalPlayer player = Minecraft.getInstance().player;
        return switch (item) {
            case Entity entity -> {
                assert player != null;
                yield player.distanceTo(entity);
            }
            case BlockPos blockPos -> {
                assert player != null;
                yield player.getEyePosition().distanceTo(Vec3.atCenterOf(blockPos));
            }
            case org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.Waypoint waypoint -> {
                assert player != null;
                Vec3 target = org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointUtils.getTargetVectorForPlayer(waypoint, player, true);
                if (target != null) {
                    yield player.getEyePosition().distanceTo(target);
                }
                yield Double.MAX_VALUE;
            }
            default -> Double.MAX_VALUE;
        };
    }

    public void playSoundForGroupItems(Function<T, Vec3> mapper, float volume) {
        LocalPlayer player = Minecraft.getInstance().player;
        net.minecraft.world.level.Level level = Minecraft.getInstance().level;
        org.mcaccess.minecraftaccess.Config.POI.WallOcclusionFeedbackMode mode = org.mcaccess.minecraftaccess.Config.getInstance().poi.wallOcclusionFeedback;
        boolean applyAttenuation = (mode == org.mcaccess.minecraftaccess.Config.POI.WallOcclusionFeedbackMode.SOUND_AND_VOICE
                || mode == org.mcaccess.minecraftaccess.Config.POI.WallOcclusionFeedbackMode.SOUND_ONLY);

        for (T item : items) {
            Vec3 pos = mapper.apply(item);
            float effectiveVolume = volume;
            if (applyAttenuation && player != null && level != null) {
                effectiveVolume *= org.mcaccess.minecraftaccess.utils.audio.AcousticOcclusion.getVolumeMultiplier(player.getEyePosition(), pos, level);
            }
            sound.play(pos, effectiveVolume);
        }
    }

    public record Sound(SoundEvent sound, float pitch) {
        static Logger log = LoggerFactory.getLogger(POIGroup.Sound.class);

        public void play(Vec3 pos, float volume) {
            if (sound == null) return;
            log.debug("Play POI sound [{}] at [x:{} y:{} z{}]", sound, pos.x, pos.y, pos.z);
            assert Minecraft.getInstance().level != null;
            Minecraft.getInstance().level.playLocalSound(pos.x, pos.y, pos.z, sound, SoundSource.BLOCKS, volume, pitch, true);
        }
    }
}
