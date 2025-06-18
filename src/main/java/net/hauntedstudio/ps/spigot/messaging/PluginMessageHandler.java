package net.hauntedstudio.ps.spigot.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PluginMessageHandler implements PluginMessageListener {

    private static final String CHANNEL = "ps:messaging";
    private final JavaPlugin plugin;
    private final Gson gson;
    private final Map<String, Consumer<JsonObject>> messageHandlers;

    public PluginMessageHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.messageHandlers = new HashMap<>();

        // Register the channel
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    /**
     * Registers a handler for a specific message type
     *
     * @param type    The message type to handle
     * @param handler The handler function that receives the JSON data
     */
    public void registerHandler(String type, Consumer<JsonObject> handler) {
        messageHandlers.put(type, handler);
    }

    /**
     * Sends a JSON message to the BungeeCord server
     *
     * @param player The player to send the message through
     * @param type   The message type
     * @param json   The JSON data to send
     */
    public void sendJson(Player player, String type, JsonElement json) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);

            out.writeUTF(type);

            out.writeUTF(gson.toJson(json));

            player.sendPluginMessage(plugin, CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send plugin message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a JSON message to the BungeeCord server using any available player
     *
     * @param type The message type
     * @param json The JSON data to send
     * @return true if the message was sent, false if no player was available
     */
    public boolean sendJson(String type, JsonElement json) {
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            return false;
        }

        Player player = plugin.getServer().getOnlinePlayers().iterator().next();
        sendJson(player, type, json);
        return true;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

            String type = in.readUTF();
            String jsonString = in.readUTF();
            JsonObject json = gson.fromJson(jsonString, JsonObject.class);

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
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }


    /// calls Requests etc
    public boolean requestOnlineServers(Consumer<JsonObject> callback) {
        String responseId = "serverList_" + System.currentTimeMillis();
        registerHandler(responseId, callback);

        JsonObject request = new JsonObject();
        request.addProperty("responseId", responseId);

        return sendJson("getOnlineServers", request);
    }
}