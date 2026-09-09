package org.mcaccess.minecraftaccess.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.mcaccess.minecraftaccess.utils.ModifierUtils;

/**
 * Neutralizza i comandi debug vanilla (F3 + tasto) quando Ctrl+Alt sono premuti insieme (Rev MC-26.22).
 */
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {

    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void suppressDebugKeysWhenCtrlAlt(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ModifierUtils.hasControlAndAlt()) {
            cir.setReturnValue(true);
        }
    }
}