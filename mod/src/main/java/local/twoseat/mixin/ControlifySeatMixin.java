// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat.mixin;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import local.twoseat.ControllerSeat;
import local.twoseat.ControllerProbe;
import dev.isxander.controlify.controller.ControllerEntity;
import local.twoseat.SplitSession;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Controlify.class, remap = false)
public abstract class ControlifySeatMixin {
    @Inject(method = "tickController", at = @At("HEAD"))
    private void twoseat$testDevice(ControllerEntity controller, boolean outOfFocus, CallbackInfo ci) {
        ControllerProbe.poll(controller, outOfFocus);
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void twoseat$guestTick(Minecraft mc, CallbackInfo ci) {
        if (!SplitSession.isGuest()) ci.cancel();
    }
    @Inject(method = "currentInputMode", at = @At("HEAD"), cancellable = true)
    private void twoseat$mode(CallbackInfoReturnable<InputMode> ci) {
        ci.setReturnValue(SplitSession.isGuest() && ControllerSeat.active() ? InputMode.CONTROLLER : InputMode.KEYBOARD_MOUSE);
    }
    @Inject(method = "setInputMode", at = @At("HEAD"), cancellable = true)
    private void twoseat$noGlobalMode(InputMode mode, CallbackInfoReturnable<Boolean> ci) {
        // P1 keyboard events must not disable P2 or clear either player's binding history.
        ci.setReturnValue(false);
    }
    @Inject(method = "hideMouse", at = @At("HEAD"), cancellable = true)
    private void twoseat$noPhysicalCursor(boolean hide, boolean move, CallbackInfo ci) {
        if (SplitSession.isGuest()) ci.cancel();
    }
}
