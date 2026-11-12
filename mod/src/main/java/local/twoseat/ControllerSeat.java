// SPDX-License-Identifier: LGPL-3.0-or-later
package local.twoseat;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.ingame.ControllerPlayerMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Adapts the embedded Controlify engine to P2. No native driver or binding table is reimplemented. */
public final class ControllerSeat {
    private static boolean keyboardOnly;
    private static ControllerEntity lastController;
    private static ControllerPlayerMovement movement;

    public static ControllerEntity controller() {
        return keyboardOnly ? null : Controlify.instance().getCurrentController().filter(ControllerSeat::usable).orElse(null);
    }

    private static boolean usable(ControllerEntity controller) {
        // A device can expose both XInput and a raw/vendor interface with zero controls.
        // Leave unmapped raw devices to the manual mapper instead of stealing P2 input.
        return controller.input().map(input -> input.isDefinitelyGamepad() || input.confObj().mapping != null).orElse(false);
    }

    private static void selectUsableController() {
        if (keyboardOnly || ControllerProbe.disconnected()) return;
        if (Controlify.instance().getCurrentController().filter(ControllerSeat::usable).isPresent()) return;
        Controlify.instance().getControllerManager().ifPresent(manager -> manager.getConnectedControllers().stream()
            .filter(ControllerSeat::usable).findFirst().ifPresent(c -> {
                Controlify.instance().setCurrentController(c, false);
                TwoSeat.LOG.info("[TwoSeat] P2 selected usable controller: {} ({})", c.name(), c.uid());
            }));
    }

    public static boolean active() { return controller() != null; }
    public static boolean keyboardOnly() { return keyboardOnly; }
    public static void toggleKeyboard() {
        keyboardOnly = !keyboardOnly;
        reset();
        TwoSeat.LOG.info("[TwoSeat] P2 input: {}", keyboardOnly ? "keyboard only" : "controller when connected");
    }
    public static void reset() { lastController = null; movement = null; }

    static boolean movement(SecondInput target, boolean slow, float factor) {
        ControllerEntity controller = controller();
        if (controller == null) { reset(); return false; }
        Minecraft mc = Minecraft.getInstance();
        if (lastController != controller || movement == null) {
            lastController = controller;
            movement = new ControllerPlayerMovement(controller, mc.player);
        }
        movement.tick(slow, factor);
        target.up = movement.up; target.down = movement.down;
        target.left = movement.left; target.right = movement.right;
        target.forwardImpulse = movement.forwardImpulse; target.leftImpulse = movement.leftImpulse;
        target.jumping = movement.jumping; target.shiftKeyDown = movement.shiftKeyDown;
        return true;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ControllerEntity previous = controller();
        selectUsableController();
        // The normal Forge START callback is cancelled by ControlifySeatMixin.
        // Polling under P2 also scopes binding contexts and emulated KeyMappings to P2.
        if (Controlify.instance().getControllerManager().isPresent()) Controlify.instance().tick(mc);
        selectUsableController(); // Include a device hot-plugged during the native poll.
        ControllerEntity controller = controller();
        if (previous != null && controller == null) net.minecraft.client.KeyMapping.releaseAll();
        if (controller == null || !mc.isWindowActive() || mc.isPaused()) return;
        if (mc.screen == null) {
            if (ControlifyBindings.PAUSE.on(controller).justPressed()) {
                SplitSession.openMenu();
            } else if (ControlifyBindings.INVENTORY.on(controller).justPressed()) {
                mc.setScreen(new InventoryScreen(mc.player));
            } else {
                if (ControlifyBindings.NEXT_SLOT.on(controller).justPressed()) mc.player.getInventory().swapPaint(-1);
                if (ControlifyBindings.PREV_SLOT.on(controller).justPressed()) mc.player.getInventory().swapPaint(1);
            }
        }
    }

    static boolean attack() {
        ControllerEntity c = controller();
        return c != null && Minecraft.getInstance().isWindowActive() && ControlifyBindings.ATTACK.on(c).digitalNow();
    }
    static boolean use() {
        ControllerEntity c = controller();
        return c != null && Minecraft.getInstance().isWindowActive() && ControlifyBindings.USE.on(c).digitalNow();
    }
    static void renderInput() {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.isPaused() || !mc.isWindowActive()) return;
        if (mc.screen == null) Controlify.instance().inGameInputHandler().ifPresent(handler -> handler.processPlayerLook(mc.getDeltaFrameTime()));
        if (mc.screen != null) Controlify.instance().virtualMouseHandler().updateMouse();
    }
}
