package net.hauntedstudio.ps;

import net.hauntedstudio.ps.util.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Wrapper implements Runnable {
    private final Logger logger;
    private final ProcessManager processManager = new ProcessManager();
    private ServerSocket serverSocket;
    private volatile boolean heartbeatReceived = false;
    private int missedHeartbeats = 0;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public static final List<Socket> clients = new ArrayList<>();

    public Wrapper(int port, Logger logger) {
        this.logger = logger;
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("Wrapper started on port: " + port);
            startHeartbeatChecker();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to start wrapper on port: " + port);
        }
    }

    private void startHeartbeatChecker() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!heartbeatReceived) {
                missedHeartbeats++;
                System.out.println("No heartbeat received. Missed count: " + missedHeartbeats);
                if (missedHeartbeats >= 3) {
                    StopWrapper();
                }
            } else {
                missedHeartbeats = 0;
            }
            heartbeatReceived = false;
        }, 6, 5, TimeUnit.SECONDS);
    }

    private void StopWrapper() {
        System.out.println("No heartbeats received for 3 intervals. Stopping Wrapper.");
        try {
            scheduler.shutdownNow();
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error while stopping the wrapper.");
        }

        System.out.println("Stopping all servers...");
        processManager.stopAllServers();
        System.exit(0);
    }


    @Override
    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                clients.add(clientSocket);
                System.out.println("New connection accepted.");
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error accepting connection.");
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if ("HEARTBEAT".equals(line)) {
                    logger.debug("Heartbeat received from client.");
                    heartbeatReceived = true;
                } else if (line.startsWith("startServer")) {
                    String[] parts = line.split(" ");
                    System.out.println("Starting server..." + parts.length);
                    if (parts.length == 3) {
                        // Expected format: startServer <uuid> <serverName> <serverDirPath>
                        System.out.println("start Server: " + line);
                        String uuid = parts[1];
                        String serverDirPath = parts[2];
                        File serverDir = new File(serverDirPath);
                        String serverName = serverDir.getName();

                        try {
                            processManager.startServer(uuid, serverName, serverDir);
                            logger.debug("Started server for " + uuid + " with name " + serverName);
                        } catch (IOException e) {
                            logger.debug("Failed to start server: " + e.getMessage());
                        }
                    }
                } else if (line.startsWith("stopAllServers")) {
                    System.out.println("Stopping all servers...");
                    processManager.stopAllServers();
                    logger.debug("All servers stopped.");
                } else {
                    logger.debug("Received unknown command: " + line);
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected or error: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        int port = 22415;
        boolean debug = false;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number, using default: " + port);
            }
        }
        if (args.length > 1 && "debug".equalsIgnoreCase(args[1])) {
            debug = true;
        }
        Logger logger = new Logger(debug);
        Wrapper wrapper = new Wrapper(port, logger);
        Thread thread = new Thread(wrapper);
        thread.start();
    }
}