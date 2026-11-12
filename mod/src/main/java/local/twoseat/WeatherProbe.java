package local.twoseat;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.twoseat.mixin.LevelRendererAccess;
import net.minecraft.client.Minecraft;

/** Bounded opt-in frame/animation measurements. Never active in ordinary play. */
public final class WeatherProbe {
    private static String id;
    private static long until, previous;
    private static final List<Double> frames = new ArrayList<>();
    private static int hostStart, guestStart, hostEnd, guestEnd, rewinds;
    private static double guestPhase = -1;
    private static boolean focused;
    private static final MobTrace hostMob = new MobTrace(273), guestMob = new MobTrace(546);

    private static final class MobTrace {
        final UUID uuid;
        int firstTick, lastTick, samples, rewinds;
        double phase, maxStep, previousX, previousY, previousZ;
        MobTrace(int low) { uuid = new UUID(0, low); }
        void clear() { samples = rewinds = firstTick = lastTick = 0; phase = -1; maxStep = 0; }
        void sample(Minecraft mc, float partial) {
            for (var entity : mc.level.entitiesForRendering()) if (entity.getUUID().equals(uuid)) {
                if (samples == 0) firstTick = entity.tickCount;
                double current = entity.tickCount + partial;
                if (phase >= 0 && current + .001 < phase) rewinds++;
                double x = net.minecraft.util.Mth.lerp(partial, entity.xo, entity.getX());
                double y = net.minecraft.util.Mth.lerp(partial, entity.yo, entity.getY());
                double z = net.minecraft.util.Mth.lerp(partial, entity.zo, entity.getZ());
                if (samples > 0) maxStep = Math.max(maxStep, Math.sqrt(Math.pow(x-previousX,2)+Math.pow(y-previousY,2)+Math.pow(z-previousZ,2)));
                previousX=x; previousY=y; previousZ=z;
                lastTick=entity.tickCount; phase=current; samples++;
                break;
            }
        }
        JsonObject report() {
            JsonObject value = new JsonObject(); value.addProperty("samples", samples);
            value.addProperty("ticksAdvanced", lastTick-firstTick); value.addProperty("animationClockRewinds", rewinds);
            value.addProperty("maxInterpolatedFrameStepBlocks", maxStep); return value;
        }
    }

    static void start(String name, int seconds, SplitSession session) {
        if (!DevProbe.ENABLED) return;
        id = name; until = System.nanoTime() + Math.min(60, Math.max(1, seconds)) * 1_000_000_000L;
        previous = 0; frames.clear(); rewinds = 0; guestPhase = -1; focused = true;
        hostStart = ((LevelRendererAccess)Minecraft.getInstance().levelRenderer).twoseat$ticks();
        guestStart = -1;
        hostMob.clear(); guestMob.clear();
        session.withGuest(() -> guestStart = ((LevelRendererAccess)Minecraft.getInstance().levelRenderer).twoseat$ticks());
        hostEnd = hostStart; guestEnd = guestStart;
    }

    public static void frame() {
        if (!DevProbe.ENABLED || id == null) return;
        long now = System.nanoTime();
        if (previous != 0) frames.add((now - previous) / 1_000_000.0);
        previous = now;
        focused &= Minecraft.getInstance().isWindowActive();
        if (now >= until) finish();
    }

    public static void view(boolean guest, float partial) {
        if (!DevProbe.ENABLED || id == null) return;
        int ticks = ((LevelRendererAccess)Minecraft.getInstance().levelRenderer).twoseat$ticks();
        double phase = ticks + partial;
        (guest ? guestMob : hostMob).sample(Minecraft.getInstance(), partial);
        if (guest) {
            if (guestPhase >= 0 && phase + .001 < guestPhase) rewinds++;
            guestPhase = phase; guestEnd = ticks;
        } else hostEnd = ticks;
    }

    private static void finish() {
        Minecraft mc = Minecraft.getInstance();
        JsonObject value = new JsonObject();
        double[] sorted = frames.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double total = frames.stream().mapToDouble(Double::doubleValue).sum();
        value.addProperty("frames", frames.size()); value.addProperty("seconds", total / 1000);
        value.addProperty("fps", 1000 * frames.size() / total);
        value.addProperty("vanillaFpsAtEnd", mc.getFps());
        if (sorted.length > 0) {
            value.addProperty("p50FrameMs", sorted[(int)((sorted.length - 1) * .50)]);
            value.addProperty("p95FrameMs", sorted[(int)((sorted.length - 1) * .95)]);
            value.addProperty("p99FrameMs", sorted[(int)((sorted.length - 1) * .99)]);
            int n = Math.max(1, (int)Math.ceil(sorted.length * .01));
            value.addProperty("onePercentLowFps", 1000 / Arrays.stream(sorted, sorted.length - n, sorted.length).average().orElseThrow());
        }
        value.addProperty("allFramesFocused", focused);
        value.addProperty("guestWeatherPhaseRewinds", rewinds);
        value.addProperty("hostRendererTicksAdvanced", hostEnd - hostStart);
        value.addProperty("guestRendererTicksAdvanced", guestEnd - guestStart);
        value.add("hostMob", hostMob.report()); value.add("guestMob", guestMob.report());
        value.addProperty("width", mc.getWindow().getWidth()); value.addProperty("height", mc.getWindow().getHeight());
        value.addProperty("gpu", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
        value.addProperty("renderDistance", mc.options.renderDistance().get());
        value.addProperty("simulationDistance", mc.options.simulationDistance().get());
        value.addProperty("vsync", mc.options.enableVsync().get());
        value.addProperty("maxFps", mc.options.framerateLimit().get());
        value.addProperty("graphics", mc.options.graphicsMode().get().toString());
        value.addProperty("particles", mc.options.particles().get().toString());
        var server = mc.getSingleplayerServer();
        if (server != null) value.addProperty("meanServerTickMs", server.getAverageTickTime());
        JsonArray samples = new JsonArray(); frames.forEach(samples::add); value.add("frameMs", samples);
        try {
            Path folder = mc.gameDirectory.toPath().resolve("twoseat-evidence"); Files.createDirectories(folder);
            Files.writeString(folder.resolve(id + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(value));
        } catch (Exception e) { TwoSeat.LOG.error("[TwoSeat probe] benchmark write failed", e); }
        TwoSeat.LOG.info("[TwoSeat probe] benchmark {} done: frames={} fps={} weatherRewinds={}", id, frames.size(), 1000 * frames.size() / total, rewinds);
        id = null; frames.clear();
    }
}
