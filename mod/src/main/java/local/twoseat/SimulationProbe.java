package local.twoseat;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server-side vanilla ticking evidence, restricted to the opt-in test driver. */
final class SimulationProbe {
    static void inspect(String id) {
        var mc = Minecraft.getInstance(); var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            JsonObject report = new JsonObject(); JsonArray players = new JsonArray();
            report.addProperty("viewDistance", server.getPlayerList().getViewDistance());
            report.addProperty("simulationDistance", server.getPlayerList().getSimulationDistance());
            report.addProperty("meanServerTickMs", server.getAverageTickTime());
            for (var p : server.getPlayerList().getPlayers()) {
                JsonObject player = new JsonObject(); player.addProperty("name", p.getGameProfile().getName());
                JsonArray chunks = new JsonArray();
                for (int offset : new int[]{0, 6, 7, 8, 9}) {
                    BlockPos pos = new BlockPos(p.chunkPosition().getMinBlockX() + 8 + offset*16, 150, p.chunkPosition().getMinBlockZ()+8);
                    JsonObject chunk = new JsonObject(); chunk.addProperty("offsetChunks", offset);
                    chunk.addProperty("entityTicking", p.serverLevel().isPositionEntityTicking(pos));
                    chunk.addProperty("blockTickingReady", p.serverLevel().getChunkSource().isPositionTicking(new ChunkPos(pos).toLong()));
                    var loaded = p.serverLevel().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
                    chunk.addProperty("loaded", loaded != null);
                    if (loaded != null) {
                        BlockEntity block = loaded.getBlockEntity(pos);
                        if (block != null) chunk.add("blockEntity", NbtOps.INSTANCE.convertTo(com.mojang.serialization.JsonOps.INSTANCE, block.saveWithoutMetadata()));
                    }
                    chunks.add(chunk);
                }
                player.add("chunks", chunks); players.add(player);
            }
            JsonArray mobs = new JsonArray();
            for (var entity : server.overworld().getAllEntities()) {
                if (entity.getTags().stream().noneMatch(tag -> tag.startsWith("TwoSeatSim"))) continue;
                JsonObject mob = new JsonObject(); mob.addProperty("uuid", entity.getUUID().toString());
                mob.addProperty("tags", entity.getTags().toString()); mob.addProperty("ticks", entity.tickCount);
                mob.addProperty("entityTicking", server.overworld().isPositionEntityTicking(entity.blockPosition()));
                mobs.add(mob);
            }
            report.add("players", players); report.add("entities", mobs);
            DevProbe.write(mc, id, report);
        });
    }
}
