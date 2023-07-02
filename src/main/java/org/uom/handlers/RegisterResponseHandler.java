package org.uom.handlers;

import org.uom.contracts.handlers.MessageHandler;
import org.uom.domain.Node;
import org.uom.model.message.Message;
import org.uom.model.message.RegisterResponse;

public class RegisterResponseHandler implements MessageHandler {
    @Override
    public void handle(Node node, Message message) {
        RegisterResponse registerResponse = new RegisterResponse(message);
        if (registerResponse.neighbourIp1 != null) {
            node.join(registerResponse.neighbourIp1, registerResponse.neighbourPort1);
        }
        if (registerResponse.neighbourIp2 != null) {
            node.join(registerResponse.neighbourIp2, registerResponse.neighbourPort2);
        }
    }
}
