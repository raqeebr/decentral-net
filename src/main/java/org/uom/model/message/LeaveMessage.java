package org.uom.model.message;

public class LeaveMessage extends Message {
    public String ipAddress;
    public int port;

    public LeaveMessage(Message message) {
        super(message);
        this.ipAddress = this.payload[2];
        this.port = Integer.parseInt(this.payload[3]);
    }
}
