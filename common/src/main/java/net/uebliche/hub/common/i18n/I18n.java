package net.uebliche.hub.common.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Simple JSON-based i18n loader shared across loaders.
 * Files are expected at /i18n/<locale>.json on the classpath.
 */
public final class I18n {
    private static final Map<String, Map<String, String>> TRANSLATIONS;
    private static final String FALLBACK = "en_us";

    static {
        TRANSLATIONS = new HashMap<>();
        loadLocale("en_us");
        loadLocale("de_de");
    }

    private static void loadLocale(String locale) {
        try (var in = I18n.class.getClassLoader().getResourceAsStream("i18n/" + locale + ".json")) {
            if (in == null) return;
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> map = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
            if (map != null) {
                TRANSLATIONS.put(locale.toLowerCase(), map);
            }
        } catch (Exception ignored) {
        }
    }

    private static Map<String, String> localeMap(String locale) {
        if (locale != null) {
            String lc = locale.toLowerCase();
            if (TRANSLATIONS.containsKey(lc)) {
                return TRANSLATIONS.get(lc);
            }
        }
        return TRANSLATIONS.getOrDefault(FALLBACK, Collections.emptyMap());
    }

    public static String raw(String locale, String key) {
        Map<String, String> map = localeMap(locale);
        String val = map.get(key);
        if (val != null) return val;
        map = localeMap(FALLBACK);
        return map.getOrDefault(key, "<red>" + key);
    }

    public static Component component(String locale, MiniMessage mini, String key, TagResolver... resolvers) {
        Objects.requireNonNull(mini, "MiniMessage");
        String raw = raw(locale, key);
        return mini.deserialize(raw, resolvers);
    }
}
