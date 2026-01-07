package net.uebliche.hub.fabric;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.entity.EntitySpawnReason;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.uebliche.hub.common.i18n.I18n;
import net.uebliche.hub.common.model.CompassConfig;
import net.uebliche.hub.common.model.ItemSpec;
import net.uebliche.hub.common.model.LobbyNpcSpec;
import net.uebliche.hub.common.model.LobbySignSpec;
import net.uebliche.hub.common.model.NavigatorConfig;
import net.uebliche.hub.common.model.NavigatorEntrySpec;
import net.uebliche.hub.common.model.LobbyListEntry;
import net.uebliche.hub.common.HubEntrypoint;
import net.uebliche.hub.common.service.CompassService;
import net.uebliche.hub.common.service.LobbyDisplayService;
import net.uebliche.hub.common.storage.JumpRunScoreRepository;
import net.uebliche.hub.common.storage.PlayerLocation;
import net.uebliche.hub.common.storage.PlayerLocationRepository;
import net.uebliche.hub.common.storage.StorageBackendType;
import net.uebliche.hub.common.storage.StorageLogger;
import net.uebliche.hub.common.storage.StorageManager;
import net.uebliche.hub.common.storage.StorageSettings;
import net.uebliche.hub.common.update.UpdateChecker;
import net.uebliche.hub.fabric.net.HubPayload;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.JsonOps;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fabric loader implementation: gives hub items, GUI selectors, gameplay
 * protection (damage/hunger), cross-world teleport and server connect support.
 */
public class HubFabric implements ModInitializer, HubEntrypoint {
    public static final String MOD_ID = "hub";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private final CompassService configService = new CompassService();
    private CompassConfig compassConfig = CompassConfig.fallback();
    private NavigatorConfig navigatorConfig = NavigatorConfig.fallback();
    private Item compassItem = Items.COMPASS;
    private Item navigatorItem = Items.COMPASS;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private boolean disableDamage = true;
    private boolean disableHunger = true;
    private boolean healOnJoin = true;
    private boolean spawnCommandEnabled = true;
    private String i18nDefaultLocale = "en_us";
    private boolean i18nUseClientLocale = true;
    private Map<String, Map<String, String>> i18nOverrides = new HashMap<>();
    private long cacheTtlMillis = 10_000L;
    private LobbyCacheService lobbyCacheService;
    private MenuService menuService;
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
    private Block jumpRunBlock = Blocks.QUARTZ_BLOCK;
    private Block jumpRunStartBlock = Blocks.EMERALD_BLOCK;
    private Block jumpRunFinishBlock = Blocks.GOLD_BLOCK;
    private boolean jumpRunTeleportOnStart = true;
    private final Map<UUID, Long> jumpRunStarts = new HashMap<>();
    private final List<BlockPos> jumpRunPositions = new ArrayList<>();
    private BlockPos jumpRunStartPos;
    private BlockPos jumpRunFinishPos;
    private StorageManager storageManager;
    private PlayerLocationRepository locationRepository;
    private JumpRunScoreRepository scoreRepository;
    private String storageServerId = "lobby";
    private final Map<UUID, Long> joinTeleportSchedule = new HashMap<>();
    private long serverTicks = 0;
    private static final Identifier HUB_CHANNEL = Identifier.fromNamespaceAndPath("uebliche", "hub");
    private final LobbyDisplayService lobbyDisplayService = new LobbyDisplayService();
    private LobbyDisplayService.LobbyDisplayConfig<LobbyNpcSpec> lobbyNpcConfig =
            new LobbyDisplayService.LobbyDisplayConfig<>(false, List.of());
    private LobbyDisplayService.LobbyDisplayConfig<LobbySignSpec> lobbySignConfig =
            new LobbyDisplayService.LobbyDisplayConfig<>(false, List.of());
    private final java.util.Map<java.util.UUID, LobbyNpcSpec> lobbyNpcById = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Entity> lobbyNpcEntities = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Entity> lobbyNpcTextEntities = new java.util.HashMap<>();
    private final java.util.Map<SignKey, LobbySignSpec> lobbySignByKey = new java.util.HashMap<>();
    private volatile java.util.List<LobbyListEntry> lastLobbyEntries = java.util.List.of();

    @Override
    public void onInitialize() {
        loadConfig();
        this.lobbyCacheService = new LobbyCacheService(cacheTtlMillis);
        this.menuService = new MenuService(compassConfig, navigatorConfig, mini,
                material -> resolveItem(material, Items.COMPASS), lobbyCacheService,
                this::teleportPlayer, this::sendServerConnect,
                this::openLobbySelectorFromNavigator);
        registerItems();
        registerMenus();
        registerGameplayGuards();
        registerTransport();
        registerJoinTeleports();
        registerLobbyDisplays();
        java.util.concurrent.CompletableFuture.runAsync(() -> UpdateChecker.checkModrinth("HrTclB8n", MOD_ID,
                msg -> LOGGER.info(msg)));
        PayloadTypeRegistry.playC2S().register(HubPayload.TYPE, HubPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HubPayload.TYPE, HubPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HubPayload.TYPE, (payload, context) -> {
            ByteArrayDataInput in = ByteStreams.newDataInput(payload.data());
            String type = in.readUTF();
            if (!"LIST".equalsIgnoreCase(type))
                return;
            UUID playerId = context.player().getUUID();
            try {
                playerId = UUID.fromString(in.readUTF());
            } catch (Exception ignored) {
            }
            int count = in.readInt();
            java.util.List<LobbyListEntry> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String lobby = in.readUTF();
                String targetServer = in.readUTF();
                int online = in.readInt();
                int max = in.readInt();
                entries.add(new LobbyListEntry(lobby, targetServer, online, max));
            }
            UUID finalPlayerId = playerId;
            var srv = context.player().level().getServer();
            if (srv != null) {
                srv.execute(() -> {
                    lobbyCacheService.store(finalPlayerId, entries);
                    lastLobbyEntries = entries;
                    refreshLobbyNpcDisplays(srv);
                    if (lobbyCacheService.consumePending(finalPlayerId)
                            && context.player().getUUID().equals(finalPlayerId)) {
                        menuService.openMenu(context.player(), SelectionMode.COMPASS);
                    }
                });
            }
        });
        LOGGER.info(
                "HUB Fabric loader initialized with compass/navigator menus, gameplay guards, teleport, and server connect.");
    }

    private void registerLobbyDisplays() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            spawnLobbyNpcs(server);
            applyLobbySigns(server);
            if (jumpRunEnabled) {
                generateJumpRunCourse(server, false);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            lobbyNpcById.clear();
            lobbyNpcEntities.clear();
            lobbyNpcTextEntities.clear();
            lobbySignByKey.clear();
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            LobbyNpcSpec spec = lobbyNpcById.get(entity.getUUID());
            if (spec == null) {
                return InteractionResult.PASS;
            }
            performLobbyAction(serverPlayer, spec.action(), spec.server(), spec.targetWorld(),
                    spec.targetX(), spec.targetY(), spec.targetZ(), spec.targetYaw(), spec.targetPitch());
            return InteractionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hitResult.getBlockPos();
            SignKey key = new SignKey(world.dimension(), pos);
            LobbySignSpec spec = lobbySignByKey.get(key);
            if (spec == null) {
                return InteractionResult.PASS;
            }
            performLobbyAction(serverPlayer, spec.action(), spec.server(), spec.targetWorld(),
                    spec.targetX(), spec.targetY(), spec.targetZ(), spec.targetYaw(), spec.targetPitch());
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    public void registerItems() {
        // Give items on player join
        ServerLifecycleEvents.SERVER_STARTED
                .register(server -> server.getPlayerList().getPlayers().forEach(this::giveHubItems));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> giveHubItems(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> giveHubItemsOnRespawn(newPlayer));
    }

    @Override
    public void registerMenus() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("hubnav")
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                int idx = IntegerArgumentType.getInteger(ctx, "index") - 1;
                                performNavigatorAction(player, idx);
                                return 1;
                            })));
            if (spawnCommandEnabled) {
                dispatcher.register(Commands.literal("spawn")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            teleportToSpawn(player);
                            return 1;
                        }));
            }
            dispatcher.register(Commands.literal("hubnpc")
                    .then(Commands.literal("list")
                            .executes(ctx -> listNpcs(ctx.getSource())))
                    .then(Commands.literal("enable")
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                    .executes(ctx -> setNpcEnabled(ctx.getSource(),
                                            BoolArgumentType.getBool(ctx, "enabled")))))
                    .then(Commands.literal("remove")
                            .then(Commands.argument("id", StringArgumentType.word())
                                    .executes(ctx -> removeNpc(ctx.getSource(),
                                            StringArgumentType.getString(ctx, "id")))))
                    .then(Commands.literal("add")
                            .then(Commands.argument("id", StringArgumentType.word())
                                    .then(Commands.argument("action", StringArgumentType.word())
                                            .executes(ctx -> addNpc(ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "id"),
                                                    StringArgumentType.getString(ctx, "action"),
                                                    ""))
                                            .then(Commands.argument("server", StringArgumentType.word())
                                                    .executes(ctx -> addNpc(ctx.getSource(),
                                                            StringArgumentType.getString(ctx, "id"),
                                                            StringArgumentType.getString(ctx, "action"),
                                                            StringArgumentType.getString(ctx, "server"))))))));
            dispatcher.register(Commands.literal("hubjump")
                    .then(Commands.literal("start")
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                startJumpRun(player);
                                return 1;
                            }))
                    .then(Commands.literal("stop")
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                stopJumpRun(player);
                                return 1;
                            }))
                    .then(Commands.literal("generate")
                            .executes(ctx -> {
                                if (!jumpRunEnabled) {
                                    ctx.getSource().sendFailure(tr(ctx.getSource(), "lobby.jump.disabled"));
                                    return 0;
                                }
                                generateJumpRunCourse(ctx.getSource().getServer(), true);
                                return 1;
                            }))
                    .then(Commands.literal("info")
                            .executes(ctx -> {
                                sendJumpRunInfo(ctx.getSource());
                                return 1;
                            })));
        });

        // Handle right-click use
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer))
                return InteractionResult.PASS;
            ItemStack inHand = player.getItemInHand(hand);
            if (isConfigItem(inHand, compassItem, compassConfig.item().name())) {
            if (!lobbyCacheService.hasFreshCache(serverPlayer.getUUID())) {
                requestLobbyList(serverPlayer);
                lobbyCacheService.markPending(serverPlayer.getUUID());
            }
            menuService.openMenu(serverPlayer, SelectionMode.COMPASS);
            return InteractionResult.SUCCESS;
        }
        if (navigatorConfig.enabled() && isConfigItem(inHand, navigatorItem, navigatorConfig.item().name())) {
            menuService.openMenu(serverPlayer, SelectionMode.NAVIGATOR);
            return InteractionResult.SUCCESS;
        }
            return InteractionResult.PASS;
        });
    }

    @Override
    public void registerGameplayGuards() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (disableDamage && entity instanceof ServerPlayer) {
                return false;
            }
            return true;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            serverTicks++;
            processJoinTeleportSchedule(server);
            tickJumpRun(server);
            if (!disableHunger) {
                return;
            }
            server.getPlayerList().getPlayers().forEach(player -> {
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20f);
            });
        });
    }

    @Override
    public void registerTransport() {
        // Transport handled via UseItem + navigator selection; no extra hook needed.
    }

    private void registerJoinTeleports() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> scheduleJoinTeleport(handler.player, server));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            storeLastLocation(handler.player);
            joinTeleportSchedule.remove(handler.player.getUUID());
            jumpRunStarts.remove(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            server.getPlayerList().getPlayers().forEach(this::storeLastLocation);
            joinTeleportSchedule.clear();
            if (storageManager != null) {
                storageManager.close();
            }
        });
    }

    private int listNpcs(CommandSourceStack source) {
        if (lobbyNpcConfig.entries().isEmpty()) {
            source.sendSuccess(() -> tr(source, "lobby.npc.none"), false);
            return 1;
        }
        source.sendSuccess(() -> tr(source, "lobby.npc.list.header"), false);
        lobbyNpcConfig.entries().forEach(spec -> source.sendSuccess(
                () -> tr(source, "lobby.npc.list.entry", Placeholder.unparsed("id", spec.id())),
                false
        ));
        return 1;
    }

    private int setNpcEnabled(CommandSourceStack source, boolean enabled) {
        try {
            Path configPath = resolveConfigPath();
            ensureConfigExists(configPath);
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(configPath).build();
            ConfigurationNode node = loader.load();
            node.node("lobby-npcs", "enabled").set(enabled);
            loader.save(node);
            loadConfig();
            reloadLobbyDisplays(source.getServer());
            String locale = resolveLocale(source);
            String stateRaw = resolveRaw(locale, enabled ? "lobby.state.enabled" : "lobby.state.disabled");
            source.sendSuccess(() -> tr(source, "lobby.npc.enabled", Placeholder.parsed("state", stateRaw)), false);
            return 1;
        } catch (Exception ex) {
            source.sendFailure(tr(source, "lobby.npc.update.failed"));
            LOGGER.error("Failed to update lobby NPCs.", ex);
            return 0;
        }
    }

    private int removeNpc(CommandSourceStack source, String id) {
        if (id == null || id.isBlank()) {
            source.sendFailure(tr(source, "lobby.npc.id.required"));
            return 0;
        }
        try {
            Path configPath = resolveConfigPath();
            ensureConfigExists(configPath);
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(configPath).build();
            ConfigurationNode node = loader.load();
            ConfigurationNode entry = node.node("lobby-npcs", "entries", id);
            if (entry.virtual()) {
                source.sendFailure(tr(source, "lobby.npc.not-found", Placeholder.unparsed("id", id)));
                return 0;
            }
            entry.raw(null);
            loader.save(node);
            loadConfig();
            reloadLobbyDisplays(source.getServer());
            source.sendSuccess(() -> tr(source, "lobby.npc.removed", Placeholder.unparsed("id", id)), false);
            return 1;
        } catch (Exception ex) {
            source.sendFailure(tr(source, "lobby.npc.remove.failed"));
            LOGGER.error("Failed to remove lobby NPC.", ex);
            return 0;
        }
    }

    private int addNpc(CommandSourceStack source, String id, String action, String server) {
        if (id == null || id.isBlank()) {
            source.sendFailure(tr(source, "lobby.npc.id.required"));
            return 0;
        }
        if (!isNpcAction(action)) {
            source.sendFailure(tr(source, "lobby.npc.action.invalid"));
            return 0;
        }
        if ("server".equalsIgnoreCase(action) && (server == null || server.isBlank())) {
            source.sendFailure(tr(source, "lobby.npc.server.required"));
            return 0;
        }
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ex) {
            source.sendFailure(tr(source, "lobby.npc.only-player-add"));
            return 0;
        }
        try {
            Path configPath = resolveConfigPath();
            ensureConfigExists(configPath);
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(configPath).build();
            ConfigurationNode node = loader.load();
            ConfigurationNode entry = node.node("lobby-npcs", "entries", id);
            entry.node("world").set(serializeDimension(player.level()));
            entry.node("x").set(player.getX());
            entry.node("y").set(player.getY());
            entry.node("z").set(player.getZ());
            entry.node("yaw").set(player.getYRot());
            entry.node("pitch").set(player.getXRot());
            entry.node("name").set("<gold>" + id);
            entry.node("entity").set(entry.node("entity").getString("VILLAGER"));
            entry.node("action").set(action.toLowerCase(Locale.ROOT));
            if (server != null && !server.isBlank()) {
                entry.node("server").set(server);
            }
            node.node("lobby-npcs", "enabled").set(true);
            loader.save(node);
            loadConfig();
            reloadLobbyDisplays(source.getServer());
            source.sendSuccess(() -> tr(source, "lobby.npc.saved", Placeholder.unparsed("id", id)), false);
            return 1;
        } catch (Exception ex) {
            source.sendFailure(tr(source, "lobby.npc.save.failed"));
            LOGGER.error("Failed to save lobby NPC.", ex);
            return 0;
        }
    }

    private void reloadLobbyDisplays(MinecraftServer server) {
        if (server == null) {
            return;
        }
        removeLobbyNpcs(server);
        spawnLobbyNpcs(server);
        applyLobbySigns(server);
    }

    private void removeLobbyNpcs(MinecraftServer server) {
        if (lobbyNpcById.isEmpty()) {
            return;
        }
        for (UUID id : new ArrayList<>(lobbyNpcById.keySet())) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
            }
        }
        lobbyNpcById.clear();
    }

    private boolean isNpcAction(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("server") || normalized.equals("lobby") || normalized.equals("teleport");
    }

    private Path resolveConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("hub.yaml");
    }

    private void ensureConfigExists(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            writeDefaults(path);
        }
    }

    private String serializeDimension(Level level) {
        if (level.dimension() == Level.OVERWORLD) {
            return "world";
        }
        if (level.dimension() == Level.NETHER) {
            return "nether";
        }
        if (level.dimension() == Level.END) {
            return "end";
        }
        return level.dimension().toString();
    }

    private void spawnLobbyNpcs(MinecraftServer server) {
        lobbyNpcById.clear();
        lobbyNpcEntities.clear();
        clearLobbyNpcText(server);
        if (!lobbyNpcConfig.enabled()) {
            return;
        }
        for (LobbyNpcSpec spec : lobbyNpcConfig.entries()) {
            Identifier worldId = normalizeDimensionId(spec.world());
            if (worldId == null) {
                continue;
            }
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
            if (level == null) {
                continue;
            }
            EntityType<?> type = resolveEntityType(spec.entity());
            Entity entity = type.create(level, EntitySpawnReason.COMMAND);
            if (entity == null) {
                continue;
            }
            LobbyListEntry entry = findLobbyEntry(spec);
            List<String> lines = spec.lines() != null ? spec.lines() : List.of();
            entity.setPos(spec.x(), spec.y(), spec.z());
            entity.setYRot(spec.yaw());
            entity.setXRot(spec.pitch());
            if (lines.isEmpty()) {
                entity.setCustomName(mm(applyNpcPlaceholders(spec.name(), spec, entry)));
                entity.setCustomNameVisible(true);
            } else {
                entity.setCustomName(Component.empty());
                entity.setCustomNameVisible(false);
            }
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setNoGravity(true);
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
            }
            level.addFreshEntity(entity);
            lobbyNpcById.put(entity.getUUID(), spec);
            lobbyNpcEntities.put(entity.getUUID(), entity);
            if (!lines.isEmpty()) {
                spawnNpcText(level, spec, entry);
            }
        }
    }

    private void applyLobbySigns(MinecraftServer server) {
        lobbySignByKey.clear();
        if (!lobbySignConfig.enabled()) {
            return;
        }
        for (LobbySignSpec spec : lobbySignConfig.entries()) {
            Identifier worldId = normalizeDimensionId(spec.world());
            if (worldId == null) {
                continue;
            }
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
            if (level == null) {
                continue;
            }
            BlockPos pos = new BlockPos(spec.x(), spec.y(), spec.z());
            if (!(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.SignBlock)) {
                level.setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState());
            }
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SignBlockEntity sign) {
                SignText text = sign.getFrontText();
                List<String> lines = spec.lines() != null ? spec.lines() : List.of();
                for (int i = 0; i < 4; i++) {
                    String raw = i < lines.size() ? lines.get(i) : "";
                    text = text.setMessage(i, mm(raw));
                }
                applySignFrontText(sign, text);
                sign.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                lobbySignByKey.put(new SignKey(level.dimension(), pos), spec);
            }
        }
    }

    private void refreshLobbyNpcDisplays(MinecraftServer server) {
        if (server == null || !lobbyNpcConfig.enabled()) {
            return;
        }
        if (lobbyNpcEntities.isEmpty()) {
            spawnLobbyNpcs(server);
            return;
        }
        clearLobbyNpcText(server);
        for (var entry : lobbyNpcById.entrySet()) {
            LobbyNpcSpec spec = entry.getValue();
            Entity entity = lobbyNpcEntities.get(entry.getKey());
            LobbyListEntry lobbyEntry = findLobbyEntry(spec);
            List<String> lines = spec.lines() != null ? spec.lines() : List.of();
            if (entity != null) {
                if (lines.isEmpty()) {
                    entity.setCustomName(mm(applyNpcPlaceholders(spec.name(), spec, lobbyEntry)));
                    entity.setCustomNameVisible(true);
                } else {
                    entity.setCustomName(Component.empty());
                    entity.setCustomNameVisible(false);
                }
            }
            if (!lines.isEmpty()) {
                Identifier worldId = normalizeDimensionId(spec.world());
                if (worldId == null) {
                    continue;
                }
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
                if (level == null) {
                    continue;
                }
                spawnNpcText(level, spec, lobbyEntry);
            }
        }
    }

    private void clearLobbyNpcText(MinecraftServer server) {
        if (lobbyNpcTextEntities.isEmpty()) {
            return;
        }
        for (Entity entity : lobbyNpcTextEntities.values()) {
            if (entity != null && !entity.isRemoved()) {
                entity.discard();
            }
        }
        lobbyNpcTextEntities.clear();
    }

    private void spawnNpcText(ServerLevel level, LobbyNpcSpec spec, LobbyListEntry entry) {
        List<String> lines = spec.lines() != null ? spec.lines() : List.of();
        if (lines.isEmpty()) {
            return;
        }
        double spacing = 0.25;
        double baseY = spec.y() + 2.2 + (lines.size() - 1) * spacing;
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            double y = baseY - i * spacing;
            Entity text = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
            if (text == null) {
                continue;
            }
            text.setPos(spec.x(), y, spec.z());
            text.setCustomName(mm(applyNpcPlaceholders(raw, spec, entry)));
            text.setCustomNameVisible(true);
            text.setInvulnerable(true);
            text.setSilent(true);
            text.setNoGravity(true);
            if (text instanceof net.minecraft.world.entity.decoration.ArmorStand stand) {
                stand.setInvisible(true);
            }
            level.addFreshEntity(text);
            lobbyNpcTextEntities.put(text.getUUID(), text);
        }
    }

    private LobbyListEntry findLobbyEntry(LobbyNpcSpec spec) {
        if (lastLobbyEntries.isEmpty()) {
            return null;
        }
        String serverKey = normalizeKey(spec.server());
        String idKey = normalizeKey(spec.id());
        if (!serverKey.isBlank()) {
            for (LobbyListEntry entry : lastLobbyEntries) {
                if (normalizeKey(entry.lobby()).equals(serverKey) || normalizeKey(entry.server()).equals(serverKey)) {
                    return entry;
                }
            }
        }
        if (!idKey.isBlank()) {
            for (LobbyListEntry entry : lastLobbyEntries) {
                if (normalizeKey(entry.lobby()).equals(idKey) || normalizeKey(entry.server()).equals(idKey)) {
                    return entry;
                }
            }
        }
        return null;
    }

    private String applyNpcPlaceholders(String template, LobbyNpcSpec spec, LobbyListEntry entry) {
        if (template == null) {
            return "";
        }
        String lobby = entry != null && entry.lobby() != null ? entry.lobby() : defaultLobbyName(spec);
        String server = entry != null && entry.server() != null ? entry.server() : valueOrEmpty(spec.server());
        String online = entry != null ? String.valueOf(entry.online()) : "0";
        String max = entry != null ? String.valueOf(entry.max()) : "0";
        return template
                .replace("<id>", valueOrEmpty(spec.id()))
                .replace("<name>", valueOrEmpty(spec.name()))
                .replace("<lobby>", valueOrEmpty(lobby))
                .replace("<server>", valueOrEmpty(server))
                .replace("<online>", online)
                .replace("<max>", max);
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
        return raw.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String valueOrEmpty(String raw) {
        return raw == null ? "" : raw;
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

    private void applySignFrontText(SignBlockEntity sign, SignText text) {
        try {
            var method = SignBlockEntity.class.getDeclaredMethod("setFrontText", SignText.class);
            method.setAccessible(true);
            method.invoke(sign, text);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            var method = SignBlockEntity.class.getDeclaredMethod("setText", SignText.class, boolean.class);
            method.setAccessible(true);
            method.invoke(sign, text, true);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void performLobbyAction(ServerPlayer player, NavigatorEntrySpec.NavigatorAction action, String server,
                                    String worldName, double x, double y, double z, float yaw, float pitch) {
        switch (action) {
            case SERVER -> sendServerConnect(player, server);
            case LOBBY_SELECTOR -> openLobbySelectorFromNavigator(player);
            default -> teleportTo(player, worldName, x, y, z, yaw, pitch);
        }
    }

    private void teleportTo(ServerPlayer player, String worldName, double x, double y, double z, float yaw, float pitch) {
        Identifier worldId = normalizeDimensionId(worldName);
        if (worldId == null) {
            player.sendSystemMessage(tr(player, "lobby.world.invalid", Placeholder.unparsed("world", worldName)));
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            player.sendSystemMessage(tr(player, "lobby.server.not-available"));
            return;
        }
        ServerLevel targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
        if (targetLevel == null) {
            player.sendSystemMessage(tr(player, "lobby.world.not-found", Placeholder.unparsed("world", worldName)));
            return;
        }
        if (player.level() == targetLevel) {
            player.connection.teleport(x, y, z, yaw, pitch);
            return;
        }
        try {
            Class<?> relClass = Class.forName("net.minecraft.world.entity.RelativeMovement");
            @SuppressWarnings({ "rawtypes", "unchecked" })
            java.util.Set rel = java.util.EnumSet.noneOf((Class) relClass);
            player.teleportTo(targetLevel, x, y, z, rel, yaw, pitch, true);
        } catch (Exception ex) {
            player.connection.teleport(x, y, z, yaw, pitch);
        }
    }

    private EntityType<?> resolveEntityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EntityType.VILLAGER;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        Identifier id = Identifier.tryParse(normalized);
        if (id == null) {
            return EntityType.VILLAGER;
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(EntityType.VILLAGER);
    }

    private record SignKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private void giveHubItems(ServerPlayer player) {
        ItemStack compass = buildItem(compassConfig.item(), compassItem);
        placeInSlot(player, compassConfig.item().slot(), compass);
        if (navigatorConfig.enabled()) {
            ItemStack nav = buildItem(navigatorConfig.item(), navigatorItem);
            placeInSlot(player, navigatorConfig.item().slot(), nav);
        }
        if (healOnJoin) {
            resetVitals(player);
        }
    }

    private void giveHubItemsOnRespawn(ServerPlayer player) {
        if (compassConfig.item().restoreOnRespawn()) {
            ItemStack compass = buildItem(compassConfig.item(), compassItem);
            placeInSlot(player, compassConfig.item().slot(), compass);
        }
        if (navigatorConfig.enabled() && navigatorConfig.item().restoreOnRespawn()) {
            ItemStack nav = buildItem(navigatorConfig.item(), navigatorItem);
            placeInSlot(player, navigatorConfig.item().slot(), nav);
        }
        if (healOnJoin) {
            resetVitals(player);
        }
        teleportToSpawn(player);
    }

    private void placeInSlot(ServerPlayer player, int slot, ItemStack stack) {
        Inventory inv = player.getInventory();
        if (slot >= 0 && slot < inv.getContainerSize()) {
            inv.setItem(slot, stack);
        } else {
            player.getInventory().add(stack);
        }
    }

    private ItemStack buildItem(ItemSpec spec, Item defaultItem) {
        ItemStack stack = new ItemStack(defaultItem);
        String cleanName = stripTags(spec.name());
        if (!cleanName.isEmpty()) {
            stack.set(DataComponents.CUSTOM_NAME, mm(spec.name()));
        }
        var lore = spec.lore().stream().map(this::mm).toList();
        if (!lore.isEmpty()) {
            stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
        }
        return stack;
    }

    private boolean isConfigItem(ItemStack stack, Item target, String name) {
        if (stack == null)
            return false;
        if (!stack.is(target))
            return false;
        String cleanName = stripTags(name);
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            return Objects.equals(customName.getString(), cleanName);
        }
        return true;
    }

    private void performNavigatorAction(ServerPlayer player, int index) {
        if (index < 0 || index >= navigatorConfig.entries().size()) {
            player.sendSystemMessage(tr(player, "lobby.selection.invalid"));
            return;
        }
        NavigatorEntrySpec entry = navigatorConfig.entries().get(index);
        switch (entry.action()) {
            case SERVER -> player.sendSystemMessage(tr(player, "lobby.server.unsupported"));
            case LOBBY_SELECTOR -> openLobbySelectorFromNavigator(player);
            default -> teleportPlayer(player, entry);
        }
    }

    private void teleportPlayer(ServerPlayer player, NavigatorEntrySpec entry) {
        Identifier worldId = normalizeDimensionId(entry.world());
        if (worldId == null) {
            player.sendSystemMessage(tr(player, "lobby.world.invalid", Placeholder.unparsed("world", entry.world())));
            return;
        }
        Level currentLevel = player.level();
        MinecraftServer server = currentLevel.getServer();
        if (server == null) {
            player.sendSystemMessage(tr(player, "lobby.server.not-available"));
            return;
        }
        ServerLevel targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
        if (targetLevel == null) {
            player.sendSystemMessage(tr(player, "lobby.world.not-found", Placeholder.unparsed("world", entry.world())));
            return;
        }
        if (player.level() == targetLevel) {
            player.connection.teleport(entry.x(), entry.y(), entry.z(), entry.yaw(), entry.pitch());
            return;
        }
        try {
            Class<?> relClass = Class.forName("net.minecraft.world.entity.RelativeMovement");
            @SuppressWarnings({ "rawtypes", "unchecked" })
            java.util.Set rel = java.util.EnumSet.noneOf((Class) relClass);
            player.teleportTo(targetLevel, entry.x(), entry.y(), entry.z(), rel, entry.yaw(), entry.pitch(), true);
        } catch (Exception ex) {
            // fallback to simple teleport
            player.connection.teleport(entry.x(), entry.y(), entry.z(), entry.yaw(), entry.pitch());
        }
    }

    private void openLobbySelectorFromNavigator(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!lobbyCacheService.hasFreshCache(id)) {
            requestLobbyList(player);
            lobbyCacheService.markPending(id);
            return;
        }
        menuService.openMenu(player, SelectionMode.COMPASS);
    }

    private Identifier normalizeDimensionId(String raw) {
        if (raw == null || raw.isBlank())
            return Identifier.withDefaultNamespace("overworld");
        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (!id.contains(":")) {
            if (id.equals("world") || id.equals("overworld")) {
                id = "minecraft:overworld";
            } else if (id.equals("nether") || id.equals("the_nether")) {
                id = "minecraft:the_nether";
            } else if (id.equals("end") || id.equals("the_end")) {
                id = "minecraft:the_end";
            } else {
                id = "minecraft:" + id;
            }
        }
        return Identifier.tryParse(id);
    }

    @Override
    public void loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve("hub.yaml");
        try {
            Files.createDirectories(configDir);
            if (Files.notExists(configPath)) {
                writeDefaults(configPath);
            }
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();
            ConfigurationNode node = loader.load();
            i18nDefaultLocale = I18n.normalizeLocale(node.node("i18n", "default-locale").getString("en_us"));
            i18nUseClientLocale = node.node("i18n", "use-client-locale").getBoolean(true);
            i18nOverrides = loadI18nOverrides(node, configDir);
            compassConfig = configService.loadCompass(node);
            navigatorConfig = configService.loadNavigator(node);
            compassItem = resolveItem(compassConfig.item().material(), Items.COMPASS);
            navigatorItem = resolveItem(navigatorConfig.item().material(), Items.COMPASS);
            disableDamage = node.node("gameplay", "disable-damage").getBoolean(true);
            disableHunger = node.node("gameplay", "disable-hunger").getBoolean(true);
            healOnJoin = node.node("gameplay", "heal-on-join").getBoolean(true);
            spawnCommandEnabled = node.node("gameplay", "spawn-command").getBoolean(true);
            spawnTeleportEnabled = node.node("spawn-teleport", "enabled").getBoolean(false);
            spawnWorld = node.node("spawn-teleport", "world").getString("world");
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
            joinTeleportDelayTicks = delaySeconds * 20L;
            long maxAgeSeconds = Math.max(0L, node.node("join-teleport", "last-location", "max-age-seconds").getLong(0L));
            joinTeleportLastMaxAgeMillis = maxAgeSeconds <= 0 ? 0L : maxAgeSeconds * 1000L;
            cacheTtlMillis = Math.max(1000L, node.node("compass", "cache-ttl-millis").getLong(10_000L));
            lobbyNpcConfig = lobbyDisplayService.loadNpcs(node);
            lobbySignConfig = lobbyDisplayService.loadSigns(node);
            jumpRunEnabled = node.node("jump-run", "enabled").getBoolean(false);
            jumpRunCourseId = node.node("jump-run", "course-id").getString("default");
            String jumpWorldDefault = spawnWorld != null && !spawnWorld.isBlank() ? spawnWorld : "world";
            jumpRunWorld = node.node("jump-run", "world").getString(jumpWorldDefault);
            jumpRunBlocks = Math.max(3, node.node("jump-run", "blocks").getInt(20));
            jumpRunMinDistance = Math.max(1.0, node.node("jump-run", "spacing", "min").getDouble(2.5));
            jumpRunMaxDistance = Math.max(jumpRunMinDistance, node.node("jump-run", "spacing", "max").getDouble(4.5));
            jumpRunMinYOffset = node.node("jump-run", "y-offset", "min").getInt(-1);
            jumpRunMaxYOffset = node.node("jump-run", "y-offset", "max").getInt(2);
            jumpRunMinY = node.node("jump-run", "height", "min").getInt(70);
            jumpRunMaxY = node.node("jump-run", "height", "max").getInt(140);
            jumpRunTeleportOnStart = node.node("jump-run", "teleport-on-start").getBoolean(true);
            jumpRunBlock = resolveJumpRunBlock(node.node("jump-run", "block").getString("QUARTZ_BLOCK"), Blocks.QUARTZ_BLOCK);
            jumpRunStartBlock = resolveJumpRunBlock(node.node("jump-run", "start-block").getString("EMERALD_BLOCK"), Blocks.EMERALD_BLOCK);
            jumpRunFinishBlock = resolveJumpRunBlock(node.node("jump-run", "finish-block").getString("GOLD_BLOCK"), Blocks.GOLD_BLOCK);
            initStorage(node, configDir);
        } catch (Exception ex) {
            LOGGER.error("Failed to load hub config, using defaults.", ex);
            compassConfig = CompassConfig.fallback();
            navigatorConfig = NavigatorConfig.fallback();
            i18nDefaultLocale = "en_us";
            i18nUseClientLocale = true;
            i18nOverrides = new HashMap<>();
            compassItem = Items.COMPASS;
            navigatorItem = Items.COMPASS;
            disableDamage = true;
            disableHunger = true;
            healOnJoin = true;
            spawnTeleportEnabled = false;
            joinTeleportMode = JoinTeleportMode.NONE;
            joinTeleportDelayTicks = 0;
            joinTeleportLastMaxAgeMillis = 0;
            cacheTtlMillis = 10_000L;
            lobbyNpcConfig = new LobbyDisplayService.LobbyDisplayConfig<>(false, List.of());
            lobbySignConfig = new LobbyDisplayService.LobbyDisplayConfig<>(false, List.of());
            jumpRunEnabled = false;
            jumpRunCourseId = "default";
            jumpRunWorld = spawnWorld != null && !spawnWorld.isBlank() ? spawnWorld : "world";
            jumpRunBlocks = 20;
            jumpRunMinDistance = 2.5;
            jumpRunMaxDistance = 4.5;
            jumpRunMinYOffset = -1;
            jumpRunMaxYOffset = 2;
            jumpRunMinY = 70;
            jumpRunMaxY = 140;
            jumpRunTeleportOnStart = true;
            jumpRunBlock = Blocks.QUARTZ_BLOCK;
            jumpRunStartBlock = Blocks.EMERALD_BLOCK;
            jumpRunFinishBlock = Blocks.GOLD_BLOCK;
            initStorage(null, configDir);
        }
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

    private void writeDefaults(Path path) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
        ConfigurationNode node = loader.createNode();
        CompassConfig cc = CompassConfig.fallback();
        NavigatorConfig nc = NavigatorConfig.fallback();
        node.node("i18n", "default-locale").set("en_us");
        node.node("i18n", "use-client-locale").set(true);
        node.node("i18n", "overrides", "en_us").set(new HashMap<>());
        node.node("i18n", "overrides", "de_de").set(new HashMap<>());
        node.node("compass", "enabled").set(cc.enabled());
        node.node("compass", "gui-title").set(cc.guiTitle());
        node.node("compass", "item", "name").set(cc.item().name());
        node.node("compass", "item", "lore").set(cc.item().lore());
        node.node("compass", "item", "material").set(cc.item().material());
        node.node("compass", "item", "slot").set(cc.item().slot());
        node.node("compass", "item", "allow-move").set(cc.item().allowMove());
        node.node("compass", "item", "allow-drop").set(cc.item().allowDrop());
        node.node("compass", "item", "drop-on-death").set(cc.item().dropOnDeath());
        node.node("compass", "item", "restore-on-respawn").set(cc.item().restoreOnRespawn());
        node.node("compass", "list-item", "name").set(cc.listItemNameTemplate());
        node.node("compass", "list-item", "lore").set(cc.listItemLoreTemplate());

        node.node("navigator", "enabled").set(nc.enabled());
        node.node("navigator", "gui-title").set(nc.guiTitle());
        node.node("navigator", "gui-rows").set(nc.guiRows());
        node.node("navigator", "item", "name").set(nc.item().name());
        node.node("navigator", "item", "lore").set(nc.item().lore());
        node.node("navigator", "item", "material").set(nc.item().material());
        node.node("navigator", "item", "slot").set(nc.item().slot());
        node.node("navigator", "item", "allow-move").set(nc.item().allowMove());
        node.node("navigator", "item", "allow-drop").set(nc.item().allowDrop());
        node.node("navigator", "item", "drop-on-death").set(nc.item().dropOnDeath());
        node.node("navigator", "item", "restore-on-respawn").set(nc.item().restoreOnRespawn());
        node.node("gameplay", "disable-damage").set(true);
        node.node("gameplay", "disable-hunger").set(true);
        node.node("gameplay", "heal-on-join").set(true);
        node.node("gameplay", "spawn-command").set(true);
        node.node("compass", "cache-ttl-millis").set(10_000L);
        node.node("spawn-teleport", "enabled").set(false);
        node.node("spawn-teleport", "world").set("world");
        node.node("spawn-teleport", "x").set(0);
        node.node("spawn-teleport", "y").set(64);
        node.node("spawn-teleport", "z").set(0);
        node.node("spawn-teleport", "yaw").set(0);
        node.node("spawn-teleport", "pitch").set(0);
        node.node("join-teleport", "mode").set("none");
        node.node("join-teleport", "delay-seconds").set(0);
        node.node("join-teleport", "last-location", "max-age-seconds").set(0);
        node.node("jump-run", "enabled").set(false);
        node.node("jump-run", "course-id").set("default");
        node.node("jump-run", "world").set("world");
        node.node("jump-run", "blocks").set(20);
        node.node("jump-run", "spacing", "min").set(2.5);
        node.node("jump-run", "spacing", "max").set(4.5);
        node.node("jump-run", "y-offset", "min").set(-1);
        node.node("jump-run", "y-offset", "max").set(2);
        node.node("jump-run", "height", "min").set(70);
        node.node("jump-run", "height", "max").set(140);
        node.node("jump-run", "block").set("QUARTZ_BLOCK");
        node.node("jump-run", "start-block").set("EMERALD_BLOCK");
        node.node("jump-run", "finish-block").set("GOLD_BLOCK");
        node.node("jump-run", "teleport-on-start").set(true);

        node.node("storage", "server-id").set("lobby");
        node.node("storage", "scores", "primary").set("local-sql");
        node.node("storage", "locations", "primary").set("local-sql");
        node.node("storage", "locations", "cache").set("none");
        node.node("storage", "fallback", "enabled").set(true);
        node.node("storage", "sql", "url").set("jdbc:mariadb://localhost:3306/hub");
        node.node("storage", "sql", "user").set("hub");
        node.node("storage", "sql", "password").set("");
        node.node("storage", "sql", "driver").set("org.mariadb.jdbc.Driver");
        node.node("storage", "local-sql", "url").set("jdbc:sqlite:" + path.getParent().resolve("hub.db").toAbsolutePath());
        node.node("storage", "local-sql", "driver").set("org.sqlite.JDBC");
        node.node("storage", "mongo", "uri").set("mongodb://localhost:27017");
        node.node("storage", "mongo", "database").set("hub");
        node.node("storage", "mongo", "collection-prefix").set("hub_");
        node.node("storage", "redis", "uri").set("redis://localhost:6379");
        node.node("storage", "redis", "database").set(0);
        node.node("storage", "redis", "key-prefix").set("hub");
        node.node("lobby-npcs", "enabled").set(false);
        node.node("lobby-npcs", "entries", "lobby", "world").set("world");
        node.node("lobby-npcs", "entries", "lobby", "x").set(0);
        node.node("lobby-npcs", "entries", "lobby", "y").set(64);
        node.node("lobby-npcs", "entries", "lobby", "z").set(0);
        node.node("lobby-npcs", "entries", "lobby", "yaw").set(0);
        node.node("lobby-npcs", "entries", "lobby", "pitch").set(0);
        node.node("lobby-npcs", "entries", "lobby", "name").set("<gold>Lobby");
        node.node("lobby-npcs", "entries", "lobby", "entity").set("VILLAGER");
        node.node("lobby-npcs", "entries", "lobby", "action").set("server");
        node.node("lobby-npcs", "entries", "lobby", "server").set("lobby");
        node.node("lobby-signs", "enabled").set(false);
        node.node("lobby-signs", "entries", "lobby", "world").set("world");
        node.node("lobby-signs", "entries", "lobby", "x").set(2);
        node.node("lobby-signs", "entries", "lobby", "y").set(64);
        node.node("lobby-signs", "entries", "lobby", "z").set(0);
        node.node("lobby-signs", "entries", "lobby", "lines").set(List.of("<gold>Lobby", "<gray>Click to join"));
        node.node("lobby-signs", "entries", "lobby", "action").set("server");
        node.node("lobby-signs", "entries", "lobby", "server").set("lobby");
        loader.save(node);
    }

    private Item resolveItem(String id, Item fallback) {
        if (id == null || id.isBlank())
            return fallback;
        String normalized = id.toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        Identifier identifier = Identifier.tryParse(normalized);
        if (identifier == null) {
            return fallback;
        }
        return BuiltInRegistries.ITEM.getOptional(identifier).orElse(fallback);
    }

    private String stripTags(String input) {
        if (input == null)
            return "";
        return input.replaceAll("<[^>]+>", "");
    }

    private Component mm(String raw, TagResolver... resolvers) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        try {
            var adv = mini.deserialize(raw, resolvers);
            var vanillaJson = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(adv);
            var jsonElement = JsonParser.parseString(vanillaJson);
            var vanilla = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).result().orElse(null);
            return vanilla != null ? vanilla : Component.literal(stripTags(raw));
        } catch (Exception ex) {
            return Component.literal(stripTags(raw));
        }
    }

    private Component mm(String raw) {
        return mm(raw, TagResolver.empty());
    }

    private Component tr(CommandSourceStack source, String key, TagResolver... resolvers) {
        String locale = resolveLocale(source);
        String raw = resolveRaw(locale, key);
        return mm(raw, resolvers);
    }

    private Component tr(ServerPlayer player, String key, TagResolver... resolvers) {
        String locale = resolveLocale(player);
        String raw = resolveRaw(locale, key);
        return mm(raw, resolvers);
    }

    private String resolveRaw(String locale, String key) {
        String normalized = I18n.normalizeLocale(locale);
        Map<String, String> overrides = i18nOverrides.get(normalized);
        if (overrides != null && overrides.containsKey(key)) {
            return overrides.get(key);
        }
        return I18n.raw(normalized, key);
    }

    private String resolveLocale(CommandSourceStack source) {
        if (!i18nUseClientLocale || source == null) {
            return i18nDefaultLocale;
        }
        try {
            ServerPlayer player = source.getPlayer();
            if (player != null) {
                return resolveLocale(player);
            }
        } catch (Exception ignored) {
        }
        return i18nDefaultLocale;
    }

    private String resolveLocale(ServerPlayer player) {
        if (!i18nUseClientLocale || player == null) {
            return i18nDefaultLocale;
        }
        try {
            var method = player.getClass().getMethod("getLocale");
            Object result = method.invoke(player);
            if (result instanceof java.util.Locale loc) {
                return I18n.normalizeLocale(loc.toLanguageTag());
            }
            if (result instanceof String str) {
                return I18n.normalizeLocale(str);
            }
        } catch (Exception ignored) {
        }
        try {
            var method = player.getClass().getMethod("getLanguage");
            Object result = method.invoke(player);
            if (result instanceof String str) {
                return I18n.normalizeLocale(str);
            }
        } catch (Exception ignored) {
        }
        return i18nDefaultLocale;
    }

    private void resetVitals(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20f);
    }

    private void scheduleJoinTeleport(ServerPlayer player, MinecraftServer server) {
        if (joinTeleportMode == JoinTeleportMode.NONE) {
            return;
        }
        if (joinTeleportDelayTicks <= 0) {
            if (server != null) {
                server.execute(() -> runJoinTeleport(player));
            } else {
                runJoinTeleport(player);
            }
            return;
        }
        joinTeleportSchedule.put(player.getUUID(), serverTicks + joinTeleportDelayTicks);
    }

    private void processJoinTeleportSchedule(MinecraftServer server) {
        if (joinTeleportSchedule.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Long>> iterator = joinTeleportSchedule.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() > serverTicks) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                runJoinTeleport(player);
            }
            iterator.remove();
        }
    }

    private void runJoinTeleport(ServerPlayer player) {
        if (player == null) {
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

    private boolean teleportToLastLocation(ServerPlayer player) {
        if (locationRepository == null) {
            return false;
        }
        PlayerLocation stored;
        try {
            stored = locationRepository.getLocation(storageServerId, player.getUUID());
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
        Identifier worldId = normalizeDimensionId(stored.world());
        if (worldId == null) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
        if (level == null) {
            return false;
        }
        try {
            Class<?> relClass = Class.forName("net.minecraft.world.entity.RelativeMovement");
            @SuppressWarnings({ "rawtypes", "unchecked" })
            java.util.Set rel = java.util.EnumSet.noneOf((Class) relClass);
            player.teleportTo(level, stored.x(), stored.y(), stored.z(), rel, stored.yaw(), stored.pitch(), true);
        } catch (Exception ex) {
            player.connection.teleport(stored.x(), stored.y(), stored.z(), stored.yaw(), stored.pitch());
        }
        return true;
    }

    private void teleportToSpawn(ServerPlayer player, boolean force) {
        if ((!spawnTeleportEnabled && !force) || spawnWorld == null || spawnWorld.isBlank()) {
            return;
        }
        Identifier worldId = normalizeDimensionId(spawnWorld);
        if (worldId == null) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
        if (level == null) {
            return;
        }
        try {
            Class<?> relClass = Class.forName("net.minecraft.world.entity.RelativeMovement");
            @SuppressWarnings({ "rawtypes", "unchecked" })
            java.util.Set rel = java.util.EnumSet.noneOf((Class) relClass);
            player.teleportTo(level, spawnX, spawnY, spawnZ, rel, spawnYaw, spawnPitch, true);
        } catch (Exception ex) {
            player.connection.teleport(spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
        }
    }

    private void teleportToSpawn(ServerPlayer player) {
        teleportToSpawn(player, false);
    }

    private void storeLastLocation(ServerPlayer player) {
        if (player == null || locationRepository == null) {
            return;
        }
        try {
            PlayerLocation stored = new PlayerLocation(
                    storageServerId,
                    player.getUUID(),
                    serializeDimension(player.level()),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYRot(),
                    player.getXRot(),
                    System.currentTimeMillis()
            );
            locationRepository.saveLocation(stored);
        } catch (Exception ex) {
            LOGGER.error("Failed to save hub player location.", ex);
        }
    }

    private void tickJumpRun(MinecraftServer server) {
        if (!jumpRunEnabled || jumpRunFinishPos == null || server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Long startedAt = jumpRunStarts.get(player.getUUID());
            if (startedAt == null) {
                continue;
            }
            if (!isJumpRunWorld(player.level())) {
                continue;
            }
            BlockPos pos = player.blockPosition();
            if (pos.getX() == jumpRunFinishPos.getX()
                    && pos.getY() == jumpRunFinishPos.getY()
                    && pos.getZ() == jumpRunFinishPos.getZ()) {
                completeJumpRun(player, startedAt);
            }
        }
    }

    private void startJumpRun(ServerPlayer player) {
        if (!jumpRunEnabled) {
            player.sendSystemMessage(tr(player, "lobby.jump.disabled"));
            return;
        }
        if (jumpRunStartPos == null || jumpRunFinishPos == null) {
            generateJumpRunCourse(player.level().getServer(), true);
        }
        if (jumpRunStartPos == null) {
            player.sendSystemMessage(tr(player, "lobby.jump.no-course"));
            return;
        }
        jumpRunStarts.put(player.getUUID(), System.currentTimeMillis());
        if (jumpRunTeleportOnStart) {
            teleportToJumpRunStart(player);
        }
        player.sendSystemMessage(tr(player, "lobby.jump.started"));
    }

    private void stopJumpRun(ServerPlayer player) {
        if (jumpRunStarts.remove(player.getUUID()) != null) {
            player.sendSystemMessage(tr(player, "lobby.jump.stopped"));
        } else {
            player.sendSystemMessage(tr(player, "lobby.jump.no-active"));
        }
    }

    private void sendJumpRunInfo(CommandSourceStack source) {
        String locale = resolveLocale(source);
        String stateRaw = resolveRaw(locale, jumpRunEnabled ? "lobby.state.enabled" : "lobby.state.disabled");
        source.sendSuccess(() -> tr(source, "lobby.jump.info.status", Placeholder.parsed("state", stateRaw)), false);
        source.sendSuccess(() -> tr(source, "lobby.jump.info.course",
                Placeholder.unparsed("course", jumpRunCourseId),
                Placeholder.unparsed("blocks", String.valueOf(jumpRunBlocks))), false);
    }

    private void completeJumpRun(ServerPlayer player, long startedAt) {
        jumpRunStarts.remove(player.getUUID());
        long duration = Math.max(0L, System.currentTimeMillis() - startedAt);
        long best = duration;
        long runCount = 1L;
        if (scoreRepository != null) {
            try {
                var score = scoreRepository.recordRun(jumpRunCourseId, player.getUUID(), duration);
                if (score != null) {
                    best = score.bestTimeMillis();
                    runCount = score.runCount();
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to record jump and run score.", ex);
            }
        }
        player.sendSystemMessage(tr(player, "lobby.jump.finish",
                Placeholder.unparsed("time", formatDuration(duration)),
                Placeholder.unparsed("best", formatDuration(best)),
                Placeholder.unparsed("runs", String.valueOf(runCount))));
    }

    private void generateJumpRunCourse(MinecraftServer server, boolean announce) {
        if (!jumpRunEnabled) {
            if (announce) {
                LOGGER.info("Jump and run is disabled in config.");
            }
            return;
        }
        ServerLevel level = resolveJumpRunLevel(server);
        if (level == null) {
            if (announce) {
                LOGGER.warn("Jump and run world not found.");
            }
            return;
        }
        clearJumpRunCourse(level);
        jumpRunPositions.clear();
        jumpRunStarts.clear();

        Random random = new Random();
        int baseX = (int) Math.round(spawnX);
        int baseY = (int) Math.round(spawnY);
        int baseZ = (int) Math.round(spawnZ);
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
            BlockPos pos = new BlockPos(x, y, z);
            level.setBlock(pos, blockType.defaultBlockState(), 3);
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

    private void clearJumpRunCourse(ServerLevel level) {
        if (jumpRunPositions.isEmpty()) {
            return;
        }
        for (BlockPos pos : jumpRunPositions) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void teleportToJumpRunStart(ServerPlayer player) {
        if (jumpRunStartPos == null) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        ServerLevel level = resolveJumpRunLevel(server);
        if (level == null) {
            return;
        }
        double x = jumpRunStartPos.getX() + 0.5;
        double y = jumpRunStartPos.getY() + 1.0;
        double z = jumpRunStartPos.getZ() + 0.5;
        try {
            Class<?> relClass = Class.forName("net.minecraft.world.entity.RelativeMovement");
            @SuppressWarnings({ "rawtypes", "unchecked" })
            java.util.Set rel = java.util.EnumSet.noneOf((Class) relClass);
            player.teleportTo(level, x, y, z, rel, player.getYRot(), player.getXRot(), true);
        } catch (Exception ex) {
            player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        }
    }

    private ServerLevel resolveJumpRunLevel(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        Identifier worldId = normalizeDimensionId(jumpRunWorld);
        if (worldId == null) {
            return server.overworld();
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
        return level != null ? level : server.overworld();
    }

    private boolean isJumpRunWorld(ServerLevel level) {
        if (level == null) {
            return false;
        }
        Identifier worldId = normalizeDimensionId(jumpRunWorld);
        if (worldId == null) {
            return false;
        }
        return level.dimension().equals(ResourceKey.create(Registries.DIMENSION, worldId));
    }

    private Block resolveJumpRunBlock(String raw, Block fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        Identifier id = Identifier.tryParse(normalized);
        if (id == null) {
            return fallback;
        }
        return BuiltInRegistries.BLOCK.getOptional(id).orElse(fallback);
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

    private void sendServerConnect(ServerPlayer player, String target) {
        if (target == null || target.isBlank()) {
            player.sendSystemMessage(tr(player, "lobby.entry.no-server"));
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("CONNECT");
        out.writeUTF(player.getUUID().toString());
        out.writeUTF(target);
        ServerPlayNetworking.send(player, new HubPayload(out.toByteArray()));
        player.sendSystemMessage(tr(player, "lobby.connecting", Placeholder.unparsed("server", target)));
    }

    private void requestLobbyList(ServerPlayer player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LIST");
        out.writeUTF(player.getUUID().toString());
        ServerPlayNetworking.send(player, new HubPayload(out.toByteArray()));
    }

    private enum JoinTeleportMode {
        NONE,
        SPAWN,
        LAST
    }

}
