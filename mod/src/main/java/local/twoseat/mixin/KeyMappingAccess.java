package local.twoseat.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccess {
    @Accessor("isDown") boolean twoseat$down();
    @Accessor("isDown") void twoseat$down(boolean value);
    @Accessor("clickCount") int twoseat$clicks();
    @Accessor("clickCount") void twoseat$clicks(int value);
}
