package com.uni.gamesever.models;

public class ServerInfoEvent {
    private final String type = "SERVER_INFO";
    private String serverTime;
    private String serverVersion;
    private String protocolVersion;
    private String motd;

    public ServerInfoEvent(String serverTime, String serverVersion, String protocolVersion, String motd) {
        this.serverTime = serverTime;
        this.serverVersion = serverVersion;
        this.protocolVersion = protocolVersion;
        this.motd = motd;
    }

    public String getType() {
        return type;
    }

    public String getServerTime() {
        return serverTime;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public String getMotd() {
        return motd;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }
}
