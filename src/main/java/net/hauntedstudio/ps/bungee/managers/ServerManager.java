package net.hauntedstudio.ps.bungee.managers;

import com.google.gson.Gson;
import gnu.trove.impl.sync.TSynchronizedShortByteMap;
import lombok.Getter;
import net.hauntedstudio.ps.bungee.PS;
import net.hauntedstudio.ps.bungee.models.PSInfo;
import net.hauntedstudio.ps.bungee.models.Server;
import net.hauntedstudio.ps.bungee.models.Template;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerManager {
    private final PS plugin;

    @Getter
    private final HashMap<ProxiedPlayer, Server> servers = new HashMap<>();

    public ServerManager(PS plugin) {
        this.plugin = plugin;
        this.plugin.getLoger().debug("ServerManager initialized.");
        File serversDir = new File(plugin.getDataFolder(), "servers");
        if (!serversDir.exists()) {
            if (serversDir.mkdirs()) {
                plugin.getLoger().debug("Servers directory created successfully.");
            } else {
                plugin.getLoger().error("Failed to create servers directory.");
            }
        }
    }

    public ServerInfo addServer(String name, String address, int port) {
        this.plugin.getLoger().debug("Adding server: " + name + " at " + address + ":" + port);
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        String key = "ps_"+name;
        ServerInfo serverInfo = this.plugin.getProxy().constructServerInfo(
                key,
                socketAddress,
                key + " Instance",
                false
        );

        this.plugin.getProxy().getServers().put(key, serverInfo);
        return serverInfo;
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

    public Server createServer(ProxiedPlayer player, String serverName, Template template) {
        //Check if player is null
        if (serverName == null || serverName.isEmpty()) {
            plugin.getLoger().error("Server name cannot be null or empty.");
            player.sendMessage("§cServer name cannot be empty.");
            return null;
        }

        //Check if the template is valid
        if (template == null) {
            plugin.getLoger().error("Template cannot be null for server creation.");
            player.sendMessage("§cTemplate not found or invalid.");
            return null;
        }

        //Check if player already has a server with the same name
        if (hasPlayerServer(player, serverName)) {
            plugin.getLoger().error("Player already has a server with the name: " + serverName);
            player.sendMessage("§cYou already have a server with this name.");
            return null;
        }

        //Check if player has permission to use the template
        if (template.doesNeedPermissions() && !hasPermission(player, "privatserver.template." + template.getId())) {
            plugin.getLoger().error("Player does not have permission to use this template: " + template.getId());
            return null;
        }

        //Check if player reached the max servers limit
        int maxServers = plugin.getSettingsManager().getSettings().getMaxServersPerPlayer();
        if (maxServers > 0 && getPlayerServers(player).length >= maxServers && !(hasPermission(player, "privatserver.bypass.maxservers"))) {
            plugin.getLoger().error("Player has reached the maximum number of servers: " + maxServers);
            player.sendMessage("§cYou have reached the maximum number of servers allowed.");
            return null;
        }

        plugin.getLoger().debug("Creating server for player: " + player.getName() + " with template: " + template.getName());
        player.sendMessage("§aCreating your server...");

        int port = plugin.getUtils().findFreePortInRange(this.plugin.getSettingsManager().getSettings().getServerPortRange().getMin(),
                this.plugin.getSettingsManager().getSettings().getServerPortRange().getMax());
        if (!plugin.getUtils().isFreePort(port)) {
            plugin.getLoger().error("No free port found for server: " + serverName);
            player.sendMessage("§cNo free port available. Please try again later.");
            return null;
        }

        Server server = new Server();
        server.setOwnerUUID(player.getUniqueId().toString());
        server.setOwnerUsername(player.getDisplayName());
        server.setName(serverName);
        server.setAddress("127.0.0.1");
        server.setPort(port);
        server.setMotd(player.getDisplayName() + "'s PrivateServer");
        server.setMaxPlayers(template.getMaxPlayers());
        server.setOnlinePlayers(0);
        server.setPublic(true);
        server.setStatus("Offline");

        File serverDir = new File(plugin.getDataFolder(), "servers/" + player.getUniqueId().toString() + "/" + serverName);
        if (!serverDir.exists() && !serverDir.mkdirs()) {
            plugin.getLoger().error("Failed to create server directory for: " + serverName);
            player.sendMessage("§cFailed to create server directory. Please contact an administrator.");
            return null;
        }

        Path target = template.getServerFile().toPath();
        this.plugin.getLoger().debug("Trying to use template server file: " + target.toString());
        Path link = Paths.get(serverDir.getAbsolutePath(), "spigot.jar");
        try {
            Files.createSymbolicLink(link, target);
            plugin.getLoger().debug("Symbolic link created: " + link + " -> " + target);
            player.sendMessage("§aServer files prepared successfully.");
        } catch (UnsupportedOperationException e) {
            plugin.getLoger().error("Symbolic links not supported on this platform.");
            player.sendMessage("§cSymbolic links are not supported on this system. Please contact an administrator.");
        } catch (IOException e) {
            plugin.getLoger().error("Failed to create symbolic link: " + e.getMessage());
            player.sendMessage("§cFailed to prepare server files. Please contact an administrator.");
        } catch (Exception e) {
            plugin.getLoger().error("Unexpected error: " + e.getMessage());
            player.sendMessage("§cAn unexpected error occurred. Please contact an administrator.");
        }

        // Create serverProperties file
        File serverProperties = new File(serverDir, "server.properties");
        try (var in = plugin.getClass().getClassLoader().getResourceAsStream("server.properties")) {
            if (in == null) {
                plugin.getLoger().error("server.properties not found in resources!");
                player.sendMessage("§cDefault server properties not found. Please contact an administrator.");
                return null;
            }
            plugin.getDataFolder().mkdirs();
            java.nio.file.Files.copy(in, serverProperties.toPath());

            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(serverProperties)) {
                props.load(fis);
            }
            props.setProperty("motd", server.getMotd());
            props.setProperty("server-port", String.valueOf(server.getPort()));
            try (FileOutputStream fos = new FileOutputStream(serverProperties)) {
                props.store(fos, "Updated by ServerManager");
                plugin.getLoger().debug("server.properties updated with motd and server-port.");
            } catch (IOException e) {
                plugin.getLoger().error("Failed to update server.properties: " + e.getMessage());
                player.sendMessage("§cFailed to update server properties. Please contact an administrator.");
                return null;
            }

            plugin.getLoger().debug("Default settings.json copied from resources.");
        } catch (IOException e) {
            plugin.getLoger().error("Failed to copy default server.properties: " + e.getMessage());
            player.sendMessage("§cFailed to prepare server properties. Please contact an administrator.");
            return null;
        }

        //Create Json in the server directory
        File configJson = new File(serverDir, "ps.json");
        Gson gson = new Gson();

        PSInfo psInfo = new PSInfo();
        psInfo.setOwnerUUID(player.getUniqueId().toString());
        psInfo.setOwnerUsername(player.getDisplayName());
        psInfo.setServerName(serverName);

        try (FileWriter writer = new FileWriter(configJson)) {
            gson.toJson(psInfo, writer);
            plugin.getLoger().debug("Created server config JSON file.");
        } catch (IOException e) {
            plugin.getLoger().error("Failed to create server config JSON: " + e.getMessage());
            player.sendMessage("§cFailed to create server configuration. Please contact an administrator.");
            return null;
        }

        File templateFolder = new File(template.getPath());
        if (templateFolder.exists() && templateFolder.isDirectory()) {
            try {
                Files.walk(templateFolder.toPath()).forEach(source -> {
                    Path destination = serverDir.toPath().resolve(templateFolder.toPath().relativize(source));
                    if (source.getFileName().toString().equalsIgnoreCase("spigot.jar")) {
                        return;
                    }
                    if (source.getFileName().toString().equalsIgnoreCase("template.json")) {
                        return;
                    }
                    try {
                        if (Files.isDirectory(source)) {
                            if (!Files.exists(destination)) {
                                Files.createDirectory(destination);
                            }
                        } else {
                            Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        plugin.getLoger().error("Failed to copy file: " + source + " -> " + destination + ": " + e.getMessage());
                    }
                });
                plugin.getLoger().debug("Template files copied to server directory.");
            } catch (IOException e) {
                plugin.getLoger().error("Failed to copy template files: " + e.getMessage());
                player.sendMessage("§cFailed to copy template files. Please contact an administrator.");
                return null;
            }
        } else {
            plugin.getLoger().error("Template folder is invalid or does not exist.");
            player.sendMessage("§cTemplate folder is missing. Please contact an administrator.");
            return null;
        }


        plugin.getLoger().debug("Created server: " + serverName + " on port " + port);
        player.sendMessage("§aYour server '" + serverName + "' has been created on port " + port + "!");
        return server;
    }


    public void startServer(ProxiedPlayer initiator, UUID targetPlayerUuid, String serverName) {
        // Check permissions
        if (!hasPermission(initiator, "privatserver.start.others")) {
            initiator.sendMessage("§cYou don't have permission to start other players' servers.");
            return;
        }

        File serverDir = new File(plugin.getDataFolder(), "servers/" + targetPlayerUuid + "/" + serverName);
        if (!serverDir.exists() || !serverDir.isDirectory()) {
            initiator.sendMessage("§cServer '" + serverName + "' not found for the specified player.");
            return;
        }

        if (isServerRunning(targetPlayerUuid, serverName)) {
            initiator.sendMessage("§cThis server is already running.");
            return;
        }

        plugin.getLoger().debug("Starting server for player UUID: " + targetPlayerUuid + " with name: " + serverName);
//        plugin.getPsClient().sendMessage("startServer " + targetPlayerUuid + " " + serverDir.getAbsolutePath());
        plugin.getPsClient().sendStartServerCommand(String.valueOf(targetPlayerUuid), serverName);

        Server server = getServerByName(targetPlayerUuid, serverName);
        if (server == null) {
            initiator.sendMessage("§cFailed to retrieve server information. Please try again later.");
            return;
        }

        servers.put(ProxyServer.getInstance().getPlayer(targetPlayerUuid), server);
        initiator.sendMessage("§aStarting server '" + serverName + "' for player " +
                (ProxyServer.getInstance().getPlayer(targetPlayerUuid) != null ?
                        ProxyServer.getInstance().getPlayer(targetPlayerUuid).getName() : "unknown") + "...");
    }

    // Start server for the player
    public void startServer(ProxiedPlayer player, String serverName) {
        if (!hasPlayerServer(player, serverName)) {
            player.sendMessage("§cYou don't have a server with this name.");
            return;
        }

        if (!hasPermission(player, "privatserver.start")) {
            player.sendMessage("§cYou don't have permission to start servers.");
            return;
        }

        if (hasPlayerOneOnlineServer(player)) {
            player.sendMessage("§cYou already have a server with players online. Please stop it first.");
            return;
        }

        if (servers.containsKey(player)) {
            player.sendMessage("§cYou already have a server with players online. Please stop it first.");
            return;
        }

        startServer(player, player.getUniqueId(), serverName);
    }

    public Server getServerByName(UUID playerUuid, String serverName) {
        File serverDir = new File(plugin.getDataFolder(), "servers/" + playerUuid + "/" + serverName);
        if (!serverDir.exists() || !serverDir.isDirectory()) {
            plugin.getLoger().debug("Server directory not found: " + serverDir.getPath());
            return null;
        }

        String ownerUsername = "";
        File psJsonFile = new File(serverDir, "ps.json");
        if (psJsonFile.exists()) {
            try (FileReader reader = new FileReader(psJsonFile)) {
                Gson gson = new Gson();
                PSInfo psInfo = gson.fromJson(reader, PSInfo.class);
                if (psInfo != null) {
                    ownerUsername = psInfo.getOwnerUsername();
                }
            } catch (IOException e) {
                plugin.getLoger().error("Failed to read ps.json: " + e.getMessage());
            }
        }

        Server server = new Server();
        server.setOwnerUUID(playerUuid.toString());
        server.setOwnerUsername(ownerUsername);
        server.setName(serverName);
        server.setAddress(loadServerProperties(serverDir).getProperty("address"));
        server.setPort(Integer.parseInt(loadServerProperties(serverDir).getProperty("server-port", "25565")));
        server.setMotd(loadServerProperties(serverDir).getProperty("motd", "Private Server"));
        server.setMaxPlayers(Integer.parseInt(loadServerProperties(serverDir).getProperty("max-players", "20")));
        server.setOnlinePlayers(0);
        server.setStatus("Offline");

        plugin.getLoger().debug("Retrieved server: " + serverName + " for player UUID: " + playerUuid);
        return server;
    }


    public void stopAllServers() {
        plugin.getLoger().debug("Stopping all servers...");
        plugin.getPsClient().sendMessage("stopAllServers");
        servers.clear();
        plugin.getLoger().debug("All servers stopped.");
    }

    private boolean isServerRunning(UUID playerUuid, String serverName) {
        for (Map.Entry<ProxiedPlayer, Server> entry : servers.entrySet()) {
            if (entry.getKey() != null &&
                    entry.getKey().getUniqueId().equals(playerUuid) &&
                    entry.getValue().getName().equals(serverName)) {
                return true;
            }
        }
        return false;
    }
    public boolean hasPlayerServer(ProxiedPlayer player, String serverName) {
        File serverDir = new File(plugin.getDataFolder(), "servers/" + player.getUniqueId().toString() + "/" + serverName);
        return serverDir.exists() && serverDir.isDirectory();
    }
    public Server[] getPlayerServers(ProxiedPlayer player) {
        File serverDir = new File(plugin.getDataFolder(), "servers/" + player.getUniqueId().toString());
        if (!serverDir.exists() || !serverDir.isDirectory()) {
            return new Server[0];
        }

        File[] serverFiles = serverDir.listFiles();
        if (serverFiles == null || serverFiles.length == 0) {
            return new Server[0];
        }

        Server[] servers = new Server[serverFiles.length];
        for (int i = 0; i < serverFiles.length; i++) {
            File file = serverFiles[i];
            if (file.isDirectory()) {
                String name = file.getName();
                servers[i] = new Server();
                servers[i].setName(name);
            }
        }
        return servers;
    }
    public boolean hasPlayerOneOnlineServer(ProxiedPlayer player) {
        Server[] servers = getPlayerServers(player);
        for (Server server : servers) {
            if (server.getOnlinePlayers() > 0) {
                return true;
            }
        }
        return false;
    }
    public Server getServerByName(ProxiedPlayer player, String serverName) {
        if (!hasPlayerServer(player, serverName)) {
            player.sendMessage("§cYou don't have a server with this name.");
            return null;
        }

        File serverDir = new File(plugin.getDataFolder(), "servers/" + player.getUniqueId().toString() + "/" + serverName);
        if (!serverDir.exists() || !serverDir.isDirectory()) {
            player.sendMessage("§cServer directory not found.");
            return null;
        }

        Server server = new Server();
        server.setOwnerUUID(player.getUniqueId().toString());
        server.setName(serverName);
        server.setAddress(loadServerProperties(serverDir).getProperty("address"));
        server.setPort(Integer.parseInt(loadServerProperties(serverDir).getProperty("server-port", "25565")));
        server.setMotd(loadServerProperties(serverDir).getProperty("motd", "Private Server"));
        server.setMaxPlayers(Integer.parseInt(loadServerProperties(serverDir).getProperty("max-players", "20")));
        server.setOnlinePlayers(0);
        server.setStatus("Offline");

        plugin.getLoger().debug("Retrieved server: " + serverName + " for player: " + player.getName());
        return server;
    }
    public Properties loadServerProperties(File serverDir) {
        File serverProperties = new File(serverDir, "server.properties");
        Properties props = new Properties();
        if (!serverProperties.exists()) {
            plugin.getLoger().error("server.properties not found in " + serverDir.getAbsolutePath());
            return props;
        }
        try (FileInputStream fis = new FileInputStream(serverProperties)) {
            props.load(fis);
        } catch (IOException e) {
            plugin.getLoger().error("Failed to load server.properties: " + e.getMessage());
        }
        return props;
    }
    private boolean hasPermission(ProxiedPlayer player, String permission) {
        if (player.hasPermission("privatserver.admin")) return true;
        if (!player.hasPermission(permission)) {
            player.sendMessage("§cYou don't have permission to do this.");
            return false;
        }
        return true;
    }
    public UUID getUUIDFromUsername(String username) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(username);
        if (player != null) {
            return player.getUniqueId();
        }

        File serversDir = new File(plugin.getDataFolder(), "servers");
        if (serversDir.exists() && serversDir.isDirectory()) {
            File[] playerDirs = serversDir.listFiles();
            if (playerDirs != null) {
                for (File playerDir : playerDirs) {
                    if (playerDir.isDirectory()) {
                        try {
                            UUID uuid = UUID.fromString(playerDir.getName());
                            String name = plugin.getProxy().getPlayer(uuid) != null ?
                                    plugin.getProxy().getPlayer(uuid).getName() : null;

                            if (name != null && name.equalsIgnoreCase(username)) {
                                return uuid;
                            }

                            File psJsonFile = new File(playerDir, "ps.json");
                            if (psJsonFile.exists()) {
                                try (FileReader reader = new FileReader(psJsonFile)) {
                                    Gson gson = new Gson();
                                    PSInfo psInfo = gson.fromJson(reader, PSInfo.class);
                                    if (psInfo != null && psInfo.getOwnerUsername().equalsIgnoreCase(username)) {
                                        return UUID.fromString(psInfo.getOwnerUUID());
                                    }
                                } catch (IOException e) {
                                    plugin.getLoger().error("Failed to read ps.json for " + playerDir.getName() + ": " + e.getMessage());
                                }
                            }

                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        }

        plugin.getLoger().debug("Could not resolve UUID for username: " + username);
        return null;
    }
    public boolean hasPlayerServer(UUID playerUuid, String serverName) {
        File serverDir = new File(plugin.getDataFolder(), "servers/" + playerUuid + "/" + serverName);
        return serverDir.exists() && serverDir.isDirectory();
    }
}