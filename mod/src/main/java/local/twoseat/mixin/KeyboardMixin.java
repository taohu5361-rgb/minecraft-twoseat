package local.twoseat.mixin;

import local.twoseat.SplitSession;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void twoseat$key(long window, int key, int scan, int action, int mods, CallbackInfo ci) {
        SplitSession.keyEvent(key, action);
        if (SplitSession.menuKey(key, scan, action, mods)) ci.cancel();
        else if (SplitSession.reserveKey(key)) ci.cancel();
    }
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void twoseat$char(long window, int codepoint, int mods, CallbackInfo ci) {
        if (SplitSession.menuChar(codepoint, mods)) ci.cancel();
    }
}
