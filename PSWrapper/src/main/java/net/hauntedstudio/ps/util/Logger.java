package net.hauntedstudio.ps.util;

public class Logger {
    private final boolean debug;

    public Logger(boolean debug) {
        this.debug = debug;
    }

    public void debug(String msg) {
        if (debug) {
            System.out.println("[DEBUG] " + msg);
        }
    }
}
