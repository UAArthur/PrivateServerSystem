package net.hauntedstudio.ps.spigot.events;

import net.hauntedstudio.ps.spigot.PS;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;
import java.util.UUID;

public class JoinListener implements Listener {

    private PS plugin;

    public JoinListener(PS plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (this.plugin.isPrivateServer()) {
            String ownerUUID = this.plugin.getPsInfo().get("OwnerUUID").getAsString();
            // Check if the owner already has Operator status
            if (Bukkit.getOperators().stream().noneMatch(op -> Objects.requireNonNull(op.getUniqueId().toString()).equalsIgnoreCase(e.getPlayer().getName()))) {
                // If not, make the owner an operator
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
                Bukkit.getOperators().add(offlinePlayer);
                this.plugin.getPsLogger().info("Made operator: " + offlinePlayer.getName());
            } else {
                this.plugin.getPsLogger().info("Owner is already an operator: " + e.getPlayer().getName());
            }
        }


    }
}
