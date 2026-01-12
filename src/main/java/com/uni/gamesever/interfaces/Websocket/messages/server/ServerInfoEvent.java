package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class ServerInfoEvent extends Message {
    private String serverTime;
    private String serverVersion;
    private String protocolVersion;
    private String motd;

    public ServerInfoEvent(String serverTime, String serverVersion, String protocolVersion, String motd) {
        super("SERVER_INFO");
        this.serverTime = serverTime;
        this.serverVersion = serverVersion;
        this.protocolVersion = protocolVersion;
        this.motd = motd;
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
