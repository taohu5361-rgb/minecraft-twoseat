package local.twoseat.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardAccess {
    @Invoker("keyPress") void twoseat$key(long window, int key, int scan, int action, int modifiers);
}
