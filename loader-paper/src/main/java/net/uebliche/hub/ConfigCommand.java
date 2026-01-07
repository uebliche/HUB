package net.uebliche.hub;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

class ConfigCommand extends Command implements TabCompleter {

    private final HubPlugin plugin;

    private static final List<String> COMMON_PATHS = List.of(
            "compass.enabled",
            "compass.gui-title",
            "compass.item.name",
            "compass.item.lore",
            "compass.item.material",
            "compass.item.slot",
            "compass.item.allow-move",
            "compass.item.allow-drop",
            "compass.item.drop-on-death",
            "compass.item.restore-on-respawn",
            "compass.list-item.name",
            "compass.list-item.lore",
            "navigator.enabled",
            "navigator.gui-title",
            "navigator.item.name",
            "navigator.item.lore",
            "navigator.item.material",
            "navigator.item.slot",
            "navigator.item.allow-move",
            "navigator.item.allow-drop",
            "navigator.item.drop-on-death",
            "navigator.item.restore-on-respawn"
    );

    ConfigCommand(HubPlugin plugin) {
        super("hubconfig");
        this.plugin = plugin;
        this.setPermission("hub.config");
        this.setDescription("Configure HUB plugin in-game");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("hub.config")) {
            sender.sendMessage(plugin.translate(sender, "lobby.command.no-permission"));
            return true;
        }
        if (args.length == 0) {
            sendOverview(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.loadConfig();
                sender.sendMessage(plugin.translate(sender, "lobby.command.config.reloaded"));
                return true;
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.translate(sender, "lobby.command.config.set.usage",
                            Placeholder.unparsed("label", label)));
                    return true;
                }
                String path = args[1];
                String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (applyValue(path, value)) {
                    plugin.saveConfig();
                    plugin.loadConfig();
                    sender.sendMessage(plugin.translate(sender, "lobby.command.config.set.updated",
                            Placeholder.unparsed("path", path),
                            Placeholder.unparsed("value", value)));
                } else {
                    sender.sendMessage(plugin.translate(sender, "lobby.command.config.set.failed",
                            Placeholder.unparsed("path", path),
                            Placeholder.unparsed("value", value)));
                }
                return true;
            }
            case "npc" -> {
                handleNpcCommand(sender, label, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
            case "jump", "jr", "jumprun" -> {
                handleJumpRunCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
            case "show" -> {
                if (args.length < 2) {
                    sendOverview(sender);
                    return true;
                }
                String path = args[1];
                Object current = plugin.getConfig().get(path);
                sender.sendMessage(plugin.translate(sender, "lobby.command.config.get.value",
                        Placeholder.unparsed("path", path),
                        Placeholder.unparsed("value", String.valueOf(current))));
                return true;
            }
            default -> {
                sendOverview(sender);
                return true;
            }
        }
    }

    private boolean applyValue(String path, String raw) {
        if (path.toLowerCase(Locale.ROOT).endsWith("material")) {
            Material material = Material.matchMaterial(raw);
            if (material == null) {
                return false;
            }
            plugin.getConfig().set(path, material.name());
            return true;
        }
        if (path.toLowerCase(Locale.ROOT).contains("lore")) {
            List<String> lore = splitLines(raw);
            plugin.getConfig().set(path, lore);
            return true;
        }
        Object existing = plugin.getConfig().get(path);
        if (existing instanceof Boolean || isBoolean(raw)) {
            plugin.getConfig().set(path, parseBoolean(raw));
            return true;
        }
        if (existing instanceof Integer) {
            try {
                plugin.getConfig().set(path, Integer.parseInt(raw));
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (existing instanceof Number) {
            try {
                plugin.getConfig().set(path, Double.parseDouble(raw));
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (raw.matches("^-?\\d+$")) {
            plugin.getConfig().set(path, Integer.parseInt(raw));
            return true;
        }
        if (raw.matches("^-?\\d+\\.\\d+$")) {
            plugin.getConfig().set(path, Double.parseDouble(raw));
            return true;
        }
        plugin.getConfig().set(path, raw);
        return true;
    }

    private List<String> splitLines(String raw) {
        if (raw.contains("|")) {
            return Arrays.stream(raw.split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return List.of(raw);
    }

    private boolean isBoolean(String raw) {
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return v.matches("^(true|false|1|0|yes|no|on|off)$");
    }

    private boolean parseBoolean(String raw) {
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return v.matches("^(true|1|yes|y|on)$");
    }

    private void sendOverview(CommandSender sender) {
        sender.sendMessage(plugin.translate(sender, "lobby.command.config.info.header"));
        List<String> paths = new ArrayList<>(COMMON_PATHS);
        paths.addAll(navigatorEntryPaths());
        for (String path : paths) {
            Object value = plugin.getConfig().get(path);
            Component line = Component.text("- ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(path, NamedTextColor.AQUA))
                    .append(Component.text(" = ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(value), NamedTextColor.WHITE))
                    .append(Component.space())
                    .append(Component.text("[edit]", NamedTextColor.YELLOW)
                            .clickEvent(ClickEvent.suggestCommand("/hubconfig set " + path + " ")));
            sender.sendMessage(line);
        }
        sender.sendMessage(plugin.translate(sender, "lobby.command.config.info.hint"));
        sender.sendMessage(plugin.translate(sender, "lobby.command.config.info.jump"));
    }

    private void handleJumpRunCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.translate(sender, "lobby.command.jump.usage"));
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "generate", "regen" -> {
                plugin.generateJumpRunCourse(true);
                sender.sendMessage(plugin.translate(sender, "lobby.command.jump.generated"));
            }
            case "start" -> {
                if (sender instanceof Player player) {
                    plugin.startJumpRun(player);
                } else {
                    sender.sendMessage(plugin.translate(sender, "lobby.command.jump.only-player-start"));
                }
            }
            case "stop", "cancel" -> {
                if (sender instanceof Player player) {
                    plugin.stopJumpRun(player);
                } else {
                    sender.sendMessage(plugin.translate(sender, "lobby.command.jump.only-player-stop"));
                }
            }
            case "info" -> plugin.sendJumpRunInfo(sender);
            default -> sender.sendMessage(plugin.translate(sender, "lobby.command.jump.usage"));
        }
    }

    private void handleNpcCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendNpcUsage(sender, label);
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add" -> handleNpcAdd(sender, label, args);
            case "remove" -> handleNpcRemove(sender, label, args);
            case "list" -> handleNpcList(sender);
            case "enable" -> handleNpcEnable(sender, label, args);
            default -> sendNpcUsage(sender, label);
        }
    }

    private void sendNpcUsage(CommandSender sender, String label) {
        sender.sendMessage(plugin.translate(sender, "lobby.command.npc.header"));
        sender.sendMessage(plugin.translate(sender, "lobby.command.npc.usage.add",
                Placeholder.unparsed("label", label)));
        sender.sendMessage(plugin.translate(sender, "lobby.command.npc.usage.remove",
                Placeholder.unparsed("label", label)));
        sender.sendMessage(plugin.translate(sender, "lobby.command.npc.usage.list",
                Placeholder.unparsed("label", label)));
        sender.sendMessage(plugin.translate(sender, "lobby.command.npc.usage.enable",
                Placeholder.unparsed("label", label)));
    }

    private void handleNpcAdd(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.translate(sender, "lobby.npc.only-player-add"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.translate(sender, "lobby.command.npc.usage.add",
                    Placeholder.unparsed("label", label)));
            return;
        }
        String id = args[1];
        String action = args[2].toLowerCase(Locale.ROOT);
        String server = args.length >= 4 ? args[3] : "";
        if (!isNpcAction(action)) {
            sender.sendMessage(plugin.translate(sender, "lobby.npc.action.invalid"));
            return;
        }
        if ("server".equals(action) && server.isBlank()) {
            sender.sendMessage(plugin.translate(sender, "lobby.npc.server.required"));
            return;
        }
        ConfigurationSection entries = plugin.getConfig().getConfigurationSection("lobby-npcs.entries");
        if (entries == null) {
            entries = plugin.getConfig().createSection("lobby-npcs.entries");
        }
        ConfigurationSection entry = entries.getConfigurationSection(id);
        if (entry == null) {
            entry = entries.createSection(id);
        }
        var loc = player.getLocation();
        entry.set("world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        entry.set("x", loc.getX());
        entry.set("y", loc.getY());
        entry.set("z", loc.getZ());
        entry.set("yaw", loc.getYaw());
        entry.set("pitch", loc.getPitch());
        entry.set("name", "<gold>" + id);
        entry.set("entity", entry.getString("entity", "VILLAGER"));
        entry.set("action", action);
        if (!server.isBlank()) {
            entry.set("server", server);
        }
        plugin.getConfig().set("lobby-npcs.enabled", true);
        plugin.saveConfig();
        plugin.loadConfig();
        sender.sendMessage(plugin.translate(sender, "lobby.npc.saved",
                Placeholder.unparsed("id", id)));
    }

    private void handleNpcRemove(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.translate(sender, "lobby.command.npc.usage.remove",
                    Placeholder.unparsed("label", label)));
            return;
        }
        String id = args[1];
        ConfigurationSection entries = plugin.getConfig().getConfigurationSection("lobby-npcs.entries");
        if (entries == null || !entries.isConfigurationSection(id)) {
            sender.sendMessage(plugin.translate(sender, "lobby.npc.not-found",
                    Placeholder.unparsed("id", id)));
            return;
        }
        entries.set(id, null);
        plugin.saveConfig();
        plugin.loadConfig();
        sender.sendMessage(plugin.translate(sender, "lobby.npc.removed",
                Placeholder.unparsed("id", id)));
    }

    private void handleNpcList(CommandSender sender) {
        ConfigurationSection entries = plugin.getConfig().getConfigurationSection("lobby-npcs.entries");
        if (entries == null || entries.getKeys(false).isEmpty()) {
            sender.sendMessage(plugin.translate(sender, "lobby.npc.none"));
            return;
        }
        sender.sendMessage(plugin.translate(sender, "lobby.npc.list.header"));
        for (String key : entries.getKeys(false)) {
            sender.sendMessage(plugin.translate(sender, "lobby.npc.list.entry",
                    Placeholder.unparsed("id", key)));
        }
    }

    private void handleNpcEnable(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.translate(sender, "lobby.command.npc.usage.enable",
                    Placeholder.unparsed("label", label)));
            return;
        }
        boolean enabled = parseBoolean(args[1]);
        plugin.getConfig().set("lobby-npcs.enabled", enabled);
        plugin.saveConfig();
        plugin.loadConfig();
        Component state = enabled
                ? plugin.translate(sender, "lobby.state.enabled")
                : plugin.translate(sender, "lobby.state.disabled");
        sender.sendMessage(plugin.translate(sender, "lobby.npc.enabled",
                Placeholder.component("state", state)));
    }

    private boolean isNpcAction(String raw) {
        return "server".equals(raw) || "lobby".equals(raw) || "teleport".equals(raw);
    }

    private List<String> navigatorEntryPaths() {
        List<String> paths = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("navigator.entries");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                paths.add("navigator.entries." + key + ".name");
                paths.add("navigator.entries." + key + ".lore");
                paths.add("navigator.entries." + key + ".icon");
                paths.add("navigator.entries." + key + ".action");
                paths.add("navigator.entries." + key + ".server");
                paths.add("navigator.entries." + key + ".world");
                paths.add("navigator.entries." + key + ".x");
                paths.add("navigator.entries." + key + ".y");
                paths.add("navigator.entries." + key + ".z");
                paths.add("navigator.entries." + key + ".yaw");
                paths.add("navigator.entries." + key + ".pitch");
            }
        }
        return paths;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("hub.config")) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("reload", "set", "show", "npc").stream()
                    .filter(opt -> opt.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args[0].equalsIgnoreCase("npc")) {
            if (args.length == 2) {
                return List.of("add", "remove", "list", "enable").stream()
                        .filter(opt -> opt.startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .toList();
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
                ConfigurationSection entries = plugin.getConfig().getConfigurationSection("lobby-npcs.entries");
                if (entries == null) {
                    return List.of();
                }
                return entries.getKeys(false).stream()
                        .filter(key -> key.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                        .toList();
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
                return List.of("server", "lobby", "teleport").stream()
                        .filter(opt -> opt.startsWith(args[3].toLowerCase(Locale.ROOT)))
                        .toList();
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("enable")) {
                return List.of("true", "false").stream()
                        .filter(opt -> opt.startsWith(args[2].toLowerCase(Locale.ROOT)))
                        .toList();
            }
            return List.of();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("show"))) {
            List<String> paths = new ArrayList<>(COMMON_PATHS);
            paths.addAll(navigatorEntryPaths());
            return paths.stream()
                    .filter(path -> path.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
