package local.twoseat.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mutable;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleGroup;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

@Mixin(ParticleEngine.class)
public interface ParticleAccess {
    @Accessor("level") void twoseat$level(ClientLevel level);
    @Accessor("particles") Map<ParticleRenderType, Queue<Particle>> twoseat$particles();
    @Mutable @Accessor("particles") void twoseat$particles(Map<ParticleRenderType, Queue<Particle>> value);
    @Accessor("trackingEmitters") Queue<TrackingEmitter> twoseat$emitters();
    @Mutable @Accessor("trackingEmitters") void twoseat$emitters(Queue<TrackingEmitter> value);
    @Accessor("particlesToAdd") Queue<Particle> twoseat$pending();
    @Mutable @Accessor("particlesToAdd") void twoseat$pending(Queue<Particle> value);
    @Accessor("trackedParticleCounts") Object2IntOpenHashMap<ParticleGroup> twoseat$counts();
    @Mutable @Accessor("trackedParticleCounts") void twoseat$counts(Object2IntOpenHashMap<ParticleGroup> value);
}
