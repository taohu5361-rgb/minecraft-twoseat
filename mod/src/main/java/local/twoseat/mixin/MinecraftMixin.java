package local.twoseat.mixin;

import local.twoseat.SplitSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void twoseat$frameSample(boolean renderLevel, CallbackInfo ci) {
        local.twoseat.WeatherProbe.frame();
    }
    @Inject(method = "updateScreenAndTick", at = @At("HEAD"), cancellable = true)
    private void twoseat$noNestedFrame(Screen screen, CallbackInfo ci) {
        if (SplitSession.isGuest()) {
            ((Minecraft)(Object)this).setScreen(screen);
            ci.cancel();
        }
    }
}
