package net.hauntedstudio.ps.spigot;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PSLogger {
    private final Logger logger;
    private final String prefix;

    /**
     * Creates a new PSLogger instance
     *
     * @param plugin The main plugin instance
     */
    public PSLogger(PS plugin) {
        this.logger = plugin.getServer().getLogger();
        this.prefix = "[PrivateServerSystem] ";
    }

    /**
     * Logs an info message
     *
     * @param message The message to log
     */
    public void info(String message) {
        logger.info(message);
    }

    /**
     * Logs a warning message
     *
     * @param message The message to log
     */
    public void warning(String message) {
        logger.warning(message);
    }

    /**
     * Logs an error message
     *
     * @param message The message to log
     */
    public void error(String message) {
        logger.severe(message);
    }

    /**
     * Logs a debug message (only if debug is enabled)
     *
     * @param message The message to log
     */
    public void debug(String message) {
        logger.fine(message);
    }

    /**
     * Logs an exception with an error message
     *
     * @param message The error message
     * @param e The exception
     */
    public void error(String message, Throwable e) {
        logger.log(Level.SEVERE, message, e);
    }
}