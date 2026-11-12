// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat.mixin;

import dev.isxander.controlify.ingame.InGameInputHandler;
import local.twoseat.ControllerSeat;
import local.twoseat.SplitSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameInputHandler.class, remap = false)
public abstract class ControllerInGameMixin {
    @Shadow protected abstract void handlePlayerLookInput(boolean enabled);
    @Inject(method = "inputTick", at = @At("HEAD"), cancellable = true)
    private void twoseat$lookOnly(CallbackInfo ci) {
        // Reuse Controlify's sensitivity, stick curves and gyro; route actions in ControllerSeat.
        handlePlayerLookInput(SplitSession.isGuest() && ControllerSeat.active());
        ci.cancel();
    }
    @Inject(method = "processPlayerLook", at = @At("HEAD"), cancellable = true)
    private void twoseat$guestLook(float delta, CallbackInfo ci) {
        if (!SplitSession.isGuest() || !ControllerSeat.active()) ci.cancel();
    }
}
