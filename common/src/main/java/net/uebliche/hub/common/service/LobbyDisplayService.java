package net.uebliche.hub.common.service;

import net.uebliche.hub.common.model.LobbyNpcSpec;
import net.uebliche.hub.common.model.LobbySignSpec;
import net.uebliche.hub.common.model.NavigatorEntrySpec;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LobbyDisplayService {

    public record LobbyDisplayConfig<T>(boolean enabled, List<T> entries) {
    }

    public LobbyDisplayConfig<LobbyNpcSpec> loadNpcs(ConfigurationNode root) {
        ConfigurationNode npcRoot = root.node("lobby-npcs");
        boolean enabled = npcRoot.node("enabled").getBoolean(false);
        List<LobbyNpcSpec> entries = new ArrayList<>();
        ConfigurationNode entriesNode = npcRoot.node("entries");
        if (entriesNode != null) {
            if (entriesNode.isMap()) {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : entriesNode.childrenMap().entrySet()) {
                    String id = entry.getKey() != null ? entry.getKey().toString() : "npc-" + entries.size();
                    entries.add(readNpc(id, entry.getValue()));
                }
            } else if (entriesNode.isList()) {
                int index = 0;
                for (ConfigurationNode node : entriesNode.childrenList()) {
                    String id = node.node("id").getString("npc-" + index);
                    entries.add(readNpc(id, node));
                    index++;
                }
            }
        }
        return new LobbyDisplayConfig<>(enabled, entries);
    }

    public LobbyDisplayConfig<LobbySignSpec> loadSigns(ConfigurationNode root) {
        ConfigurationNode signRoot = root.node("lobby-signs");
        boolean enabled = signRoot.node("enabled").getBoolean(false);
        List<LobbySignSpec> entries = new ArrayList<>();
        ConfigurationNode entriesNode = signRoot.node("entries");
        if (entriesNode != null) {
            if (entriesNode.isMap()) {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : entriesNode.childrenMap().entrySet()) {
                    String id = entry.getKey() != null ? entry.getKey().toString() : "sign-" + entries.size();
                    entries.add(readSign(id, entry.getValue()));
                }
            } else if (entriesNode.isList()) {
                int index = 0;
                for (ConfigurationNode node : entriesNode.childrenList()) {
                    String id = node.node("id").getString("sign-" + index);
                    entries.add(readSign(id, node));
                    index++;
                }
            }
        }
        return new LobbyDisplayConfig<>(enabled, entries);
    }

    private LobbyNpcSpec readNpc(String id, ConfigurationNode node) {
        String world = node.node("world").getString("world");
        double x = node.node("x").getDouble(0);
        double y = node.node("y").getDouble(64);
        double z = node.node("z").getDouble(0);
        float yaw = (float) node.node("yaw").getDouble(0);
        float pitch = (float) node.node("pitch").getDouble(0);
        String name = node.node("name").getString("<gold>Lobby");
        List<String> lines;
        try {
            lines = node.node("lines").getList(String.class, List.of());
        } catch (SerializationException ex) {
            lines = List.of();
        }
        String entity = node.node("entity").getString("VILLAGER");
        NavigatorEntrySpec.NavigatorAction action = parseAction(node.node("action").getString("server"));
        String server = node.node("server").getString("");
        String targetWorld = node.node("target", "world").getString(world);
        double targetX = node.node("target", "x").getDouble(x);
        double targetY = node.node("target", "y").getDouble(y);
        double targetZ = node.node("target", "z").getDouble(z);
        float targetYaw = (float) node.node("target", "yaw").getDouble(yaw);
        float targetPitch = (float) node.node("target", "pitch").getDouble(pitch);
        return new LobbyNpcSpec(id, world, x, y, z, yaw, pitch, name, lines, entity, action, server,
                targetWorld, targetX, targetY, targetZ, targetYaw, targetPitch);
    }

    private LobbySignSpec readSign(String id, ConfigurationNode node) {
        String world = node.node("world").getString("world");
        int x = node.node("x").getInt(0);
        int y = node.node("y").getInt(64);
        int z = node.node("z").getInt(0);
        List<String> lines;
        try {
            lines = node.node("lines").getList(String.class, List.of());
        } catch (SerializationException ex) {
            lines = List.of();
        }
        NavigatorEntrySpec.NavigatorAction action = parseAction(node.node("action").getString("server"));
        String server = node.node("server").getString("");
        String targetWorld = node.node("target", "world").getString(world);
        double targetX = node.node("target", "x").getDouble(x);
        double targetY = node.node("target", "y").getDouble(y);
        double targetZ = node.node("target", "z").getDouble(z);
        float targetYaw = (float) node.node("target", "yaw").getDouble(0);
        float targetPitch = (float) node.node("target", "pitch").getDouble(0);
        return new LobbySignSpec(id, world, x, y, z, lines, action, server, targetWorld,
                targetX, targetY, targetZ, targetYaw, targetPitch);
    }

    private NavigatorEntrySpec.NavigatorAction parseAction(String raw) {
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
}
