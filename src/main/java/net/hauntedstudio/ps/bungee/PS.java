package net.hauntedstudio.ps.bungee;

import lombok.Getter;
import net.hauntedstudio.ps.bungee.commands.privateServer_CMD;
import net.hauntedstudio.ps.bungee.managers.ServerManager;
import net.hauntedstudio.ps.bungee.managers.SettingsManager;
import net.hauntedstudio.ps.bungee.managers.TemplateManager;
import net.hauntedstudio.ps.bungee.messaging.PluginMessageHandler;
import net.hauntedstudio.ps.bungee.utils.Logger;
import net.hauntedstudio.ps.bungee.utils.Utils;
import net.hauntedstudio.ps.bungee.wrapper.PSClient;
import net.md_5.bungee.api.plugin.Plugin;

public class PS extends Plugin {
    //Classes
    @Getter
    private Utils utils;
    private Logger logger;
    @Getter
    private PSClient psClient;
    @Getter
    private PluginMessageHandler messageHandler;


    //Managers
    @Getter
    private ServerManager serverManager;
    @Getter
    private TemplateManager templateManager;
    @Getter
    private SettingsManager settingsManager;

    @Getter
    private boolean isDebug = true;


    public PS(){
        // Initialize the logger
        this.utils = new Utils();
        this.logger = new Logger(this);
        this.psClient = new PSClient(this);
        // Managers
        this.serverManager = new ServerManager(this);
        this.templateManager = new TemplateManager(this);
        this.settingsManager = new SettingsManager(this);
        //pluginmessage handler
        messageHandler = new PluginMessageHandler(this);
    }
    @Override
    public void onEnable() {
        getLogger().info("!PrivateServer BungeeCord plugin enabled!");

        // Register commands
        getProxy().getPluginManager().registerCommand(this, new privateServer_CMD(this, "privateServer"));

        // Start the PSClient connection
        psClient.startConnection();
    }

    @Override
    public void onDisable() {
        getLogger().info("!PrivateServer BungeeCord plugin disabled!");
        getServerManager().stopAllServers();
    }

    public Logger getLoger() {
        return logger;
    }

}
