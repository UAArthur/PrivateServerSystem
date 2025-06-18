package net.hauntedstudio.ps.spigot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.hauntedstudio.ps.spigot.commands.PrivateServer_CMD;
import net.hauntedstudio.ps.spigot.events.JoinListener;
import net.hauntedstudio.ps.spigot.events.LeaveListener;
import net.hauntedstudio.ps.spigot.managers.GuiManager;
import net.hauntedstudio.ps.spigot.messaging.PluginMessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class PS extends JavaPlugin {

    @Getter
    private PSLogger psLogger;
    @Getter
    private JsonObject psInfo;
    @Getter
    private PluginMessageHandler messageHandler;


    //Managers
    @Getter
    private GuiManager guiManager;

    public PS(){
        this.psLogger = new PSLogger(this);
        File file = new File("ps.json");
        Gson gson = new Gson();
        messageHandler = new PluginMessageHandler(this);
        //load the ps.json file if it exists
        if (isPrivateServer()) {
            try {
                psInfo = gson.fromJson(new FileReader(file), JsonObject.class);
            } catch (FileNotFoundException e) {
                getLogger().warning("Could not find ps.json file");
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("!PS Spigot Plugin Enabled!");

        // Initialize GUI Manager
        this.guiManager = new GuiManager();

        // Register Listener
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LeaveListener(this), this);
        Bukkit.getPluginManager().registerEvents(this.guiManager, this);

        // Register commands
        this.getCommand("privateserver").setExecutor(new PrivateServer_CMD(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("!PS Spigot Plugin Disabled!");
    }

    public boolean isPrivateServer() {
        File file = new File("ps.json");
        if (file.exists()) {
            getLogger().info("Private server detected.");
            return true;
        } else {
            getLogger().info("Public server detected.");
            return false;
        }
    }

    public boolean isOwner(Player player){
        if (!isPrivateServer()) {
            return false;
        }
        String ownerUUID = psInfo.get("owner").getAsString();
        return player.getUniqueId().toString().equals(ownerUUID);
    }
}