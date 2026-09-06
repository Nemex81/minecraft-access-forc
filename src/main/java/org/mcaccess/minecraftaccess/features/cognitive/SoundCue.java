package org.mcaccess.minecraftaccess.features.cognitive;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable 3D spatial sound cue associated with a cognitive event.
 */
public record SoundCue(
        @Nullable SoundEvent soundEvent,
        SoundSource soundSource,
        @Nullable BlockPos position,
        float volume,
        float pitch
) {
    public static SoundCue of(
            @Nullable SoundEvent soundEvent,
            SoundSource soundSource,
            @Nullable BlockPos position,
            float volume,
            float pitch
    ) {
        return new SoundCue(soundEvent, soundSource, position, volume, pitch);
    }
}
