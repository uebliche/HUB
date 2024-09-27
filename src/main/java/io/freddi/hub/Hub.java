package io.freddi.hub;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.freddi.hub.utils.*;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
            new UpdateChecker(this);
        } catch (ConfigurateException e) {
            logger.error("Failed to load config!", e);
        }
        new CommandUtils(this);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Goodbye!");
    }

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        if (configUtils.config().autoSelect.onJoin) {
            event.setInitialServer(Utils.util(LobbyUtils.class).findBest(event.getPlayer()).server());
        }
        UpdateChecker updateChecker = Utils.util(UpdateChecker.class);
        if (configUtils.config().updateChecker.enabled && updateChecker.updateAvailable && (configUtils.config().updateChecker.notification.isBlank() || event.getPlayer().hasPermission(configUtils.config().updateChecker.notification))) {
            event.getPlayer().sendMessage(miniMessage().deserialize(configUtils.config().updateChecker.notification, Placeholder.parsed("current", Props.VERSION), Placeholder.parsed("latest", updateChecker.latest)));
        }
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        if (Utils.util(ConfigUtils.class).config().autoSelect.onServerKick)
            event.getPlayer().createConnectionRequest(Utils.util(LobbyUtils.class).findBest(event.getPlayer()).server()).getServer();

    }

    public ProxyServer server() {
        return server;
    }
}
