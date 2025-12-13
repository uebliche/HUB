package net.uebliche.hub;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
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
                sender.sendMessage(Component.text("Config reloaded.", NamedTextColor.GREEN));
                return true;
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /" + label + " set <path> <value>", NamedTextColor.YELLOW));
                    return true;
                }
                String path = args[1];
                String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (applyValue(path, value)) {
                    plugin.saveConfig();
                    plugin.loadConfig();
                    sender.sendMessage(Component.text("Updated " + path + " -> " + value, NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Could not update " + path + " with value '" + value + "'.", NamedTextColor.RED));
                }
                return true;
            }
            case "show" -> {
                if (args.length < 2) {
                    sendOverview(sender);
                    return true;
                }
                String path = args[1];
                Object current = plugin.getConfig().get(path);
                sender.sendMessage(Component.text(path + " = " + String.valueOf(current), NamedTextColor.AQUA));
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
        sender.sendMessage(Component.text("HUB Config:", NamedTextColor.GOLD));
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
        sender.sendMessage(Component.text("Use /hubconfig set <path> <value> (use '|' to separate lore lines).", NamedTextColor.GRAY));
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
            return List.of("reload", "set", "show").stream()
                    .filter(opt -> opt.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
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
