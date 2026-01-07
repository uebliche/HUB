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
                registerLocale(locale, map, true);
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized boolean reloadFromClasspath(String locale) {
        if (locale == null || locale.isBlank()) {
            return false;
        }
        String normalized = normalizeLocale(locale);
        try (var in = I18n.class.getClassLoader().getResourceAsStream("i18n/" + normalized + ".json")) {
            if (in == null) {
                return false;
            }
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> map = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
            if (map != null) {
                registerLocale(normalized, map, true);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static synchronized void registerLocale(String locale, Map<String, String> entries, boolean replace) {
        if (locale == null || locale.isBlank() || entries == null) {
            return;
        }
        String key = normalizeLocale(locale);
        Map<String, String> base = replace ? new HashMap<>() : new HashMap<>(TRANSLATIONS.getOrDefault(key, new HashMap<>()));
        entries.forEach((entryKey, value) -> {
            if (entryKey != null && value != null) {
                base.put(entryKey, value);
            }
        });
        TRANSLATIONS.put(key, base);
    }

    public static synchronized void registerLocale(String locale, Map<String, String> entries) {
        registerLocale(locale, entries, false);
    }

    public static synchronized void clearLocale(String locale) {
        if (locale == null) {
            return;
        }
        TRANSLATIONS.remove(normalizeLocale(locale));
    }

    public static String normalizeLocale(String locale) {
        if (locale == null) {
            return FALLBACK;
        }
        return locale.trim().toLowerCase().replace('-', '_');
    }

    private static Map<String, String> localeMap(String locale) {
        if (locale != null) {
            String lc = normalizeLocale(locale);
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
