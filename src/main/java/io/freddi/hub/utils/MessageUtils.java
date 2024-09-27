package io.freddi.hub.utils;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.freddi.hub.Hub;
import io.freddi.hub.config.Config;
import io.freddi.hub.config.Lobby;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class MessageUtils extends Utils<MessageUtils> {


    public MessageUtils(Hub hub) {
        super(hub);
    }

    public Component toMessage(String message, Object... objects) {
        return miniMessage().deserialize(message, placeholders(objects).toArray(new TagResolver.Single[0]));
    }

    public void sendMessage(Player player, String message, Object... objects) {
        if (message.isBlank()) return;
        player.sendMessage(toMessage(message, objects));
    }

    public List<TagResolver.Single> placeholders(Object... objects) {
        List<TagResolver.Single> placeholders = new ArrayList<>();
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        Config.Placeholder placeholder = configUtils.config().placeholder;
        for (Object object : objects) {
            if (object instanceof TagResolver.Single) {
                placeholders.add((TagResolver.Single) object);
            }
            if (object instanceof RegisteredServer registeredServer) {
                if (placeholder.server.enabled() || placeholder.serverHost.enabled() || placeholder.serverPort.enabled() || placeholder.serverPlayerCount.enabled() || placeholder.serverPlayerPerPlayerUsername.enabled() || placeholder.serverPlayerPerPlayerUuid.enabled()) {
                    ServerInfo serverInfo = registeredServer.getServerInfo();
                    if (placeholder.server.enabled())
                        placeholders.add(Placeholder.unparsed(placeholder.server.key(), serverInfo.getName()));
                    if (placeholder.serverHost.enabled())
                        placeholders.add(Placeholder.unparsed(placeholder.serverHost.key(), serverInfo.getAddress().getHostString()));
                    if (placeholder.serverPort.enabled())
                        placeholders.add(Placeholder.unparsed(placeholder.serverPort.key(), String.valueOf(serverInfo.getAddress().getPort())));

                    if (placeholder.serverPlayerCount.enabled())
                        placeholders.add(Placeholder.unparsed(placeholder.serverPlayerCount.key(), String.valueOf(registeredServer.getPlayersConnected().size())));
                    if (placeholder.serverPlayerPerPlayerUsername.enabled() || placeholder.serverPlayerPerPlayerUuid.enabled()) {
                        AtomicInteger i = new AtomicInteger(0);
                        registeredServer.getPlayersConnected().forEach(player -> {
                            int id = i.getAndIncrement();
                            if (placeholder.serverPlayerPerPlayerUsername.enabled())
                                placeholders.add(Placeholder.unparsed(placeholder.serverPlayerPerPlayerUsername.key().replaceFirst(placeholder.serverPlayerPerPlayerUsername.placeholder(), String.valueOf(id)), player.getUsername()));
                            if (placeholder.serverPlayerPerPlayerUuid.enabled())
                                placeholders.add(Placeholder.unparsed(placeholder.serverPlayerPerPlayerUuid.key().replaceFirst(placeholder.serverPlayerPerPlayerUsername.placeholder(), String.valueOf(id)), player.getUniqueId().toString()));
                        });
                    }
                }
            }

            if (object instanceof Lobby lobby) {
                if (placeholder.lobby.enabled())
                    placeholders.add(Placeholder.unparsed(placeholder.lobby.key(), lobby.name));
                if (placeholder.lobbyFilter.enabled())
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyFilter.key(), lobby.filter.toString()));
                if (placeholder.lobbyPermission.enabled())
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyPermission.key(), lobby.permission));
                if (placeholder.lobbyPriority.enabled())
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyPriority.key(), String.valueOf(lobby.priority)));

                if (placeholder.lobbyCommandPerCommandStandalone.enabled() || placeholder.lobbyCommandPerCommandSubcommand.enabled() || placeholder.lobbyCommandPerCommandHideOn.enabled())
                    //Commands
                    lobby.commands.forEach((s, command) -> {
                        if (placeholder.lobbyCommandPerCommandStandalone.enabled())
                            placeholders.add(Placeholder.unparsed(placeholder.lobbyCommandPerCommandStandalone.key().replaceFirst(placeholder.lobbyCommandPerCommandStandalone.placeholder(), s), command.standalone ? "true" : "false"));
                        if (placeholder.lobbyCommandPerCommandSubcommand.enabled())
                            placeholders.add(Placeholder.unparsed(placeholder.lobbyCommandPerCommandSubcommand.key().replaceFirst(placeholder.lobbyCommandPerCommandSubcommand.placeholder(), s), command.subcommand ? "true" : "false"));
                        if (placeholder.lobbyCommandPerCommandHideOn.enabled())
                            placeholders.add(Placeholder.unparsed(placeholder.lobbyCommandPerCommandHideOn.key().replaceFirst(placeholder.lobbyCommandPerCommandHideOn.placeholder(), s), command.hideOn().toString()));
                    });
                if (placeholder.lobbyAutojoin.enabled())
                    placeholders.add(Placeholder.unparsed(placeholder.lobbyAutojoin.key(), String.valueOf(lobby.autojoin)));
            }
            if (object instanceof Player player) {
                if (placeholder.player.enabled() || placeholder.playerUuid.enabled()) {
                    if (placeholder.player.enabled())
                        placeholders.add(Placeholder.unparsed(placeholder.player.key(), player.getUsername()));
                    if (placeholder.playerUuid.enabled())
                        placeholders.add(Placeholder.unparsed(placeholder.playerUuid.key(), player.getUniqueId().toString()));
                }
            }
        }
        return placeholders;
    }

    public void sendDebugMessage(Audience recipient, String message) {
        sendDebugMessage(recipient, miniMessage().deserialize(message));
    }

    public void sendDebugMessage(Audience recipient, Component message) {
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        if (configUtils.config().debug.enabled && (((recipient instanceof Player player) && Utils.util(PlayerUtils.class).canDebug(player)) || recipient instanceof ConsoleCommandSource))
            recipient.sendMessage(Component.empty().append(Component.text("[Debug]: ").style(Style.style(TextDecoration.BOLD, NamedTextColor.YELLOW))).append(message));
    }


    public void sendDebugCommandMessage(Audience recipient, String message) {
        sendDebugCommandMessage(recipient, Component.text(message));
    }

    public void sendDebugCommandMessage(Audience recipient, Component message) {
        recipient.sendMessage(toDebugMessage(message));
    }

    private Component toDebugMessage(Component message) {
        return Component.empty().append(Component.text("[Debug]: ").style(Style.style(TextDecoration.BOLD, NamedTextColor.YELLOW))).append(message);
    }

    public void broadcastDebugMessage(String message) {
        broadcastDebugMessage(miniMessage().deserialize(message));
    }

    public void broadcastDebugMessage(Component message) {
        hub.server().filterAudience(audience -> (audience instanceof Player player) && Utils.util(PlayerUtils.class).canDebug(player) || audience instanceof ConsoleCommandSource).sendMessage(toDebugMessage(message));
    }
}
