package net.uebliche.hub.commands;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Lobby;
import net.uebliche.hub.utils.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class HubCommand {

    private final Hub hub;
    protected ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public HubCommand(Hub hub) {
        this.hub = hub;
    }

    public void create() {
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        var base = BrigadierCommand.literalArgumentBuilder(configUtils.config().baseHubCommand)
                .requires(commandSource -> (
                        commandSource instanceof ConsoleCommandSource
                                || (
                                commandSource instanceof Player player &&
                                        player.getCurrentServer().isPresent() &&
                                        !configUtils.config().hideHubCommandOnLobby.matcher(player.getCurrentServer().get().getServerInfo().getName()).matches()
                        ))
                )
                .executes(commandContext -> {
                    messageUtils.sendDebugMessage(commandContext.getSource(), "🤖 Executing Hub Command!");
                    return execute(commandContext);
                })
                .then(new DebugCommand(hub).create());
        configUtils.config().lobbies.forEach(lobby -> {
            lobby.commands.forEach((s, command) -> {
                if (command.subcommand) {
                    base.then(
                            BrigadierCommand.literalArgumentBuilder(s)
                                    .requires(source -> (source instanceof Player player) && (lobby.permission.isBlank() || player.hasPermission(lobby.permission)))
                                    // .requires(commandSource -> !command.hidden)
                                    .executes(commandContext -> {
                                        messageUtils.sendDebugMessage(commandContext.getSource(), "🤖 Executing Hub Command for Lobby: " + s + "!");
                                        return execute(commandContext, lobby);
                                    }));
                }
                if (command.standalone)
                    Utils.util(CommandUtils.class).registerCommand(hub.server().getCommandManager()
                                    .metaBuilder(s)
                                    .plugin(hub)
                                    .build(),
                            new BrigadierCommand(
                                    BrigadierCommand.literalArgumentBuilder(s)
                                            .requires(source -> (source instanceof Player player) && (player.hasPermission(lobby.permission) || lobby.permission.isBlank()))
                                            .executes(commandContext -> {
                                                messageUtils.broadcastDebugMessage("🤖 " + commandContext.getSource().toString() + " -> Executing Hub Command for Lobby: " + s + "!");
                                                return execute(commandContext, lobby);
                                            })
                            )
                    );
            });
        });

        hub.server().getCommandManager().register(
                hub.server().getCommandManager()
                        .metaBuilder(configUtils.config().baseHubCommand)
                        .aliases(configUtils.config().aliases.toArray(new String[0]))
                        .plugin(hub)
                        .build(),
                new BrigadierCommand(base.build())
        );

    }

    private int execute(CommandContext<CommandSource> commandContext) {
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        messageUtils.broadcastDebugMessage("🤖 " + commandContext.getSource().toString() + " ->  Executing Hub Command");
        Player player = commandContext.getSource() instanceof Player ? (Player) commandContext.getSource() : null;
        if (player == null) {
            commandContext.getSource().sendMessage(messageUtils.toMessage(configUtils.config().systemMessages.playersOnlyCommandMessage));
            return 0;
        }
        if (!player.getCurrentServer().isPresent()) {
            messageUtils.sendMessage(player, "<red>❌ User is on no Server!");
            return 0;
        }
        if (tryRedirectToParentHub(player)) {
            return 1;
        }
        var lobbyUtils = Utils.util(LobbyUtils.class);
        var pingResult = lobbyUtils.findBest(player);
        if (pingResult.isEmpty()) {
            messageUtils.sendMessage(player, configUtils.config().systemMessages.noLobbyFoundMessage, player);
            messageUtils.sendDebugMessage(player, "<red>❌ No Server found!");
            return 0;
        }
        pingResult.get().connect();
//        Optional.ofNullable(LobbyUtils.util(LobbyUtils.class).findBest(player)).ifPresentOrElse(pingResult -> {
//            player.createConnectionRequest(pingResult.server()).connect().thenAccept(connection -> {
//                if (connection.isSuccessful()) {
//                    messageUtils.sendDebugMessage(player, "<green>✔ Connection successful!");
//                    messageUtils.sendMessage(player, pingResult.lobby().messages().successMessage == null ? configUtils.config().messages.successMessage : pingResult.lobby().messages().successMessage, pingResult.lobby(), player);
//                } else {
//                    messageUtils.sendDebugMessage(player, "<red>❌ Connection failed!");
//                }
//            });
//        }, () -> {
//            messageUtils.sendMessage(player, configUtils.config().systemMessages.noLobbyFoundMessage, player);
//            messageUtils.sendDebugMessage(player, "<red>❌ No Server found!");
//        });
        return 1;
    }

    private boolean tryRedirectToParentHub(Player player) {
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        LobbyUtils lobbyUtils = Utils.util(LobbyUtils.class);
        PlayerUtils playerUtils = Utils.util(PlayerUtils.class);

        if (configUtils == null || messageUtils == null || lobbyUtils == null || playerUtils == null) {
            return false;
        }

        var currentServerOpt = player.getCurrentServer();
        if (currentServerOpt.isEmpty()) {
            return false;
        }

        var currentServerName = currentServerOpt.get().getServerInfo().getName();
        Optional<Lobby> currentLobbyOpt = configUtils.config().lobbies.stream()
                .filter(lobby -> lobby.filter.matcher(currentServerName).matches())
                .findFirst();
        if (currentLobbyOpt.isEmpty()) {
            return false;
        }

        var currentLobby = currentLobbyOpt.get();
        var parentNames = new ArrayList<String>();
        if (currentLobby.parent != null && !currentLobby.parent.isBlank()) {
            parentNames.add(currentLobby.parent);
        }
        if (currentLobby.parentGroups != null) {
            parentNames.addAll(currentLobby.parentGroups);
        }

        var orderedUniqueParents = new LinkedHashSet<String>();
        parentNames.forEach(name -> {
            if (name != null && !name.isBlank()) {
                orderedUniqueParents.add(name);
            }
        });

        if (orderedUniqueParents.isEmpty()) {
            return false;
        }

        boolean attempted = false;
        var resolver = new ParentTargetResolver(configUtils.config(), messageUtils, player);
        var visitedParentLobbies = new LinkedHashSet<String>();
        for (String parentName : orderedUniqueParents) {
            if (parentName.equalsIgnoreCase(currentLobby.name)) {
                messageUtils.sendDebugMessage(player, "<yellow>⚠️ Parent lobby for " + currentLobby.name + " points to itself; skipping.");
                continue;
            }

            var candidateLobbies = resolver.resolve(parentName);
            if (candidateLobbies.isEmpty()) {
                messageUtils.sendDebugMessage(player, "<yellow>⚠️ Parent lobby or group '" + parentName + "' not found in config; skipping.</yellow>");
                continue;
            }

            for (Lobby parentLobby : candidateLobbies) {
                var key = parentLobby.name.toLowerCase(Locale.ROOT);
                if (!visitedParentLobbies.add(key)) {
                    continue;
                }
                if (!playerUtils.permissionCheck(player, parentLobby)) {
                    messageUtils.sendDebugMessage(player, "<red>❌ Player lacks permission for parent lobby " + parentLobby.name + ".");
                    continue;
                }

                attempted = true;
                var target = lobbyUtils.findBestForLobby(player, parentLobby, Set.of(currentServerName));
                if (target.isEmpty()) {
                    messageUtils.sendDebugMessage(player, "<red>❌ No available server for parent lobby " + parentLobby.name + ".");
                    continue;
                }

                messageUtils.sendDebugMessage(player, "<green>⬆ Sending player to parent lobby " + parentLobby.name + ".");
                target.get().connect();
                return true;
            }
        }

        if (attempted) {
            messageUtils.sendMessage(player, configUtils.config().systemMessages.noLobbyFoundMessage, player);
        }
        return attempted;
    }

    private static final class ParentTargetResolver {
        private final Map<String, Lobby> lobbyByName = new LinkedHashMap<>();
        private final Map<String, net.uebliche.hub.config.Config.LobbyGroup> groupByName = new LinkedHashMap<>();
        private final Map<String, List<String>> childrenByGroup = new LinkedHashMap<>();
        private final MessageUtils messageUtils;
        private final Player player;

        private ParentTargetResolver(net.uebliche.hub.config.Config config, MessageUtils messageUtils, Player player) {
            this.messageUtils = messageUtils;
            this.player = player;
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
                    messageUtils.sendDebugMessage(player,
                            "<yellow>⚠️ Lobby '" + lobbyName + "' from group '" + group.name + "' not found; skipping.</yellow>");
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

    private int execute(CommandContext<CommandSource> commandContext, Lobby lobby) {
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        Player player = commandContext.getSource() instanceof Player ? (Player) commandContext.getSource() : null;
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        messageUtils.broadcastDebugMessage("🤖 " + commandContext.getSource().toString() + " ->  Executing Hub Command for Lobby: " + lobby.name + "!");
        if (player == null) {
            commandContext.getSource().sendMessage(miniMessage().deserialize(configUtils.config().systemMessages.playersOnlyCommandMessage));
            return 1;
        }
        var currentServer = player.getCurrentServer().orElse(null);
        if (currentServer == null) {
            messageUtils.sendMessage(player, "<red>❌ User is on no Server!");
            return 1;
        }
        if (lobby.filter.matcher(currentServer.getServerInfo().getName()).matches()) {
            messageUtils.sendMessage(player, lobby.messages().alreadyConnectedMessage == null ? configUtils.config().messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, currentServer.getServer(), lobby);
            return 1;
        }

        executor.execute(() -> {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            AtomicBoolean isConnected = new AtomicBoolean(false);
            hub.server().getAllServers()
                    .stream()
                    .filter(registeredServer -> lobby.filter.matcher(registeredServer.getServerInfo().getName()).matches()).forEach(registeredServer -> {
                        CompletableFuture.runAsync(() -> {
                            registeredServer.ping().thenAccept(result -> {
                                if (isConnected.get())
                                    return;
                                isConnected.set(true);
                                Utils.util(PlayerUtils.class).connect(player, registeredServer, lobby);
                                executor.shutdown();
                            });
                        }, executor);
                    });
        });
        return 1;
    }
}
