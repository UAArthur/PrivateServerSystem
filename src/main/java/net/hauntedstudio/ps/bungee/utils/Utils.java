package net.hauntedstudio.ps.bungee.utils;

public class Utils {

    public String formatMessage(String message, Object... args) {
        return String.format(message, args);
    }

    public String sanitizeString(String input) {
        return input.replaceAll("[^a-zA-Z0-9_\\- ]", "");
    }

    public boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

    public int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new RuntimeException("No free port found", e);
        }
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
