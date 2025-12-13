package net.uebliche.hub.fabric;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Consumer;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.uebliche.hub.common.model.CompassConfig;
import net.uebliche.hub.common.model.LobbyListEntry;
import net.uebliche.hub.common.model.NavigatorConfig;
import net.uebliche.hub.common.model.NavigatorEntrySpec;

public class MenuService {
    private final CompassConfig compassConfig;
    private final NavigatorConfig navigatorConfig;
    private final MiniMessage mini;
    private final Function<String, Item> itemResolver;
    private final LobbyCacheService lobbyCache;
    private final BiConsumer<ServerPlayer, NavigatorEntrySpec> teleportAction;
    private final BiConsumer<ServerPlayer, String> serverConnectAction;
    private final Consumer<ServerPlayer> lobbySelectorAction;

    public MenuService(CompassConfig compassConfig, NavigatorConfig navigatorConfig, MiniMessage mini,
            Function<String, Item> itemResolver, LobbyCacheService lobbyCache,
            BiConsumer<ServerPlayer, NavigatorEntrySpec> teleportAction,
            BiConsumer<ServerPlayer, String> serverConnectAction,
            Consumer<ServerPlayer> lobbySelectorAction) {
        this.compassConfig = compassConfig;
        this.navigatorConfig = navigatorConfig;
        this.mini = mini;
        this.itemResolver = itemResolver;
        this.lobbyCache = lobbyCache;
        this.teleportAction = teleportAction;
        this.serverConnectAction = serverConnectAction;
        this.lobbySelectorAction = lobbySelectorAction;
    }

    public void openMenu(ServerPlayer player, SelectionMode mode) {
        String title = mode == SelectionMode.COMPASS ? compassConfig.guiTitle() : navigatorConfig.guiTitle();
        var entries = mode == SelectionMode.COMPASS ? lobbyEntriesFor(player) : navigatorConfig.entries();
        int rowsConfigured = mode == SelectionMode.NAVIGATOR ? navigatorConfig.guiRows() : 0;
        int rowsAuto = (int) Math.ceil(entries.size() / 9.0);
        int rows = rowsConfigured > 0 ? rowsConfigured : rowsAuto;
        rows = Math.max(1, Math.min(6, rows));
        int size = rows * 9;
        SimpleContainer container = new SimpleContainer(size);
        ItemStack filler = new ItemStack(itemResolver.apply("gray_stained_glass_pane"));
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
            ItemStack icon = new ItemStack(itemResolver.apply(entry.icon() == null ? "compass" : entry.icon()));
            String displayName = entry.name();
            icon.set(DataComponents.CUSTOM_NAME, mm(displayName));
            if (mode == SelectionMode.COMPASS && !compassConfig.listItemLoreTemplate().isEmpty()) {
                var lore = entry.lore().stream().map(this::mm).toList();
                icon.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
            }
            container.setItem(slot, icon);
        }
        final MenuType<?> menuType = menuTypeForRows(rows);
        final int menuRows = rows;
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return mm(title);
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, Inventory inv,
                    net.minecraft.world.entity.player.Player p) {
                return new SelectionMenu(menuType, syncId, inv, container, menuRows, mode);
            }
        });
    }

    private List<NavigatorEntrySpec> lobbyEntriesFor(ServerPlayer player) {
        var entries = lobbyCache.entriesFor(player.getUUID());
        return entries.stream()
                .map(e -> new NavigatorEntrySpec(
                        applyLobbyPlaceholders(compassConfig.listItemNameTemplate(), e),
                        buildLobbyLore(e),
                        "LIGHT_BLUE_DYE",
                        NavigatorEntrySpec.NavigatorAction.SERVER,
                        e.server(),
                        e.server(),
                        0, 64, 0, 0, 0, -1))
                .toList();
    }

    private String applyLobbyPlaceholders(String template, LobbyListEntry e) {
        if (template == null)
            return "";
        return template
                .replace("<lobby>", e.lobby() == null ? "" : e.lobby())
                .replace("<server>", e.server() == null ? "" : e.server())
                .replace("<online>", String.valueOf(e.online()))
                .replace("<max>", String.valueOf(e.max()));
    }

    private List<String> buildLobbyLore(LobbyListEntry e) {
        if (compassConfig.listItemLoreTemplate().isEmpty()) {
            return List.of(
                    applyLobbyPlaceholders("<gray>Server: <yellow><server>", e),
                    applyLobbyPlaceholders("<gray>Players: <green><online></dark_gray>/<green><max>", e),
                    "<dark_gray>Click to join");
        }
        return compassConfig.listItemLoreTemplate().stream()
                .map(t -> applyLobbyPlaceholders(t, e))
                .toList();
    }

    private MenuType<?> menuTypeForRows(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private Component mm(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        try {
            var adv = mini.deserialize(raw);
            var vanillaJson = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(adv);
            var jsonElement = com.google.gson.JsonParser.parseString(vanillaJson);
            var vanilla = net.minecraft.network.chat.ComponentSerialization.CODEC
                    .parse(com.mojang.serialization.JsonOps.INSTANCE, jsonElement).result().orElse(null);
            return vanilla != null ? vanilla : Component.literal(stripTags(raw));
        } catch (Exception ex) {
            return Component.literal(stripTags(raw));
        }
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

    private String stripTags(String input) {
        if (input == null)
            return "";
        return input.replaceAll("<[^>]+>", "");
    }

    private class SelectionMenu extends ChestMenu {
        private final SelectionMode mode;

        public SelectionMenu(MenuType<?> type, int syncId, Inventory playerInv, SimpleContainer container, int rows,
                SelectionMode mode) {
            super(type, syncId, playerInv, container, Math.max(1, rows));
            this.mode = mode;
        }

        @Override
        public void clicked(int slot, int button, ClickType clickType,
                net.minecraft.world.entity.player.Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                var entries = mode == SelectionMode.COMPASS ? lobbyEntriesFor(serverPlayer)
                        : navigatorConfig.entries();
                if (slot >= 0 && slot < entries.size()) {
                    NavigatorEntrySpec entry = entries.get(slot);
                    switch (entry.action()) {
                        case SERVER -> serverConnectAction.accept(serverPlayer, entry.server());
                        case LOBBY_SELECTOR -> {
                            lobbySelectorAction.accept(serverPlayer);
                            serverPlayer.closeContainer();
                            return;
                        }
                        default -> teleportAction.accept(serverPlayer, entry);
                    }
                    serverPlayer.closeContainer();
                    return;
                }
            }
            super.clicked(slot, button, clickType, player);
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }
    }
}
