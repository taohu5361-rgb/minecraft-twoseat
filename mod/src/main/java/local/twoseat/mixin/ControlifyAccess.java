// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat.mixin;

import dev.isxander.controlify.Controlify;
import java.util.Queue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Controlify.class, remap = false)
public interface ControlifyAccess {
    @Accessor("setupWizards") Queue<?> twoseat$wizards();
    @Accessor("currentSetupWizard") void twoseat$wizard(dev.isxander.controlify.utils.ControllerSetupWizard wizard);
}
