package net.uebliche.hub.neoforge;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.commands.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.uebliche.hub.common.HubEntrypoint;
import net.uebliche.hub.common.model.CompassConfig;
import net.uebliche.hub.common.model.ItemSpec;
import net.uebliche.hub.common.model.NavigatorConfig;
import net.uebliche.hub.common.model.NavigatorEntrySpec;
import net.uebliche.hub.common.service.CompassService;
import com.google.gson.JsonParser;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Mod("hub")
public class HubNeoForge implements HubEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("hub");
    private final CompassService configService = new CompassService();
    private CompassConfig compassConfig = CompassConfig.fallback();
    private NavigatorConfig navigatorConfig = NavigatorConfig.fallback();
    private Item compassItem = Items.COMPASS;
    private Item navigatorItem = Items.COMPASS;
    private boolean disableDamage = true;
    private boolean disableHunger = true;
    private boolean healOnJoin = true;
    private boolean spawnCommandEnabled = true;
    private boolean spawnTeleportEnabled = false;
    private String spawnWorld = "world";
    private double spawnX = 0;
    private double spawnY = 64;
    private double spawnZ = 0;
    private float spawnYaw = 0;
    private float spawnPitch = 0;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public HubNeoForge() {
        loadConfig();
    }

    @Override
    public void loadConfig() {
        Path configDir = FMLPaths.CONFIGDIR.get();
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
        }
    }

    @Override
    public void registerItems() {
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    @Override
    public void registerMenus() {
        NeoForge.EVENT_BUS.addListener(this::onRightClick);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    @Override
    public void registerGameplayGuards() {
        NeoForge.EVENT_BUS.addListener(this::onLivingAttack);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
    }

    @Override
    public void registerTransport() {
        // Teleport is wired via navigator selection; server-connect not implemented.
    }

    private void onServerStarted(ServerStartedEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            giveHubItems(player);
        }
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            giveHubItems(player);
            if (healOnJoin) resetVitals(player);
            teleportToSpawn(player);
        }
    }

    private void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (compassConfig.item().restoreOnRespawn()) {
                placeInSlot(player, compassConfig.item().slot(), buildItem(compassConfig.item(), compassItem));
            }
            if (navigatorConfig.enabled() && navigatorConfig.item().restoreOnRespawn()) {
                placeInSlot(player, navigatorConfig.item().slot(), buildItem(navigatorConfig.item(), navigatorItem));
            }
            if (healOnJoin) resetVitals(player);
            teleportToSpawn(player);
        }
    }

    private void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        if (isConfigItem(stack, compassItem, compassConfig.item().name())) {
            openMenu(player, SelectionMode.COMPASS);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (navigatorConfig.enabled() && isConfigItem(stack, navigatorItem, navigatorConfig.item().name())) {
            openMenu(player, SelectionMode.NAVIGATOR);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hubnav")
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int idx = IntegerArgumentType.getInteger(ctx, "index") - 1;
                            performNavigatorAction(player, idx);
                            return 1;
                        })));
        if (spawnCommandEnabled) {
            event.getDispatcher().register(Commands.literal("spawn")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        teleportToSpawn(player);
                        return 1;
                    }));
        }
    }

    private void onLivingAttack(LivingIncomingDamageEvent event) {
        if (disableDamage && event.getEntity() instanceof ServerPlayer) {
            event.setCanceled(true);
        }
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (disableHunger) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20f);
        }
    }

    private void giveHubItems(ServerPlayer player) {
        placeInSlot(player, compassConfig.item().slot(), buildItem(compassConfig.item(), compassItem));
        if (navigatorConfig.enabled()) {
            placeInSlot(player, navigatorConfig.item().slot(), buildItem(navigatorConfig.item(), navigatorItem));
        }
    }

    private void placeInSlot(ServerPlayer player, int slot, ItemStack stack) {
        Inventory inv = player.getInventory();
        if (slot >= 0 && slot < inv.getContainerSize()) {
            inv.setItem(slot, stack);
        } else {
            inv.add(stack);
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
        if (stack == null) return false;
        if (!stack.is(target)) return false;
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
            case SERVER -> player.sendSystemMessage(Component.literal("Server connect is not supported on NeoForge."));
            case LOBBY_SELECTOR -> player.sendSystemMessage(Component.literal("Lobby selector is not available on NeoForge."));
            default -> teleportPlayer(player, entry);
        }
    }

    private void teleportPlayer(ServerPlayer player, NavigatorEntrySpec entry) {
        ResourceLocation worldId = normalizeDimensionId(entry.world());
        if (worldId == null) {
            player.sendSystemMessage(Component.literal("Invalid world: " + entry.world()));
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            player.sendSystemMessage(Component.literal("Server not available for teleport."));
            return;
        }
        ServerLevel targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, worldId));
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("World not found: " + entry.world()));
            return;
        }
        player.teleportTo(targetLevel, entry.x(), entry.y(), entry.z(),
                Collections.<Relative>emptySet(), entry.yaw(), entry.pitch(), false);
    }

    private void openMenu(ServerPlayer player, SelectionMode mode) {
        String title = mode == SelectionMode.COMPASS ? compassConfig.guiTitle() : navigatorConfig.guiTitle();
        var entries = navigatorConfig.entries();
        int rowsCfg = navigatorConfig.guiRows();
        int rowsAuto = (int) Math.ceil(entries.size() / 9.0);
        int rows = rowsCfg > 0 ? rowsCfg : rowsAuto;
        rows = Math.max(1, Math.min(6, rows));
        int size = rows * 9;
        SimpleContainer container = new SimpleContainer(size);
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponents.CUSTOM_NAME, Component.empty());
        for (int i = 0; i < size; i++) {
            container.setItem(i, filler.copy());
        }
        var slotOrder = buildSlotOrder(rows, entries.size());
        int orderIdx = 0;
        for (NavigatorEntrySpec entry : entries) {
            int slot = entry.slot();
            if (slot < 0 || slot >= size) {
                if (orderIdx >= slotOrder.size()) break;
                slot = slotOrder.get(orderIdx++);
            }
            ItemStack icon = new ItemStack(resolveItem(entry.icon(), Items.COMPASS));
            String displayName = mode == SelectionMode.COMPASS
                    ? formatTemplate(compassConfig.listItemNameTemplate(), entry)
                    : entry.name();
            icon.set(DataComponents.CUSTOM_NAME, mm(displayName));
            if (mode == SelectionMode.COMPASS && !compassConfig.listItemLoreTemplate().isEmpty()) {
                var lore = compassConfig.listItemLoreTemplate().stream()
                        .map(template -> formatTemplate(template, entry))
                        .map(line -> mm(line))
                        .toList();
                icon.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
            }
            container.setItem(slot, icon);
        }
        final int finalRows = rows;
        final SelectionMode finalMode = mode;
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return mm(title);
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, Inventory inv, net.minecraft.world.entity.player.Player p) {
                return new SelectionMenu(syncId, inv, container, finalRows, finalMode);
            }
        });
    }

    private class SelectionMenu extends ChestMenu {
        private final SelectionMode mode;

        public SelectionMenu(int syncId, Inventory playerInv, SimpleContainer container, int rows, SelectionMode mode) {
            super(MenuType.GENERIC_9x3, syncId, playerInv, container, Math.max(1, rows));
            this.mode = mode;
        }

        @Override
        public void clicked(int slot, int button, ClickType clickType, net.minecraft.world.entity.player.Player player) {
            if (player instanceof ServerPlayer serverPlayer && slot >= 0 && slot < navigatorConfig.entries().size()) {
                handleSelection(serverPlayer, mode, slot);
                serverPlayer.closeContainer();
                return;
            }
            super.clicked(slot, button, clickType, player);
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }
    }

    private void handleSelection(ServerPlayer player, SelectionMode mode, int slot) {
        if (slot < 0 || slot >= navigatorConfig.entries().size()) return;
        NavigatorEntrySpec entry = navigatorConfig.entries().get(slot);
        if (mode == SelectionMode.NAVIGATOR || mode == SelectionMode.COMPASS) {
            if (entry.action() == NavigatorEntrySpec.NavigatorAction.SERVER) {
                player.sendSystemMessage(Component.literal("Server connect is not supported on NeoForge."));
            } else {
                teleportPlayer(player, entry);
            }
        }
    }

    private String stripTags(String input) {
        if (input == null) return "";
        return input.replaceAll("<[^>]+>", "");
    }

    private Component mm(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        try {
            var adv = mini.deserialize(raw);
            var vanillaJson = GsonComponentSerializer.gson().serialize(adv);
            var jsonElement = JsonParser.parseString(vanillaJson);
            var vanilla = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).result().orElse(null);
            return vanilla != null ? vanilla : Component.literal(stripTags(raw));
        } catch (Exception ex) {
            return Component.literal(stripTags(raw));
        }
    }

    private Item resolveItem(String id, Item fallback) {
        if (id == null || id.isBlank()) return fallback;
        String normalized = id.toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        ResourceLocation identifier = ResourceLocation.tryParse(normalized);
        if (identifier == null) {
            return fallback;
        }
        return BuiltInRegistries.ITEM.getOptional(identifier).orElse(fallback);
    }

    private ResourceLocation normalizeDimensionId(String world) {
        if (world == null || world.isBlank()) return null;
        String id = world.contains(":") ? world : "minecraft:" + world;
        return ResourceLocation.tryParse(id);
    }

    private List<Integer> buildSlotOrder(int rows, int count) {
        List<Integer> order = new ArrayList<>();
        int startRow = rows > 2 ? 1 : 0;
        int endRow = rows > 2 ? rows - 2 : rows - 1;
        for (int r = startRow; r <= endRow && order.size() < count; r++) {
            int cStart = rows > 2 ? 1 : 0;
            int cEnd = rows > 2 ? 7 : 8;
            for (int c = cStart; c <= cEnd && order.size() < count; c++) {
                order.add(r * 9 + c);
            }
        }
        return order;
    }

    private String formatTemplate(String template, NavigatorEntrySpec entry) {
        if (template == null) return "";
        return template
                .replace("<lobby>", entry.world() == null ? "" : entry.world())
                .replace("<server>", entry.server() == null ? "" : entry.server())
                .replace("<online>", "0")
                .replace("<max>", "0");
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
        node.node("spawn-teleport", "enabled").set(false);
        node.node("spawn-teleport", "world").set("world");
        node.node("spawn-teleport", "x").set(0);
        node.node("spawn-teleport", "y").set(64);
        node.node("spawn-teleport", "z").set(0);
        node.node("spawn-teleport", "yaw").set(0);
        node.node("spawn-teleport", "pitch").set(0);
        loader.save(node);
    }

    private enum SelectionMode {
        COMPASS,
        NAVIGATOR
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
        var worldId = normalizeDimensionId(spawnWorld);
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
        player.teleportTo(level, spawnX, spawnY, spawnZ, Collections.<Relative>emptySet(), spawnYaw, spawnPitch, false);
    }
}
