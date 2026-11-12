// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat.mixin;

import dev.isxander.controlify.gui.guide.InGameButtonGuide;
import local.twoseat.ControllerSeat;
import local.twoseat.SplitSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameButtonGuide.class, remap = false)
public abstract class ControllerGuideMixin {
    @Inject(method = "renderHud", at = @At("HEAD"), cancellable = true)
    private void twoseat$guestGuide(CallbackInfo ci) {
        if (!SplitSession.isGuest() || !ControllerSeat.active()) ci.cancel();
    }
}
