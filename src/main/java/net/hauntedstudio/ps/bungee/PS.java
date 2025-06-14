package net.hauntedstudio.ps.bungee;

import lombok.Getter;
import net.hauntedstudio.ps.bungee.commands.privateServer_CMD;
import net.hauntedstudio.ps.bungee.managers.ServerManager;
import net.hauntedstudio.ps.bungee.managers.TemplateManager;
import net.hauntedstudio.ps.bungee.utils.Logger;
import net.hauntedstudio.ps.bungee.utils.Utils;
import net.md_5.bungee.api.plugin.Plugin;

public class PS extends Plugin {
    //Classes
    @Getter
    private Utils utils;
    private Logger logger;
    //Managers
    @Getter
    private ServerManager serverManager;
    @Getter
    private TemplateManager templateManager;
    @Getter
    private boolean isDebug = true;


    public PS(){
        // Initialize the logger
        this.utils = new Utils();
        this.logger = new Logger(this);
        // Managers
        this.serverManager = new ServerManager(this);
        this.templateManager = new TemplateManager(this);
    }
    @Override
    public void onEnable() {
        getLogger().info("!PrivateServer BungeeCord plugin enabled!");

        // Register commands
        getProxy().getPluginManager().registerCommand(this, new privateServer_CMD(this, "privateServer"));

    }

    @Override
    public void onDisable() {
        getLogger().info("!PrivateServer BungeeCord plugin disabled!");
    }

    public Logger getLoger() {
        return logger;
    }

}
