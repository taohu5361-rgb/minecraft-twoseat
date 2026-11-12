package local.twoseat;

import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

/** Optional bridge to an externally installed Voxy. No Voxy implementation is bundled. */
public final class VoxyCompat {
    private static final Map<Object, Float> distances = new WeakHashMap<>();
    private static Method getter;
    private static Field config, distance;
    private static boolean tried, available;

    private static boolean ready() {
        if (!tried) {
            tried = true;
            if (!ModList.get().isLoaded("voxy")) return false;
            try {
                Class<?> type = Class.forName("me.cortex.voxy.client.config.VoxyConfig");
                config = type.getField("CONFIG");
                distance = type.getField("sectionRenderDistance");
                getter = Class.forName("me.cortex.voxy.client.core.IGetVoxyRenderSystem").getMethod("voxy$getRenderSystem");
                available = true;
            } catch (ReflectiveOperationException e) {
                TwoSeat.LOG.warn("[TwoSeat Voxy] Unsupported Voxy API; distance bridge disabled", e);
            }
        }
        return available;
    }

    /** Upstream's settings page updates only the currently active LevelRenderer. */
    public static void beforeView() {
        if (!ready()) return;
        try {
            Object renderer = getter.invoke(Minecraft.getInstance().levelRenderer);
            if (renderer == null) return;
            float value = distance.getFloat(config.get(null));
            if (!Float.valueOf(value).equals(distances.get(renderer))) {
                renderer.getClass().getMethod("setRenderDistance", float.class).invoke(renderer, value);
                distances.put(renderer, value);
            }
        } catch (ReflectiveOperationException e) {
            available = false;
            TwoSeat.LOG.error("[TwoSeat Voxy] Distance bridge failed", e);
        }
    }

    static void testDistance(int chunks) throws ReflectiveOperationException {
        if (!DevProbe.ENABLED || !ready()) throw new IllegalStateException("Voxy probe is unavailable");
        if (chunks != 32 && chunks != 64 && chunks != 128) throw new IllegalArgumentException("Use the staged 32/64/128 distances");
        // Voxy's top-level sections are 512 blocks wide = 32 Minecraft chunks.
        distance.setFloat(config.get(null), chunks / 32.0f);
    }

    private static Object field(Object instance, String name) throws ReflectiveOperationException {
        Field f = instance.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(instance);
    }

    static JsonObject inspect() {
        JsonObject report = new JsonObject();
        report.addProperty("available", ready());
        if (!available) return report;
        try {
            report.addProperty("configuredChunks", distance.getFloat(config.get(null)) * 32);
            Object renderer = getter.invoke(Minecraft.getInstance().levelRenderer);
            report.addProperty("renderer", System.identityHashCode(renderer));
            if (renderer == null) return report;
            Object engine = field(renderer, "worldIn");
            report.addProperty("worldEngine", System.identityHashCode(engine));
            report.addProperty("dirtyListeners", VoxyCallbacks.count(engine, "setDirtyCallback"));
            Object mapper = engine.getClass().getMethod("getMapper").invoke(engine);
            report.addProperty("biomeListeners", VoxyCallbacks.count(mapper, "setBiomeCallback"));
            report.addProperty("activeWorldSections", ((Number)engine.getClass().getMethod("getActiveSectionCount").invoke(engine)).intValue());
            Object geometry = field(renderer, "geometryData");
            report.addProperty("geometrySections", ((Number)geometry.getClass().getMethod("getSectionCount").invoke(geometry)).intValue());
            java.util.List<String> lines = new java.util.ArrayList<>();
            renderer.getClass().getMethod("addDebugInfo", java.util.List.class).invoke(renderer, lines);
            com.google.gson.JsonArray debug = new com.google.gson.JsonArray();
            lines.forEach(debug::add);
            report.add("debug", debug);
            Object instance = Class.forName("me.cortex.voxy.commonImpl.VoxyCommon").getMethod("getInstance").invoke(null);
            Object imports = instance.getClass().getMethod("getImportManager").invoke(instance);
            Field active = Class.forName("me.cortex.voxy.commonImpl.ImportManager").getDeclaredField("activeImporters");
            active.setAccessible(true);
            synchronized (imports) {
                report.addProperty("activeImports", ((Map<?,?>)active.get(imports)).size());
            }
            Object tracker = field(renderer, "renderDistanceTracker");
            report.addProperty("topLevelRingRadius", (int)field(tracker, "renderDistance"));
            Object selector = field(renderer, "viewportSelector");
            Object viewport = selector.getClass().getMethod("getViewport").invoke(selector);
            report.addProperty("viewport", System.identityHashCode(viewport));
            report.addProperty("viewportWidth", viewport.getClass().getField("width").getInt(viewport));
            report.addProperty("viewportHeight", viewport.getClass().getField("height").getInt(viewport));
            for (String coordinate : new String[]{"cameraX", "cameraY", "cameraZ"})
                report.addProperty(coordinate, ((Number)viewport.getClass().getField(coordinate).get(viewport)).doubleValue());
        } catch (ReflectiveOperationException e) {
            report.addProperty("error", e.toString());
        }
        return report;
    }

    private VoxyCompat() {}
}
