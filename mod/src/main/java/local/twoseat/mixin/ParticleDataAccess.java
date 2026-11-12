package local.twoseat.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleDataAccess {
    @Accessor("age") int twoseat$age();
    @Accessor("level") ClientLevel twoseat$level();
}
