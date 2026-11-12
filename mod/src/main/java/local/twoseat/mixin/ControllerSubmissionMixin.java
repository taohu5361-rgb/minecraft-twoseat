// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat.mixin;

import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.gui.screen.SubmitUnknownControllerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TwoSeat does not offer or send controller-identification submissions. */
@Mixin(value = SubmitUnknownControllerScreen.class, remap = false)
public abstract class ControllerSubmissionMixin {
    @Inject(method = "canSubmit", at = @At("HEAD"), cancellable = true)
    private static void twoseat$noSubmissionScreen(ControllerEntity controller, CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(false);
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void twoseat$noSubmissionRequest(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(false);
    }
}
