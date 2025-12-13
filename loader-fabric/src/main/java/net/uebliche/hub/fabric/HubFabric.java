package net.uebliche.hub.fabric;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ComponentSerialization;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.uebliche.hub.common.model.CompassConfig;
import net.uebliche.hub.common.model.ItemSpec;
import net.uebliche.hub.common.model.NavigatorConfig;
import net.uebliche.hub.common.model.NavigatorEntrySpec;
import net.uebliche.hub.common.model.LobbyListEntry;
import net.uebliche.hub.common.HubEntrypoint;
import net.uebliche.hub.common.service.CompassService;
import net.uebliche.hub.common.update.UpdateChecker;
import net.uebliche.hub.fabric.net.HubPayload;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;
import java.util.ArrayList;

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
    private static final Identifier HUB_CHANNEL = Identifier.fromNamespaceAndPath("uebliche", "hub");

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
            if (!disableHunger)
                return;
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
        teleportToSpawn(player);
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
            player.sendSystemMessage(Component.literal("Invalid selection."));
            return;
        }
        NavigatorEntrySpec entry = navigatorConfig.entries().get(index);
        switch (entry.action()) {
            case SERVER -> player.sendSystemMessage(Component.literal("Server connect is not supported on Fabric."));
            case LOBBY_SELECTOR -> openLobbySelectorFromNavigator(player);
            default -> teleportPlayer(player, entry);
        }
    }

    private void teleportPlayer(ServerPlayer player, NavigatorEntrySpec entry) {
        Identifier worldId = normalizeDimensionId(entry.world());
        if (worldId == null) {
            player.sendSystemMessage(Component.literal("Invalid world: " + entry.world()));
            return;
        }
        Level currentLevel = player.level();
        MinecraftServer server = currentLevel.getServer();
        if (server == null) {
            player.sendSystemMessage(Component.literal("Server not available for teleport."));
            return;
        }
        ServerLevel targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("World not found: " + entry.world()));
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
            cacheTtlMillis = Math.max(1000L, node.node("compass", "cache-ttl-millis").getLong(10_000L));
        } catch (Exception ex) {
            LOGGER.error("Failed to load hub config, using defaults.", ex);
            compassConfig = CompassConfig.fallback();
            navigatorConfig = NavigatorConfig.fallback();
            compassItem = Items.COMPASS;
            navigatorItem = Items.COMPASS;
            disableDamage = true;
            disableHunger = true;
            healOnJoin = true;
            spawnTeleportEnabled = false;
            cacheTtlMillis = 10_000L;
        }
    }

    private void writeDefaults(Path path) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
        ConfigurationNode node = loader.createNode();
        CompassConfig cc = CompassConfig.fallback();
        NavigatorConfig nc = NavigatorConfig.fallback();
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

    private Component mm(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        try {
            var adv = mini.deserialize(raw);
            var vanillaJson = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(adv);
            var jsonElement = JsonParser.parseString(vanillaJson);
            var vanilla = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).result().orElse(null);
            return vanilla != null ? vanilla : Component.literal(stripTags(raw));
        } catch (Exception ex) {
            return Component.literal(stripTags(raw));
        }
    }

    private void resetVitals(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20f);
    }

    private void teleportToSpawn(ServerPlayer player) {
        if (!spawnTeleportEnabled || spawnWorld == null || spawnWorld.isBlank()) {
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

    private void sendServerConnect(ServerPlayer player, String target) {
        if (target == null || target.isBlank()) {
            player.sendSystemMessage(Component.literal("No server configured for this entry."));
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("CONNECT");
        out.writeUTF(player.getUUID().toString());
        out.writeUTF(target);
        ServerPlayNetworking.send(player, new HubPayload(out.toByteArray()));
        player.sendSystemMessage(Component.literal("Connecting to " + target + "..."));
    }

    private void requestLobbyList(ServerPlayer player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("LIST");
        out.writeUTF(player.getUUID().toString());
        ServerPlayNetworking.send(player, new HubPayload(out.toByteArray()));
    }

}
