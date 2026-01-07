package net.uebliche.hub.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Lobby;
import net.uebliche.hub.data.PingResult;
import net.uebliche.hub.utils.MessageUtils.DebugCategory;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        return findBest(player, Set.of());
    }

    public Optional<RegisteredServer> findForcedHostServer(Player player, String host) {
        var configUtils = Utils.util(ConfigUtils.class);
        var messageUtils = Utils.util(MessageUtils.class);
        var playerUtils = Utils.util(PlayerUtils.class);
        if (configUtils == null || messageUtils == null || playerUtils == null) {
            return Optional.empty();
        }
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }

        String normalizedHost = normalizeHost(host);
        if (normalizedHost.isBlank()) {
            return Optional.empty();
        }

        var config = configUtils.config();
        if (config == null) {
            return Optional.empty();
        }

        var direct = resolveDirectForcedHost(player, normalizedHost, config, messageUtils);
        if (direct.isPresent()) {
            return direct;
        }

        var lobbyTarget = resolveLobbyForcedHost(player, normalizedHost, config, messageUtils, playerUtils);
        if (lobbyTarget.isPresent()) {
            return lobbyTarget;
        }

        return resolveGroupForcedHost(player, normalizedHost, config, messageUtils, playerUtils);
    }

    public Optional<PingResult> findBest(Player player, Set<String> excludedServerNames) {
        var configUtils = Utils.util(ConfigUtils.class);
        var messageUtils = Utils.util(MessageUtils.class);
        var playerUtils = Utils.util(PlayerUtils.class);
        var excludedNormalized = excludedServerNames.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (configUtils == null || configUtils.config() == null) {
            return Optional.empty();
        }

        messageUtils.sendDebugMessage(DebugCategory.FINDER, player, "🔍 Searching for Best Lobby Server...");

        var accessibleLobbies = configUtils.config().lobbies.stream()
                .sorted(Comparator.comparingInt(Lobby::priority).reversed())
                .filter(lobby -> playerUtils.permissionCheck(player, lobby))
                .toList();

        if (accessibleLobbies.isEmpty()) {
            messageUtils.sendDebugMessage(DebugCategory.FINDER, player, "<yellow>⚠️ No eligible lobbies available for this player.");
            return Optional.empty();
        }

        AtomicBoolean attemptedRefresh = new AtomicBoolean(false);

        var preferred = preferLastLobby(player, accessibleLobbies, attemptedRefresh, excludedNormalized);
        if (preferred.isPresent()) {
            return preferred;
        }

        PingResult best = null;
        CachedPing bestSource = null;
        var noServerAnnouncements = new HashSet<String>();

        for (Lobby lobby : accessibleLobbies) {
            var matching = cachedServers.values().stream()
                    .filter(status -> lobby.filter.matcher(status.server().getServerInfo().getName()).matches())
                    .filter(status -> !excludedNormalized.contains(status.server().getServerInfo().getName().toLowerCase(Locale.ROOT)))
                    .toList();

            if (matching.isEmpty() && !attemptedRefresh.get()) {
                attemptedRefresh.set(true);
                messageUtils.sendDebugMessage(DebugCategory.FINDER, player, "<yellow>⚠️ Cache empty for lobby " + lobby.name + "; refreshing now...");
                try {
                    refreshNow().join();
                } catch (Exception exception) {
                    messageUtils.sendDebugMessage(DebugCategory.FINDER, player, "<red>❌ Failed to refresh lobby cache: " + exception.getClass().getSimpleName() + (exception.getMessage() != null ? " - " + exception.getMessage() : "") + ".");
                }
                matching = cachedServers.values().stream()
                        .filter(status -> lobby.filter.matcher(status.server().getServerInfo().getName()).matches())
                        .filter(status -> !excludedNormalized.contains(status.server().getServerInfo().getName().toLowerCase(Locale.ROOT)))
                        .toList();
            }

            if (matching.isEmpty()) {
                if (noServerAnnouncements.add(lobby.name)) {
                    messageUtils.sendDebugMessage(DebugCategory.FINDER, player, "<yellow>⚠️ No cached servers available for " + lobby.name + ".");
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
            messageUtils.sendDebugMessage(DebugCategory.FINDER, player, "<red>❌ No cached lobby data available.");
            return Optional.empty();
        }

        long age = Math.max(System.currentTimeMillis() - bestSource.updatedAt(), 0);
        var serverName = best.server().getServerInfo().getName();
        var online = best.players().getOnline();
        var max = best.players().getMax();
        var latency = best.latency();

        messageUtils.sendDebugMessage(DebugCategory.FINDER, player,
                "<yellow>🧭 Selected <gold>" + serverName + "</gold> "
                        + "<gray>(" + online + "<dark_gray>/" + max + " players, "
                        + "latency " + latency + "ms, cached " + age + "ms ago)</gray>");

        return Optional.of(best);
    }

    public Optional<PingResult> findBestForLobby(Player player, Lobby lobby) {
        return findBestForLobby(player, lobby, Set.of());
    }

    public Optional<PingResult> findBestForLobby(Player player, Lobby lobby, Set<String> excludedServerNames) {
        var configUtils = Utils.util(ConfigUtils.class);
        var messageUtils = Utils.util(MessageUtils.class);
        var playerUtils = Utils.util(PlayerUtils.class);

        if (configUtils == null || messageUtils == null || playerUtils == null || lobby == null) {
            return Optional.empty();
        }

        if (!playerUtils.permissionCheck(player, lobby)) {
            return Optional.empty();
        }

        var excludedNormalized = excludedServerNames.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        AtomicBoolean attemptedRefresh = new AtomicBoolean(false);

        var candidates = getCachedResults(player, lobby).stream()
                .filter(result -> !excludedNormalized.contains(result.result().server().getServerInfo().getName().toLowerCase(Locale.ROOT)))
                .toList();

        if (candidates.isEmpty() && !attemptedRefresh.get()) {
            attemptedRefresh.set(true);
            messageUtils.sendDebugMessage(DebugCategory.FINDER, player, "<yellow>⚠️ Cache empty for lobby " + lobby.name + "; refreshing now...</yellow>");
            try {
                refreshNow().join();
            } catch (Exception exception) {
                messageUtils.sendDebugMessage(DebugCategory.FINDER, player, "<red>❌ Failed to refresh lobby cache: " + exception.getClass().getSimpleName() + (exception.getMessage() != null ? " - " + exception.getMessage() : "") + ".");
            }
            candidates = getCachedResults(player, lobby).stream()
                    .filter(result -> !excludedNormalized.contains(result.result().server().getServerInfo().getName().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (candidates.isEmpty()) {
            messageUtils.sendDebugMessage(DebugCategory.FINDER, player, "<yellow>⚠️ No cached servers available for " + lobby.name + ".");
            return Optional.empty();
        }

        var best = candidates.getFirst();
        var serverName = best.result().server().getServerInfo().getName();
        var online = best.result().players().getOnline();
        var max = best.result().players().getMax();
        var latency = best.result().latency();
        messageUtils.sendDebugMessage(DebugCategory.FINDER, player,
                "<yellow>🧭 Selected <gold>" + serverName + "</gold> "
                        + "<gray>(" + online + "<dark_gray>/" + max + " players, "
                        + "latency " + latency + "ms, cached " + best.ageMillis() + "ms ago)</gray>");

        return Optional.of(best.result());
    }

    private Optional<PingResult> preferLastLobby(Player player, List<Lobby> accessibleLobbies, AtomicBoolean attemptedRefresh, Set<String> excludedServers) {
        var configUtils = Utils.util(ConfigUtils.class);
        var messageUtils = Utils.util(MessageUtils.class);
        var lastLobbyTracker = Utils.util(LastLobbyTracker.class);

        if (configUtils == null || messageUtils == null || lastLobbyTracker == null) {
            return Optional.empty();
        }

        if (!configUtils.config().lastLobby.enabled) {
            return Optional.empty();
        }

        var remembered = lastLobbyTracker.get(player);
        if (remembered.isEmpty()) {
            return Optional.empty();
        }

        var lobbyOpt = accessibleLobbies.stream()
                .filter(lobby -> lobby.name.equalsIgnoreCase(remembered.get().lobbyName()))
                .findFirst();

        if (lobbyOpt.isEmpty()) {
            messageUtils.sendDebugMessage(DebugCategory.LAST_LOBBY, player, "<yellow>⚠️ Last lobby " + remembered.get().lobbyName() + " is not available for this player.");
            return Optional.empty();
        }

        var lobby = lobbyOpt.get();
        var preferredServerName = remembered.get().serverName();
        var cached = getCachedResults(player, lobby);
        var preferredServerKey = preferredServerName.toLowerCase(Locale.ROOT);
        if (excludedServers.contains(preferredServerKey)) {
            messageUtils.sendDebugMessage(DebugCategory.LAST_LOBBY, player, "<yellow>⚠️ Remembered lobby server " + preferredServerName + " is excluded; falling back to normal selection.");
            return Optional.empty();
        }

        if (cached.isEmpty() && !attemptedRefresh.get()) {
            attemptedRefresh.set(true);
            messageUtils.sendDebugMessage(DebugCategory.LAST_LOBBY, player, "<yellow>⚠️ Cache empty for remembered lobby; refreshing now...</yellow>");
            try {
                refreshNow().join();
            } catch (Exception exception) {
                messageUtils.sendDebugMessage(DebugCategory.LAST_LOBBY, player, "<red>❌ Failed to refresh lobby cache: " + exception.getClass().getSimpleName() + (exception.getMessage() != null ? " - " + exception.getMessage() : "") + ".");
            }
            cached = getCachedResults(player, lobby);
        }

        var preferred = cached.stream()
                .filter(result -> !excludedServers.contains(result.result().server().getServerInfo().getName().toLowerCase(Locale.ROOT)))
                .filter(result -> result.result().server().getServerInfo().getName().equalsIgnoreCase(preferredServerName))
                .findFirst();

        if (preferred.isPresent()) {
            messageUtils.sendDebugMessage(DebugCategory.LAST_LOBBY, player, "<green>⏪ Preferring last lobby server " + preferredServerName + ".</green>");
            return Optional.of(preferred.get().result());
        }

        messageUtils.sendDebugMessage(DebugCategory.LAST_LOBBY, player, "<yellow>⚠️ Last lobby server " + preferredServerName + " is unavailable; falling back to normal selection.");
        return Optional.empty();
    }

    private double score(CachedPing cached) {
        double usage = cached.usage();
        return Math.abs((usage + 0.2) - 0.5);
    }

    private String normalizeHost(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        int portIndex = value.indexOf(':');
        if (portIndex > -1) {
            value = value.substring(0, portIndex);
        }
        return value;
    }

    private boolean hostMatches(String host, String entry) {
        String candidate = normalizeHost(entry);
        if (candidate.isBlank()) {
            return false;
        }
        if (candidate.startsWith("*.")) {
            String suffix = candidate.substring(2);
            return host.equalsIgnoreCase(suffix) || host.endsWith("." + suffix);
        }
        return host.equalsIgnoreCase(candidate);
    }

    private Optional<RegisteredServer> resolveDirectForcedHost(Player player, String host, net.uebliche.hub.config.Config config,
                                                               MessageUtils messageUtils) {
        var entries = config.forcedHosts != null ? config.forcedHosts : List.<net.uebliche.hub.config.Config.ForcedHost>of();
        for (var entry : entries) {
            if (entry == null || entry.host == null || entry.host.isBlank()) {
                continue;
            }
            if (!hostMatches(host, entry.host)) {
                continue;
            }
            String targetName = entry.server == null ? "" : entry.server.trim();
            if (targetName.isBlank()) {
                messageUtils.sendDebugMessage(DebugCategory.FORCED_HOSTS, player,
                        "<yellow>⚠️ Forced host " + host + " matched but no server is configured.</yellow>");
                continue;
            }
            var serverOpt = hub.server().getServer(targetName);
            if (serverOpt.isEmpty()) {
                messageUtils.sendDebugMessage(DebugCategory.FORCED_HOSTS, player,
                        "<red>❌ Forced host " + host + " targets missing server " + targetName + ".</red>");
                continue;
            }
            messageUtils.sendDebugMessage(DebugCategory.FORCED_HOSTS, player,
                    "<green>🔗 Forced host " + host + " -> server " + targetName + ".</green>");
            return serverOpt;
        }
        return Optional.empty();
    }

    private Optional<RegisteredServer> resolveLobbyForcedHost(Player player, String host, net.uebliche.hub.config.Config config,
                                                              MessageUtils messageUtils, PlayerUtils playerUtils) {
        var lobbies = config.lobbies != null ? config.lobbies : List.<Lobby>of();
        var candidates = lobbies.stream()
                .filter(lobby -> lobby.forcedHosts != null && !lobby.forcedHosts.isEmpty())
                .filter(lobby -> lobby.forcedHosts.stream().anyMatch(entry -> hostMatches(host, entry)))
                .sorted(Comparator.comparingInt(Lobby::priority).reversed())
                .toList();

        for (Lobby lobby : candidates) {
            if (!playerUtils.permissionCheck(player, lobby)) {
                messageUtils.sendDebugMessage(DebugCategory.FORCED_HOSTS, player,
                        "<yellow>⚠️ Forced host " + host + " matches " + lobby.name + " but permission is missing.</yellow>");
                continue;
            }
            var result = findBestForLobby(player, lobby);
            if (result.isPresent()) {
                messageUtils.sendDebugMessage(DebugCategory.FORCED_HOSTS, player,
                        "<green>🔗 Forced host " + host + " -> lobby " + lobby.name + ".</green>");
                return Optional.of(result.get().server());
            }
        }
        return Optional.empty();
    }

    private Optional<RegisteredServer> resolveGroupForcedHost(Player player, String host, net.uebliche.hub.config.Config config,
                                                              MessageUtils messageUtils, PlayerUtils playerUtils) {
        if (config.lobbyGroups == null || config.lobbyGroups.isEmpty()) {
            return Optional.empty();
        }
        var lobbyByName = indexLobbies(config.lobbies);
        var groupByName = indexGroups(config.lobbyGroups);
        var childrenByGroup = buildGroupChildren(groupByName);

        for (var group : config.lobbyGroups) {
            if (group == null || group.forcedHosts == null || group.forcedHosts.isEmpty()) {
                continue;
            }
            if (group.forcedHosts.stream().noneMatch(entry -> hostMatches(host, entry))) {
                continue;
            }
            var lobbies = collectGroupLobbies(group, lobbyByName, groupByName, childrenByGroup, messageUtils, player);
            if (lobbies.isEmpty()) {
                continue;
            }
            var ordered = lobbies.stream()
                    .sorted(Comparator.comparingInt(Lobby::priority).reversed())
                    .toList();
            for (var lobby : ordered) {
                if (!playerUtils.permissionCheck(player, lobby)) {
                    messageUtils.sendDebugMessage(DebugCategory.FORCED_HOSTS, player,
                            "<yellow>⚠️ Forced host " + host + " matched group " + group.name + " but permission is missing for " + lobby.name + ".</yellow>");
                    continue;
                }
                var result = findBestForLobby(player, lobby);
                if (result.isPresent()) {
                    messageUtils.sendDebugMessage(DebugCategory.FORCED_HOSTS, player,
                            "<green>🔗 Forced host " + host + " -> group " + group.name + " (" + lobby.name + ").</green>");
                    return Optional.of(result.get().server());
                }
            }
        }
        return Optional.empty();
    }

    private Map<String, Lobby> indexLobbies(List<Lobby> lobbies) {
        var map = new java.util.LinkedHashMap<String, Lobby>();
        if (lobbies == null) {
            return map;
        }
        for (var lobby : lobbies) {
            String key = normalizeName(lobby == null ? "" : lobby.name);
            if (!key.isBlank()) {
                map.put(key, lobby);
            }
        }
        return map;
    }

    private Map<String, net.uebliche.hub.config.Config.LobbyGroup> indexGroups(
            List<net.uebliche.hub.config.Config.LobbyGroup> groups) {
        var map = new java.util.LinkedHashMap<String, net.uebliche.hub.config.Config.LobbyGroup>();
        if (groups == null) {
            return map;
        }
        for (var group : groups) {
            String key = normalizeName(group == null ? "" : group.name);
            if (!key.isBlank()) {
                map.put(key, group);
            }
        }
        return map;
    }

    private Map<String, List<String>> buildGroupChildren(
            Map<String, net.uebliche.hub.config.Config.LobbyGroup> groupByName) {
        var children = new java.util.LinkedHashMap<String, List<String>>();
        groupByName.values().forEach(group -> {
            String nameKey = normalizeName(group.name);
            String parentKey = normalizeName(group.parentGroup);
            if (nameKey.isBlank() || parentKey.isBlank() || parentKey.equals(nameKey)) {
                return;
            }
            if (!groupByName.containsKey(parentKey)) {
                return;
            }
            children.computeIfAbsent(parentKey, ignored -> new java.util.ArrayList<>()).add(nameKey);
        });
        return children;
    }

    private List<Lobby> collectGroupLobbies(net.uebliche.hub.config.Config.LobbyGroup group, Map<String, Lobby> lobbyByName,
                                            Map<String, net.uebliche.hub.config.Config.LobbyGroup> groupByName,
                                            Map<String, List<String>> childrenByGroup,
                                            MessageUtils messageUtils, Player player) {
        var results = new java.util.ArrayList<Lobby>();
        String rootKey = normalizeName(group == null ? "" : group.name);
        if (rootKey.isBlank()) {
            return results;
        }
        collectGroupLobbies(rootKey, lobbyByName, groupByName, childrenByGroup,
                new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>(), results, messageUtils, player);
        return results;
    }

    private void collectGroupLobbies(String groupKey, Map<String, Lobby> lobbyByName,
                                     Map<String, net.uebliche.hub.config.Config.LobbyGroup> groupByName,
                                     Map<String, List<String>> childrenByGroup,
                                     Set<String> visitedGroups, Set<String> visitedLobbies, List<Lobby> out,
                                     MessageUtils messageUtils, Player player) {
        if (!visitedGroups.add(groupKey)) {
            return;
        }
        var group = groupByName.get(groupKey);
        if (group == null) {
            return;
        }
        var entries = group.lobbies != null ? group.lobbies : List.<String>of();
        for (String lobbyName : entries) {
            String lobbyKey = normalizeName(lobbyName);
            if (lobbyKey.isBlank()) {
                continue;
            }
            var lobby = lobbyByName.get(lobbyKey);
            if (lobby == null) {
                messageUtils.sendDebugMessage(DebugCategory.FORCED_HOSTS, player,
                        "<yellow>⚠️ Lobby '" + lobbyName + "' from group '" + group.name + "' not found; skipping.</yellow>");
                continue;
            }
            if (visitedLobbies.add(lobby.name.toLowerCase(Locale.ROOT))) {
                out.add(lobby);
            }
        }
        var children = childrenByGroup.getOrDefault(groupKey, List.of());
        for (var childKey : children) {
            collectGroupLobbies(childKey, lobbyByName, groupByName, childrenByGroup,
                    visitedGroups, visitedLobbies, out, messageUtils, player);
        }
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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
            messageUtils.broadcastDebugMessage(DebugCategory.PINGS,
                    "<gray>📡 Pinging " + server.getServerInfo().getName() + " (timeout " + timeout.toMillis() + "ms)...</gray>");
        }
        try {
            var ping = server.ping().orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS).join();
            if (ping == null) {
                if (messageUtils != null) {
                    messageUtils.broadcastDebugMessage(DebugCategory.PINGS,
                            "<red>📡 Ping to " + server.getServerInfo().getName() + " returned no data.</red>");
                }
                return null;
            }
            var players = ping.getPlayers().orElseGet(() -> new ServerPing.Players(0, 1, List.of()));
            long now = System.currentTimeMillis();
            if (messageUtils != null) {
                messageUtils.broadcastDebugMessage(DebugCategory.PINGS,
                        "<green>📡 Ping success for " + server.getServerInfo().getName() + ": " + players.getOnline() + "/" + players.getMax() + " players, latency " + (now - start) + "ms.</green>");
            }
            return new CachedPing(server, players, now - start, now);
        } catch (Exception exception) {
            if (messageUtils != null) {
                messageUtils.broadcastDebugMessage(DebugCategory.PINGS,
                        "<red>📡 Ping failed for " + server.getServerInfo().getName() + ": " + exception.getClass().getSimpleName() + (exception.getMessage() != null ? " - " + exception.getMessage() : "") + "</red>");
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

