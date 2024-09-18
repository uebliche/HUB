package io.freddi.hub;

import com.google.inject.Inject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@Plugin(id = Props.ID, name = Props.PROJECTNAME, version = Props.VERSION, authors = Props.AUTHOR)
public class Hub {

    @Inject
    private final Logger logger;

    private final ProxyServer server;
    private final Path dataDirectory;
    private final ConcurrentLinkedDeque<CommandMeta> commands = new ConcurrentLinkedDeque<>();
    protected Config config;
    protected YamlConfigurationLoader configLoader;
    protected CommentedConfigurationNode node;
    protected ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    protected UpdateChecker updateChecker;

    @Inject
    public Hub(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        configLoader = YamlConfigurationLoader.builder()
                .path(dataDirectory.resolve("config.yml"))
                .defaultOptions(opts -> opts.shouldCopyDefaults(true).header("Thanks <3").implicitInitialization(true))
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .headerMode(HeaderMode.PRESET)
                .build();
        try {
            reload();
            this.updateChecker = new UpdateChecker(this, logger);
        } catch (ConfigurateException e) {
            logger.error("Failed to load config!", e);
            registerDebugCommand();
            logger.info("Debug Command Registered! (/hub debug reload to reload the config)");
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Good bye!");
    }

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        if (config.autoSelect.onJoin) {
            event.setInitialServer(findBest(event.getPlayer()).server);
        }
        if (config.updateChecker.enabled && updateChecker.updateAvailable && (config.updateChecker.notification.isBlank() || event.getPlayer().hasPermission(config.updateChecker.notification))) {
            event.getPlayer().sendMessage(miniMessage().deserialize(config.updateChecker.notification, Placeholder.parsed("current", Props.VERSION), Placeholder.parsed("latest", updateChecker.latest)));
        }
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        if (config.autoSelect.onServerKick)
            event.getPlayer().createConnectionRequest(findBest(event.getPlayer()).server).getServer();

    }


    public boolean permissionCheck(Player player, Config.Lobby lobby) {
        sendDebugMessage(player, "🔎 Checking if user can join " + lobby.name);
        if (lobby.permission.isBlank() || player.hasPermission(lobby.permission)) {
            sendDebugMessage(player, "<green>✔ User has Permission to join " + lobby.name + ".");
            return true;
        } else {
            sendDebugMessage(player, "<red>❌ User has no Permission to join " + lobby.name + ".");
            return false;
        }
    }

    public PingResult findBest(Player player) {
        sendDebugMessage(player, "🔎 Searching for Best Lobby Server...");
        PingResult best = null;
        AtomicInteger duration = new AtomicInteger(config.finder.startDuration);
        while (best == null) {
            sendDebugMessage(player, "🔎 Checking Duration: " + duration.get());
            for (Config.Lobby lobby : config.lobbies) {
                if (permissionCheck(player, lobby)) {
                    var servers = getLobbies(lobby, Duration.of(duration.get(), ChronoUnit.MILLIS), executor).map(CompletableFuture::join).filter(Objects::nonNull).toList();
                    sendDebugMessage(player, "<green>🤖 Found " + servers.size() + " servers.");
                    var server = servers.stream()
                            .min(Comparator.comparingDouble(pingResult -> Math.abs((pingResult.usage() + 0.2) - 0.5)))
                            .orElse(null);
                    if (server != null && best == null) {
                        best = server;
                    }
                    break;
                }
            }
            if (duration.get() < config.finder.maxDuration)
                sendDebugMessage(player, "🤖 Finder Timeout Duration got increased to " + duration.addAndGet(config.finder.incrementDuration));

        }
        return best;
    }

    private void reload() throws ConfigurateException {
        unregisterCommands();
        node = configLoader.load();
        config = node.get(Config.class);
        processConfig();
        node.set(Config.class, config);
        configLoader.save(node);
        registerCommands();
    }

    private void processConfig() {
        config.lobbies = config.lobbies.stream().sorted(Comparator.comparingInt(o -> -o.priority)).toList();
    }

    private void unregisterCommands() {
        commands.forEach(this::unregister);
    }

    private void unregister(CommandMeta meta) {
        server.getCommandManager().unregister(meta);
        commands.remove(meta);
    }

    private void registerCommand(CommandMeta meta, BrigadierCommand command) {
        commands.add(meta);
        server.getCommandManager().register(meta, command);
    }

    private void registerCommands() {
        var basecommand = BrigadierCommand.literalArgumentBuilder(config.baseHubCommand)
                .executes(this::execute)
                .requires(commandSource -> (
                        commandSource instanceof ConsoleCommandSource
                                || (
                                commandSource instanceof Player player &&
                                        player.getCurrentServer().isPresent() &&
                                        !config.hideHubCommandOnLobby.matcher(player.getCurrentServer().get().getServerInfo().getName()).matches()
                        ))
                )
                .then(debugCommand());

        config.lobbies.forEach(lobby -> {
            lobby.commands.forEach((s, command) -> {
                if (command.subcommand) {
                    basecommand.then(
                            BrigadierCommand.literalArgumentBuilder(s)
                                    .requires(source -> (source instanceof Player player) && (player.hasPermission(lobby.permission)))
                                    // .requires(commandSource -> !command.hidden)
                                    .executes(commandContext -> execute(commandContext, lobby)));
                }
                if (command.standalone)
                    registerCommand(server.getCommandManager()
                                    .metaBuilder(s)
                                    .plugin(this)
                                    .build(),
                            new BrigadierCommand(
                                    BrigadierCommand.literalArgumentBuilder(s)
                                            .requires(source -> (source instanceof Player player) && (player.hasPermission(lobby.permission) || lobby.permission.isBlank()))
                                            .executes(commandContext -> execute(commandContext, lobby))
                            )
                    );
            });
        });

        server.getCommandManager().register(
                server.getCommandManager()
                        .metaBuilder(config.baseHubCommand)
                        .aliases(config.aliases.toArray(new String[0]))
                        .plugin(this).build(), new BrigadierCommand(basecommand.build()));

    }

    private LiteralArgumentBuilder<CommandSource> debugCommand() {
        return BrigadierCommand.literalArgumentBuilder("debug")
                .requires(commandSource -> commandSource instanceof Player player && canDebug(player)
                        || commandSource instanceof ConsoleCommandSource)
                .then(
                        BrigadierCommand.literalArgumentBuilder("disable")
                                .requires(commandSource -> config.debug.enabled)
                                .requires(commandSource -> commandSource instanceof Player player)
                                .executes(commandContext -> {
                                    config.debug.enabled = false;
                                    Player player = (Player) commandContext.getSource();
                                    try {
                                        node.set(Config.class, config);
                                        configLoader.save(node);
                                        reload();
                                    } catch (SerializationException e) {
                                        sendDebugMessage(player, "❌ Failed to Serialized Config!");
                                        sendDebugMessage(player, e.getMessage());
                                    } catch (ConfigurateException e) {
                                        sendDebugMessage(player, "❌ Failed to Save Config!");
                                        sendDebugMessage(player, e.getMessage());
                                    }
                                    return 1;
                                }))
                .then(
                        BrigadierCommand.literalArgumentBuilder("enable")
                                .requires(commandSource -> !config.debug.enabled)
                                .requires(commandSource -> commandSource instanceof Player player)
                                .executes(commandContext -> {
                                    config.debug.enabled = true;
                                    Player player = (Player) commandContext.getSource();
                                    try {
                                        node.set(Config.class, config);
                                        configLoader.save(node);
                                        reload();
                                    } catch (SerializationException e) {
                                        sendDebugCommandMessage(player, "❌ Failed to Serialized Config!");
                                        sendDebugCommandMessage(player, e.getMessage());
                                    } catch (ConfigurateException e) {
                                        sendDebugCommandMessage(player, "❌ Failed to Save Config!");
                                        sendDebugCommandMessage(player, e.getMessage());
                                    }
                                    return 1;
                                }))
                .then(
                        BrigadierCommand.literalArgumentBuilder("reload")
                                .executes(commandContext -> {
                                    executor.execute(() -> {
                                        Player player = (Player) commandContext.getSource();
                                        try {
                                            reload();
                                            sendDebugCommandMessage(player, "✔️ Reload successful!");
                                        } catch (ConfigurateException e) {
                                            sendDebugCommandMessage(player, "❌ Reload failed!");
                                            sendDebugCommandMessage(player, e.getMessage());
                                            registerDebugCommand();
                                        }
                                    });
                                    return 1;
                                }))
                .then(
                        BrigadierCommand.literalArgumentBuilder("messages")
                                .requires(commandSource -> config != null && config.messages != null)
                                .requires(commandSource -> commandSource instanceof Player player && canDebug(player))
                                .executes(commandContext -> {
                                    Player player = (Player) commandContext.getSource();
                                    sendDebugCommandMessage(player, "🤖 Testing Messages");
                                    sendDebugCommandMessage(player, toMessage(config.systemMessages.playersOnlyCommandMessage));
                                    config.lobbies.forEach(lobby -> {
                                        sendDebugMessage(player, "🤖 Testing Messages for Lobby: " + lobby.name);
                                        RegisteredServer registeredServer = server.getAllServers().stream().filter(registeredServer1 -> lobby.filter.matcher(registeredServer1.getServerInfo().getName()).matches()).findFirst().orElse(null);
                                        if (registeredServer == null) {
                                            sendDebugMessage(player, "❌ No Server found!");
                                        } else {
                                            sendDebugCommandMessage(player, toMessage(lobby.messages().successMessage == null ? config.messages.successMessage : lobby.messages().successMessage, registeredServer, lobby));
                                            sendDebugCommandMessage(player, toMessage(lobby.messages().alreadyConnectedMessage == null ? config.messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, registeredServer, lobby));
                                            sendDebugCommandMessage(player, toMessage(lobby.messages().connectionInProgressMessage == null ? config.messages.connectionInProgressMessage : lobby.messages().connectionInProgressMessage, registeredServer, lobby));
                                            sendDebugCommandMessage(player, toMessage(lobby.messages().serverDisconnectedMessage == null ? config.messages.serverDisconnectedMessage : lobby.messages().serverDisconnectedMessage, registeredServer, lobby));
                                            sendDebugCommandMessage(player, toMessage(lobby.messages().connectionCancelledMessage == null ? config.messages.connectionCancelledMessage : lobby.messages().connectionCancelledMessage, registeredServer, lobby));
                                        }
                                    });
                                    return 1;
                                }))
                .then(
                        BrigadierCommand.literalArgumentBuilder("placeholders")
                                .requires(commandSource -> commandSource instanceof Player player && canDebug(player))
                                .requires(commandSource -> config != null && config.messages != null && config.placeholder != null)
                                .then(BrigadierCommand.requiredArgumentBuilder("lobby", StringArgumentType.word())
                                        .suggests((commandContext, suggestionsBuilder) -> {
                                            config.lobbies.forEach(lobby -> {
                                                suggestionsBuilder.add(suggestionsBuilder.suggest(lobby.name));
                                            });
                                            return suggestionsBuilder.buildFuture();
                                        }).then(BrigadierCommand.requiredArgumentBuilder("server", StringArgumentType.word())
                                                .suggests((commandContext, suggestionsBuilder) -> {
                                                    server.getAllServers().forEach(registeredServer -> {
                                                        suggestionsBuilder.add(suggestionsBuilder.suggest(registeredServer.getServerInfo().getName()));
                                                    });
                                                    return suggestionsBuilder.buildFuture();
                                                })
                                                .executes(commandContext -> {
                                                    Player player = (Player) commandContext.getSource();
                                                    String lobbyArg = commandContext.getArgument("lobby", String.class);
                                                    String serverArg = commandContext.getArgument("server", String.class);
                                                    Config.Lobby lobby = config.lobbies.stream().filter(l -> l.name.equalsIgnoreCase(lobbyArg)).findFirst().orElse(null);
                                                    if (lobby == null) {
                                                        sendDebugCommandMessage(player, "❌ Lobby " + lobbyArg + " not found!");
                                                        return 1;
                                                    }
                                                    RegisteredServer registeredServer = server.getServer(serverArg).get();
                                                    if (registeredServer == null) {
                                                        sendDebugCommandMessage(player, "❌ Server " + serverArg + " not found!");
                                                        return 1;
                                                    }
                                                    placeholders(player, lobby, registeredServer).forEach(tagResolver -> {
                                                        sendDebugCommandMessage(player, toMessage(tagResolver.key() + ": <" + tagResolver.key() + ">", player, lobby, registeredServer));
                                                    });

                                                    return 1;
                                                })
                                        )
                                )
                )
                .then(BrigadierCommand.literalArgumentBuilder("getLobbies")
                        .requires(commandSource -> commandSource instanceof Player player && canDebug(player))
                        .requires(commandSource -> config != null && config.messages != null && config.placeholder != null)
                        .then(BrigadierCommand.requiredArgumentBuilder("lobby", StringArgumentType.word())
                                .suggests((commandContext, suggestionsBuilder) -> {
                                    config.lobbies.forEach(lobby -> {
                                        suggestionsBuilder.add(suggestionsBuilder.suggest(lobby.name));
                                    });
                                    return suggestionsBuilder.buildFuture();
                                })
                                .executes(commandContext -> {
                                    Player player = (Player) commandContext.getSource();
                                    String lobbyArg = commandContext.getArgument("lobby", String.class);
                                    Config.Lobby lobby = config.lobbies.stream().filter(l -> l.name.equalsIgnoreCase(lobbyArg)).findFirst().orElse(null);
                                    if (lobby == null) {
                                        sendDebugCommandMessage(player, "❌ Lobby " + lobbyArg + " not found!");
                                        return 1;
                                    }
                                    getLobbies(lobby, Duration.of(10, ChronoUnit.MILLIS), executor).forEach(pingResultCompletableFuture -> {
                                        pingResultCompletableFuture.thenAccept(pingResult -> {
                                            sendDebugCommandMessage(player, "🤖 Ping Result: " + pingResult);
                                        });
                                    });
                                    return 1;
                                })
                        )
                );
    }

    private void registerDebugCommand() {
        registerCommand(
                server.getCommandManager().metaBuilder("hub").plugin(this).build(),
                new BrigadierCommand(
                        BrigadierCommand.literalArgumentBuilder("hub")
                                .executes(commandContext -> {
                                    if (!(commandContext.getSource() instanceof Player player)) {
                                        commandContext.getSource().sendMessage(toMessage(config.systemMessages.playersOnlyCommandMessage));
                                    } else {
                                        server.getConfiguration().getAttemptConnectionOrder().stream().findAny().ifPresent(attemptConnectionOrder -> {
                                            player.createConnectionRequest(server.getServer(attemptConnectionOrder).get()).connect().thenAccept(connection -> {

                                            });
                                        });
                                    }
                                    return 1;
                                })
                                .then(debugCommand())
                                .build()
                )
        );
    }

    private int execute(CommandContext<CommandSource> commandContext) {
        Player player = commandContext.getSource() instanceof Player ? (Player) commandContext.getSource() : null;
        if (player == null) {
            commandContext.getSource().sendMessage(toMessage(config.systemMessages.playersOnlyCommandMessage));
            return 0;
        }
        if (!player.getCurrentServer().isPresent())
            return 0;
        executor.execute(() -> {
            sendDebugMessage(player, "✈ Sending Player to Lobby!");
            boolean isConnected = false;
            sendDebugMessage(player, "🔎 Found Lobbies in Config: " + String.join(", ", config.lobbies.stream().map(lobby -> lobby.name).toList()));
            for (Config.Lobby lobby : config.lobbies) {
                sendDebugMessage(player, "❓ Checking if user can join: " + lobby.name);
                if (isConnected) {
                    sendDebugMessage(player, "<red>❌ User is Already Connected.");
                    return;
                }
                if (lobby.permission.isBlank() || player.hasPermission(lobby.permission)) {
                    sendDebugMessage(player, "<green>✔ User has Permission to join " + lobby.name + ".");
                    if (player.getCurrentServer().isPresent() && lobby.filter.matcher(player.getCurrentServer().get().getServerInfo().getName()).matches()) {
                        sendDebugMessage(player, "<red>❌ Current server matches the target Lobby group!");
                        sendMessage(player, lobby.messages().alreadyConnectedMessage == null ? config.messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, player.getCurrentServer().get().getServer(), lobby);
                        isConnected = true;
                        return;
                    } else {
                        sendDebugMessage(player, "<green>✔ Current Server is not matching the target Lobby group!");
                    }
                    var servers = getLobbies(lobby, Duration.of(10, ChronoUnit.MILLIS), executor).map(CompletableFuture::join).filter(Objects::nonNull).toList();
                    sendDebugMessage(player, "🔎 Found " + servers.size() + " servers.");
                    var server = servers.stream()
                            .min(Comparator.comparingDouble(pingResult -> Math.abs((pingResult.usage() + 0.2) - 0.5)))
                            .orElse(null);
                    if (server != null) {
                        sendDebugMessage(player, "🔎 Best Server: " + server.server.getServerInfo().getName());
                        if (connect(player, server.server, lobby).join()) {
                            sendDebugMessage(player, "<green>✔ Connection successful!");
                            isConnected = true;
                            return;
                        }
                        sendDebugMessage(player, "<red>❌ Connection failed!");
                    } else {
                        sendDebugMessage(player, "<red>❌ No Server found!");
                        sendMessage(player, lobby.messages().serverDisconnectedMessage == null ? config.messages.serverDisconnectedMessage : lobby.messages().serverDisconnectedMessage, lobby, player);
                    }
                } else {
                    sendDebugMessage(player, "<red>❌ User has no Permission to join " + lobby.name + ".");
                }
            }
            sendMessage(player, config.systemMessages.noLobbyFoundMessage, player);
        });

        return 1;
    }


    private int execute(CommandContext<CommandSource> commandContext, Config.Lobby lobby) {
        Player player = commandContext.getSource() instanceof Player ? (Player) commandContext.getSource() : null;
        if (player == null) {
            commandContext.getSource().sendMessage(miniMessage().deserialize(config.systemMessages.playersOnlyCommandMessage));
            return 1;
        }
        if (lobby.filter.matcher(player.getCurrentServer().get().getServerInfo().getName()).matches()) {
            sendMessage(player, lobby.messages().alreadyConnectedMessage == null ? config.messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, player.getCurrentServer().get().getServer(), lobby);
            return 1;
        }

        executor.execute(() -> {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            AtomicBoolean isConnected = new AtomicBoolean(false);
            server.getAllServers()
                    .stream()
                    .filter(registeredServer -> lobby.filter.matcher(registeredServer.getServerInfo().getName()).matches()).forEach(registeredServer -> {
                        CompletableFuture.runAsync(() -> {
                            registeredServer.ping().thenAccept(result -> {
                                if (isConnected.get())
                                    return;
                                isConnected.set(true);
                                connect(player, registeredServer, lobby);
                                executor.shutdown();
                            });
                        }, executor);
                    });
        });
        return 1;
    }

    public CompletableFuture<Boolean> connect(Player player, RegisteredServer server, Config.Lobby lobby) {
        sendDebugMessage(player, "✈ Sending player to " + server.getServerInfo().getName() + " as member of " + lobby.name);
        return player.createConnectionRequest(server).connect().thenApply(connection -> {
            if (connection.getStatus() == ConnectionRequestBuilder.Status.SUCCESS) {
                sendMessage(player, lobby.messages().successMessage == null ? config.messages.successMessage : lobby.messages().successMessage, server, lobby);
                return true;
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.ALREADY_CONNECTED) {
                sendMessage(player, lobby.messages().alreadyConnectedMessage == null ? config.messages.alreadyConnectedMessage : lobby.messages().alreadyConnectedMessage, server, lobby);
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS) {
                sendMessage(player, lobby.messages().connectionInProgressMessage == null ? config.messages.connectionInProgressMessage : lobby.messages().connectionInProgressMessage, server, lobby);
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.SERVER_DISCONNECTED) {
                sendMessage(player, lobby.messages().serverDisconnectedMessage == null ? config.messages.serverDisconnectedMessage : lobby.messages().serverDisconnectedMessage, server, lobby);
            }
            if (connection.getStatus() == ConnectionRequestBuilder.Status.CONNECTION_CANCELLED) {
                sendMessage(player, lobby.messages().connectionCancelledMessage == null ? config.messages.connectionCancelledMessage : lobby.messages().connectionCancelledMessage, server, lobby);
            }
            return false;
        });
    }

    public void sendDebugMessage(Player player, String message) {
        sendDebugMessage(player, miniMessage().deserialize(message));
    }

    private boolean canDebug(Player player) {
        return (config.debug.permission.isBlank() || player.hasPermission(config.debug.permission));
    }

    public void sendDebugMessage(Player player, Component message) {
        if (config.debug.enabled && canDebug(player))
            player.sendMessage(Component.empty().append(Component.text("[Debug]: ").style(Style.style(TextDecoration.BOLD, NamedTextColor.YELLOW))).append(message));
    }

    public void sendDebugCommandMessage(Player player, String message) {
        sendDebugCommandMessage(player, Component.text(message));
    }

    public void sendDebugCommandMessage(Player player, Component message) {
        player.sendMessage(Component.empty().append(Component.text("[Debug]: ").style(Style.style(TextDecoration.BOLD, NamedTextColor.YELLOW))).append(message));
    }

    public List<TagResolver.Single> placeholders(Object... objects) {
        List<TagResolver.Single> placeholders = new ArrayList<>();
        Config.Placeholder placeholder = config.placeholder;
        for (Object object : objects) {
            if (object instanceof TagResolver.Single) {
                placeholders.add((TagResolver.Single) object);
            }
            if (object instanceof RegisteredServer registeredServer) {
                if (placeholder.server.enabled || placeholder.serverHost.enabled || placeholder.serverPort.enabled || placeholder.serverPlayerCount.enabled || placeholder.serverPlayerPerPlayerUsername.enabled || placeholder.serverPlayerPerPlayerUuid.enabled) {
                    ServerInfo serverInfo = registeredServer.getServerInfo();
                    if (placeholder.server.enabled)
                        placeholders.add(Placeholder.unparsed(placeholder.server.key, serverInfo.getName()));
                    if (placeholder.serverHost.enabled)
                        placeholders.add(Placeholder.unparsed(placeholder.serverHost.key, serverInfo.getAddress().getHostString()));
                    if (placeholder.serverPort.enabled)
                        placeholders.add(Placeholder.unparsed(placeholder.serverPort.key, String.valueOf(serverInfo.getAddress().getPort())));

                    if (placeholder.serverPlayerCount.enabled)
                        placeholders.add(Placeholder.unparsed(placeholder.serverPlayerCount.key, String.valueOf(registeredServer.getPlayersConnected().size())));
                    if (placeholder.serverPlayerPerPlayerUsername.enabled || placeholder.serverPlayerPerPlayerUuid.enabled) {
                        AtomicInteger i = new AtomicInteger(0);
                        registeredServer.getPlayersConnected().forEach(player -> {
                            int id = i.getAndIncrement();
                            if (placeholder.serverPlayerPerPlayerUsername.enabled)
                                placeholders.add(Placeholder.unparsed(placeholder.serverPlayerPerPlayerUsername.key.replaceFirst(placeholder.serverPlayerPerPlayerUsername.placeholder, String.valueOf(id)), player.getUsername()));
                            if (placeholder.serverPlayerPerPlayerUuid.enabled)
                                placeholders.add(Placeholder.unparsed(placeholder.serverPlayerPerPlayerUuid.key.replaceFirst(placeholder.serverPlayerPerPlayerUsername.placeholder, String.valueOf(id)), player.getUniqueId().toString()));
                        });
                    }
                }
            }

            if (object instanceof Config.Lobby lobby) {
                if (placeholder.lobby.enabled)
                    placeholders.add(Placeholder.unparsed(placeholder.lobby.key, lobby.name));
                if (placeholder.lobbyFilter.enabled)
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyFilter.key, lobby.filter.toString()));
                if (placeholder.lobbyPermission.enabled)
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyPermission.key, lobby.permission));
                if (placeholder.lobbyPriority.enabled)
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyPriority.key, String.valueOf(lobby.priority)));

                if (placeholder.lobbyCommandPerCommandStandalone.enabled || placeholder.lobbyCommandPerCommandSubcommand.enabled || placeholder.lobbyCommandPerCommandHideOn.enabled)
                    //Commands
                    lobby.commands.forEach((s, command) -> {
                        if (placeholder.lobbyCommandPerCommandStandalone.enabled)
                            placeholders.add(Placeholder.unparsed(placeholder.lobbyCommandPerCommandStandalone.key.replaceFirst(placeholder.lobbyCommandPerCommandStandalone.placeholder, s), command.standalone ? "true" : "false"));
                        if (placeholder.lobbyCommandPerCommandSubcommand.enabled)
                            placeholders.add(Placeholder.unparsed(placeholder.lobbyCommandPerCommandSubcommand.key.replaceFirst(placeholder.lobbyCommandPerCommandSubcommand.placeholder, s), command.subcommand ? "true" : "false"));
                        if (placeholder.lobbyCommandPerCommandHideOn.enabled)
                            placeholders.add(Placeholder.unparsed(placeholder.lobbyCommandPerCommandHideOn.key.replaceFirst(placeholder.lobbyCommandPerCommandHideOn.placeholder, s), command.hideOn().toString()));
                    });
                if (placeholder.lobbyAutojoin.enabled)
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyAutojoin.key, String.valueOf(lobby.autojoin)));
            }
            if (object instanceof Player player) {
                if (placeholder.player.enabled || placeholder.playerUuid.enabled) {
                    if (placeholder.player.enabled)
                        placeholders.add(Placeholder.unparsed(placeholder.player.key, player.getUsername()));
                    if (placeholder.playerUuid.enabled)
                        placeholders.add(Placeholder.unparsed(placeholder.playerUuid.key, player.getUniqueId().toString()));
                }
            }
        }
        return placeholders;
    }

    public Component toMessage(String message, Object... objects) {
        return miniMessage().deserialize(message, placeholders(objects).toArray(new TagResolver.Single[0]));
    }

    public void sendMessage(Player player, String message, Object... objects) {
        if (message.isBlank()) return;
        player.sendMessage(toMessage(message, objects));
    }

    public Stream<CompletableFuture<PingResult>> getLobbies(Config.Lobby lobby, Duration timeout, Executor executor) {
        return server.getAllServers().stream().filter(registeredServer -> lobby.filter.matcher(registeredServer.getServerInfo().getName()).matches()).map(registeredServer -> {
            return CompletableFuture.supplyAsync(() -> {
                var time = System.currentTimeMillis();
                try {
                    var ping = registeredServer.ping(PingOptions.builder().timeout(timeout).build()).join();
                    if (ping != null && ping.getPlayers().isPresent()) {
                        var players = ping.getPlayers().get();
                        return new PingResult(System.currentTimeMillis() - time, registeredServer, players);
                    }
                } catch (Exception ignored) {
                }
                return null;
            }, executor);
        });
    }

    record PingResult(
            long latency,
            RegisteredServer server,
            ServerPing.Players players
    ) {

        public Double usage() {
            return (double) players.getOnline() / players.getMax();
        }
    }
}
