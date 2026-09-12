package org.mcaccess.minecraftaccess.mixin;

import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.mcaccess.minecraftaccess.utils.ModifierUtils;

/**
 * Impedisce l'apertura della schermata di debug (F3) quando Ctrl+Alt sono premuti insieme,
 * proteggendo l'interruttore buche corto raggio (Ctrl+Alt+F3) da interferenze visive (Rev MC-26.22).
 */
@Mixin(DebugScreenEntryList.class)
abstract class DebugScreenEntryListMixin {

    @Inject(method = "toggleDebugOverlay", at = @At("HEAD"), cancellable = true)
    private void suppressDebugOverlayWhenCtrlAlt(CallbackInfo ci) {
        if (ModifierUtils.hasControlAndAlt()) {
            ci.cancel();
        }
    }
}