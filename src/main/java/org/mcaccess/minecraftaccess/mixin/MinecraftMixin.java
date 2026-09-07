package org.mcaccess.minecraftaccess.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.mcaccess.minecraftaccess.features.AccessMenu;
import org.mcaccess.minecraftaccess.features.door.DoorInteractionHelper;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Shadow
    public abstract boolean hasAltDown();

    @Shadow
    public HitResult hitResult;

    /**
     * {@link AccessMenu} allows menu functions to be triggered when
     * no screen opened and alt key with number key are pressed.
     * We need to suppress original hotbar slot selecting feature.
     */
    @Inject(method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"),
            cancellable = true)
    private void suppressHotbarSlotSelecting(CallbackInfo ci) {
        if (hasAltDown()) {
            ci.cancel();
        }
    }

    /**
     * Permissive door and gate interaction (Rev MC-26.12).
     * If the player is looking towards an open door/gate within reach, snaps hitResult
     * to the door block so right-click/interact reliably closes the door without requiring
     * pixel-perfect aiming at the 3-pixel open door frame.
     */
    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void permissiveDoorInteraction(CallbackInfo ci) {
        HitResult permissiveHit = DoorInteractionHelper.resolvePermissiveDoorHit((Minecraft) (Object) this);
        if (permissiveHit != null) {
            this.hitResult = permissiveHit;
        }
    }
}
