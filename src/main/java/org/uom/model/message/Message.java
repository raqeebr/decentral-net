package org.uom.model.message;

public class Message {
    public final String resType;
    public String originIp;
    public int originPort;
    protected String[] payload;

    public Message(String ipAddress, int port, String response) {
        this.originIp = ipAddress;
        this.originPort = port;
        this.payload = response.split(" ");
        this.resType = this.payload[1];
    }

    protected Message(Message response) {
        this.originIp = response.originIp;
        this.originPort = response.originPort;
        this.payload = response.payload;
        this.resType = this.payload[1];
    }
}
