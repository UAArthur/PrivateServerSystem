package net.hauntedstudio.ps.spigot.gui;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public abstract class GUI {
    protected final String title;
    protected int size;
    protected final Inventory inventory;
    protected final Map<Integer, GUIAction> actions;

    public GUI(String title, int size) {
        this.title = title;
        this.size = size;
        this.inventory = Bukkit.createInventory(null, size, title);
        this.actions = new HashMap<>();
    }

    public abstract void populateInventory();

    public void setItem(int slot, ItemStack item, GUIAction action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public void open(Player player) {
        populateInventory();
        player.openInventory(inventory);
    }

    public boolean handleClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return false;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (actions.containsKey(slot)) {
            actions.get(slot).execute((Player) event.getWhoClicked(), this);
            return true;
        }

        return false;
    }

    public interface GUIAction {
        void execute(Player player, GUI gui);
    }

    public void setInventorySize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Inventory size must be a multiple of 9 between 9 and 54.");
        }

        Inventory newInventory = Bukkit.createInventory(null, size, title);

        int minSize = Math.min(this.inventory.getSize(), size);
        for (int i = 0; i < minSize; i++) {
            ItemStack item = this.inventory.getItem(i);
            if (item != null) {
                newInventory.setItem(i, item);
            }
        }

        this.size = size;
        this.actions.clear();

        try {
            java.lang.reflect.Field field = this.getClass().getSuperclass().getDeclaredField("inventory");
            field.setAccessible(true);
            field.set(this, newInventory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update inventory", e);
        }
    }
}