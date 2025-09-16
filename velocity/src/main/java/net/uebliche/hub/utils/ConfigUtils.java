package net.uebliche.hub.utils;

import net.uebliche.hub.Hub;
import net.uebliche.hub.config.Config;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConfigUtils extends Utils<ConfigUtils> {

    private Config config;
    private YamlConfigurationLoader configLoader;
    private CommentedConfigurationNode node;
    private List<Runnable> onReload = new ArrayList<>();

    public ConfigUtils(Hub hub, Path dataDirectory) {
        super(hub);
        configLoader = YamlConfigurationLoader.builder()
                .path(dataDirectory.resolve("config.yml"))
                .defaultOptions(opts -> opts.shouldCopyDefaults(true).header("Thanks <3").implicitInitialization(true))
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .headerMode(HeaderMode.PRESET)
                .build();
    }

    public void reload() throws ConfigurateException {
        node = configLoader.load();
        config = node.get(Config.class);
        processConfig();
        node.set(Config.class, config);
        configLoader.save(node);
        onReload.forEach(Runnable::run);
    }

    public void save(Audience audience) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        try {
            node.set(Config.class, config);
            configLoader.save(node);
            reload();
        } catch (SerializationException e) {
            messageUtils.sendDebugMessage(audience, "<red>❌ Failed to Serialized Config!");
            messageUtils.sendDebugMessage(audience, e.getMessage());
        } catch (ConfigurateException e) {
            messageUtils.sendDebugMessage(audience, "<red>❌ Failed to Save Config!");
            messageUtils.sendDebugMessage(audience, e.getMessage());
        }
    }

    public void debug(Audience audience, boolean enable) {
        config.debug.enabled = enable;
        save(audience);
    }

    public void onReload(Runnable runner) {
        onReload.add(runner);
    }

    private void processConfig() {
        config.lobbies = config.lobbies.stream().sorted(Comparator.comparingInt(o -> -o.priority)).toList();
    }

    public Config config() {
        return config;
    }

    public ConfigUtils setConfig(Config config) {
        this.config = config;
        return this;
    }

    public YamlConfigurationLoader configLoader() {
        return configLoader;
    }

    public ConfigUtils setConfigLoader(YamlConfigurationLoader configLoader) {
        this.configLoader = configLoader;
        return this;
    }

    public CommentedConfigurationNode node() {
        return node;
    }

    public ConfigUtils setNode(CommentedConfigurationNode node) {
        this.node = node;
        return this;
    }

    public List<Runnable> onReload() {
        return onReload;
    }

    public ConfigUtils setOnReload(List<Runnable> onReload) {
        this.onReload = onReload;
        return this;
    }

    public void reload(Audience recipient) {
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        try {
            reload();
            messageUtils.sendDebugCommandMessage(recipient, "<green>✔ Reload successful!");
        } catch (ConfigurateException e) {
            messageUtils.sendDebugCommandMessage(recipient, "<red>❌ Reload failed!");
            messageUtils.sendDebugCommandMessage(recipient, e.getMessage());
        }

    }
}
