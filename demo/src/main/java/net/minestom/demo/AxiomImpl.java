package net.minestom.demo;

import net.kyori.adventure.text.NBTComponent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


/**
 * https://gist.github.com/mworzala/60b3a0c5005251ae5f6d9965974ca015
 */
public class AxiomImpl {

    Logger log = Logger.getLogger("Axiom");
    private static final int MAX_BUFFERSIZE = 0x100000;

    public AxiomImpl() {
        MinecraftServer.getGlobalEventHandler()
                .addListener(PlayerPluginMessageEvent.class, this::onPluginMessage)
                .addListener(PlayerSpawnEvent.class, event -> {
                })
                .addListener(RemoveEntityFromInstanceEvent.class, event -> {
                    if (event.getEntity() instanceof Player player) {
                        byte[] data = NetworkBuffer.makeArray(buffer -> {
                            buffer.write(NetworkBuffer.UUID, UUID.randomUUID());// id
                            buffer.write(NetworkBuffer.INT, 0);
                            buffer.write(NetworkBuffer.BYTE, (byte) 0);
                        });
                        player.sendPluginMessage("axiom:set_world_property", data);
                    }
                });
    }


    private void onPluginMessage(@NotNull PlayerPluginMessageEvent event) {
        var player = event.getPlayer();
        var message = event.getMessageString();
        var buffer = new NetworkBuffer(ByteBuffer.wrap(event.getMessage()));
        switch (event.getIdentifier()) {
            case "minecraft:register" -> handleRegisterPluginMessageChannels(player, event.getMessageString());
            case "axiom:hello" -> handleHello(player, buffer);
            case "axiom:set_gamemode" -> handleSetGamemode(player, buffer);
            case "axiom:set_fly_speed" -> handleSetFlySpeed(player, buffer);
            case "axiom:set_block" -> handleSetBlock(player, buffer);
//            case "axiom:set_hotbar_slot" -> handleSetHotbarSlot(player, buffer);
//            case "axiom:switch_active_hotbar" -> handleSwitchActiveHotbar(player, buffer);
            case "axiom:teleport" -> handleTeleport(player, buffer);
            case "axiom:set_block_buffer" -> handleBigPayload(player, buffer);
            case "axiom:spawn_entity" -> handleSpawnEntity(player, buffer);
            default -> {
                log.info("Unhandled Plugin Message "+  event.getIdentifier()+" - "+ event.getMessageString());
            }
        }
    }

    private void handleBigPayload(Player player, NetworkBuffer buffer) {

    }

    private void handleTeleport(Player player, NetworkBuffer buffer) {
        //  buffer.read;


    }

    private void handleSetFlySpeed(Player player, NetworkBuffer buffer) {
        float speed = buffer.read(NetworkBuffer.FLOAT);
        player.setFlyingSpeed(speed);
    }

    private void handleSetBlock(Player player, NetworkBuffer buffer) {
        //TODO:
        log.info("Set Block "+ buffer.toString());
        var key = buffer.read(NetworkBuffer.BYTE);
        log.info("key: "+ key);
        var uuid = buffer.read(NetworkBuffer.UUID);
        log.info("uuid: "+ uuid);
        var continuation = buffer.read(NetworkBuffer.BOOLEAN);
        if (continuation) {
            buffer.read(NetworkBuffer.NOTHING);
        }
        byte type = buffer.read(NetworkBuffer.BYTE);
        log.info("type: "+ type);
        if (type == 0) {
            AtomicBoolean reachRateLimit = new AtomicBoolean(false);

        }


    }

    private void handleSetGamemode(Player player, NetworkBuffer buffer) {
        GameMode gamemode = GameMode.fromId((int) buffer.read(NetworkBuffer.BYTE));
        player.setGameMode(gamemode);
    }

    private void handleSpawnEntity(Player player, NetworkBuffer buffer) {
        UUID uuid = buffer.read(NetworkBuffer.UUID);
        double x = buffer.read(NetworkBuffer.DOUBLE);
        double y = buffer.read(NetworkBuffer.DOUBLE);
        double z = buffer.read(NetworkBuffer.DOUBLE);
        float yaw = buffer.read(NetworkBuffer.FLOAT);
        float pitch = buffer.read(NetworkBuffer.FLOAT);

        UUID copyFrom = buffer.readOptional(NetworkBuffer.OPT_UUID);
        var comp = buffer.readOptional(NetworkBuffer.COMPONENT);
        log.info("Spawn Entity: " + uuid);

    }

    private void handleHello(Player player, NetworkBuffer buffer) {
        log.info("Recived Hello from " + player.getUsername());
        var apiVersion = buffer.read(NetworkBuffer.VAR_INT);
        var dataVersion = buffer.read(NetworkBuffer.VAR_INT);
        player.sendMessage("API: " + apiVersion);
        player.sendMessage("Data: " + dataVersion);
    }

    private void handleRegisterPluginMessageChannels(Player player, String messageString) {
        for (String channel : messageString.split("\\u0000")) {
            log.info("Found Channel: "+ channel);
            if (channel.equalsIgnoreCase("axiom:enable")) {
                player.sendPluginMessage("minecraft:register", "axiom:hello\u0000" +
                        "axiom:set_gamemode\u0000" +
                        "axiom:set_fly_speed\u0000" +
                        "axiom:set_world_time\u0000" +
                        "axiom:set_world_property\u0000" +
                        "axiom:set_block\u0000" +
                        "axiom:set_hotbar_slot\u0000" +
                        "axiom:switch_active_hotbar\u0000" +
                        "axiom:teleport\u0000" +
                        "axiom:set_editor_views\u0000" +
                        "axiom:request_chunk_data\u0000" +
                        "axiom:spawn_entity\u0000" +
                        "axiom:manipulate_entity\u0000" +
                        "axiom:delete_entity\u0000" +
                        "axiom:marker_nbt_request\u0000" +
                        "axiom:request_blueprint");
                player.setGameMode(GameMode.CREATIVE);
                byte[] data = NetworkBuffer.makeArray(buffer -> {
                    buffer.write(NetworkBuffer.BOOLEAN, true);// Enable
                    buffer.write(NetworkBuffer.INT, MAX_BUFFERSIZE);// Max Buffer Size
                    buffer.write(NetworkBuffer.BOOLEAN, false);// No source info
                    buffer.write(NetworkBuffer.BOOLEAN, false);// No source settings
                    buffer.write(NetworkBuffer.VAR_INT, 5); // Maximum Reach
                    buffer.write(NetworkBuffer.VAR_INT, 16); // Max editor views
                    buffer.write(NetworkBuffer.BOOLEAN, true);// Editable Views
                });
                player.sendPluginMessage("axiom:enable", data);
            }
        }
    }

    private record SpawnEntry(UUID newUuid, double x, double y, double z, float yaw, float pitch, UUID copyFrom, NBTComponent tag) {
        public SpawnEntry(NetworkBuffer buffer) {
            this(buffer.read(NetworkBuffer.UUID), buffer.read(NetworkBuffer.DOUBLE), buffer.read(NetworkBuffer.DOUBLE),
                    buffer.read(NetworkBuffer.DOUBLE), buffer.read(NetworkBuffer.FLOAT), buffer.read(NetworkBuffer.FLOAT),
                    buffer.readOptional(NetworkBuffer.UUID), buffer.read(NetworkBuffer.lazy(() -> {
                        return null;
                    })));
        }
    }


}
