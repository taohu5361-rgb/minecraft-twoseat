package local.twoseat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import local.twoseat.mixin.MinecraftAccess;
import local.twoseat.mixin.GameRendererAccess;
import local.twoseat.mixin.MouseAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class SplitSession {
    private static SplitSession instance;
    public static int viewportWidth;
    private final Minecraft mc = Minecraft.getInstance();
    private final ConcurrentLinkedQueue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private volatile Connection connection;
    private ClientContext guest;
    private MouseHandler guestMouse;
    private MouseHandler physicalMouse;
    private boolean insideGuest, rendering, toggleHeld, inventoryHeld, attackHeld;
    private boolean toggleRequested, inventoryRequested, attackRequested, useRequested;
    private TextureTarget leftTarget, rightTarget;
    private int ticks;

    public SplitSession() { instance = this; }

    public static boolean enqueue(Connection connection, Packet<?> packet) {
        SplitSession s = instance;
        if (s == null || s.connection != connection) return false;
        s.packets.add(packet);
        return true;
    }

    public static boolean isGuest() { return instance != null && instance.insideGuest; }
    public static void keyEvent(int key, int action) {
        SplitSession s = instance;
        if (s == null || action != GLFW.GLFW_PRESS) return;
        if (key == GLFW.GLFW_KEY_F8) s.toggleRequested = true;
        if (key == GLFW.GLFW_KEY_F9) ControllerSeat.toggleKeyboard();
        if (s.guest == null) return;
        if (ControllerSeat.active()) return;
        if (key == GLFW.GLFW_KEY_O) s.inventoryRequested = true;
        if (key == GLFW.GLFW_KEY_U) s.attackRequested = true;
        if (key == GLFW.GLFW_KEY_P) s.useRequested = true;
    }
    public static boolean reserveKey(int key) {
        SplitSession s = instance;
        if (s == null || s.guest == null || s.mc.screen != null || ControllerSeat.active()) return false;
        return key == GLFW.GLFW_KEY_I || key == GLFW.GLFW_KEY_J || key == GLFW.GLFW_KEY_K || key == GLFW.GLFW_KEY_L
            || key == GLFW.GLFW_KEY_U || key == GLFW.GLFW_KEY_P || key == GLFW.GLFW_KEY_O
            || key == GLFW.GLFW_KEY_RIGHT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_SHIFT
            || (key >= GLFW.GLFW_KEY_KP_1 && key <= GLFW.GLFW_KEY_KP_9);
    }

    private boolean guestMenu() { return guest != null && guest.screen != null && guest.player != null && !insideGuest && !ControllerSeat.active(); }
    public static boolean menuKey(int key, int scan, int action, int mods) {
        SplitSession s = instance;
        if (s == null || !s.guestMenu() || key == GLFW.GLFW_KEY_F8 || key == GLFW.GLFW_KEY_O) return false;
        s.withGuest(() -> {
            if (action == GLFW.GLFW_RELEASE) s.mc.screen.keyReleased(key, scan, mods);
            else s.mc.screen.keyPressed(key, scan, mods);
        });
        return true;
    }
    public static boolean menuChar(int codepoint, int mods) {
        SplitSession s = instance;
        if (s == null || !s.guestMenu()) return false;
        s.withGuest(() -> { for (char c : Character.toChars(codepoint)) s.mc.screen.charTyped(c, mods); });
        return true;
    }
    private double menuX() { return (((MouseAccess)physicalMouse).twoseat$rawX() - mc.getWindow().getScreenWidth() / 2.0) * mc.screen.width / (mc.getWindow().getScreenWidth() / 2.0); }
    private double menuY() { return ((MouseAccess)physicalMouse).twoseat$rawY() * mc.screen.height / mc.getWindow().getScreenHeight(); }
    public static Double renderMouseX(double raw) {
        SplitSession s = instance;
        if (s == null || !s.rendering || s.mc.screen == null) return null;
        if (s.insideGuest && ControllerSeat.active()) return null;
        if (s.insideGuest) raw = ((MouseAccess)s.physicalMouse).twoseat$rawX();
        return 2 * raw - (s.insideGuest ? s.mc.getWindow().getScreenWidth() : 0);
    }
    public static Double renderMouseY() {
        SplitSession s = instance;
        if (s == null || !s.rendering || !s.insideGuest || s.mc.screen == null || ControllerSeat.active()) return null;
        return ((MouseAccess)s.physicalMouse).twoseat$rawY();
    }
    public static boolean menuMouse(int button, int action) {
        SplitSession s = instance;
        if (s == null) return false;
        if (!s.guestMenu()) {
            if (s.guest == null || s.mc.screen == null || s.insideGuest) return false;
            double x = 2 * s.mc.mouseHandler.xpos() * s.mc.screen.width / s.mc.getWindow().getScreenWidth();
            double y = s.menuY();
            if (action == GLFW.GLFW_PRESS) s.mc.screen.mouseClicked(x, y, button);
            else s.mc.screen.mouseReleased(x, y, button);
            return true;
        }
        s.withGuest(() -> {
            if (action == GLFW.GLFW_PRESS) s.mc.screen.mouseClicked(s.menuX(), s.menuY(), button);
            else s.mc.screen.mouseReleased(s.menuX(), s.menuY(), button);
        });
        return true;
    }
    public static boolean menuScroll(double amount) {
        SplitSession s = instance;
        if (s == null) return false;
        if (!s.guestMenu()) {
            if (s.guest == null || s.mc.screen == null || s.insideGuest) return false;
            s.mc.screen.mouseScrolled(2 * s.mc.mouseHandler.xpos() * s.mc.screen.width / s.mc.getWindow().getScreenWidth(), s.menuY(), amount);
            return true;
        }
        s.withGuest(() -> s.mc.screen.mouseScrolled(s.menuX(), s.menuY(), amount));
        return true;
    }

    @SubscribeEvent public void hud(RenderGuiEvent.Post event) {
        if (guest == null) return;
        String label = insideGuest ? (ControllerSeat.active() ? "P2  Controller | F9: keyboard" : "P2  IJKL / arrows / U / P / O | F9: mode") : "P1  WASD + mouse | F8: leave";
        event.getGuiGraphics().drawString(mc.font, label, 4, 4, 0xFFFFFF);
    }
    @SubscribeEvent public void renderEnd(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !rendering) DevProbe.afterRender();
    }

    /** Always restore the host even when a packet handler throws. Never called on Netty threads. */
    public void withGuest(Runnable task) {
        if (guest == null) return;
        if (insideGuest) { task.run(); return; }
        ClientContext host = new ClientContext();
        host.capture(mc);
        int oldWidth = viewportWidth;
        if (viewportWidth == 0 && guest.player != null) viewportWidth = mc.getWindow().getWidth() / 2;
        insideGuest = true;
        guest.mouse = guestMouse;
        guest.apply(mc);
        try { task.run(); }
        finally { guest.capture(mc); host.apply(mc); insideGuest = false; viewportWidth = oldWidth; }
    }

    void start() {
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.screen != null) return;
        guest = new ClientContext();
        physicalMouse = mc.mouseHandler;
        guestMouse = new MouseHandler(mc); // No setup(): GLFW callbacks remain owned by P1.
        guest.levelRenderer = new LevelRenderer(mc, mc.getEntityRenderDispatcher(), mc.getBlockEntityRenderDispatcher(), mc.renderBuffers());
        guest.levelRenderer.onResourceManagerReload(mc.getResourceManager());
        guest.gameRenderer = new GameRenderer(mc,
            new ItemInHandRenderer(mc, mc.getEntityRenderDispatcher(), mc.getItemRenderer()), mc.getResourceManager(), mc.renderBuffers());
        ((GameRendererAccess)guest.gameRenderer).twoseat$shaders().putAll(((GameRendererAccess)mc.gameRenderer).twoseat$shaders());
        ((GameRendererAccess)guest.gameRenderer).twoseat$blitShader(((GameRendererAccess)mc.gameRenderer).twoseat$blitShader());
        guest.gui = new ForgeGui(mc);
        guest.toasts = new net.minecraft.client.gui.components.toasts.ToastComponent(mc);
        connection = Connection.connectToLocalServer(mc.getConnection().getConnection().getRemoteAddress());
        // Forge's Connection activation handler registers its login channel asynchronously.
        // Registering it again here races channelActive and can see a null channel.
        connection.setListener(new ClientHandshakePacketListenerImpl(connection, mc, null, null, false, null,
            status -> TwoSeat.LOG.info("[TwoSeat] guest login: {}", status.getString())));
        // This is an in-memory local transport. It opens no TCP/LAN listener.
        connection.send(new ClientIntentionPacket("local", 0, ConnectionProtocol.LOGIN));
        connection.send(new ServerboundHelloPacket("TwoSeatGuest", java.util.Optional.of(UUID.nameUUIDFromBytes("OfflinePlayer:TwoSeatGuest".getBytes(java.nio.charset.StandardCharsets.UTF_8)))));
        TwoSeat.LOG.info("[TwoSeat] Started guest local connection in JVM {}", ProcessHandle.current().pid());
    }

    void stop() {
        Connection old = connection;
        connection = null;
        if (old != null) old.disconnect(Component.literal("TwoSeat guest left"));
        packets.clear();
        if (guest != null && guest.levelRenderer != null) {
            guest.levelRenderer.setLevel(null);
            guest.levelRenderer.close();
        }
        // Shader programs and texture manager belong to the shared client.
        if (guest != null && guest.gameRenderer != null) guest.gameRenderer.lightTexture().close();
        guest = null;
        guestMouse = null;
        ControllerSeat.reset();
        inventoryRequested = attackRequested = useRequested = false;
        if (leftTarget != null) leftTarget.destroyBuffers();
        if (rightTarget != null) rightTarget.destroyBuffers();
        leftTarget = rightTarget = null;
        viewportWidth = 0;
        if (mc.level != null) {
            mc.levelRenderer.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            mc.gameRenderer.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        }
        if (mc.screen == null && mc.level != null) mc.mouseHandler.grabMouse();
        TwoSeat.LOG.info("[TwoSeat] Guest session closed");
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || insideGuest) return;
        ticks++;
        DevProbe.tick(this);
        boolean toggle = SecondInput.down(GLFW.GLFW_KEY_F8);
        if (toggleRequested || (toggle && !toggleHeld)) { if (guest == null) start(); else stop(); }
        toggleRequested = false;
        toggleHeld = toggle;
        if (guest == null) return;
        if (mc.level == null || mc.getSingleplayerServer() == null) { stop(); return; }
        if (connection.channel() == null) return;
        if (!connection.isConnected()) { stop(); return; }
        withGuest(() -> {
            Packet<?> packet;
            int budget = 10000;
            while (budget-- > 0 && (packet = packets.poll()) != null) handle(packet, connection.getPacketListener());
            connection.tick();
            if (mc.level == null || mc.player == null) return;
            if (!(mc.player.input instanceof SecondInput)) {
                mc.player.input = new SecondInput();
                mc.setCameraEntity(mc.player);
                TwoSeat.LOG.info("[TwoSeat] Guest ready: {} UUID={} entity={}", mc.player.getGameProfile().getName(), mc.player.getUUID(), mc.player.getId());
            }
            if (mc.isPaused()) return;
            ControllerSeat.tick();
            if (mc.screen != null) {
                // ReceivingLevelScreen must get ticks to finish terrain loading.
                mc.screen.tick();
            }
            boolean inventory = !ControllerSeat.active() && SecondInput.down(GLFW.GLFW_KEY_O);
            if (inventoryRequested || (inventory && !inventoryHeld)) {
                if (mc.screen == null) mc.setScreen(new InventoryScreen(mc.player));
                else mc.player.closeContainer();
            }
            inventoryHeld = inventory;
            inventoryRequested = false;
            if (mc.screen == null && !ControllerSeat.active()) {
                float yaw = (SecondInput.down(GLFW.GLFW_KEY_RIGHT) ? 1 : 0) - (SecondInput.down(GLFW.GLFW_KEY_LEFT) ? 1 : 0);
                float pitch = (SecondInput.down(GLFW.GLFW_KEY_DOWN) ? 1 : 0) - (SecondInput.down(GLFW.GLFW_KEY_UP) ? 1 : 0);
                mc.player.turn(yaw * 24, pitch * 24);
                for (int i = 0; i < 9; i++) if (SecondInput.down(GLFW.GLFW_KEY_KP_1 + i)) mc.player.getInventory().selected = i;
            }
            MinecraftAccess a = (MinecraftAccess)mc;
            if (a.twoseat$useDelay() > 0) a.twoseat$useDelay(a.twoseat$useDelay() - 1);
            if (a.twoseat$missTime() > 0) a.twoseat$missTime(a.twoseat$missTime() - 1);
            mc.gameMode.tick();
            // Vanilla advances this before world/game-renderer ticks. Weather UVs and
            // block-breaking overlays use this clock, not GameRenderer's own clock.
            mc.levelRenderer.tick();
            mc.level.tickEntities();
            mc.level.tick(() -> true);
            mc.gameRenderer.tick();
            mc.gui.tick(false);
            mc.level.animateTick(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
            mc.particleEngine.tick();
            mc.gameRenderer.pick(1);
            boolean attack = mc.screen == null && (ControllerSeat.active() ? ControllerSeat.attack() : SecondInput.down(GLFW.GLFW_KEY_U));
            if (mc.screen == null && (attackRequested || (attack && !attackHeld))) a.twoseat$attack();
            attackRequested = false;
            a.twoseat$continueAttack(attack);
            attackHeld = attack;
            boolean use = mc.screen == null && (ControllerSeat.active() ? ControllerSeat.use() : useRequested || SecondInput.down(GLFW.GLFW_KEY_P));
            if (!use && mc.player.isUsingItem()) mc.gameMode.releaseUsingItem(mc.player);
            if (use && a.twoseat$useDelay() == 0 && !mc.player.isUsingItem()) a.twoseat$use();
            useRequested = false;
            if (ticks % 200 == 0) TwoSeat.LOG.info("[TwoSeat] guest tick pos={} health={} items={}", mc.player.position(), mc.player.getHealth(), mc.player.getInventory().getSelected());
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handle(Packet packet, PacketListener listener) { packet.handle(listener); }

    public static boolean render(float partialTick, long nanos, boolean world) {
        SplitSession s = instance;
        if (s == null || s.rendering || s.guest == null || s.guest.player == null || s.mc.level == null) return false;
        s.renderViews(partialTick, nanos, world);
        return true;
    }

    private void renderViews(float partialTick, long nanos, boolean world) {
        RenderTarget main = mc.getMainRenderTarget();
        int width = mc.getWindow().getWidth(), height = mc.getWindow().getHeight(), half = width / 2;
        if (leftTarget == null || leftTarget.width != half || leftTarget.height != height) {
            if (leftTarget != null) { leftTarget.destroyBuffers(); rightTarget.destroyBuffers(); }
            leftTarget = new TextureTarget(half, height, true, Minecraft.ON_OSX);
            rightTarget = new TextureTarget(width - half, height, true, Minecraft.ON_OSX);
            mc.levelRenderer.resize(half, height);
            mc.gameRenderer.resize(half, height);
            withGuest(() -> {
                mc.levelRenderer.resize(width - half, height);
                mc.gameRenderer.resize(width - half, height);
            });
        }
        MinecraftAccess a = (MinecraftAccess)mc;
        rendering = true;
        try {
            viewportWidth = half;
            leftTarget.clear(Minecraft.ON_OSX);
            a.twoseat$target(leftTarget); leftTarget.bindWrite(true);
            resizeScreen();
            mc.gameRenderer.render(partialTick, nanos, world);
            withGuest(() -> {
                viewportWidth = rightTarget.width;
                ControllerSeat.renderInput();
                rightTarget.clear(Minecraft.ON_OSX);
                a.twoseat$target(rightTarget); rightTarget.bindWrite(true);
                resizeScreen();
                mc.gameRenderer.render(partialTick, nanos, world);
            });
        } finally {
            viewportWidth = 0; a.twoseat$target(main); rendering = false;
        }
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, leftTarget.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, half, height, 0, 0, half, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, rightTarget.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, width - half, height, half, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(true);
        RenderSystem.viewport(0, 0, width, height);
        DevProbe.afterRender();
    }

    private void resizeScreen() {
        if (mc.screen != null && (mc.screen.width != mc.getWindow().getGuiScaledWidth() || mc.screen.height != mc.getWindow().getGuiScaledHeight()))
            mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }
}
