package io.freddi.hub.data;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import io.freddi.hub.config.Lobby;
import io.freddi.hub.utils.PlayerUtils;
import io.freddi.hub.utils.Utils;

public record PingResult(
        Player player,
        long latency,
        RegisteredServer server,
        ServerPing.Players players,
        Lobby lobby
) {

    public Double usage() {
        return (double) players.getOnline() / players.getMax();
    }

    public void connect() {
        Utils.util(PlayerUtils.class).connect(player, server, lobby);
    }
}