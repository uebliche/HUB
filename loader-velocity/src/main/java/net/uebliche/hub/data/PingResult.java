package net.uebliche.hub.data;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.uebliche.hub.config.Lobby;
import net.uebliche.hub.utils.PlayerUtils;
import net.uebliche.hub.utils.Utils;

public record PingResult(
        Player player,
        long latency,
        RegisteredServer server,
        ServerPing.Players players,
        Lobby lobby
) {

    public Double usage() {
        int maxPlayers = Math.max(players.getMax(), 1);
        return (double) players.getOnline() / maxPlayers;
    }

    public void connect() {
        Utils.util(PlayerUtils.class).connect(player, server, lobby);
    }
}
