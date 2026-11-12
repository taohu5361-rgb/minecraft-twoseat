package local.twoseat;

import java.util.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import local.twoseat.mixin.ParticleAccess;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleGroup;

/** Per-view live particles; provider registry, sprite atlas and textures remain shared. */
final class ParticleState {
    private Map<ParticleRenderType, Queue<Particle>> particles = new IdentityHashMap<>();
    private Queue<TrackingEmitter> emitters = new ArrayDeque<>();
    private Queue<Particle> pending = new ArrayDeque<>();
    private Object2IntOpenHashMap<ParticleGroup> counts = new Object2IntOpenHashMap<>();

    void capture(ParticleEngine engine) {
        ParticleAccess a = (ParticleAccess)engine;
        particles = a.twoseat$particles(); emitters = a.twoseat$emitters();
        pending = a.twoseat$pending(); counts = a.twoseat$counts();
    }
    void apply(ParticleEngine engine) {
        ParticleAccess a = (ParticleAccess)engine;
        a.twoseat$particles(particles); a.twoseat$emitters(emitters);
        a.twoseat$pending(pending); a.twoseat$counts(counts);
    }
}
