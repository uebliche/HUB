package net.uebliche.ffa;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.Auth.Velocity;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Standalone Minestom FFA test server with two kits and a flat, persistent world.
 */
public final class FfaServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(FfaServer.class);
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String DEFAULT_VELOCITY_SECRET = "secret";
    private final Map<String, Kit> kits = new HashMap<>();
    private final Map<UUID, String> selectedKits = new ConcurrentHashMap<>();
    private final Pos spawnPos = new Pos(0, 65, 0);
    private InstanceContainer instance;

    public static void main(String[] args) {
        new FfaServer().start();
    }

    private void start() {
        MinecraftServer server = initServer();
        registerKits();

        Path worldPath = Path.of("run/worlds/ffa");
        instance = MinecraftServer.getInstanceManager().createInstanceContainer(new AnvilLoader(worldPath));
        instance.setGenerator(unit -> {
            var modifier = unit.modifier();
            modifier.fillHeight(0, 1, Block.BEDROCK);
            modifier.fillHeight(1, 60, Block.DIRT);
            modifier.fillHeight(60, 61, Block.GRASS_BLOCK);
        });

        var global = MinecraftServer.getGlobalEventHandler();
        global.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(spawnPos);
            event.getPlayer().setGameMode(GameMode.SURVIVAL);
            LOGGER.info("Player joining FFA: {} ({})", event.getPlayer().getUsername(), event.getPlayer().getUuid());
        });
        global.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                var player = event.getPlayer();
                String defaultKit = kits.keySet().stream().findFirst().orElse("warrior");
                selectedKits.put(player.getUuid(), defaultKit);
                applyKit(player, defaultKit);
                player.teleport(spawnPos);
                player.sendMessage(MINI.deserialize("<green>FFA bereit. Nutze <yellow>/kit <kitname></yellow> für Kits.</green>"));
            }
        });
        global.addListener(PlayerRespawnEvent.class, event -> {
            var player = event.getPlayer();
            applyKit(player, selectedKits.getOrDefault(player.getUuid(), kits.keySet().stream().findFirst().orElse("warrior")));
            player.teleport(spawnPos);
        });
        global.addListener(PlayerBlockBreakEvent.class, event -> {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
                event.getPlayer().sendActionBar(MINI.deserialize("<red>Bauen nur im Creative erlaubt.</red>"));
            }
        });
        global.addListener(PlayerBlockPlaceEvent.class, event -> {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
                event.getPlayer().sendActionBar(MINI.deserialize("<red>Bauen nur im Creative erlaubt.</red>"));
            }
        });

        registerCommands();
        MinecraftServer.getSchedulerManager().buildShutdownTask(MinecraftServer::stopCleanly);
        server.start("0.0.0.0", 25565);
    }

    private void registerKits() {
        kits.put("warrior", new Kit("warrior", "Warrior (Schwert)", player -> {
            resetPlayer(player);
            var inv = player.getInventory();
            inv.setItemStack(0, ItemStack.of(Material.DIAMOND_SWORD));
            inv.setItemStack(1, ItemStack.of(Material.SHIELD));
            inv.setItemStack(2, ItemStack.of(Material.GOLDEN_APPLE, 4));
            inv.setItemStack(3, ItemStack.of(Material.COOKED_BEEF, 32));
            player.setEquipment(EquipmentSlot.HELMET, ItemStack.of(Material.IRON_HELMET));
            player.setEquipment(EquipmentSlot.CHESTPLATE, ItemStack.of(Material.IRON_CHESTPLATE));
            player.setEquipment(EquipmentSlot.LEGGINGS, ItemStack.of(Material.IRON_LEGGINGS));
            player.setEquipment(EquipmentSlot.BOOTS, ItemStack.of(Material.IRON_BOOTS));
        }));

        kits.put("archer", new Kit("archer", "Archer (Bogen)", player -> {
            resetPlayer(player);
            var inv = player.getInventory();
            inv.setItemStack(0, ItemStack.of(Material.STONE_SWORD));
            inv.setItemStack(1, ItemStack.of(Material.BOW));
            inv.setItemStack(2, ItemStack.of(Material.ARROW, 64));
            inv.setItemStack(3, ItemStack.of(Material.COOKED_BEEF, 32));
            inv.setItemStack(4, ItemStack.of(Material.SPLASH_POTION));
            player.setEquipment(EquipmentSlot.HELMET, ItemStack.of(Material.CHAINMAIL_HELMET));
            player.setEquipment(EquipmentSlot.CHESTPLATE, ItemStack.of(Material.CHAINMAIL_CHESTPLATE));
            player.setEquipment(EquipmentSlot.LEGGINGS, ItemStack.of(Material.CHAINMAIL_LEGGINGS));
            player.setEquipment(EquipmentSlot.BOOTS, ItemStack.of(Material.CHAINMAIL_BOOTS));
        }));
    }

    private void registerCommands() {
        var kitArg = ArgumentType.Word("kit");
        var kit = new Command("kit");
        kit.addSyntax((sender, ctx) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MINI.deserialize("<red>Nur Spieler.</red>"));
                return;
            }
            String kitId = ctx.get(kitArg).toLowerCase(Locale.ROOT);
            if (!kits.containsKey(kitId)) {
                player.sendMessage(MINI.deserialize("<red>Unbekanntes Kit. Verfügbar: "
                        + String.join(", ", kits.keySet()) + "</red>"));
                return;
            }
            selectedKits.put(player.getUuid(), kitId);
            applyKit(player, kitId);
            player.sendMessage(MINI.deserialize("<green>Kit gesetzt: <yellow>" + kits.get(kitId).display + "</yellow></green>"));
        }, kitArg);

        kit.setDefaultExecutor((sender, ctx) -> sender.sendMessage(MINI.deserialize(
                "<gray>Kits: <yellow>" + String.join(", ", kits.keySet()) + "</yellow> | Nutze /kit <name></gray>")));

        MinecraftServer.getCommandManager().register(kit);
    }

    private void applyKit(Player player, String kitId) {
        Kit kit = kits.get(kitId);
        if (kit == null) {
            return;
        }
        kit.apply.accept(player);
    }

    private void resetPlayer(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setFood(20);
        player.setHealth(20f);
        player.clearEffects();
    }

    private MinecraftServer initServer() {
        String secret = System.getenv().getOrDefault("VELOCITY_SECRET", DEFAULT_VELOCITY_SECRET);
        if (secret == null || secret.isBlank()) {
            return MinecraftServer.init();
        }
        LOGGER.info("Enabling Velocity forwarding for FFA with provided secret.");
        return MinecraftServer.init(new Velocity(secret));
    }

    private record Kit(String id, String display, Consumer<Player> apply) {
    }
}
