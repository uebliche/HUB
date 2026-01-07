package net.uebliche.hub.utils;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Lobby;
import net.uebliche.hub.utils.MessageUtils.DebugCategory;

import java.util.concurrent.CompletableFuture;

public class PlayerUtils extends Utils<PlayerUtils> {


    public PlayerUtils(Hub hub) {
        super(hub);
    }

    public boolean permissionCheck(Player player, Lobby lobby) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        messageUtils.sendDebugMessage(DebugCategory.PERMISSIONS, player, "🔎 Checking if user can join " + lobby.name);
        boolean allowed = lobby.permission.isBlank() || player.hasPermission(lobby.permission);
        DataCollector dataCollector = Utils.util(DataCollector.class);
        if (dataCollector != null) {
            dataCollector.recordPermission(player, lobby.permission, allowed);
        }
        if (allowed) {
            messageUtils.sendDebugMessage(DebugCategory.PERMISSIONS, player, "<green>✔ User has Permission to join " + lobby.name + ".");
            return true;
        } else {
            messageUtils.sendDebugMessage(DebugCategory.PERMISSIONS, player, "<red>❌ User has no Permission to join " + lobby.name + ".");
            return false;
        }
    }

    public CompletableFuture<Boolean> connect(Player player, RegisteredServer server, Lobby lobby) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        messageUtils.sendDebugMessage(DebugCategory.TRANSFER, player, "✈ Sending player to " + server.getServerInfo().getName() + " as member of " + lobby.name);
        if(player.getCurrentServer().isPresent()){
            messageUtils.sendDebugMessage(DebugCategory.TRANSFER, player, "🔎 Checking if user is already connected to " + server.getServerInfo().getName());
            if (player.getCurrentServer().get().getServerInfo().getName().equals(server.getServerInfo().getName())) {
                messageUtils.sendDebugMessage(DebugCategory.TRANSFER, player, "<red>❌ User is already connected to " + server.getServerInfo().getName() + ".");
                String fallback = lobby.messages().alreadyConnectedMessage == null
                        ? configUtils.config().messages.alreadyConnectedMessage
                        : lobby.messages().alreadyConnectedMessage;
                messageUtils.sendMessage(player, messageUtils.i18n("proxy.message.already-connected", player, fallback, server, lobby));
                return CompletableFuture.completedFuture(false);
            }
        } else {
            messageUtils.sendDebugMessage(DebugCategory.TRANSFER, player, "<red>❌ User is not connected to any server.");
        }
        return player.createConnectionRequest(server).connect().thenApply(connection -> {
            if (connection.getStatus() == ConnectionRequestBuilder.Status.SUCCESS) {
                String fallback = lobby.messages().successMessage == null
                        ? configUtils.config().messages.successMessage
                        : lobby.messages().successMessage;
                messageUtils.sendMessage(player, messageUtils.i18n("proxy.message.success", player, fallback, server, lobby));
                return true;
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.ALREADY_CONNECTED) {
                String fallback = lobby.messages().alreadyConnectedMessage == null
                        ? configUtils.config().messages.alreadyConnectedMessage
                        : lobby.messages().alreadyConnectedMessage;
                messageUtils.sendMessage(player, messageUtils.i18n("proxy.message.already-connected", player, fallback, server, lobby));
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS) {
                String fallback = lobby.messages().connectionInProgressMessage == null
                        ? configUtils.config().messages.connectionInProgressMessage
                        : lobby.messages().connectionInProgressMessage;
                messageUtils.sendMessage(player, messageUtils.i18n("proxy.message.in-progress", player, fallback, server, lobby));
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.SERVER_DISCONNECTED) {
                String fallback = lobby.messages().serverDisconnectedMessage == null
                        ? configUtils.config().messages.serverDisconnectedMessage
                        : lobby.messages().serverDisconnectedMessage;
                messageUtils.sendMessage(player, messageUtils.i18n("proxy.message.server-disconnected", player, fallback, server, lobby));
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_CANCELLED) {
                String fallback = lobby.messages().connectionCancelledMessage == null
                        ? configUtils.config().messages.connectionCancelledMessage
                        : lobby.messages().connectionCancelledMessage;
                messageUtils.sendMessage(player, messageUtils.i18n("proxy.message.cancelled", player, fallback, server, lobby));
            }
            return false;
        });
    }

    public boolean canDebug(Player player) {
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        return (
                configUtils.config().debug.permission.isBlank() ||
                        player.hasPermission(configUtils.config().debug.permission)
        );
    }
}
