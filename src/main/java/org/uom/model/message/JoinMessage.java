package org.uom.model.message;

public class JoinMessage extends Message {
    public String neighbourIpAddress;
    public String neighbourPort;

    public JoinMessage(Message response) {
        super(response);
        this.neighbourIpAddress = this.payload[2];
        this.neighbourPort = this.payload[3];
    }
}
