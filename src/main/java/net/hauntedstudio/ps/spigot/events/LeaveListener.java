package net.hauntedstudio.ps.spigot.events;

import net.hauntedstudio.ps.spigot.PS;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LeaveListener implements Listener {
    private final PS plugin;

    public LeaveListener(PS plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        if (this.plugin.isPrivateServer()){
            if (this.plugin.getServer().getOnlinePlayers().size() <= 1) {
                this.plugin.getPsLogger().warning("No player left, shutting down the server in 60 seconds.");
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                    if (this.plugin.getServer().getOnlinePlayers().size() <= 1) {
                        this.plugin.getServer().shutdown();
                    }
                }, 1200L);
            }
        }
    }
}
