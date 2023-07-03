package org.uom.model.message;

public class SearchResponse extends Message {
    public int fileCount;
    public String fileHostIp;
    public int fileHostPort;
    public int hops;
    public String[] fileNames;

    public SearchResponse(Message response) {
        super(response);
        this.fileCount = Integer.parseInt(this.payload[2]);
        this.fileHostIp = this.payload[3];
        this.fileHostPort = Integer.parseInt(this.payload[4]);
        this.hops = Integer.parseInt(this.payload[5]);
        this.fileNames = this.payload[6].split(",");
    }
}
