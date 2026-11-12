package local.twoseat;

import com.google.gson.JsonObject;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Opt-in regression: real transformed upstream mip output, serial versus two callers. */
final class VoxyMipProbe {
    static JsonObject run() throws Exception {
        if (!DevProbe.ENABLED) throw new IllegalStateException("Probe disabled");
        Class<?> texture = Class.forName("me.cortex.voxy.client.core.model.ColourDepthTextureData");
        Class<?> memory = Class.forName("me.cortex.voxy.common.util.MemoryBuffer");
        Method put = Class.forName("me.cortex.voxy.client.core.model.MipGen").getMethod("putTextures",
            boolean.class, Array.newInstance(texture, 0).getClass(), memory);
        Method zero = memory.getMethod("zero"), bytes = memory.getMethod("asByteBuffer"), free = memory.getMethod("free");
        Object[] inputs = new Object[8];
        for (int n = 0; n < inputs.length; n++) {
            Object faces = Array.newInstance(texture, 6);
            Random random = new Random(9321L + n);
            for (int f = 0; f < 6; f++) {
                int[] colour = new int[256], depth = new int[256];
                for (int i = 0; i < colour.length; i++) {
                    colour[i] = random.nextInt(0x1000000) | (random.nextBoolean() ? 0xff000000 : 0);
                    depth[i] = random.nextInt(256);
                }
                Array.set(faces, f, texture.getConstructor(int[].class, int[].class, int.class, int.class)
                    .newInstance(colour, depth, 16, 16));
            }
            inputs[n] = faces;
        }
        byte[][] expected = new byte[inputs.length][];
        Object baseline = memory.getConstructor(long.class).newInstance(8192L);
        try {
            for (int i = 0; i < inputs.length; i++) {
                zero.invoke(baseline); put.invoke(null, false, inputs[i], baseline);
                expected[i] = new byte[8192]; ((ByteBuffer)bytes.invoke(baseline)).get(expected[i]);
            }
        } finally { free.invoke(baseline); }
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int worker = 0; worker < 2; worker++) {
                final int offset = worker * 3;
                tasks.add(pool.submit(() -> {
                    Object buffer = null;
                    try {
                        buffer = memory.getConstructor(long.class).newInstance(8192L);
                        byte[] actual = new byte[8192]; start.await();
                        for (int i = 0; i < 1000; i++) {
                            int n = (i + offset) % inputs.length;
                            zero.invoke(buffer); put.invoke(null, false, inputs[n], buffer);
                            ((ByteBuffer)bytes.invoke(buffer)).get(actual);
                            if (!Arrays.equals(expected[n], actual)) throw new AssertionError("Concurrent mip output differs from serial reference");
                        }
                    } catch (Exception e) { throw new RuntimeException(e); }
                    finally { if (buffer != null) try { free.invoke(buffer); } catch (Exception e) { throw new RuntimeException(e); } }
                }));
            }
            start.countDown();
            for (var task : tasks) task.get(30, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        JsonObject result = new JsonObject();
        result.addProperty("passed", true); result.addProperty("threads", 2);
        result.addProperty("callsPerThread", 1000); result.addProperty("patterns", inputs.length);
        result.addProperty("outputMatchesSerialBytes", true);
        return result;
    }
    private VoxyMipProbe() {}
}
