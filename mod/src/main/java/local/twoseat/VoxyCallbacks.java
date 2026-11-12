package local.twoseat;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Fan out shared Voxy data changes to each live renderer, with renderer-scoped cleanup. */
public final class VoxyCallbacks {
    private static final Object lock = new Object();
    private static final Map<Object, Map<String, Slot>> sources = new WeakHashMap<>();
    private static final Map<Object, List<Slot>> owners = new IdentityHashMap<>();

    private static final class Entry {
        final Object callback;
        boolean live = true;
        Entry(Object callback) { this.callback = callback; }
        synchronized void call(Method method, Object[] arguments) throws Throwable {
            if (!live) return;
            try { method.invoke(callback, arguments); }
            catch (InvocationTargetException failure) { throw failure.getCause(); }
        }
        synchronized void stop() { live = false; }
    }

    private static final class Slot {
        final WeakReference<Object> source;
        final Method setter;
        final Object proxy;
        final Map<Object, Entry> entries = new IdentityHashMap<>();
        volatile Entry[] snapshot = new Entry[0];

        Slot(Object source, String name) {
            this.source = new WeakReference<>(source);
            setter = java.util.Arrays.stream(source.getClass().getMethods())
                .filter(m -> m.getName().equals(name) && m.getParameterCount() == 1).findFirst().orElseThrow();
            Class<?> type = setter.getParameterTypes()[0];
            proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(self);
                    case "equals" -> self == arguments[0];
                    case "toString" -> "TwoSeat Voxy callback dispatcher";
                    default -> throw new UnsupportedOperationException(method.getName());
                };
                Throwable failure = null;
                for (Entry entry : snapshot) {
                    try { entry.call(method, arguments); }
                    catch (Throwable problem) { if (failure == null) failure = problem; else failure.addSuppressed(problem); }
                }
                if (failure != null) throw failure;
                return null;
            });
        }
        void refresh() { snapshot = entries.values().toArray(Entry[]::new); }
        void install() {
            try { setter.invoke(source.get(), proxy); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot install Voxy callbacks", failure); }
        }
    }

    public static void register(Object source, String setter, Object callback, Object owner) {
        Slot slot;
        Entry old;
        synchronized (lock) {
            var methods = sources.computeIfAbsent(source, unused -> new java.util.HashMap<>());
            slot = methods.computeIfAbsent(setter, name -> new Slot(source, name));
            old = slot.entries.put(owner, new Entry(callback));
            var owned = owners.computeIfAbsent(owner, unused -> new ArrayList<>());
            if (!owned.contains(slot)) owned.add(slot);
            slot.refresh();
        }
        if (old != null) old.stop();
        // No registry/entry lock is held while calling into Voxy's mapper.
        slot.install();
    }

    /** A renderer clearing its singleton callback must not clear its sibling's dispatcher. */
    public static boolean keepDispatcher(Object source, String setter, Object callback) {
        if (callback != null) return false;
        synchronized (lock) {
            var methods = sources.get(source);
            Slot slot = methods == null ? null : methods.get(setter);
            return slot != null && slot.snapshot.length != 0;
        }
    }

    public static void release(Object owner) {
        List<Entry> retired = new ArrayList<>();
        synchronized (lock) {
            List<Slot> slots = owners.remove(owner);
            if (slots == null) return;
            for (Slot slot : slots) {
                Entry entry = slot.entries.remove(owner);
                if (entry != null) retired.add(entry);
                slot.refresh();
            }
        }
        // Finish any in-flight calls before the upstream renderer frees its queues/buffers.
        retired.forEach(Entry::stop);
    }

    static int count(Object source, String setter) {
        synchronized (lock) {
            var methods = sources.get(source);
            Slot slot = methods == null ? null : methods.get(setter);
            return slot == null ? 0 : slot.snapshot.length;
        }
    }
    private VoxyCallbacks() {}
}
