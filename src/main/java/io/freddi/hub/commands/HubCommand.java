package io.freddi.hub.commands;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import io.freddi.hub.Hub;
import io.freddi.hub.config.Lobby;
import io.freddi.hub.utils.*;

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
        LobbyUtils.util(LobbyUtils.class).findBest(player).connect();
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
