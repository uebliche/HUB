package net.uebliche.hub.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Lobby;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LastLobbyTracker extends Utils<LastLobbyTracker> {

    private final Map<UUID, Entry> lastLobbies = new ConcurrentHashMap<>();

    public LastLobbyTracker(Hub hub) {
        super(hub);
    }

    public void remember(Player player, Lobby lobby, RegisteredServer server) {
        if (player == null || lobby == null || server == null) {
            return;
        }
        lastLobbies.put(player.getUniqueId(),
                new Entry(lobby.name, server.getServerInfo().getName(), System.currentTimeMillis()));
    }

    public Optional<Entry> get(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lastLobbies.get(player.getUniqueId()));
    }

    public void forget(Player player) {
        if (player == null) {
            return;
        }
        forget(player.getUniqueId());
    }

    public void forget(UUID playerId) {
        if (playerId == null) {
            return;
        }
        lastLobbies.remove(playerId);
    }

    @Override
    public void close() {
        lastLobbies.clear();
    }

    public record Entry(String lobbyName, String serverName, long timestamp) {
    }
}
