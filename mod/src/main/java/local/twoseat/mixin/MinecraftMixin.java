package local.twoseat.mixin;

import local.twoseat.SplitSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Redirect(method="runTick",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;isPauseScreen()Z"))
    private boolean twoseat$noGlobalMenuPause(Screen screen) {
        return !SplitSession.isActive() && screen.isPauseScreen();
    }
    @Inject(method="pauseGame",at=@At("HEAD"),cancellable=true)
    private void twoseat$seatMenu(boolean lostFocus,CallbackInfo ci) {
        if (SplitSession.isActive()) {
            SplitSession.openMenu();
            ci.cancel(); // Vanilla also pauses the shared sound manager here.
        }
    }
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
