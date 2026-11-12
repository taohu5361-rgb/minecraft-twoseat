// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat;

import com.google.gson.JsonObject;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.id.ControllerType;
import dev.isxander.controlify.controller.impl.ControllerStateImpl;
import dev.isxander.controlify.controller.info.*;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.isxander.controlify.controller.input.InputComponent;
import dev.isxander.controlify.controllermanager.UniqueControllerID;
import dev.isxander.controlify.driver.Driver;
import dev.isxander.controlify.utils.CUtil;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;
import local.twoseat.mixin.ControlifyAccess;

/** Opt-in in-process test device. Exercises the real binding/deadzone/UI pipeline, not USB/SDL. */
public final class ControllerProbe {
    private static ControllerEntity device;
    private static boolean disconnected;
    private static JsonObject state = new JsonObject();
    private record Id(String value) implements UniqueControllerID {}

    static void connect() {
        if (!DevProbe.ENABLED) throw new IllegalStateException("Probe disabled");
        if (Controlify.instance().getControllerManager().isEmpty()) throw new IllegalStateException("Controlify not initialized");
        disconnected = false;
        device = new ControllerEntity(new ControllerInfo(new Id("twoseat-test-only"), ControllerType.DEFAULT, Optional.empty()), new Driver() {
            @Override public void addComponents(ControllerEntity controller) {
                controller.setComponent(new UIDComponent("twoseat-test-only"));
                controller.setComponent(new GUIDComponent("twoseat-test-only"));
                controller.setComponent(new DriverNameComponent("TwoSeat opt-in test device"));
                controller.setComponent(new InputComponent(controller, 15, 10, 0, true, GamepadInputs.DEADZONE_GROUPS, "default"));
            }
        }, CUtil.LOGGER.createSubLogger("TwoSeatProbe"));
        state = new JsonObject();
        ControlifyAccess access = (ControlifyAccess)Controlify.instance();
        access.twoseat$wizards().clear();
        access.twoseat$wizard(null);
        net.minecraft.client.Minecraft.getInstance().setScreen(null);
        Controlify.instance().setCurrentController(device, false);
    }
    static void state(JsonObject value) {
        if (!DevProbe.ENABLED || device == null) throw new IllegalStateException("No probe device");
        state = value.deepCopy();
    }
    static void disconnect() {
        if (!DevProbe.ENABLED) return;
        if (Controlify.instance().getCurrentController().orElse(null) == device) Controlify.instance().setCurrentController(null, false);
        device = null; state = new JsonObject();
        disconnected = true;
    }
    static void restore() { if (DevProbe.ENABLED) { disconnect(); disconnected = false; } }
    public static boolean disconnected() { return DevProbe.ENABLED && disconnected; }
    public static void poll(ControllerEntity controller, boolean outOfFocus) {
        if (!DevProbe.ENABLED || device == null || controller != device) return;
        ControllerStateImpl next = new ControllerStateImpl();
        if (!outOfFocus) {
            if (state.has("axes")) state.getAsJsonObject("axes").entrySet().forEach(e ->
                next.setAxis(new ResourceLocation("controlify", "axis/" + e.getKey()), e.getValue().getAsFloat()));
            if (state.has("buttons")) state.getAsJsonArray("buttons").forEach(e ->
                next.setButton(new ResourceLocation("controlify", "button/" + e.getAsString()), true));
        }
        controller.input().orElseThrow().pushState(next);
    }
    public static boolean connected() { return DevProbe.ENABLED && device != null; }
}
