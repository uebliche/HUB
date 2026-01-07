package net.uebliche.hub.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Lobby;
import net.uebliche.hub.utils.*;
import net.uebliche.hub.utils.MessageUtils.DebugCategory;

import net.uebliche.hub.config.Config;
import net.uebliche.hub.data.PingResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DebugCommand {

    private final Hub hub;


    protected ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DebugCommand(Hub hub) {
        this.hub = hub;
    }

    public LiteralArgumentBuilder<CommandSource> create() {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        PlayerUtils playerUtils = Utils.util(PlayerUtils.class);
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        var config = configUtils.config();
        return BrigadierCommand.literalArgumentBuilder("debug")
                .requires(commandSource -> commandSource instanceof Player player && playerUtils.canDebug(player)
                        || commandSource instanceof ConsoleCommandSource)
                .then(
                        BrigadierCommand.literalArgumentBuilder("disable")
                                .requires(commandSource -> config.debug.enabled)
                                .requires(commandSource -> commandSource instanceof Player player)
                                .executes(commandContext -> {
                                    configUtils.debug(commandContext.getSource(), false);
                                    return 1;
                                }))
                .then(
                        BrigadierCommand.literalArgumentBuilder("enable")
                                .requires(commandSource -> !config.debug.enabled)
                                .requires(commandSource -> commandSource instanceof Player player)
                                .executes(commandContext -> {
                                    configUtils.debug(commandContext.getSource(), true);
                                    return 1;
                                }))
                .then(
                        BrigadierCommand.literalArgumentBuilder("reload")
                                .executes(commandContext -> {
                                    executor.execute(() -> {
                                        configUtils.reload(commandContext.getSource());
                                    });
                                    return 1;
                                }))
                .then(
                        BrigadierCommand.literalArgumentBuilder("messages")
                                .requires(commandSource -> config != null && config.messages != null)
                                .requires(commandSource -> (commandSource instanceof Player player && Utils.util(PlayerUtils.class).canDebug(player)) || commandSource instanceof ConsoleCommandSource)
                                .executes(commandContext -> {
                                    var sender = commandContext.getSource();
                                    messageUtils.sendDebugCommandMessage(sender, "🤖 Testing Messages");
                                    messageUtils.sendDebugCommandMessage(sender, messageUtils.toMessage(config.systemMessages.playersOnlyCommandMessage));
                                    config.lobbies.forEach(lobby -> {
                                        messageUtils.sendDebugMessage(DebugCategory.COMMANDS, sender, "🤖 Testing Messages for Lobby: " + lobby.name);
                                        RegisteredServer registeredServer = hub.server().getAllServers().stream().filter(registeredServer1 -> lobby.filter.matcher(registeredServer1.getServerInfo().getName()).matches()).findFirst().orElse(null);
                                        if (registeredServer == null) {
                                            messageUtils.sendDebugMessage(DebugCategory.COMMANDS, sender, "❌ No Server found!");
                                        } else {
                                            messageUtils.sendDebugCommandMessage(sender, messageUtils.toMessage(lobby.messages().successMessage == null ? config.messages.successMessage : lobby.messages().successMessage, registeredServer, lobby));
                                            messageUtils.sendDebugCommandMessage(sender, messageUtils.toMessage(lobby.messages().alreadyConnectedMessage == null ? config.messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, registeredServer, lobby));
                                            messageUtils.sendDebugCommandMessage(sender, messageUtils.toMessage(lobby.messages().connectionInProgressMessage == null ? config.messages.connectionInProgressMessage : lobby.messages().connectionInProgressMessage, registeredServer, lobby));
                                            messageUtils.sendDebugCommandMessage(sender, messageUtils.toMessage(lobby.messages().serverDisconnectedMessage == null ? config.messages.serverDisconnectedMessage : lobby.messages().serverDisconnectedMessage, registeredServer, lobby));
                                            messageUtils.sendDebugCommandMessage(sender, messageUtils.toMessage(lobby.messages().connectionCancelledMessage == null ? config.messages.connectionCancelledMessage : lobby.messages().connectionCancelledMessage, registeredServer, lobby));
                                        }
                                    });
                                    return 1;
                                }))
                .then(
                        BrigadierCommand.literalArgumentBuilder("placeholders")
                                .requires(commandSource -> (commandSource instanceof Player player && Utils.util(PlayerUtils.class).canDebug(player)) || commandSource instanceof ConsoleCommandSource)
                                .requires(commandSource -> config != null && config.messages != null && config.placeholder != null)
                                .then(BrigadierCommand.requiredArgumentBuilder("lobby", StringArgumentType.word())
                                        .suggests((commandContext, suggestionsBuilder) -> {
                                            config.lobbies.forEach(lobby -> {
                                                suggestionsBuilder.suggest(lobby.name);
                                            });
                                            return suggestionsBuilder.buildFuture();
                                        }).then(BrigadierCommand.requiredArgumentBuilder("server", StringArgumentType.word())
                                                .suggests((commandContext, suggestionsBuilder) -> {
                                                    hub.server().getAllServers().forEach(registeredServer -> {
                                                        suggestionsBuilder.suggest(registeredServer.getServerInfo().getName());
                                                    });
                                                    return suggestionsBuilder.buildFuture();
                                                })
                                                .executes(commandContext -> {
                                                    Player player = (Player) commandContext.getSource();
                                                    String lobbyArg = commandContext.getArgument("lobby", String.class);
                                                    String serverArg = commandContext.getArgument("server", String.class);
                                                    Lobby lobby = config.lobbies.stream().filter(l -> l.name.equalsIgnoreCase(lobbyArg)).findFirst().orElse(null);
                                                    if (lobby == null) {
                                                        messageUtils.sendDebugCommandMessage(player, "❌ Lobby " + lobbyArg + " not found!");
                                                        return 1;
                                                    }
                                                    RegisteredServer registeredServer = hub.server().getServer(serverArg).orElse(null);
                                                    if (registeredServer == null) {
                                                        messageUtils.sendDebugCommandMessage(player, "❌ Server " + serverArg + " not found!");
                                                        return 1;
                                                    }
                                                    messageUtils.placeholders(player, lobby, registeredServer).forEach(tagResolver -> {
                                                        messageUtils.sendDebugCommandMessage(player, messageUtils.toMessage(tagResolver.key() + ": <" + tagResolver.key() + ">", player, lobby, registeredServer));
                                                    });

                                                    return 1;
                                                })
                                        )
                                )
                )
                .then(BrigadierCommand.literalArgumentBuilder("getLobbies")
                        .requires(commandSource -> (commandSource instanceof Player player && Utils.util(PlayerUtils.class).canDebug(player)) || commandSource instanceof ConsoleCommandSource)
                        .requires(commandSource -> config != null && config.messages != null && config.placeholder != null)
                        .then(BrigadierCommand.requiredArgumentBuilder("lobby", StringArgumentType.word())
                                .suggests((commandContext, suggestionsBuilder) -> {
                                    config.lobbies.forEach(lobby -> {
                                        suggestionsBuilder.suggest(lobby.name);
                                    });
                                    return suggestionsBuilder.buildFuture();
                                })
                                .executes(commandContext -> {
                                    Player player = (Player) commandContext.getSource();
                                    String lobbyArg = commandContext.getArgument("lobby", String.class);
                                    Lobby lobby = config.lobbies.stream().filter(l -> l.name.equalsIgnoreCase(lobbyArg)).findFirst().orElse(null);
                                    if (lobby == null) {
                                        messageUtils.sendDebugCommandMessage(player, "❌ Lobby " + lobbyArg + " not found!");
                                        return 1;
                                    }
                                    var lobbyUtils = Utils.util(LobbyUtils.class);
                                    messageUtils.sendDebugCommandMessage(player, "<gray>🔄 Refreshing lobby cache...</gray>");
                                    executor.execute(() -> {
                                        try {
                                            lobbyUtils.refreshNow().join();
                                            var results = lobbyUtils.getCachedResults(player, lobby);
                                            if (results.isEmpty()) {
                                                messageUtils.sendDebugCommandMessage(player, "<red>❌ No cached ping data for " + lobby.name + ".");
                                                return;
                                            }
                                            results.forEach(snapshot -> {
                                                var ping = snapshot.result();
                                                var serverName = ping.server().getServerInfo().getName();
                                                var online = ping.players().getOnline();
                                                var max = ping.players().getMax();
                                                var latency = ping.latency();
                                                var age = snapshot.ageMillis();
                                                messageUtils.sendDebugCommandMessage(player,
                                                        "<gray>🤖 " + serverName + "</gray> <gold>" + online + "</gold>/<gold>" + max
                                                                + "</gold> players, latency <aqua>" + latency + "ms</aqua>, cached <yellow>" + age + "ms</yellow> ago.");
                                            });
                                        } catch (Exception ignored) {
                                            messageUtils.sendDebugCommandMessage(player, "<red>❌ Failed to refresh lobby cache.</red>");
                                        }
                                    });
                                    return 1;
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("data-dump")
                        .requires(commandSource -> (commandSource instanceof Player player && Utils.util(PlayerUtils.class).canDebug(player))
                                || commandSource instanceof ConsoleCommandSource)
                        .executes(commandContext -> {
                            var dataCollector = Utils.util(DataCollector.class);
                            if (dataCollector == null) {
                                messageUtils.sendDebugCommandMessage(commandContext.getSource(), "❌ Data collection is not available.");
                                return 0;
                            }
                            dataCollector.dumpNow();
                            messageUtils.sendDebugCommandMessage(commandContext.getSource(), "<green>✔ Data dump written.</green>");
                            return 1;
                        }))
                .then(BrigadierCommand.literalArgumentBuilder("predict")
                        .requires(commandSource -> (commandSource instanceof Player player && Utils.util(PlayerUtils.class).canDebug(player))
                                || commandSource instanceof ConsoleCommandSource)
                        .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    hub.server().getAllPlayers().forEach(player -> builder.suggest(player.getUsername()));
                                    return builder.buildFuture();
                                })
                                .executes(commandContext -> {
                                    executor.execute(() -> runPrediction(commandContext.getSource(),
                                            commandContext.getArgument("player", String.class)));
                                    return 1;
                                })
                        )
                );
    }

    private void runPrediction(CommandSource sender, String playerName) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        LobbyUtils lobbyUtils = Utils.util(LobbyUtils.class);
        PlayerUtils playerUtils = Utils.util(PlayerUtils.class);
        if (configUtils == null || lobbyUtils == null || messageUtils == null || playerUtils == null) {
            return;
        }
        var config = configUtils.config();
        if (config == null) {
            messageUtils.sendDebugCommandMessage(sender, "<red>❌ Config not loaded.</red>");
            return;
        }
        var target = hub.server().getPlayer(playerName).orElse(null);
        if (target == null) {
            messageUtils.sendDebugCommandMessage(sender, "<red>❌ Player not found: " + playerName + "</red>");
            return;
        }
        messageUtils.sendDebugCommandMessage(sender, "<gray>Prediction for <yellow>" + target.getUsername() + "</yellow>:</gray>");

        Map<String, String> predictions = new LinkedHashMap<>();
        predictions.put("kick", predictKick(target, config, lobbyUtils));

        var baseResult = predictBaseHub(target, config, lobbyUtils, playerUtils);
        String baseKey = config.baseHubCommand;
        if (baseResult.parentLobby != null) {
            baseKey = config.baseHubCommand + " (parent: " + baseResult.parentLobby.name + ")";
        }
        predictions.put(baseKey, formatPrediction(config, baseResult.pingResult, baseResult.messageOverride));

        config.lobbies.forEach(lobby -> lobby.commands.forEach((commandName, command) -> {
            if (command.subcommand) {
                String key = config.baseHubCommand + " " + commandName;
                predictions.put(key, predictLobbyCommand(target, config, lobbyUtils, playerUtils, lobby));
            }
            if (command.standalone) {
                String key = "/" + commandName;
                predictions.put(key, predictLobbyCommand(target, config, lobbyUtils, playerUtils, lobby));
            }
        }));

        predictions.forEach((key, value) ->
                messageUtils.sendDebugCommandMessage(sender, "<gray>-</gray> <aqua>" + key + "</aqua> <dark_gray>-></dark_gray> " + value));
    }

    private String predictKick(Player player, Config config, LobbyUtils lobbyUtils) {
        if (!config.autoSelect.onServerKick) {
            return "<red>disconnect</red>";
        }
        var excluded = player.getCurrentServer()
                .map(server -> Set.of(server.getServerInfo().getName()))
                .orElse(Set.of());
        Optional<PingResult> result = lobbyUtils.findBest(player, excluded);
        return formatPrediction(config, result.orElse(null), null);
    }

    private PredictionResult predictBaseHub(Player player, Config config, LobbyUtils lobbyUtils, PlayerUtils playerUtils) {
        var currentServer = player.getCurrentServer().orElse(null);
        if (currentServer == null) {
            return new PredictionResult(null, null, "<yellow>no-server</yellow>");
        }
        var parentResult = predictParentRedirect(player, currentServer.getServerInfo().getName(), config, lobbyUtils, playerUtils);
        if (parentResult.isPresent()) {
            return parentResult.get();
        }
        Optional<PingResult> result = lobbyUtils.findBest(player);
        return new PredictionResult(result.orElse(null), null, null);
    }

    private Optional<PredictionResult> predictParentRedirect(Player player, String currentServerName, Config config,
                                                            LobbyUtils lobbyUtils, PlayerUtils playerUtils) {
        if (currentServerName == null || currentServerName.isBlank()) {
            return Optional.empty();
        }
        Lobby currentLobby = config.lobbies.stream()
                .filter(lobby -> lobby.filter.matcher(currentServerName).matches())
                .findFirst()
                .orElse(null);
        if (currentLobby == null) {
            return Optional.empty();
        }
        var parentNames = new ArrayList<String>();
        if (currentLobby.parent != null && !currentLobby.parent.isBlank()) {
            parentNames.add(currentLobby.parent);
        }
        if (currentLobby.parentGroups != null) {
            parentNames.addAll(currentLobby.parentGroups);
        }
        var orderedParents = new LinkedHashSet<String>();
        parentNames.forEach(name -> {
            if (name != null && !name.isBlank()) {
                orderedParents.add(name);
            }
        });
        if (orderedParents.isEmpty()) {
            return Optional.empty();
        }

        var resolver = new ParentTargetResolver(config);
        var visitedParentLobbies = new LinkedHashSet<String>();
        for (String parentName : orderedParents) {
            if (parentName.equalsIgnoreCase(currentLobby.name)) {
                continue;
            }
            var candidateLobbies = resolver.resolve(parentName);
            if (candidateLobbies.isEmpty()) {
                continue;
            }
            for (Lobby parentLobby : candidateLobbies) {
                var key = parentLobby.name.toLowerCase(Locale.ROOT);
                if (!visitedParentLobbies.add(key)) {
                    continue;
                }
                if (!playerUtils.permissionCheck(player, parentLobby)) {
                    continue;
                }
                var result = lobbyUtils.findBestForLobby(player, parentLobby, Set.of(currentServerName));
                if (result.isPresent()) {
                    return Optional.of(new PredictionResult(result.get(), parentLobby, null));
                }
            }
        }
        return Optional.empty();
    }

    private String predictLobbyCommand(Player player, Config config, LobbyUtils lobbyUtils, PlayerUtils playerUtils, Lobby lobby) {
        if (!playerUtils.permissionCheck(player, lobby)) {
            return "<red>denied</red>";
        }
        var currentServer = player.getCurrentServer().orElse(null);
        if (currentServer != null && lobby.filter.matcher(currentServer.getServerInfo().getName()).matches()) {
            return formatServerTarget(config, currentServer.getServerInfo().getName(), lobby);
        }
        Optional<PingResult> result = lobbyUtils.findBestForLobby(player, lobby);
        return formatPrediction(config, result.orElse(null), null);
    }

    private String formatPrediction(Config config, PingResult result, String override) {
        if (override != null && !override.isBlank()) {
            return override;
        }
        if (result == null) {
            return "<red>none</red>";
        }
        return formatServerTarget(config, result.server().getServerInfo().getName(), result.lobby());
    }

    private String formatServerTarget(Config config, String serverName, Lobby lobby) {
        if (serverName == null || serverName.isBlank()) {
            return "<red>none</red>";
        }
        String group = groupForLobby(config, lobby);
        return "<green>" + serverName + "</green> <gray>(" + group + ")</gray>";
    }

    private String groupForLobby(Config config, Lobby lobby) {
        if (config == null || lobby == null) {
            return "unknown";
        }
        if (config.lobbyGroups != null) {
            for (var group : config.lobbyGroups) {
                if (group == null || group.lobbies == null) {
                    continue;
                }
                for (String lobbyName : group.lobbies) {
                    if (lobbyName != null && lobbyName.equalsIgnoreCase(lobby.name)) {
                        return group.name == null || group.name.isBlank() ? lobby.name : group.name;
                    }
                }
            }
        }
        return lobby.name == null || lobby.name.isBlank() ? "unknown" : lobby.name;
    }

    private record PredictionResult(PingResult pingResult, Lobby parentLobby, String messageOverride) {
    }

    private static final class ParentTargetResolver {
        private final Map<String, Lobby> lobbyByName = new LinkedHashMap<>();
        private final Map<String, Config.LobbyGroup> groupByName = new LinkedHashMap<>();
        private final Map<String, List<String>> childrenByGroup = new LinkedHashMap<>();

        private ParentTargetResolver(Config config) {
            if (config == null) {
                return;
            }
            if (config.lobbies != null) {
                config.lobbies.forEach(lobby -> {
                    var key = normalizeName(lobby.name);
                    if (!key.isBlank()) {
                        lobbyByName.put(key, lobby);
                    }
                });
            }
            if (config.lobbyGroups != null) {
                config.lobbyGroups.forEach(group -> {
                    var key = normalizeName(group.name);
                    if (!key.isBlank()) {
                        groupByName.put(key, group);
                    }
                });
            }
            groupByName.values().forEach(group -> {
                var nameKey = normalizeName(group.name);
                var parentKey = normalizeName(group.parentGroup);
                if (nameKey.isBlank() || parentKey.isBlank() || parentKey.equals(nameKey)) {
                    return;
                }
                if (!groupByName.containsKey(parentKey)) {
                    return;
                }
                childrenByGroup.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(nameKey);
            });
        }

        private static String normalizeName(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }

        private List<Lobby> resolve(String parentName) {
            var key = normalizeName(parentName);
            if (key.isBlank()) {
                return List.of();
            }
            var lobby = lobbyByName.get(key);
            if (lobby != null) {
                return List.of(lobby);
            }
            var group = groupByName.get(key);
            if (group == null) {
                return List.of();
            }
            var results = new ArrayList<Lobby>();
            collectGroupLobbies(key, new LinkedHashSet<>(), new LinkedHashSet<>(), results);
            return results;
        }

        private void collectGroupLobbies(String groupKey, Set<String> visitedGroups, Set<String> visitedLobbies, List<Lobby> out) {
            if (!visitedGroups.add(groupKey)) {
                return;
            }
            var group = groupByName.get(groupKey);
            if (group == null) {
                return;
            }
            var entries = group.lobbies != null ? group.lobbies : List.<String>of();
            for (String lobbyName : entries) {
                var lobbyKey = normalizeName(lobbyName);
                if (lobbyKey.isBlank()) {
                    continue;
                }
                var lobby = lobbyByName.get(lobbyKey);
                if (lobby == null) {
                    continue;
                }
                if (visitedLobbies.add(lobby.name.toLowerCase(Locale.ROOT))) {
                    out.add(lobby);
                }
            }
            var children = childrenByGroup.getOrDefault(groupKey, List.of());
            for (String childKey : children) {
                collectGroupLobbies(childKey, visitedGroups, visitedLobbies, out);
            }
        }
    }

}
