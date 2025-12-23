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
                                        messageUtils.sendDebugMessage(sender, "🤖 Testing Messages for Lobby: " + lobby.name);
                                        RegisteredServer registeredServer = hub.server().getAllServers().stream().filter(registeredServer1 -> lobby.filter.matcher(registeredServer1.getServerInfo().getName()).matches()).findFirst().orElse(null);
                                        if (registeredServer == null) {
                                            messageUtils.sendDebugMessage(sender, "❌ No Server found!");
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
                        }));
    }

}
