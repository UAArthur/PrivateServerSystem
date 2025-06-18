package net.hauntedstudio.ps.bungee.wrapper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.hauntedstudio.ps.bungee.PS;
import net.md_5.bungee.api.config.ServerInfo;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PSClient {
    private PS plugin;
    private Socket socket;
    private final String host = "localhost";
    private final int port = 22415;
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private ScheduledExecutorService reconnectExecutor;
    private static final int RECONNECT_DELAY_SECONDS = 10;

    public PSClient(PS plugin) {
        this.plugin = plugin;
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public void startConnection() {
        if (isConnecting.getAndSet(true)) {
            return;
        }

        try {
            socket = new Socket(host, port);
            System.out.println("Connected to PSWrapper at " + host + ":" + port);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            startHeartbeatThread(out);
            startListenerThread();

            isConnecting.set(false);
        } catch (Exception e) {
            plugin.getLoger().error("Failed to connect to PSWrapper at " + host + ":" + port + ": " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void startHeartbeatThread(PrintWriter out) {
        new Thread(() -> {
            try {
                while (socket != null && !socket.isClosed()) {
                    out.println("HEARTBEAT");
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                plugin.getLoger().error("Heartbeat thread error: " + e.getMessage());
                handleDisconnection();
            }
        }).start();
    }

    private void startListenerThread() {
        new Thread(() -> {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processIncomingMessage(line);
                }
            } catch (Exception e) {
                plugin.getLoger().error("Error reading from PSWrapper: " + e.getMessage());
                handleDisconnection();
            }
        }).start();
    }

    private void processIncomingMessage(String line) {
        try {
            if (line.startsWith("SERVER_STARTED")) {
                String args = line.substring("SERVER_STARTED".length()).trim();
                String[] parts = args.split(" ");
                System.out.println("Server started: " + parts[0] + " Sending player to it: " + parts[1]);
                File serverDir = new File(parts[0]);
                File psJson = new File(serverDir, "PS.json");
                Gson gson = new Gson();
                JsonObject psData = gson.fromJson(new java.io.FileReader(psJson), JsonObject.class);
                int serverPort = Integer.parseInt(this.plugin.getServerManager().loadServerProperties(serverDir).getProperty("server-port"));
                ServerInfo serverInfo = this.plugin.getServerManager().addServer(psData.get("OwnerUsername").getAsString(), "127.0.0.1", serverPort);

                try {
                    java.util.UUID playerUUID = java.util.UUID.fromString(parts[1]);
                    var player = this.plugin.getProxy().getPlayer(playerUUID);
                    if (player != null) {
                        player.connect(serverInfo);
                    } else {
                        plugin.getLoger().error("Cannot connect player to server: Player with UUID " + parts[1] + " not found");
                    }
                } catch (IllegalArgumentException e) {
                    var player = this.plugin.getProxy().getPlayer(parts[1]);
                    if (player != null) {
                        player.connect(serverInfo);
                    } else {
                        plugin.getLoger().error("Cannot connect player to server: Player " + parts[1] + " not found");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLoger().error("Error processing message: " + e.getMessage());
        }
    }

    private void handleDisconnection() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        isConnecting.set(false);
        plugin.getLoger().info("Scheduling reconnection attempt in " + RECONNECT_DELAY_SECONDS + " seconds");
        reconnectExecutor.schedule(this::startConnection, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    public void sendStartServerCommand(String uuid, String serverName) {
        sendMessage("START_SERVER " + uuid + " " + serverName);
    }

    public void sendMessage(String message) {
        if (socket != null && !socket.isClosed()) {
            try {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                out.println(message);
            } catch (Exception e) {
                plugin.getLoger().error("Failed to send message to PSWrapper: " + e.getMessage());
                handleDisconnection();
            }
        } else {
            plugin.getLoger().error("Socket is not connected. Attempting to reconnect...");
            startConnection();
        }
    }

    public void stopConnection() {
        reconnectExecutor.shutdownNow();
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                System.out.println("Connection to PSWrapper closed.");
            } catch (Exception e) {
                plugin.getLoger().error("Failed to close connection to PSWrapper: " + e.getMessage());
            }
        }
    }
}