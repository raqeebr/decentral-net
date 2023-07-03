package org.uom.model.message;

public class SearchMessage extends Message {
    public String initiatorIp;
    public int initiatorPort;
    public String query;
    public int hops;

    public SearchMessage(Message response) {
        super(response);
        this.initiatorIp = this.payload[2];
        this.initiatorPort = Integer.parseInt(this.payload[3]);
        this.query = this.payload[4];
        this.hops = Integer.parseInt(this.payload[5]);
    }
}
