package net.hauntedstudio.ps.bungee.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Server {
    private String ownerUUID;
    private String name;
    private String address;
    private int port;
    private String motd;
    private int maxPlayers;
    private int onlinePlayers;
    private boolean isPublic;
    private String Status;


}
