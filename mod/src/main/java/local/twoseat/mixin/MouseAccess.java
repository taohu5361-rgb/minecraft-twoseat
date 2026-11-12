package local.twoseat.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseAccess {
    @Accessor("xpos") double twoseat$rawX();
    @Accessor("ypos") double twoseat$rawY();
}
