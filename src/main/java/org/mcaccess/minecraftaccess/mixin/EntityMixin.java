package org.mcaccess.minecraftaccess.mixin;

import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import org.mcaccess.minecraftaccess.Config;

@Mixin(Entity.class)
abstract class EntityMixin {
    @ModifyArg(
            method = "playStepSound",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"),
            index = 1,
            require = 0
    )
    private float modifyPlayStepSoundVolume(float volume) {
        if (Objects.equals(Minecraft.getInstance().player, this)) {
            return volume * (Config.getInstance().features.playerStepSoundVolume / 100.0f);
        }
        return volume;
    }

    @ModifyArg(
            method = "playCombinationStepSounds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"),
            index = 1,
            require = 0
    )
    private float modifyPlayCombinationStepSoundsVolume(float volume) {
        if (Objects.equals(Minecraft.getInstance().player, this)) {
            return volume * (Config.getInstance().features.playerStepSoundVolume / 100.0f);
        }
        return volume;
    }

    @ModifyArg(
            method = "playMuffledStepSound",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"),
            index = 1,
            require = 0
    )
    private float modifyPlayMuffledStepSoundVolume(float volume) {
        if (Objects.equals(Minecraft.getInstance().player, this)) {
            return volume * (Config.getInstance().features.playerStepSoundVolume / 100.0f);
        }
        return volume;
    }
}
