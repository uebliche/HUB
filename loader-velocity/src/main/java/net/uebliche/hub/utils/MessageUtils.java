package net.uebliche.hub.utils;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Config;
import net.uebliche.hub.config.Lobby;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.uebliche.hub.common.i18n.I18n;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class MessageUtils extends Utils<MessageUtils> {

    private String i18nDefaultLocale = "en_us";
    private boolean i18nUseClientLocale = true;
    private Map<String, Map<String, String>> i18nOverrides = new HashMap<>();

    public enum DebugCategory {
        GENERAL,
        COMMANDS,
        FINDER,
        PINGS,
        COMPASS,
        PERMISSIONS,
        TRANSFER,
        EVENTS,
        PLACEHOLDERS,
        FORCED_HOSTS,
        LAST_LOBBY,
        CONFIG
    }

    public MessageUtils(Hub hub) {
        super(hub);
    }

    public Component toMessage(String message, Object... objects) {
        return miniMessage().deserialize(message, placeholders(objects).toArray(new TagResolver.Single[0]));
    }

    public Component i18n(String key, Player player, Object... resolvers) {
        return i18n(key, player, null, resolvers);
    }

    public Component i18n(String key, Audience audience, String fallback, Object... resolvers) {
        String locale = resolveLocale(audience);
        String raw = resolveRaw(locale, key, fallback);
        return miniMessage().deserialize(raw, placeholders(resolvers).toArray(new TagResolver.Single[0]));
    }

    public Component i18n(String key, Player player, String fallback, Object... resolvers) {
        String locale = playerLocale(player);
        String raw = resolveRaw(locale, key, fallback);
        return miniMessage().deserialize(raw, placeholders(resolvers).toArray(new TagResolver.Single[0]));
    }

    public void sendMessage(Player player, String message, Object... objects) {
        if (message == null || message.isBlank()) {
            return;
        }
        player.sendMessage(toMessage(message, objects));
    }

    public void sendMessage(Player player, Component message) {
        if (message == null) {
            return;
        }
        player.sendMessage(message);
    }

    public List<TagResolver.Single> placeholders(Object... objects) {
        List<TagResolver.Single> placeholders = new ArrayList<>();
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        Config.Placeholder placeholder = configUtils.config().placeholder;
        for (Object object : objects) {
            if (object instanceof TagResolver.Single single) {
                placeholders.add(single);
                continue;
            }
            if (object instanceof RegisteredServer registeredServer) {
                if (placeholder.server.enabled() || placeholder.serverHost.enabled() || placeholder.serverPort.enabled()
                        || placeholder.serverPlayerCount.enabled()
                        || placeholder.serverPlayerPerPlayerUsername.enabled()
                        || placeholder.serverPlayerPerPlayerUuid.enabled()) {
                    ServerInfo serverInfo = registeredServer.getServerInfo();
                    if (placeholder.server.enabled()) {
                        placeholders.add(Placeholder.unparsed(placeholder.server.key(), serverInfo.getName()));
                    }
                    if (placeholder.serverHost.enabled()) {
                        placeholders.add(Placeholder.unparsed(placeholder.serverHost.key(),
                                serverInfo.getAddress().getHostString()));
                    }
                    if (placeholder.serverPort.enabled()) {
                        placeholders.add(Placeholder.unparsed(placeholder.serverPort.key(),
                                String.valueOf(serverInfo.getAddress().getPort())));
                    }
                    if (placeholder.serverPlayerCount.enabled()) {
                        int playerCount = registeredServer.getPlayersConnected().size();
                        placeholders.add(
                                Placeholder.unparsed(placeholder.serverPlayerCount.key(), String.valueOf(playerCount)));
                        broadcastDebugMessage(DebugCategory.PLACEHOLDERS,
                                "<gray>👥 Server " + serverInfo.getName() + " currently tracks "
                                        + playerCount + " players.</gray>");
                    }
                    if (placeholder.serverPlayerPerPlayerUsername.enabled()
                            || placeholder.serverPlayerPerPlayerUuid.enabled()) {
                        AtomicInteger i = new AtomicInteger(0);
                        registeredServer.getPlayersConnected().forEach(player -> {
                            int id = i.getAndIncrement();
                            if (placeholder.serverPlayerPerPlayerUsername.enabled()) {
                                placeholders
                                        .add(Placeholder
                                                .unparsed(
                                                        placeholder.serverPlayerPerPlayerUsername.key()
                                                                .replaceFirst(placeholder.serverPlayerPerPlayerUsername
                                                                        .placeholder(), String.valueOf(id)),
                                                        player.getUsername()));
                            }
                            if (placeholder.serverPlayerPerPlayerUuid.enabled()) {
                                placeholders
                                        .add(Placeholder
                                                .unparsed(
                                                        placeholder.serverPlayerPerPlayerUuid.key()
                                                                .replaceFirst(placeholder.serverPlayerPerPlayerUuid
                                                                        .placeholder(), String.valueOf(id)),
                                                        player.getUniqueId().toString()));
                            }
                        });
                    }
                }
            }
            if (object instanceof Lobby lobby) {
                if (placeholder.lobby.enabled()) {
                    placeholders.add(Placeholder.unparsed(placeholder.lobby.key(), lobby.name));
                }
                if (placeholder.lobbyFilter.enabled()) {
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyFilter.key(), lobby.filter.toString()));
                }
                if (placeholder.lobbyPermission.enabled()) {
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyPermission.key(), lobby.permission));
                }
                if (placeholder.lobbyPriority.enabled()) {
                    placeholders
                            .add(Placeholder.unparsed(placeholder.lobbyPriority.key(), String.valueOf(lobby.priority)));
                }
                if (placeholder.lobbyCommandPerCommandStandalone.enabled()
                        || placeholder.lobbyCommandPerCommandSubcommand.enabled()
                        || placeholder.lobbyCommandPerCommandHideOn.enabled()) {
                    lobby.commands.forEach((s, command) -> {
                        if (placeholder.lobbyCommandPerCommandStandalone.enabled()) {
                            placeholders
                                    .add(Placeholder
                                            .unparsed(
                                                    placeholder.lobbyCommandPerCommandStandalone.key()
                                                            .replaceFirst(placeholder.lobbyCommandPerCommandStandalone
                                                                    .placeholder(), s),
                                                    command.standalone ? "true" : "false"));
                        }
                        if (placeholder.lobbyCommandPerCommandSubcommand.enabled()) {
                            placeholders
                                    .add(Placeholder
                                            .unparsed(
                                                    placeholder.lobbyCommandPerCommandSubcommand.key()
                                                            .replaceFirst(placeholder.lobbyCommandPerCommandSubcommand
                                                                    .placeholder(), s),
                                                    command.subcommand ? "true" : "false"));
                        }
                        if (placeholder.lobbyCommandPerCommandHideOn.enabled()) {
                            placeholders.add(Placeholder.unparsed(
                                    placeholder.lobbyCommandPerCommandHideOn.key()
                                            .replaceFirst(placeholder.lobbyCommandPerCommandHideOn.placeholder(), s),
                                    command.hideOn().toString()));
                        }
                    });
                }
                if (placeholder.lobbyAutojoin.enabled()) {
                    placeholders
                            .add(Placeholder.unparsed(placeholder.lobbyAutojoin.key(), String.valueOf(lobby.autojoin)));
                }
            }
            if (object instanceof Player player) {
                if (placeholder.player.enabled()) {
                    placeholders.add(Placeholder.unparsed(placeholder.player.key(), player.getUsername()));
                }
                if (placeholder.playerUuid.enabled()) {
                    placeholders
                            .add(Placeholder.unparsed(placeholder.playerUuid.key(), player.getUniqueId().toString()));
                }
            }
        }
        return placeholders;
    }

    private String playerLocale(Player player) {
        if (!i18nUseClientLocale) {
            return i18nDefaultLocale;
        }
        Locale loc = player != null ? player.getEffectiveLocale() : null;
        if (loc == null) {
            return i18nDefaultLocale;
        }
        return I18n.normalizeLocale(loc.toLanguageTag());
    }

    private String resolveLocale(Audience audience) {
        if (audience instanceof Player player) {
            return playerLocale(player);
        }
        return i18nDefaultLocale;
    }

    private String resolveRaw(String locale, String key, String fallback) {
        String normalized = I18n.normalizeLocale(locale);
        Map<String, String> overrides = i18nOverrides.get(normalized);
        if (overrides != null && overrides.containsKey(key)) {
            return overrides.get(key);
        }
        String raw = I18n.raw(normalized, key);
        if (raw != null && !raw.equals("<red>" + key)) {
            return raw;
        }
        return fallback == null ? raw : fallback;
    }

    public void reloadI18n(Config config, Path dataDirectory) {
        if (config == null) {
            return;
        }
        i18nDefaultLocale = I18n.normalizeLocale(config.i18n.defaultLocale);
        i18nUseClientLocale = config.i18n.useClientLocale;
        I18n.reloadFromClasspath("en_us");
        I18n.reloadFromClasspath("de_de");
        Map<String, Map<String, String>> merged = new HashMap<>();
        mergeOverrides(merged, loadI18nFiles(dataDirectory == null ? null : dataDirectory.resolve("i18n")));
        mergeOverrides(merged, config.i18n.overrides);
        i18nOverrides = merged;
    }

    private Map<String, Map<String, String>> loadI18nFiles(Path dir) {
        Map<String, Map<String, String>> result = new HashMap<>();
        if (dir == null || Files.notExists(dir)) {
            return result;
        }
        try {
            Files.createDirectories(dir);
            try (var stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    int dot = fileName.lastIndexOf('.');
                    if (dot <= 0) {
                        return;
                    }
                    String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
                    String locale = fileName.substring(0, dot);
                    Map<String, String> entries = switch (ext) {
                        case "yml", "yaml" -> readYamlLocale(path);
                        case "json" -> readJsonLocale(path);
                        default -> Map.of();
                    };
                    if (!entries.isEmpty()) {
                        result.put(I18n.normalizeLocale(locale), entries);
                    }
                });
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private Map<String, String> readYamlLocale(Path path) {
        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
            ConfigurationNode node = loader.load();
            Map<String, String> entries = new HashMap<>();
            flattenNode(node, "", entries);
            return entries;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Map<String, String> readJsonLocale(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> entries = new Gson().fromJson(reader, type);
            return entries == null ? Map.of() : entries;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private void flattenNode(ConfigurationNode node, String prefix, Map<String, String> out) {
        if (node == null) {
            return;
        }
        if (node.isList()) {
            List<String> lines = new ArrayList<>();
            for (ConfigurationNode child : node.childrenList()) {
                String line = child.getString();
                if (line != null) {
                    lines.add(line);
                }
            }
            if (!lines.isEmpty() && !prefix.isBlank()) {
                out.put(prefix, String.join("\n", lines));
            }
            return;
        }
        if (!node.isMap()) {
            String value = node.getString();
            if (value != null && !prefix.isBlank()) {
                out.put(prefix, value);
            }
            return;
        }
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String key = String.valueOf(entry.getKey());
            String next = prefix.isBlank() ? key : prefix + "." + key;
            flattenNode(entry.getValue(), next, out);
        }
    }

    private void mergeOverrides(Map<String, Map<String, String>> target, Map<String, Map<String, String>> source) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String locale = I18n.normalizeLocale(entry.getKey());
            Map<String, String> existing = target.computeIfAbsent(locale, k -> new HashMap<>());
            existing.putAll(entry.getValue());
        }
    }

    public void sendDebugMessage(Audience recipient, String message) {
        sendDebugMessage(DebugCategory.GENERAL, recipient, miniMessage().deserialize(message));
    }

    public void sendDebugMessage(Audience recipient, Component message) {
        sendDebugMessage(DebugCategory.GENERAL, recipient, message);
    }

    public void sendDebugMessage(DebugCategory category, Audience recipient, String message) {
        sendDebugMessage(category, recipient, miniMessage().deserialize(message));
    }

    public void sendDebugMessage(DebugCategory category, Audience recipient, Component message) {
        if (!isDebugEnabled(category)) {
            return;
        }
        boolean canSend = ((recipient instanceof Player player) && Utils.util(PlayerUtils.class).canDebug(player))
                || recipient instanceof ConsoleCommandSource;
        if (canSend) {
            recipient.sendMessage(toDebugMessage(message));
        }
        broadcastDebugMessage(category, Component.text(recipient.toString()).append(message));
    }

    public void sendDebugCommandMessage(Audience recipient, String message) {
        sendDebugCommandMessage(recipient, miniMessage().deserialize(message));
    }

    public void sendDebugCommandMessage(Audience recipient, Component message) {
        recipient.sendMessage(toDebugMessage(message));
    }

    private Component toDebugMessage(Component message) {
        return Component.empty()
                .append(Component.text("[Debug]: ").style(Style.style(TextDecoration.BOLD, NamedTextColor.YELLOW)))
                .append(message);
    }

    public void broadcastDebugMessage(String message) {
        broadcastDebugMessage(DebugCategory.GENERAL, miniMessage().deserialize(message));
    }

    public void broadcastDebugMessage(Component message) {
        broadcastDebugMessage(DebugCategory.GENERAL, message);
    }

    public void broadcastDebugMessage(DebugCategory category, String message) {
        broadcastDebugMessage(category, miniMessage().deserialize(message));
    }

    public void broadcastDebugMessage(DebugCategory category, Component message) {
        if (!isDebugEnabled(category)) {
            return;
        }
        hub.server()
                .filterAudience(audience -> (audience instanceof Player player)
                        && Utils.util(PlayerUtils.class).canDebug(player) || audience instanceof ConsoleCommandSource)
                .sendMessage(toDebugMessage(message));
    }

    private boolean isDebugEnabled(DebugCategory category) {
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        if (configUtils == null || configUtils.config() == null) {
            return false;
        }
        var debug = configUtils.config().debug;
        if (debug == null || !debug.enabled) {
            return false;
        }
        var categories = debug.categories;
        if (categories == null) {
            return category != DebugCategory.PINGS;
        }
        if (category == null) {
            return true;
        }
        return switch (category) {
            case GENERAL -> categories.general;
            case COMMANDS -> categories.commands;
            case FINDER -> categories.finder;
            case PINGS -> categories.pings;
            case COMPASS -> categories.compass;
            case PERMISSIONS -> categories.permissions;
            case TRANSFER -> categories.transfer;
            case EVENTS -> categories.events;
            case PLACEHOLDERS -> categories.placeholders;
            case FORCED_HOSTS -> categories.forcedHosts;
            case LAST_LOBBY -> categories.lastLobby;
            case CONFIG -> categories.config;
        };
    }
}
