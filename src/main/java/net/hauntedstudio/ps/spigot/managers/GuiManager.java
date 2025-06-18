package net.hauntedstudio.ps.spigot.managers;

import net.hauntedstudio.ps.spigot.gui.GUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.*;

public class GuiManager implements Listener {
    private final List<GUI> guis;

    public GuiManager() {
        this.guis = new ArrayList<>();
    }

    public void openGui(Player player, GUI gui) {
        if (!guis.contains(gui))
            guis.add(gui);
        gui.open(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        GUI gui = guis.stream()
                .filter(g -> g.getInventory().equals(event.getClickedInventory()))
                .findFirst()
                .orElse(null);

        if (gui != null) {
            event.setCancelled(true); // Prevent default behavior
            gui.handleClick(event); // Handle the click in the GUI
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

    }
}