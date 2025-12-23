package net.uebliche.hub.utils;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Lobby;

import java.util.concurrent.CompletableFuture;

public class PlayerUtils extends Utils<PlayerUtils> {


    public PlayerUtils(Hub hub) {
        super(hub);
    }

    public boolean permissionCheck(Player player, Lobby lobby) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        messageUtils.sendDebugMessage(player, "🔎 Checking if user can join " + lobby.name);
        boolean allowed = lobby.permission.isBlank() || player.hasPermission(lobby.permission);
        DataCollector dataCollector = Utils.util(DataCollector.class);
        if (dataCollector != null) {
            dataCollector.recordPermission(player, lobby.permission, allowed);
        }
        if (allowed) {
            messageUtils.sendDebugMessage(player, "<green>✔ User has Permission to join " + lobby.name + ".");
            return true;
        } else {
            messageUtils.sendDebugMessage(player, "<red>❌ User has no Permission to join " + lobby.name + ".");
            return false;
        }
    }

    public CompletableFuture<Boolean> connect(Player player, RegisteredServer server, Lobby lobby) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        messageUtils.sendDebugMessage(player, "✈ Sending player to " + server.getServerInfo().getName() + " as member of " + lobby.name);
        if(player.getCurrentServer().isPresent()){
            messageUtils.sendDebugMessage(player, "🔎 Checking if user is already connected to " + server.getServerInfo().getName());
            if (player.getCurrentServer().get().getServerInfo().getName().equals(server.getServerInfo().getName())) {
                messageUtils.sendDebugMessage(player, "<red>❌ User is already connected to " + server.getServerInfo().getName() + ".");
                messageUtils.sendMessage(player, lobby.messages().alreadyConnectedMessage == null ? configUtils.config().messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, server, lobby);
                return CompletableFuture.completedFuture(false);
            }
        } else {
            messageUtils.sendDebugMessage(player, "<red>❌ User is not connected to any server.");
        }
        return player.createConnectionRequest(server).connect().thenApply(connection -> {
            if (connection.getStatus() == ConnectionRequestBuilder.Status.SUCCESS) {
                messageUtils.sendMessage(player, lobby.messages().successMessage == null ? configUtils.config().messages.successMessage : lobby.messages().successMessage, server, lobby);
                return true;
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.ALREADY_CONNECTED) {
                messageUtils.sendMessage(player, lobby.messages().alreadyConnectedMessage == null ? configUtils.config().messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, server, lobby);
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS) {
                messageUtils.sendMessage(player, lobby.messages().connectionInProgressMessage == null ? configUtils.config().messages.connectionInProgressMessage : lobby.messages().connectionInProgressMessage, server, lobby);
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.SERVER_DISCONNECTED) {
                messageUtils.sendMessage(player, lobby.messages().serverDisconnectedMessage == null ? configUtils.config().messages.serverDisconnectedMessage : lobby.messages().serverDisconnectedMessage, server, lobby);
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_CANCELLED) {
                messageUtils.sendMessage(player, lobby.messages().connectionCancelledMessage == null ? configUtils.config().messages.connectionCancelledMessage : lobby.messages().connectionCancelledMessage, server, lobby);
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
