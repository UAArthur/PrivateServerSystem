package net.hauntedstudio.ps.bungee.utils;

import java.util.HashSet;
import java.util.Set;

public class Utils {
    private final Set<Integer> usedPorts = new HashSet<>();


    public String formatMessage(String message, Object... args) {
        return String.format(message, args);
    }

    public String sanitizeString(String input) {
        return input.replaceAll("[^a-zA-Z0-9_\\- ]", "");
    }

    public synchronized int findFreePortInRange(int minPort, int maxPort) {
        for (int port = minPort; port <= maxPort; port++) {
            if (usedPorts.contains(port)) continue;
            try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
                socket.setReuseAddress(true);
                usedPorts.add(port);
                return port;
            } catch (java.io.IOException ignored) {
            }
        }
        throw new RuntimeException("No free port found in range " + minPort + "-" + maxPort);
    }

    public boolean isFreePort(int port) {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    public boolean isValidUUID(String uuid) {
        return uuid != null && uuid.matches("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$");
    }
}
