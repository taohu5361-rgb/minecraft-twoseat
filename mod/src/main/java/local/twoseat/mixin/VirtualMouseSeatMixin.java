// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat.mixin;

import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import local.twoseat.SplitSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VirtualMouseHandler.class, remap = false)
public abstract class VirtualMouseSeatMixin {
    @Shadow private boolean virtualMouseEnabled;
    @Inject(method = "enableVirtualMouse", at = @At("HEAD"))
    private void twoseat$centerNewCursor(CallbackInfo ci) {
        if (SplitSession.isGuest() && !virtualMouseEnabled) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            ((dev.isxander.controlify.mixins.feature.virtualmouse.MouseHandlerAccessor)mc.mouseHandler)
                .invokeOnMove(mc.getWindow().getWindow(), mc.getWindow().getScreenWidth() / 2.0, mc.getWindow().getScreenHeight() / 2.0);
        }
    }
    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void twoseat$guestMouse(CallbackInfo ci) {
        if (!SplitSession.isGuest()) ci.cancel();
    }
    @Inject(method = "setMousePosition", at = @At("HEAD"), cancellable = true)
    private void twoseat$noWarp(CallbackInfo ci) {
        if (SplitSession.isGuest()) ci.cancel();
    }
    @Redirect(method = {"enableVirtualMouse", "disableVirtualMouse"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetInputMode(JII)V"))
    private void twoseat$noCursorMode(long window, int mode, int value) {
        if (!SplitSession.isGuest()) org.lwjgl.glfw.GLFW.glfwSetInputMode(window, mode, value);
    }
}
