package local.twoseat;

import com.google.gson.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Opt-in local regression driver. Disabled unless -Dtwoseat.probe=true is supplied. */
final class DevProbe {
    static final boolean ENABLED = Boolean.getBoolean("twoseat.probe");
    static final Set<Integer> KEYS = new HashSet<>();
    private static String lastId = "";
    private static String screenshot;

    static void tick(SplitSession session) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        Path file = mc.gameDirectory.toPath().resolve("twoseat-command.json");
        if (!Files.isRegularFile(file)) return;
        try {
            String content = Files.readString(file);
            if (content.isBlank()) return;
            JsonObject cmd = JsonParser.parseString(content).getAsJsonObject();
            String id = cmd.get("id").getAsString();
            if (id.equals(lastId)) return;
            lastId = id;
            String op = cmd.get("op").getAsString();
            switch (op) {
                case "world" -> {
                    String name = "TwoSeat-Probe";
                    if (Files.isRegularFile(mc.gameDirectory.toPath().resolve("saves").resolve(name).resolve("level.dat")))
                        mc.createWorldOpenFlows().loadLevel(new net.minecraft.client.gui.screens.TitleScreen(), name);
                    else mc.createWorldOpenFlows().createFreshLevel(name,
                        new net.minecraft.world.level.LevelSettings(name, net.minecraft.world.level.GameType.SURVIVAL, false,
                            net.minecraft.world.Difficulty.PEACEFUL, false, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                        new net.minecraft.world.level.levelgen.WorldOptions(12345L, false, false),
                        net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);
                }
                case "saveQuit" -> { session.stop(); if (mc.level != null) mc.level.disconnect(); mc.clearLevel(); mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen()); }
                case "join" -> session.start();
                case "leave" -> session.stop();
                case "controllerConnect" -> session.withGuest(ControllerProbe::connect);
                case "controllerFirstConnect" -> session.withGuest(() -> ControllerProbe.connect(true));
                case "controllerState" -> ControllerProbe.state(cmd.getAsJsonObject("state"));
                case "controllerDisconnect" -> session.withGuest(ControllerProbe::disconnect);
                case "controllerRestore" -> session.withGuest(ControllerProbe::restore);
                case "hostInventory" -> {
                    if (mc.screen == null) mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                    else mc.player.closeContainer();
                }
                case "keys" -> { KEYS.clear(); for (JsonElement e : cmd.getAsJsonArray("keys")) KEYS.add(e.getAsInt()); }
                case "hostForward" -> mc.options.keyUp.setDown(cmd.get("down").getAsBoolean());
                case "server" -> {
                    if (mc.getSingleplayerServer() != null) {
                        String command = cmd.get("command").getAsString();
                        var server = mc.getSingleplayerServer();
                        if (!server.getWorldData().getLevelName().startsWith("TwoSeat")) throw new IllegalStateException("Probe commands require a TwoSeat test world");
                        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
                    }
                }
                case "inspect" -> inspect(session, id);
                case "benchmark" -> WeatherProbe.start(id, cmd.get("seconds").getAsInt(), session);
                case "simulation" -> SimulationProbe.inspect(id);
                case "testSurface" -> {
                    var server = mc.getSingleplayerServer();
                    if (server != null && server.getWorldData().getLevelName().startsWith("TwoSeat")) server.execute(() -> {
                        for (var p : server.getPlayerList().getPlayers()) {
                            int y = p.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ());
                            p.teleportTo(p.serverLevel(), p.getX(), y+1, p.getZ(), p.getYRot(), 0);
                        }
                    });
                }
                case "testOptions" -> {
                    mc.options.renderDistance().set(8); mc.options.simulationDistance().set(8);
                    mc.options.enableVsync().set(false); mc.options.framerateLimit().set(260);
                    mc.options.pauseOnLostFocus = false;
                }
                case "clearToasts" -> { mc.getToasts().clear(); session.withGuest(() -> mc.getToasts().clear()); }
                case "screenshot" -> screenshot = id;
                case "quit" -> mc.stop();
                default -> throw new IllegalArgumentException(op);
            }
            TwoSeat.LOG.info("[TwoSeat probe] {} {}", id, op);
        } catch (Exception e) { TwoSeat.LOG.error("[TwoSeat probe] command failed", e); }
    }

    private static void inspect(SplitSession session, String id) {
        Minecraft mc = Minecraft.getInstance();
        JsonObject report = new JsonObject();
        report.addProperty("pid", ProcessHandle.current().pid());
        report.addProperty("windowActive", mc.isWindowActive());
        report.addProperty("controllerProbe", ControllerProbe.connected());
        report.addProperty("controllerActive", ControllerSeat.active());
        JsonArray controllers = new JsonArray();
        dev.isxander.controlify.Controlify.instance().getControllerManager().ifPresent(manager -> {
            for (var controller : manager.getConnectedControllers()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("uid", controller.uid());
                entry.addProperty("name", controller.name());
                entry.addProperty("driver", controller.driverName());
                entry.addProperty("selected", dev.isxander.controlify.Controlify.instance().getCurrentController().orElse(null) == controller);
                controller.input().ifPresent(input -> {
                    entry.addProperty("standardGamepad", input.isDefinitelyGamepad());
                    entry.addProperty("hasMapping", input.confObj().mapping != null);
                    entry.addProperty("axes", input.axisCount());
                    entry.addProperty("buttons", input.buttonCount());
                });
                controllers.add(entry);
            }
        });
        report.add("controllers", controllers);
        report.addProperty("hostInput", mc.player == null ? "none" : mc.player.input.getClass().getSimpleName());
        report.addProperty("hostAttackKey", mc.options.keyAttack.isDown());
        report.addProperty("hostUseKey", mc.options.keyUse.isDown());
        report.addProperty("hostScreen", mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
        report.addProperty("hostMouse", System.identityHashCode(mc.mouseHandler));
        report.addProperty("hostMouseX", mc.mouseHandler.xpos());
        report.addProperty("hostMouseY", mc.mouseHandler.ypos());
        report.addProperty("hostUUID", mc.player == null ? "none" : mc.player.getUUID().toString());
        report.addProperty("hostPosition", mc.player == null ? "none" : mc.player.position().toString());
        report.addProperty("hostYaw", mc.player == null ? 0 : mc.player.getYRot());
        report.addProperty("hostPitch", mc.player == null ? 0 : mc.player.getXRot());
        clientProgress(mc, report, "host");
        session.withGuest(() -> {
            report.addProperty("guestUUID", mc.player == null ? "none" : mc.player.getUUID().toString());
            report.addProperty("guestPosition", mc.player == null ? "none" : mc.player.position().toString());
            report.addProperty("guestYaw", mc.player == null ? 0 : mc.player.getYRot());
            report.addProperty("guestPitch", mc.player == null ? 0 : mc.player.getXRot());
            clientProgress(mc, report, "guest");
            report.addProperty("guestScreen", mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
            report.addProperty("guestHit", String.valueOf(mc.hitResult));
            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult block) {
                report.addProperty("guestHitBlock", block.getBlockPos().toString());
                report.addProperty("guestHitFace", block.getDirection().toString());
                report.addProperty("guestHitState", mc.level.getBlockState(block.getBlockPos()).toString());
            }
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult entity)
                report.addProperty("guestHitEntity", entity.getEntity().getName().getString());
            if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container) {
                var access = (dev.isxander.controlify.mixins.feature.guide.screen.AbstractContainerScreenAccessor)container;
                JsonArray slots = new JsonArray();
                for (var slot : container.getMenu().slots) {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("id", slot.index);
                    entry.addProperty("x", access.getLeftPos() + slot.x + 8);
                    entry.addProperty("y", access.getTopPos() + slot.y + 8);
                    entry.addProperty("item", slot.getItem().toString());
                    slots.add(entry);
                }
                report.add("guestSlots", slots);
            }
            report.addProperty("guestInput", mc.player == null ? "none" : mc.player.input.getClass().getSimpleName());
            report.addProperty("guestAttackKey", mc.options.keyAttack.isDown());
            report.addProperty("guestUseKey", mc.options.keyUse.isDown());
            report.addProperty("guestMouse", System.identityHashCode(mc.mouseHandler));
            report.addProperty("guestMouseX", mc.mouseHandler.xpos());
            report.addProperty("guestMouseY", mc.mouseHandler.ypos());
            report.addProperty("guestCarried", mc.player == null ? "none" : mc.player.containerMenu.getCarried().toString());
        });
        var server = mc.getSingleplayerServer();
        if (server == null) { write(mc, id, report); return; }
        server.execute(() -> {
            JsonArray players = new JsonArray();
            for (var p : server.getPlayerList().getPlayers()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("name", p.getGameProfile().getName());
                entry.addProperty("uuid", p.getUUID().toString());
                entry.addProperty("entityId", p.getId());
                entry.addProperty("position", p.position().toString());
                entry.addProperty("health", p.getHealth());
                entry.addProperty("gameMode", p.gameMode.getGameModeForPlayer().getName());
                entry.addProperty("selectedItem", p.getMainHandItem().toString());
                entry.addProperty("entityClass", p.getClass().getName());
                entry.add("playerData", net.minecraft.nbt.NbtOps.INSTANCE.convertTo(com.mojang.serialization.JsonOps.INSTANCE,
                    p.saveWithoutId(new net.minecraft.nbt.CompoundTag())));
                entry.addProperty("jumpStat", p.getStats().getValue(net.minecraft.stats.Stats.CUSTOM, net.minecraft.stats.Stats.JUMP));
                JsonObject advancements = new JsonObject();
                for (String name : new String[]{"minecraft:story/mine_stone", "minecraft:story/upgrade_tools"}) {
                    var advancement = server.getAdvancements().getAdvancement(new net.minecraft.resources.ResourceLocation(name));
                    advancements.addProperty(name, p.getAdvancements().getOrStartProgress(advancement).isDone());
                }
                entry.add("advancements", advancements);
                JsonArray inventory = new JsonArray();
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    if (!p.getInventory().getItem(i).isEmpty()) inventory.add(i + ":" + p.getInventory().getItem(i));
                }
                entry.add("inventory", inventory);
                players.add(entry);
            }
            report.add("serverPlayers", players);
            report.addProperty("serverThread", Thread.currentThread().getName());
            write(mc, id, report);
        });
    }
    private static void clientProgress(Minecraft mc, JsonObject report, String seat) {
        report.addProperty(seat + "RendererTicks", ((local.twoseat.mixin.LevelRendererAccess)mc.levelRenderer).twoseat$ticks());
        report.addProperty(seat + "ParticleCount", mc.particleEngine.countParticles());
        report.addProperty(seat + "ParticleState", System.identityHashCode(((local.twoseat.mixin.ParticleAccess)mc.particleEngine).twoseat$particles()));
        JsonObject particleTypes = new JsonObject();
        int mismatches = 0;
        var access = (local.twoseat.mixin.ParticleAccess)mc.particleEngine;
        java.util.List<net.minecraft.client.particle.Particle> particles = new java.util.ArrayList<>(access.twoseat$pending());
        access.twoseat$particles().values().forEach(particles::addAll);
        for (var particle : particles) {
            var data = (local.twoseat.mixin.ParticleDataAccess)particle;
            if (data.twoseat$level() != mc.level) mismatches++;
            String type = particle.getClass().getSimpleName();
            JsonObject group = particleTypes.has(type) ? particleTypes.getAsJsonObject(type) : new JsonObject();
            group.addProperty("count", group.has("count") ? group.get("count").getAsInt()+1 : 1);
            group.addProperty("maxAge", Math.max(group.has("maxAge") ? group.get("maxAge").getAsInt() : 0, data.twoseat$age()));
            particleTypes.add(type, group);
        }
        report.add(seat + "ParticleTypes", particleTypes);
        report.addProperty(seat + "ParticleWorldMismatches", mismatches);
        if (mc.level != null) report.addProperty(seat + "ServerSimulationDistance", mc.level.getServerSimulationDistance());
        report.addProperty(seat + "Toasts", System.identityHashCode(mc.getToasts()));
        report.addProperty(seat + "AdvancementToast", mc.getToasts().getToast(
            net.minecraft.client.gui.components.toasts.AdvancementToast.class,
            net.minecraft.client.gui.components.toasts.Toast.NO_TOKEN) != null);
        if (mc.player == null) return;
        report.addProperty(seat + "Stats", System.identityHashCode(mc.player.getStats()));
        report.addProperty(seat + "Recipes", System.identityHashCode(mc.player.getRecipeBook()));
        report.addProperty(seat + "Advancements", System.identityHashCode(mc.getConnection().getAdvancements()));
        report.addProperty(seat + "XP", mc.player.experienceLevel);
        JsonArray recipes = new JsonArray();
        for (var recipe : mc.level.getRecipeManager().getRecipes())
            if (mc.player.getRecipeBook().contains(recipe)) recipes.add(recipe.getId().toString());
        report.add(seat + "KnownRecipes", recipes);
    }
    static void write(Minecraft mc, String id, JsonObject value) {
        try {
            Path folder = mc.gameDirectory.toPath().resolve("twoseat-evidence");
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(id + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(value));
        } catch (Exception e) { TwoSeat.LOG.error("[TwoSeat probe] evidence write failed", e); }
    }
    static void afterRender() {
        if (!ENABLED || screenshot == null) return;
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "twoseat-" + screenshot + ".png", mc.getMainRenderTarget(), text -> TwoSeat.LOG.info("[TwoSeat probe] {}", text.getString()));
        screenshot = null;
    }
}
