package local.twoseat;

import java.util.IdentityHashMap;
import java.util.Map;
import local.twoseat.mixin.KeyMappingAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The physical keyboard and Controlify key emulation must never share held/click state. */
final class SeatKeys {
    private record State(boolean down, int clicks) {}
    private final Map<KeyMapping, State> states = new IdentityHashMap<>();

    void capture(Minecraft mc) {
        for (KeyMapping key : mc.options.keyMappings) {
            KeyMappingAccess access = (KeyMappingAccess)key;
            states.put(key, new State(access.twoseat$down(), access.twoseat$clicks()));
        }
    }

    void apply(Minecraft mc) {
        for (KeyMapping key : mc.options.keyMappings) {
            KeyMappingAccess access = (KeyMappingAccess)key;
            State state = states.get(key);
            access.twoseat$down(state != null && state.down());
            access.twoseat$clicks(state == null ? 0 : state.clicks());
        }
    }
}
