package io.freddi.hub.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.PingOptions;
import io.freddi.hub.Hub;
import io.freddi.hub.config.Lobby;
import io.freddi.hub.data.PingResult;

import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class LobbyUtils extends Utils<LobbyUtils> {

    protected ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public LobbyUtils(Hub hub) {
        super(hub);
    }

    public Stream<CompletableFuture<PingResult>> getLobbies(Lobby lobby, Duration timeout, Executor executor) {
        return hub.server().getAllServers().stream().filter(registeredServer -> lobby.filter.matcher(registeredServer.getServerInfo().getName()).matches()).map(registeredServer -> CompletableFuture.supplyAsync(() -> {
            var time = System.currentTimeMillis();
            try {
                var ping = registeredServer.ping(PingOptions.builder().timeout(timeout).build()).join();
                if (ping != null && ping.getPlayers().isPresent()) {
                    var players = ping.getPlayers().get();
                    return new PingResult(System.currentTimeMillis() - time, registeredServer, players);
                }
            } catch (Exception ignored) {
            }
            return null;
        }, executor));
    }

    public PingResult findBest(Player player) {
        var messageUtils = Utils.util(MessageUtils.class);
        var config = Utils.util(ConfigUtils.class).config();
        var playerUtils = Utils.util(PlayerUtils.class);

        messageUtils.sendDebugMessage(player, "🔎 Searching for Best Lobby Server...");

        PingResult best = null;
        int duration = config.finder.startDuration;

        var sortedLobbies = config.lobbies.stream()
                .sorted(Comparator.comparingInt(Lobby::priority).reversed())
                .toList();

        while (best == null && duration <= config.finder.maxDuration) {
            messageUtils.sendDebugMessage(player, "🔎 Checking Duration: " + duration);


            for (Lobby lobby : sortedLobbies) {
                if (!playerUtils.permissionCheck(player, lobby)) continue;

                var servers = getLobbies(lobby, Duration.ofMillis(duration), executor)
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList();

                messageUtils.sendDebugMessage(player, "<green>🤖 Found " + servers.size() + " servers.");

                best = servers.stream()
                        .min(Comparator.comparingDouble(p -> Math.abs((p.usage() + 0.2) - 0.5)))
                        .orElse(null);

                // Sobald ein Server gefunden wurde, keine weiteren Lobbies prüfen
                if (best != null) break;
            }

            if (best == null) {
                duration += config.finder.incrementDuration;
                messageUtils.sendDebugMessage(player, "🤖 Finder Timeout Duration got increased to " + duration);
            }
        }

        return best;
    }
}
