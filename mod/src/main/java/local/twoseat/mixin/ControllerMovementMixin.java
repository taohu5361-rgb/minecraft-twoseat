// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat.mixin;

import dev.isxander.controlify.ingame.ControllerPlayerMovement;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ControllerPlayerMovement.class, remap = false)
public abstract class ControllerMovementMixin {
    @Inject(method = {"updatePlayerInput", "ensureCorrectInput"}, at = @At("HEAD"), cancellable = true)
    private static void twoseat$keepSeatInput(LocalPlayer player, CallbackInfo ci) {
        // TwoSeat owns KeyboardInput for P1 and SecondInput for P2; never install DualInput.
        ci.cancel();
    }
}
