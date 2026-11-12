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
        report.addProperty("hostUUID", mc.player == null ? "none" : mc.player.getUUID().toString());
        report.addProperty("hostPosition", mc.player == null ? "none" : mc.player.position().toString());
        report.addProperty("hostYaw", mc.player == null ? 0 : mc.player.getYRot());
        report.addProperty("hostPitch", mc.player == null ? 0 : mc.player.getXRot());
        session.withGuest(() -> {
            report.addProperty("guestUUID", mc.player == null ? "none" : mc.player.getUUID().toString());
            report.addProperty("guestPosition", mc.player == null ? "none" : mc.player.position().toString());
            report.addProperty("guestYaw", mc.player == null ? 0 : mc.player.getYRot());
            report.addProperty("guestPitch", mc.player == null ? 0 : mc.player.getXRot());
            report.addProperty("guestScreen", mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
            report.addProperty("guestHit", String.valueOf(mc.hitResult));
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
    private static void write(Minecraft mc, String id, JsonObject value) {
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
