package net.uebliche.hub;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class SpawnCommand extends Command {
    private final HubPlugin plugin;

    SpawnCommand(HubPlugin plugin) {
        super("spawn");
        this.plugin = plugin;
        setDescription("Teleport to configured spawn");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        boolean success = plugin.teleportToSpawn(player, true);
        if (!success) {
            player.sendMessage(HubPlugin.MINI.deserialize("<red>Spawn location is not configured."));
        }
        return true;
    }
}
