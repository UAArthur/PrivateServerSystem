package net.hauntedstudio.ps.bungee.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hauntedstudio.ps.bungee.PS;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PluginMessageHandler implements Listener {

    private static final String CHANNEL = "ps:messaging";
    private final PS plugin;
    private final Gson gson;
    private final Map<String, Consumer<JsonObject>> messageHandlers;

    public PluginMessageHandler(PS plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.messageHandlers = new HashMap<>();

        plugin.getProxy().registerChannel(CHANNEL);
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    /**
     * Registers a handler for a specific message type
     * @param type The message type to handle
     * @param handler The handler function that receives the JSON data
     */
    public void registerHandler(String type, Consumer<JsonObject> handler) {
        messageHandlers.put(type, handler);
    }

    /**
     * Sends a JSON message to a specific server
     * @param server The target server
     * @param type The message type
     * @param json The JSON data to send
     */
    public void sendJson(ServerInfo server, String type, JsonElement json) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);

            out.writeUTF(type);

            out.writeUTF(gson.toJson(json));

            server.sendData(CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send plugin message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a JSON message to all servers
     * @param type The message type
     * @param json The JSON data to send
     */
    public void broadcastJson(String type, JsonElement json) {
        for (ServerInfo server : plugin.getProxy().getServers().values()) {
            sendJson(server, type, json);
        }
    }

    /**
     * Sends a JSON message to the server a player is connected to
     * @param player The player whose server should receive the message
     * @param type The message type
     * @param json The JSON data to send
     */
    public void sendJsonToPlayerServer(ProxiedPlayer player, String type, JsonElement json) {
        Server server = player.getServer();
        if (server != null) {
            sendJson(server.getInfo(), type, json);
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(CHANNEL)) {
            return;
        }

        event.setCancelled(true);

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));

            String type = in.readUTF();
            String jsonString = in.readUTF();
            JsonObject json = gson.fromJson(jsonString, JsonObject.class);

            if (type.equals("getOnlineServers") && event.getSender() instanceof Server) {
                String responseId = json.get("responseId").getAsString();
                Server senderServer = (Server) event.getSender();

                JsonObject response = new JsonObject();
                JsonObject serversObject = new JsonObject();

                System.out.println(this.plugin.getServerManager().getServers().size());
                this.plugin.getServerManager().getServers().forEach(((player, server) -> {
                    System.out.println("Processing server for player: " + player.getName());
                    ServerInfo serverInfo = this.plugin.getProxy().getServerInfo("ps_" + server.getOwnerUsername());
                    System.out.println("Server info for ps_" + server.getOwnerUsername() + ": " + serverInfo);
                    if (serverInfo != null) {
                        JsonObject serverData = new JsonObject();
                        serverData.addProperty("name", serverInfo.getName());
                        serverData.addProperty("online", server.getStatus());
                        serverData.addProperty("playerCount", serverInfo.getPlayers().size());
                        serversObject.add(serverInfo.getName(), serverData);
                    }
                }));

                response.add("servers", serversObject);

                System.out.println("Outgoing server data: " + response);

                sendJson(senderServer.getInfo(), responseId, response);
                return;
            }

            Consumer<JsonObject> handler = messageHandlers.get(type);
            if (handler != null) {
                handler.accept(json);
            } else {
                plugin.getLogger().warning("Received unknown plugin message type: " + type);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to process plugin message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unregister() {
        plugin.getProxy().unregisterChannel(CHANNEL);
    }
}