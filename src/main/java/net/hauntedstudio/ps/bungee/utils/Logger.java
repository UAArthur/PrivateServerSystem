package net.hauntedstudio.ps.bungee.utils;

import net.hauntedstudio.ps.bungee.PS;

public class Logger {
    private PS plugin;

    public Logger(PS plugin) {
        this.plugin = plugin;
    }


    public void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public void warning(String message) {
        System.out.println("[WARNING] " + message);
    }

    public void error(String message) {
        System.err.println("[ERROR] " + message);
    }

    public void debug(String message) {
        if (plugin.isDebug())
            System.out.println("[DEBUG] " + message);
    }
}
