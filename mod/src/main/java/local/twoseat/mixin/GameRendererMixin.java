package local.twoseat.mixin;

import local.twoseat.SplitSession;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void twoseat$render(float partialTick, long nanos, boolean renderWorld, CallbackInfo ci) {
        if (SplitSession.render(partialTick, nanos, renderWorld)) ci.cancel();
        else local.twoseat.WeatherProbe.view(SplitSession.isGuest(), partialTick);
    }
}
