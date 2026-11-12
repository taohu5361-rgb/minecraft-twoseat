// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat.mixin;

import dev.isxander.controlify.bindings.output.KeyMappingEmulationOutput;
import local.twoseat.ControllerSeat;
import local.twoseat.SplitSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyMappingEmulationOutput.class, remap = false)
public abstract class ControllerKeyOutputMixin {
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void twoseat$guestKeysOnly(CallbackInfo ci) {
        if (!SplitSession.isGuest() || !ControllerSeat.active()) ci.cancel();
    }
}
