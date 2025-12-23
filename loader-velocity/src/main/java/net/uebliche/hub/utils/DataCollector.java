package net.uebliche.hub.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Config;
import net.uebliche.hub.config.Lobby;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class DataCollector extends Utils<DataCollector> {

    private final ProxyServer server;
    private final Path dataDirectory;

    private final ConcurrentMap<String, UserSnapshot> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> serversSeen = new ConcurrentHashMap<>();

    private volatile Settings settings = new Settings();
    private ScheduledTask dumpTask;

    public DataCollector(Hub hub, Path dataDirectory) {
        super(hub);
        this.server = hub.server();
        this.dataDirectory = dataDirectory;
        var configUtils = Utils.util(ConfigUtils.class);
        if (configUtils != null) {
            configUtils.onReload(this::reloadSettings);
        }
        reloadSettings();
    }

    public void recordPlayer(Player player) {
        Settings current = settings;
        if (!current.enabled || player == null) {
            return;
        }
        String key = userKey(player, current);
        if (key == null || key.isBlank()) {
            return;
        }
        UserSnapshot snapshot = users.computeIfAbsent(key, k -> new UserSnapshot(player.getUsername(),
                current.includeUuid ? player.getUniqueId().toString() : null));
        snapshot.name = player.getUsername();
        if (current.includeUuid) {
            snapshot.uuid = player.getUniqueId().toString();
        }
        snapshot.lastSeen = System.currentTimeMillis();
    }

    public void recordPermission(Player player, String permission, boolean allowed) {
        Settings current = settings;
        if (!current.enabled || !allowed || permission == null || permission.isBlank()) {
            return;
        }
        recordPlayer(player);
        String key = userKey(player, current);
        if (key == null || key.isBlank()) {
            return;
        }
        UserSnapshot snapshot = users.get(key);
        if (snapshot != null) {
            snapshot.permissions.add(permission);
        }
    }

    public void recordServer(String serverName) {
        Settings current = settings;
        if (!current.enabled || serverName == null || serverName.isBlank()) {
            return;
        }
        serversSeen.put(serverName, System.currentTimeMillis());
    }

    public void dumpNow() {
        Settings current = settings;
        if (!current.enabled) {
            return;
        }
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException exception) {
            logger.warn("Failed to create data dump directory {}", dataDirectory, exception);
            return;
        }
        var configUtils = Utils.util(ConfigUtils.class);
        List<Lobby> lobbies = configUtils != null && configUtils.config() != null
                ? configUtils.config().lobbies
                : List.of();
        if (lobbies.isEmpty()) {
            server.getAllServers().forEach(registeredServer ->
                    recordServer(registeredServer.getServerInfo().getName()));
        } else {
            server.getAllServers().stream()
                    .filter(registeredServer -> matchesAnyLobby(registeredServer, lobbies))
                    .forEach(registeredServer -> recordServer(registeredServer.getServerInfo().getName()));
        }
        prune(current);

        DataDump dump = buildDump(current);
        Path dumpPath = dataDirectory.resolve(current.dumpFile);
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(dumpPath)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();
        try {
            CommentedConfigurationNode node = loader.createNode();
            node.set(DataDump.class, dump);
            loader.save(node);
        } catch (SerializationException exception) {
            logger.warn("Failed to serialize data dump to {}", dumpPath, exception);
        } catch (IOException exception) {
            logger.warn("Failed to write data dump to {}", dumpPath, exception);
        }
    }

    @Override
    public void close() {
        cancelDumpTask();
        dumpNow();
    }

    private void reloadSettings() {
        var configUtils = Utils.util(ConfigUtils.class);
        Config.DataCollection config = configUtils != null && configUtils.config() != null
                ? configUtils.config().dataCollection
                : new Config.DataCollection();
        settings = new Settings(config);
        reschedule();
    }

    private void reschedule() {
        cancelDumpTask();
        Settings current = settings;
        if (!current.enabled) {
            return;
        }
        int interval = Math.max(current.dumpIntervalMinutes, 1);
        dumpTask = hub.server().getScheduler()
                .buildTask(hub, this::dumpNow)
                .delay(Duration.ZERO)
                .repeat(Duration.ofMinutes(interval))
                .schedule();
        dumpNow();
    }

    private void cancelDumpTask() {
        if (dumpTask != null) {
            dumpTask.cancel();
            dumpTask = null;
        }
    }

    private DataDump buildDump(Settings current) {
        DataDump dump = new DataDump();
        dump.version = 1;
        dump.generatedAt = Instant.now().toString();
        dump.servers = serversSeen.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(Math.max(current.maxServers, 1))
                .map(Map.Entry::getKey)
                .toList();

        if (configUtils != null && configUtils.config() != null) {
            dump.lobbies = configUtils.config().lobbies.stream()
                    .map(lobby -> lobby.name)
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        }

        List<UserSnapshot> sortedUsers = users.values().stream()
                .sorted(Comparator.comparingLong(UserSnapshot::lastSeen).reversed())
                .limit(Math.max(current.maxUsers, 1))
                .toList();

        dump.users = sortedUsers.stream()
                .map(snapshot -> snapshot.toDump(current.includeUuid))
                .toList();
        return dump;
    }

    private void prune(Settings current) {
        int maxUsers = Math.max(current.maxUsers, 1);
        if (users.size() > maxUsers * 2L) {
            Set<String> keep = users.entrySet().stream()
                    .sorted(Map.Entry.<String, UserSnapshot>comparingByValue(
                            Comparator.comparingLong(UserSnapshot::lastSeen)).reversed())
                    .limit(maxUsers)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            users.keySet().retainAll(keep);
        }

        int maxServers = Math.max(current.maxServers, 1);
        if (serversSeen.size() > maxServers * 2L) {
            Set<String> keep = serversSeen.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(maxServers)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            serversSeen.keySet().retainAll(keep);
        }
    }

    private boolean matchesAnyLobby(RegisteredServer server, List<Lobby> lobbies) {
        String serverName = server.getServerInfo().getName();
        return lobbies.stream().anyMatch(lobby -> lobby.filter.matcher(serverName).matches());
    }

    private String userKey(Player player, Settings current) {
        if (current.includeUuid) {
            return player.getUniqueId().toString();
        }
        return player.getUsername() == null ? null : player.getUsername().toLowerCase(Locale.ROOT);
    }

    private static class Settings {
        final boolean enabled;
        final String dumpFile;
        final int dumpIntervalMinutes;
        final int maxUsers;
        final int maxServers;
        final boolean includeUuid;

        Settings() {
            this.enabled = true;
            this.dumpFile = "data-dump.yml";
            this.dumpIntervalMinutes = 10;
            this.maxUsers = 500;
            this.maxServers = 500;
            this.includeUuid = true;
        }

        Settings(Config.DataCollection config) {
            if (config == null) {
                config = new Config.DataCollection();
            }
            this.enabled = config.enabled;
            this.dumpFile = config.dumpFile == null || config.dumpFile.isBlank()
                    ? "data-dump.yml"
                    : config.dumpFile;
            this.dumpIntervalMinutes = config.dumpIntervalMinutes;
            this.maxUsers = config.maxUsers;
            this.maxServers = config.maxServers;
            this.includeUuid = config.includeUuid;
        }
    }

    @org.spongepowered.configurate.objectmapping.ConfigSerializable
    public static class DataDump {
        public int version = 1;
        public String generatedAt;
        public List<String> servers = List.of();
        public List<String> lobbies = List.of();
        public List<UserDump> users = List.of();

        public DataDump() {
        }
    }

    @org.spongepowered.configurate.objectmapping.ConfigSerializable
    public static class UserDump {
        public String name;
        public String uuid;
        public List<String> permissions = List.of();
        public String lastSeen;

        public UserDump() {
        }
    }

    private static class UserSnapshot {
        volatile String name;
        volatile String uuid;
        final Set<String> permissions = ConcurrentHashMap.newKeySet();
        volatile long lastSeen = System.currentTimeMillis();

        UserSnapshot(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        long lastSeen() {
            return lastSeen;
        }

        UserDump toDump(boolean includeUuid) {
            UserDump dump = new UserDump();
            dump.name = name;
            dump.uuid = includeUuid ? uuid : null;
            List<String> perms = new ArrayList<>(permissions);
            perms.sort(String::compareToIgnoreCase);
            dump.permissions = perms;
            dump.lastSeen = Instant.ofEpochMilli(lastSeen).toString();
            return dump;
        }
    }
}
