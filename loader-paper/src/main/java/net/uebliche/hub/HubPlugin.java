package net.uebliche.hub;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.uebliche.hub.common.HubEntrypoint;
import net.uebliche.hub.common.update.UpdateChecker;

public class HubPlugin extends JavaPlugin implements Listener, HubEntrypoint {

    private static final String CHANNEL = "uebliche:hub";
    private static final NamespacedKey DATA_KEY = NamespacedKey.minecraft("hub_lobby_slot");
    private static final NamespacedKey COMPASS_KEY = NamespacedKey.minecraft("hub_compass");
    private static final NamespacedKey NAVIGATOR_KEY = NamespacedKey.minecraft("hub_navigator");
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

    private final Map<UUID, List<LobbyEntry>> lobbyCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lobbyCacheAge = new ConcurrentHashMap<>();
    private final Set<UUID> pendingOpen = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Map<Integer, LobbyEntry>> openInventories = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, NavigatorEntry>> openNavigatorInventories = new ConcurrentHashMap<>();
    private Lang langEn;

    @Override
    public void loadConfig() {
        reloadConfig();
        langEn = Lang.load(this, "en_us", MINI);
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        registerItems();
        registerMenus();
        registerGameplayGuards();
        registerTransport();
        Bukkit.getScheduler().runTaskAsynchronously(this,
                () -> UpdateChecker.checkModrinth("HrTclB8n", getDescription().getVersion(),
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
        lobbyCache.clear();
        lobbyCacheAge.clear();
        pendingOpen.clear();
        openInventories.clear();
        openNavigatorInventories.clear();
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
        teleportToSpawn(event.getPlayer(), false);
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
        }
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeHubItems(event.getPlayer());
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
        try {
            String locale = player.locale().toLanguageTag().toLowerCase().replace('-', '_');
            return Lang.load(this, locale, MINI);
        } catch (Exception ignored) {
            return langEn;
        }
    }

    private void resetVitals(org.bukkit.entity.Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
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

    private enum NavigatorAction {
        TELEPORT,
        SERVER,
        LOBBY_SELECTOR
    }

    private record NavigatorEntry(Component name, List<Component> lore, Material icon, NavigatorAction action, String server,
                                  String world, double x, double y, double z, float yaw, float pitch, int slot) {
    }
}
