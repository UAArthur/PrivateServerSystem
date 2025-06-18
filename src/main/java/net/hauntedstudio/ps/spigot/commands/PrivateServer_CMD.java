package net.hauntedstudio.ps.spigot.commands;

import net.hauntedstudio.ps.spigot.PS;
import net.hauntedstudio.ps.spigot.gui.ServerListGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrivateServer_CMD implements CommandExecutor {

    private final PS plugin;
    public PrivateServer_CMD(PS plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        openServerList(player);
        return false;
    }

    public void openServerList(Player player) {
        System.out.println(player.getUniqueId().toString());
        ServerListGUI gui = new ServerListGUI(plugin);
        this.plugin.getGuiManager().openGui(player, gui);
    }
}
