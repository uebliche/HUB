package net.uebliche.hub;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Lang {

    private final YamlConfiguration yaml;
    private final MiniMessage mini;
    private final String localeCode;

    private Lang(YamlConfiguration yaml, MiniMessage mini, String localeCode) {
        this.yaml = yaml;
        this.mini = mini;
        this.localeCode = localeCode;
    }

    public static Lang load(JavaPlugin plugin, String code, MiniMessage mini) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        File langFile = new File(langDir, code + ".yml");
        if (!langFile.exists()) {
            try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
                if (in != null) {
                    Files.copy(in, langFile.toPath());
                } else {
                    try (InputStream fallback = plugin.getResource("lang/en_us.yml")) {
                        if (fallback != null) {
                            Files.copy(fallback, langFile.toPath());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (!langFile.exists()) {
            langFile = new File(langDir, "en_us.yml");
            if (!langFile.exists()) {
                return new Lang(new YamlConfiguration(), mini, code);
            }
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(langFile);
        return new Lang(yaml, mini, code);
    }

    public Component component(String key, TagResolver... resolvers) {
        String raw = yaml.getString(key);
        if (raw == null) {
            raw = net.uebliche.hub.common.i18n.I18n.raw(localeCode, key);
        }
        return mini.deserialize(raw, resolvers);
    }

    public List<Component> list(String key, TagResolver... resolvers) {
        List<String> rawList = yaml.getStringList(key);
        if (rawList == null || rawList.isEmpty()) {
            return List.of(component(key, resolvers));
        }
        return rawList.stream().map(line -> mini.deserialize(line, resolvers)).toList();
    }

    public List<String> rawList(String key) {
        List<String> list = yaml.getStringList(key);
        return list == null ? Collections.emptyList() : list;
    }
}
