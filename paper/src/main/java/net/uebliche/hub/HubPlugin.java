package net.uebliche.hub;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class HubPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "uebliche:hub");
        getServer().getMessenger().registerIncomingPluginChannel(this, "uebliche:hub", (s, player, bytes) -> {
            System.out.println("S: " + s);
            System.out.println("P: " + player);
            System.out.println("B: " + bytes);
            System.out.println("B: " + new String(bytes));
        });
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        event.getPlayer().sendMessage("Hello World!");
    }
}
