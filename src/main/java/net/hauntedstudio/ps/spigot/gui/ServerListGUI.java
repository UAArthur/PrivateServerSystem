package net.hauntedstudio.ps.spigot.gui;

import com.google.gson.JsonObject;
import net.hauntedstudio.ps.spigot.PS;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerListGUI extends GUI {
    private final PS plugin;

    public ServerListGUI(PS plugin) {
        super("§6PrivateServer §8| §eServers", 54);
        this.plugin = plugin;
    }

    @Override
    public void populateInventory() {
        this.plugin.getMessageHandler().requestOnlineServers(serversData -> {
            inventory.clear();

            plugin.getPsLogger().debug("Received server data: " + serversData);

            JsonObject servers = serversData.getAsJsonObject("servers");

            int serverCount = servers.keySet().size();
            int rowsNeeded = (int) Math.ceil(serverCount / 7.0) + 2;
            int inventorySize = Math.max(27, rowsNeeded * 9);
            this.setSize(inventorySize);

            int slot = 10;
            int serversInRow = 0;

            for (String serverName : servers.keySet()) {
                JsonObject serverData = servers.get(serverName).getAsJsonObject();
                boolean isOnline = Objects.equals(serverData.get("online").getAsString(), "Online");
                int playerCount = serverData.get("playerCount").getAsInt();

                Material material = isOnline ? Material.GREEN_WOOL : Material.RED_WOOL;
                ItemStack serverItem = new ItemStack(material);
                ItemMeta meta = serverItem.getItemMeta();
                meta.setDisplayName("§e" + serverName);

                List<String> lore = new ArrayList<>();
                lore.add("§7Status: " + (isOnline ? "§aOnline" : "§cOffline"));
                lore.add("§7Players: §f" + playerCount);
                lore.add("");
                lore.add("§eClick to connect!");
                meta.setLore(lore);

                serverItem.setItemMeta(meta);

                setItem(slot, serverItem);

                serversInRow++;
                if (serversInRow >= 7) {
                    slot = slot + 11;
                    serversInRow = 0;
                } else {
                    slot++;
                }
            }

            ItemStack infoItem = new ItemStack(Material.BOOK);
            ItemMeta infoMeta = infoItem.getItemMeta();
            infoMeta.setDisplayName("§ePrivateServer Info");
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7This is a list of all private servers");
            infoLore.add("§7you can connect to.");
            infoLore.add("§7Click on a server to connect.");
            infoLore.add("§7Create your own private server with");
            infoLore.add("§e/privateserver create§7.");
            infoMeta.setLore(infoLore);
            infoItem.setItemMeta(infoMeta);
            setItem(4, infoItem);

            fillEmptySlots();
            updateInventory();
        });
    }

    private void fillEmptySlots() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                setItem(i, glass);
            }
        }
    }

    private void updateInventory() {
        inventory.getViewers().forEach(humanEntity ->
                ((org.bukkit.entity.Player)humanEntity).updateInventory());
    }
}