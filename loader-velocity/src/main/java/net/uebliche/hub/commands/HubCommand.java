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
import java.util.LinkedHashSet;
import java.util.List;
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
        var visitedParentLobbies = new LinkedHashSet<String>();
        for (String parentName : orderedUniqueParents) {
            if (parentName.equalsIgnoreCase(currentLobby.name)) {
                messageUtils.sendDebugMessage(player, "<yellow>⚠️ Parent lobby for " + currentLobby.name + " points to itself; skipping.");
                continue;
            }

            var candidateLobbies = resolveParentTargets(parentName, configUtils.config(), messageUtils, player);
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

    private List<Lobby> resolveParentTargets(String parentName, net.uebliche.hub.config.Config config, MessageUtils messageUtils, Player player) {
        var results = new ArrayList<Lobby>();

        config.lobbies.stream()
                .filter(lobby -> lobby.name.equalsIgnoreCase(parentName))
                .findFirst()
                .ifPresent(results::add);

        if (!results.isEmpty()) {
            return results;
        }

        config.lobbyGroups.stream()
                .filter(group -> group.name.equalsIgnoreCase(parentName))
                .findFirst()
                .ifPresent(group -> {
                    group.lobbies.forEach(lobbyName -> config.lobbies.stream()
                            .filter(lobby -> lobby.name.equalsIgnoreCase(lobbyName))
                            .findFirst()
                            .ifPresentOrElse(results::add, () -> messageUtils.sendDebugMessage(player,
                                    "<yellow>⚠️ Lobby '" + lobbyName + "' from group '" + parentName + "' not found; skipping.</yellow>")));
                });

        return results;
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
