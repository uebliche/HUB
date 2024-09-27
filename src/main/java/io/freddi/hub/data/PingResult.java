package io.freddi.hub.data;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;

public record PingResult(
        long latency,
        RegisteredServer server,
        ServerPing.Players players
) {

    public Double usage() {
        return (double) players.getOnline() / players.getMax();
    }
}