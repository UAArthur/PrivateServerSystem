package net.hauntedstudio.ps.bungee.managers;

import net.hauntedstudio.ps.bungee.PS;
import net.hauntedstudio.ps.bungee.models.Server;
import net.hauntedstudio.ps.bungee.models.Template;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.List;

public class ServerManager {
    private final PS plugin;
    private List<ServerInfo> servers;

    public ServerManager(PS plugin) {
        this.plugin = plugin;
    }

    public void addServer(String name, String address, int port) {
        this.plugin.getLoger().debug("Adding server: " + name + " at " + address + ":" + port);
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        String key = name + "'s PrivateServer";
        ServerInfo serverInfo = ProxyServer.getInstance().constructServerInfo(
                key,
                socketAddress,
                key + " Instance",
                false
        );
        ProxyServer.getInstance().getServers().put(key, serverInfo);
    }

    public void removeServer(String name) {
        this.plugin.getLoger().debug("Removing server: " + name);
        ServerInfo serverInfo = ProxyServer.getInstance().getServers().remove(name + "'s PrivateServer");
        if (serverInfo != null) {
            this.plugin.getLoger().debug("Server " + name + " removed successfully.");
        } else {
            this.plugin.getLoger().debug("Server " + name + " not found.");
        }
    }

    public List<ServerInfo> getPServers() {
        return servers;
    }


    public Server createServer(ProxiedPlayer player, String serverName, Template template) {
        int port = plugin.getUtils().findFreePort();
        if (!plugin.getUtils().isFreePort(port)) {
            plugin.getLoger().error("No free port found for server: " + serverName);
            return null;
        }

        Server server = new Server();
        server.setOwnerUUID(player.getUniqueId().toString());
        server.setName(serverName);
        server.setAddress("127.0.0.1");
        server.setPort(port);
        server.setMotd(player.getDisplayName() + "'s PrivateServer");
        server.setMaxPlayers(4);
        server.setOnlinePlayers(0);
        server.setPublic(true);
        server.setStatus("Offline");
        // Optionally, apply template settings here

        plugin.getLoger().debug("Created server: " + serverName + " on port " + port);
        return server;
    }
}
