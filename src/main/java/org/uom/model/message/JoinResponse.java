package org.uom.model.message;

public class JoinResponse extends Message {
    public int value;

    public JoinResponse(Message response) {
        super(response);
        this.value = Integer.parseInt(this.payload[2]);
    }

    public JoinResponse(String ipAddress, int port, String response, int value) {
        super(ipAddress, port, response);
        this.value = value;
    }
}
