package net.uebliche.hub.utils;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
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
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.checkerframework.checker.units.qual.m;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class MessageUtils extends Utils<MessageUtils> {

    public MessageUtils(Hub hub) {
        super(hub);
    }

    public Component toMessage(String message, Object... objects) {
        return miniMessage().deserialize(message, placeholders(objects).toArray(new TagResolver.Single[0]));
    }

    public void sendMessage(Player player, String message, Object... objects) {
        if (message.isBlank()) {
            return;
        }
        player.sendMessage(toMessage(message, objects));
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
                        broadcastDebugMessage("<gray>👥 Server " + serverInfo.getName() + " currently tracks "
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

    public void sendDebugMessage(Audience recipient, String message) {
        sendDebugMessage(recipient, miniMessage().deserialize(message));
        broadcastDebugMessage(Component.text(recipient.toString()).append(miniMessage().deserialize(message)));
    }

    public void sendDebugMessage(Audience recipient, Component message) {
        if (!isDebugEnabled()) {
            return;
        }
        boolean canSend = ((recipient instanceof Player player) && Utils.util(PlayerUtils.class).canDebug(player))
                || recipient instanceof ConsoleCommandSource;
        if (canSend) {
            recipient.sendMessage(toDebugMessage(message));
        }
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
        if (!isDebugEnabled()) {
            return;
        }
        broadcastDebugMessage(miniMessage().deserialize(message));
    }

    public void broadcastDebugMessage(Component message) {
        if (!isDebugEnabled()) {
            return;
        }
        hub.server()
                .filterAudience(audience -> (audience instanceof Player player)
                        && Utils.util(PlayerUtils.class).canDebug(player) || audience instanceof ConsoleCommandSource)
                .sendMessage(toDebugMessage(message));
    }

    private boolean isDebugEnabled() {
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        return configUtils != null && configUtils.config() != null && configUtils.config().debug.enabled;
    }
}
