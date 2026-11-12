package local.twoseat.mixin;

import local.twoseat.SplitSession;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Shadow private double xpos;
    @Inject(method = "xpos", at = @At("HEAD"), cancellable = true)
    private void twoseat$mouseX(CallbackInfoReturnable<Double> ci) {
        Double x = SplitSession.renderMouseX(xpos);
        if (x != null) ci.setReturnValue(x);
    }
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void twoseat$turn(CallbackInfo ci) {
        if (SplitSession.isGuest()) ci.cancel();
    }
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void twoseat$press(long window, int button, int action, int mods, CallbackInfo ci) {
        if (SplitSession.menuMouse(button, action)) ci.cancel();
    }
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void twoseat$scroll(long window, double x, double y, CallbackInfo ci) {
        if (SplitSession.menuScroll(y)) ci.cancel();
    }
}
