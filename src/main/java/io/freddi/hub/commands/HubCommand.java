package io.freddi.hub.commands;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import io.freddi.hub.Hub;
import io.freddi.hub.config.Lobby;
import io.freddi.hub.utils.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
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
        messageUtils.broadcastDebugMessage("🤖 " + commandContext.getSource().toString() + " ->  Executing Hub Command!");
        Player player = commandContext.getSource() instanceof Player ? (Player) commandContext.getSource() : null;
        if (player == null) {
            commandContext.getSource().sendMessage(messageUtils.toMessage(configUtils.config().systemMessages.playersOnlyCommandMessage));
            return 0;
        }
        if (!player.getCurrentServer().isPresent()) {
            messageUtils.sendMessage(player, "<red>❌ User is on no Server!");
            return 0;
        }
        executor.execute(() -> {
            messageUtils.sendDebugMessage(player, "✈ Sending Player to Lobby!");
            boolean isConnected = false;
            messageUtils.sendDebugMessage(player, "🔎 Found Lobbies in Config: " + String.join(", ", configUtils.config().lobbies.stream().map(lobby -> lobby.name).toList()));
            for (Lobby lobby : configUtils.config().lobbies) {
                messageUtils.sendDebugMessage(player, "❓ Checking if user can join: " + lobby.name);
                if (isConnected) {
                    messageUtils.sendDebugMessage(player, "<red>❌ User is Already Connected.");
                    return;
                }
                if (lobby.permission.isBlank() || player.hasPermission(lobby.permission)) {
                    messageUtils.sendDebugMessage(player, "<green>✔ User has Permission to join " + lobby.name + ".");
                    if (player.getCurrentServer().isPresent() && lobby.filter.matcher(player.getCurrentServer().get().getServerInfo().getName()).matches()) {
                        messageUtils.sendDebugMessage(player, "<red>❌ Current server matches the target Lobby group!");
                        messageUtils.sendMessage(player, lobby.messages().alreadyConnectedMessage == null ? configUtils.config().messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, player.getCurrentServer().get().getServer(), lobby);
                        isConnected = true;
                        return;
                    } else {
                        messageUtils.sendDebugMessage(player, "<green>✔ Current Server is not matching the target Lobby group!");
                    }
                    var servers = Utils.util(LobbyUtils.class).getLobbies(lobby, Duration.of(10, ChronoUnit.MILLIS), executor).map(CompletableFuture::join).filter(Objects::nonNull).toList();
                    messageUtils.sendDebugMessage(player, "🔎 Found " + servers.size() + " servers.");
                    var server = servers.stream()
                            .min(Comparator.comparingDouble(pingResult -> Math.abs((pingResult.usage() + 0.2) - 0.5)))
                            .orElse(null);
                    if (server != null) {
                        messageUtils.sendDebugMessage(player, "🔎 Best Server: " + server.server().getServerInfo().getName());
                        if (Utils.util(PlayerUtils.class).connect(player, server.server(), lobby).join()) {
                            messageUtils.sendDebugMessage(player, "<green>✔ Connection successful!");
                            isConnected = true;
                            return;
                        }
                        messageUtils.sendDebugMessage(player, "<red>❌ Connection failed!");
                    } else {
                        messageUtils.sendDebugMessage(player, "<red>❌ No Server found!");
                        messageUtils.sendMessage(player, lobby.messages().serverDisconnectedMessage == null ? configUtils.config().messages.serverDisconnectedMessage : lobby.messages().serverDisconnectedMessage, lobby, player);
                    }
                } else {
                    messageUtils.sendDebugMessage(player, "<red>❌ User has no Permission to join " + lobby.name + ".");
                }
            }
            messageUtils.sendMessage(player, configUtils.config().systemMessages.noLobbyFoundMessage, player);
        });
        return 1;
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
        if (lobby.filter.matcher(player.getCurrentServer().get().getServerInfo().getName()).matches()) {
            messageUtils.sendMessage(player, lobby.messages().alreadyConnectedMessage == null ? configUtils.config().messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, player.getCurrentServer().get().getServer(), lobby);
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
