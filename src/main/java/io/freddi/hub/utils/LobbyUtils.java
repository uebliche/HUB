package io.freddi.hub.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.PingOptions;
import io.freddi.hub.Hub;
import io.freddi.hub.config.Lobby;
import io.freddi.hub.data.PingResult;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class LobbyUtils extends Utils<LobbyUtils> {

    protected ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public LobbyUtils(Hub hub) {
        super(hub);
    }

    public Stream<CompletableFuture<PingResult>> getLobbies(Lobby lobby, Duration timeout, Executor executor) {
        return hub.server().getAllServers().stream().filter(registeredServer -> lobby.filter.matcher(registeredServer.getServerInfo().getName()).matches()).map(registeredServer -> {
            return CompletableFuture.supplyAsync(() -> {
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
            }, executor);
        });
    }

    public PingResult findBest(Player player) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        messageUtils.sendDebugMessage(player, "🔎 Searching for Best Lobby Server...");
        PingResult best = null;
        AtomicInteger duration = new AtomicInteger(configUtils.config().finder.startDuration);
        while (best == null) {
            messageUtils.sendDebugMessage(player, "🔎 Checking Duration: " + duration.get());
            for (Lobby lobby : configUtils.config().lobbies) {
                if (Utils.util(PlayerUtils.class).permissionCheck(player, lobby)) {
                    var servers = getLobbies(lobby, Duration.of(duration.get(), ChronoUnit.MILLIS), executor).map(CompletableFuture::join).filter(Objects::nonNull).toList();
                    messageUtils.sendDebugMessage(player, "<green>🤖 Found " + servers.size() + " servers.");
                    var server = servers.stream()
                            .min(Comparator.comparingDouble(pingResult -> Math.abs((pingResult.usage() + 0.2) - 0.5)))
                            .orElse(null);
                    if (server != null && best == null) {
                        best = server;
                    }
                    break;
                }
            }
            if (duration.get() < configUtils.config().finder.maxDuration)
                messageUtils.sendDebugMessage(player, "🤖 Finder Timeout Duration got increased to " + duration.addAndGet(configUtils.config().finder.incrementDuration));

        }
        return best;
    }
}
