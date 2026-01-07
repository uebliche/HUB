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
            sender.sendMessage(plugin.translate(sender, "lobby.spawn.players-only"));
            return true;
        }
        boolean success = plugin.teleportToSpawn(player, true);
        if (!success) {
            player.sendMessage(plugin.translate(player, "lobby.spawn.not-configured"));
        }
        return true;
    }
}
