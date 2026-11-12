package local.twoseat.mixin;

import local.twoseat.VoxyCallbacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.cortex.voxy.client.core.VoxyRenderSystem", remap = false)
public abstract class VoxyRendererCallbacksMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lme/cortex/voxy/common/world/WorldEngine;setDirtyCallback(Lme/cortex/voxy/common/world/WorldEngine$ISectionChangeCallback;)V"), remap = false)
    private void twoseat$watchWorld(@Coerce Object world, @Coerce Object callback) {
        VoxyCallbacks.register(world, "setDirtyCallback", callback, this);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lme/cortex/voxy/common/world/other/Mapper;setBiomeCallback(Ljava/util/function/Consumer;)V"), remap = false)
    private void twoseat$watchBiomes(@Coerce Object mapper, @Coerce Object callback) {
        VoxyCallbacks.register(mapper, "setBiomeCallback", callback, this);
    }

    @Inject(method = "releaseResources", at = @At("HEAD"), remap = false)
    private void twoseat$unwatch(CallbackInfo ci) { VoxyCallbacks.release(this); }
}
