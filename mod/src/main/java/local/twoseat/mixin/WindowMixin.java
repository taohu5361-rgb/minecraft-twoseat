package local.twoseat.mixin;

import com.mojang.blaze3d.platform.Window;
import local.twoseat.SplitSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    private void twoseat$width(CallbackInfoReturnable<Integer> ci) {
        if (SplitSession.viewportWidth > 0) ci.setReturnValue(SplitSession.viewportWidth);
    }
    @Inject(method = "getGuiScaledWidth", at = @At("HEAD"), cancellable = true)
    private void twoseat$guiWidth(CallbackInfoReturnable<Integer> ci) {
        if (SplitSession.viewportWidth > 0)
            ci.setReturnValue((int)Math.ceil(SplitSession.viewportWidth / ((Window)(Object)this).getGuiScale()));
    }
}
