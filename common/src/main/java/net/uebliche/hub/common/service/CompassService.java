package net.uebliche.hub.common.service;

import net.uebliche.hub.common.ConfigKeys;
import net.uebliche.hub.common.model.CompassConfig;
import net.uebliche.hub.common.model.ItemSpec;
import net.uebliche.hub.common.model.LobbyListEntry;
import net.uebliche.hub.common.model.NavigatorConfig;
import net.uebliche.hub.common.model.NavigatorEntrySpec;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;

public class CompassService {

    public CompassConfig loadCompass(ConfigurationNode root) {
        if (root == null) {
            return CompassConfig.fallback();
        }
        return new CompassConfig(
                root.node("compass", "enabled").getBoolean(true),
                root.node("compass", "gui-title").getString("<aqua>Select Lobby"),
                new ItemSpec(
                        root.node("compass", "item", "name").getString("<gold>Lobby Compass"),
                        readStringList(root.node("compass", "item", "lore"), List.of("<gray>Right-click to choose a lobby")),
                        root.node("compass", "item", "material").getString("MAGMA_CREAM"),
                        root.node("compass", "item", "slot").getInt(0),
                        root.node("compass", "item", "allow-move").getBoolean(false),
                        root.node("compass", "item", "allow-drop").getBoolean(false),
                        root.node("compass", "item", "drop-on-death").getBoolean(true),
                        root.node("compass", "item", "restore-on-respawn").getBoolean(true)
                ),
                root.node("compass", "list-item", "name").getString("<gold><lobby>"),
                readStringList(root.node("compass", "list-item", "lore"),
                        List.of("<gray>Server: <yellow><server>",
                                "<gray>Players: <green><online></dark_gray>/<green><max>",
                                "<dark_gray>Click to join"))
        );
    }

    public NavigatorConfig loadNavigator(ConfigurationNode root) {
        if (root == null) {
            return NavigatorConfig.fallback();
        }
        boolean enabled = root.node("navigator", "enabled").getBoolean(true);
        String title = root.node("navigator", "gui-title").getString("<aqua>Navigator");
        int rows = Math.max(0, root.node("navigator", "gui-rows").getInt(0));
        ItemSpec item = new ItemSpec(
                root.node("navigator", "item", "name").getString("<gold>Navigator"),
                readStringList(root.node("navigator", "item", "lore"), List.of("<gray>Open navigator")),
                root.node("navigator", "item", "material").getString("COMPASS"),
                root.node("navigator", "item", "slot").getInt(4),
                root.node("navigator", "item", "allow-move").getBoolean(false),
                root.node("navigator", "item", "allow-drop").getBoolean(false),
                root.node("navigator", "item", "drop-on-death").getBoolean(true),
                root.node("navigator", "item", "restore-on-respawn").getBoolean(true)
        );
        List<NavigatorEntrySpec> entries = new ArrayList<>();
        ConfigurationNode entriesNode = root.node("navigator", "entries");
        for (var entry : entriesNode.childrenMap().entrySet()) {
            ConfigurationNode node = entry.getValue();
            if (node == null) continue;
            String name = node.node("name").getString("<gold>" + entry.getKey().toString());
            List<String> lore = readStringList(node.node("lore"), List.of());
            String icon = node.node("icon").getString("COMPASS");
            String actionRaw = node.node("action").getString("teleport").toUpperCase();
            NavigatorEntrySpec.NavigatorAction action = switch (actionRaw) {
                case "SERVER" -> NavigatorEntrySpec.NavigatorAction.SERVER;
                case "LOBBY", "LOBBY_SELECTOR" -> NavigatorEntrySpec.NavigatorAction.LOBBY_SELECTOR;
                default -> NavigatorEntrySpec.NavigatorAction.TELEPORT;
            };
            String server = node.node("server").getString("");
            String world = node.node("world").getString("world");
            double x = node.node("x").getDouble(0);
            double y = node.node("y").getDouble(64);
            double z = node.node("z").getDouble(0);
            float yaw = (float) node.node("yaw").getDouble(0);
            float pitch = (float) node.node("pitch").getDouble(0);
            int slot = node.node("slot").getInt(-1);
            entries.add(new NavigatorEntrySpec(name, lore, icon, action, server, world, x, y, z, yaw, pitch, slot));
        }
        if (entries.isEmpty()) {
            return NavigatorConfig.fallback();
        }
        boolean openLobbySelectorRightClick = root.node("navigator", "open-lobby-selector-on-right-click").getBoolean(false);
        return new NavigatorConfig(enabled, title, rows, item, entries, openLobbySelectorRightClick);
    }

    private List<String> readStringList(ConfigurationNode node, List<String> fallback) {
        try {
            List<String> list = node.getList(String.class);
            if (list == null || list.isEmpty()) {
                return fallback;
            }
            return list;
        } catch (SerializationException e) {
            return fallback;
        }
    }
}
