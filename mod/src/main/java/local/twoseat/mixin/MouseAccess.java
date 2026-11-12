package local.twoseat.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseAccess {
    @Invoker("onMove") void twoseat$move(long window,double x,double y);
    @Invoker("onPress") void twoseat$press(long window,int button,int action,int modifiers);
    @Accessor("xpos") double twoseat$rawX();
    @Accessor("ypos") double twoseat$rawY();
}
