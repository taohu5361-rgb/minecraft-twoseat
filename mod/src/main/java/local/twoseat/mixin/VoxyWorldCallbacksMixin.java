package local.twoseat.mixin;

import local.twoseat.VoxyCallbacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.cortex.voxy.common.world.WorldEngine", remap = false)
public abstract class VoxyWorldCallbacksMixin {
    @Inject(method = "setDirtyCallback", at = @At("HEAD"), cancellable = true, remap = false)
    private void twoseat$keepOtherView(@Coerce Object callback, CallbackInfo ci) {
        if (VoxyCallbacks.keepDispatcher(this, "setDirtyCallback", callback)) ci.cancel();
    }
}
