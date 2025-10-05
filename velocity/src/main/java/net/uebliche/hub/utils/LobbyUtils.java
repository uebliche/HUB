package net.uebliche.hub.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Lobby;
import net.uebliche.hub.data.PingResult;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class LobbyUtils extends Utils<LobbyUtils> {

    private final ExecutorService pingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, CachedPing> cachedServers = new ConcurrentHashMap<>();
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private ScheduledTask refreshTask;

    public LobbyUtils(Hub hub) {
        super(hub);
        var configUtils = Utils.util(ConfigUtils.class);
        if (configUtils != null) {
            configUtils.onReload(this::reschedule);
        }
        reschedule();
    }

    public Optional<PingResult> findBest(Player player) {
        var configUtils = Utils.util(ConfigUtils.class);
        var messageUtils = Utils.util(MessageUtils.class);
        var playerUtils = Utils.util(PlayerUtils.class);

        if (configUtils == null || configUtils.config() == null) {
            return Optional.empty();
        }

        messageUtils.sendDebugMessage(player, "🔍 Searching for Best Lobby Server...");

        var accessibleLobbies = configUtils.config().lobbies.stream()
                .sorted(Comparator.comparingInt(Lobby::priority).reversed())
                .filter(lobby -> playerUtils.permissionCheck(player, lobby))
                .toList();

        if (accessibleLobbies.isEmpty()) {
            messageUtils.sendDebugMessage(player, "<yellow>⚠️ No eligible lobbies available for this player.");
            return Optional.empty();
        }

        PingResult best = null;
        CachedPing bestSource = null;
        var noServerAnnouncements = new HashSet<String>();

        boolean attemptedRefresh = false;

        for (Lobby lobby : accessibleLobbies) {
            var matching = cachedServers.values().stream()
                    .filter(status -> lobby.filter.matcher(status.server().getServerInfo().getName()).matches())
                    .toList();

            if (matching.isEmpty() && !attemptedRefresh) {
                attemptedRefresh = true;
                messageUtils.sendDebugMessage(player, "<yellow>⚠️ Cache empty for lobby " + lobby.name + "; refreshing now...");
                try {
                    refreshNow().join();
                } catch (Exception exception) {
                    messageUtils.sendDebugMessage(player, "<red>❌ Failed to refresh lobby cache: " + exception.getClass().getSimpleName() + (exception.getMessage() != null ? " - " + exception.getMessage() : "") + ".");
                }
                matching = cachedServers.values().stream()
                        .filter(status -> lobby.filter.matcher(status.server().getServerInfo().getName()).matches())
                        .toList();
            }

            if (matching.isEmpty()) {
                if (noServerAnnouncements.add(lobby.name)) {
                    messageUtils.sendDebugMessage(player, "<yellow>⚠️ No cached servers available for " + lobby.name + ".");
                }
                continue;
            }

            CachedPing lobbyBest = matching.stream()
                    .min(Comparator.comparingDouble(this::score))
                    .orElse(null);

            if (lobbyBest != null) {
                best = new PingResult(player, lobbyBest.latency(), lobbyBest.server(), lobbyBest.players(), lobby);
                bestSource = lobbyBest;
                break;
            }
        }

        if (best == null) {
            messageUtils.sendDebugMessage(player, "<red>❌ No cached lobby data available.");
            return Optional.empty();
        }

        long age = Math.max(System.currentTimeMillis() - bestSource.updatedAt(), 0);
        var serverName = best.server().getServerInfo().getName();
        var online = best.players().getOnline();
        var max = best.players().getMax();
        var latency = best.latency();

        messageUtils.sendDebugMessage(player,
                "<yellow>🧭 Selected <gold>" + serverName + "</gold> "
                        + "<gray>(" + online + "<dark_gray>/" + max + " players, "
                        + "latency " + latency + "ms, cached " + age + "ms ago)</gray>");

        return Optional.of(best);
    }

    private double score(CachedPing cached) {
        double usage = cached.usage();
        return Math.abs((usage + 0.2) - 0.5);
    }

    private void reschedule() {
        cancelRefreshTask();
        var configUtils = Utils.util(ConfigUtils.class);
        if (configUtils == null || configUtils.config() == null) {
            return;
        }
        int refreshTicks = Math.max(configUtils.config().finder.refreshIntervalInTicks, 1);
        Duration interval = Duration.ofMillis(refreshTicks * 50L);
        refreshTask = hub.server().getScheduler()
                .buildTask(hub, this::refreshAll)
                .delay(Duration.ZERO)
                .repeat(interval)
                .schedule();
        refreshAll();
    }

    public CompletableFuture<Void> refreshNow() {
        return CompletableFuture.runAsync(this::refreshAll, pingExecutor);
    }

    public List<CachedResult> getCachedResults(Player player, Lobby lobby) {
        long now = System.currentTimeMillis();
        return cachedServers.values().stream()
                .filter(status -> lobby.filter.matcher(status.server().getServerInfo().getName()).matches())
                .sorted(Comparator.comparingDouble(this::score))
                .map(cached -> new CachedResult(
                        new PingResult(player, cached.latency(), cached.server(), cached.players(), lobby),
                        Math.max(now - cached.updatedAt(), 0)
                ))
                .toList();
    }

    private void refreshAll() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }
        try {
            var configUtils = Utils.util(ConfigUtils.class);
            if (configUtils == null || configUtils.config() == null) {
                return;
            }
            int timeoutMillis = Math.max(configUtils.config().finder.maxDuration, configUtils.config().finder.startDuration);
            timeoutMillis = Math.max(timeoutMillis, 50);
            Duration timeout = Duration.ofMillis(timeoutMillis);
            var lobbies = configUtils.config().lobbies;
            var servers = hub.server().getAllServers().stream()
                    .filter(server -> matchesAnyLobby(server, lobbies))
                    .toList();
            if (servers.isEmpty()) {
                cachedServers.clear();
                return;
            }
            var activeNames = servers.stream()
                    .map(server -> server.getServerInfo().getName())
                    .collect(Collectors.toSet());
            cachedServers.keySet().removeIf(name -> !activeNames.contains(name));
            var futures = servers.stream()
                    .collect(Collectors.toMap(server -> server.getServerInfo().getName(),
                            server -> CompletableFuture.supplyAsync(() -> pingServer(server, timeout), pingExecutor)));
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
            futures.forEach((name, future) -> {
                CachedPing result = future.getNow(null);
                if (result != null) {
                    cachedServers.put(name, result);
                } else {
                    cachedServers.remove(name);
                }
            });
        } finally {
            refreshing.set(false);
        }
    }

    private boolean matchesAnyLobby(RegisteredServer server, List<Lobby> lobbies) {
        var serverName = server.getServerInfo().getName();
        return lobbies.stream().anyMatch(lobby -> lobby.filter.matcher(serverName).matches());
    }

    private CachedPing pingServer(RegisteredServer server, Duration timeout) {
        long start = System.currentTimeMillis();
        var messageUtils = Utils.util(MessageUtils.class);
        if (messageUtils != null) {
            messageUtils.broadcastDebugMessage("<gray>📡 Pinging " + server.getServerInfo().getName() + " (timeout " + timeout.toMillis() + "ms)...</gray>");
        }
        try {
            var ping = server.ping().orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS).join();
            if (ping == null) {
                if (messageUtils != null) {
                    messageUtils.broadcastDebugMessage("<red>📡 Ping to " + server.getServerInfo().getName() + " returned no data.</red>");
                }
                return null;
            }
            var players = ping.getPlayers().orElseGet(() -> new ServerPing.Players(0, 1, List.of()));
            long now = System.currentTimeMillis();
            if (messageUtils != null) {
                messageUtils.broadcastDebugMessage("<green>📡 Ping success for " + server.getServerInfo().getName() + ": " + players.getOnline() + "/" + players.getMax() + " players, latency " + (now - start) + "ms.</green>");
            }
            return new CachedPing(server, players, now - start, now);
        } catch (Exception exception) {
            if (messageUtils != null) {
                messageUtils.broadcastDebugMessage("<red>📡 Ping failed for " + server.getServerInfo().getName() + ": " + exception.getClass().getSimpleName() + (exception.getMessage() != null ? " - " + exception.getMessage() : "") + "</red>");
            }
            return null;
        }
    }

    private void cancelRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    @Override
    public void close() {
        cancelRefreshTask();
        refreshing.set(false);
        cachedServers.clear();
        pingExecutor.shutdownNow();
    }

    private record CachedPing(RegisteredServer server, ServerPing.Players players, long latency, long updatedAt) {
        double usage() {
            int maxPlayers = Math.max(players.getMax(), 1);
            return (double) players.getOnline() / maxPlayers;
        }
    }

    public record CachedResult(PingResult result, long ageMillis) {
    }
}

