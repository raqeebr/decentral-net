package org.uom.model.message;

public class SocketMessage {
    public String ipAddress;
    public int port;
    public String body;

    public SocketMessage(String ipAddress, int port, String message) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.body = message;
    }
}
