package local.twoseat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import org.lwjgl.glfw.GLFW;

/** Basic shared-keyboard layout, independent of vanilla's global KeyMappings. */
final class SecondInput extends Input {
    static boolean down(int key) {
        if (DevProbe.ENABLED && DevProbe.KEYS.contains(key)) return true;
        Minecraft mc = Minecraft.getInstance();
        return mc.isWindowActive() && GLFW.glfwGetKey(mc.getWindow().getWindow(), key) == GLFW.GLFW_PRESS;
    }
    @Override public void tick(boolean slow, float factor) {
        boolean enabled = Minecraft.getInstance().screen == null;
        up = enabled && down(GLFW.GLFW_KEY_I); down = enabled && down(GLFW.GLFW_KEY_K);
        left = enabled && down(GLFW.GLFW_KEY_J); right = enabled && down(GLFW.GLFW_KEY_L);
        forwardImpulse = (up ? 1 : 0) - (down ? 1 : 0);
        leftImpulse = (left ? 1 : 0) - (right ? 1 : 0);
        jumping = enabled && down(GLFW.GLFW_KEY_RIGHT_CONTROL);
        shiftKeyDown = enabled && down(GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (slow) { forwardImpulse *= factor; leftImpulse *= factor; }
    }
}
