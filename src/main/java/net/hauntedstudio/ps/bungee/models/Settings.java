package net.hauntedstudio.ps.bungee.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Settings {
    private int maxServersPerPlayer;
    private int defaultMaxPlayers;
    private boolean allowPublicServers;
    private boolean debugMode;
    private String serverNamePrefix;
    private String defaultTemplateId;
    private ServerPortRange serverPortRange;
    private MysqlSettings mysql;
    private int maxServersOnline;
    private boolean useAutomaticRestart;
    private int automaticRestartInterval;

    @Getter
    @Setter
    public static class ServerPortRange {
        private int min;
        private int max;
    }

    @Getter
    @Setter
    public static class MysqlSettings {
        private String host;
        private int port;
        private String database;
        private String user;
        private String password;
    }
}