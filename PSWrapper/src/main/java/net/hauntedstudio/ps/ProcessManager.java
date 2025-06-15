package net.hauntedstudio.ps;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessManager {
    private final Map<String, Process> runningServers = new ConcurrentHashMap<>();

    public void startServer(String uuid, String serverName, File serverDir) throws IOException {
        // Only use serverName as the key
        String key = serverName;
        File jarFile = new File(serverDir, "spigot.jar");
        if (!jarFile.exists()) throw new IOException("spigot.jar not found in " + serverDir);

        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\Java\\jdk-21\\bin\\java",
                "-Xmx1G",
                "-jar",
                "spigot.jar",
                "nogui"
        );
        pb.directory(serverDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        runningServers.put(key, process);

        new Thread(() -> {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Done")) {
                        java.util.List<Socket> clientsToRemove = new java.util.ArrayList<>();

                        Wrapper.clients.forEach(client -> {
                            try {
                                if (!client.isClosed()) {
                                    client.getOutputStream().write(
                                            ("SERVER_STARTED " + serverDir + " " + uuid + "\n").getBytes()
                                    );
                                } else {
                                    clientsToRemove.add(client);
                                }
                            } catch (IOException e) {
                                clientsToRemove.add(client);
                                e.printStackTrace();
                            }
                        });

                        Wrapper.clients.removeAll(clientsToRemove);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stopAllServers() {
        for (Process process : runningServers.values()) {
            if (process.isAlive()) {
                process.destroy();
            }
        }
        runningServers.clear();
    }
}