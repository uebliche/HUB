package net.uebliche.hub.minestom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.Auth;
import net.minestom.server.Auth.Velocity;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.entity.Player;
import net.uebliche.hub.common.model.CompassConfig;
import net.uebliche.hub.common.model.ItemSpec;
import net.uebliche.hub.common.model.NavigatorConfig;
import net.uebliche.hub.common.model.NavigatorEntrySpec;
import net.uebliche.hub.common.model.LobbyNpcSpec;
import net.uebliche.hub.common.model.LobbySignSpec;
import net.uebliche.hub.common.service.CompassService;
import net.uebliche.hub.common.service.LobbyDisplayService;
import net.uebliche.hub.common.i18n.I18n;
import net.uebliche.hub.common.storage.JumpRunScoreRepository;
import net.uebliche.hub.common.storage.PlayerLocation;
import net.uebliche.hub.common.storage.PlayerLocationRepository;
import net.uebliche.hub.common.storage.StorageBackendType;
import net.uebliche.hub.common.storage.StorageLogger;
import net.uebliche.hub.common.storage.StorageManager;
import net.uebliche.hub.common.storage.StorageSettings;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public final class HubMinestom {
    private static final Logger LOGGER = LoggerFactory.getLogger(HubMinestom.class);
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String CHANNEL = "uebliche:hub";
    private static final String RUN_DIR_ENV = "HUB_MINESTOM_RUN_DIR";
    private static final String DEFAULT_RUN_DIR = "run";
    private final CompassService configService = new CompassService();
    private final LobbyDisplayService displayService = new LobbyDisplayService();
    private CompassConfig compassConfig = CompassConfig.fallback();
    private NavigatorConfig navigatorConfig = NavigatorConfig.fallback();
    private LobbyDisplayService.LobbyDisplayConfig<LobbyNpcSpec> lobbyNpcConfig =
            new LobbyDisplayService.LobbyDisplayConfig<>(false, List.of());
    private LobbyDisplayService.LobbyDisplayConfig<LobbySignSpec> lobbySignConfig =
            new LobbyDisplayService.LobbyDisplayConfig<>(false, List.of());
    private String velocitySecret = "hubsecret";
    private String i18nDefaultLocale = "en_us";
    private boolean i18nUseClientLocale = true;
    private Map<String, Map<String, String>> i18nOverrides = new HashMap<>();
    private boolean spawnTeleportEnabled;
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private float spawnYaw;
    private float spawnPitch;
    private JoinTeleportMode joinTeleportMode = JoinTeleportMode.NONE;
    private long joinTeleportDelaySeconds;
    private long joinTeleportLastMaxAgeMillis;
    private InstanceContainer instance;
    private Material compassMat;
    private Material navigatorMat;
    private final Map<Integer, LobbyNpcSpec> lobbyNpcByEntityId = new HashMap<>();
    private final Map<Integer, Entity> lobbyNpcEntities = new HashMap<>();
    private final Map<Integer, Entity> lobbyNpcTextEntities = new HashMap<>();
    private final Map<BlockVec, LobbySignSpec> lobbySignByPos = new HashMap<>();
    private StorageManager storageManager;
    private PlayerLocationRepository locationRepository;
    private JumpRunScoreRepository scoreRepository;
    private String storageServerId = "lobby";
    private boolean jumpRunEnabled;
    private String jumpRunCourseId = "default";
    private int jumpRunBlocks = 20;
    private double jumpRunMinDistance = 2.5;
    private double jumpRunMaxDistance = 4.5;
    private int jumpRunMinYOffset = -1;
    private int jumpRunMaxYOffset = 2;
    private int jumpRunMinY = 70;
    private int jumpRunMaxY = 140;
    private Block jumpRunBlock = Block.QUARTZ_BLOCK;
    private Block jumpRunStartBlock = Block.EMERALD_BLOCK;
    private Block jumpRunFinishBlock = Block.GOLD_BLOCK;
    private boolean jumpRunTeleportOnStart = true;
    private final Map<UUID, Long> jumpRunStarts = new HashMap<>();
    private final List<BlockVec> jumpRunPositions = new ArrayList<>();
    private BlockVec jumpRunStartPos;
    private BlockVec jumpRunFinishPos;

    public static void main(String[] args) {
        new HubMinestom().start();
    }

    private void start() {
        loadConfig();
        MinecraftServer server = initServer();

        instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
        instance.setGenerator(unit -> unit.modifier().fillHeight(40, 41, Block.GRASS_BLOCK));
        applyLobbySigns();
        spawnLobbyNpcs();
        registerCommands();
        if (jumpRunEnabled) {
            generateJumpRunCourse(false);
        }
        MinecraftServer.getSchedulerManager().buildTask(this::tickJumpRun)
                .repeat(Duration.ofMillis(100))
                .schedule();

        var global = MinecraftServer.getGlobalEventHandler();
        global.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
            event.getPlayer().setGameMode(GameMode.ADVENTURE);
            LOGGER.info("Player joining: {} ({})", event.getPlayer().getUsername(), event.getPlayer().getUuid());
        });
        global.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                giveItems(event.getPlayer());
                scheduleJoinTeleport(event.getPlayer());
            }
        });
        global.addListener(PlayerUseItemEvent.class, event -> {
            var item = event.getItemStack();
            if (isCompass(item)) {
                event.getPlayer().sendMessage(tr(event.getPlayer(), "lobby.compass.unavailable"));
            } else if (isNavigator(item)) {
                openNavigator(event.getPlayer());
            }
        });
        global.addListener(PlayerEntityInteractEvent.class, event -> {
            LobbyNpcSpec spec = lobbyNpcByEntityId.get(event.getTarget().getEntityId());
            if (spec != null) {
                handleLobbyAction(event.getPlayer(), spec.action(), spec.server(), spec.targetWorld(),
                        spec.targetX(), spec.targetY(), spec.targetZ(), spec.targetYaw(), spec.targetPitch());
            }
        });
        global.addListener(PlayerBlockInteractEvent.class, event -> {
            LobbySignSpec spec = lobbySignByPos.get(event.getBlockPosition());
            if (spec != null) {
                event.setCancelled(true);
                handleLobbyAction(event.getPlayer(), spec.action(), spec.server(), spec.targetWorld(),
                        spec.targetX(), spec.targetY(), spec.targetZ(), spec.targetYaw(), spec.targetPitch());
            }
        });
        global.addListener(PlayerRespawnEvent.class, event -> giveItems(event.getPlayer()));
        global.addListener(ItemDropEvent.class, event -> {
            ItemStack stack = event.getItemStack();
            if ((isCompass(stack) && !compassConfig.item().allowDrop())
                    || (isNavigator(stack) && !navigatorConfig.item().allowDrop())) {
                event.setCancelled(true);
            }
        });
        global.addListener(InventoryPreClickEvent.class, event -> {
            ItemStack clicked = event.getInventory().getItemStack(event.getSlot());
            if (clicked == null)
                return;
            boolean compassBlocked = isCompass(clicked) && !compassConfig.item().allowMove();
            boolean navigatorBlocked = isNavigator(clicked) && !navigatorConfig.item().allowMove();
            if (compassBlocked || navigatorBlocked) {
                event.setCancelled(true);
            }
        });
        global.addListener(PlayerDisconnectEvent.class, event -> {
            LOGGER.info("Player disconnected: {} ({})", event.getPlayer().getUsername(), event.getPlayer().getUuid());
            storeLastLocation(event.getPlayer());
            jumpRunStarts.remove(event.getPlayer().getUuid());
        });

        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
            if (storageManager != null) {
                storageManager.close();
            }
            MinecraftServer.stopCleanly();
        });
        server.start("0.0.0.0", 25565);
    }

    private void loadConfig() {
        try {
            Path configDir = resolveRunDir().resolve("config");
            Path configPath = configDir.resolve("hub.yaml");
            Files.createDirectories(configDir);
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();
            if (Files.notExists(configPath)) {
                ConfigurationNode fresh = loader.createNode();
                fresh.node("minestom", "velocity-secret").set(velocitySecret);
                fresh.node("i18n", "default-locale").set("en_us");
                fresh.node("i18n", "use-client-locale").set(true);
                fresh.node("i18n", "overrides", "en_us").set(new HashMap<>());
                fresh.node("i18n", "overrides", "de_de").set(new HashMap<>());
                fresh.node("spawn-teleport", "enabled").set(false);
                fresh.node("spawn-teleport", "x").set(0);
                fresh.node("spawn-teleport", "y").set(64);
                fresh.node("spawn-teleport", "z").set(0);
                fresh.node("spawn-teleport", "yaw").set(0);
                fresh.node("spawn-teleport", "pitch").set(0);
                fresh.node("join-teleport", "mode").set("none");
                fresh.node("join-teleport", "delay-seconds").set(0);
                fresh.node("join-teleport", "last-location", "max-age-seconds").set(0);
                fresh.node("jump-run", "enabled").set(false);
                fresh.node("jump-run", "course-id").set("default");
                fresh.node("jump-run", "blocks").set(20);
                fresh.node("jump-run", "spacing", "min").set(2.5);
                fresh.node("jump-run", "spacing", "max").set(4.5);
                fresh.node("jump-run", "y-offset", "min").set(-1);
                fresh.node("jump-run", "y-offset", "max").set(2);
                fresh.node("jump-run", "height", "min").set(70);
                fresh.node("jump-run", "height", "max").set(140);
                fresh.node("jump-run", "block").set("minecraft:quartz_block");
                fresh.node("jump-run", "start-block").set("minecraft:emerald_block");
                fresh.node("jump-run", "finish-block").set("minecraft:gold_block");
                fresh.node("jump-run", "teleport-on-start").set(true);
                fresh.node("storage", "server-id").set("lobby");
                fresh.node("storage", "scores", "primary").set("local-sql");
                fresh.node("storage", "locations", "primary").set("local-sql");
                fresh.node("storage", "locations", "cache").set("none");
                fresh.node("storage", "fallback", "enabled").set(true);
                fresh.node("storage", "sql", "url").set("jdbc:mariadb://localhost:3306/hub");
                fresh.node("storage", "sql", "user").set("hub");
                fresh.node("storage", "sql", "password").set("");
                fresh.node("storage", "sql", "driver").set("org.mariadb.jdbc.Driver");
                fresh.node("storage", "local-sql", "url").set("jdbc:sqlite:" + configDir.resolve("hub.db").toAbsolutePath());
                fresh.node("storage", "local-sql", "driver").set("org.sqlite.JDBC");
                fresh.node("storage", "mongo", "uri").set("mongodb://localhost:27017");
                fresh.node("storage", "mongo", "database").set("hub");
                fresh.node("storage", "mongo", "collection-prefix").set("hub_");
                fresh.node("storage", "redis", "uri").set("redis://localhost:6379");
                fresh.node("storage", "redis", "database").set(0);
                fresh.node("storage", "redis", "key-prefix").set("hub");
                fresh.node("lobby-npcs", "enabled").set(false);
                fresh.node("lobby-npcs", "entries", "lobby", "world").set("world");
                fresh.node("lobby-npcs", "entries", "lobby", "x").set(0);
                fresh.node("lobby-npcs", "entries", "lobby", "y").set(64);
                fresh.node("lobby-npcs", "entries", "lobby", "z").set(0);
                fresh.node("lobby-npcs", "entries", "lobby", "yaw").set(0);
                fresh.node("lobby-npcs", "entries", "lobby", "pitch").set(0);
                fresh.node("lobby-npcs", "entries", "lobby", "name").set("<gold>Lobby");
                fresh.node("lobby-npcs", "entries", "lobby", "entity").set("VILLAGER");
                fresh.node("lobby-npcs", "entries", "lobby", "action").set("server");
                fresh.node("lobby-npcs", "entries", "lobby", "server").set("lobby");
                fresh.node("lobby-signs", "enabled").set(false);
                fresh.node("lobby-signs", "entries", "lobby", "world").set("world");
                fresh.node("lobby-signs", "entries", "lobby", "x").set(2);
                fresh.node("lobby-signs", "entries", "lobby", "y").set(64);
                fresh.node("lobby-signs", "entries", "lobby", "z").set(0);
                fresh.node("lobby-signs", "entries", "lobby", "lines").set(List.of("<gold>Lobby", "<gray>Click to join"));
                fresh.node("lobby-signs", "entries", "lobby", "action").set("server");
                fresh.node("lobby-signs", "entries", "lobby", "server").set("lobby");
                loader.save(fresh);
            }
            ConfigurationNode node = loader.load();
            compassConfig = configService.loadCompass(node);
            navigatorConfig = configService.loadNavigator(node);
            velocitySecret = node.node("minestom", "velocity-secret").getString(velocitySecret);
            i18nDefaultLocale = I18n.normalizeLocale(node.node("i18n", "default-locale").getString("en_us"));
            i18nUseClientLocale = node.node("i18n", "use-client-locale").getBoolean(true);
            i18nOverrides = loadI18nOverrides(node, configDir);
            spawnTeleportEnabled = node.node("spawn-teleport", "enabled").getBoolean(false);
            spawnX = node.node("spawn-teleport", "x").getDouble(0);
            spawnY = node.node("spawn-teleport", "y").getDouble(64);
            spawnZ = node.node("spawn-teleport", "z").getDouble(0);
            spawnYaw = (float) node.node("spawn-teleport", "yaw").getDouble(0);
            spawnPitch = (float) node.node("spawn-teleport", "pitch").getDouble(0);
            ConfigurationNode joinModeNode = node.node("join-teleport", "mode");
            if (joinModeNode.virtual()) {
                joinTeleportMode = spawnTeleportEnabled ? JoinTeleportMode.SPAWN : JoinTeleportMode.NONE;
            } else {
                String joinModeRaw = joinModeNode.getString("none");
                joinTeleportMode = parseJoinTeleportMode(joinModeRaw);
            }
            long delaySeconds = Math.max(0L, node.node("join-teleport", "delay-seconds").getLong(0L));
            joinTeleportDelaySeconds = delaySeconds;
            long maxAgeSeconds = Math.max(0L, node.node("join-teleport", "last-location", "max-age-seconds").getLong(0L));
            joinTeleportLastMaxAgeMillis = maxAgeSeconds <= 0 ? 0L : maxAgeSeconds * 1000L;
            jumpRunEnabled = node.node("jump-run", "enabled").getBoolean(false);
            jumpRunCourseId = node.node("jump-run", "course-id").getString("default");
            jumpRunBlocks = Math.max(3, node.node("jump-run", "blocks").getInt(20));
            jumpRunMinDistance = Math.max(1.0, node.node("jump-run", "spacing", "min").getDouble(2.5));
            jumpRunMaxDistance = Math.max(jumpRunMinDistance, node.node("jump-run", "spacing", "max").getDouble(4.5));
            jumpRunMinYOffset = node.node("jump-run", "y-offset", "min").getInt(-1);
            jumpRunMaxYOffset = node.node("jump-run", "y-offset", "max").getInt(2);
            jumpRunMinY = node.node("jump-run", "height", "min").getInt(70);
            jumpRunMaxY = node.node("jump-run", "height", "max").getInt(140);
            jumpRunTeleportOnStart = node.node("jump-run", "teleport-on-start").getBoolean(true);
            jumpRunBlock = resolveJumpRunBlock(node.node("jump-run", "block").getString("minecraft:quartz_block"), Block.QUARTZ_BLOCK);
            jumpRunStartBlock = resolveJumpRunBlock(node.node("jump-run", "start-block").getString("minecraft:emerald_block"), Block.EMERALD_BLOCK);
            jumpRunFinishBlock = resolveJumpRunBlock(node.node("jump-run", "finish-block").getString("minecraft:gold_block"), Block.GOLD_BLOCK);
            lobbyNpcConfig = displayService.loadNpcs(node);
            lobbySignConfig = displayService.loadSigns(node);
            initStorage(node, configDir);
        } catch (Exception ex) {
            ex.printStackTrace();
            compassConfig = CompassConfig.fallback();
            navigatorConfig = NavigatorConfig.fallback();
            velocitySecret = "hubsecret";
            i18nDefaultLocale = "en_us";
            i18nUseClientLocale = true;
            i18nOverrides = new HashMap<>();
            spawnTeleportEnabled = false;
            joinTeleportMode = JoinTeleportMode.NONE;
            joinTeleportDelaySeconds = 0;
            joinTeleportLastMaxAgeMillis = 0;
            jumpRunEnabled = false;
            jumpRunCourseId = "default";
            jumpRunBlocks = 20;
            jumpRunMinDistance = 2.5;
            jumpRunMaxDistance = 4.5;
            jumpRunMinYOffset = -1;
            jumpRunMaxYOffset = 2;
            jumpRunMinY = 70;
            jumpRunMaxY = 140;
            jumpRunTeleportOnStart = true;
            jumpRunBlock = Block.QUARTZ_BLOCK;
            jumpRunStartBlock = Block.EMERALD_BLOCK;
            jumpRunFinishBlock = Block.GOLD_BLOCK;
            lobbyNpcConfig = new LobbyDisplayService.LobbyDisplayConfig<>(false, List.of());
            lobbySignConfig = new LobbyDisplayService.LobbyDisplayConfig<>(false, List.of());
            initStorage(null, resolveRunDir().resolve("config"));
        }
        compassMat = resolveMaterial(compassConfig.item(), "minecraft:compass");
        navigatorMat = resolveMaterial(navigatorConfig.item(), "minecraft:compass");
    }

    private void initStorage(ConfigurationNode node, Path configDir) {
        StorageSettings settings = buildStorageSettings(node, configDir);
        storageServerId = settings.serverId();
        if (storageManager != null) {
            storageManager.close();
        }
        storageManager = StorageManager.create(settings, new StorageLogger() {
            @Override
            public void info(String message) {
                LOGGER.info(message);
            }

            @Override
            public void warn(String message) {
                LOGGER.warn(message);
            }

            @Override
            public void error(String message, Throwable error) {
                LOGGER.error(message, error);
            }
        });
        locationRepository = storageManager.locations();
        scoreRepository = storageManager.scores();
    }

    private StorageSettings buildStorageSettings(ConfigurationNode node, Path configDir) {
        ConfigurationNode root = node == null ? YamlConfigurationLoader.builder().path(configDir.resolve("hub.yaml")).build().createNode() : node;
        String serverId = root.node("storage", "server-id").getString("lobby");
        StorageBackendType scorePrimary = StorageBackendType.fromString(root.node("storage", "scores", "primary").getString("local-sql"));
        StorageBackendType locationPrimary = StorageBackendType.fromString(root.node("storage", "locations", "primary").getString("local-sql"));
        StorageBackendType locationCache = StorageBackendType.fromString(root.node("storage", "locations", "cache").getString("none"));
        boolean fallbackEnabled = root.node("storage", "fallback", "enabled").getBoolean(true);

        String sqlUrl = root.node("storage", "sql", "url").getString("jdbc:mariadb://localhost:3306/hub");
        String sqlUser = root.node("storage", "sql", "user").getString("hub");
        String sqlPassword = root.node("storage", "sql", "password").getString("");
        String sqlDriver = root.node("storage", "sql", "driver").getString("org.mariadb.jdbc.Driver");
        StorageSettings.SqlSettings sql = new StorageSettings.SqlSettings(sqlUrl, sqlUser, sqlPassword, sqlDriver);

        String localDefault = "jdbc:sqlite:" + configDir.resolve("hub.db").toAbsolutePath();
        String localUrl = root.node("storage", "local-sql", "url").getString(localDefault);
        String localDriver = root.node("storage", "local-sql", "driver").getString("org.sqlite.JDBC");
        StorageSettings.SqlSettings localSql = new StorageSettings.SqlSettings(localUrl, "", "", localDriver);

        String mongoUri = root.node("storage", "mongo", "uri").getString("mongodb://localhost:27017");
        String mongoDatabase = root.node("storage", "mongo", "database").getString("hub");
        String mongoPrefix = root.node("storage", "mongo", "collection-prefix").getString("hub_");
        StorageSettings.MongoSettings mongo = new StorageSettings.MongoSettings(mongoUri, mongoDatabase, mongoPrefix);

        String redisUri = root.node("storage", "redis", "uri").getString("redis://localhost:6379");
        int redisDb = root.node("storage", "redis", "database").getInt(0);
        String redisPrefix = root.node("storage", "redis", "key-prefix").getString("hub");
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

    private Path resolveRunDir() {
        String raw = System.getenv(RUN_DIR_ENV);
        if (raw == null || raw.isBlank()) {
            return Path.of(DEFAULT_RUN_DIR);
        }
        return Path.of(raw);
    }

    private void giveItems(net.minestom.server.entity.Player player) {
        var inv = player.getInventory();
        clearHubItems(inv);
        ItemStack compass = buildItem(compassConfig.item(), compassMat);
        ItemStack navigator = buildItem(navigatorConfig.item(), navigatorMat);
        if (compassConfig.enabled()) {
            int slot = Math.max(0, Math.min(8, compassConfig.item().slot()));
            inv.setItemStack(slot, compass);
        }
        if (navigatorConfig.enabled()) {
            int slot = Math.max(0, Math.min(8, navigatorConfig.item().slot()));
            inv.setItemStack(slot, navigator);
        }
    }

    private void clearHubItems(net.minestom.server.inventory.PlayerInventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItemStack(i);
            if (stack != null && (isCompass(stack) || isNavigator(stack))) {
                inv.setItemStack(i, ItemStack.AIR);
            }
        }
    }

    private ItemStack buildItem(ItemSpec spec, Material fallback) {
        Material mat = resolveMaterial(spec, fallback.key().asString());
        ItemStack stack = ItemStack.of(mat, 1);
        if (spec.name() != null && !spec.name().isBlank()) {
            stack = stack.withCustomName(MINI.deserialize(spec.name()));
        }
        if (spec.lore() != null && !spec.lore().isEmpty()) {
            List<Component> lore = spec.lore().stream().map(MINI::deserialize).toList();
            stack = stack.withLore(lore);
        }
        return stack;
    }

    private Material resolveMaterial(ItemSpec spec, String defaultKey) {
        String key = defaultKey;
        if (spec != null && spec.material() != null && !spec.material().isBlank()) {
            key = spec.material();
        }
        String namespaced = key.toLowerCase(Locale.ROOT);
        if (!namespaced.contains(":")) {
            namespaced = "minecraft:" + namespaced;
        }
        Material mat = Material.fromKey(namespaced);
        if (mat == null) {
            mat = Material.AIR;
        }
        return mat;
    }

    private Map<String, Map<String, String>> loadI18nOverrides(ConfigurationNode node, Path configDir) {
        I18n.reloadFromClasspath("en_us");
        I18n.reloadFromClasspath("de_de");
        Map<String, Map<String, String>> merged = new HashMap<>();
        mergeOverrides(merged, loadI18nFiles(configDir.resolve("i18n")));
        mergeOverrides(merged, readOverrideNode(node.node("i18n", "overrides")));
        return merged;
    }

    private Map<String, Map<String, String>> loadI18nFiles(Path dir) {
        Map<String, Map<String, String>> result = new HashMap<>();
        if (dir == null || Files.notExists(dir)) {
            return result;
        }
        try {
            Files.createDirectories(dir);
            try (var stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    int dot = fileName.lastIndexOf('.');
                    if (dot <= 0) {
                        return;
                    }
                    String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
                    String locale = fileName.substring(0, dot);
                    Map<String, String> entries = switch (ext) {
                        case "yml", "yaml" -> readYamlLocale(path);
                        case "json" -> readJsonLocale(path);
                        default -> Map.of();
                    };
                    if (!entries.isEmpty()) {
                        result.put(I18n.normalizeLocale(locale), entries);
                    }
                });
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private Map<String, String> readYamlLocale(Path path) {
        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
            ConfigurationNode node = loader.load();
            Map<String, String> entries = new HashMap<>();
            flattenNode(node, "", entries);
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

    private Map<String, Map<String, String>> readOverrideNode(ConfigurationNode root) {
        Map<String, Map<String, String>> result = new HashMap<>();
        if (root == null || root.virtual()) {
            return result;
        }
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.childrenMap().entrySet()) {
            String locale = String.valueOf(entry.getKey());
            Map<String, String> entries = new HashMap<>();
            flattenNode(entry.getValue(), "", entries);
            if (!entries.isEmpty()) {
                result.put(I18n.normalizeLocale(locale), entries);
            }
        }
        return result;
    }

    private void flattenNode(ConfigurationNode node, String prefix, Map<String, String> out) {
        if (node == null) {
            return;
        }
        if (node.isList()) {
            List<String> lines = new ArrayList<>();
            for (ConfigurationNode child : node.childrenList()) {
                String line = child.getString();
                if (line != null) {
                    lines.add(line);
                }
            }
            if (!lines.isEmpty() && !prefix.isBlank()) {
                out.put(prefix, String.join("\n", lines));
            }
            return;
        }
        if (!node.isMap()) {
            String value = node.getString();
            if (value != null && !prefix.isBlank()) {
                out.put(prefix, value);
            }
            return;
        }
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String key = String.valueOf(entry.getKey());
            String next = prefix.isBlank() ? key : prefix + "." + key;
            flattenNode(entry.getValue(), next, out);
        }
    }

    private void mergeOverrides(Map<String, Map<String, String>> target, Map<String, Map<String, String>> source) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String locale = I18n.normalizeLocale(entry.getKey());
            Map<String, String> existing = target.computeIfAbsent(locale, k -> new HashMap<>());
            existing.putAll(entry.getValue());
        }
    }

    private String resolveLocale(CommandSender sender) {
        if (!i18nUseClientLocale) {
            return i18nDefaultLocale;
        }
        if (sender instanceof Player player) {
            Locale locale = player.getLocale();
            if (locale != null) {
                return I18n.normalizeLocale(locale.toLanguageTag());
            }
        }
        return i18nDefaultLocale;
    }

    private String resolveRaw(String locale, String key) {
        String normalized = I18n.normalizeLocale(locale);
        Map<String, String> overrides = i18nOverrides.get(normalized);
        if (overrides != null && overrides.containsKey(key)) {
            return overrides.get(key);
        }
        return I18n.raw(normalized, key);
    }

    private Component tr(CommandSender sender, String key, TagResolver... resolvers) {
        String locale = resolveLocale(sender);
        String raw = resolveRaw(locale, key);
        return MINI.deserialize(raw, resolvers);
    }

    private void registerCommands() {
        var addLiteral = ArgumentType.Literal("add");
        var removeLiteral = ArgumentType.Literal("remove");
        var listLiteral = ArgumentType.Literal("list");
        var enableLiteral = ArgumentType.Literal("enable");
        var idArg = ArgumentType.Word("id");
        var actionArg = ArgumentType.Word("action");
        var serverArg = ArgumentType.Word("server");
        var enabledArg = ArgumentType.Boolean("enabled");

        var cmd = new Command("hubnpc");
        cmd.setCondition((sender, commandString) -> true);

        cmd.addSyntax((sender, ctx) -> listNpcs(sender), listLiteral);
        cmd.addSyntax((sender, ctx) -> setNpcEnabled(sender, ctx.get(enabledArg)), enableLiteral, enabledArg);
        cmd.addSyntax((sender, ctx) -> removeNpc(sender, ctx.get(idArg)), removeLiteral, idArg);
        cmd.addSyntax((sender, ctx) -> addNpc(sender, ctx.get(idArg), ctx.get(actionArg), ""),
                addLiteral, idArg, actionArg);
        cmd.addSyntax((sender, ctx) -> addNpc(sender, ctx.get(idArg), ctx.get(actionArg), ctx.get(serverArg)),
                addLiteral, idArg, actionArg, serverArg);

        cmd.setDefaultExecutor((sender, ctx) -> {
            sender.sendMessage(tr(sender, "lobby.command.npc.header"));
            sender.sendMessage(tr(sender, "lobby.command.npc.usage.add", Placeholder.unparsed("label", "hubnpc")));
            sender.sendMessage(tr(sender, "lobby.command.npc.usage.remove", Placeholder.unparsed("label", "hubnpc")));
            sender.sendMessage(tr(sender, "lobby.command.npc.usage.list", Placeholder.unparsed("label", "hubnpc")));
            sender.sendMessage(tr(sender, "lobby.command.npc.usage.enable", Placeholder.unparsed("label", "hubnpc")));
        });
        MinecraftServer.getCommandManager().register(cmd);

        var jumpCmd = new Command("hubjump");
        jumpCmd.setCondition((sender, commandString) -> true);

        var startLiteral = ArgumentType.Literal("start");
        var stopLiteral = ArgumentType.Literal("stop");
        var generateLiteral = ArgumentType.Literal("generate");
        var infoLiteral = ArgumentType.Literal("info");

        jumpCmd.addSyntax((sender, ctx) -> {
            if (sender instanceof Player player) {
                startJumpRun(player);
            } else {
                sender.sendMessage(tr(sender, "lobby.command.jump.only-player-start"));
            }
        }, startLiteral);
        jumpCmd.addSyntax((sender, ctx) -> {
            if (sender instanceof Player player) {
                stopJumpRun(player);
            } else {
                sender.sendMessage(tr(sender, "lobby.command.jump.only-player-stop"));
            }
        }, stopLiteral);
        jumpCmd.addSyntax((sender, ctx) -> {
            if (!jumpRunEnabled) {
                sender.sendMessage(tr(sender, "lobby.jump.disabled"));
                return;
            }
            generateJumpRunCourse(true);
            sender.sendMessage(tr(sender, "lobby.command.jump.generated"));
        }, generateLiteral);
        jumpCmd.addSyntax((sender, ctx) -> sendJumpRunInfo(sender), infoLiteral);

        jumpCmd.setDefaultExecutor((sender, ctx) -> sender.sendMessage(tr(sender, "lobby.command.jump.usage")));
        MinecraftServer.getCommandManager().register(jumpCmd);
    }

    private void listNpcs(CommandSender sender) {
        if (lobbyNpcConfig.entries().isEmpty()) {
            sender.sendMessage(tr(sender, "lobby.npc.none"));
            return;
        }
        sender.sendMessage(tr(sender, "lobby.npc.list.header"));
        for (LobbyNpcSpec spec : lobbyNpcConfig.entries()) {
            sender.sendMessage(tr(sender, "lobby.npc.list.entry", Placeholder.unparsed("id", spec.id())));
        }
    }

    private void setNpcEnabled(CommandSender sender, boolean enabled) {
        try {
            Path configPath = resolveConfigPath();
            if (Files.notExists(configPath)) {
                loadConfig();
            }
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(configPath).build();
            ConfigurationNode node = loader.load();
            node.node("lobby-npcs", "enabled").set(enabled);
            loader.save(node);
            loadConfig();
            reloadLobbyDisplays();
            Component state = enabled
                    ? tr(sender, "lobby.state.enabled")
                    : tr(sender, "lobby.state.disabled");
            sender.sendMessage(tr(sender, "lobby.npc.enabled", Placeholder.component("state", state)));
        } catch (Exception ex) {
            sender.sendMessage(tr(sender, "lobby.npc.update.failed"));
            LOGGER.error("Failed to update lobby NPCs.", ex);
        }
    }

    private void removeNpc(CommandSender sender, String id) {
        if (id == null || id.isBlank()) {
            sender.sendMessage(tr(sender, "lobby.npc.id.required"));
            return;
        }
        try {
            Path configPath = resolveConfigPath();
            if (Files.notExists(configPath)) {
                loadConfig();
            }
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(configPath).build();
            ConfigurationNode node = loader.load();
            ConfigurationNode entry = node.node("lobby-npcs", "entries", id);
            if (entry.virtual()) {
                sender.sendMessage(tr(sender, "lobby.npc.not-found", Placeholder.unparsed("id", id)));
                return;
            }
            entry.raw(null);
            loader.save(node);
            loadConfig();
            reloadLobbyDisplays();
            sender.sendMessage(tr(sender, "lobby.npc.removed", Placeholder.unparsed("id", id)));
        } catch (Exception ex) {
            sender.sendMessage(tr(sender, "lobby.npc.remove.failed"));
            LOGGER.error("Failed to remove lobby NPC.", ex);
        }
    }

    private void addNpc(CommandSender sender, String id, String action, String server) {
        if (id == null || id.isBlank()) {
            sender.sendMessage(tr(sender, "lobby.npc.id.required"));
            return;
        }
        if (!isNpcAction(action)) {
            sender.sendMessage(tr(sender, "lobby.npc.action.invalid"));
            return;
        }
        if ("server".equalsIgnoreCase(action) && (server == null || server.isBlank())) {
            sender.sendMessage(tr(sender, "lobby.npc.server.required"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(tr(sender, "lobby.npc.only-player-add"));
            return;
        }
        try {
            Path configPath = resolveConfigPath();
            if (Files.notExists(configPath)) {
                loadConfig();
            }
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(configPath).build();
            ConfigurationNode node = loader.load();
            ConfigurationNode entry = node.node("lobby-npcs", "entries", id);
            Pos pos = player.getPosition();
            entry.node("world").set("world");
            entry.node("x").set(pos.x());
            entry.node("y").set(pos.y());
            entry.node("z").set(pos.z());
            entry.node("yaw").set(pos.yaw());
            entry.node("pitch").set(pos.pitch());
            entry.node("name").set("<gold>" + id);
            entry.node("entity").set(entry.node("entity").getString("VILLAGER"));
            entry.node("action").set(action.toLowerCase(Locale.ROOT));
            if (server != null && !server.isBlank()) {
                entry.node("server").set(server);
            }
            node.node("lobby-npcs", "enabled").set(true);
            loader.save(node);
            loadConfig();
            reloadLobbyDisplays();
            sender.sendMessage(tr(sender, "lobby.npc.saved", Placeholder.unparsed("id", id)));
        } catch (Exception ex) {
            sender.sendMessage(tr(sender, "lobby.npc.save.failed"));
            LOGGER.error("Failed to save lobby NPC.", ex);
        }
    }

    private boolean isNpcAction(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("server") || normalized.equals("lobby") || normalized.equals("teleport");
    }

    private Path resolveConfigPath() {
        return resolveRunDir().resolve("config").resolve("hub.yaml");
    }

    private void reloadLobbyDisplays() {
        removeLobbyNpcs();
        applyLobbySigns();
        spawnLobbyNpcs();
    }

    private void removeLobbyNpcs() {
        clearLobbyNpcText();
        if (lobbyNpcEntities.isEmpty()) {
            lobbyNpcByEntityId.clear();
            return;
        }
        for (Entity entity : lobbyNpcEntities.values()) {
            if (entity != null) {
                entity.remove();
            }
        }
        lobbyNpcEntities.clear();
        lobbyNpcByEntityId.clear();
    }

    private void spawnLobbyNpcs() {
        lobbyNpcByEntityId.clear();
        lobbyNpcEntities.clear();
        clearLobbyNpcText();
        if (!lobbyNpcConfig.enabled() || instance == null) {
            return;
        }
        for (LobbyNpcSpec spec : lobbyNpcConfig.entries()) {
            EntityType type = resolveEntityType(spec.entity());
            Entity entity = new Entity(type);
            entity.setInstance(instance, new Pos(spec.x(), spec.y(), spec.z(), spec.yaw(), spec.pitch()));
            List<String> lines = spec.lines() != null ? spec.lines() : List.of();
            if (lines.isEmpty()) {
                entity.setCustomName(MINI.deserialize(applyNpcPlaceholders(spec.name(), spec)));
                entity.setCustomNameVisible(true);
            } else {
                entity.setCustomName(null);
                entity.setCustomNameVisible(false);
            }
            entity.setNoGravity(true);
            entity.setSilent(true);
            lobbyNpcByEntityId.put(entity.getEntityId(), spec);
            lobbyNpcEntities.put(entity.getEntityId(), entity);
            if (!lines.isEmpty()) {
                spawnNpcText(spec);
            }
        }
    }

    private void clearLobbyNpcText() {
        if (lobbyNpcTextEntities.isEmpty()) {
            return;
        }
        for (Entity entity : lobbyNpcTextEntities.values()) {
            if (entity != null) {
                entity.remove();
            }
        }
        lobbyNpcTextEntities.clear();
    }

    private void spawnNpcText(LobbyNpcSpec spec) {
        if (instance == null) {
            return;
        }
        List<String> lines = spec.lines() != null ? spec.lines() : List.of();
        if (lines.isEmpty()) {
            return;
        }
        double spacing = 0.25;
        double baseY = spec.y() + 2.2 + (lines.size() - 1) * spacing;
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            double y = baseY - i * spacing;
            Entity text = new Entity(EntityType.ARMOR_STAND);
            text.setInstance(instance, new Pos(spec.x(), y, spec.z(), spec.yaw(), spec.pitch()));
            text.setCustomName(MINI.deserialize(applyNpcPlaceholders(raw, spec)));
            text.setCustomNameVisible(true);
            text.setNoGravity(true);
            text.setSilent(true);
            EntityMeta meta = text.getEntityMeta();
            meta.setInvisible(true);
            meta.setHasNoGravity(true);
            if (meta instanceof ArmorStandMeta standMeta) {
                standMeta.setMarker(true);
                standMeta.setSmall(true);
                standMeta.setHasNoBasePlate(true);
            }
            lobbyNpcTextEntities.put(text.getEntityId(), text);
        }
    }

    private String applyNpcPlaceholders(String template, LobbyNpcSpec spec) {
        if (template == null) {
            return "";
        }
        String lobby = defaultLobbyName(spec);
        String server = valueOrEmpty(spec.server());
        return template
                .replace("<id>", valueOrEmpty(spec.id()))
                .replace("<name>", valueOrEmpty(spec.name()))
                .replace("<lobby>", valueOrEmpty(lobby))
                .replace("<server>", valueOrEmpty(server))
                .replace("<online>", "0")
                .replace("<max>", "0");
    }

    private String defaultLobbyName(LobbyNpcSpec spec) {
        if (spec.server() != null && !spec.server().isBlank()) {
            return spec.server();
        }
        return valueOrEmpty(spec.id());
    }

    private String valueOrEmpty(String raw) {
        return raw == null ? "" : raw;
    }

    private void applyLobbySigns() {
        lobbySignByPos.clear();
        if (!lobbySignConfig.enabled() || instance == null) {
            return;
        }
        for (LobbySignSpec spec : lobbySignConfig.entries()) {
            BlockVec pos = new BlockVec(spec.x(), spec.y(), spec.z());
            CompoundBinaryTag nbt = buildSignNbt(spec.lines());
            Block sign = Block.OAK_SIGN.withNbt(nbt);
            instance.setBlock(pos, sign);
            lobbySignByPos.put(pos, spec);
        }
    }

    private CompoundBinaryTag buildSignNbt(List<String> lines) {
        List<String> safe = lines != null ? lines : List.of();
        ListBinaryTag.Builder listBuilder = ListBinaryTag.builder(BinaryTagTypes.STRING);
        GsonComponentSerializer gson = GsonComponentSerializer.gson();
        for (int i = 0; i < 4; i++) {
            String raw = i < safe.size() ? safe.get(i) : "";
            String json = gson.serialize(MINI.deserialize(raw));
            listBuilder.add(net.kyori.adventure.nbt.StringBinaryTag.stringBinaryTag(json));
        }
        ListBinaryTag messages = listBuilder.build();
        CompoundBinaryTag front = CompoundBinaryTag.builder()
                .put("messages", messages)
                .putString("color", "black")
                .putBoolean("has_glowing_text", false)
                .build();
        CompoundBinaryTag back = CompoundBinaryTag.builder()
                .put("messages", messages)
                .putString("color", "black")
                .putBoolean("has_glowing_text", false)
                .build();
        return CompoundBinaryTag.builder()
                .put("front_text", front)
                .put("back_text", back)
                .build();
    }

    private void scheduleJoinTeleport(net.minestom.server.entity.Player player) {
        if (joinTeleportMode == JoinTeleportMode.NONE) {
            return;
        }
        Runnable task = () -> runJoinTeleport(player);
        if (joinTeleportDelaySeconds <= 0) {
            task.run();
            return;
        }
        MinecraftServer.getSchedulerManager()
                .buildTask(task)
                .delay(Duration.ofSeconds(joinTeleportDelaySeconds))
                .schedule();
    }

    private void runJoinTeleport(net.minestom.server.entity.Player player) {
        if (player == null || player.isRemoved()) {
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

    private boolean teleportToLastLocation(net.minestom.server.entity.Player player) {
        if (locationRepository == null) {
            return false;
        }
        PlayerLocation stored;
        try {
            stored = locationRepository.getLocation(storageServerId, player.getUuid());
        } catch (Exception ex) {
            LOGGER.error("Failed to load hub player location.", ex);
            return false;
        }
        if (stored == null) {
            return false;
        }
        if (joinTeleportLastMaxAgeMillis > 0
                && System.currentTimeMillis() - stored.updatedAt() > joinTeleportLastMaxAgeMillis) {
            return false;
        }
        player.teleport(new Pos(stored.x(), stored.y(), stored.z(), stored.yaw(), stored.pitch()));
        return true;
    }

    private boolean teleportToSpawn(net.minestom.server.entity.Player player, boolean force) {
        if ((!spawnTeleportEnabled && !force)) {
            return false;
        }
        player.teleport(new Pos(spawnX, spawnY, spawnZ, spawnYaw, spawnPitch));
        return true;
    }

    private void storeLastLocation(net.minestom.server.entity.Player player) {
        if (player == null || locationRepository == null) {
            return;
        }
        try {
            Pos pos = player.getPosition();
            String worldId = instance != null ? instance.getUuid().toString() : "";
            PlayerLocation stored = new PlayerLocation(
                    storageServerId,
                    player.getUuid(),
                    worldId,
                    pos.x(),
                    pos.y(),
                    pos.z(),
                    pos.yaw(),
                    pos.pitch(),
                    System.currentTimeMillis()
            );
            locationRepository.saveLocation(stored);
        } catch (Exception ex) {
            LOGGER.error("Failed to save hub player location.", ex);
        }
    }

    private void tickJumpRun() {
        if (!jumpRunEnabled || jumpRunFinishPos == null || instance == null) {
            return;
        }
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            Long startedAt = jumpRunStarts.get(player.getUuid());
            if (startedAt == null) {
                continue;
            }
            if (player.getInstance() != instance) {
                continue;
            }
            Pos pos = player.getPosition();
            if (pos.blockX() == jumpRunFinishPos.blockX()
                    && pos.blockY() == jumpRunFinishPos.blockY()
                    && pos.blockZ() == jumpRunFinishPos.blockZ()) {
                completeJumpRun(player, startedAt);
            }
        }
    }

    private void startJumpRun(Player player) {
        if (!jumpRunEnabled) {
            player.sendMessage(tr(player, "lobby.jump.disabled"));
            return;
        }
        if (jumpRunStartPos == null || jumpRunFinishPos == null) {
            generateJumpRunCourse(true);
        }
        if (jumpRunStartPos == null) {
            player.sendMessage(tr(player, "lobby.jump.no-course"));
            return;
        }
        jumpRunStarts.put(player.getUuid(), System.currentTimeMillis());
        if (jumpRunTeleportOnStart) {
            player.teleport(new Pos(jumpRunStartPos.x() + 0.5, jumpRunStartPos.y() + 1.0, jumpRunStartPos.z() + 0.5,
                    player.getPosition().yaw(), player.getPosition().pitch()));
        }
        player.sendMessage(tr(player, "lobby.jump.started"));
    }

    private void stopJumpRun(Player player) {
        if (jumpRunStarts.remove(player.getUuid()) != null) {
            player.sendMessage(tr(player, "lobby.jump.stopped"));
        } else {
            player.sendMessage(tr(player, "lobby.jump.no-active"));
        }
    }

    private void sendJumpRunInfo(CommandSender sender) {
        Component state = jumpRunEnabled
                ? tr(sender, "lobby.state.enabled")
                : tr(sender, "lobby.state.disabled");
        sender.sendMessage(tr(sender, "lobby.jump.info.status", Placeholder.component("state", state)));
        sender.sendMessage(tr(sender, "lobby.jump.info.course",
                Placeholder.unparsed("course", jumpRunCourseId),
                Placeholder.unparsed("blocks", String.valueOf(jumpRunBlocks))));
    }

    private void completeJumpRun(Player player, long startedAt) {
        jumpRunStarts.remove(player.getUuid());
        long duration = Math.max(0L, System.currentTimeMillis() - startedAt);
        long best = duration;
        long runCount = 1L;
        if (scoreRepository != null) {
            try {
                var score = scoreRepository.recordRun(jumpRunCourseId, player.getUuid(), duration);
                if (score != null) {
                    best = score.bestTimeMillis();
                    runCount = score.runCount();
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to record jump and run score.", ex);
            }
        }
        player.sendMessage(tr(player, "lobby.jump.finish",
                Placeholder.unparsed("time", formatDuration(duration)),
                Placeholder.unparsed("best", formatDuration(best)),
                Placeholder.unparsed("runs", String.valueOf(runCount))));
    }

    private void generateJumpRunCourse(boolean announce) {
        if (!jumpRunEnabled) {
            if (announce) {
                LOGGER.info("Jump and run is disabled in config.");
            }
            return;
        }
        if (instance == null) {
            if (announce) {
                LOGGER.warn("Jump and run instance not ready.");
            }
            return;
        }
        clearJumpRunCourse();
        jumpRunPositions.clear();
        jumpRunStarts.clear();

        Random random = new Random();
        int baseX = spawnTeleportEnabled ? (int) Math.round(spawnX) : 0;
        int baseY = spawnTeleportEnabled ? (int) Math.round(spawnY) : 41;
        int baseZ = spawnTeleportEnabled ? (int) Math.round(spawnZ) : 0;
        if (jumpRunMinY > 0) {
            baseY = Math.max(baseY, jumpRunMinY);
        }
        baseY = Math.min(baseY, jumpRunMaxY);

        int x = baseX;
        int y = baseY;
        int z = baseZ;
        for (int i = 0; i < jumpRunBlocks; i++) {
            Block blockType = jumpRunBlock;
            if (i == 0) {
                blockType = jumpRunStartBlock;
            } else if (i == jumpRunBlocks - 1) {
                blockType = jumpRunFinishBlock;
            }
            instance.setBlock(x, y, z, blockType);
            BlockVec pos = new BlockVec(x, y, z);
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
            LOGGER.info("Jump and run course generated with " + jumpRunBlocks + " blocks.");
        }
    }

    private void clearJumpRunCourse() {
        if (instance == null || jumpRunPositions.isEmpty()) {
            return;
        }
        for (BlockVec pos : jumpRunPositions) {
            instance.setBlock(pos, Block.AIR);
        }
    }

    private Block resolveJumpRunBlock(String raw, Block fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        Block block = Block.fromKey(normalized);
        return block != null ? block : fallback;
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
        long totalSeconds = Math.max(0L, millis) / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long ms = Math.abs(millis) % 1000L;
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%d:%02d.%03d", minutes, seconds, ms);
        }
        return String.format(Locale.ROOT, "%d.%03ds", seconds, ms);
    }

    private void handleLobbyAction(Player player, NavigatorEntrySpec.NavigatorAction action, String server,
                                   String targetWorld, double x, double y, double z, float yaw, float pitch) {
        if (action == NavigatorEntrySpec.NavigatorAction.SERVER) {
            sendConnectRequest(player, server);
            return;
        }
        if (action == NavigatorEntrySpec.NavigatorAction.LOBBY_SELECTOR) {
            player.sendMessage(tr(player, "lobby.selector.unavailable"));
            return;
        }
        player.teleport(new Pos(x, y, z, yaw, pitch));
    }

    private void sendConnectRequest(Player player, String server) {
        if (server == null || server.isBlank()) {
            player.sendMessage(tr(player, "lobby.server.missing"));
            return;
        }
        byte[] inner = buildConnectPayload(player, server);
        byte[] payload = wrapPayload(inner);
        player.sendPluginMessage(CHANNEL, payload);
        player.sendMessage(tr(player, "lobby.connecting", Placeholder.unparsed("server", server)));
    }

    private byte[] buildConnectPayload(Player player, String target) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("CONNECT");
            out.writeUTF(player.getUuid().toString());
            out.writeUTF(target);
            out.flush();
            return bos.toByteArray();
        } catch (Exception ex) {
            return new byte[0];
        }
    }

    private byte[] wrapPayload(byte[] inner) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeVarInt(bos, inner.length);
        bos.writeBytes(inner);
        return bos.toByteArray();
    }

    private void writeVarInt(ByteArrayOutputStream out, int value) {
        int v = value;
        while ((v & -128) != 0) {
            out.write(v & 127 | 128);
            v >>>= 7;
        }
        out.write(v);
    }

    private EntityType resolveEntityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EntityType.VILLAGER;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        EntityType type = EntityType.fromKey(normalized);
        return type != null ? type : EntityType.VILLAGER;
    }

    private boolean isCompass(ItemStack stack) {
        return stack != null && !stack.isAir() && stack.material() == compassMat;
    }

    private boolean isNavigator(ItemStack stack) {
        return stack != null && !stack.isAir() && stack.material() == navigatorMat;
    }

    private void openNavigator(net.minestom.server.entity.Player player) {
        player.sendMessage(tr(player, "lobby.navigator.list.header"));
        for (NavigatorEntrySpec entry : navigatorConfig.entries()) {
            String name = entry.name() != null ? entry.name() : "<yellow>Entry";
            player.sendMessage(MINI.deserialize(name));
        }
    }

    private MinecraftServer initServer() {
        String secret = System.getenv().getOrDefault("VELOCITY_SECRET", velocitySecret);
        if (secret == null || secret.isBlank()) {
            return MinecraftServer.init();
        }
        LOGGER.info("Enabling Velocity forwarding for Minestom with provided secret.");
        return MinecraftServer.init(new Velocity(secret));
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

    private enum JoinTeleportMode {
        NONE,
        SPAWN,
        LAST
    }
}
