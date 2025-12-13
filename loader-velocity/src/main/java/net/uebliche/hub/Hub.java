package net.uebliche.hub;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.uebliche.hub.utils.*;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Path;
import java.util.Set;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@Plugin(id = Hub.PLUGIN_ID, name = Hub.PLUGIN_NAME, version = Hub.VERSION, authors = {Hub.AUTHOR})
public class Hub {
    public static final String PLUGIN_ID = "hub";
    public static final String PLUGIN_NAME = "HUB";
    public static final String VERSION = "dev";
    public static final String AUTHOR = "uebliche";

    private static final MinecraftChannelIdentifier HUB_CHANNEL = MinecraftChannelIdentifier.create("uebliche", "hub");

    @Inject
    private final Logger logger;

    private final ProxyServer server;
    private final Path dataDirectory;

    @Inject
    public Hub(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        new ConfigUtils(this, dataDirectory);
        new PlayerUtils(this);
        new MessageUtils(this);
        new LobbyUtils(this);
        new LastLobbyTracker(this);
        server.getChannelRegistrar().register(HUB_CHANNEL);
        try {
            Utils.util(ConfigUtils.class).reload();
            // ensure update checker util is registered
            if (Utils.util(UpdateChecker.class) == null) {
                new UpdateChecker(this);
            }
            scheduleUpdateChecker();
        } catch (ConfigurateException e) {
            logger.error("Failed to load config!", e);
        }
        new CommandUtils(this);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        Utils.shutdownAll();
        logger.info("Goodbye!");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(HUB_CHANNEL)) {
            return;
        }
        if (!(event.getSource() instanceof ServerConnection connection)) {
            return;
        }
        byte[] data = event.getData();
        if (data == null || data.length == 0) {
            return;
        }
        ByteArrayDataInput root = ByteStreams.newDataInput(data);
        String type;
        ByteArrayDataInput in;
        try {
            int len = readVarInt(root);
            byte[] payload = new byte[len];
            root.readFully(payload);
            in = ByteStreams.newDataInput(payload);
            type = in.readUTF();
        } catch (Exception ex) {
            logger.warn("Received hub plugin message with invalid payload ({} bytes)", data.length, ex);
            return;
        }
        if ("LIST".equalsIgnoreCase(type)) {
            handleLobbyListRequest(connection, in);
        } else if ("CONNECT".equalsIgnoreCase(type)) {
            handleConnectRequest(connection, in);
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    private void handleLobbyListRequest(ServerConnection connection, ByteArrayDataInput in) {
        var messageUtils = Utils.util(MessageUtils.class);
        var configUtils = Utils.util(ConfigUtils.class);
        var lobbyUtils = Utils.util(LobbyUtils.class);
        var playerUtils = Utils.util(PlayerUtils.class);
        if (configUtils == null || lobbyUtils == null || playerUtils == null) {
            return;
        }
        var player = connection.getPlayer();
        try {
            in.readUTF(); // consume optional player UUID from client
        } catch (Exception ignored) {
        }
        var accessible = configUtils.config().lobbies.stream()
                .filter(lobby -> playerUtils.permissionCheck(player, lobby))
                .toList();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LIST");
        out.writeUTF(player.getUniqueId().toString());
        var entries = accessible.stream()
                .map(lobby -> lobbyUtils.getCachedResults(player, lobby).stream().findFirst()
                        .map(result -> new LobbyEntry(lobby.name, result.result().server(), result.result().players()))
                        .orElse(null))
                .filter(entry -> entry != null)
                .toList();
        out.writeInt(entries.size());
        for (LobbyEntry entry : entries) {
            out.writeUTF(entry.name());
            out.writeUTF(entry.server().getServerInfo().getName());
            out.writeInt(entry.players().getOnline());
            out.writeInt(entry.players().getMax());
        }
        byte[] inner = out.toByteArray();
        ByteArrayDataOutput wrapped = ByteStreams.newDataOutput();
        writeVarInt(wrapped, inner.length);
        wrapped.write(inner);
        connection.sendPluginMessage(HUB_CHANNEL, wrapped.toByteArray());
        // Debug messages remain English to keep logs consistent across locales
        messageUtils.sendDebugMessage(player, messageUtils.i18n("compass.debug-list", player,
                Placeholder.parsed("count", String.valueOf(entries.size()))));
    }

    private void handleConnectRequest(ServerConnection connection, ByteArrayDataInput in) {
        var messageUtils = Utils.util(MessageUtils.class);
        var configUtils = Utils.util(ConfigUtils.class);
        var lobbyUtils = Utils.util(LobbyUtils.class);
        var playerUtils = Utils.util(PlayerUtils.class);
        if (configUtils == null || lobbyUtils == null || playerUtils == null) {
            return;
        }
        var player = connection.getPlayer();
        try {
            in.readUTF(); // consume optional player UUID
        } catch (Exception ignored) {
        }
        String lobbyName = in.readUTF();
        var lobbyOpt = configUtils.config().lobbies.stream()
                .filter(l -> l.name.equalsIgnoreCase(lobbyName))
                .findFirst();
        if (lobbyOpt.isEmpty()) {
            messageUtils.sendDebugMessage(player, messageUtils.i18n("compass.error-not-found", player,
                    Placeholder.parsed("lobby", lobbyName)));
            return;
        }
        var lobby = lobbyOpt.get();
        if (!playerUtils.permissionCheck(player, lobby)) {
            messageUtils.sendMessage(player, messageUtils.i18n("compass.error-no-permission", player));
            return;
        }
        var results = lobbyUtils.getCachedResults(player, lobby);
        if (results.isEmpty()) {
            messageUtils.sendMessage(player, messageUtils.i18n("compass.error-no-server", player));
            return;
        }
        var best = results.getFirst().result();
        messageUtils.sendDebugMessage(player, messageUtils.i18n("compass.debug-connect", player,
                Placeholder.parsed("server", best.server().getServerInfo().getName())));
        best.connect();
    }

    private int readVarInt(ByteArrayDataInput in) {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private void writeVarInt(ByteArrayDataOutput out, int value) {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        messageUtils.sendDebugMessage(event.getPlayer(), "<gray>PlayerChooseInitialServerEvent triggered (initial="
                + event.getInitialServer().map(s -> s.getServerInfo().getName()).orElse("<none>") + ")</gray>");
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        if (configUtils.config().autoSelect.onJoin) {
            var lobbyUtils = Utils.util(LobbyUtils.class);
            var selection = lobbyUtils.findBest(event.getPlayer());
            if (selection.isPresent()) {
                event.setInitialServer(selection.get().server());
            } else {
                messageUtils.sendDebugMessage(event.getPlayer(), "<red>❌ No lobby could be selected during login.");
                if (event.getInitialServer().isEmpty()) {
                    event.getPlayer().disconnect(messageUtils
                            .toMessage(configUtils.config().systemMessages.noLobbyFoundMessage, event.getPlayer()));
                }
            }
        }
        UpdateChecker updateChecker = Utils.util(UpdateChecker.class);
        if (configUtils.config().updateChecker.enabled && updateChecker.updateAvailable
                && (configUtils.config().updateChecker.notification.isBlank()
                        || event.getPlayer().hasPermission(configUtils.config().updateChecker.notification))) {
            event.getPlayer().sendMessage(miniMessage().deserialize(configUtils.config().updateChecker.notification,
                    Placeholder.parsed("current", VERSION), Placeholder.parsed("latest", updateChecker.latest)));
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        var configUtils = Utils.util(ConfigUtils.class);
        var messageUtils = Utils.util(MessageUtils.class);
        var playerUtils = Utils.util(PlayerUtils.class);
        var lastLobbyTracker = Utils.util(LastLobbyTracker.class);
        if (configUtils == null || messageUtils == null || playerUtils == null || lastLobbyTracker == null) {
            return;
        }
        if (!configUtils.config().lastLobby.enabled) {
            return;
        }
        var server = event.getServer();
        var player = event.getPlayer();
        configUtils.config().lobbies.stream()
                .filter(lobby -> lobby.filter.matcher(server.getServerInfo().getName()).matches())
                .filter(lobby -> playerUtils.permissionCheck(player, lobby))
                .findFirst()
                .ifPresent(lobby -> {
                    lastLobbyTracker.remember(player, lobby, server);
                    messageUtils.sendDebugMessage(player, "<gray>💾 Remembered last lobby as "
                            + server.getServerInfo().getName() + " (" + lobby.name + ").</gray>");
                });
    }

    private void scheduleUpdateChecker() {
        var configUtils = Utils.util(ConfigUtils.class);
        if (configUtils == null || configUtils.config() == null || !configUtils.config().updateChecker.enabled) {
            return;
        }
        if (Utils.util(UpdateChecker.class) == null) {
            new UpdateChecker(this);
        }
        server.getScheduler().buildTask(this, () -> {
            var result = net.uebliche.hub.common.update.UpdateChecker.checkModrinth(
                    "HrTclB8n", VERSION, msg -> logger.info(msg));
            Utils.util(UpdateChecker.class).update(result);
        }).delay(java.time.Duration.ZERO)
                .repeat(java.time.Duration.ofMinutes(Math.max(5,
                        configUtils.config().updateChecker.checkIntervalInMin == null ? 30
                                : configUtils.config().updateChecker.checkIntervalInMin)))
                .schedule();
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        var configUtils = Utils.util(ConfigUtils.class);
        var config = configUtils.config();
        var kickMessageConfig = config.kickMessage;
        if (kickMessageConfig != null && kickMessageConfig.enabled) {
            event.getServerKickReason().ifPresent(message -> {
                String plainReason = PlainTextComponentSerializer.plainText().serialize(message).trim();
                if (!plainReason.isEmpty()) {
                    var decoratedMessage = message;
                    if (kickMessageConfig.prefix != null && !kickMessageConfig.prefix.isBlank()) {
                        decoratedMessage = miniMessage().deserialize(kickMessageConfig.prefix).append(decoratedMessage);
                    }
                    if (kickMessageConfig.suffix != null && !kickMessageConfig.suffix.isBlank()) {
                        decoratedMessage = decoratedMessage.append(miniMessage().deserialize(kickMessageConfig.suffix));
                    }
                    event.getPlayer().sendMessage(decoratedMessage);
                }
            });
        }
        messageUtils.sendDebugMessage(event.getPlayer(), "<gray>KickedFromServerEvent triggered from "
                + event.getServer().getServerInfo().getName() + " (result=" + event.getResult() + ")</gray>");

        var player = event.getPlayer();
        boolean hasCurrentServer = player.getCurrentServer().isPresent();
        boolean hasExistingRedirect = event.getResult() instanceof KickedFromServerEvent.RedirectPlayer;

        if (config.autoSelect.onServerKick) {
            Utils.util(LobbyUtils.class).findBest(player,
                    Set.of(event.getServer().getServerInfo().getName())).ifPresentOrElse(pingResult -> {
                        messageUtils.sendDebugMessage(player, "🔁 Redirecting player after kick.");
                        event.setResult(KickedFromServerEvent.RedirectPlayer.create(pingResult.server()));
                    }, () -> {
                        if (hasExistingRedirect) {
                            messageUtils.sendDebugMessage(player, "<yellow>⚠️ No cached fallback lobby available; keeping existing redirect result.");
                            return;
                        }
                        messageUtils.sendDebugMessage(player, "<red>❌ No fallback lobby available after kick.");
                        if (!hasExistingRedirect) {
                            event.setResult(KickedFromServerEvent.DisconnectPlayer
                                    .create(messageUtils.toMessage(config.messages.serverDisconnectedMessage,
                                            event.getServer(), player)));
                        }
                    });
            return;
        }

        // No auto-fallback configured; if this was the very first connection and no other redirect is present, disconnect.
        if (!hasCurrentServer && !hasExistingRedirect) {
            messageUtils.sendDebugMessage(player, "<yellow>⚠️ Initial lobby connection failed, disconnecting player.");
            event.setResult(KickedFromServerEvent.DisconnectPlayer
                    .create(messageUtils.toMessage(config.messages.serverDisconnectedMessage,
                            event.getServer(), player)));
        }

    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        var lastLobbyTracker = Utils.util(LastLobbyTracker.class);
        if (lastLobbyTracker != null) {
            lastLobbyTracker.forget(event.getPlayer());
        }
    }

    public ProxyServer server() {
        return server;
    }

    private record LobbyEntry(String name, RegisteredServer server, com.velocitypowered.api.proxy.server.ServerPing.Players players) {
    }
}
