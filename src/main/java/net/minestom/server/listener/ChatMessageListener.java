package net.minestom.server.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.message.ChatPosition;
import net.minestom.server.message.Messenger;
import net.minestom.server.monitoring.EventsJFR;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientSignedCommandChatPacket;

import java.util.Collection;

public class ChatMessageListener {
    private static final CommandManager COMMAND_MANAGER = MinecraftServer.getCommandManager();
    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    public static void commandChatListener(ClientCommandChatPacket packet, Player player) {
        final String command = packet.message();
        new EventsJFR.PlayerCommand(player.getUuid().toString(), command).commit();
        if (Messenger.canReceiveCommand(player)) {
            COMMAND_MANAGER.execute(player, command);
        } else {
            Messenger.sendRejectionMessage(player);
        }
    }

    public static void signedCommandChatListener(ClientSignedCommandChatPacket packet, Player player) {
        // Intentionally do the same thing as commandChatListener. We don't use signed commands, but Geyser
        // and Gate (the proxy) always send them so this is for compatibility with them.
        final String command = packet.message();
        new EventsJFR.PlayerCommand(player.getUuid().toString(), command).commit();
        if (Messenger.canReceiveCommand(player)) {
            COMMAND_MANAGER.execute(player, command);
        } else {
            Messenger.sendRejectionMessage(player);
        }
    }

    public static void chatMessageListener(ClientChatMessagePacket packet, Player player) {
        final String message = packet.message();
        new EventsJFR.PlayerChat(player.getUuid().toString(), message).commit();
        if (!Messenger.canReceiveMessage(player)) {
            Messenger.sendRejectionMessage(player);
            return;
        }

        final Collection<Player> players = CONNECTION_MANAGER.getOnlinePlayers();
        PlayerChatEvent playerChatEvent = new PlayerChatEvent(player, players, message);

        // Call the event
        EventDispatcher.callCancellable(playerChatEvent, () -> {
            final Collection<Player> recipients = playerChatEvent.getRecipients();

            if (!recipients.isEmpty()) {
                // delegate to the messenger to avoid sending messages we shouldn't be
                Messenger.sendMessage(
                        recipients,
                        playerChatEvent.getFormattedMessage(),
                        ChatPosition.CHAT,
                        player.getUuid());
            }
        });
    }
}
