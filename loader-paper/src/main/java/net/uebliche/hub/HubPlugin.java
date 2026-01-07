package net.uebliche.hub;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.ArmorStand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.uebliche.hub.common.HubEntrypoint;
import net.uebliche.hub.common.i18n.I18n;
import net.uebliche.hub.common.model.LobbyNpcSpec;
import net.uebliche.hub.common.model.LobbySignSpec;
import net.uebliche.hub.common.model.NavigatorEntrySpec;
import net.uebliche.hub.common.storage.JumpRunScoreRepository;
import net.uebliche.hub.common.storage.PlayerLocation;
import net.uebliche.hub.common.storage.PlayerLocationRepository;
import net.uebliche.hub.common.storage.StorageBackendType;
import net.uebliche.hub.common.storage.StorageLogger;
import net.uebliche.hub.common.storage.StorageManager;
import net.uebliche.hub.common.storage.StorageSettings;
import net.uebliche.hub.common.update.UpdateChecker;
import net.uebliche.hub.metrics.Metrics;

public class HubPlugin extends JavaPlugin implements Listener, HubEntrypoint {

    private static final String CHANNEL = "uebliche:hub";
    private static final NamespacedKey DATA_KEY = NamespacedKey.minecraft("hub_lobby_slot");
    private static final NamespacedKey COMPASS_KEY = NamespacedKey.minecraft("hub_compass");
    private static final NamespacedKey NAVIGATOR_KEY = NamespacedKey.minecraft("hub_navigator");
    private static final NamespacedKey NPC_KEY = NamespacedKey.minecraft("hub_lobby_npc");
    private static final NamespacedKey NPC_TEXT_KEY = NamespacedKey.minecraft("hub_lobby_npc_text");
    static final MiniMessage MINI = MiniMessage.miniMessage();

    private boolean compassEnabled;
    private Component guiTitle;
    private Component compassName;
    private List<Component> compassLore;
    private String entryNameTemplate;
    private List<String> entryLoreTemplate;
    private Material compassMaterial;
    private int compassSlot;
    private long cacheTtlMillis;
    private boolean allowMove;
    private boolean allowDrop;
    private boolean dropOnDeath;
    private boolean restoreOnRespawn;
    private boolean navigatorEnabled;
    private Component navigatorTitle;
    private Component navigatorName;
    private List<Component> navigatorLore;
    private Material navigatorMaterial;
    private int navigatorSlot;
    private int navigatorRows;
    private boolean navigatorAllowMove;
    private boolean navigatorAllowDrop;
    private boolean navigatorDropOnDeath;
    private boolean navigatorRestoreOnRespawn;
    private List<NavigatorEntry> navigatorEntries = new ArrayList<>();
    private boolean navigatorOpenLobbyRightClick = false;
    private boolean navigatorConfigOpenLobbyRightClick() {
        return navigatorOpenLobbyRightClick;
    }
    private boolean disableDamage;
    private boolean disableHunger;
    private boolean healOnJoin;
    private boolean spawnCommandEnabled;
    private boolean spawnTeleportEnabled;
    private String spawnWorld;
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private float spawnYaw;
    private float spawnPitch;
    private JoinTeleportMode joinTeleportMode = JoinTeleportMode.NONE;
    private long joinTeleportDelayTicks;
    private long joinTeleportLastMaxAgeMillis;
    private boolean lobbyNpcsEnabled;
    private boolean lobbySignsEnabled;
    private List<LobbyNpcSpec> lobbyNpcSpecs = List.of();
    private List<LobbySignSpec> lobbySignSpecs = List.of();
    private final Map<String, LobbyNpcSpec> lobbyNpcById = new ConcurrentHashMap<>();
    private final Map<String, Entity> lobbyNpcEntities = new ConcurrentHashMap<>();
    private final Map<String, LobbySignSpec> lobbySignByKey = new ConcurrentHashMap<>();
    private volatile List<LobbyEntry> lastLobbyList = List.of();
    private boolean jumpRunEnabled;
    private String jumpRunCourseId = "default";
    private String jumpRunWorld = "world";
    private int jumpRunBlocks = 20;
    private double jumpRunMinDistance = 2.5;
    private double jumpRunMaxDistance = 4.5;
    private int jumpRunMinYOffset = -1;
    private int jumpRunMaxYOffset = 2;
    private int jumpRunMinY = 70;
    private int jumpRunMaxY = 140;
    private Material jumpRunBlock = Material.QUARTZ_BLOCK;
    private Material jumpRunStartBlock = Material.EMERALD_BLOCK;
    private Material jumpRunFinishBlock = Material.GOLD_BLOCK;
    private boolean jumpRunTeleportOnStart = true;
    private final Map<UUID, Long> jumpRunStarts = new ConcurrentHashMap<>();
    private final List<JumpRunBlockPos> jumpRunPositions = new ArrayList<>();
    private JumpRunBlockPos jumpRunStartPos;
    private JumpRunBlockPos jumpRunFinishPos;

    private final Map<UUID, List<LobbyEntry>> lobbyCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lobbyCacheAge = new ConcurrentHashMap<>();
    private final Set<UUID> pendingOpen = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Map<Integer, LobbyEntry>> openInventories = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, NavigatorEntry>> openNavigatorInventories = new ConcurrentHashMap<>();
    private StorageManager storageManager;
    private PlayerLocationRepository locationRepository;
    private JumpRunScoreRepository scoreRepository;
    private String storageServerId = "lobby";
    private String i18nDefaultLocale = "en_us";
    private boolean i18nUseClientLocale = true;
    private Map<String, Map<String, String>> i18nOverrides = new HashMap<>();
    private Lang langEn;
    private Metrics metrics;

    @Override
    public void loadConfig() {
        reloadConfig();
        i18nDefaultLocale = I18n.normalizeLocale(getConfig().getString("i18n.default-locale", "en_us"));
        i18nUseClientLocale = getConfig().getBoolean("i18n.use-client-locale", true);
        i18nOverrides = loadI18nOverrides();
        loadExternalI18n();
        langEn = Lang.load(this, i18nDefaultLocale, MINI, i18nOverrides.get(i18nDefaultLocale));
        compassEnabled = getConfig().getBoolean("compass.enabled", true);
        guiTitle = MINI.deserialize(getConfig().getString("compass.gui-title", "<aqua>Select Lobby"));
        compassName = MINI.deserialize(getConfig().getString("compass.item.name", "<gold>Lobby Compass"));
        compassLore = getConfig().getStringList("compass.item.lore").stream()
                .map(MINI::deserialize)
                .toList();
        if (compassLore.isEmpty()) {
            compassLore = langEn.list("compass.default-lore", Placeholder.unparsed("lobby", ""), Placeholder.unparsed("server", ""));
        }
        entryNameTemplate = getConfig().getString("compass.list-item.name", "<gold><lobby>");
        entryLoreTemplate = getConfig().getStringList("compass.list-item.lore");
        if (entryLoreTemplate.isEmpty()) {
            entryLoreTemplate = langEn.rawList("compass.list-item.default-lore");
        }
        String materialName = getConfig().getString("compass.item.material", "COMPASS");
        Material material = Material.matchMaterial(materialName);
        compassMaterial = material != null ? material : Material.COMPASS;
        compassSlot = Math.max(0, getConfig().getInt("compass.item.slot", 0));
        cacheTtlMillis = Math.max(1000L, getConfig().getLong("compass.cache-ttl-millis", 10_000L));
        allowMove = getConfig().getBoolean("compass.item.allow-move", false);
        allowDrop = getConfig().getBoolean("compass.item.allow-drop", false);
        dropOnDeath = getConfig().getBoolean("compass.item.drop-on-death", true);
        restoreOnRespawn = getConfig().getBoolean("compass.item.restore-on-respawn", true);

        navigatorEnabled = getConfig().getBoolean("navigator.enabled", true);
        navigatorTitle = MINI.deserialize(getConfig().getString("navigator.gui-title", "<aqua>Navigator"));
        navigatorName = MINI.deserialize(getConfig().getString("navigator.item.name", "<gold>Navigator"));
        navigatorLore = getConfig().getStringList("navigator.item.lore").stream()
                .map(MINI::deserialize)
                .toList();
        if (navigatorLore.isEmpty()) {
            navigatorLore = List.of(MINI.deserialize("<gray>Open navigator"));
        }
        Material navMat = Material.matchMaterial(getConfig().getString("navigator.item.material", "COMPASS"));
        navigatorMaterial = navMat != null ? navMat : Material.COMPASS;
        navigatorSlot = Math.max(0, getConfig().getInt("navigator.item.slot", 4));
        navigatorRows = Math.max(0, Math.min(6, getConfig().getInt("navigator.gui-rows", 0)));
        navigatorAllowMove = getConfig().getBoolean("navigator.item.allow-move", false);
        navigatorAllowDrop = getConfig().getBoolean("navigator.item.allow-drop", false);
        navigatorDropOnDeath = getConfig().getBoolean("navigator.item.drop-on-death", true);
        navigatorRestoreOnRespawn = getConfig().getBoolean("navigator.item.restore-on-respawn", true);
        navigatorEntries = loadNavigatorEntries();
        navigatorOpenLobbyRightClick = getConfig().getBoolean("navigator.open-lobby-selector-on-right-click", false);

        disableDamage = getConfig().getBoolean("gameplay.disable-damage", true);
        disableHunger = getConfig().getBoolean("gameplay.disable-hunger", true);
        healOnJoin = getConfig().getBoolean("gameplay.heal-on-join", true);
        spawnCommandEnabled = getConfig().getBoolean("gameplay.spawn-command", true);
        spawnTeleportEnabled = getConfig().getBoolean("spawn-teleport.enabled", false);
        spawnWorld = getConfig().getString("spawn-teleport.world", "world");
        spawnX = getConfig().getDouble("spawn-teleport.x", 0);
        spawnY = getConfig().getDouble("spawn-teleport.y", 64);
        spawnZ = getConfig().getDouble("spawn-teleport.z", 0);
        spawnYaw = (float) getConfig().getDouble("spawn-teleport.yaw", 0);
        spawnPitch = (float) getConfig().getDouble("spawn-teleport.pitch", 0);
        if (getConfig().contains("join-teleport.mode")) {
            String joinTeleportRaw = getConfig().getString("join-teleport.mode", "none");
            joinTeleportMode = parseJoinTeleportMode(joinTeleportRaw);
        } else {
            joinTeleportMode = spawnTeleportEnabled ? JoinTeleportMode.SPAWN : JoinTeleportMode.NONE;
        }
        long delaySeconds = Math.max(0L, getConfig().getLong("join-teleport.delay-seconds", 0L));
        joinTeleportDelayTicks = delaySeconds * 20L;
        long maxAgeSeconds = Math.max(0L, getConfig().getLong("join-teleport.last-location.max-age-seconds", 0L));
        joinTeleportLastMaxAgeMillis = maxAgeSeconds <= 0 ? 0L : maxAgeSeconds * 1000L;
        lobbyNpcsEnabled = getConfig().getBoolean("lobby-npcs.enabled", false);
        lobbySignsEnabled = getConfig().getBoolean("lobby-signs.enabled", false);
        lobbyNpcSpecs = loadLobbyNpcSpecs();
        lobbySignSpecs = loadLobbySignSpecs();
        reloadLobbyDisplays();
        initStorage();
        loadJumpRunConfig();
        if (jumpRunEnabled) {
            generateJumpRunCourse(false);
        }
    }

    private Map<String, Map<String, String>> loadI18nOverrides() {
        Map<String, Map<String, String>> result = new HashMap<>();
        ConfigurationSection root = getConfig().getConfigurationSection("i18n.overrides");
        if (root == null) {
            return result;
        }
        for (String locale : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(locale);
            if (section == null) {
                continue;
            }
            Map<String, String> entries = new HashMap<>();
            for (String key : section.getKeys(true)) {
                Object value = section.get(key);
                if (value instanceof String str) {
                    entries.put(key, str);
                } else if (value instanceof List<?> list) {
                    List<String> lines = list.stream().map(String::valueOf).toList();
                    entries.put(key, String.join("\n", lines));
                }
            }
            if (!entries.isEmpty()) {
                result.put(I18n.normalizeLocale(locale), entries);
            }
        }
        return result;
    }

    private void loadExternalI18n() {
        I18n.reloadFromClasspath("en_us");
        I18n.reloadFromClasspath("de_de");
        File dir = new File(getDataFolder(), "i18n");
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            if (dot <= 0) {
                continue;
            }
            String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            String locale = name.substring(0, dot);
            Map<String, String> entries = switch (ext) {
                case "yml", "yaml" -> readYamlLocale(file);
                case "json" -> readJsonLocale(file.toPath());
                default -> null;
            };
            if (entries != null && !entries.isEmpty()) {
                I18n.registerLocale(locale, entries);
            }
        }
    }

    private Map<String, String> readYamlLocale(File file) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            Map<String, String> entries = new HashMap<>();
            for (String key : yaml.getKeys(true)) {
                Object value = yaml.get(key);
                if (value instanceof String str) {
                    entries.put(key, str);
                } else if (value instanceof List<?> list) {
                    List<String> lines = list.stream().map(String::valueOf).toList();
                    entries.put(key, String.join("\n", lines));
                }
            }
            return entries;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Map<String, String> readJsonLocale(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> entries = new Gson().fromJson(reader, type);
            return entries == null ? Map.of() : entries;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private void loadJumpRunConfig() {
        jumpRunEnabled = getConfig().getBoolean("jump-run.enabled", false);
        jumpRunCourseId = getConfig().getString("jump-run.course-id", "default");
        jumpRunWorld = getConfig().getString("jump-run.world", spawnWorld != null && !spawnWorld.isBlank() ? spawnWorld : "world");
        jumpRunBlocks = Math.max(3, getConfig().getInt("jump-run.blocks", 20));
        jumpRunMinDistance = Math.max(1.0, getConfig().getDouble("jump-run.spacing.min", 2.5));
        jumpRunMaxDistance = Math.max(jumpRunMinDistance, getConfig().getDouble("jump-run.spacing.max", 4.5));
        jumpRunMinYOffset = getConfig().getInt("jump-run.y-offset.min", -1);
        jumpRunMaxYOffset = getConfig().getInt("jump-run.y-offset.max", 2);
        jumpRunMinY = getConfig().getInt("jump-run.height.min", 70);
        jumpRunMaxY = getConfig().getInt("jump-run.height.max", 140);
        jumpRunTeleportOnStart = getConfig().getBoolean("jump-run.teleport-on-start", true);
        jumpRunBlock = resolveJumpRunMaterial(getConfig().getString("jump-run.block", "QUARTZ_BLOCK"), Material.QUARTZ_BLOCK);
        jumpRunStartBlock = resolveJumpRunMaterial(getConfig().getString("jump-run.start-block", "EMERALD_BLOCK"), Material.EMERALD_BLOCK);
        jumpRunFinishBlock = resolveJumpRunMaterial(getConfig().getString("jump-run.finish-block", "GOLD_BLOCK"), Material.GOLD_BLOCK);
    }

    private Material resolveJumpRunMaterial(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        return material != null ? material : fallback;
    }

    private void initStorage() {
        StorageSettings settings = buildStorageSettings();
        storageServerId = settings.serverId();
        if (storageManager != null) {
            storageManager.close();
        }
        storageManager = StorageManager.create(settings, new StorageLogger() {
            @Override
            public void info(String message) {
                getLogger().info(message);
            }

            @Override
            public void warn(String message) {
                getLogger().warning(message);
            }

            @Override
            public void error(String message, Throwable error) {
                getLogger().log(Level.SEVERE, message, error);
            }
        });
        locationRepository = storageManager.locations();
        scoreRepository = storageManager.scores();
    }

    private StorageSettings buildStorageSettings() {
        String serverId = getConfig().getString("storage.server-id", "lobby");
        StorageBackendType scorePrimary = StorageBackendType.fromString(getConfig().getString("storage.scores.primary", "local-sql"));
        StorageBackendType locationPrimary = StorageBackendType.fromString(getConfig().getString("storage.locations.primary", "local-sql"));
        StorageBackendType locationCache = StorageBackendType.fromString(getConfig().getString("storage.locations.cache", "none"));
        boolean fallbackEnabled = getConfig().getBoolean("storage.fallback.enabled", true);

        String sqlUrl = getConfig().getString("storage.sql.url", "jdbc:mariadb://localhost:3306/hub");
        String sqlUser = getConfig().getString("storage.sql.user", "hub");
        String sqlPassword = getConfig().getString("storage.sql.password", "");
        String sqlDriver = getConfig().getString("storage.sql.driver", "org.mariadb.jdbc.Driver");
        StorageSettings.SqlSettings sql = new StorageSettings.SqlSettings(sqlUrl, sqlUser, sqlPassword, sqlDriver);

        String localUrlDefault = "jdbc:sqlite:" + new File(getDataFolder(), "hub.db").getAbsolutePath();
        String localUrl = getConfig().getString("storage.local-sql.url", localUrlDefault);
        String localDriver = getConfig().getString("storage.local-sql.driver", "org.sqlite.JDBC");
        StorageSettings.SqlSettings localSql = new StorageSettings.SqlSettings(localUrl, "", "", localDriver);

        String mongoUri = getConfig().getString("storage.mongo.uri", "mongodb://localhost:27017");
        String mongoDatabase = getConfig().getString("storage.mongo.database", "hub");
        String mongoPrefix = getConfig().getString("storage.mongo.collection-prefix", "hub_");
        StorageSettings.MongoSettings mongo = new StorageSettings.MongoSettings(mongoUri, mongoDatabase, mongoPrefix);

        String redisUri = getConfig().getString("storage.redis.uri", "redis://localhost:6379");
        int redisDb = getConfig().getInt("storage.redis.database", 0);
        String redisPrefix = getConfig().getString("storage.redis.key-prefix", "hub");
        StorageSettings.RedisSettings redis = new StorageSettings.RedisSettings(redisUri, redisDb, redisPrefix);

        return new StorageSettings(
                serverId,
                new StorageSettings.ScoreRouting(scorePrimary),
                new StorageSettings.LocationRouting(locationPrimary, locationCache),
                sql,
                localSql,
                mongo,
                redis,
                fallbackEnabled
        );
    }

    private List<NavigatorEntry> loadNavigatorEntries() {
        List<NavigatorEntry> entries = new ArrayList<>();
        ConfigurationSection section = getConfig().getConfigurationSection("navigator.entries");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                String nameRaw = entry.getString("name", "<gold>" + key);
                List<Component> lore = entry.getStringList("lore").stream().map(MINI::deserialize).toList();
                Material icon = Optional.ofNullable(Material.matchMaterial(entry.getString("icon", "COMPASS")))
                        .orElse(Material.COMPASS);
                String actionStr = entry.getString("action", "teleport").toUpperCase(Locale.ROOT);
                NavigatorAction action = switch (actionStr) {
                    case "SERVER" -> NavigatorAction.SERVER;
                    case "LOBBY" -> NavigatorAction.LOBBY_SELECTOR;
                    default -> NavigatorAction.TELEPORT;
                };
                String server = entry.getString("server", "");
                String world = entry.getString("world", "world");
                double x = entry.getDouble("x", 0);
                double y = entry.getDouble("y", 64);
                double z = entry.getDouble("z", 0);
                float yaw = (float) entry.getDouble("yaw", 0);
                float pitch = (float) entry.getDouble("pitch", 0);
                int slot = entry.getInt("slot", -1);
                entries.add(new NavigatorEntry(MINI.deserialize(nameRaw), lore, icon, action, server, world, x, y, z, yaw, pitch, slot));
            }
        }
        if (entries.isEmpty()) {
            entries.add(new NavigatorEntry(
                    MINI.deserialize("<gold>Spawn"),
                    List.of(MINI.deserialize("<gray>Teleport to spawn")),
                    Material.COMPASS,
                    NavigatorAction.TELEPORT,
                    "",
                    "world", 0, 64, 0, 0f, 0f, -1
            ));
        }
        return entries;
    }

    private List<LobbyNpcSpec> loadLobbyNpcSpecs() {
        List<LobbyNpcSpec> specs = new ArrayList<>();
        ConfigurationSection section = getConfig().getConfigurationSection("lobby-npcs.entries");
        if (section == null) {
            return specs;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String id = entry.getString("id", key);
            String world = entry.getString("world", "world");
            double x = entry.getDouble("x", 0);
            double y = entry.getDouble("y", 64);
            double z = entry.getDouble("z", 0);
            float yaw = (float) entry.getDouble("yaw", 0);
            float pitch = (float) entry.getDouble("pitch", 0);
            String name = entry.getString("name", "<gold>Lobby");
            List<String> lines = entry.getStringList("lines");
            String entity = entry.getString("entity", "VILLAGER");
            NavigatorEntrySpec.NavigatorAction action = parseLobbyAction(entry.getString("action", "server"));
            String server = entry.getString("server", "");
            String targetWorld = entry.getString("target.world", world);
            double targetX = entry.getDouble("target.x", x);
            double targetY = entry.getDouble("target.y", y);
            double targetZ = entry.getDouble("target.z", z);
            float targetYaw = (float) entry.getDouble("target.yaw", yaw);
            float targetPitch = (float) entry.getDouble("target.pitch", pitch);
            specs.add(new LobbyNpcSpec(id, world, x, y, z, yaw, pitch, name, lines, entity, action, server,
                    targetWorld, targetX, targetY, targetZ, targetYaw, targetPitch));
        }
        return specs;
    }

    private List<LobbySignSpec> loadLobbySignSpecs() {
        List<LobbySignSpec> specs = new ArrayList<>();
        ConfigurationSection section = getConfig().getConfigurationSection("lobby-signs.entries");
        if (section == null) {
            return specs;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String id = entry.getString("id", key);
            String world = entry.getString("world", "world");
            int x = entry.getInt("x", 0);
            int y = entry.getInt("y", 64);
            int z = entry.getInt("z", 0);
            List<String> lines = entry.getStringList("lines");
            NavigatorEntrySpec.NavigatorAction action = parseLobbyAction(entry.getString("action", "server"));
            String server = entry.getString("server", "");
            String targetWorld = entry.getString("target.world", world);
            double targetX = entry.getDouble("target.x", x);
            double targetY = entry.getDouble("target.y", y);
            double targetZ = entry.getDouble("target.z", z);
            float targetYaw = (float) entry.getDouble("target.yaw", 0);
            float targetPitch = (float) entry.getDouble("target.pitch", 0);
            specs.add(new LobbySignSpec(id, world, x, y, z, lines, action, server,
                    targetWorld, targetX, targetY, targetZ, targetYaw, targetPitch));
        }
        return specs;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        setupMetrics();
        registerItems();
        registerMenus();
        registerGameplayGuards();
        registerTransport();
        Bukkit.getScheduler().runTaskAsynchronously(this,
                () -> UpdateChecker.checkModrinth(net.uebliche.hub.common.update.UpdateChecker.MODRINTH_PROJECT_ID,
                        getDescription().getVersion(),
                        msg -> getLogger().info(msg)));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, (channel, player, bytes) -> handleIncoming(player.getUniqueId(), bytes));
        var cfgCmd = new ConfigCommand(this);
        getServer().getCommandMap().register("hub", cfgCmd);
        if (spawnCommandEnabled) {
            getServer().getCommandMap().register("hub", new SpawnCommand(this));
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        getServer().getOnlinePlayers().forEach(this::savePlayerLocation);
        lobbyCache.clear();
        lobbyCacheAge.clear();
        pendingOpen.clear();
        openInventories.clear();
        openNavigatorInventories.clear();
        removeLobbyNpcs();
        lobbyNpcById.clear();
        lobbySignByKey.clear();
        if (metrics != null) {
            metrics.shutdown();
        }
        if (storageManager != null) {
            storageManager.close();
        }
    }

    private void setupMetrics() {
        boolean enabled = getConfig().getBoolean("bstats.enabled", false);
        int serviceId = getConfig().getInt("bstats.service-id", 0);
        if (!enabled || serviceId <= 0) {
            return;
        }
        metrics = new Metrics(this, serviceId);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (compassEnabled) {
            giveCompass(event.getPlayer());
        }
        if (navigatorEnabled) {
            giveNavigator(event.getPlayer());
        }
        if (healOnJoin) {
            resetVitals(event.getPlayer());
        }
        scheduleJoinTeleport(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        savePlayerLocation(event.getPlayer());
        jumpRunStarts.remove(event.getPlayer().getUniqueId());
        removeHubItems(event.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!jumpRunEnabled || jumpRunFinishPos == null) {
            return;
        }
        UUID playerId = event.getPlayer().getUniqueId();
        Long startedAt = jumpRunStarts.get(playerId);
        if (startedAt == null) {
            return;
        }
        var to = event.getTo();
        if (to == null || to.getWorld() == null) {
            return;
        }
        if (!to.getWorld().getName().equalsIgnoreCase(jumpRunFinishPos.world())) {
            return;
        }
        int bx = to.getBlockX();
        int by = to.getBlockY();
        int bz = to.getBlockZ();
        if (isFinishBlock(bx, by, bz) || isFinishBlock(bx, by - 1, bz)) {
            completeJumpRun(event.getPlayer(), startedAt);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        var item = event.getItem();
        var player = event.getPlayer();
        if (isCompass(item)) {
            event.setCancelled(true);
            if (!hasFreshCache(player.getUniqueId())) {
                requestLobbyList(player.getUniqueId());
                pendingOpen.add(player.getUniqueId());
                return;
            }
            openSelector(player.getUniqueId());
            return;
        }
        if (isNavigator(item)) {
            event.setCancelled(true);
            openNavigator(player);
            return;
        }
        var clicked = event.getClickedBlock();
        if (clicked != null && lobbySignsEnabled) {
            LobbySignSpec signSpec = lobbySignByKey.get(signKey(clicked.getWorld().getUID().toString(),
                    clicked.getX(), clicked.getY(), clicked.getZ()));
            if (signSpec != null) {
                event.setCancelled(true);
                handleLobbyAction(player, signSpec.action(), signSpec.server(), signSpec.targetWorld(),
                        signSpec.targetX(), signSpec.targetY(), signSpec.targetZ(),
                        signSpec.targetYaw(), signSpec.targetPitch());
            }
        }
    }

    @EventHandler
    public void onPlayerInteractNpc(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        var id = entity.getPersistentDataContainer().get(NPC_KEY, PersistentDataType.STRING);
        if (id == null) {
            return;
        }
        LobbyNpcSpec spec = lobbyNpcById.get(id);
        if (spec == null) {
            return;
        }
        event.setCancelled(true);
        handleLobbyAction(event.getPlayer(), spec.action(), spec.server(), spec.targetWorld(),
                spec.targetX(), spec.targetY(), spec.targetZ(), spec.targetYaw(), spec.targetPitch());
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        var stack = event.getItemDrop().getItemStack();
        if ((!allowDrop && isCompass(stack)) || (!navigatorAllowDrop && isNavigator(stack))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getDrops().isEmpty()) {
            return;
        }
        event.getDrops().removeIf(item -> (!dropOnDeath && isCompass(item)) || (!navigatorDropOnDeath && isNavigator(item)));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        var player = event.getPlayer();
        if (compassEnabled && restoreOnRespawn) {
            giveCompass(player);
        }
        if (navigatorEnabled && navigatorRestoreOnRespawn) {
            giveNavigator(player);
        }
        if (healOnJoin) {
            resetVitals(player);
        }
        teleportToSpawn(player, false);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        var cursor = event.getOldCursor();
        if ((!allowMove && isCompass(cursor)) || (!navigatorAllowMove && isNavigator(cursor))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (disableDamage && event.getEntity() instanceof org.bukkit.entity.Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        if (disableHunger) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setExhaustion(0f);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() != null && event.getView().title() != null && guiTitle.equals(event.getView().title())) {
            event.setCancelled(true);
            var player = event.getWhoClicked();
            var mapping = openInventories.get(player.getUniqueId());
            if (mapping == null) {
                return;
            }
            var entry = mapping.get(event.getRawSlot());
            if (entry == null) {
                return;
            }
            player.closeInventory();
            if (player instanceof org.bukkit.entity.Player bukkitPlayer) {
                String currentServer = getServer().getName();
                if (currentServer != null && currentServer.equalsIgnoreCase(entry.server())) {
                    bukkitPlayer.sendMessage(resolveLang(bukkitPlayer).component("compass.connecting", Placeholder.unparsed("lobby", entry.lobby())));
                    return;
                }
                sendConnectRequest(bukkitPlayer.getUniqueId(), entry.lobby());
                bukkitPlayer.sendMessage(resolveLang(bukkitPlayer).component("compass.connecting", Placeholder.unparsed("lobby", entry.lobby())));
            }
            return;
        }

        if (event.getView() != null && event.getView().title() != null && navigatorTitle.equals(event.getView().title())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof org.bukkit.entity.Player p) {
                var mapping = openNavigatorInventories.get(p.getUniqueId());
                if (mapping != null) {
                    var navEntry = mapping.get(event.getRawSlot());
                    if (navEntry != null) {
                        if (navigatorConfigOpenLobbyRightClick() && event.getClick().isRightClick()) {
                            UUID id = p.getUniqueId();
                            if (!hasFreshCache(id)) {
                                requestLobbyList(id);
                                pendingOpen.add(id);
                            } else {
                                openSelector(id);
                            }
                            p.closeInventory();
                        } else {
                            handleNavigatorAction(p, navEntry);
                        }
                    }
                }
            }
            return;
        }

        if ((event.getWhoClicked() instanceof org.bukkit.entity.Player p)) {
            boolean containsCompass = isCompass(event.getCurrentItem())
                    || isCompass(event.getCursor());
            if (event.getHotbarButton() >= 0) {
                ItemStack hotbarItem = p.getInventory().getItem(event.getHotbarButton());
                containsCompass = containsCompass || isCompass(hotbarItem);
            }
            switch (event.getClick()) {
                case SWAP_OFFHAND -> containsCompass = containsCompass
                        || isCompass(p.getInventory().getItemInOffHand())
                        || isCompass(event.getCurrentItem());
                default -> {
                }
            }
            boolean containsNavigator = isNavigator(event.getCurrentItem())
                    || isNavigator(event.getCursor());
            if (event.getHotbarButton() >= 0) {
                ItemStack hotbarItem = p.getInventory().getItem(event.getHotbarButton());
                containsNavigator = containsNavigator || isNavigator(hotbarItem);
            }
            switch (event.getClick()) {
                case SWAP_OFFHAND -> containsNavigator = containsNavigator
                        || isNavigator(p.getInventory().getItemInOffHand())
                        || isNavigator(event.getCurrentItem());
                default -> {
                }
            }
            if ((!allowMove && containsCompass) || (!navigatorAllowMove && containsNavigator)) {
                event.setCancelled(true);
            }
        }
    }

    private void giveCompass(org.bukkit.entity.Player player) {
        ItemStack existing = player.getInventory().getItem(Math.max(0, Math.min(compassSlot, player.getInventory().getSize() - 1)));
        if (isCompass(existing)) {
            return;
        }
        ItemStack compass = new ItemStack(compassMaterial);
        ItemMeta meta = compass.getItemMeta();
        meta.displayName(compassName);
        meta.lore(compassLore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(COMPASS_KEY, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        int slot = Math.max(0, Math.min(compassSlot, player.getInventory().getSize() - 1));
        player.getInventory().setItem(slot, compass);
    }

    private void giveNavigator(org.bukkit.entity.Player player) {
        ItemStack existing = player.getInventory().getItem(Math.max(0, Math.min(navigatorSlot, player.getInventory().getSize() - 1)));
        if (isNavigator(existing)) {
            return;
        }
        ItemStack navigator = new ItemStack(navigatorMaterial);
        ItemMeta meta = navigator.getItemMeta();
        meta.displayName(navigatorName);
        meta.lore(navigatorLore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(NAVIGATOR_KEY, PersistentDataType.BYTE, (byte) 1);
        navigator.setItemMeta(meta);
        int slot = Math.max(0, Math.min(navigatorSlot, player.getInventory().getSize() - 1));
        player.getInventory().setItem(slot, navigator);
    }

    private void removeHubItems(org.bukkit.entity.Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isCompass(item) || isNavigator(item)) {
                inv.clear(i);
            }
        }
        ItemStack offhand = inv.getItemInOffHand();
        if (isCompass(offhand) || isNavigator(offhand)) {
            inv.setItemInOffHand(null);
        }
    }

    private boolean hasFreshCache(UUID playerId) {
        var age = lobbyCacheAge.get(playerId);
        if (age == null) {
            return false;
        }
        return System.currentTimeMillis() - age < cacheTtlMillis;
    }

    private void requestLobbyList(UUID playerId) {
        var player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        ByteArrayDataOutput inner = ByteStreams.newDataOutput();
        inner.writeUTF("LIST");
        inner.writeUTF(playerId.toString());
        sendPayload(player, inner);
    }

    private void sendConnectRequest(UUID playerId, String lobby) {
        var player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        ByteArrayDataOutput inner = ByteStreams.newDataOutput();
        inner.writeUTF("CONNECT");
        inner.writeUTF(playerId.toString());
        inner.writeUTF(lobby);
        sendPayload(player, inner);
    }

    private void handleIncoming(UUID targetPlayer, byte[] data) {
        ByteArrayDataInput root = ByteStreams.newDataInput(data);
        ByteArrayDataInput in;
        String type;
        try {
            int len = readVarInt(root);
            byte[] payload = new byte[len];
            root.readFully(payload);
            in = ByteStreams.newDataInput(payload);
            type = in.readUTF();
        } catch (Exception ex) {
            return;
        }
        if ("LIST".equalsIgnoreCase(type)) {
            UUID playerId = targetPlayer;
            try {
                playerId = UUID.fromString(in.readUTF());
            } catch (Exception ignored) {
            }
            int count = in.readInt();
            List<LobbyEntry> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String lobby = in.readUTF();
                String server = in.readUTF();
                int online = in.readInt();
                int max = in.readInt();
                entries.add(new LobbyEntry(lobby, server, online, max));
            }
            lobbyCache.put(playerId, entries);
            lobbyCacheAge.put(playerId, System.currentTimeMillis());
            lastLobbyList = entries;
            if (lobbyNpcsEnabled) {
                refreshLobbyNpcDisplays();
            }
            if (pendingOpen.remove(playerId)) {
                openSelector(playerId);
            }
        }
    }

    private void sendPayload(org.bukkit.entity.Player player, ByteArrayDataOutput inner) {
        byte[] payload = inner.toByteArray();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        writeVarInt(out, payload.length);
        out.write(payload);
        player.sendPluginMessage(this, CHANNEL, out.toByteArray());
    }

    private int readVarInt(ByteArrayDataInput in) {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IllegalStateException("VarInt too big");
            }
        } while ((read & 0b10000000) != 0);
        return result;
    }

    private void writeVarInt(ByteArrayDataOutput out, int value) {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private void openSelector(UUID playerId) {
        var player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        var entries = lobbyCache.getOrDefault(playerId, List.of()).stream()
                .filter(e -> e.lobby() != null && !e.lobby().isBlank())
                .toList();
        if (entries.isEmpty()) {
            player.sendMessage(resolveLang(player).component("compass.none"));
            return;
        }
        int size = Math.min(54, Math.max(9, ((entries.size() - 1) / 9 + 1) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, guiTitle);
        Map<Integer, LobbyEntry> slotMap = new HashMap<>();
        int slot = 0;
        String currentServer = getServer().getName();
        for (LobbyEntry entry : entries) {
            if (slot >= size) {
                break;
            }
            boolean isCurrent = currentServer != null && currentServer.equalsIgnoreCase(entry.server());
            ItemStack item = new ItemStack(Material.LIGHT_BLUE_DYE);
            ItemMeta meta = item.getItemMeta();
                TagResolver resolver = TagResolver.resolver(
                        Placeholder.unparsed("lobby", entry.lobby()),
                        Placeholder.unparsed("server", entry.server()),
                        Placeholder.unparsed("online", String.valueOf(entry.online())),
                        Placeholder.unparsed("max", String.valueOf(entry.max())),
                        Placeholder.unparsed("current", isCurrent ? "(You)" : "")
                );
            Component name = MINI.deserialize(entryNameTemplate, resolver);
            if (isCurrent) {
                name = name.append(Component.space()).append(resolveLang(player).component("compass.current-tag"));
            }
            meta.displayName(name);
            List<Component> lore = entryLoreTemplate.stream()
                    .map(line -> MINI.deserialize(line, resolver))
                    .toList();
            if (isCurrent) {
                lore = new ArrayList<>(lore);
                lore.add(resolveLang(player).component("compass.current-lore"));
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, entry.lobby());
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            slotMap.put(slot, entry);
            slot++;
        }
        openInventories.put(playerId, slotMap);
        player.openInventory(inventory);
    }

    private void openNavigator(org.bukkit.entity.Player player) {
        if (!navigatorEnabled) {
            return;
        }
        if (navigatorEntries.isEmpty()) {
            player.sendMessage(resolveLang(player).component("navigator.none"));
            return;
        }
        int rows = navigatorRows > 0 ? navigatorRows : ((navigatorEntries.size() - 1) / 9 + 1);
        rows = Math.max(1, Math.min(6, rows));
        int size = rows * 9;
        Inventory inv = Bukkit.createInventory(null, size, navigatorTitle);
        Map<Integer, NavigatorEntry> mapping = new HashMap<>();
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }
        List<Integer> slotOrder = buildNavigatorSlots(rows, navigatorEntries.size());
        int orderIndex = 0;
        for (NavigatorEntry entry : navigatorEntries) {
            int slot = entry.slot();
            if (slot < 0 || slot >= size) {
                if (orderIndex >= slotOrder.size()) break;
                slot = slotOrder.get(orderIndex++);
            }
            ItemStack item = new ItemStack(entry.icon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(entry.name());
            meta.lore(entry.lore());
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            mapping.put(slot, entry);
        }
        openNavigatorInventories.put(player.getUniqueId(), mapping);
        player.openInventory(inv);
    }

    private List<Integer> buildNavigatorSlots(int rows, int count) {
        List<Integer> slots = new ArrayList<>();
        int startRow = rows > 2 ? 1 : 0;
        int endRow = rows > 2 ? rows - 2 : rows - 1;
        for (int r = startRow; r <= endRow && slots.size() < count; r++) {
            int cStart = rows > 2 ? 1 : 0;
            int cEnd = rows > 2 ? 7 : 8;
            for (int c = cStart; c <= cEnd && slots.size() < count; c++) {
                slots.add(r * 9 + c);
            }
        }
        return slots;
    }

    private NavigatorAction parseNavigatorAction(String raw) {
        if (raw == null) {
            return NavigatorAction.SERVER;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOBBY", "LOBBY_SELECTOR" -> NavigatorAction.LOBBY_SELECTOR;
            case "TELEPORT" -> NavigatorAction.TELEPORT;
            default -> NavigatorAction.SERVER;
        };
    }

    private NavigatorEntrySpec.NavigatorAction parseLobbyAction(String raw) {
        if (raw == null) {
            return NavigatorEntrySpec.NavigatorAction.SERVER;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOBBY", "LOBBY_SELECTOR" -> NavigatorEntrySpec.NavigatorAction.LOBBY_SELECTOR;
            case "TELEPORT" -> NavigatorEntrySpec.NavigatorAction.TELEPORT;
            default -> NavigatorEntrySpec.NavigatorAction.SERVER;
        };
    }

    private void handleNavigatorAction(org.bukkit.entity.Player player, NavigatorEntry entry) {
        if (entry.action() == NavigatorAction.SERVER && entry.server() != null && !entry.server().isBlank()) {
            sendConnectRequest(player.getUniqueId(), entry.server());
            player.sendMessage(resolveLang(player).component("navigator.connecting", Placeholder.unparsed("server", entry.server())));
            return;
        }
        if (entry.action() == NavigatorAction.LOBBY_SELECTOR) {
            UUID id = player.getUniqueId();
            if (!hasFreshCache(id)) {
                requestLobbyList(id);
                pendingOpen.add(id);
                return;
            }
            openSelector(id);
            return;
        }
        var world = Bukkit.getWorld(entry.world());
        if (world == null) {
            player.sendMessage(resolveLang(player).component("navigator.world-missing", Placeholder.unparsed("world", entry.world())));
            return;
        }
        player.teleport(new org.bukkit.Location(world, entry.x(), entry.y(), entry.z(), entry.yaw(), entry.pitch()));
        player.sendMessage(resolveLang(player).component("navigator.teleport", Placeholder.unparsed("world", entry.world())));
    }

    private void handleLobbyAction(org.bukkit.entity.Player player, NavigatorEntrySpec.NavigatorAction action, String server,
                                   String worldName, double x, double y, double z, float yaw, float pitch) {
        if (action == NavigatorEntrySpec.NavigatorAction.SERVER) {
            if (server == null || server.isBlank()) {
                return;
            }
            sendConnectRequest(player.getUniqueId(), server);
            player.sendMessage(resolveLang(player).component("navigator.connecting", Placeholder.unparsed("server", server)));
            return;
        }
        if (action == NavigatorEntrySpec.NavigatorAction.LOBBY_SELECTOR) {
            UUID id = player.getUniqueId();
            if (!hasFreshCache(id)) {
                requestLobbyList(id);
                pendingOpen.add(id);
                return;
            }
            openSelector(id);
            return;
        }
        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(resolveLang(player).component("navigator.world-missing", Placeholder.unparsed("world", worldName)));
            return;
        }
        player.teleport(new org.bukkit.Location(world, x, y, z, yaw, pitch));
        player.sendMessage(resolveLang(player).component("navigator.teleport", Placeholder.unparsed("world", worldName)));
    }

    private void reloadLobbyDisplays() {
        removeLobbyNpcs();
        removeLobbyNpcText();
        lobbyNpcById.clear();
        lobbyNpcEntities.clear();
        lobbySignByKey.clear();
        if (lobbySignsEnabled) {
            applyLobbySigns();
        }
        if (lobbyNpcsEnabled) {
            spawnLobbyNpcs();
        }
    }

    private void spawnLobbyNpcs() {
        for (LobbyNpcSpec spec : lobbyNpcSpecs) {
            var world = Bukkit.getWorld(spec.world());
            if (world == null) {
                continue;
            }
            EntityType type = resolveEntityType(spec.entity());
            var location = new org.bukkit.Location(world, spec.x(), spec.y(), spec.z(), spec.yaw(), spec.pitch());
            Entity entity = world.spawnEntity(location, type);
            LobbyEntry lobbyEntry = findLobbyEntry(spec);
            TagResolver resolver = npcResolver(spec, lobbyEntry);
            List<String> lines = spec.lines() != null ? spec.lines() : List.of();
            if (lines.isEmpty()) {
                entity.customName(MINI.deserialize(spec.name(), resolver));
                entity.setCustomNameVisible(true);
            } else {
                entity.customName(null);
                entity.setCustomNameVisible(false);
            }
            entity.setSilent(true);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            if (entity instanceof org.bukkit.entity.Mob mob) {
                mob.setAI(false);
                mob.setRemoveWhenFarAway(false);
            }
            entity.getPersistentDataContainer().set(NPC_KEY, PersistentDataType.STRING, spec.id());
            lobbyNpcById.put(spec.id(), spec);
            lobbyNpcEntities.put(spec.id(), entity);
            if (!lines.isEmpty()) {
                spawnNpcText(world, spec, resolver);
            }
        }
    }

    private void removeLobbyNpcs() {
        for (var world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(NPC_KEY, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    private void removeLobbyNpcText() {
        for (var world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(NPC_TEXT_KEY, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    private void refreshLobbyNpcDisplays() {
        removeLobbyNpcText();
        for (LobbyNpcSpec spec : lobbyNpcSpecs) {
            LobbyEntry entry = findLobbyEntry(spec);
            TagResolver resolver = npcResolver(spec, entry);
            List<String> lines = spec.lines() != null ? spec.lines() : List.of();
            Entity entity = lobbyNpcEntities.get(spec.id());
            if (entity != null) {
                if (lines.isEmpty()) {
                    entity.customName(MINI.deserialize(spec.name(), resolver));
                    entity.setCustomNameVisible(true);
                } else {
                    entity.customName(null);
                    entity.setCustomNameVisible(false);
                }
            }
            if (!lines.isEmpty()) {
                var world = Bukkit.getWorld(spec.world());
                if (world != null) {
                    spawnNpcText(world, spec, resolver);
                }
            }
        }
    }

    private TagResolver npcResolver(LobbyNpcSpec spec, LobbyEntry entry) {
        String lobby = entry != null && entry.lobby() != null ? entry.lobby() : defaultLobbyName(spec);
        String server = entry != null && entry.server() != null ? entry.server() : valueOrEmpty(spec.server());
        String online = entry != null ? String.valueOf(entry.online()) : "0";
        String max = entry != null ? String.valueOf(entry.max()) : "0";
        return TagResolver.resolver(
                Placeholder.unparsed("id", valueOrEmpty(spec.id())),
                Placeholder.unparsed("name", valueOrEmpty(spec.name())),
                Placeholder.unparsed("lobby", valueOrEmpty(lobby)),
                Placeholder.unparsed("server", valueOrEmpty(server)),
                Placeholder.unparsed("online", online),
                Placeholder.unparsed("max", max)
        );
    }

    private LobbyEntry findLobbyEntry(LobbyNpcSpec spec) {
        if (lastLobbyList.isEmpty()) {
            return null;
        }
        String serverKey = normalizeKey(spec.server());
        String idKey = normalizeKey(spec.id());
        if (!serverKey.isBlank()) {
            for (LobbyEntry entry : lastLobbyList) {
                if (normalizeKey(entry.lobby()).equals(serverKey) || normalizeKey(entry.server()).equals(serverKey)) {
                    return entry;
                }
            }
        }
        if (!idKey.isBlank()) {
            for (LobbyEntry entry : lastLobbyList) {
                if (normalizeKey(entry.lobby()).equals(idKey) || normalizeKey(entry.server()).equals(idKey)) {
                    return entry;
                }
            }
        }
        return null;
    }

    private String defaultLobbyName(LobbyNpcSpec spec) {
        if (spec.server() != null && !spec.server().isBlank()) {
            return spec.server();
        }
        return valueOrEmpty(spec.id());
    }

    private String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String valueOrEmpty(String raw) {
        return raw == null ? "" : raw;
    }

    private void spawnNpcText(org.bukkit.World world, LobbyNpcSpec spec, TagResolver resolver) {
        List<String> lines = spec.lines() != null ? spec.lines() : List.of();
        if (lines.isEmpty()) {
            return;
        }
        double spacing = 0.25;
        double baseY = spec.y() + 2.2 + (lines.size() - 1) * spacing;
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            double y = baseY - i * spacing;
            var location = new org.bukkit.Location(world, spec.x(), y, spec.z(), spec.yaw(), spec.pitch());
            ArmorStand stand = world.spawn(location, ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(true);
                as.setSmall(true);
                as.setGravity(false);
                as.setSilent(true);
                as.setInvulnerable(true);
                as.setCollidable(false);
                as.customName(MINI.deserialize(raw, resolver));
                as.setCustomNameVisible(true);
                as.getPersistentDataContainer().set(NPC_TEXT_KEY, PersistentDataType.STRING, spec.id());
            });
        }
    }

    private void applyLobbySigns() {
        for (LobbySignSpec spec : lobbySignSpecs) {
            var world = Bukkit.getWorld(spec.world());
            if (world == null) {
                continue;
            }
            var block = world.getBlockAt(spec.x(), spec.y(), spec.z());
            if (!(block.getState() instanceof Sign)) {
                block.setType(Material.OAK_SIGN);
            }
            var state = block.getState();
            if (state instanceof Sign sign) {
                List<String> lines = spec.lines() != null ? spec.lines() : List.of();
                for (int i = 0; i < 4; i++) {
                    String line = i < lines.size() ? lines.get(i) : "";
                    sign.line(i, MINI.deserialize(line));
                }
                sign.update(true, false);
                lobbySignByKey.put(signKey(world.getUID().toString(), spec.x(), spec.y(), spec.z()), spec);
            }
        }
    }

    private String signKey(String worldId, int x, int y, int z) {
        return worldId + ":" + x + ":" + y + ":" + z;
    }

    private EntityType resolveEntityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EntityType.VILLAGER;
        }
        try {
            EntityType type = EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return type.isSpawnable() ? type : EntityType.VILLAGER;
        } catch (IllegalArgumentException ex) {
            return EntityType.VILLAGER;
        }
    }

    private boolean isCompass(ItemStack item) {
        if (item == null || item.getType() != compassMaterial) {
            return false;
        }
        var meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(COMPASS_KEY, PersistentDataType.BYTE);
    }

    private boolean isNavigator(ItemStack item) {
        if (item == null || item.getType() != navigatorMaterial) {
            return false;
        }
        var meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(NAVIGATOR_KEY, PersistentDataType.BYTE);
    }

    private Lang resolveLang(org.bukkit.entity.Player player) {
        String locale = i18nDefaultLocale;
        if (i18nUseClientLocale && player != null) {
            try {
                locale = I18n.normalizeLocale(player.locale().toLanguageTag());
            } catch (Exception ignored) {
                locale = i18nDefaultLocale;
            }
        }
        return Lang.load(this, locale, MINI, i18nOverrides.get(I18n.normalizeLocale(locale)));
    }

    private Lang resolveLang(org.bukkit.command.CommandSender sender) {
        if (sender instanceof org.bukkit.entity.Player player) {
            return resolveLang(player);
        }
        return Lang.load(this, i18nDefaultLocale, MINI, i18nOverrides.get(i18nDefaultLocale));
    }

    Component translate(org.bukkit.command.CommandSender sender, String key, TagResolver... resolvers) {
        return resolveLang(sender).component(key, resolvers);
    }

    private void resetVitals(org.bukkit.entity.Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
    }

    private void scheduleJoinTeleport(org.bukkit.entity.Player player) {
        if (joinTeleportMode == JoinTeleportMode.NONE) {
            return;
        }
        if (joinTeleportDelayTicks <= 0) {
            runJoinTeleport(player);
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> runJoinTeleport(player), joinTeleportDelayTicks);
    }

    private void runJoinTeleport(org.bukkit.entity.Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (joinTeleportMode == JoinTeleportMode.SPAWN) {
            teleportToSpawn(player, true);
            return;
        }
        if (joinTeleportMode == JoinTeleportMode.LAST) {
            if (!teleportToLastLocation(player)) {
                teleportToSpawn(player, true);
            }
        }
    }

    private boolean teleportToLastLocation(org.bukkit.entity.Player player) {
        if (locationRepository == null) {
            return false;
        }
        PlayerLocation stored;
        try {
            stored = locationRepository.getLocation(storageServerId, player.getUniqueId());
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Failed to load last location.", ex);
            return false;
        }
        if (stored == null) {
            return false;
        }
        if (joinTeleportLastMaxAgeMillis > 0
                && System.currentTimeMillis() - stored.updatedAt() > joinTeleportLastMaxAgeMillis) {
            return false;
        }
        if (stored.world() == null || stored.world().isBlank()) {
            return false;
        }
        var world = Bukkit.getWorld(stored.world());
        if (world == null) {
            return false;
        }
        var loc = new org.bukkit.Location(world, stored.x(), stored.y(), stored.z(), stored.yaw(), stored.pitch());
        player.teleport(loc);
        return true;
    }

    private void savePlayerLocation(org.bukkit.entity.Player player) {
        if (player == null) {
            return;
        }
        if (locationRepository == null) {
            return;
        }
        var location = player.getLocation();
        if (location.getWorld() == null) {
            return;
        }
        try {
            PlayerLocation stored = new PlayerLocation(
                    storageServerId,
                    player.getUniqueId(),
                    location.getWorld().getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    System.currentTimeMillis()
            );
            locationRepository.saveLocation(stored);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Failed to save last location.", ex);
        }
    }

    void generateJumpRunCourse(boolean announce) {
        if (!jumpRunEnabled) {
            if (announce) {
                getLogger().info("Jump and run is disabled in config.");
            }
            return;
        }
        var world = resolveJumpRunWorld();
        if (world == null) {
            if (announce) {
                getLogger().warning("Jump and run world not found.");
            }
            return;
        }
        clearJumpRunCourse(world);
        jumpRunPositions.clear();
        jumpRunStarts.clear();

        Random random = new Random();
        int baseX = spawnTeleportEnabled ? (int) Math.round(spawnX) : world.getSpawnLocation().getBlockX();
        int baseY = spawnTeleportEnabled ? (int) Math.round(spawnY) : world.getSpawnLocation().getBlockY();
        int baseZ = spawnTeleportEnabled ? (int) Math.round(spawnZ) : world.getSpawnLocation().getBlockZ();
        if (jumpRunMinY > 0) {
            baseY = Math.max(baseY, jumpRunMinY);
        }
        baseY = Math.min(baseY, jumpRunMaxY);

        int x = baseX;
        int y = baseY;
        int z = baseZ;
        for (int i = 0; i < jumpRunBlocks; i++) {
            Material blockType = jumpRunBlock;
            if (i == 0) {
                blockType = jumpRunStartBlock;
            } else if (i == jumpRunBlocks - 1) {
                blockType = jumpRunFinishBlock;
            }
            world.getBlockAt(x, y, z).setType(blockType, false);
            JumpRunBlockPos pos = new JumpRunBlockPos(world.getName(), x, y, z);
            jumpRunPositions.add(pos);
            if (i == 0) {
                jumpRunStartPos = pos;
            }
            if (i == jumpRunBlocks - 1) {
                jumpRunFinishPos = pos;
            }
            if (i < jumpRunBlocks - 1) {
                double distance = jumpRunMinDistance + (jumpRunMaxDistance - jumpRunMinDistance) * random.nextDouble();
                int dx = random.nextBoolean() ? (int) Math.round(distance) : -(int) Math.round(distance);
                int dz = random.nextBoolean() ? (int) Math.round(distance) : -(int) Math.round(distance);
                int dy = jumpRunMinYOffset + random.nextInt(Math.max(1, jumpRunMaxYOffset - jumpRunMinYOffset + 1));
                x += dx;
                z += dz;
                y = clamp(y + dy, jumpRunMinY, jumpRunMaxY);
            }
        }

        if (announce) {
            getLogger().info("Jump and run course generated with " + jumpRunBlocks + " blocks.");
        }
    }

    void startJumpRun(org.bukkit.entity.Player player) {
        if (!jumpRunEnabled) {
            player.sendMessage(resolveLang(player).component("lobby.jump.disabled"));
            return;
        }
        if (jumpRunStartPos == null || jumpRunFinishPos == null) {
            generateJumpRunCourse(true);
        }
        if (jumpRunStartPos == null) {
            player.sendMessage(resolveLang(player).component("lobby.jump.no-course"));
            return;
        }
        jumpRunStarts.put(player.getUniqueId(), System.currentTimeMillis());
        if (jumpRunTeleportOnStart) {
            var world = resolveJumpRunWorld();
            if (world != null) {
                var loc = new org.bukkit.Location(world, jumpRunStartPos.x() + 0.5, jumpRunStartPos.y() + 1.0, jumpRunStartPos.z() + 0.5, player.getLocation().getYaw(), player.getLocation().getPitch());
                player.teleport(loc);
            }
        }
        player.sendMessage(resolveLang(player).component("lobby.jump.started"));
    }

    void stopJumpRun(org.bukkit.entity.Player player) {
        if (jumpRunStarts.remove(player.getUniqueId()) != null) {
            player.sendMessage(resolveLang(player).component("lobby.jump.stopped"));
        } else {
            player.sendMessage(resolveLang(player).component("lobby.jump.no-active"));
        }
    }

    void sendJumpRunInfo(org.bukkit.command.CommandSender sender) {
        TagResolver stateResolver = jumpRunEnabled
                ? Placeholder.component("state", resolveLang(sender).component("lobby.state.enabled"))
                : Placeholder.component("state", resolveLang(sender).component("lobby.state.disabled"));
        sender.sendMessage(resolveLang(sender).component("lobby.jump.info.status", stateResolver));
        sender.sendMessage(resolveLang(sender).component(
                "lobby.jump.info.course",
                Placeholder.unparsed("course", jumpRunCourseId),
                Placeholder.unparsed("blocks", String.valueOf(jumpRunBlocks))
        ));
    }

    private void completeJumpRun(org.bukkit.entity.Player player, long startedAt) {
        jumpRunStarts.remove(player.getUniqueId());
        long duration = Math.max(0L, System.currentTimeMillis() - startedAt);
        long best = duration;
        long runCount = 1L;
        if (scoreRepository != null) {
            try {
                var score = scoreRepository.recordRun(jumpRunCourseId, player.getUniqueId(), duration);
                if (score != null) {
                    best = score.bestTimeMillis();
                    runCount = score.runCount();
                }
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "Failed to record jump and run score.", ex);
            }
        }
        player.sendMessage(resolveLang(player).component(
                "lobby.jump.finish",
                Placeholder.unparsed("time", formatDuration(duration)),
                Placeholder.unparsed("best", formatDuration(best)),
                Placeholder.unparsed("runs", String.valueOf(runCount))
        ));
    }

    private boolean isFinishBlock(int x, int y, int z) {
        if (jumpRunFinishPos == null) {
            return false;
        }
        return jumpRunFinishPos.x() == x && jumpRunFinishPos.y() == y && jumpRunFinishPos.z() == z;
    }

    private org.bukkit.World resolveJumpRunWorld() {
        if (jumpRunWorld == null || jumpRunWorld.isBlank()) {
            return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        org.bukkit.World world = Bukkit.getWorld(jumpRunWorld);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        return world;
    }

    private void clearJumpRunCourse(org.bukkit.World world) {
        if (world == null || jumpRunPositions.isEmpty()) {
            return;
        }
        for (JumpRunBlockPos pos : jumpRunPositions) {
            if (!world.getName().equalsIgnoreCase(pos.world())) {
                continue;
            }
            world.getBlockAt(pos.x(), pos.y(), pos.z()).setType(Material.AIR, false);
        }
    }

    private int clamp(int value, int min, int max) {
        if (max > 0 && value > max) {
            return max;
        }
        if (min > 0 && value < min) {
            return min;
        }
        return value;
    }

    private String formatDuration(long millis) {
        if (millis <= 0) {
            return "0.00s";
        }
        return String.format(Locale.US, "%.2fs", millis / 1000.0);
    }

    boolean teleportToSpawn(org.bukkit.entity.Player player, boolean force) {
        if ((!spawnTeleportEnabled && !force) || spawnWorld == null || spawnWorld.isBlank()) {
            return false;
        }
        var world = Bukkit.getWorld(spawnWorld);
        if (world == null) {
            return false;
        }
        var loc = new org.bukkit.Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
        player.teleport(loc);
        return true;
    }

    void teleportToSpawn(org.bukkit.entity.Player player) {
        teleportToSpawn(player, false);
    }

    private JoinTeleportMode parseJoinTeleportMode(String raw) {
        if (raw == null) {
            return JoinTeleportMode.NONE;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "spawn" -> JoinTeleportMode.SPAWN;
            case "last", "last-location", "last_location" -> JoinTeleportMode.LAST;
            default -> JoinTeleportMode.NONE;
        };
    }

    // HubEntrypoint plumbing
    @Override
    public void registerItems() {
        // Items are given via event handlers already registered as Listener.
    }

    @Override
    public void registerMenus() {
        // Menu handling is wired through Inventory events.
    }

    @Override
    public void registerGameplayGuards() {
        // Gameplay guards are handled by registered event handlers.
    }

    @Override
    public void registerTransport() {
        // Transport is handled by navigator/compass handlers already registered.
    }

    private record LobbyEntry(String lobby, String server, int online, int max) {
    }

    private record JumpRunBlockPos(String world, int x, int y, int z) {
    }

    private enum JoinTeleportMode {
        NONE,
        SPAWN,
        LAST
    }

    private enum NavigatorAction {
        TELEPORT,
        SERVER,
        LOBBY_SELECTOR
    }

    private record NavigatorEntry(Component name, List<Component> lore, Material icon, NavigatorAction action, String server,
                                  String world, double x, double y, double z, float yaw, float pitch, int slot) {
    }
}
