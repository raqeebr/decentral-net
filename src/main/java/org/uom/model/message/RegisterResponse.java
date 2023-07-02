package org.uom.model.message;

import org.uom.utils.Constants.MessageTypes;

public class RegisterResponse extends Message {
    public int noOfNodes;
    public String neighbourIp1;
    public String neighbourIp2;
    public int neighbourPort1;
    public int neighbourPort2;

    public RegisterResponse(Message response) {
        super(response);

        if (!this.resType.equals(MessageTypes.REGOK)) {
            return;
        }
        if ((this.noOfNodes = Integer.parseInt(this.payload[2])) == 0) {
            return;
        }

        this.neighbourIp1 = this.payload[3];
        this.neighbourPort1 = Integer.parseInt(this.payload[4]);

        if (this.noOfNodes > 1) {
            this.neighbourIp2 = this.payload[5];
            this.neighbourPort2 = Integer.parseInt(this.payload[6]);
        }
    }
}