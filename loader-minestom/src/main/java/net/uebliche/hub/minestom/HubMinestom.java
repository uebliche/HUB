package net.uebliche.hub.minestom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.Auth;
import net.minestom.server.Auth.Velocity;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.uebliche.hub.common.model.CompassConfig;
import net.uebliche.hub.common.model.ItemSpec;
import net.uebliche.hub.common.model.NavigatorConfig;
import net.uebliche.hub.common.model.NavigatorEntrySpec;
import net.uebliche.hub.common.service.CompassService;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class HubMinestom {
    private static final Logger LOGGER = LoggerFactory.getLogger(HubMinestom.class);
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final CompassService configService = new CompassService();
    private CompassConfig compassConfig = CompassConfig.fallback();
    private NavigatorConfig navigatorConfig = NavigatorConfig.fallback();
    private String velocitySecret = "hubsecret";
    private InstanceContainer instance;
    private Material compassMat;
    private Material navigatorMat;

    public static void main(String[] args) {
        new HubMinestom().start();
    }

    private void start() {
        loadConfig();
        MinecraftServer server = initServer();

        instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
        instance.setGenerator(unit -> unit.modifier().fillHeight(40, 41, Block.GRASS_BLOCK));

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
            }
        });
        global.addListener(PlayerUseItemEvent.class, event -> {
            var item = event.getItemStack();
            if (isCompass(item)) {
                event.getPlayer().sendMessage(MINI.deserialize("<gray>Lobby compass is not wired yet on Minestom."));
            } else if (isNavigator(item)) {
                openNavigator(event.getPlayer());
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
        });

        MinecraftServer.getSchedulerManager().buildShutdownTask(MinecraftServer::stopCleanly);
        server.start("0.0.0.0", 25565);
    }

    private void loadConfig() {
        try {
            Path configDir = Path.of("run/config");
            Path configPath = configDir.resolve("hub.yaml");
            Files.createDirectories(configDir);
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();
            if (Files.notExists(configPath)) {
                ConfigurationNode fresh = loader.createNode();
                fresh.node("minestom", "velocity-secret").set(velocitySecret);
                loader.save(fresh);
            }
            ConfigurationNode node = loader.load();
            compassConfig = configService.loadCompass(node);
            navigatorConfig = configService.loadNavigator(node);
            velocitySecret = node.node("minestom", "velocity-secret").getString(velocitySecret);
        } catch (Exception ex) {
            ex.printStackTrace();
            compassConfig = CompassConfig.fallback();
            navigatorConfig = NavigatorConfig.fallback();
            velocitySecret = "hubsecret";
        }
        compassMat = resolveMaterial(compassConfig.item(), "minecraft:compass");
        navigatorMat = resolveMaterial(navigatorConfig.item(), "minecraft:compass");
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

    private boolean isCompass(ItemStack stack) {
        return stack != null && !stack.isAir() && stack.material() == compassMat;
    }

    private boolean isNavigator(ItemStack stack) {
        return stack != null && !stack.isAir() && stack.material() == navigatorMat;
    }

    private void openNavigator(net.minestom.server.entity.Player player) {
        player.sendMessage(MINI.deserialize("<gray>Navigator entries:"));
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
}
