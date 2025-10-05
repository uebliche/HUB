package net.uebliche.hub;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent.ServerKickResult;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.uebliche.hub.utils.*;
import net.uebliche.hub.utils.UpdateChecker;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Path;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@Plugin(id = Props.ID, name = Props.PROJECTNAME, version = Props.VERSION, authors = Props.AUTHOR)
public class Hub {

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
        try {
            Utils.util(ConfigUtils.class).reload();
            new net.uebliche.hub.utils.UpdateChecker(this);
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
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        messageUtils.sendDebugMessage(event.getPlayer(), "<gray>PlayerChooseInitialServerEvent triggered (initial=" + event.getInitialServer().map(s -> s.getServerInfo().getName()).orElse("<none>") + ")</gray>");
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        if (configUtils.config().autoSelect.onJoin) {
            var lobbyUtils = Utils.util(LobbyUtils.class);
            var selection = lobbyUtils.findBest(event.getPlayer());
            if (selection.isPresent()) {
                event.setInitialServer(selection.get().server());
            } else {
                messageUtils.sendDebugMessage(event.getPlayer(), "<red>❌ No lobby could be selected during login.");
                if (event.getInitialServer().isEmpty()) {
                    event.getPlayer().disconnect(messageUtils.toMessage(configUtils.config().systemMessages.noLobbyFoundMessage, event.getPlayer()));
                }
            }
        }
        UpdateChecker updateChecker = Utils.util(UpdateChecker.class);
        if (configUtils.config().updateChecker.enabled && updateChecker.updateAvailable && (configUtils.config().updateChecker.notification.isBlank() || event.getPlayer().hasPermission(configUtils.config().updateChecker.notification))) {
            event.getPlayer().sendMessage(miniMessage().deserialize(configUtils.config().updateChecker.notification, Placeholder.parsed("current", Props.VERSION), Placeholder.parsed("latest", updateChecker.latest)));
        }
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        messageUtils.sendDebugMessage(event.getPlayer(), "<gray>KickedFromServerEvent triggered from " + event.getServer().getServerInfo().getName() + " (result=" + event.getResult() + ")</gray>");
        if (Utils.util(ConfigUtils.class).config().autoSelect.onServerKick) {
            var configUtils = Utils.util(ConfigUtils.class);
            Utils.util(LobbyUtils.class).findBest(event.getPlayer()).ifPresentOrElse(pingResult -> {
                messageUtils.sendDebugMessage(event.getPlayer(), "🔁 Redirecting player after kick.");
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(pingResult.server()));
            }, () -> {
                messageUtils.sendDebugMessage(event.getPlayer(), "<red>❌ No fallback lobby available after kick.");
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(messageUtils.toMessage(configUtils.config().messages.serverDisconnectedMessage, event.getServer(), event.getPlayer())));
            });
        }

    }

    public ProxyServer server() {
        return server;
    }
}

